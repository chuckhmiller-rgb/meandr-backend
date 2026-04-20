package com.meandr.meandrDataServices.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps internal Meandr entity type strings to valid Google Places API v1 types.
 * Types not in this map are OSM-only and should be excluded from Google Places
 * calls.
 *
 * Valid Google Places types:
 * https://developers.google.com/maps/documentation/places/web-service/place-types
 */
public class GooglePlacesTypeMapper {

    // Maps internal type → one or more valid Google Places API types
    private static final Map<String, List<String>> TYPE_MAP = Map.ofEntries(
            // Scenic / Outdoors
            Map.entry("DOG_PARK", List.of("dog_park")),
            Map.entry("WATERFALL", List.of("tourist_attraction")),
            Map.entry("SCENIC_OVERLOOK", List.of("tourist_attraction")),
            Map.entry("SCENIC_BYWAY", List.of("tourist_attraction")),
            Map.entry("NATURE_RESERVE", List.of("national_park", "park")),
            Map.entry("PEAK", List.of("tourist_attraction")),
            Map.entry("BOULDER", List.of("tourist_attraction")),
            Map.entry("TRAILHEAD", List.of("park", "hiking_area")),
            Map.entry("CLIMBING_CRAG", List.of("tourist_attraction")),
            Map.entry("BIRD_HIDE", List.of("park")),
            Map.entry("PARK", List.of("park", "national_park", "hiking_area")),
            Map.entry("MONUMENT", List.of("monument", "historical_landmark", "national_park", "tourist_attraction")),
            // History / Culture
            Map.entry("RUINS", List.of("tourist_attraction", "historical_landmark")),
            Map.entry("COVERED_BRIDGE", List.of("tourist_attraction")),
            Map.entry("BATTLEFIELD", List.of("historical_place", "historical_landmark", "tourist_attraction")),
            Map.entry("HISTORIC_SITE", List.of("historical_landmark", "tourist_attraction")),
            Map.entry("MUSEUM", List.of("museum")),
            Map.entry("ART_GALLERY", List.of("art_gallery")),
            // Food / Drink
            Map.entry("RESTAURANT", List.of("restaurant")),
            Map.entry("CAFE", List.of("cafe")),
            Map.entry("BREWERY", List.of("bar")),
            Map.entry("WINERY", List.of("tourist_attraction")),
            Map.entry("DISTILLERY", List.of("tourist_attraction")),
            Map.entry("FARMERS_MARKET", List.of("market")),
            // Rest / Refuel
            Map.entry("GAS_STATION", List.of("gas_station")),
            Map.entry("REST_AREA", List.of("rest_stop")),
            Map.entry("CAMPGROUND", List.of("campground")),
            Map.entry("LODGING", List.of("lodging")),
            // Nature / Science
            Map.entry("BOTANICAL_GARDEN", List.of("botanical_garden")),
            Map.entry("ZOO", List.of("zoo")),
            Map.entry("AQUARIUM", List.of("aquarium")),
            Map.entry("OBSERVATORY", List.of("tourist_attraction")),
            // Quirky / Offbeat
            Map.entry("ROADSIDE_ATTRACTION", List.of("tourist_attraction")),
            Map.entry("AMUSEMENT_PARK", List.of("amusement_park")),
            // Fishing / Wildlife
            Map.entry("FISHING_SPOT", List.of("park", "tourist_attraction")),
            Map.entry("WILDLIFE_AREA", List.of("national_park", "park")),
            Map.entry("ARTWORK", List.of("tourist_attraction")),
            Map.entry("ATTRACTION", List.of("tourist_attraction")),
            Map.entry("CAVE", List.of("tourist_attraction")),
            Map.entry("DARK_SKY_AREA", List.of("tourist_attraction")),
            Map.entry("RIVER_ACCESS", List.of("park", "tourist_attraction")),
            Map.entry("WAYSIDE_SHRINE", List.of("tourist_attraction")),
            Map.entry("RV_PARK", List.of("rv_park")),
            Map.entry("PICNIC_SITE", List.of("park")),
            Map.entry("WILDERNESS_HUT", List.of("tourist_attraction")),
            // Worship
            Map.entry("church", List.of("church")),
            Map.entry("CHURCH", List.of("church")),
            Map.entry("synagogue", List.of("synagogue")),
            Map.entry("SYNAGOGUE", List.of("synagogue")),
            Map.entry("mosque", List.of("mosque")),
            Map.entry("MOSQUE", List.of("mosque")),
            Map.entry("hindu_temple", List.of("hindu_temple")),
            Map.entry("HINDU_TEMPLE", List.of("hindu_temple")),
            // Civic & Education
            Map.entry("university", List.of("university")),
            Map.entry("UNIVERSITY", List.of("university")),
            Map.entry("courthouse", List.of("courthouse")),
            Map.entry("COURTHOUSE", List.of("courthouse")),
            Map.entry("city_hall", List.of("city_hall")),
            Map.entry("CITY_HALL", List.of("city_hall")),
            Map.entry("town_square", List.of("town_square")),
            Map.entry("TOWN_SQUARE", List.of("town_square")),
            Map.entry("stadium", List.of("stadium")),
            Map.entry("STADIUM", List.of("stadium")),
            Map.entry("sports_complex", List.of("sports_complex")),
            Map.entry("SPORTS_COMPLEX", List.of("sports_complex"))
    );

    /**
     * Converts a list of internal Meandr type strings to valid Google Places
     * API types. OSM-only types with no mapping are silently dropped.
     * Deduplicates the result.
     */
    public static List<String> toGoogleTypes(List<String> internalTypes) {
        return internalTypes.stream()
                .filter(TYPE_MAP::containsKey)
                .flatMap(t -> TYPE_MAP.get(t).stream())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the type has a valid Google Places mapping.
     */
    public static boolean isMappable(String internalType) {
        return TYPE_MAP.containsKey(internalType);
    }

    /**
     * Returns the set of all known internal types.
     */
    public static Set<String> knownTypes() {
        return TYPE_MAP.keySet();
    }
}
