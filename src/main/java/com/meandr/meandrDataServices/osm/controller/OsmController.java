package com.meandr.meandrDataServices.osm.controller;

import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import com.meandr.meandrDataServices.osm.model.OsmSearchRequest;
import com.meandr.meandrDataServices.osm.service.OsmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for OpenStreetMap Overpass API integration.
 *
 * Base path: /api/v1/osm
 */
@RestController
@RequestMapping("/api/v1/osm")
@CrossOrigin(origins = "*")
public class OsmController {

    private final OsmService osmService;

    public OsmController(OsmService osmService) {
        this.osmService = osmService;
    }

    // ── POST /api/v1/osm/search ──────────────────────────────────────────────

    /**
     * Search for OSM POIs along a route corridor.
     *
     * Request body:
     * {
     *   "routePoints": [{"lat": 35.1, "lng": -106.5}, ...],
     *   "corridorRadiusMiles": 5.0,
     *   "entityTypes": ["WATERFALL", "SCENIC_OVERLOOK", "HOT_SPRING"],
     *   "maxResults": 50
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<List<OsmPlace>> searchAlongRoute(
            @RequestBody OsmSearchRequest request) {

        if (request.getRoutePoints() == null || request.getRoutePoints().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<OsmPlace> results = osmService.searchAlongRoute(request);
        return ResponseEntity.ok(results);
    }

    // ── GET /api/v1/osm/types ────────────────────────────────────────────────

    /**
     * Returns all supported OSM entity types with their display metadata.
     * Used by the frontend to populate the entity type picker.
     *
     * Response:
     * [
     *   { "id": "WATERFALL", "displayName": "Waterfall", "emoji": "💧",
     *     "osmKey": "waterway", "osmValue": "waterfall" },
     *   ...
     * ]
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getSupportedTypes() {
        List<Map<String, String>> types = Arrays.stream(OsmEntityType.values())
                .map(t -> Map.of(
                        "id",          t.name(),
                        "displayName", t.displayName,
                        "emoji",       t.emoji,
                        "osmKey",      t.osmKey,
                        "osmValue",    t.osmValue
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }

    // ── GET /api/v1/osm/health ───────────────────────────────────────────────

    /**
     * Quick health check — verifies Overpass API is reachable.
     * Fires a minimal query (single node lookup).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        OsmSearchRequest req = new OsmSearchRequest();
        req.setRoutePoints(List.of(new OsmSearchRequest.LatLng(36.1069, -112.1129))); // Grand Canyon
        req.setEntityTypes(List.of(OsmEntityType.SCENIC_OVERLOOK));
        req.setCorridorRadiusMiles(1.0);
        req.setMaxResults(1);

        try {
            List<OsmPlace> results = osmService.searchAlongRoute(req);
            return ResponseEntity.ok(Map.of(
                    "status",      "ok",
                    "overpassUp",  true,
                    "testResults", results.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status",     "degraded",
                    "overpassUp", false,
                    "error",      e.getMessage()
            ));
        }
    }
}