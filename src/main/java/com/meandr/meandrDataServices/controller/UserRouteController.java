/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.dto.UserRouteDto;
import com.meandr.meandrDataServices.service.UserRouteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author chuck
 */
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class UserRouteController {

    private final UserRouteService userRouteService;

    // Create a new route
    @PostMapping("/createRoute")
    public ResponseEntity<UserRouteDto> createRoute(@RequestBody UserRouteDto routeDto) {
        UserRouteDto savedRoute = userRouteService.saveRoute(routeDto);
        return new ResponseEntity<>(savedRoute, HttpStatus.CREATED);
    }
    
    @PostMapping("/beautifyRoute")
    public ResponseEntity<UserRouteDto> beautifyRoute(@RequestBody UserRouteDto routeDto) {
        UserRouteDto savedRoute = userRouteService.saveRoute(routeDto);
        return new ResponseEntity<>(savedRoute, HttpStatus.CREATED);
    }

    @PostMapping("/addRouteStop")
    public ResponseEntity<UserRouteDto> addStop(
            @RequestParam String userName,
            @RequestParam String routeName,
            @RequestBody UserRouteDto.RouteStopDto newStop) {

        return ResponseEntity.ok(userRouteService.addStopToRoute(userName, routeName, newStop));
    }

    // Get all routes for a specific user
    @GetMapping("/search/getAllRoutes")
    public ResponseEntity<List<UserRouteDto>> getAllUserRoutes(@RequestParam String userName) {
        List<UserRouteDto> routes = userRouteService.getAllUserRoutes(userName);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/search/getRoute")
    public ResponseEntity<UserRouteDto> getRoute(
            @RequestParam Long id) {
        return ResponseEntity.ok(userRouteService.getRouteById(id));
    }

    @GetMapping("/search/getRouteDetails")
    public ResponseEntity<UserRouteDto> getRouteDetails(
            @RequestParam String userName,
            @RequestParam String routeName) {

        UserRouteDto route = userRouteService.getRouteByNames(userName, routeName);
        return ResponseEntity.ok(route);
    }
}
