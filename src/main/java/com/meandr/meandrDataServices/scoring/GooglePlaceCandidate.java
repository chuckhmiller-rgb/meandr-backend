package com.meandr.meandrDataServices.scoring;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Google Places candidate ready for scoring.
 */

@Data
@NoArgsConstructor

public class GooglePlaceCandidate {

    private String placeId;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String entityType;
    private Double rating;
    private Integer userRatingCount;
    private Double detourMinutes;
    private Double distFromStart;
    private String openingHoursJson;
    private Integer utcOffsetMinutes;
    private String googlePhoto;
    private String nationalPhoneNumber;
    private String internationalPhoneNumber;
        
    
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
    private String searchSource;
    private String selectionDebugCode;

}
