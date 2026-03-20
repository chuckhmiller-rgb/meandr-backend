package com.meandr.meandrDataServices.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meandr.meandrDataServices.dto.CoordinateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Slf4j
@Service
public class MapBoxRoutingService {

    private final String accessToken;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MapBoxRoutingService() {
        this.accessToken = System.getenv("MAPBOX_API_KEY");
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("MAPBOX_ACCESS_TOKEN not set - scenic routing will be disabled");
        }
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get route alternatives from MapBox Directions API. Returns list of routes
     * as coordinate lists.
     */
    /**
     * Get route alternatives from MapBox Directions API.
     *
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @return List of route alternatives with scenic percentages
     */
    public List<MapBoxRoute> getRouteAlternatives(double originLat, double originLng,
            double destLat, double destLng) {
        try {
            // MapBox uses lng,lat order (opposite of Google!)
            String coordinates = String.format("%s,%s;%s,%s",
                    originLng, originLat, destLng, destLat);

            String url = String.format(
                    "https://api.mapbox.com/directions/v5/mapbox/driving/%s"
                    + "?alternatives=true&steps=true&geometries=geojson&access_token=%s",
                    coordinates,
                    accessToken
            );

            log.info("Calling MapBox Directions API");
            log.debug("MapBox URL: {}", url.replace(accessToken, "***"));

            String jsonResponse = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            // Check for errors
            if (root.has("code") && !root.get("code").asText().equals("Ok")) {
                log.error("MapBox API error: {}", root.get("message").asText());
                return new ArrayList<>();
            }

            JsonNode routesNode = root.get("routes");

            if (routesNode == null || !routesNode.isArray()) {
                log.warn("No routes found in MapBox response");
                return new ArrayList<>();
            }

            log.info("MapBox returned {} routes in response", routesNode.size());

            List<MapBoxRoute> routes = new ArrayList<>();

            // Parse ALL routes (not just the first one)
            for (int i = 0; i < routesNode.size(); i++) {
                JsonNode routeNode = routesNode.get(i);

                // Extract basic route info
                double duration = routeNode.get("duration").asDouble(); // seconds
                double distance = routeNode.get("distance").asDouble(); // meters

                // Calculate scenic percentage
                double scenicPercentage = calculateScenicPercentage(routeNode);

                // Extract geometry coordinates
                List<CoordinateDto> routeCoordinates = extractCoordinates(routeNode);  // ← Renamed!

                MapBoxRoute route = new MapBoxRoute(
                        duration,
                        distance,
                        scenicPercentage,
                        routeCoordinates // ← Use new name
                );

                routes.add(route);

                log.info("Route {}: {} mins, {} km, {}% scenic",
                        i + 1,
                        String.format("%.1f", duration / 60.0),
                        String.format("%.1f", distance / 1000.0),
                        String.format("%.1f", scenicPercentage));
            }

            log.info("Parsed {} route alternatives from MapBox", routes.size());
            return routes;

        } catch (Exception e) {
            log.error("Failed to fetch MapBox routes: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract coordinates from MapBox route geometry.
     */
    private List<CoordinateDto> extractCoordinates(JsonNode routeNode) {
        List<CoordinateDto> coordinates = new ArrayList<>();

        try {
            JsonNode geometryNode = routeNode.get("geometry");
            if (geometryNode != null) {
                JsonNode coordsNode = geometryNode.get("coordinates");
                if (coordsNode != null && coordsNode.isArray()) {
                    for (JsonNode coord : coordsNode) {
                        double lng = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        coordinates.add(new CoordinateDto(lat, lng));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract coordinates: {}", e.getMessage());
        }

        return coordinates;
    }

    /**
     * Parse MapBox JSON response into route objects.
     */
    private List<MapBoxRoute> parseMapBoxResponse(String jsonResponse) {
        List<MapBoxRoute> routes = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode routesNode = root.get("routes");

            if (routesNode != null && routesNode.isArray()) {
                for (JsonNode routeNode : routesNode) {
                    double duration = routeNode.get("duration").asDouble(); // seconds
                    double distance = routeNode.get("distance").asDouble(); // meters

                    // Extract coordinates from geometry
                    List<CoordinateDto> coordinates = new ArrayList<>();
                    JsonNode geometryNode = routeNode.get("geometry");
                    if (geometryNode != null) {
                        JsonNode coordsArray = geometryNode.get("coordinates");
                        if (coordsArray != null && coordsArray.isArray()) {
                            for (JsonNode coord : coordsArray) {
                                // MapBox uses [lng, lat] format
                                double lng = coord.get(0).asDouble();
                                double lat = coord.get(1).asDouble();
                                coordinates.add(new CoordinateDto(lat, lng));
                            }
                        }
                    }

                    // Calculate scenic percentage from steps
                    double scenicPercent = calculateScenicPercentage(routeNode);

                    routes.add(new MapBoxRoute(duration, distance, scenicPercent, coordinates));
                }
            }

            log.info("Parsed {} route alternatives from MapBox", routes.size());

        } catch (Exception e) {
            log.error("Failed to parse MapBox response: {}", e.getMessage());
        }

        return routes;
    }

    /**
     * Calculate scenic percentage based on road classes in the route.
     */
    private double calculateScenicPercentage(JsonNode routeNode) {
        try {
            double totalDistance = routeNode.get("distance").asDouble();
            double scenicDistance = 0.0;

            JsonNode legsNode = routeNode.get("legs");
            if (legsNode != null && legsNode.isArray()) {
                for (JsonNode leg : legsNode) {
                    JsonNode stepsNode = leg.get("steps");
                    if (stepsNode != null && stepsNode.isArray()) {
                        for (JsonNode step : stepsNode) {
                            double stepDistance = step.get("distance").asDouble();
                            String roadClass = extractRoadClass(step);

                            if (isScenicRoad(roadClass)) {
                                scenicDistance += stepDistance;
                            }
                        }
                    }
                }
            }

            return (scenicDistance / totalDistance) * 100.0;

        } catch (Exception e) {
            log.warn("Could not calculate scenic percentage: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Extract road class from step (if available).
     */
    private String extractRoadClass(JsonNode step) {
        // MapBox stores road class inside intersections array
        JsonNode intersectionsNode = step.get("intersections");
        if (intersectionsNode != null && intersectionsNode.isArray() && intersectionsNode.size() > 0) {
            // Get the first intersection's road class
            JsonNode firstIntersection = intersectionsNode.get(0);
            JsonNode mapboxStreetsNode = firstIntersection.get("mapbox_streets_v8");
            if (mapboxStreetsNode != null) {
                JsonNode classNode = mapboxStreetsNode.get("class");
                if (classNode != null && !classNode.asText().isEmpty()) {
                    String roadClass = classNode.asText();
                    log.debug("Found road class: {}", roadClass);
                    return roadClass;
                }
            }
        }

        // Fallback: parse name if road_class not available
        JsonNode nameNode = step.get("name");
        if (nameNode != null) {
            String name = nameNode.asText();

            if (name.matches(".*I-\\d+.*") || name.contains("Interstate")) {
                return "motorway";
            }
            if (name.matches(".*US[- ]\\d+.*")) {
                return "primary";
            }
            if (name.matches(".*(State Route|SR-|CA-|Highway).*")) {
                return "secondary";
            }
        }

        // If we can't determine, assume NOT scenic
        log.warn("Could not determine road class for step: {}", nameNode != null ? nameNode.asText() : "unknown");
        return "motorway";
    }

    /**
     * Determine if a road class is considered "scenic".
     */
    private boolean isScenicRoad(String roadClass) {
        if (roadClass == null) {
            return true;
        }

        // Motorways (interstates) are NOT scenic
        // Everything else (primary, secondary, tertiary, local) is considered scenic
        return !roadClass.equals("motorway") && !roadClass.equals("motorway_link");
    }


    /**
     * Simple data class to hold MapBox route information.
     */
    @Data
    @AllArgsConstructor
    public static class MapBoxRoute {

        private double duration;        // seconds
        private double distance;        // meters
        private double scenicPercentage; // 0-100
        private List<CoordinateDto> coordinates;

        public double getDurationMinutes() {
            return duration / 60.0;
        }

        public double getDistanceKm() {
            return distance / 1000.0;
        }
    }
}
