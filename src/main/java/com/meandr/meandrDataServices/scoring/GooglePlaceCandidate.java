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
    
    
}
