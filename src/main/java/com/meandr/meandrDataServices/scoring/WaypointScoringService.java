package com.meandr.meandrDataServices.scoring;

import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Scores and interleaves Google Places and OSM waypoints into a single ranked list.
 *
 * Design principles:
 * - Both sources normalize to a 0–100 score
 * - Detour penalty formula is identical for both sources
 * - OSM results are prioritized by a configurable bias weight (default 0.6)
 * - Interleaving is per route segment so results feel geographically distributed
 */
@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@Service
public class WaypointScoringService {

    // ── Configuration ────────────────────────────────────────────────────────

    /** OSM share of final interleaved list. Google Places gets (1 - OSM_BIAS). */
    private static final double OSM_BIAS = 0.6;

    /** Detour penalty: points deducted per minute of added detour time. */
    private static final double DETOUR_PENALTY_PER_MINUTE = 3.0;

    /** Maximum detour minutes before score floors at 0. */
    private static final double MAX_DETOUR_MINUTES = 30.0;

    // ── OSM Proxy Score Weights ───────────────────────────────────────────────

    private static final double OSM_WEIGHT_PREFERENCE_MATCH = 40.0;
    private static final double OSM_WEIGHT_HAS_NAME         = 10.0;
    private static final double OSM_WEIGHT_WIKIPEDIA         = 15.0;
    private static final double OSM_WEIGHT_WEBSITE           = 8.0;
    private static final double OSM_WEIGHT_PUBLIC_ACCESS     = 10.0;
    private static final double OSM_WEIGHT_HAS_DESCRIPTION   = 5.0;
    private static final double OSM_WEIGHT_ELEVATION_MAX     = 12.0;
    // Detour penalty is shared constant above

    /** Elevation above which a peak is considered "highly notable" (metres). */
    private static final double PEAK_ELEVATION_NOTABLE = 3000.0;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Score and interleave Google Places and OSM waypoints.
     *
     * @param googlePlaces   Pre-fetched Google Places candidates with existing score fields
     * @param osmPlaces      OSM candidates from OsmService
     * @param userPreferences Ordered list of preferred entity type IDs (index 0 = most preferred)
     * @param maxResults     Total waypoints to return
     * @return Interleaved, scored, deduplicated list of ScoredWaypoint
     */
    public List<ScoredWaypoint> scoreAndInterleave(
            List<GooglePlaceCandidate> googlePlaces,
            List<OsmPlace> osmPlaces,
            List<String> userPreferences,
            int maxResults) {

        // Build preference rank map: entityTypeId → rank weight (1.0 = top, decreasing)
        Map<String, Double> prefWeights = buildPreferenceWeights(userPreferences);

        // Score each source independently
        List<ScoredWaypoint> scoredGoogle = scoreGooglePlaces(googlePlaces, prefWeights);
        List<ScoredWaypoint> scoredOsm    = scoreOsmPlaces(osmPlaces, prefWeights);

        // Sort each list descending
        scoredGoogle.sort(Comparator.comparingDouble(ScoredWaypoint::getScore).reversed());
        scoredOsm.sort(Comparator.comparingDouble(ScoredWaypoint::getScore).reversed());

        // Interleave by OSM_BIAS
        return interleave(scoredOsm, scoredGoogle, maxResults);
    }

    // ── Google Places Scoring ─────────────────────────────────────────────────

    /**
     * Normalized Google Places score (0–100).
     *
     * Base quality = rating (1–5) × log10(userRatingCount + 1), scaled to 0–70
     * Preference match = up to 20 points
     * Detour penalty = up to −30 points (shared formula)
     */
    private List<ScoredWaypoint> scoreGooglePlaces(
            List<GooglePlaceCandidate> candidates,
            Map<String, Double> prefWeights) {

        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        // Find max raw quality for normalization
        double maxRaw = candidates.stream()
                .mapToDouble(c -> rawGoogleQuality(c))
                .max().orElse(1.0);

        List<ScoredWaypoint> result = new ArrayList<>();
        for (GooglePlaceCandidate c : candidates) {
            double quality  = (rawGoogleQuality(c) / Math.max(maxRaw, 0.001)) * 70.0;
            double prefBonus = prefBonus(c.getEntityType(), prefWeights, 20.0);
            double penalty  = detourPenalty(c.getDetourMinutes());
            double score    = Math.max(0, Math.min(100, quality + prefBonus - penalty));

            result.add(ScoredWaypoint.fromGoogle(c, score));
        }
        return result;
    }

    private double rawGoogleQuality(GooglePlaceCandidate c) {
        double rating = c.getRating() != null ? c.getRating() : 0.0;
        double count  = c.getUserRatingCount() != null ? c.getUserRatingCount() : 0.0;
        return rating * Math.log10(count + 1);
    }

    // ── OSM Proxy Scoring ─────────────────────────────────────────────────────

