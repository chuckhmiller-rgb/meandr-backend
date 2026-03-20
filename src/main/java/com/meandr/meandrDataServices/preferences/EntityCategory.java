package com.meandr.meandrDataServices.preferences;

import java.util.Arrays;
import java.util.List;

/**
 * The 10 preference categories shown in the UI.
 * Each category owns a set of entity type IDs — either Google Places type strings
 * or OsmEntityType.name() values. The frontend uses this to group types into
 * collapsible accordions.
 */
public enum EntityCategory {

    SCENIC_VIEWS(
        "Scenic & Views", "🌄",
        List.of(
            // Google
            "tourist_attraction", "natural_feature",
            // OSM
            "SCENIC_OVERLOOK", "PEAK", "COVERED_BRIDGE", "SCENIC_BYWAY"
        )
    ),

    WATER_FEATURES(
        "Water Features", "💧",
        List.of(
            // OSM only
            "WATERFALL", "HOT_SPRING", "SWIMMING_HOLE", "RIVER_ACCESS", "SPRING"
        )
    ),

    TRAILS_OUTDOORS(
        "Trails & Outdoors", "🥾",
        List.of(
            // Google
            "park", "campground", "rv_park",
            // OSM
            "TRAILHEAD", "PICNIC_SITE", "WILDERNESS_HUT", "NATURE_RESERVE"
        )
    ),

    CLIMBING_CAVING(
        "Climbing & Caving", "🧗",
        List.of(
            // OSM only
            "CLIMBING_CRAG", "BOULDER", "CAVE"
        )
    ),

    FISHING_WILDLIFE(
        "Fishing & Wildlife", "🎣",
        List.of(
            // OSM only
            "RIVER_ACCESS", "BIRD_HIDE", "DARK_SKY_AREA", "OBSERVATORY"
        )
    ),

    FOOD_DRINK(
        "Food & Drink", "🍽️",
        List.of(
            // Google only
            "restaurant", "cafe", "bar", "bakery", "meal_takeaway", "night_club"
        )
    ),

    HISTORY_CULTURE(
        "History & Culture", "🏛️",
        List.of(
            // Google
            "museum", "art_gallery", "library", "performing_arts_theater",
            // OSM
            "ARCHAEOLOGICAL_SITE", "RUINS", "BATTLEFIELD", "GHOST_TOWN", "ARTWORK"
        )
    ),

    QUIRKY_OFFBEAT(
        "Quirky & Offbeat", "✨",
        List.of(
            // OSM
            "ATTRACTION", "MILESTONE", "WAYSIDE_SHRINE"
        )
    ),

    NATURE_SCIENCE(
        "Nature & Science", "🌿",
        List.of(
            // Google
            "zoo", "aquarium",
            // OSM
            "NATURE_RESERVE", "OBSERVATORY", "CAVE"
        )
    ),

    REST_REFUEL(
        "Rest & Refuel", "⛽",
        List.of(
            // Google only
            "gas_station", "lodging", "restaurant", "convenience_store"
        )
    );

    public final String displayName;
    public final String emoji;
    public final List<String> entityTypeIds;

    EntityCategory(String displayName, String emoji, List<String> entityTypeIds) {
        this.displayName   = displayName;
        this.emoji         = emoji;
        this.entityTypeIds = entityTypeIds;
    }

    /**
     * Find the category that owns a given entity type ID.
     * Returns null if the type isn't assigned to any category.
     */
    public static EntityCategory forEntityType(String entityTypeId) {
        if (entityTypeId == null) return null;
        for (EntityCategory cat : values()) {
            if (cat.entityTypeIds.contains(entityTypeId) ||
                cat.entityTypeIds.contains(entityTypeId.toLowerCase())) {
                return cat;
            }
        }
        return null;
    }
}