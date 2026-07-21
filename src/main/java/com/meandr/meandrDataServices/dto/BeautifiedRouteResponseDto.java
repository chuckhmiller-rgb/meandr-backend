package com.meandr.meandrDataServices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meandr.meandrDataServices.model.ScenicSpot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeautifiedRouteResponseDto {

    private int waypointCount;
    private String masterPolyline;
    private String routeDebugUrl;
    private List<ScenicSpot> selectedWaypoints;
    private List<ScenicSpot> rejectedWaypoints;
    private List<RouteStepSummaryDto> beautifiedRoute;
    private String originName;
    private String destinationName;
    private int dwellTimePerStop;
    private String restStopCadence;
    private double routeEnhancementThreshold;
    private boolean avoidHighways;
    private boolean avoidTolls;
    private boolean excludeOrigin;
    private boolean excludeDest;
    private List<String> entityPreferences;
    private List<String> includeKeywords;
    private List<String> excludeKeywords;
    private TripSummary summary;
    private String warningMessage;
    private List<Map<String, Double>> restStopZones = new ArrayList<>();

    //private List<RestStopDto> restStops;
    /**
     * Single constructor — covers both standard and enhanced routes.
     */
    public BeautifiedRouteResponseDto(
            int waypointCount,
            String masterPolyline,
            String routeDebugUrl,
            List<ScenicSpot> selectedWaypoints,
            List<ScenicSpot> rejectedWaypoints,
            List<RouteStepSummaryDto> beautifiedRoute,
            double totalDetourMins,
            long originalDurationMins,
            String restStopCadence,
            double routeEnhancementThreshold,
            String warningMessage,
            List<Map<String, Double>> restStopZones) {

        this.waypointCount = waypointCount;
        this.masterPolyline = masterPolyline;
        this.routeDebugUrl = routeDebugUrl;
        this.restStopCadence = restStopCadence;
        this.selectedWaypoints = selectedWaypoints;
        this.rejectedWaypoints = rejectedWaypoints;
        this.beautifiedRoute = beautifiedRoute;
        this.warningMessage = warningMessage;
        this.restStopZones = restStopZones;

        double enhancementBudgetMins = originalDurationMins * (routeEnhancementThreshold / 100.0);

        this.summary = new TripSummary(
                originalDurationMins,
                totalDetourMins,
                enhancementBudgetMins,
                waypointCount,
                routeEnhancementThreshold
        );
    }
}
