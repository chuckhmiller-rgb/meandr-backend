package com.meandr.meandrDataServices.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SaveRouteRequestDto {
    private String userName;
    private String routeName;
    private String originName;
    private String destinationName;
    private Double originLat;
    private Double originLng;
    private Double destLat;
    private Double destLng;
    private String masterPolyline;
    private Integer baseTripMins;
    private Integer addedMins;
    private Integer mf;
    private Boolean avoidHighways;
    private Boolean avoidTolls;
    private Boolean excludeOrigin;
    private Boolean excludeDest;
    private List<String> includeKeywords;
    private List<String> excludeKeywords;
    private List<String> entityPreferences;
    private List<StopDto> stops;
    private List<StopDto> rejectedStops;
    private List<Map<String, Double>> restStopZones;
    private String restStopCadence;

    @Data
    @NoArgsConstructor
    public static class StopDto {
        private String placeId;
        private String placeName;
        private String placeAddress;
        private Float placeLat;
        private Float placeLon;
        private String entityType;
        private Integer detourMins;
        private Double rating;
        private Integer reviewsTotal;
        private String openingHoursJson;
        private String utcOffsetMinutes;
        
    }
}