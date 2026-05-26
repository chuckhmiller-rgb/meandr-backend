/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.meandr.meandrDataServices.dto.CoordinateDto;
import com.meandr.meandrDataServices.model.ScenicSpot;
import com.meandr.meandrDataServices.service.PlacesCacheService;
import com.meandr.meandrDataServices.util.GooglePlacesTypeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClientException;

import org.springframework.cache.annotation.Cacheable;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author chuck
 */
@CrossOrigin(origins = "https://meandr-app.vercel.app")
@RestController
@RequestMapping("/api/places")
@Slf4j
public class GoogleApiProxyController {

    @Value("${google.api.key}")
    private String apiKey;

    private static final Set<String> EXCLUDED_PLACE_TYPES = Set.of(
            "veterinary_care", "hospital", "doctor",
            "dentist", "pharmacy", "lawyer", "real_estate_agency",
            "insurance_agency", "bank", "atm", "police", "funeral_home",
            "physiotherapist", "accounting", "storage", "moving_company",
            "car_dealer", "car_repair", "car_wash", "laundry", "post_office", "restaurant"
    );

    private static final List<String> TYPE_PRIORITY = List.of(
            "dog_park", "botanical_garden", "zoo", "aquarium", "campground",
            "amusement_park", "art_gallery",
            "restaurant", "fast_food_restaurant", "cafe", "bar", "bakery", "night_club",
            "museum", "historical_landmark",
            "national_park", "hiking_area", "tourist_attraction",
            "church", "synagogue", "mosque", "hindu_temple",
            "university", "stadium", "courthouse", "city_hall",
            "lodging", "gas_station", "park"
    );

    public static final Map<String, String> ENTITY_KEYWORDS = Map.ofEntries(
            Map.entry("WATERFALL", "waterfall falls cascade"),
            Map.entry("CLIMBING_CRAG", "climbing crag route"),
            Map.entry("BOULDER", "bouldering boulder climbing route"),
            Map.entry("HOT_SPRING", "hotspring thermal spring"),
            Map.entry("BIRD_HIDE", "bird hide birding wildlife blind"),
            Map.entry("SWIMMING_HOLE", "swimming hole swimming creek"),
            Map.entry("CAVE", "cave cavern grotto"),
            Map.entry("DARK_SKY_AREA", "dark sky stargazing darksky"),
            Map.entry("SCENIC_OVERLOOK", "overlook scenic viewpoint vista"),
            Map.entry("PEAK", "mountain peak summit"),
            Map.entry("RIVER_ACCESS", "river access boat launch canoe kayak"),
            Map.entry("GHOST_TOWN", "ghost town abandoned historic ruins"),
            Map.entry("ARCHAEOLOGICAL_SITE", "archaeological site ruins excavation"),
            Map.entry("TRAILHEAD", "trailhead trail access hiking"),
            Map.entry("STATE_PARK", "state park"),
            Map.entry("DOG_PARK", "dog park off leash"),
            Map.entry("OBSERVATORY", "observatory telescope stargazing")
    );

    @Autowired
    private PlacesCacheService placesCacheService;

    private String searchTextUrl = "https://places.googleapis.com/v1/places:searchText";
    private String placesNearbyUrl = "https://places.googleapis.com/v1/places:searchNearby";

    private final RestTemplate restTemplate = new RestTemplate();

    private String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json";

    @Autowired
    private CacheManager cacheManager;

    public String cacheKey(double lat, double lng, double radius, List<String> entityTypes) {
        double gridLat = Math.round(lat / 0.05) * 0.05;
        double gridLng = Math.round(lng / 0.05) * 0.05;
        String types = entityTypes.stream().sorted().collect(Collectors.joining(","));
        return String.format("%.2f|%.2f|%.0f|%s", gridLat, gridLng, radius, types);
    }

