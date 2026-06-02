/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.dto.WaypointPhotoDto;
import com.meandr.meandrDataServices.model.WaypointPhoto;
import com.meandr.meandrDataServices.repository.WaypointPhotoRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Timestamp;
import java.time.LocalDateTime;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author charlesmiller
 */
@CrossOrigin(origins = {
    "https://meandr-app.vercel.app",
    "https://miss-proarmy-nonfraternally.ngrok-free.dev",
    "http://localhost:5173"
})
@RestController
@RequestMapping("/api/v1/waypoint-photos")
@RequiredArgsConstructor
@Slf4j
public class WaypointPhotoController {

    private final WaypointPhotoRepository waypointPhotoRepository;
    private LocalDateTime takenAt;

    @PostMapping
    public ResponseEntity<?> savePhoto(@RequestBody WaypointPhotoDto dto,
            HttpServletRequest request) {

        log.info("WaypointPhoto: received placeId={}, routeId={}, userId={}, userName={}, imageSize={}",
                dto.getPlaceId(), dto.getRouteId(), dto.getUserId(), dto.getUserName(),
                dto.getImageData() != null ? dto.getImageData().length() : 0);
        try {
            byte[] imageData = Base64.getDecoder().decode(dto.getImageData());

            String userName = null;
            if (request.getUserPrincipal() != null) {
                userName = request.getUserPrincipal().getName();
            }

            WaypointPhoto photo = WaypointPhoto.builder()
                    .userId(dto.getUserId())
                    .userName(dto.getUserName())
                    .routeId(dto.getRouteId())
                    .placeId(dto.getPlaceId())
                    .placeName(dto.getName())
                    .imageData(imageData)
                    .takenAt(LocalDateTime.now())
                    .notes(dto.getNotes())
                    .build();

            waypointPhotoRepository.save(photo);
            return ResponseEntity.ok(Map.of("success", true, "id", photo.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<?> getPhotos(@PathVariable String placeId, @RequestParam(required = false) Long userId) {
        List<WaypointPhoto> photos = userId != null
                ? waypointPhotoRepository.findByPlaceIdAndUserId(placeId, userId)
                : waypointPhotoRepository.findByPlaceId(placeId);
        List<Map<String, Object>> result = photos.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("placeId", p.getPlaceId());
            m.put("placeName", p.getPlaceName());
            m.put("imageData", Base64.getEncoder().encodeToString(p.getImageData()));
            m.put("takenAt", p.getTakenAt());
            m.put("userName", p.getUserName());
            m.put("notes", p.getNotes());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        if (!waypointPhotoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        waypointPhotoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/place/{placeId}")
    public ResponseEntity<?> deleteAllForPlace(@PathVariable String placeId) {
        List<WaypointPhoto> photos = waypointPhotoRepository.findByPlaceId(placeId);
        waypointPhotoRepository.deleteAll(photos);
        return ResponseEntity.ok(Map.of("success", true, "deleted", photos.size()));
    }

    @GetMapping("/count/{placeId}")
    public ResponseEntity<?> countPhotos(@PathVariable String placeId, @RequestParam(required = false) Long userId) {
        //long count = waypointPhotoRepository.countByPlaceId(placeId);
        long count = userId != null
                ? waypointPhotoRepository.countByPlaceIdAndUserId(placeId, userId)
                : waypointPhotoRepository.countByPlaceId(placeId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
