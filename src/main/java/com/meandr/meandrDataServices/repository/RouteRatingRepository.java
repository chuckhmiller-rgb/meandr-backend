package com.meandr.meandrDataServices.repository;

import com.meandr.meandrDataServices.model.RouteRating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRatingRepository extends JpaRepository<RouteRating, Long> {
    List<RouteRating> findByRouteIdAndUserIdOrderByCreatedAtDesc(Long routeId, Long userId);
}