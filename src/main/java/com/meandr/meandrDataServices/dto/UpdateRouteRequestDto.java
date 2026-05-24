package com.meandr.meandrDataServices.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdateRouteRequestDto {
    private String masterPolyline;
    private List<StopDto> stops;
    private List<StopDto> rejectedStops;

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
    }
}