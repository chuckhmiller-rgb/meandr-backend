package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.dto.BeautifiedRouteResponseDto;
import com.meandr.meandrDataServices.dto.BeautifyRequestDto;
import com.meandr.meandrDataServices.service.RouteBeautifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        log.info("Request body: {}", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request));
        log.info("Beautifying route: origin={},{} dest={},{} enhancement={}% avoidHighways={}",
                request.getOrigin().getLat(), request.getOrigin().getLng(),
                request.getDestination().getLat(), request.getDestination().getLng(),
                request.getRouteEnhancementThreshold(),
                request.isAvoidHighways(),
                request.isAvoidTolls());
        
        

        BeautifiedRouteResponseDto response = beautifierService.beautifyRouteWithScenicRoads(
                request.getOrigin(),
                request.getDestination(),
                request.getRouteEnhancementThreshold(),
                request.getRadius(),
                request.getEntityPreferences(),
                request.isAvoidHighways(),
                request.isAvoidTolls(),
                request.getDwellTimePerStop()
        );

        return ResponseEntity.ok(response);
    }
}