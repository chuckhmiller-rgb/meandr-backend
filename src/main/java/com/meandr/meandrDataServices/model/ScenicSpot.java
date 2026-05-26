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
    private String openingHoursJson;
    private int segmentIndex;
    private String selectionPhase; // "P0", "P0c", "P1", "P2" label coded prefix for ease of test validation
    private String searchSource;   // "NB", "KW" label coded prefix for ease of test validation
    /**
     * Debug code for selection phase and search source tracking. Only populated
     * when DebugConfig.SHOW_SELECTION_DEBUG = true. Format: "[phase/source]"
     * e.g.: "NB" = found via searchNearby "KW" = found via searchText keyword
     * "NB-WR" = found via searchNearby wide radius retry "KW-WR" = found via
     * searchText keyword wide radius retry "P0/NB" = Pass 0 anchor, found via
     * searchNearby "P0c/KW" = Pass 0 companion, found via keyword search
     * "P1/NB" = Pass 1 selection, found via searchNearby "P2/KW" = Pass 2
     * diffusion, found via keyword search
     */
    private String selectionDebugCode;
    
    

    // 3. THE COPY CONSTRUCTOR 
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
        this.openingHoursJson = other.openingHoursJson; 
        this.segmentIndex = other.segmentIndex;         
        this.selectionPhase = other.selectionPhase;
        this.selectionDebugCode = other.selectionDebugCode;

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
