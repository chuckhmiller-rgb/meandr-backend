/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.service;

import com.meandr.meandrDataServices.dto.UserRouteDto;
import com.meandr.meandrDataServices.model.RouteStop;
import com.meandr.meandrDataServices.model.UserRoute;
import com.meandr.meandrDataServices.repository.UserRouteRepository;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author chuck
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserRouteService {

    private final UserRouteRepository userRouteRepository;

    public UserRouteDto saveRoute(UserRouteDto dto) {
        UserRoute route = new UserRoute();
        route.setUserName(dto.getUserName());
        route.setRouteName(dto.getRouteName());

        // Convert DTO stops to Entities
        List<RouteStop> stops = dto.getStops().stream().map(stopDto -> {
            RouteStop stop = new RouteStop();
            //stop.setPlaceId(stopDto.getPlaceID());
            stop.setPlaceId(stopDto.getPlaceId());
            stop.setPlaceName(stopDto.getPlaceName());
            stop.setPlaceLat(stopDto.getPlaceLat());
            stop.setPlaceLon(stopDto.getPlaceLon());
            stop.setRoute(route); // CRITICAL: Link child to parent
            return stop;
        }).collect(Collectors.toList());

        route.setStops(stops);
        UserRoute saved = userRouteRepository.save(route);
        return convertToDto(saved);
    }

    public UserRouteDto addStopToRoute(String userName, String routeName, UserRouteDto.RouteStopDto stopDto) {
        // 1. Find the existing route
        UserRoute route = userRouteRepository.findByUserNameAndRouteName(userName, routeName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        // 2. Convert DTO to Entity
        RouteStop newStop = new RouteStop();
        newStop.setPlaceId(stopDto.getPlaceId());
        newStop.setPlaceName(stopDto.getPlaceName());
        newStop.setPlaceLon(stopDto.getPlaceLat());
        newStop.setPlaceLon(stopDto.getPlaceLon());

        // 3. Link them (Bi-directional)
        newStop.setRoute(route);
        route.getStops().add(newStop);

        // 4. Save the parent (CascadeType.ALL handles the child save)
        UserRoute updatedRoute = userRouteRepository.save(route);

        return convertToDto(updatedRoute);
    }

    public List<UserRouteDto> getAllUserRoutes(String userName) {
        try {
            // 1. Fetch routes from the DB
            // The Repository will automatically JOIN the stops because of @EntityGraph
            List<UserRoute> routes = userRouteRepository.findByuserName(userName);

            // 2. Map the list of Entities to a list of DTOs
            return routes.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // Log the error for debugging
            log.error("Error fetching routes for user: {}", userName, e);
            throw new RuntimeException("Could not retrieve routes: " + e.getMessage());
        }
    }

    public UserRouteDto getRouteByIdAndUser(Long routeId, String userName) {
        UserRoute route = userRouteRepository.findByIdAndUserName(routeId, userName)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Route not found for id: " + routeId + " and user: " + userName
        ));

        return convertToDto(route);
    }

    public UserRouteDto getRouteById(Long routeId) {
        UserRoute route = userRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Route not found for id: " + routeId));

        return convertToDto(route);
    }

    public UserRouteDto getRouteByNames(String userName, String routeName) {
        UserRoute route = userRouteRepository.findByUserNameAndRouteName(userName, routeName)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No route found named '" + routeName + "' for user '" + userName + "'"
        ));

        return convertToDto(route);
    }

    private UserRouteDto convertToDto(UserRoute entity) {
        List<UserRouteDto.RouteStopDto> stopDtos = entity.getStops().stream()
                .map(stop -> UserRouteDto.RouteStopDto.builder()
                .placeId(stop.getPlaceId())
                .placeName(stop.getPlaceName())
                .placeLat(stop.getPlaceLat())
                .placeLon(stop.getPlaceLon())
                .build())
                .collect(Collectors.toList());

        return UserRouteDto.builder()
                //.id(entity.getId())
                .userName(entity.getUserName())
                .routeName(entity.getRouteName())
                .stops(stopDtos)
                .build();
    }

}
