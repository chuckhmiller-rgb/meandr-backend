package com.meandr.meandrDataServices.scoring;

import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified waypoint representation after scoring. Wraps either a Google Places
 * or OSM result with a normalized 0–100 score and the metadata the frontend
 * needs to display it.
 */
@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@NoArgsConstructor  // Generates empty constructor for Jackson
@Slf4j
public class ScoredWaypoint {

    public enum Source {
        GOOGLE, OSM
    }

    private Source source;
    private double score;

    // Identity
    private String id;           // placeId for Google, osmId string for OSM
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String openingHoursJson;

    // Display metadata
    private String entityType;   // Google type string or OsmEntityType.name()
    private String displayName;  // Human-readable type label
    private String emoji;        // Category emoji

    // Quality signals (nullable for OSM)
    private Double rating;
    private Integer userRatingCount;

    // OSM-specific enrichment
    private String wikipedia;
    private String website;
    private Double elevation;
    private String access;
    private String difficulty;

    // Route context (set by RouteBeautifierService)
    private Double detourMinutes;
    private Double distFromStart;
    /**
     * Tracks how and when this place was found and selected during route
     * beautification.
     *
     * SELECTION STRATEGY — four-pass priority hierarchy: Pass 0: Find best KW
     * anchor per segment (rating >= 4.5, reviews >= 100), then cluster KW → NB
     * → PO companions within 10km. Pass 1: For segments still empty after Pass
     * 0, find best NB anchor (rating >= 4.5, reviews >= 1000), then cluster KW
     * → NB → PO companions. Pass 2: For segments still empty after Pass 1, find
     * best PO anchor (rating >= 4.5, reviews >= 1000), then cluster KW → NB →
     * PO companions. Pass 3: Diffuse remaining budget — exhaust KW spots first
     * across all segments, then NB, then PO.
     *
     * searchSource — always populated, indicates which search method found this
     * place: "NB" = found via searchNearby (standard radius) "KW" = found via
     * searchText keyword (standard radius) "NB-WR" = found via searchNearby
     * wide radius retry "KW-WR" = found via searchText keyword wide radius
     * retry "NB-DEST" = found via searchNearby destination wide search
     *
     * selectionDebugCode — only populated when DebugConfig.SHOW_SELECTION_DEBUG
     * = true. Combines selection pass and search source, e.g.: "P0/KW" = Pass 0
     * KW anchor "P0c/NB" = Pass 0 companion, found via nearby search "P1/NB" =
     * Pass 1 NB anchor "P1c/KW" = Pass 1 companion, found via keyword search
     * "P2/NB-WR" = Pass 2 PO anchor, found via wide radius nearby "P3/KW" =
     * Pass 3 diffusion, found via keyword search
     */
    private String selectionDebugCode;
    private String searchSource;

    // ── Factory methods ───────────────────────────────────────────────────────
    public static ScoredWaypoint fromGoogle(GooglePlaceCandidate c, double score) {
        ScoredWaypoint w = new ScoredWaypoint();
        w.source = Source.GOOGLE;
        w.score = score;
        w.id = c.getPlaceId();
        w.name = c.getName();
        w.latitude = c.getLatitude();
        w.longitude = c.getLongitude();
        w.address = c.getAddress();
        w.openingHoursJson = c.getOpeningHoursJson();
        w.entityType = c.getEntityType();
        //log.info("fromGoogle: {} entityType={} candidate.entityType={}", 
        //c.getName(), w.entityType, c.getEntityType());
        w.displayName = formatGoogleType(c.getEntityType());
        w.emoji = googleEmoji(c.getEntityType());
        w.rating = c.getRating();
        w.userRatingCount = c.getUserRatingCount();
        w.detourMinutes = c.getDetourMinutes();
        w.distFromStart = c.getDistFromStart();
        w.selectionDebugCode = c.getSelectionDebugCode();
        w.searchSource = c.getSearchSource();
        return w;
    }

    public static ScoredWaypoint fromOsm(OsmPlace p, double score) {
        ScoredWaypoint w = new ScoredWaypoint();
        w.source = Source.OSM;
        w.score = score;
        w.id = p.getOsmId() != null ? "osm_" + p.getOsmId() : null;
        w.name = p.getName();
        w.latitude = p.getLatitude();
        w.longitude = p.getLongitude();
        w.entityType = p.getEntityType() != null ? p.getEntityType().name() : null;
        w.displayName = p.getEntityType() != null ? p.getEntityType().displayName : "Point of Interest";
        w.emoji = p.getEntityType() != null ? p.getEntityType().emoji : "📍";
        w.wikipedia = p.getWikipedia();
        w.website = p.getWebsite();
        w.elevation = p.getElevation();
        w.access = p.getAccess();
        w.difficulty = p.getDifficulty();
        w.detourMinutes = p.getDetourMinutes();
        w.distFromStart = p.getDistanceFromRouteMiles();
        return w;
    }

    // ── Google type helpers ───────────────────────────────────────────────────
    private static String formatGoogleType(String type) {
        if (type == null) {
            return "Place";
        }
        return type.replace("_", " ")
                .substring(0, 1).toUpperCase()
                + type.replace("_", " ").substring(1);
    }

    private static String googleEmoji(String type) {
        if (type == null) {
            return "📍";
        }
        return switch (type) {
            case "restaurant", "food", "meal_takeaway" ->
                "🍽️";
            case "cafe" ->
                "☕";
            case "bar", "night_club" ->
                "🍺";
            case "bakery" ->
                "🥐";
            case "museum" ->
                "🏛️";
            case "art_gallery" ->
                "🎨";
            case "park" ->
                "🌳";
            case "campground" ->
                "⛺";
            case "tourist_attraction" ->
                "✨";
            case "natural_feature" ->
                "🌿";
            case "zoo" ->
                "🦁";
            case "aquarium" ->
                "🐠";
            case "amusement_park" ->
                "🎡";
            case "lodging" ->
                "🏨";
            case "gas_station" ->
                "⛽";
            case "library" ->
                "📚";
            case "movie_theater" ->
                "🎬";
            default ->
                "📍";
        };
    }

}
