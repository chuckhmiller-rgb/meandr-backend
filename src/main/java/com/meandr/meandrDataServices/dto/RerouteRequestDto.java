package com.meandr.meandrDataServices.dto;

import lombok.Data;
import java.util.List;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class RerouteRequestDto {

    private CoordinateDto origin;
    private CoordinateDto destination;
    private boolean avoidHighways;
    private boolean avoidTolls;
    private List<WaypointDto> waypoints;

    @Data
    public static class WaypointDto {

        private double lat;
        private double lng;
        private String placeId;
    }
}
