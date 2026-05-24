package com.meandr.meandrDataServices.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meandr.meandrDataServices.dto.SaveRouteRequestDto;
import com.meandr.meandrDataServices.dto.SaveRouteRequestDto.StopDto;
import com.meandr.meandrDataServices.dto.UpdateRouteRequestDto;
import com.meandr.meandrDataServices.dto.UserRouteSummaryDto;
import com.meandr.meandrDataServices.model.RouteStop;
import com.meandr.meandrDataServices.model.UserRoute;
import com.meandr.meandrDataServices.repository.UserRouteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/user-routes")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "https://meandr-app.vercel.app")
public class UserRouteController {

    private final UserRouteRepository userRouteRepository;

    // Save a new route (temporary by default)
    @PostMapping
    public ResponseEntity<UserRoute> saveRoute(@RequestBody SaveRouteRequestDto request) throws JsonProcessingException {
        UserRoute route = UserRoute.builder()
                .userName(request.getUserName())
                .routeName(request.getRouteName() != null ? request.getRouteName()
                        : request.getOriginName() + " → " + request.getDestinationName())
                .originName(request.getOriginName())
                .destinationName(request.getDestinationName())
                .originLat(request.getOriginLat())
                .originLng(request.getOriginLng())
                .destLat(request.getDestLat())
                .destLng(request.getDestLng())
                .masterPolyline(request.getMasterPolyline())
                .baseTripMins(request.getBaseTripMins())
                .addedMins(request.getAddedMins())
                .mf(request.getMf())
                .avoidHighways(request.getAvoidHighways())
                .avoidTolls(request.getAvoidTolls())
                .excludeOrigin(request.getExcludeOrigin())
                .excludeDest(request.getExcludeDest())
                .entityPreferences(request.getEntityPreferences() != null
                        ? new ObjectMapper().writeValueAsString(request.getEntityPreferences())
                        : null)
                .rejectedStops(request.getRejectedStops() != null
                        ? new ObjectMapper().writeValueAsString(request.getRejectedStops())
                        : null)
                .isSaved(false)
                .build();

        // Add stops
        if (request.getStops() != null) {
            List<RouteStop> stops = IntStream.range(0, request.getStops().size())
                    .mapToObj(i -> {
                        SaveRouteRequestDto.StopDto s = request.getStops().get(i);
                        RouteStop stop = new RouteStop();
                        stop.setStopOrder(i);
                        stop.setPlaceId(s.getPlaceId());
                        stop.setPlaceName(s.getPlaceName());
                        stop.setPlaceAddress(s.getPlaceAddress());
                        stop.setPlaceLat(s.getPlaceLat());
                        stop.setPlaceLon(s.getPlaceLon());
                        stop.setEntityType(s.getEntityType());
                        stop.setDetourMins(s.getDetourMins());
                        stop.setRating(s.getRating());
                        stop.setReviewsTotal(s.getReviewsTotal());
                        stop.setRoute(route);
                        return stop;
                    })
                    .toList();
            route.setStops(stops);
        }

        UserRoute saved = userRouteRepository.save(route);
        log.info("Saved route {} for user {}", saved.getId(), saved.getUserName());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{userName}/summary")
    public ResponseEntity<List<UserRouteSummaryDto>> getRouteSummaries(@PathVariable String userName) {
        List<UserRoute> routes = userRouteRepository.findByUserNameOrderByCreatedAtDesc(userName);
        List<UserRouteSummaryDto> summaries = routes.stream().map(r -> UserRouteSummaryDto.builder()
                .id(r.getId())
                .routeName(r.getRouteName())
                .originName(r.getOriginName())
                .destinationName(r.getDestinationName())
                .baseTripMins(r.getBaseTripMins())
                .addedMins(r.getAddedMins())
                .isSaved(r.getIsSaved())
                .createdAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .stopCount(r.getStops() != null ? r.getStops().size() : 0)
                .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    // Get all routes for a user (recent + saved)
    @GetMapping("/{userName}")
    public ResponseEntity<List<UserRoute>> getRoutes(@PathVariable String userName) {
        return ResponseEntity.ok(userRouteRepository.findByUserNameOrderByCreatedAtDesc(userName));
    }

    // Get only saved routes
    @GetMapping("/{userName}/saved")
    public ResponseEntity<List<UserRoute>> getSavedRoutes(@PathVariable String userName) {
        return ResponseEntity.ok(userRouteRepository.findByUserNameAndIsSavedTrueOrderByCreatedAtDesc(userName));
    }

    @GetMapping("/route/{id}")
    public ResponseEntity<UserRoute> getRoute(@PathVariable Long id) {
        return userRouteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Promote a route to saved (permanent)
    @PatchMapping("/{id}/save")
    public ResponseEntity<UserRoute> promoteToSaved(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Optional<UserRoute> opt = userRouteRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UserRoute route = opt.get();
        route.setIsSaved(true);
        route.setExpiresAt(null);
        if (body.containsKey("routeName")) {
            route.setRouteName(body.get("routeName"));
        }
        return ResponseEntity.ok(userRouteRepository.save(route));
    }

    // Delete a route
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        userRouteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Cleanup expired routes (call periodically)
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanup() {
        userRouteRepository.deleteExpiredRoutes(LocalDateTime.now());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/update-route")
    @Transactional
    public ResponseEntity<UserRoute> updateRoute(
            @PathVariable Long id,
            @RequestBody UpdateRouteRequestDto body) throws JsonProcessingException {
        Optional<UserRoute> opt = userRouteRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UserRoute route = opt.get();

        if (body.getMasterPolyline() != null) {
            route.setMasterPolyline(body.getMasterPolyline());
        }
        if (body.getRejectedStops() != null) {
            route.setRejectedStops(new ObjectMapper().writeValueAsString(body.getRejectedStops()));
        }
        // Replace stops
        route.getStops().clear(); // orphanRemoval handles the deletes
        if (body.getStops() != null) {
            List<RouteStop> newStops = IntStream.range(0, body.getStops().size())
                    .mapToObj(i -> {
                        UpdateRouteRequestDto.StopDto s = body.getStops().get(i);
                        RouteStop stop = new RouteStop();
                        stop.setStopOrder(i);
                        stop.setPlaceId(s.getPlaceId());
                        stop.setPlaceName(s.getPlaceName());
                        stop.setPlaceLat(s.getPlaceLat());
                        stop.setPlaceLon(s.getPlaceLon());
                        stop.setRoute(route);
                        stop.setEntityType(s.getEntityType());
                        stop.setPlaceAddress(s.getPlaceAddress());
                        stop.setDetourMins(s.getDetourMins());
                        stop.setRating(s.getRating());
                        stop.setReviewsTotal(s.getReviewsTotal());
                        return stop;
                    }).toList();
            route.getStops().addAll(newStops);
        }
        return ResponseEntity.ok(userRouteRepository.save(route));
    }
}
