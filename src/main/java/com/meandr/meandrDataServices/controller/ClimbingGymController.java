/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

/**
 *
 * @author chuck
 */
import com.meandr.meandrDataServices.dto.ClimbingGymResponseDto;
import com.meandr.meandrDataServices.model.ClimbingGym;
import com.meandr.meandrDataServices.repository.ClimbingGymRepository;
import com.meandr.meandrDataServices.service.ClimbingGymService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gyms")
@RequiredArgsConstructor
public class ClimbingGymController {

    private final ClimbingGymRepository gymRepository;
    private final ClimbingGymService gymService;

    // 1. Get all active gyms
    @GetMapping("/getAllClimbingGyms")
    public List<ClimbingGym> getAllGyms() {
        return gymRepository.findAll();
    }

    // 2. Create a new gym
    // JSON body should include the "id" object: {"id": {"name": "...", "latitude": ...}, "city": "..."}
    @PostMapping("/createOrUpdateGym")
    public ResponseEntity<ClimbingGym> createOrUpdateGym(@RequestBody ClimbingGym gym) {
        ClimbingGym savedGym = gymRepository.save(gym);
        return new ResponseEntity<>(savedGym, HttpStatus.CREATED);
    }

    // 3. Find by City
    @GetMapping("/searchGymsByCity/{city}")
    public List<ClimbingGym> getGymsByCity(@RequestParam String city) {
        return gymRepository.findByCityIgnoreCase(city);
    }

    // 4. Find by Name
    // Note: Since IDs are complex, we pass them as params or look them up by name/coords
    @GetMapping("/{gymName}")
    public ResponseEntity<List<ClimbingGymResponseDto>> getGymDetails(
            @PathVariable String gymName) {
        return ResponseEntity.ok(gymService.getGymByName(gymName));
    }

    @GetMapping("/getUSGyms")
    public ResponseEntity<List<ClimbingGym>> getAllUSGyms() {
        List<ClimbingGym> usGyms = gymService.getAllGyms().stream()
                .filter(gym -> "USA".equalsIgnoreCase(gym.getCountry())
                || "US".equalsIgnoreCase(gym.getCountry()))
                .toList();
        return ResponseEntity.ok(usGyms);
    }
    
    @GetMapping("/searchGymsByCountry/{country}")
    public List<ClimbingGymResponseDto> searchByCountry(@RequestParam("country") String countryName) {
        // 'countryName' now holds whatever the user typed in the form
        return gymService.getGymsByCountry(countryName);
    }
    
    @GetMapping("/climbingGymsWithin-bbox")
    public ResponseEntity<List<ClimbingGym>> getGymsInBounds(
            @RequestParam double minLon,
            @RequestParam double minLat,
            @RequestParam double maxLon,
            @RequestParam double maxLat) {

        List<ClimbingGym> gyms = gymService.findClimbingGymsByInBoundingBox(
                minLon, minLat, maxLon, maxLat);

        return ResponseEntity.ok(gyms);
    }

    // 5. Bulk Import 
    @PostMapping("/bulkGymJSONImport")
    public ResponseEntity<String> bulkImport(@RequestBody List<ClimbingGym> gyms) {
        gymRepository.saveAll(gyms);
        return ResponseEntity.ok("Successfully imported " + gyms.size() + " gyms.");
    }
}
