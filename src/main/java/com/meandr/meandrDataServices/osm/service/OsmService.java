package com.meandr.meandrDataServices.osm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import com.meandr.meandrDataServices.osm.model.OsmSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OsmService {

    private static final Logger log = LoggerFactory.getLogger(OsmService.class);

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final double MILES_TO_METERS = 1609.344;

    /**
     * Grid cell size in degrees for deduplicating route points before querying Overpass.
     * 0.15 degrees ≈ ~16km — well above the typical search radius so overlapping
     * circles are avoided without leaving gaps.
     */
    private static final double DEDUP_GRID_DEGREES = 0.15;

    /** Hard cap on route points sent to Overpass regardless of deduplication. */
    private static final int MAX_OVERPASS_POINTS = 25;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OsmService() {
        this.httpClient   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Search for OSM POIs along the provided route corridor.
     * Deduplicates route points onto a grid before querying to avoid
     * redundant Overpass clauses and 414 URI Too Long errors.
     */
    public List<OsmPlace> searchAlongRoute(OsmSearchRequest request) {
        List<OsmEntityType> types = resolveTypes(request.getEntityTypes());
        double radiusMeters       = request.getCorridorRadiusMiles() * MILES_TO_METERS;

        // Deduplicate route points onto a grid and cap count
        List<OsmSearchRequest.LatLng> dedupedPoints = deduplicateRoutePoints(request.getRoutePoints());
        log.info("OSM query: {} deduplicated points (from {}), {} types, radius={}m",
                dedupedPoints.size(), request.getRoutePoints().size(), types.size(), (int) radiusMeters);

        String query = buildOverpassQuery(dedupedPoints, types, radiusMeters);
        log.debug("Overpass query:\n{}", query);

        String json = executeOverpassQuery(query);
        if (json == null) return Collections.emptyList();

        List<OsmPlace> places = parseOverpassResponse(json, types);

        // Attach distance-from-route metadata and sort
        enrichWithDistance(places, dedupedPoints);
        places.sort(Comparator.comparingDouble(p -> p.getDistanceFromRouteMiles() == null ? 999.0 : p.getDistanceFromRouteMiles()));

        // Deduplicate by OSM id
        places = deduplicateById(places);

        // Apply max results cap
        int max = request.getMaxResults();
        return places.size() > max ? places.subList(0, max) : places;
    }

    // ── Route Point Deduplication ────────────────────────────────────────────

    /**
     * Snaps each route point to a coarse grid and returns one representative
     * point per grid cell. This eliminates clusters of near-identical coordinates
     * that inflate Overpass queries without adding coverage.
     */
    private List<OsmSearchRequest.LatLng> deduplicateRoutePoints(List<OsmSearchRequest.LatLng> points) {
        Set<String> seenCells = new LinkedHashSet<>();
        List<OsmSearchRequest.LatLng> result = new ArrayList<>();

        for (OsmSearchRequest.LatLng p : points) {
            long gridLat = Math.round(p.getLat() / DEDUP_GRID_DEGREES);
            long gridLng = Math.round(p.getLng() / DEDUP_GRID_DEGREES);
            String cell = gridLat + "," + gridLng;
            if (seenCells.add(cell)) {
                result.add(p);
                if (result.size() >= MAX_OVERPASS_POINTS) break;
            }
        }
        return result;
    }

    // ── Query Builder ────────────────────────────────────────────────────────

    /**
     * Builds an Overpass QL query.
     *
     * Strategy: for each (entity type × route point) pair, emit a
     * "node[key=value](around:radius,lat,lng)" clause.
     * Union all clauses in a single request to minimize round trips.
     *
     * Example output:
     *   [out:json][timeout:30];
     *   (
     *     node["waterway"="waterfall"](around:8046,35.1,-106.5);
     *     way["waterway"="waterfall"](around:8046,35.1,-106.5);
     *     ...
     *   );
     *   out center tags;
     */
    private String buildOverpassQuery(
            List<OsmSearchRequest.LatLng> routePoints,
            List<OsmEntityType> types,
            double radiusMeters) {

        StringBuilder sb = new StringBuilder();
        sb.append("[out:json][timeout:30];\n(\n");

        for (OsmSearchRequest.LatLng point : routePoints) {
            for (OsmEntityType type : types) {
                String around = String.format(Locale.US,
                        "(around:%.0f,%.6f,%.6f)",
                        radiusMeters, point.getLat(), point.getLng());

                // Query both nodes and ways — many OSM features are mapped as ways
                sb.append(String.format("  node[\"%s\"=\"%s\"]%s;\n",
                        type.osmKey, type.osmValue, around));
                sb.append(String.format("  way[\"%s\"=\"%s\"]%s;\n",
                        type.osmKey, type.osmValue, around));
            }
        }

        sb.append(");\nout center tags;");
        return sb.toString();
    }

    // ── HTTP Execution ───────────────────────────────────────────────────────

    /**
     * Executes an Overpass QL query via HTTP POST.
     * POST is required for large queries — GET hits Apache's URI length limit (414).
     */
    private String executeOverpassQuery(String query) {
        try {
            String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OVERPASS_URL))
                    .header("User-Agent", "Meandr/1.0 (scenic route planning app)")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(35))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Overpass API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }
            return response.body();

        } catch (Exception e) {
            log.error("Error calling Overpass API", e);
            return null;
        }
    }

    // ── Response Parser ──────────────────────────────────────────────────────

    private List<OsmPlace> parseOverpassResponse(String json, List<OsmEntityType> requestedTypes) {
        List<OsmPlace> results = new ArrayList<>();

        // Build a lookup map: "key=value" → OsmEntityType for fast tag matching
        Map<String, OsmEntityType> tagIndex = new HashMap<>();
        for (OsmEntityType type : requestedTypes) {
            tagIndex.put(type.osmKey + "=" + type.osmValue, type);
        }

        try {
            JsonNode root     = objectMapper.readTree(json);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) return results;

            for (JsonNode el : elements) {
                OsmPlace place = parseElement(el, tagIndex);
                if (place != null) results.add(place);
            }

        } catch (Exception e) {
            log.error("Error parsing Overpass response", e);
        }

        return results;
    }

    private OsmPlace parseElement(JsonNode el, Map<String, OsmEntityType> tagIndex) {
        JsonNode tags = el.get("tags");
        if (tags == null) return null;

        // Determine which entity type this element matches
        OsmEntityType matchedType = null;
        for (Map.Entry<String, OsmEntityType> entry : tagIndex.entrySet()) {
            String[] parts = entry.getKey().split("=", 2);
            JsonNode tagVal = tags.get(parts[0]);
            if (tagVal != null && tagVal.asText().equals(parts[1])) {
                matchedType = entry.getValue();
                break;
            }
        }
        if (matchedType == null) return null;

        // Skip unnamed features unless they're peaks or waterfalls (unnamed ones are still interesting)
        String name = tags.has("name") ? tags.get("name").asText() : null;
        boolean allowUnnamed = matchedType == OsmEntityType.WATERFALL
                || matchedType == OsmEntityType.PEAK
                || matchedType == OsmEntityType.SCENIC_OVERLOOK;
        if (name == null && !allowUnnamed) return null;

        // Resolve coordinates — nodes have lat/lon directly; ways have a "center" object
        double lat, lon;
        String type = el.has("type") ? el.get("type").asText() : "node";
        if ("way".equals(type) && el.has("center")) {
            JsonNode center = el.get("center");
            lat = center.get("lat").asDouble();
            lon = center.get("lon").asDouble();
        } else if (el.has("lat") && el.has("lon")) {
            lat = el.get("lat").asDouble();
            lon = el.get("lon").asDouble();
        } else {
            return null; // No usable coordinates
        }

        OsmPlace place = new OsmPlace();
        place.setOsmId(el.has("id") ? el.get("id").asLong() : null);
        place.setOsmType(type);
        place.setEntityType(matchedType);
        place.setName(name != null ? name : matchedType.displayName);
        place.setLatitude(lat);
        place.setLongitude(lon);

        // Map common OSM tags to place fields
        place.setDescription(tagText(tags, "description"));
        place.setWebsite(tagText(tags, "website", "url", "contact:website"));
        place.setWikipedia(tagText(tags, "wikipedia"));
        place.setWikimedia(tagText(tags, "wikimedia_commons"));
        place.setAccess(tagText(tags, "access"));
        place.setOpeningHours(tagText(tags, "opening_hours"));
        place.setOperator(tagText(tags, "operator"));
        place.setSurface(tagText(tags, "surface"));
        place.setFee(tagText(tags, "fee"));
        place.setPhone(tagText(tags, "phone", "contact:phone"));

        // Elevation — tagged as "ele" in metres
        if (tags.has("ele")) {
            try { place.setElevation(Double.parseDouble(tags.get("ele").asText())); }
            catch (NumberFormatException ignored) {}
        }

        // Climbing difficulty from SAC scale or climbing:grade tags
        place.setDifficulty(tagText(tags, "sac_scale", "climbing:grade:yds", "climbing:grade"));

        return place;
    }

    // ── Enrichment ───────────────────────────────────────────────────────────

    /**
     * For each place, find the nearest route point and compute great-circle distance.
     */
    private void enrichWithDistance(List<OsmPlace> places, List<OsmSearchRequest.LatLng> routePoints) {
        for (OsmPlace place : places) {
            double minDist = Double.MAX_VALUE;
            for (OsmSearchRequest.LatLng point : routePoints) {
                double d = haversineMiles(place.getLatitude(), place.getLongitude(),
                        point.getLat(), point.getLng());
                if (d < minDist) minDist = d;
            }
            place.setDistanceFromRouteMiles(minDist == Double.MAX_VALUE ? null : Math.round(minDist * 10.0) / 10.0);
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private List<OsmEntityType> resolveTypes(List<OsmEntityType> requested) {
        if (requested == null || requested.isEmpty()) {
            return Arrays.asList(OsmEntityType.values());
        }
        return requested;
    }

    private List<OsmPlace> deduplicateById(List<OsmPlace> places) {
        Map<Long, OsmPlace> seen = new LinkedHashMap<>();
        for (OsmPlace p : places) {
            if (p.getOsmId() != null) {
                seen.putIfAbsent(p.getOsmId(), p);
            } else {
                seen.put((long) System.identityHashCode(p), p);
            }
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * Read the first non-null value from a sequence of tag keys.
     */
    private String tagText(JsonNode tags, String... keys) {
        for (String key : keys) {
            if (tags.has(key)) return tags.get(key).asText();
        }
        return null;
    }

    /**
     * Haversine formula — great-circle distance in miles.
     */
    private double haversineMiles(double lat1, double lon1, double lat2, double lon2) {
        double R    = 3958.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}