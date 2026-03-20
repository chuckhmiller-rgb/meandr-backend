package com.meandr.meandrDataServices.osm.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/osm/search
 *
 * The caller provides a list of lat/lng points representing the route,
 * a corridor radius (miles), and which OSM entity types to search for.
 * If entityTypes is null or empty, all supported types are searched.
 */
@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@NoArgsConstructor  // Generates empty constructor for Jackson
public class OsmSearchRequest {

    /**
     * Ordered list of lat/lng points along the route.
     * Typically sampled every N miles from the full polyline — doesn't need to be exhaustive.
     */
    private List<LatLng> routePoints;

    /**
     * How far off the route (in miles) to search. Default: 5.0
     */
    private Double corridorRadiusMiles;

    /**
     * Which OSM entity types to include. If null/empty, all types are returned.
     */
    private List<OsmEntityType> entityTypes;

    /**
     * Max results to return across all types. Default: 50
     */
    private Integer maxResults;


    // ── Inner class ──────────────────────────────────────────────────────────

    public static class LatLng {
        private Double lat;
        private Double lng;

        public LatLng() {}
        public LatLng(Double lat, Double lng) { this.lat = lat; this.lng = lng; }

        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public List<LatLng> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<LatLng> routePoints) { this.routePoints = routePoints; }

    public Double getCorridorRadiusMiles() { return corridorRadiusMiles != null ? corridorRadiusMiles : 5.0; }
    public void setCorridorRadiusMiles(Double corridorRadiusMiles) { this.corridorRadiusMiles = corridorRadiusMiles; }

    public List<OsmEntityType> getEntityTypes() { return entityTypes; }
    public void setEntityTypes(List<OsmEntityType> entityTypes) { this.entityTypes = entityTypes; }

    public Integer getMaxResults() { return maxResults != null ? maxResults : 50; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}