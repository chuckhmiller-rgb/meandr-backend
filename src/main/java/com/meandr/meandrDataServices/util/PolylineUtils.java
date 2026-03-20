/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.util;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.meandr.meandrDataServices.model.ScenicSpot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author chuck
 */
@Slf4j
public class PolylineUtils {

    @Value("${google.api.key}")
    private String apiKey;

    /*public static List<LatLng> decodePolyline6(List<CoordinateDto> encoded) {

        log.info("Decoding Polyline ...");

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            // The divisor is 1e6 for Polyline6 (Mapbox default)
            poly.add(new LatLng(lat / 1e6, lng / 1e6));
        }
        return poly;
    }

    public DirectionsResult getBeautifiedRoute(String origin, String destination, List<String> scenicPlaceIds) {

        log.info("Beautifying route for route -> origin = " + origin + " and destination = " + destination);

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();

        DirectionsApiRequest request = DirectionsApi.newRequest(context)
                .origin(origin)
                .destination(destination)
                .mode(TravelMode.DRIVING);

        // Convert your scenic Place IDs into Waypoints
        if (scenicPlaceIds != null && !scenicPlaceIds.isEmpty()) {
            request.waypoints(scenicPlaceIds.toArray(new String[0]));
            // Optimization makes sure the "scenic" route doesn't double-back on itself
            request.optimizeWaypoints(true);
        }

        try {
            return request.await();
        } catch (ApiException | IOException | InterruptedException e) {
            log.error("Failed to calculate beautified route with exception " + e.toString());
            throw new RuntimeException("Failed to calculate beautified route", e);
        }
    }*/

}
