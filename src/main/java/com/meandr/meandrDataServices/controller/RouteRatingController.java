package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.model.RouteRating;
import com.meandr.meandrDataServices.repository.RouteRatingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/route-ratings")
@RequiredArgsConstructor
public class RouteRatingController {

    private final RouteRatingRepository routeRatingRepository;

    @GetMapping("/{routeId}")
    public ResponseEntity<?> getRatings(@PathVariable Long routeId, @RequestParam Long userId) {
        List<RouteRating> ratings = routeRatingRepository.findByRouteIdAndUserIdOrderByCreatedAtDesc(routeId, userId);
        return ResponseEntity.ok(ratings);
    }

    @PostMapping("/{routeId}")
    public ResponseEntity<?> addRating(@PathVariable Long routeId, @RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();

        RouteRating rating = RouteRating.builder()
                .routeId(routeId)
                .userId(userId)
                .viewsRating(body.get("viewsRating") != null ? ((Number) body.get("viewsRating")).intValue() : null)
                .trafficRating(body.get("trafficRating") != null ? ((Number) body.get("trafficRating")).intValue() : null)
                .experienceRating(body.get("experienceRating") != null ? ((Number) body.get("experienceRating")).intValue() : null)
                .journalEntry((String) body.get("journalEntry"))
                .build();

        RouteRating saved = routeRatingRepository.save(rating);
        return ResponseEntity.ok(saved);
    }
}
