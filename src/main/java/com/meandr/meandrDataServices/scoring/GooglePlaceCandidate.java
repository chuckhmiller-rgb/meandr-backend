package com.meandr.meandrDataServices.scoring;

/**
 * Represents a Google Places candidate ready for scoring.
 * This is populated from your existing RouteBeautifierService output —
 * adapt field names to match your actual Place/Waypoint model.
 */
public class GooglePlaceCandidate {

    private String placeId;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String entityType;      // Google Places type string e.g. "restaurant"
    private Double rating;          // 1.0–5.0
    private Integer userRatingCount;
    private Double detourMinutes;
    private Double distFromStart;   // miles along route

    public GooglePlaceCandidate() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getUserRatingCount() { return userRatingCount; }
    public void setUserRatingCount(Integer userRatingCount) { this.userRatingCount = userRatingCount; }

    public Double getDetourMinutes() { return detourMinutes; }
    public void setDetourMinutes(Double detourMinutes) { this.detourMinutes = detourMinutes; }

    public Double getDistFromStart() { return distFromStart; }
    public void setDistFromStart(Double distFromStart) { this.distFromStart = distFromStart; }
}