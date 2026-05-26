package com.meandr.meandrDataServices.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps internal Meandr entity type strings to valid Google Places API v1 types.
 * Updated for February 2026 Google Places API release.
 *
 * All keys are uppercase — toGoogleTypes normalizes input to uppercase before lookup.
 *
 * Strategy:
 * - Use specific Google types where they exist (state_park, beach, marina etc.)
 * - For broad/noisy types (natural_feature), rely on keyword search instead
 * - Multiple types in list = searchNearby returns places matching ANY of them
 */
public class GooglePlacesTypeMapper {

    private static final Map<String, List<String>> TYPE_MAP = Map.ofEntries(
            // ── Scenic & Views ──────────────────────────────────────────────
            Map.entry("WATERFALL",               List.of("tourist_attraction")),
            Map.entry("SCENIC_OVERLOOK",         List.of("observation_deck", "tourist_attraction")),
            Map.entry("SCENIC_BYWAY",            List.of("tourist_attraction")),
            Map.entry("PEAK",                    List.of("tourist_attraction")),
            Map.entry("COVERED_BRIDGE",          List.of("tourist_attraction", "historical_landmark")),

            // ── Water Features ───────────────────────────────────────────────
            Map.entry("HOT_SPRING",              List.of("tourist_attraction")),
            Map.entry("SWIMMING_HOLE",           List.of("tourist_attraction")),
            Map.entry("RIVER_ACCESS",            List.of("park", "tourist_attraction")),
            Map.entry("SPRING",                  List.of("tourist_attraction")),
            Map.entry("BEACH",                   List.of("beach")),
            Map.entry("MARINA",                  List.of("marina")),

            // ── Trails & Outdoors ────────────────────────────────────────────
            Map.entry("PARK",                    List.of("park", "city_park")),
            Map.entry("STATE_PARK",              List.of("state_park")),
            Map.entry("NATIONAL_PARK",           List.of("national_park")),
            Map.entry("NATURE_RESERVE",          List.of("wildlife_refuge", "national_park", "park")),
            Map.entry("TRAILHEAD",               List.of("hiking_area", "park")),
            Map.entry("PICNIC_SITE",             List.of("picnic_ground", "park")),
            Map.entry("CAMPGROUND",              List.of("campground")),
            Map.entry("RV_PARK",                 List.of("rv_park")),
            Map.entry("WILDERNESS_HUT",          List.of("tourist_attraction")),
            Map.entry("DOG_PARK",                List.of("dog_park")),
            Map.entry("BOTANICAL_GARDEN",        List.of("botanical_garden")),
            Map.entry("WILDLIFE_AREA",           List.of("wildlife_refuge", "wildlife_park", "national_park")),
            Map.entry("OFF_ROADING",             List.of("off_roading_area")),
            Map.entry("HIKING_AREA",             List.of("hiking_area")),

            // ── Climbing & Caving ────────────────────────────────────────────
            Map.entry("CLIMBING_CRAG",           List.of("tourist_attraction", "adventure_sports_center")),
            Map.entry("BOULDER",                 List.of("tourist_attraction")),
            Map.entry("CAVE",                    List.of("tourist_attraction")),

            // ── History & Culture ────────────────────────────────────────────
            Map.entry("MUSEUM",                  List.of("museum", "history_museum", "art_museum")),
            Map.entry("ART_GALLERY",             List.of("art_gallery", "art_museum")),
            Map.entry("LIBRARY",                 List.of("library")),
            Map.entry("PERFORMING_ARTS_THEATER", List.of("performing_arts_theater")),
            Map.entry("RUINS",                   List.of("historical_landmark", "historical_place")),
            Map.entry("BATTLEFIELD",             List.of("historical_landmark", "historical_place", "tourist_attraction")),
            Map.entry("HISTORIC_SITE",           List.of("historical_landmark", "cultural_landmark", "historical_place")),
            Map.entry("ARCHAEOLOGICAL_SITE",     List.of("historical_landmark", "tourist_attraction")),
            Map.entry("GHOST_TOWN",              List.of("historical_landmark", "tourist_attraction")),
            Map.entry("ARTWORK",                 List.of("sculpture", "tourist_attraction")),
            Map.entry("MONUMENT",                List.of("monument", "historical_landmark")),
            Map.entry("CASTLE",                  List.of("castle", "historical_landmark")),
            Map.entry("HISTORIC_DISTRICT",       List.of("historical_landmark", "tourist_attraction")),
            Map.entry("ATTRACTION",              List.of("tourist_attraction")),
            Map.entry("MILESTONE",               List.of("historical_landmark", "tourist_attraction")),
            Map.entry("WAYSIDE_SHRINE",          List.of("tourist_attraction")),

            // ── Food & Drink ─────────────────────────────────────────────────
            Map.entry("RESTAURANT",              List.of("restaurant")),
            Map.entry("CAFE",                    List.of("cafe", "coffee_shop")),
            Map.entry("BAR",                     List.of("bar", "cocktail_bar")),
            Map.entry("BAKERY",                  List.of("bakery")),
            Map.entry("MEAL_TAKEAWAY",           List.of("fast_food_restaurant")),
            Map.entry("NIGHT_CLUB",              List.of("night_club")),
            Map.entry("BREWERY",                 List.of("brewery", "brewpub")),
            Map.entry("WINERY",                  List.of("vineyard")),
            Map.entry("DISTILLERY",              List.of("tourist_attraction")),
            Map.entry("FARMERS_MARKET",          List.of("market")),
            Map.entry("BEER_GARDEN",             List.of("beer_garden")),

            // ── Nature & Science ─────────────────────────────────────────────
            Map.entry("ZOO",                     List.of("zoo")),
            Map.entry("AQUARIUM",                List.of("aquarium")),
            Map.entry("OBSERVATORY",             List.of("tourist_attraction")),
            Map.entry("DARK_SKY_AREA",           List.of("tourist_attraction")),
            Map.entry("BIRD_HIDE",               List.of("wildlife_refuge", "park")),

            // ── Entertainment ────────────────────────────────────────────────
            Map.entry("AMUSEMENT_PARK",          List.of("amusement_park")),
            Map.entry("ROADSIDE_ATTRACTION",     List.of("tourist_attraction")),
            Map.entry("TOURIST_ATTRACTION",      List.of("tourist_attraction")),
            Map.entry("NATURAL_FEATURE",         List.of("tourist_attraction")),
            Map.entry("FISHING_SPOT",            List.of("park", "tourist_attraction")),

            // ── Rest & Refuel ────────────────────────────────────────────────
            Map.entry("GAS_STATION",             List.of("gas_station")),
            Map.entry("LODGING",                 List.of("lodging")),
            Map.entry("CONVENIENCE_STORE",       List.of("convenience_store")),
            Map.entry("REST_AREA",               List.of("rest_stop")),

            // ── Worship ──────────────────────────────────────────────────────
            Map.entry("CHURCH",                  List.of("church")),
            Map.entry("SYNAGOGUE",               List.of("synagogue")),
            Map.entry("MOSQUE",                  List.of("mosque")),
            Map.entry("HINDU_TEMPLE",            List.of("hindu_temple")),

            // ── Civic & Education ────────────────────────────────────────────
            Map.entry("UNIVERSITY",              List.of("university")),
            Map.entry("COURTHOUSE",              List.of("courthouse")),
            Map.entry("CITY_HALL",               List.of("city_hall")),
            Map.entry("TOWN_SQUARE",             List.of("plaza", "town_square")),
            Map.entry("STADIUM",                 List.of("stadium")),
            Map.entry("SPORTS_COMPLEX",          List.of("sports_complex"))
    );

    public static List<String> toGoogleTypes(List<String> internalTypes) {
        return internalTypes.stream()
                .filter(t -> TYPE_MAP.containsKey(t.toUpperCase()))
                .flatMap(t -> TYPE_MAP.get(t.toUpperCase()).stream())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    public static boolean isMappable(String internalType) {
        return TYPE_MAP.containsKey(internalType.toUpperCase());
    }

    public static Set<String> knownTypes() {
        return TYPE_MAP.keySet();
    }
}