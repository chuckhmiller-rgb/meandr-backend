package com.meandr.meandrDataServices.osm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single POI returned from the OSM Overpass API.
 * This is both the internal model and the API response DTO — no JPA persistence,
 * OSM data is fetched on demand and not stored in our database.
 */

@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@NoArgsConstructor  // Generates empty constructor for Jackson
public class OsmPlace {

    private Long osmId;
    private String osmType;          // "node", "way", "relation"
    private OsmEntityType entityType;
    private String name;
    private Double latitude;
    private Double longitude;

    // Enrichment fields from OSM tags
    private String description;
    private String website;
    private String wikipedia;
    private String wikimedia;
    private Double elevation;        // metres, if tagged
    private String access;           // "yes", "private", "permissive"
    private String openingHours;
    private String operator;
    private String surface;          // for trails: "paved", "gravel", etc.
    private String difficulty;       // for climbing: "beginner", "intermediate", etc.
    private String fee;              // "yes", "no"
    private String phone;

    // Computed by service
    private Double distanceFromRouteMiles;
    private Double detourMinutes;


    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getOsmId() { return osmId; }
    public void setOsmId(Long osmId) { this.osmId = osmId; }

    public String getOsmType() { return osmType; }
    public void setOsmType(String osmType) { this.osmType = osmType; }

    public OsmEntityType getEntityType() { return entityType; }
    public void setEntityType(OsmEntityType entityType) { this.entityType = entityType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getWikipedia() { return wikipedia; }
    public void setWikipedia(String wikipedia) { this.wikipedia = wikipedia; }

    public String getWikimedia() { return wikimedia; }
    public void setWikimedia(String wikimedia) { this.wikimedia = wikimedia; }

    public Double getElevation() { return elevation; }
    public void setElevation(Double elevation) { this.elevation = elevation; }

    public String getAccess() { return access; }
    public void setAccess(String access) { this.access = access; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getSurface() { return surface; }
    public void setSurface(String surface) { this.surface = surface; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getFee() { return fee; }
    public void setFee(String fee) { this.fee = fee; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getDistanceFromRouteMiles() { return distanceFromRouteMiles; }
    public void setDistanceFromRouteMiles(Double distanceFromRouteMiles) { this.distanceFromRouteMiles = distanceFromRouteMiles; }

    public Double getDetourMinutes() { return detourMinutes; }
    public void setDetourMinutes(Double detourMinutes) { this.detourMinutes = detourMinutes; }
}
