package com.meandr.meandrDataServices.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class UpdateRouteRequestDto {
    private String masterPolyline;
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
        private Double distFromStart;
    }
}