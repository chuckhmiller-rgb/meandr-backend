/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meandr.meandrDataServices.config.MeandrConstants;
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
import java.util.HashSet;
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
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author chuck
 */
@CrossOrigin(origins = "https://meandr-app.vercel.app")
@Slf4j
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class GoogleApiProxyController {

    @Value("${google.api.key}")
    private String apiKey;

    private static final Set<String> EXCLUDED_PLACE_TYPES = MeandrConstants.EXCLUDED_PLACE_TYPES;
    private static final List<String> MOST_RELEVANT_TYPES = MeandrConstants.MOST_RELEVANT_TYPES;
    private static final Map<String, String> ENTITY_KEYWORDS = MeandrConstants.ENTITY_KEYWORDS;

    private static final String FIELD_MASK_FIELDS = MeandrConstants.FIELD_MASK_FIELDS;

    @Autowired
    private PlacesCacheService placesCacheService;

    private String placesSearchTextUrl = "https://places.googleapis.com/v1/places:searchText";
    private String placesSearchNearbyUrl = "https://places.googleapis.com/v1/places:searchNearby";

    private final RestTemplate restTemplate = new RestTemplate();

    private String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json";

    @Autowired
    private CacheManager cacheManager;

    public String cacheKey(double lat, double lng, double radius, List<String> entityTypes) {
        double gridLat = Math.round(lat / 0.05) * 0.05;
        double gridLng = Math.round(lng / 0.05) * 0.05;
        String types = (entityTypes == null || entityTypes.isEmpty())
                ? "none"
                : entityTypes.stream().sorted().collect(Collectors.joining(","));
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

    @Cacheable(
            value = "scenicSpots",
            key = "#root.target.cacheKey(#lat, #lng, #radius, #entityTypes)"
    )
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
                + "places.location,places.rating,places.userRatingCount,places.regularOpeningHours,places.utcOffsetMinutes,places.evChargeOptions");

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
                ScenicSpot spot = mapToScenicSpot(node, true);
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
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.types,places.location,places.rating,places.userRatingCount,places.utcOffsetMinutes,places.evChargeOptions");

        requestBody.put("rankPreference", "POPULARITY");

        // 3. Prepare the Request Entity (Body + Headers)
        // Note: We pass the requestBody directly because it already contains your 
        // includedTypes, excludedTypes, and locationRestriction.
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Use exchange() to perform a POST request
            ResponseEntity<String> response = restTemplate.exchange(placesSearchNearbyUrl,
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

    @GetMapping("/rest-stop/nearby")
    public ResponseEntity<List<Map<String, Object>>> restStopNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "15000") int radius,
            @RequestParam(defaultValue = "all") String category) {

        List<String> includedTypes = switch (category) {
            case "food" ->
                List.of("restaurant", "cafe", "bar", "bakery", "brewery", "winery",
                "beer_garden", "farmers_market", "night_club", "meal_takeaway", "convenience_store");
            case "fuel" ->
                List.of("gas_station");
            case "lodging" ->
                List.of("lodging", "campground");
            case "rv_park" ->
                List.of("rv_park");
            case "ev_charging" ->
                List.of("electric_vehicle_charging_station", "rest_stop");
            default ->
                List.of("restaurant", "cafe", "bar", "bakery", "brewery", "winery",
                "beer_garden", "farmers_market", "night_club", "meal_takeaway",
                "gas_station", "lodging", "convenience_store", "rest_stop",
                "electric_vehicle_charging_station", "campground", "rv_park");
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", MeandrConstants.FIELD_MASK_FIELDS);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("includedTypes", includedTypes);
        requestBody.put("maxResultCount", 20);
        requestBody.put("rankPreference", "POPULARITY");
        requestBody.put("locationRestriction", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", (double) radius
                )
        ));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(placesSearchNearbyUrl, HttpMethod.POST, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode places = root.path("places");
            List<Map<String, Object>> results = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();
            for (JsonNode place : places) {
                Map<String, Object> p = mapPlace(place, false);
                if (p != null) {
                    results.add(p);
                    seenIds.add((String) p.get("placeId"));
                }
            }

            // EV text-search fallback — only relevant for ev_charging / all
            if (category.equals("ev_charging") || category.equals("all")) {
                HttpHeaders textHeaders = new HttpHeaders();
                textHeaders.setContentType(MediaType.APPLICATION_JSON);
                textHeaders.set("X-Goog-Api-Key", apiKey);
                textHeaders.set("X-Goog-FieldMask", MeandrConstants.FIELD_MASK_FIELDS);
                Map<String, Object> textBody = new HashMap<>();
                textBody.put("textQuery", "EV charging station");
                textBody.put("locationBias", Map.of(
                        "circle", Map.of(
                                "center", Map.of("latitude", lat, "longitude", lng),
                                "radius", (double) radius
                        )
                ));
                textBody.put("maxResultCount", 10);
                HttpEntity<Map<String, Object>> textEntity = new HttpEntity<>(textBody, textHeaders);
                ResponseEntity<String> textResponse = restTemplate.exchange(placesSearchTextUrl, HttpMethod.POST, textEntity, String.class);
                JsonNode textRoot = mapper.readTree(textResponse.getBody());
                for (JsonNode place : textRoot.path("places")) {
                    String id = place.path("id").asText();
                    if (!seenIds.contains(id)) {
                        Map<String, Object> p = mapPlace(place, false);
                        p.put("entityType", "ev_charging");
                        results.add(p);
                        seenIds.add(id);
                    }
                }
            }

            // caching, same as before
            List<ScenicSpot> toCache = results.stream().map(p -> {
                ScenicSpot spot = new ScenicSpot();
                spot.setPlaceId((String) p.get("placeId"));
                spot.setName((String) p.get("name"));
                spot.setAddress((String) p.get("address"));
                spot.setLat((double) p.get("lat"));
                spot.setLng((double) p.get("lng"));
                spot.setRating(p.get("rating") != null ? (double) p.get("rating") : 0.0);
                spot.setUserRatingsTotal(p.get("userRatingsTotal") != null ? (int) p.get("userRatingsTotal") : 0);
                spot.setEntityType((String) p.get("entityType"));
                spot.setOpeningHoursJson((String) p.get("openingHoursJson"));
                spot.setUtcOffsetMinutes((Integer) p.get("utcOffsetMinutes"));
                spot.setGooglePhoto((String) p.get("googlePhoto"));
                return spot;
            }).collect(Collectors.toList());

            if (!toCache.isEmpty()) {
                placesCacheService.saveAll(toCache);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error calling rest stop nearby search: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/nearby")
    public ResponseEntity<Map<String, Object>> nearby(@RequestBody Map<String, Object> request) {
        double lat = ((Number) request.get("lat")).doubleValue();
        double lng = ((Number) request.get("lng")).doubleValue();
        int requestedRadius = request.containsKey("radius") ? ((Number) request.get("radius")).intValue() : 40000;
        int radius = Math.max(requestedRadius, 40000);
        List<String> entityPreferences = (List<String>) request.getOrDefault("entityPreferences", new ArrayList<>());
        List<String> includeKeywords = (List<String>) request.getOrDefault("includeKeywords", new ArrayList<>());

        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> rectangle = radiusToRectangle(lat, lng, radius);

        List<String> prefTypes = new ArrayList<>();
        for (String pref : entityPreferences) {
            List<String> googleTypes = GooglePlacesTypeMapper.toGoogleTypes(List.of(pref.toLowerCase()));
            prefTypes.addAll(googleTypes);
        }
        prefTypes = prefTypes.stream().distinct().collect(Collectors.toList());

        try {
            // Entity preference types via Nearby Search (type-restricted, not free text)
            if (!prefTypes.isEmpty()) {
                HttpHeaders nearbyHeaders = new HttpHeaders();
                nearbyHeaders.setContentType(MediaType.APPLICATION_JSON);
                nearbyHeaders.set("X-Goog-Api-Key", apiKey);
                nearbyHeaders.set("X-Goog-FieldMask", FIELD_MASK_FIELDS);

                Map<String, Object> nearbyBody = new HashMap<>();
                nearbyBody.put("includedTypes", prefTypes);
                nearbyBody.put("maxResultCount", 20);
                nearbyBody.put("locationRestriction", Map.of(
                        "circle", Map.of("center", Map.of("latitude", lat, "longitude", lng), "radius", (double) radius)
                ));

                HttpEntity<Map<String, Object>> nearbyEntity = new HttpEntity<>(nearbyBody, nearbyHeaders);
                try {
                    ResponseEntity<String> nearbyResponse = restTemplate.exchange(placesSearchNearbyUrl, HttpMethod.POST, nearbyEntity, String.class);
                    for (JsonNode place : mapper.readTree(nearbyResponse.getBody()).path("places")) {
                        String id = place.path("id").asText();
                        if (!seenIds.contains(id)) {
                            Map<String, Object> p = mapPlace(place, true);
                            if (p != null) {
                                results.add(p);
                                seenIds.add(id);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Nearby search failed for prefTypes {}: {}", prefTypes, ex.getMessage());
                }
            }

            // Include keyword searches — free text, kept as Text Search
            for (String keyword : includeKeywords) {
                HttpHeaders textHeaders = new HttpHeaders();
                textHeaders.setContentType(MediaType.APPLICATION_JSON);
                textHeaders.set("X-Goog-Api-Key", apiKey);
                textHeaders.set("X-Goog-FieldMask", FIELD_MASK_FIELDS.toString());

                Map<String, Object> textBody = new HashMap<>();
                textBody.put("textQuery", keyword);
                textBody.put("maxResultCount", 10);
                textBody.put("locationRestriction", Map.of("rectangle", rectangle));

                HttpEntity<Map<String, Object>> textEntity = new HttpEntity<>(textBody, textHeaders);
                try {
                    ResponseEntity<String> textResponse = restTemplate.exchange(placesSearchTextUrl, HttpMethod.POST, textEntity, String.class);
                    for (JsonNode place : mapper.readTree(textResponse.getBody()).path("places")) {
                        String id = place.path("id").asText();
                        if (!seenIds.contains(id)) {
                            Map<String, Object> p = mapPlace(place, true);
                            if (p != null) {
                                results.add(p);
                                seenIds.add(id);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Text search failed for keyword {}: {}", keyword, ex.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("searchRectangle", rectangle);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calling nearby search: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/googlePhoto")
    public ResponseEntity<byte[]> googlePhoto(@RequestParam String name, @RequestParam(defaultValue = "400") int maxHeightPx) {
        String url = "https://places.googleapis.com/v1/" + name + "/media?maxHeightPx=" + maxHeightPx + "&key=" + apiKey;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(response.getHeaders().getContentType().toString()))
                .body(response.getBody());
    }

    private Map<String, Object> radiusToRectangle(double lat, double lng, int radiusMeters) {
        double latDelta = radiusMeters / 111320.0;
        double lngDelta = radiusMeters / (111320.0 * Math.cos(Math.toRadians(lat)));
        return Map.of(
                "low", Map.of("latitude", lat - latDelta, "longitude", lng - lngDelta),
                "high", Map.of("latitude", lat + latDelta, "longitude", lng + lngDelta)
        );
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
        headers.set("X-Goog-FieldMask", FIELD_MASK_FIELDS);

        try {
            JsonNode response = restTemplate.postForObject(placesSearchNearbyUrl,
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class
            );

            if (response == null || !response.has("places")) {
                log.info("No scenic spots found near ({},{})", lat, lng);
                return Collections.emptyList();
            }

            List<ScenicSpot> spots = new ArrayList<>();
            for (JsonNode node : response.get("places")) {
                ScenicSpot spot = mapToScenicSpot(node, true);
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

    private Map<String, Object> mapPlace(JsonNode place, boolean applyExclusions) {
        ScenicSpot spot = mapToScenicSpot(place, applyExclusions);
        if (spot == null) {
            return null;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("placeId", spot.getPlaceId());
        p.put("name", spot.getName());
        p.put("address", spot.getAddress());
        p.put("lat", spot.getLat());
        p.put("lng", spot.getLng());
        p.put("rating", spot.getRating());
        p.put("userRatingsTotal", spot.getUserRatingsTotal());
        p.put("openingHoursJson", spot.getOpeningHoursJson());
        p.put("entityType", spot.getEntityType());
        p.put("utcOffsetMinutes", spot.getUtcOffsetMinutes());

        JsonNode evCharge = place.path("evChargeOptions");
        if (!evCharge.isMissingNode()) {
            p.put("evChargeOptions", evCharge.toString());

            List<Map<String, Object>> connectorDetails = new ArrayList<>();
            for (JsonNode conn : evCharge.path("connectorAggregation")) {
                Map<String, Object> c = new HashMap<>();
                c.put("type", formatConnectorType(conn.path("type").asText("")));
                c.put("maxChargeRateKw", conn.path("maxChargeRateKw").asDouble(0));
                c.put("count", conn.path("count").asInt(0));

                if (conn.has("availableCount")) {
                    c.put("availableCount", conn.path("availableCount").asInt());
                }
                connectorDetails.add(c);
            }
            p.put("connectorDetails", connectorDetails);
            p.put("connectorCount", evCharge.path("connectorCount").asInt(0));

        } else {
            p.put("evChargeOptions", null);
            p.put("connectorDetails", List.of());
            p.put("connectorCount", 0);

        }

        JsonNode photos = place.path("photos");
        log.info("Raw photos node for {}: {}", place.path("displayName").path("text").asText(), photos.toString());
        if (photos.isArray() && photos.size() > 0) {
            p.put("googlePhoto", photos.get(0).path("name").asText(null));
        } else {
            p.put("googlePhoto", null);
        }

        return p;
    }

    private String formatConnectorType(String raw) {
        return switch (raw) {
            case "EV_CONNECTOR_TYPE_J1772" ->
                "J1772";
            case "EV_CONNECTOR_TYPE_CCS_COMBO_1", "EV_CONNECTOR_TYPE_CCS_COMBO_2" ->
                "CCS Combo";
            case "EV_CONNECTOR_TYPE_CHADEMO" ->
                "CHAdeMO";
            case "EV_CONNECTOR_TYPE_TESLA" ->
                "Tesla";
            case "EV_CONNECTOR_TYPE_NACS" ->
                "NACS";
            case "EV_CONNECTOR_TYPE_TYPE_2" ->
                "Type 2";
            case "EV_CONNECTOR_TYPE_OTHER", "EV_CONNECTOR_TYPE_UNSPECIFIED" ->
                "Other";
            default ->
                raw.replace("EV_CONNECTOR_TYPE_", "").replace("_", " ");
        };
    }

    private ScenicSpot mapToScenicSpot(JsonNode node, boolean applyExclusions) {
        if (applyExclusions && node.has("types")) {
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
            String bestType = MOST_RELEVANT_TYPES.stream()
                    .filter(placeTypes::contains)
                    .findFirst()
                    .orElse(placeTypes.get(0));
            spot.setEntityType(bestType);
        }

        String openingHoursJson = null;
        if (node.has("currentOpeningHours")) {
            openingHoursJson = node.get("currentOpeningHours").toString();
        } else if (node.has("regularOpeningHours")) {
            openingHoursJson = node.get("regularOpeningHours").toString();
        }
        spot.setOpeningHoursJson(openingHoursJson);

        if (node.has("utcOffsetMinutes")) {
            spot.setUtcOffsetMinutes(node.get("utcOffsetMinutes").asInt());
        }

        JsonNode photos = node.path("photos");
        if (photos.isArray() && photos.size() > 0) {
            spot.setGooglePhoto(photos.get(0).path("name").asText(null));
        }

        spot.setScore(0.0);
        spot.setDetour(0);
        spot.setDistFromStart(0.0);

        log.info("Mapped: {}, Rating: {}, Reviews: {}, EntityType: {}",
                name, rating, userRatingsTotal, spot.getEntityType());
        return spot;
    }
}