    /**
     * Proxy-signal OSM score (0–100).
     *
     * Preference match  = up to 40 pts  (OSM gets more weight here since no quality signal)
     * Has name          = 10 pts
     * Wikipedia link    = 15 pts
     * Website           = 8 pts
     * Public access     = 10 pts
     * Has description   = 5 pts
     * Elevation bonus   = up to 12 pts  (peaks only)
     * Detour penalty    = up to −30 pts (same formula as Google)
     *
     * Raw max before penalty = 100 pts
     */
    private List<ScoredWaypoint> scoreOsmPlaces(
            List<OsmPlace> candidates,
            Map<String, Double> prefWeights) {

        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        List<ScoredWaypoint> result = new ArrayList<>();
        for (OsmPlace p : candidates) {
            double score = 0;

            // Preference match (40 pts max)
            score += prefBonus(p.getEntityType() != null ? p.getEntityType().name() : null,
                    prefWeights, OSM_WEIGHT_PREFERENCE_MATCH);

            // Proxy quality signals
            if (p.getName() != null && !p.getName().equals(
                    p.getEntityType() != null ? p.getEntityType().displayName : "")) {
                score += OSM_WEIGHT_HAS_NAME;  // has a real name, not just fallback
            }
            if (p.getWikipedia() != null)    score += OSM_WEIGHT_WIKIPEDIA;
            if (p.getWebsite() != null)      score += OSM_WEIGHT_WEBSITE;
            if (p.getDescription() != null)  score += OSM_WEIGHT_HAS_DESCRIPTION;

            String access = p.getAccess();
            if (access == null || "yes".equals(access) || "permissive".equals(access)) {
                score += OSM_WEIGHT_PUBLIC_ACCESS;  // null = assumed public
            }

            // Elevation bonus for peaks
            if (p.getEntityType() == OsmEntityType.PEAK && p.getElevation() != null) {
                double elevFraction = Math.min(p.getElevation() / PEAK_ELEVATION_NOTABLE, 1.0);
                score += elevFraction * OSM_WEIGHT_ELEVATION_MAX;
            }

            // Detour penalty (same formula as Google)
            score -= detourPenalty(p.getDetourMinutes());

            score = Math.max(0, Math.min(100, score));
            result.add(ScoredWaypoint.fromOsm(p, score));
        }
        return result;
    }

    // ── Interleaving ──────────────────────────────────────────────────────────

    /**
     * Interleave OSM and Google results by OSM_BIAS.
     *
     * OSM_BIAS = 0.6 means ~60% of slots go to OSM, 40% to Google.
     * We alternate picking from each list proportionally rather than
     * strict alternation, so the OSM advantage is maintained throughout
     * without creating obvious "OSM then Google" blocks.
     */
    private List<ScoredWaypoint> interleave(
            List<ScoredWaypoint> osm,
            List<ScoredWaypoint> google,
            int maxResults) {

        List<ScoredWaypoint> result = new ArrayList<>();
        int oi = 0, gi = 0;
        double osmCredit    = 0.0;
        double googleCredit = 0.0;

        while (result.size() < maxResults && (oi < osm.size() || gi < google.size())) {
            osmCredit    += OSM_BIAS;
            googleCredit += (1.0 - OSM_BIAS);

            // Pick OSM if it has credit and items remain
            while (osmCredit >= 1.0 && oi < osm.size() && result.size() < maxResults) {
                result.add(osm.get(oi++));
                osmCredit -= 1.0;
            }
            // Pick Google if it has credit and items remain
            while (googleCredit >= 1.0 && gi < google.size() && result.size() < maxResults) {
                result.add(google.get(gi++));
                googleCredit -= 1.0;
            }

            // Drain remaining from whichever list still has items
            if (oi >= osm.size() && gi < google.size()) {
                while (gi < google.size() && result.size() < maxResults)
                    result.add(google.get(gi++));
            }
            if (gi >= google.size() && oi < osm.size()) {
                while (oi < osm.size() && result.size() < maxResults)
                    result.add(osm.get(oi++));
            }
        }

        return result;
    }

    // ── Shared Utilities ──────────────────────────────────────────────────────

    /**
     * Detour penalty: linear from 0 at 0 minutes to maxPenalty at MAX_DETOUR_MINUTES.
     * Identical formula for both sources.
     */
    private double detourPenalty(Double detourMinutes) {
        if (detourMinutes == null || detourMinutes <= 0) return 0;
        double capped = Math.min(detourMinutes, MAX_DETOUR_MINUTES);
        return (capped / MAX_DETOUR_MINUTES) * (DETOUR_PENALTY_PER_MINUTE * MAX_DETOUR_MINUTES);
    }

    /**
     * Preference bonus: full weight for rank-1 preference, tapering to 20% for rank 10+.
     * entityTypeId can be a Google Places type string or an OsmEntityType.name().
     */
    private double prefBonus(String entityTypeId, Map<String, Double> prefWeights, double maxBonus) {
        if (entityTypeId == null || prefWeights.isEmpty()) return 0;
        Double weight = prefWeights.get(entityTypeId.toLowerCase());
        if (weight == null) return 0;
        return weight * maxBonus;
    }

    /**
     * Convert ordered preference list to a weight map.
     * Rank 1 → 1.0, rank 2 → 0.85, rank 3 → 0.72, tapering logarithmically.
     */
    private Map<String, Double> buildPreferenceWeights(List<String> preferences) {
        Map<String, Double> weights = new HashMap<>();
        if (preferences == null || preferences.isEmpty()) return weights;
        for (int i = 0; i < preferences.size(); i++) {
            double weight = 1.0 / (1.0 + 0.3 * i);  // 1.0, 0.77, 0.63, 0.53 ...
            weights.put(preferences.get(i).toLowerCase(), weight);
        }
        return weights;
    }
}