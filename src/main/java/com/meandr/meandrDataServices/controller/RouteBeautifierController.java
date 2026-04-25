package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.dto.BeautifiedRouteResponseDto;
import com.meandr.meandrDataServices.dto.BeautifyRequestDto;
import com.meandr.meandrDataServices.dto.RerouteRequestDto;
import com.meandr.meandrDataServices.model.ScenicSpot;
import com.meandr.meandrDataServices.osm.model.OsmSearchRequest.LatLng;
import com.meandr.meandrDataServices.service.RouteBeautifierService;
import com.meandr.meandrDataServices.service.RouteBeautifierService.RoutingResultWithWaypoints;
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

import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "https://meandr-app.vercel.app")
@Slf4j
@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
public class RouteBeautifierController {

    public final RouteBeautifierService beautifierService;

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
        log.info("Beautifying route: origin={},{} dest={},{} enhancement={}% avoidHighways={} avoidTolls={} excludeOrigin={} excludeDest={} ",
                request.getOrigin().getLat(), request.getOrigin().getLng(),
                request.getDestination().getLat(), request.getDestination().getLng(),
                request.getRouteEnhancementThreshold(),
                request.isAvoidHighways(),
                request.isAvoidTolls(),
                request.isExcludeOrigin(),
                request.isExcludeDest());

        BeautifiedRouteResponseDto response = beautifierService.beautifyRouteWithScenicRoads(
                request.getOrigin(),
                request.getDestination(),
                request.getRouteEnhancementThreshold(),
                request.getRadius(),
                request.getEntityPreferences(),
                request.isAvoidHighways(),
                request.isAvoidTolls(),
                request.isExcludeOrigin(),
                request.isExcludeDest(),
                request.getDwellTimePerStop(),
                request.getSelectedRouteCoords()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reroute")
    public ResponseEntity<?> reroute(@RequestBody RerouteRequestDto request) {
        try {
            Map<String, Object> result = beautifierService.routeWithWaypoints(
                    request.getOrigin(),
                    request.getDestination(),
                    request.getWaypoints().stream().map(w -> {
                        ScenicSpot s = new ScenicSpot();
                        s.setLat(w.getLat());
                        s.setLng(w.getLng());
                        s.setPlaceId(w.getPlaceId());
                        s.setScore(100.0); // default score so self-healing works
                        return s;
                    }).collect(Collectors.toList())
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reroute failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
