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
    private Integer utcOffsetMinutes;
    public String googlePhoto;
    private int segmentIndex;
    
    /**
     * Tracks how and when this place was found and selected during route
     * beautification.
     *
     * SELECTION STRATEGY — four-pass priority hierarchy: Pass 0: Find best KW
     * anchor per segment (rating >= 4.5, reviews >= 100), then cluster KW → NB
     * → PO companions within 10km. Pass 1: For segments still empty after Pass
     * 0, find best NB anchor (rating >= 4.5, reviews >= 1000), then cluster KW
     * → NB → PO companions. Pass 2: For segments still empty after Pass 1, find
     * best PO anchor (rating >= 4.5, reviews >= 1000), then cluster KW → NB →
     * PO companions. Pass 3: Diffuse remaining budget — exhaust KW spots first
     * across all segments, then NB, then PO.
     *
     * searchSource — always populated, indicates which search method found this
     * place: "NB" = found via searchNearby (standard radius) "KW" = found via
     * searchText keyword (standard radius) "NB-WR" = found via searchNearby
     * wide radius retry "KW-WR" = found via searchText keyword wide radius
     * retry "NB-DEST" = found via searchNearby destination wide search
     *
     * selectionDebugCode — only populated when DebugConfig.SHOW_SELECTION_DEBUG
     * = true. Combines selection pass and search source, e.g.: "P0/KW" = Pass 0
     * KW anchor "P0c/NB" = Pass 0 companion, found via nearby search "P1/NB" =
     * Pass 1 NB anchor "P1c/KW" = Pass 1 companion, found via keyword search
     * "P2/NB-WR" = Pass 2 PO anchor, found via wide radius nearby "P3/KW" =
     * Pass 3 diffusion, found via keyword search
     */
    private String selectionPhase; // "P0", "P0c", "P1", "P2" label coded prefix for ease of test validation
    private String searchSource;   // "NB", "KW" label coded prefix for ease of test validation
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
        this.utcOffsetMinutes = other.utcOffsetMinutes;
        this.googlePhoto = other.googlePhoto;
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
