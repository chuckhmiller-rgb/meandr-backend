/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

/**
 *
 * @author chuck
 */
// 5. REST Controller

import com.meandr.meandrDataServices.dto.ParkResponseDto;
import com.meandr.meandrDataServices.model.Park;
import com.meandr.meandrDataServices.repository.ParkRepository;
import com.meandr.meandrDataServices.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/parks")
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;
    private final ParkRepository parkRepository;

    @PostMapping("/createOrUpdatePark")
    public ResponseEntity<Park> createOrUpdatePark(@RequestBody Park park) {
        Park savedPark = parkRepository.save(park);
        return new ResponseEntity<>(savedPark, HttpStatus.CREATED);
    }
    
    @GetMapping("/getAllParks")
    public ResponseEntity<List<ParkResponseDto>> getAllParks() {
        return ResponseEntity.ok(parkService.getAllParks());
    }

    @GetMapping("/{parkName}")
    public ResponseEntity<List<ParkResponseDto>> getParkById(@PathVariable String parkName) {
        return ResponseEntity.ok(parkService.getParkByName(parkName));
    }

    
    @GetMapping("/filterByNational")
    public ResponseEntity<List<ParkResponseDto>> getNationalParks() {
        List<ParkResponseDto> national = parkService.getAllParks().stream()
                .filter(ParkResponseDto::isNationalPark)
                .toList();
        return ResponseEntity.ok(national);
    }
    
    @GetMapping("/filterByState")
    public ResponseEntity<List<ParkResponseDto>> getStateParks() {
        List<ParkResponseDto> state = parkService.getAllParks().stream()
                .filter(ParkResponseDto::isStatePark)
                .toList();
        return ResponseEntity.ok(state);
    }
    
    // 5. Bulk Import 
    @PostMapping("/bulkParkJSONImport")
    public ResponseEntity<String> bulkImport(@RequestBody List<Park> parks) {
        parkRepository.saveAll(parks);
        return ResponseEntity.ok("Successfully imported " + parks.size() + " parks.");
    }
    
    @GetMapping("/parksWithin-bbox")
    public ResponseEntity<List<Park>> getParksInBounds(
            @RequestParam double minLon,
            @RequestParam double minLat,
            @RequestParam double maxLon,
            @RequestParam double maxLat) {

        List<Park> parks = parkService.findParksInBoundingBox(
                minLon, minLat, maxLon, maxLat);

        return ResponseEntity.ok(parks);
    }
}
