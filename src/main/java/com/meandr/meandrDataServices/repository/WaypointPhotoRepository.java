package com.meandr.meandrDataServices.repository;

import com.meandr.meandrDataServices.model.WaypointPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaypointPhotoRepository extends JpaRepository<WaypointPhoto, Long> {
    List<WaypointPhoto> findByPlaceId(String placeId);
    List<WaypointPhoto> findByRouteId(Long routeId);
    long countByPlaceId(String placeId);
    long countByPlaceIdAndUserId(String placeId, Long userId);
    List<WaypointPhoto> findByPlaceIdAndUserId(String placeId, Long userId);
}