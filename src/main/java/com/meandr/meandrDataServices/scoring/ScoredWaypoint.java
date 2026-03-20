package com.meandr.meandrDataServices.scoring;

import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified waypoint representation after scoring.
 * Wraps either a Google Places or OSM result with a normalized 0–100 score
 * and the metadata the frontend needs to display it.
 */

@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@NoArgsConstructor  // Generates empty constructor for Jackson
@Slf4j
public class ScoredWaypoint {

    public enum Source { GOOGLE, OSM }

    private Source source;
    private double score;

    // Identity
    private String id;           // placeId for Google, osmId string for OSM
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;

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

    // ── Factory methods ───────────────────────────────────────────────────────

    public static ScoredWaypoint fromGoogle(GooglePlaceCandidate c, double score) {
        ScoredWaypoint w = new ScoredWaypoint();
        w.source          = Source.GOOGLE;
        w.score           = score;
        w.id              = c.getPlaceId();
        w.name            = c.getName();
        w.latitude        = c.getLatitude();
        w.longitude       = c.getLongitude();
        w.address         = c.getAddress();
        w.entityType      = c.getEntityType();
        log.info("fromGoogle: {} entityType={} candidate.entityType={}", 
        c.getName(), w.entityType, c.getEntityType());
        w.displayName     = formatGoogleType(c.getEntityType());
        w.emoji           = googleEmoji(c.getEntityType());
        w.rating          = c.getRating();
        w.userRatingCount = c.getUserRatingCount();
        w.detourMinutes   = c.getDetourMinutes();
        w.distFromStart   = c.getDistFromStart();
        return w;
    }

    public static ScoredWaypoint fromOsm(OsmPlace p, double score) {
        ScoredWaypoint w  = new ScoredWaypoint();
        w.source          = Source.OSM;
        w.score           = score;
        w.id              = p.getOsmId() != null ? "osm_" + p.getOsmId() : null;
        w.name            = p.getName();
        w.latitude        = p.getLatitude();
        w.longitude       = p.getLongitude();
        w.entityType      = p.getEntityType() != null ? p.getEntityType().name() : null;
        w.displayName     = p.getEntityType() != null ? p.getEntityType().displayName : "Point of Interest";
        w.emoji           = p.getEntityType() != null ? p.getEntityType().emoji : "📍";
        w.wikipedia       = p.getWikipedia();
        w.website         = p.getWebsite();
        w.elevation       = p.getElevation();
        w.access          = p.getAccess();
        w.difficulty      = p.getDifficulty();
        w.detourMinutes   = p.getDetourMinutes();
        w.distFromStart   = p.getDistanceFromRouteMiles();
        return w;
    }

    // ── Google type helpers ───────────────────────────────────────────────────

    private static String formatGoogleType(String type) {
        if (type == null) return "Place";
        return type.replace("_", " ")
                .substring(0, 1).toUpperCase()
                + type.replace("_", " ").substring(1);
    }

    private static String googleEmoji(String type) {
        if (type == null) return "📍";
        return switch (type) {
            case "restaurant", "food", "meal_takeaway" -> "🍽️";
            case "cafe"                                -> "☕";
            case "bar", "night_club"                  -> "🍺";
            case "bakery"                              -> "🥐";
            case "museum"                              -> "🏛️";
            case "art_gallery"                         -> "🎨";
            case "park"                                -> "🌳";
            case "campground"                          -> "⛺";
            case "tourist_attraction"                  -> "✨";
            case "natural_feature"                     -> "🌿";
            case "zoo"                                 -> "🦁";
            case "aquarium"                            -> "🐠";
            case "amusement_park"                      -> "🎡";
            case "lodging"                             -> "🏨";
            case "gas_station"                         -> "⛽";
            case "library"                             -> "📚";
            case "movie_theater"                       -> "🎬";
            default                                    -> "📍";
        };
    }

    
}