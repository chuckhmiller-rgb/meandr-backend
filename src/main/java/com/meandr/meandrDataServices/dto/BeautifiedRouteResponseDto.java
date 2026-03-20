package com.meandr.meandrDataServices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meandr.meandrDataServices.model.ScenicSpot;
import java.util.List;
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
    private TripSummary summary;
    private String warningMessage;

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
            double routeEnhancementThreshold,
            String warningMessage) {

        this.waypointCount = waypointCount;
        this.masterPolyline = masterPolyline;
        this.routeDebugUrl = routeDebugUrl;
        this.selectedWaypoints = selectedWaypoints;
        this.rejectedWaypoints = rejectedWaypoints;
        this.beautifiedRoute = beautifiedRoute;
        this.warningMessage = warningMessage;

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