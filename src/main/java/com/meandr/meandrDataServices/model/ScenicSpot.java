package com.meandr.meandrDataServices.model;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chuck
 */
@Data // Generates getters, setters, equals, hashCode, and toString automatically
@AllArgsConstructor // Generates constructor for all fields
@NoArgsConstructor  // Generates empty constructor for Jackson
public class ScenicSpot {

    public String name;
    public String placeId;
    public String address;
    public boolean openNow;
    public String predictedStatus; 
    public String businessStatus; 
    public double lat;
    public double lng;
    public double rating;
    public int userRatingsTotal;
    public double score;
    public int detour; // Renamed to match the math logic; or use detourMinutes
    public double distFromStart;
    private String entityType;  // Google Places type string or OsmEntityType.name()
    private int segmentIndex;

   

        // 3. THE COPY CONSTRUCTOR (The one you just asked for)
    public ScenicSpot(ScenicSpot other) {
        this.name = other.name;
        this.placeId = other.placeId;
        this.address = other.address;
        this.lat = other.lat;
        this.lng = other.lng;
        this.rating = other.rating;
        this.userRatingsTotal = other.userRatingsTotal;
        this.score = other.score;
        this.detour = other.detour;
        this.distFromStart = other.distFromStart;
        this.entityType = other.entityType;
        this.openNow = other.openNow;
        this.businessStatus = other.businessStatus;
    }

    /*
    
     // Getter and Setter
    public double getDistFromStart() {
        return distFromStart;
    }

    public void setDistFromStart(double distFromStart) {
        this.distFromStart = distFromStart;
    }
    
    public ScenicSpot(ScenicSpot other) {
        this.name = other.name;
        this.placeId = other.placeId;
        this.address = other.address;
        this.lat = other.lat;
        this.lng = other.lng;
        this.rating = other.rating;
        this.userRatingsTotal = other.userRatingsTotal;
        this.score = other.score;
        this.detourMinutes = other.detourMinutes;
        this.businessStatus = other.businessStatus; // Added
        this.openNow = other.openNow;                     // Added
    }

    

    /**
     * Constructor for basic mapping from Google Places API
     */
   /* public ScenicSpot(String name, String id, double rating, int userRatingsTotal, double lat, double lng) {
        this.name = name;
        this.placeId = id;
        this.rating = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.lat = lat;
        this.lng = lng;
    }

    public ScenicSpot(String name, String id, String address, double rating, int userRatingsTotal, double lat, double lng) {
        this.name = name;
        this.placeId = id;
        this.address = address;
        this.rating = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Constructor for manual creation with address
     
    public ScenicSpot(String name, String address, double lat, double lng) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }

    // --- HELPER METHODS ---
    /**
     * Alias for detourMinutes to support your existing service logic
     *
     * @return
     */
    /*public long getDetour() {
        return this.detourMinutes;
    }

    public String getPlaceId() {
        return this.placeId;
    }

    public void setDetour(long detourMinutes) {
        this.detourMinutes = detourMinutes;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Custom equals/hashCode to ensure deduplication only looks at placeId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScenicSpot that = (ScenicSpot) o;
        return Objects.equals(placeId, that.placeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeId);
    }
}
