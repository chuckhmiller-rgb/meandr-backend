package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.dto.BeautifiedRouteResponseDto;
import com.meandr.meandrDataServices.dto.BeautifyRequestDto;
import com.meandr.meandrDataServices.dto.RerouteRequestDto;
import com.meandr.meandrDataServices.model.ScenicSpot;
import com.meandr.meandrDataServices.osm.model.OsmSearchRequest.LatLng;
import com.meandr.meandrDataServices.service.RouteBeautifierService;
import com.meandr.meandrDataServices.service.RouteBeautifierService.RoutingResultWithWaypoints;
import com.meandr.meandrDataServices.service.MapBoxRoutingService;
import com.meandr.meandrDataServices.service.MapBoxRoutingService.MapBoxRoute;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
public class RouteBeautifierController {

    private final RouteBeautifierService routeBeautifierService;
    private final MapBoxRoutingService mapBoxRoutingService;

    

    @GetMapping("/segment-estimate")
    public ResponseEntity<Map<String, Object>> getSegmentEstimate(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng) {

        List<MapBoxRoute> routes = mapBoxRoutingService.getRouteAlternatives(originLat, originLng, destLat, destLng);

        if (routes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Could not calculate segment estimate"));
        }

        MapBoxRoute fastest = routes.get(0);
        double durationMins = fastest.getDuration() / 60.0;
        double distanceMiles = fastest.getDistance() / 1609.34;

        return ResponseEntity.ok(Map.of(
                "durationMins", Math.round(durationMins),
                "distanceMiles", Math.round(distanceMiles * 10.0) / 10.0
        ));
    }

    @Operation(summary = "Beautify route from origin and destination")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                            name = "ATL to Asheville Example",
                            value = """
                {
                  "origin": { "lat": 33.749, "lng": -84.388 },
                  "destination": { "lat": 35.595, "lng": -82.554 },
                  "routeEnhancementThreshold": 30,
                  "radius": 5000,
                  "entityPreferences": [
                    "national_park",
                    "state_park",
                    "hiking_area",
                    "observation_deck",
                    "historical_landmark",
                    "cultural_landmark",
                    "historical_place",
                    "monument",
                    "sculpture",
                    "garden",
                    "wildlife_refuge"
                  ],
                  "avoidHighways": false,
                  "dwellTimePerStop": 5
                }
                """
                    )
            )
    )
    @PostMapping(value = "/beautify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BeautifiedRouteResponseDto> beautifyRoute(
            @RequestBody BeautifyRequestDto request) throws Exception {

        log.debug("Request body: {}", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request));
        log.info("Beautifying route: origin={},{} dest={},{} enhancement={}% avoidHighways={} avoidTolls={} excludeOrigin={} excludeDest={} entityPreferences{} includeKeywords={}, excludeKeywords={}",
                request.getOrigin().getLat(),
                request.getOrigin().getLng(),
                request.getDestination().getLat(),
                request.getDestination().getLng(),
                request.getRouteEnhancementThreshold(),
                request.isAvoidHighways(),
                request.isAvoidTolls(),
                request.isExcludeOrigin(),
                request.isExcludeDest(),
                request.getEntityPreferences(),
                request.getIncludeKeywords(),
                request.getExcludeKeywords(),
                request.getRestStopCadence());

        BeautifiedRouteResponseDto response = routeBeautifierService.beautifyRouteWithScenicRoads(
                request.getOrigin(),
                request.getDestination(),
                request.getRouteEnhancementThreshold(),
                request.getRadius(),
                request.getEntityPreferences(),
                request.isAvoidHighways(),
                request.isAvoidTolls(),
                request.isExcludeOrigin(),
                request.isExcludeDest(),
                request.getRestStopCadence(),
                request.getDwellTimePerStop(),
                request.getSelectedRouteCoords(),
                request.getIncludeKeywords(),
                request.getExcludeKeywords()
        );

        // Echo request parameters back in response
        response.setOriginName(request.getOriginName());
        response.setDestinationName(request.getDestinationName());
        response.setAvoidHighways(request.isAvoidHighways());
        response.setAvoidTolls(request.isAvoidTolls());
        response.setExcludeOrigin(request.isExcludeOrigin());
        response.setExcludeDest(request.isExcludeDest());
        response.setEntityPreferences(request.getEntityPreferences());
        response.setIncludeKeywords(request.getIncludeKeywords());
        response.setExcludeKeywords(request.getExcludeKeywords());
        response.setDwellTimePerStop(request.getDwellTimePerStop());
        response.setRouteEnhancementThreshold(request.getRouteEnhancementThreshold());

        log.debug("response is: " + response.toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reroute")
    public ResponseEntity<?> reroute(@RequestBody RerouteRequestDto request) {
        try {
            Map<String, Object> result = routeBeautifierService.routeWithWaypoints(
                    request.getOrigin(),
                    request.getDestination(),
                    request.getWaypoints().stream().map(w -> {
                        ScenicSpot s = new ScenicSpot();
                        s.setLat(w.getLat());
                        s.setLng(w.getLng());
                        s.setPlaceId(w.getPlaceId());
                        s.setScore(100.0);
                        return s;
                    }).collect(Collectors.toList()),
                    request.isAvoidHighways(),
                    request.isAvoidTolls()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reroute failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