    @Operation(summary = "Geocode a place name or address to lat/lng coordinates")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                            name = "Geocode Example",
                            value = """
                        {
                          "address": "Asheville, NC"
                        }"""
                    )
            )
    )
    @PostMapping("/geocode")
    public ResponseEntity<CoordinateDto> geocode(@RequestBody Map<String, String> requestBody) {
        String address = requestBody.get("address");
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String url = geocodeUrl + "?address="
                + java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8)
                + "&key=" + apiKey;

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode location = response.get("results").get(0)
                        .path("geometry")
                        .path("location");

                double lat = location.path("lat").asDouble();
                double lng = location.path("lng").asDouble();

                log.info("Geocoded '{}' to {},{}", address, lat, lng);
                return ResponseEntity.ok(new CoordinateDto(lat, lng));
            }

            log.warn("No geocode results for address: {}", address);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Geocoding error for '{}': {}", address, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("scenicSpots");
        var stats = cache.getNativeCache().stats();
        return ResponseEntity.ok(Map.of(
                "hitRate", stats.hitRate(),
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "size", cache.getNativeCache().estimatedSize()
        ));
    }

    @Operation(summary = "searchText for places")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                            name = "Complex Search Example",
                            value = """
                                    {
                                      "textQuery": "fishing hole",
                                      "includedType": "park",
                                      "locationBias": {
                                        "circle": {
                                          "center": { "latitude": 40.7128, "longitude": -74.0060 },
                                          "radius": 5000.0
                                        }
                                      }
                                    }"""
                    )
            )
    )

    @Retryable(
            retryFor = {org.springframework.web.client.HttpStatusCodeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // This is your "Thread.sleep(2000)"
    )
    @PostMapping("/searchText")
    public ResponseEntity<String> searchText(@RequestBody Map<String, Object> requestBody) {
        // 1. Use the NEW Nearby Search endpoint

        // 2. Set up the Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        // The FieldMask is REQUIRED. This defines what data you get back.
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.types,places.location,places.rating,places.userRatingCount");

        // 3. Prepare the Request Entity (Body + Headers)
        // Note: We pass the requestBody directly because it already contains your 
        // includedTypes, excludedTypes, and locationRestriction.
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Use exchange() to perform a POST request
            ResponseEntity<String> response = restTemplate.exchange(
                    searchTextUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            log.error("Error calling Google Places API: " + e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    public List<ScenicSpot> searchTextScenic(double lat, double lng, int radius, String keyword, List<String> googleTypes) { // --- Tier 1: MySQL persistent cache ---
    
        // --- Tier 1: MySQL persistent cache ---
        List<ScenicSpot> cached = placesCacheService.findNearby(lat, lng, radius, googleTypes);
        if (!cached.isEmpty()) {
            log.info("MySQL cache hit (keyword): {} spots near ({},{})", cached.size(), lat, lng);
            return cached;
        }
        log.info("MySQL cache miss — calling Google Places searchText near ({},{}) keyword={}", lat, lng, keyword);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("textQuery", keyword);
        requestBody.put("maxResultCount", 20);
        if (googleTypes != null && !googleTypes.isEmpty()) {
            requestBody.put("includedType", googleTypes.get(0)); // searchText only allows one type
        }
        requestBody.put("locationBias", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", (double) radius
                )
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress,places.types,"
                + "places.location,places.rating,places.userRatingCount,places.regularOpeningHours");

        try {
            JsonNode response = restTemplate.postForObject(
                    "https://places.googleapis.com/v1/places:searchText",
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class
            );
            if (response == null || !response.has("places")) {
                return Collections.emptyList();
            }
            List<ScenicSpot> spots = new ArrayList<>();
            for (JsonNode node : response.get("places")) {
                ScenicSpot spot = mapToScenicSpot(node);
                if (spot != null) {
                    spot.setName(spot.getName());
                    spots.add(spot);
                }
            }

            // --- Backfill MySQL cache ---
            if (!spots.isEmpty()) {
                placesCacheService.saveAll(spots);
                log.info("Backfilled {} keyword spots to MySQL cache", spots.size());
            }
            return spots;
        } catch (Exception e) {
            log.error("Google searchText error near ({},{}): {}", lat, lng, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Operation(summary = "searchNearby for places")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                            name = "Complex Search Example",
                            value = """
                                    {
                                      "includedTypes": ["park", "tourist_attraction", "museum"],
                                      "maxResultCount": 10,
                                      "locationRestriction": {
                                        "circle": {
                                          "center": {
                                            "latitude": 34.0522,
                                            "longitude": -118.2437
                                          },
                                          "radius": 5000.0
                                        }
                                      }
                                    }"""
                    )
            )
    )

    @Retryable(
            retryFor = {org.springframework.web.client.HttpStatusCodeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // This is your "Thread.sleep(2000)"
    )

    @PostMapping("/searchNearby")
    public ResponseEntity<String> searchNearby(@RequestBody Map<String, Object> requestBody) {
        // 1. Use the NEW Nearby Search endpoint

        // 2. Set up the Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        // The FieldMask is REQUIRED. This defines what data you get back.
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.types,places.location,places.rating,places.userRatingCount");

        requestBody.put("rankPreference", "POPULARITY");

        // 3. Prepare the Request Entity (Body + Headers)
        // Note: We pass the requestBody directly because it already contains your 
        // includedTypes, excludedTypes, and locationRestriction.
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Use exchange() to perform a POST request
            ResponseEntity<String> response = restTemplate.exchange(
                    placesNearbyUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            log.error("Error calling Google Places API: " + e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-arch")
    public ResponseEntity<?> testArch() {
        String url = "https://places.googleapis.com/v1/places:searchNearby";

        Map<String, Object> body = new HashMap<>();
        body.put("includedTypes", List.of("national_park", "park", "monument", "tourist_attraction"));
        body.put("maxResultCount", 20);
        body.put("locationRestriction", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", 38.6247, "longitude", -90.1848),
                        "radius", 5000.0
                )
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey); // whatever your @Value field is named
        headers.set("X-Goog-FieldMask", "places.displayName,places.types,places.rating");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return ResponseEntity.ok(response.getBody());
    }

    @Cacheable(
            value = "scenicSpots",
            key = "#root.target.cacheKey(#lat, #lng, #radius, #entityTypes)"
    )
    public List<ScenicSpot> searchNearbyScenic(double lat, double lng, double radius, List<String> entityTypes) {

        // --- Tier 1: MySQL persistent cache ---
        List<ScenicSpot> cached = placesCacheService.findNearby(lat, lng, (int) radius, entityTypes);
        if (!cached.isEmpty()) {
            log.info("MySQL cache hit: {} spots near ({},{})", cached.size(), lat, lng);
            return cached;
        }

        log.info("MySQL cache miss — calling Google Places API near ({},{})", lat, lng);

        // --- Tier 2: Google Places API ---
        List<String> googleTypes = GooglePlacesTypeMapper.toGoogleTypes(entityTypes);
        if (googleTypes == null || googleTypes.isEmpty()) {
            log.warn("No valid Google types found for entity types: {}", entityTypes);
            return Collections.emptyList();
        }

        List<String> safeExcluded = EXCLUDED_PLACE_TYPES.stream()
                .filter(t -> !googleTypes.contains(t))
                .collect(Collectors.toList());

        Map<String, Object> requestBody = Map.of(
                "includedTypes", googleTypes,
                "excludedTypes", safeExcluded,
                "maxResultCount", 20,
                "locationRestriction", Map.of(
                        "circle", Map.of(
                                "center", Map.of("latitude", lat, "longitude", lng),
                                "radius", radius
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress,places.types,"
                + "places.location,places.rating,places.userRatingCount,places.regularOpeningHours");

        try {
            JsonNode response = restTemplate.postForObject(
                    placesNearbyUrl,
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class
            );

            if (response == null || !response.has("places")) {
                log.info("No scenic spots found near ({},{})", lat, lng);
                return Collections.emptyList();
            }

            List<ScenicSpot> spots = new ArrayList<>();
            for (JsonNode node : response.get("places")) {
                ScenicSpot spot = mapToScenicSpot(node);
                if (spot != null) {
                    spots.add(spot);
                }
            }

            // --- Backfill MySQL cache ---
            if (!spots.isEmpty()) {
                placesCacheService.saveAll(spots);
                log.info("Backfilled {} spots to MySQL cache", spots.size());
            }

            return spots;

        } catch (Exception e) {
            log.error("Google API error near ({},{}): {}", lat, lng, e.getMessage());
            return Collections.emptyList();
        }
    }

    private ScenicSpot mapToScenicSpot(JsonNode node) {
        // Check all types for excluded ones before mapping
        if (node.has("types")) {
            for (JsonNode typeNode : node.get("types")) {
                if (EXCLUDED_PLACE_TYPES.contains(typeNode.asText())) {
                    log.info("Excluding place '{}' due to type: {}",
                            node.path("displayName").path("text").asText(), typeNode.asText());
                    return null;
                }
            }
        }

        String name = node.path("displayName").path("text").asText("Unknown Location");
        String address = node.path("formattedAddress").asText();
        String id = node.path("id").asText();
        boolean openNow = node.path("openNow").asBoolean();
        String businessStatus = node.path("businessStatus").asText();
        double rating = node.path("rating").asDouble(0.0);
        int userRatingsTotal = node.path("userRatingCount").asInt(0);
        double lat = node.path("location").path("latitude").asDouble();
        double lng = node.path("location").path("longitude").asDouble();

        ScenicSpot spot = new ScenicSpot();
        spot.setName(name);
        spot.setAddress(address);
        spot.setPlaceId(id);
        spot.setOpenNow(openNow);
        spot.setBusinessStatus(businessStatus);
        spot.setRating(rating);
        spot.setUserRatingsTotal(userRatingsTotal);
        spot.setLat(lat);
        spot.setLng(lng);

        if (node.has("types") && node.get("types").isArray() && node.get("types").size() > 0) {
            List<String> placeTypes = new ArrayList<>();
            node.get("types").forEach(t -> placeTypes.add(t.asText()));
            log.info("Place types for {}: {}", name, placeTypes);
            String bestType = TYPE_PRIORITY.stream()
                    .filter(placeTypes::contains)
                    .findFirst()
                    .orElse(placeTypes.get(0));
            spot.setEntityType(bestType);
        }

        String openingHoursJson = null;
        if (node.has("regularOpeningHours")) {
            openingHoursJson = node.get("regularOpeningHours").toString();
        }
        spot.setOpeningHoursJson(openingHoursJson);

        spot.setScore(0.0);
        spot.setDetour(0);
        spot.setDistFromStart(0.0);

        log.info("Mapped: {}, Rating: {}, Reviews: {}, EntityType: {}",
                name, rating, userRatingsTotal, spot.getEntityType());
        return spot;
    }
}
