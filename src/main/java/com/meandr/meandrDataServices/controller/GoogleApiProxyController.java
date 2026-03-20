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
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author chuck
 */
@RestController
@RequestMapping("/api/places")
@Slf4j
public class GoogleApiProxyController {

    @Value("${google.api.key}")
    private String apiKey;

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
        Map<String, Object> locationRestriction = Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", radius
                )
        );

        List<String> googleTypes = GooglePlacesTypeMapper.toGoogleTypes(entityTypes);
        if (googleTypes.isEmpty()) {
            log.warn("No mappable Google Places types for: {}", entityTypes);
            return Collections.emptyList();
        }

        // Remove any overlap between included and excluded to avoid API rejection
        List<String> allExcluded = List.of(
                "funeral_home", "lawyer", "accounting",
                "insurance_agency", "real_estate_agency", "storage",
                "moving_company", "car_dealer", "car_repair", "car_wash",
                "laundry", "bank", "atm", "post_office", "hospital",
                "dentist", "doctor", "physiotherapist", "veterinary_care",
                "police"
        );
        List<String> safeExcluded = allExcluded.stream()
                .filter(t -> !googleTypes.contains(t))
                .collect(Collectors.toList());

        Map<String, Object> requestBody = Map.of(
                "includedTypes", googleTypes,
                "excludedTypes", safeExcluded,
                "maxResultCount", 20,
                "locationRestriction", locationRestriction
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress,places.types,"
                + "places.location,places.rating,places.userRatingCount");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            JsonNode response = restTemplate.postForObject(placesNearbyUrl, entity, JsonNode.class);

            List<ScenicSpot> spots = new ArrayList<>();
            if (response != null && response.has("places")) {
                for (JsonNode node : response.get("places")) {
                    ScenicSpot spot = mapToScenicSpot(node);
                    if (spot != null) {
                        spots.add(spot);
                    }
                }
            } else {
                log.info("No scenic spots found near ({},{})", lat, lng);
            }

            // --- Backfill MySQL cache ---
            if (!spots.isEmpty()) {
                placesCacheService.saveAll(spots);
                log.info("Backfilled {} spots to MySQL cache", spots.size());
            }

            return spots;

        } catch (Exception e) {
            log.error("Google API Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ScenicSpot mapToScenicSpot(JsonNode node) {
        // 1. Name is usually inside displayName.text
        String name = node.path("displayName").path("text").asText("Unknown Location");
        String address = node.path("formattedAddress").asText();
        String id = node.path("id").asText();

        // 2. Note: 'openNow' is usually inside 'regularOpeningHours' in the new API
        // If you aren't seeing it, you might need to check node.path("regularOpeningHours").path("openNow")
        boolean openNow = node.path("openNow").asBoolean();

        String businessStatus = node.path("businessStatus").asText();
        double rating = node.path("rating").asDouble(0.0);

        // FIX: Google V1 uses 'userRatingCount', not 'userRatingTotal'
        int userRatingsTotal = node.path("userRatingCount").asInt(0);

        // FIX: Google V1 uses 'latitude' and 'longitude' inside 'location'
        double lat = node.path("location").path("latitude").asDouble();
        double lng = node.path("location").path("longitude").asDouble();

        // Create the object using the No-Args constructor and setters (via @Data)
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
        // In mapToScenicSpot(), after setting other fields:
        if (node.has("types") && node.get("types").isArray() && node.get("types").size() > 0) {
            spot.setEntityType(node.get("types").get(0).asText());
        }

        // These will be populated later in the service loop
        spot.setScore(0.0);
        spot.setDetour(0);
        spot.setDistFromStart(0.0);

        log.info("Mapped: {}, Rating: {}, Reviews: {}, EntityTypes: {}", name, rating, userRatingsTotal, spot.getEntityType());
        return spot;
    }
}
