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
}