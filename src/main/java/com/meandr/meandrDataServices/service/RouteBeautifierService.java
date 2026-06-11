package com.meandr.meandrDataServices.service;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;
import com.meandr.meandrDataServices.config.DebugConfig;
import com.meandr.meandrDataServices.config.MeandrConstants;
import com.meandr.meandrDataServices.controller.GoogleApiProxyController;
import com.meandr.meandrDataServices.dto.*;
import com.meandr.meandrDataServices.model.ScenicSpot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

import com.meandr.meandrDataServices.osm.service.OsmService;
import com.meandr.meandrDataServices.osm.model.OsmSearchRequest;
import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import com.meandr.meandrDataServices.osm.model.OsmPlace;
import com.meandr.meandrDataServices.scoring.WaypointScoringService;
import com.meandr.meandrDataServices.scoring.ScoredWaypoint;
import com.meandr.meandrDataServices.scoring.GooglePlaceCandidate;
import com.meandr.meandrDataServices.util.GooglePlacesTypeMapper;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteBeautifierService {

    private final GoogleApiProxyController googleProxy;
    private final OsmService osmService;
    private final WaypointScoringService waypointScoringService;
    private final GeoApiContext context;

    private static final double MIN_QUALITY_SCORE = 25;

    // FIX 2: Cap candidates per segment to prevent urban density monopoly
    private static final int MAX_CANDIDATES_PER_SEGMENT = 10;
    private static final Map<String, Integer> MIN_REVIEWS = Map.ofEntries(
            // Major institutions — high bar
            Map.entry("university", 100),
            Map.entry("stadium", 100),
            Map.entry("aquarium", 100),
            Map.entry("zoo", 100),
            Map.entry("amusement_park", 100),
            // Museums & culture — medium-high bar
            Map.entry("museum", 50),
            Map.entry("art_gallery", 25),
            Map.entry("performing_arts_theater", 25),
            Map.entry("historical_landmark", 25),
            Map.entry("cultural_landmark", 25),
            // Food & drink — low bar
            Map.entry("restaurant", 15),
            Map.entry("fast_food_restaurant", 15),
            Map.entry("cafe", 10),
            Map.entry("bar", 10),
            Map.entry("bakery", 10),
            // Parks & outdoors — very low bar (remote areas have few reviews)
            Map.entry("national_park", 10),
            Map.entry("park", 5),
            Map.entry("campground", 5),
            Map.entry("hiking_area", 5),
            // Civic — medium bar
            Map.entry("courthouse", 25),
            Map.entry("city_hall", 25),
            Map.entry("town_square", 25),
            // Worship — higher bar due to member reviews
            Map.entry("church", 50),
            Map.entry("synagogue", 40),
            Map.entry("mosque", 10),
            Map.entry("hindu_temple", 10),
            // Lodging & gas — low bar
            Map.entry("lodging", 10),
            Map.entry("gas_station", 5),
            // Tourist attractions — medium bar
            Map.entry("tourist_attraction", 35),
            Map.entry("dog_park", 10),
            Map.entry("botanical_garden", 25)
    );

    LatLng originPoint;
    LatLng destinationPoint;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    /**
     * Inner class to hold both routing result and the waypoints that were
     * actually used.
     */
    @Data
    @AllArgsConstructor
    public static class RoutingResultWithWaypoints {

        private String polyline;
        private String debugUrl;
        private List<RouteStepSummaryDto> steps;
        private List<ScenicSpot> actualWaypoints;
    }

    /**
     * Decode Google polyline into list of coordinates.
     */
    private List<CoordinateDto> decodePolylineToCoordinates(String encodedPolyline) {
        List<com.google.maps.model.LatLng> decoded
                = com.google.maps.internal.PolylineEncoding.decode(encodedPolyline);

        return decoded.stream()
                .map(point -> new CoordinateDto(point.lat, point.lng))
                .collect(Collectors.toList());
    }

    public BeautifiedRouteResponseDto beautifyRouteWithScenicRoads(
            CoordinateDto origin,
            CoordinateDto dest,
            double routeEnhancementThreshold,
            int radius,
            List<String> entityPreferences,
            boolean avoidHighways,
            boolean avoidTolls,
            boolean excludeOrigin,
            boolean excludeDest,
            int dwellTimePerStop,
            List<List<Double>> selectedRouteCoords,
            List<String> includeKeywords,
            List<String> excludeKeywords
    ) throws Exception {

        log.info("Beautifying route: enhancementThreshold={}, avoidHighways={}, avoidTolls={}, excludeOrigin={}, excludeDest={}, hasSelectedCoords={}",
                routeEnhancementThreshold, avoidHighways, avoidTolls, excludeOrigin, excludeDest,
                selectedRouteCoords != null && !selectedRouteCoords.isEmpty());

        if (selectedRouteCoords != null && !selectedRouteCoords.isEmpty()) {
            log.info("selectedRouteCoords received: size={}, first={},{}, middle={},{}",
                    selectedRouteCoords.size(),
                    selectedRouteCoords.get(0).get(1), selectedRouteCoords.get(0).get(0),
                    selectedRouteCoords.get(selectedRouteCoords.size() / 2).get(1),
                    selectedRouteCoords.get(selectedRouteCoords.size() / 2).get(0));
        }

        List<CoordinateDto> routeCoords;
        long baselineDurationMins;
        int meandrFactorBase = 250;
        String encodedPolyline;

        if (selectedRouteCoords != null && !selectedRouteCoords.isEmpty()) {
            // Use pre-selected route coordinates from frontend
            routeCoords = selectedRouteCoords.stream()
                    .map(c -> new CoordinateDto(c.get(1), c.get(0))) // GeoJSON is [lng, lat]
                    .collect(Collectors.toList());
            baselineDurationMins = estimateDuration(routeCoords);
            encodedPolyline = encodePolyline(routeCoords);
            log.info("Using pre-selected route: {} coords, estimated {} mins", routeCoords.size(), baselineDurationMins);

        } else {
            // Fetch routes from Directions API
            com.google.maps.model.LatLng googleOrigin = new com.google.maps.model.LatLng(origin.getLat(), origin.getLng());
            com.google.maps.model.LatLng googleDest = new com.google.maps.model.LatLng(dest.getLat(), dest.getLng());

            // Always fetch fastest route for reference
            DirectionsResult fastestResult;
            try {
                fastestResult = DirectionsApi.newRequest(context)
                        .origin(googleOrigin).destination(googleDest)
                        .alternatives(false).mode(TravelMode.DRIVING).await();

            } catch (com.google.maps.errors.ZeroResultsException e) {
                throw new RuntimeException("No fastest route found between origin and destination.", e);
            }

            if (fastestResult.routes == null || fastestResult.routes.length == 0) {
                throw new RuntimeException("No fastest route found between origin and destination");
            }

            long fastestRouteMins = fastestResult.routes[0].legs[0].duration.inSeconds / 60;
            log.info("Fastest route duration: {} mins", fastestRouteMins);

            // Fetch base route with restrictions
            DirectionsApiRequest baseRequest = DirectionsApi.newRequest(context)
                    .origin(googleOrigin).destination(googleDest)
                    .alternatives(true).mode(TravelMode.DRIVING);

            List<DirectionsApi.RouteRestriction> restrictions = new ArrayList<>();
            if (avoidHighways) {
                restrictions.add(DirectionsApi.RouteRestriction.HIGHWAYS);
            }
            if (avoidTolls) {
                restrictions.add(DirectionsApi.RouteRestriction.TOLLS);
            }
            if (!restrictions.isEmpty()) {
                baseRequest.avoid(restrictions.toArray(new DirectionsApi.RouteRestriction[0]));
            }

            DirectionsResult baseResult = baseRequest.await();
            if (baseResult.routes == null || baseResult.routes.length == 0) {
                throw new RuntimeException("No base routes with restrictions found for beautification");
            }

            try {
                baseResult = DirectionsApi.newRequest(context)
                        .origin(googleOrigin).destination(googleDest)
                        .alternatives(false).mode(TravelMode.DRIVING).await();
            } catch (com.google.maps.errors.ZeroResultsException e) {
                throw new RuntimeException("No base routes with restrictions found between origin and destination.", e);
            }

            if (fastestResult.routes == null || fastestResult.routes.length == 0) {
                throw new RuntimeException("No base routes with restrictions found between origin and destination");
            }

            baselineDurationMins = baseResult.routes[0].legs[0].duration.inSeconds / 60;
            log.info("Base route with restrictions duration: {} mins (fastest was {} mins)", baselineDurationMins, fastestRouteMins);

            double enhancementPct = (routeEnhancementThreshold / baselineDurationMins) * 100.0;
            double maxAcceptableMins = baselineDurationMins * (1 + enhancementPct / 100.0);

            com.google.maps.model.DirectionsRoute selectedRoute = Arrays.stream(baseResult.routes)
                    .filter(r -> (r.legs[0].duration.inSeconds / 60.0) <= maxAcceptableMins)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(String.format(
                    "No routes available within your enhancement budget of %.0f mins.", maxAcceptableMins)));

            long totalSeconds = Arrays.stream(selectedRoute.legs)
                    .mapToLong(l -> l.duration.inSeconds)
                    .sum();
            log.info("Selected route: {} mins", totalSeconds / 60);

            String fullPolyline = concatenateLegsPolyline(selectedRoute);
            routeCoords = decodePolylineToCoordinates(fullPolyline);
            encodedPolyline = fullPolyline;
        }

        double enhancementPct = routeEnhancementThreshold;
        log.info("Enhancement: {}% of base route", String.format("%.1f", enhancementPct));
        log.info("Decoded route into {} coordinate points", routeCoords.size());

        return beautifyRoute(routeCoords, baselineDurationMins, enhancementPct,
                radius, entityPreferences, excludeOrigin, excludeDest, dwellTimePerStop, encodedPolyline, avoidHighways, avoidTolls, includeKeywords, excludeKeywords);
    }

    public Map<String, Object> routeWithWaypoints(
            CoordinateDto origin,
            CoordinateDto dest,
            List<ScenicSpot> waypoints,
            boolean avoidHighways,
            boolean avoidTolls) throws Exception {

        com.google.maps.model.LatLng googleOrigin
                = new com.google.maps.model.LatLng(origin.getLat(), origin.getLng());
        com.google.maps.model.LatLng googleDest
                = new com.google.maps.model.LatLng(dest.getLat(), dest.getLng());

        List<ScenicSpot> mutableWaypoints = new ArrayList<>(waypoints);

        while (!mutableWaypoints.isEmpty()) {
            DirectionsApiRequest request = DirectionsApi.newRequest(context)
                    .origin(googleOrigin)
                    .destination(googleDest)
                    .mode(TravelMode.DRIVING);

            List<DirectionsApi.RouteRestriction> restrictions = new ArrayList<>();
            if (avoidHighways) {
                restrictions.add(DirectionsApi.RouteRestriction.HIGHWAYS);
            }
            if (avoidTolls) {
                restrictions.add(DirectionsApi.RouteRestriction.TOLLS);
            }
            if (!restrictions.isEmpty()) {
                request.avoid(restrictions.toArray(new DirectionsApi.RouteRestriction[0]));
            }

            String[] waypointStrings = mutableWaypoints.stream()
                    .map(s -> s.getPlaceId() != null && !s.getPlaceId().isEmpty()
                    ? "place_id:" + s.getPlaceId()
                    : s.getLat() + "," + s.getLng())
                    .toArray(String[]::new);
            request.waypoints(waypointStrings);

            try {
                DirectionsResult result = request.await();
                if (result.routes.length > 0) {
                    String polyline = concatenateLegsPolyline(result.routes[0]);
                    long totalSeconds = Arrays.stream(result.routes[0].legs)
                            .mapToLong(l -> l.duration.inSeconds)
                            .sum();
                    return Map.of(
                            "encodedPolyline", polyline,
                            "durationMins", totalSeconds / 60.0
                    );
                }
            } catch (com.google.maps.errors.ZeroResultsException e) {
                ScenicSpot removed = mutableWaypoints.stream()
                        .min(Comparator.comparingDouble(ScenicSpot::getScore))
                        .get();
                mutableWaypoints.remove(removed);
                log.warn("Reroute: removed lowest-scoring waypoint '{}'. {} remaining.",
                        removed.getName(), mutableWaypoints.size());
            }
        }
        throw new RuntimeException("Could not find a valid route with any waypoints");
    }

    private long estimateDuration(List<CoordinateDto> coords) {
        double totalKm = 0;
        for (int i = 0; i < coords.size() - 1; i++) {
            totalKm += haversine(coords.get(i).getLat(), coords.get(i).getLng(),
                    coords.get(i + 1).getLat(), coords.get(i + 1).getLng());
        }
        return (long) (totalKm / 80.0 * 60.0);
    }

    private String encodePolyline(List<CoordinateDto> coords) {
        List<com.google.maps.model.LatLng> points = coords.stream()
                .map(c -> new com.google.maps.model.LatLng(c.getLat(), c.getLng()))
                .collect(Collectors.toList());
        return com.google.maps.internal.PolylineEncoding.encode(points);
    }

    /**
     * Encode coordinates to Google polyline format.
     */
    /**
     * Generate Google Maps debug URL for scenic route.
     */
    /**
     * Main beautifyRoute method.
     */
    /**
     * Core beautification engine. Fed Google route geometry, scores and selects
     * scenic waypoints within the enhancement budget.
     *
     * @param routeCoords
     * @param baselineDurationMins
     * @param routeEnhancementThreshold
     * @param radius
     * @param entityPreferences
     * @param dwellTimePerStop
     * @param encodedPolyline
     * @param excludeOrigin
     * @param excludeDest
     * @param avoidHighways
     * @param avoidTolls
     * @return
     */
    public BeautifiedRouteResponseDto beautifyRoute(
            List<CoordinateDto> routeCoords,
            long baselineDurationMins,
            double routeEnhancementThreshold,
            int radius,
            List<String> entityPreferences,
            boolean excludeOrigin,
            boolean excludeDest,
            int dwellTimePerStop,
            String encodedPolyline,
            boolean avoidHighways,
            boolean avoidTolls,
            List<String> includeKeywords,
            List<String> excludeKeywords
    ) {
        List<LatLng> path = routeCoords.stream()
                .map(coord -> new LatLng(coord.getLat(), coord.getLng()))
                .collect(Collectors.toList());

        double totalPathLength = calculateTotalPathLength(path);
        originPoint = path.get(0);
        destinationPoint = path.get(path.size() - 1);

        // Increase sample density — aim for one search every ~25km
        int samplingStep = Math.max(1, (int) (path.size() / (totalPathLength / 25.0)));
        log.info("Sampling: {} points, step={}, ~{} km between samples",
                path.size(), samplingStep, String.format("%.1f", totalPathLength / (path.size() / (double) samplingStep)));
        int dynamicRadius = Math.min(10000, (int) (radius * (1 + (routeEnhancementThreshold / 1000.0))));

        List<ScenicSpot> candidates = findScenicSpotsAlongPath(
                path,
                samplingStep,
                dynamicRadius,
                entityPreferences,
                totalPathLength,
                destinationPoint,
                (int) routeEnhancementThreshold,
                includeKeywords,
                excludeKeywords
        );

        List<ScenicSpot> topCandidates = getEscalatedSelection(
                candidates,
                totalPathLength,
                baselineDurationMins,
                routeEnhancementThreshold,
                dwellTimePerStop,
                excludeOrigin,
                excludeDest,
                entityPreferences,
                routeCoords,
                includeKeywords,
                excludeKeywords
        );

        log.info("Selected {} waypoints from {} candidates", topCandidates.size(), candidates.size());

        // Attempt routing with self-healing, fall back to polyline-only on failure
        RoutingResultWithWaypoints routing;
        try {
            routing = fetchBeautifiedPathDetails(originPoint, destinationPoint, topCandidates, avoidHighways, avoidTolls);
            log.info("Routed with {} waypoints ({} removed during self-healing)",
                    routing.getActualWaypoints().size(),
                    topCandidates.size() - routing.getActualWaypoints().size());
        } catch (Exception e) {
            log.error("Routing failed, returning polyline-only result: {}", e.getMessage());
            routing = new RoutingResultWithWaypoints(encodedPolyline, "", new ArrayList<>(), topCandidates);
        }

        List<ScenicSpot> actualWaypoints = routing.getActualWaypoints();

        Set<String> routedIds = actualWaypoints.stream()
                .map(ScenicSpot::getPlaceId)
                .collect(Collectors.toSet());

        // Ensure all candidates have a score before building rejected list
        for (ScenicSpot spot : candidates) {
            if (spot.getScore() == 0) {
                spot.setScore(spot.getRating() * Math.log(Math.max(1, spot.getUserRatingsTotal())));
            }
        }

        List<ScenicSpot> rejectedWaypoints = candidates.stream()
                .filter(spot -> !routedIds.contains(spot.getPlaceId()))
                .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                .collect(Collectors.toList());

        double totalDetourMins = actualWaypoints.stream()
                .mapToDouble(s -> s.getDetour() + dwellTimePerStop)
                .sum();

        double actualEnhancement = baselineDurationMins > 0
                ? (totalDetourMins / baselineDurationMins) * 100.0
                : 0.0;

        double enhancementBudgetMins = baselineDurationMins * (routeEnhancementThreshold / 100.0);

        String warningMessage = null;
        if (Math.abs(actualEnhancement - routeEnhancementThreshold) > 15) {
            warningMessage = String.format(
                    "Could not meet your enhancement target of %.0f%%. "
                    + "Delivered %.1f%% enhancement (%.0f of %.0f mins budget used).",
                    routeEnhancementThreshold,
                    actualEnhancement,
                    totalDetourMins,
                    enhancementBudgetMins
            );
            log.warn(warningMessage);
        }

        return new BeautifiedRouteResponseDto(
                actualWaypoints.size(),
                routing.getPolyline().isEmpty() ? encodedPolyline : routing.getPolyline(),
                routing.getDebugUrl(),
                actualWaypoints,
                rejectedWaypoints,
                routing.getSteps(),
                totalDetourMins,
                baselineDurationMins,
                routeEnhancementThreshold,
                warningMessage
        );
    }

    /**
     * Fetch route details with self-healing that removes problematic waypoints.
     * Google Directions API hard limit: 23 intermediate waypoints (+ origin +
     * dest = 25 total). Self-healing removes lowest-scoring waypoint on
     * rejection, not highest-detour.
     */
    /**
     * Fetch route details with self-healing that removes problematic waypoints.
     * Google Directions API hard limit: 23 intermediate waypoints (+ origin +
     * dest = 25 total). Self-healing removes lowest-scoring waypoint on
     * rejection, not highest-detour.
     */
    private RoutingResultWithWaypoints fetchBeautifiedPathDetails(
            LatLng origin,
            LatLng dest,
            List<ScenicSpot> waypoints,
            boolean avoidHighways,
            boolean avoidTolls
    ) throws Exception {

        waypoints.forEach(s -> log.info("Waypoint: {} placeId={}", s.getName(), s.getPlaceId()));

        com.google.maps.model.LatLng googleOrigin
                = new com.google.maps.model.LatLng(origin.lat, origin.lng);
        com.google.maps.model.LatLng googleDest
                = new com.google.maps.model.LatLng(dest.lat, dest.lng);

        while (!waypoints.isEmpty()) {
            DirectionsApiRequest request = DirectionsApi.newRequest(context)
                    .origin(googleOrigin)
                    .destination(googleDest)
                    .mode(TravelMode.DRIVING);

            List<DirectionsApi.RouteRestriction> restrictions = new ArrayList<>();
            if (avoidHighways) {
                restrictions.add(DirectionsApi.RouteRestriction.HIGHWAYS);
            }
            if (avoidTolls) {
                restrictions.add(DirectionsApi.RouteRestriction.TOLLS);
            }
            if (!restrictions.isEmpty()) {
                request.avoid(restrictions.toArray(new DirectionsApi.RouteRestriction[0]));
            }

            String[] waypointStrings = waypoints.stream()
                    .map(s -> s.getPlaceId() != null && !s.getPlaceId().isEmpty()
                    ? "place_id:" + s.getPlaceId()
                    : s.getLat() + "," + s.getLng())
                    .toArray(String[]::new);
            request.waypoints(waypointStrings);

            try {
                DirectionsResult result = request.await();
                if (result.routes.length > 0) {
                    log.info("Routing succeeded with {} waypoints", waypoints.size());
                    return new RoutingResultWithWaypoints(
                            concatenateLegsPolyline(result.routes[0]),
                            generateDebugUrl(origin, dest, waypoints),
                            processSteps(result),
                            waypoints
                    );
                }
            } catch (com.google.maps.errors.ZeroResultsException e) {
                // Remove lowest-scoring waypoint and retry
                ScenicSpot removed = waypoints.stream()
                        .min(Comparator.comparingDouble(ScenicSpot::getScore))
                        .get();
                waypoints.remove(removed);
                log.warn("Google rejected route — removed lowest-scoring waypoint '{}' (score={}). {} remaining.",
                        removed.getName(), String.format("%.1f", removed.getScore()), waypoints.size());
            }
        }

        // Last attempt: direct route with no waypoints
        try {
            List<DirectionsApi.RouteRestriction> fallbackRestrictions = new ArrayList<>();
            if (avoidHighways) {
                fallbackRestrictions.add(DirectionsApi.RouteRestriction.HIGHWAYS);
            }
            if (avoidTolls) {
                fallbackRestrictions.add(DirectionsApi.RouteRestriction.TOLLS);
            }

            DirectionsApiRequest fallbackRequest = DirectionsApi.newRequest(context)
                    .origin(googleOrigin)
                    .destination(googleDest)
                    .mode(TravelMode.DRIVING);
            if (!fallbackRestrictions.isEmpty()) {
                fallbackRequest.avoid(fallbackRestrictions.toArray(new DirectionsApi.RouteRestriction[0]));
            }

            DirectionsResult result = fallbackRequest.await();
            if (result.routes.length > 0) {
                log.warn("Fell back to direct route — all waypoints rejected");
                return new RoutingResultWithWaypoints(
                        concatenateLegsPolyline(result.routes[0]),
                        generateDebugUrl(origin, dest, new ArrayList<>()),
                        processSteps(result),
                        new ArrayList<>()
                );
            }
        } catch (Exception ex) {
            log.error("Direct route fallback also failed: {}", ex.getMessage());
        }
        log.error("Could not generate route even after removing all waypoints");
        return new RoutingResultWithWaypoints("", "", new ArrayList<>(), new ArrayList<>());
    }

    public List<ScenicSpot> getEscalatedSelection(
            List<ScenicSpot> allFoundSpots,
            double totalPathLength,
            long originalTripMins,
            double routeEnhancementThreshold,
            int dwellTimePerStop,
            boolean excludeOrigin,
            boolean excludeDest,
            List<String> entityPreferences,
            List<CoordinateDto> routeCoords,
            List<String> includeKeywords,
            List<String> excludeKeywords
    ) {
        double totalTimeBudget = originalTripMins * (routeEnhancementThreshold / 100.0);
        int numSegments = Math.max(2, Math.min(10, (int) (totalPathLength / 60.0)));
        double segmentLength = totalPathLength / numSegments;

        double avgStopCostMins = 5.0 + (totalPathLength / 3000.0) * 15.0;
        double minSegmentBudget = avgStopCostMins + dwellTimePerStop;
        double budgetPerSegment = Math.max(minSegmentBudget, totalTimeBudget / numSegments);

        double[] segmentBudget = new double[numSegments];
        double[] segmentSpent = new double[numSegments];
        Arrays.fill(segmentBudget, budgetPerSegment);

        List<ScenicSpot> finalSelection = new ArrayList<>();
        Set<String> selectedPlaceIds = new HashSet<>();
        int[] segmentCount = new int[numSegments];

        log.info("=== ESCALATED SELECTION START ===");
        log.info("Route: totalLength={} km, baseTrip={} mins, enhancement={}%, totalBudget={} mins",
                String.format("%.1f", totalPathLength), originalTripMins,
                routeEnhancementThreshold, String.format("%.1f", totalTimeBudget));
        log.info("Segments: count={}, {} km each, {} mins budget each",
                numSegments, String.format("%.1f", segmentLength), String.format("%.1f", budgetPerSegment));
        log.info("Candidates: {} total spots to evaluate", allFoundSpots.size());

        // Exclude stops near origin/destination
        if (excludeOrigin || excludeDest) {
            final double EXCLUDE_RADIUS_KM = 40.0;
            final double originLat = originPoint.lat;
            final double originLng = originPoint.lng;
            final double destLat = destinationPoint.lat;
            final double destLng = destinationPoint.lng;
            allFoundSpots = allFoundSpots.stream()
                    .filter(wp -> {
                        if (excludeOrigin && haversineKm(wp.getLat(), wp.getLng(), originLat, originLng) < EXCLUDE_RADIUS_KM) {
                            return false;
                        }
                        if (excludeDest && haversineKm(wp.getLat(), wp.getLng(), destLat, destLng) < EXCLUDE_RADIUS_KM) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        log.info("After exclude filter: {} candidates remain (excludeOrigin={}, excludeDest={})",
                allFoundSpots.size(), excludeOrigin, excludeDest);

        final List<ScenicSpot> frozenSpots = Collections.unmodifiableList(allFoundSpots);

        // Build preferred Google types
        Set<String> preferredGoogleTypes = entityPreferences.stream()
                .flatMap(p -> GooglePlacesTypeMapper.toGoogleTypes(List.of(p)).stream())
                .collect(Collectors.toSet());
        boolean hasStrictPrefs = !preferredGoogleTypes.isEmpty();
        log.info("preferredGoogleTypes: {} hasStrictPrefs={}", preferredGoogleTypes, hasStrictPrefs);

        // ── Source predicates ─────────────────────────────────────────────
        java.util.function.Predicate<ScenicSpot> isKW = wp
                -> wp.getSearchSource() != null && wp.getSearchSource().startsWith("KW");
        java.util.function.Predicate<ScenicSpot> isNB = wp
                -> wp.getSearchSource() != null && wp.getSearchSource().startsWith("NB");
        java.util.function.Predicate<ScenicSpot> isPO = wp
                -> wp.getSearchSource() == null || (!wp.getSearchSource().startsWith("KW") && !wp.getSearchSource().startsWith("NB"));
        java.util.function.Predicate<ScenicSpot> isTypeMatch = wp
                -> preferredGoogleTypes.contains(wp.getEntityType());

        // ── Helper: run one anchor+companion pass for a given anchor predicate ──
        // Finds best anchor per empty segment, clusters KW→NB→PO companions within 10km
        java.util.function.BiConsumer<String, java.util.function.Predicate<ScenicSpot>> runAnchorPass = (passName, anchorFilter) -> {
            log.info("--- {} ---", passName);

            for (int i = 0; i < numSegments; i++) {
                if (segmentCount[i] > 0) {
                    continue; // already has a stop
                }
                final int seg = i;

                double globalSpent = finalSelection.stream().mapToDouble(s -> s.getDetour() + dwellTimePerStop).sum();
                if (globalSpent >= totalTimeBudget) {
                    break;
                }

                java.util.function.Predicate<ScenicSpot> inSeg = wp
                        -> Math.min((int) (wp.getDistFromStart() / segmentLength), numSegments - 1) == seg;

                // Find anchor — KW passes use lower review threshold since niche spots are rare
                int minReviews = passName.contains("0") ? 100 : 1000;

                Optional<ScenicSpot> anchorOpt = frozenSpots.stream()
                        .filter(wp -> wp.getRating() >= 4.5)
                        .filter(wp -> wp.getUserRatingsTotal() >= minReviews)
                        .filter(wp -> !selectedPlaceIds.contains(wp.getPlaceId()))
                        .filter(inSeg)
                        .filter(anchorFilter)
                        .max(Comparator.comparingDouble(ScenicSpot::getScore));

                if (anchorOpt.isEmpty()) {
                    log.info("  {}: seg={} no anchor found", passName, seg);
                    continue;
                }

                ScenicSpot anchor = anchorOpt.get();
                double anchorCost = anchor.getDetour() + dwellTimePerStop;
                if (globalSpent + anchorCost > totalTimeBudget) {
                    log.info("  {}: seg={} anchor {} over budget (cost={}, remaining={})",
                            passName, seg, anchor.getName(),
                            String.format("%.1f", anchorCost),
                            String.format("%.1f", totalTimeBudget - globalSpent));
                    continue;
                }

                anchor.setSegmentIndex(seg);
                if (DebugConfig.SHOW_SELECTION_DEBUG) {
                    String phase = passName.contains("user") ? "P0c"
                            : passName.contains("0.5") ? "P1.5c"
                            : passName.contains("0") ? "P1c"
                            : passName.contains("1") ? "P2c"
                            : "P3c";
                    String src = anchor.getSearchSource() != null ? anchor.getSearchSource() : "";
                    anchor.setSelectionDebugCode(phase + (src.isEmpty() ? "" : "/" + src));
                }
                finalSelection.add(anchor);
                selectedPlaceIds.add(anchor.getPlaceId());
                segmentCount[seg]++;
                segmentSpent[seg] += anchorCost;
                log.info("  {}: seg={} anchor selected: {} (score={}, cost={}, source={})",
                        passName, seg, anchor.getName(),
                        String.format("%.1f", anchor.getScore()),
                        String.format("%.1f", anchorCost),
                        anchor.getSearchSource());

                // Cluster companions: KW first, then NB, then PO
                List<java.util.function.Predicate<ScenicSpot>> companionTiers = List.of(isKW, isNB, isPO);
                for (java.util.function.Predicate<ScenicSpot> tier : companionTiers) {
                    List<ScenicSpot> tierCandidates = frozenSpots.stream()
                            .filter(wp -> wp.getRating() >= 4.3)
                            .filter(wp -> wp.getUserRatingsTotal() >= 500)
                            .filter(wp -> !selectedPlaceIds.contains(wp.getPlaceId()))
                            .filter(wp -> haversineKm(anchor.getLat(), anchor.getLng(), wp.getLat(), wp.getLng()) <= 10.0)
                            .filter(tier)
                            .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                            .collect(Collectors.toList());

                    for (ScenicSpot companion : tierCandidates) {
                        if (segmentCount[seg] >= 3) {
                            break; // max 3 per segment
                        }
                        double gSpent = finalSelection.stream().mapToDouble(s -> s.getDetour() + dwellTimePerStop).sum();
                        if (gSpent >= totalTimeBudget) {
                            break;
                        }
                        double companionCost = companion.getDetour() + dwellTimePerStop;
                        if (gSpent + companionCost > totalTimeBudget) {
                            continue;
                        }

                        companion.setSegmentIndex(seg);
                        if (DebugConfig.SHOW_SELECTION_DEBUG) {
                            String phase = passName.contains("0.5") ? "P0.5c"
                                    : passName.contains("0") ? "P0c"
                                    : passName.contains("1") ? "P1c"
                                    : "P2c";
                            String src = companion.getSearchSource() != null ? companion.getSearchSource() : "";
                            companion.setSelectionDebugCode(phase + (src.isEmpty() ? "" : "/" + src));
                        }
                        finalSelection.add(companion);
                        selectedPlaceIds.add(companion.getPlaceId());
                        segmentCount[seg]++;
                        segmentSpent[seg] += companionCost;
                        log.info("  {}: seg={} companion added: {} (score={}, cost={}, source={})",
                                passName, seg, companion.getName(),
                                String.format("%.1f", companion.getScore()),
                                String.format("%.1f", companionCost),
                                companion.getSearchSource());
                    }
                }
            }
        };

        // =================================================================
// PASS 0: User keyword anchors + (KW→NB→PO companions)
// =================================================================
        java.util.function.Predicate<ScenicSpot> isUserKW = wp
                -> "KW-USER".equals(wp.getSearchSource());

        if (includeKeywords != null && !includeKeywords.isEmpty()) {
            runAnchorPass.accept("PASS 0 (user KW anchors)", isUserKW);
        }

// =================================================================
// PASS 1: KW anchors + (KW→NB→PO companions)
// =================================================================
        runAnchorPass.accept("PASS 1 (KW anchors)", isKW);

// =================================================================
// PASS 1.5: Type-matched anchors + (KW→NB→PO companions)
// =================================================================
        runAnchorPass.accept("PASS 1.5 (type-matched anchors)", isTypeMatch);

// =================================================================
// PASS 2: NB anchors + (KW→NB→PO companions) for empty segments
// =================================================================
        runAnchorPass.accept("PASS 2 (NB anchors)", isNB);

// =================================================================
// PASS 3: PO anchors + (KW→NB→PO companions) for still-empty segments
// =================================================================
        runAnchorPass.accept("PASS 3 (PO anchors)", isPO);

        long pass2Count = finalSelection.size();
        double pass2Spent = finalSelection.stream().mapToDouble(s -> s.getDetour() + dwellTimePerStop).sum();
        log.info("After Pass 0/1/2/3:{} waypoints selected, {}/{} mins budget used ({}%)",
                pass2Count, String.format("%.1f", pass2Spent), String.format("%.1f", totalTimeBudget),
                Math.round((pass2Spent / totalTimeBudget) * 100));

        // =================================================================
        // PASS 3: Budget diffusion — KW exhausted first, then NB, then PO
        // =================================================================
        log.info("--- PASS 3: Budget diffusion ---");

        // Build remaining candidates by source tier
        List<ScenicSpot> remainingKW = allFoundSpots.stream()
                .filter(wp -> !selectedPlaceIds.contains(wp.getPlaceId()))
                .filter(isKW)
                .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                .collect(Collectors.toList());
        List<ScenicSpot> remainingNB = allFoundSpots.stream()
                .filter(wp -> !selectedPlaceIds.contains(wp.getPlaceId()))
                .filter(isNB)
                .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                .collect(Collectors.toList());
        List<ScenicSpot> remainingPO = allFoundSpots.stream()
                .filter(wp -> !selectedPlaceIds.contains(wp.getPlaceId()))
                .filter(isPO)
                .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                .collect(Collectors.toList());

        List<ScenicSpot> diffusionPool = new ArrayList<>();
        diffusionPool.addAll(remainingKW);
        diffusionPool.addAll(remainingNB);
        diffusionPool.addAll(remainingPO);

        for (ScenicSpot candidate : diffusionPool) {
            double globalSpent = finalSelection.stream().mapToDouble(s -> s.getDetour() + dwellTimePerStop).sum();
            if (globalSpent >= totalTimeBudget) {
                break;
            }
            if (finalSelection.size() >= 23) {
                break;
            }
            if (selectedPlaceIds.contains(candidate.getPlaceId())) {
                continue;
            }

            int seg = Math.min((int) (candidate.getDistFromStart() / segmentLength), numSegments - 1);
            if (segmentCount[seg] >= 3) {
                continue;
            }

            double cost = candidate.getDetour() + dwellTimePerStop;
            if (globalSpent + cost > totalTimeBudget) {
                continue;
            }

            if (!isSpaceAvailable(finalSelection, candidate, totalPathLength)) {
                log.info("  Pass 3: skipped (too close): {}", candidate.getName());
                continue;
            }

            candidate.setSegmentIndex(seg);
            if (DebugConfig.SHOW_SELECTION_DEBUG) {
                String src = candidate.getSearchSource() != null ? candidate.getSearchSource() : "";
                candidate.setSelectionDebugCode("P4" + (src.isEmpty() ? "" : "/" + src));
            }
            finalSelection.add(candidate);
            selectedPlaceIds.add(candidate.getPlaceId());
            segmentCount[seg]++;
            segmentSpent[seg] += cost;
            log.info("  Pass 3: added: {} (score={}, cost={}, source={})",
                    candidate.getName(),
                    String.format("%.1f", candidate.getScore()),
                    String.format("%.1f", cost),
                    candidate.getSearchSource());
        }

        double finalSpent = finalSelection.stream().mapToDouble(s -> s.getDetour() + dwellTimePerStop).sum();
        log.info("=== ESCALATED SELECTION COMPLETE ===");
        log.info("Waypoints: {} selected | Actual time cost: {}/{} mins ({}%) | Diffusion added: {}",
                finalSelection.size(),
                String.format("%.1f", finalSpent),
                String.format("%.1f", totalTimeBudget),
                Math.round((finalSpent / totalTimeBudget) * 100),
                finalSelection.size() - pass2Count);

        for (int seg = 0; seg < numSegments; seg++) {
            final int currentSeg = seg;
            log.info("  Segment {}: {} waypoints, {}/{} mins spent",
                    seg,
                    finalSelection.stream().filter(s -> s.getSegmentIndex() == currentSeg).count(),
                    String.format("%.1f", segmentSpent[seg]),
                    String.format("%.1f", segmentBudget[seg]));
        }

        // Corridor filter
        if (routeCoords != null && !routeCoords.isEmpty()) {
            double MAX_CORRIDOR_KM = 35.0;
            List<ScenicSpot> filtered = finalSelection.stream()
                    .filter(spot -> {
                        double dist = minDistanceFromPathKm(spot.getLat(), spot.getLng(), routeCoords);
                        if (dist > MAX_CORRIDOR_KM) {
                            log.info("Corridor filter rejected: {} ({}km off route)", spot.getName(), String.format("%.1f", dist));
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            finalSelection.clear();
            finalSelection.addAll(filtered);
        }

        finalSelection.sort(Comparator.comparingDouble(ScenicSpot::getDistFromStart));
        log.info("Finally selected waypoints: {}", finalSelection.stream()
                .map(s -> s.getName() + " (" + s.getLat() + "," + s.getLng() + ")")
                .collect(Collectors.joining(", ")));
        return finalSelection;
    }

    private double runSegmentSelection(
            int segIndex,
            List<ScenicSpot> candidates,
            double budget,
            List<ScenicSpot> finalSelection,
            int dwellTimePerStop,
            double alreadySpent,
            double totalPathLengthKm,
            String phase // "P1" or "P2"
    ) {
        double spent = alreadySpent;
        log.info("    runSegmentSelection seg={} budget={} alreadySpent={} candidates={}",
                segIndex,
                String.format("%.1f", budget),
                String.format("%.1f", alreadySpent),
                candidates.size());
        for (ScenicSpot spot : candidates) {
            if (finalSelection.contains(spot)) {
                continue;
            }
            if (finalSelection.size() >= 23) {
                break;
            }

            double qualityScore = Math.max(0, (spot.getRating() - 2.5) / 2.5 * 60.0)
                    + Math.min(30.0, Math.log10(spot.getUserRatingsTotal() + 1) / Math.log10(100000) * 30.0);
            if (qualityScore < MIN_QUALITY_SCORE) {
                log.info("      ~ skipped (low quality): {} (quality={})",
                        spot.getName(), String.format("%.1f", qualityScore));
                break;
            }
            double cost = spot.getDetour() + dwellTimePerStop;
            if (spent + cost <= budget) {
                if (isSpaceAvailable(finalSelection, spot, totalPathLengthKm)) {
                    if (DebugConfig.SHOW_SELECTION_DEBUG && spot.getSelectionPhase() == null) {
                        spot.setSelectionPhase(phase);
                        String src = spot.getSearchSource() != null ? spot.getSearchSource() : "";
                        spot.setSelectionDebugCode(phase + (src.isEmpty() ? "" : "/" + src));
                    }
                    finalSelection.add(spot);
                    spent += cost;
                    log.info("      + added: {} (cost={}, spent={}/{})",
                            spot.getName(),
                            String.format("%.1f", cost),
                            String.format("%.1f", spent),
                            String.format("%.1f", budget));
                } else {
                    log.info("      ~ skipped (too close): {}", spot.getName());
                }
            } else {
                log.info("      ~ skipped (over budget): {} (cost={}, would be {}/{})",
                        spot.getName(),
                        String.format("%.1f", cost),
                        String.format("%.1f", spent + cost),
                        String.format("%.1f", budget));
            }
        }
        return spent;
    }

    /**
     * Find scenic spots along the path. Runs Google Places and OSM searches,
     * then scores and interleaves both result sets per segment via
     * WaypointScoringService.
     *
     * FIX 4: Adaptive search radius for sparse zones. FIX 6: Wide destination
     * search when final zone is sparse. FIX 7: Filter zero-rating Places API
     * garbage. FIX 8: Pass entityPreferences as List directly. NEW: OSM
     * integration with correct along-route position via path snapping.
     */
    private List<ScenicSpot> findScenicSpotsAlongPath(
            List<LatLng> path,
            int samplingStep,
            int radius,
            List<String> entityPreferences,
            double totalDist,
            LatLng dest,
            double routeEnhancementThreshold,
            List<String> includeKeywords,
            List<String> excludeKeywords
    ) {

        log.info("findScenicSpotsAlongPath: path size={}, first={},{}, middle={},{}, last={},{}",
                path.size(),
                path.get(0).lat, path.get(0).lng,
                path.get(path.size() / 2).lat, path.get(path.size() / 2).lng,
                path.get(path.size() - 1).lat, path.get(path.size() - 1).lng);
        List<ScenicSpot> candidates = new ArrayList<>();
        Set<String> seenPlaceIds = new HashSet<>();
        List<ScenicSpot> rawGoogleSpots = new ArrayList<>();
        Map<Integer, Integer> zoneResultCounts = new HashMap<>();

        // Split entity prefs into keyword vs non-keyword types (done once, reused in all passes)
        List<String> keywordTypes = entityPreferences.stream()
                .filter(GoogleApiProxyController.ENTITY_KEYWORDS::containsKey)
                .collect(Collectors.toList());
        List<String> nearbyTypes = entityPreferences.stream()
                .filter(e -> !GoogleApiProxyController.ENTITY_KEYWORDS.containsKey(e))
                .collect(Collectors.toList());

        // ── Pass 0: User include keywords ────────────────────────────────────
        if (includeKeywords != null && !includeKeywords.isEmpty()) {
            log.info("Pass 0: running {} user include keywords", includeKeywords.size());
            double odometer2 = 0;
            double lastSearch2 = 0;
            for (int i = 0; i < path.size() - 1; i += samplingStep) {
                LatLng p1 = path.get(i);
                double stepDist = haversine(p1.lat, p1.lng,
                        path.get(Math.min(i + samplingStep, path.size() - 1)).lat,
                        path.get(Math.min(i + samplingStep, path.size() - 1)).lng);
                odometer2 += stepDist;

                if (odometer2 - lastSearch2 >= 15.0) {
                    lastSearch2 = odometer2;
                    for (String keyword : includeKeywords) {
                        List<ScenicSpot> kwResults = googleProxy.searchTextScenic(
                                p1.lat, p1.lng, radius, keyword, new ArrayList<>());
                        for (ScenicSpot spot : kwResults) {
                            if (seenPlaceIds.contains(spot.getPlaceId())) {
                                continue;
                            }

                            // Filter 1: name must contain at least one include keyword
                            boolean nameMatch = includeKeywords.stream()
                                    .anyMatch(kw -> spot.getName().toLowerCase().contains(kw.toLowerCase()));
                            if (!nameMatch) {
                                log.info("Pass 0: name filter rejected: {}", spot.getName());
                                continue;
                            }

                            // Filter 2: exclude commercial entity types
                            if (spot.getEntityType() != null && MeandrConstants.EXCLUDED_PLACE_TYPES.stream()
                                    .anyMatch(t -> spot.getEntityType().toLowerCase().contains(t))) {
                                log.info("Pass 0: type filter rejected: {} ({})", spot.getName(), spot.getEntityType());
                                continue;
                            }

                            // Filter 3: exclude keywords
                            if (excludeKeywords != null && !excludeKeywords.isEmpty()) {
                                String nameLower = spot.getName() != null ? spot.getName().toLowerCase() : "";
                                String addrLower = spot.getAddress() != null ? spot.getAddress().toLowerCase() : "";
                                boolean excluded = excludeKeywords.stream()
                                        .map(String::toLowerCase)
                                        .anyMatch(kw -> nameLower.contains(kw) || addrLower.contains(kw));
                                if (excluded) {
                                    log.info("Pass 0: exclude filter rejected: {}", spot.getName());
                                    continue;
                                }
                            }

                            seenPlaceIds.add(spot.getPlaceId());
                            spot.setDistFromStart(odometer2);
                            spot.setScore(calculateScore(spot, path, totalDist, 10.0, dest));
                            spot.setSearchSource("KW-USER");
                            if (DebugConfig.SHOW_SELECTION_DEBUG) {
                                spot.setSelectionDebugCode("KW-USER");
                            }
                            rawGoogleSpots.add(spot);
                        }
                    }
                }
            }
        }

        // ── Google Places main pass ───────────────────────────────────────────
        double odometer = 0;
        double lastSearchOdometer = 0;
        for (int i = 0; i < path.size() - 1; i += samplingStep) {
            LatLng p1 = path.get(i);
            double stepDist = haversine(p1.lat, p1.lng,
                    path.get(Math.min(i + samplingStep, path.size() - 1)).lat,
                    path.get(Math.min(i + samplingStep, path.size() - 1)).lng);
            odometer += stepDist;

            if (odometer - lastSearchOdometer >= 15.0) {
                lastSearchOdometer = odometer;
                int zoneIndex = (int) (odometer / 60.0);
                int priorZoneResults = zoneResultCounts.getOrDefault(zoneIndex, 0);

                int adaptiveRadius = radius;
                if (priorZoneResults == 0 && odometer > 60.0) {
                    adaptiveRadius = Math.min(radius * 3, 15000);
                    log.debug("Sparse zone {} at {}km — expanding radius to {}",
                            zoneIndex, String.format("%.1f", odometer), adaptiveRadius);
                }

                int added = searchAndCollect(p1.lat, p1.lng, adaptiveRadius,
                        nearbyTypes, keywordTypes, "NB", "KW",
                        odometer, totalDist, routeEnhancementThreshold, dest, path,
                        seenPlaceIds, rawGoogleSpots);
                zoneResultCounts.merge(zoneIndex, added, Integer::sum);
            }
        }

        // ── Wide radius retry for sparse zones ───────────────────────────────
        for (int multiplier : new int[]{2, 3}) {
            odometer = 0;
            lastSearchOdometer = 0;
            for (int i = 0; i < path.size() - 1; i += samplingStep) {
                LatLng p1 = path.get(i);
                double stepDist = haversine(p1.lat, p1.lng,
                        path.get(Math.min(i + samplingStep, path.size() - 1)).lat,
                        path.get(Math.min(i + samplingStep, path.size() - 1)).lng);
                odometer += stepDist;

                if (odometer - lastSearchOdometer >= 15.0) {
                    lastSearchOdometer = odometer;
                    int zoneIndex = (int) (odometer / 60.0);
                    if (zoneResultCounts.getOrDefault(zoneIndex, 0) > 0) {
                        continue;
                    }

                    int wideRadius = Math.min(radius * multiplier, 50000);
                    log.info("Wide retry x{} at {}km zone {} — radius={}",
                            multiplier, String.format("%.1f", odometer), zoneIndex, wideRadius);

                    int added = searchAndCollect(p1.lat, p1.lng, wideRadius,
                            nearbyTypes, keywordTypes, "NB-WR", "KW-WR",
                            odometer, totalDist, routeEnhancementThreshold, dest, path,
                            seenPlaceIds, rawGoogleSpots);
                    zoneResultCounts.merge(zoneIndex, added, Integer::sum);
                }
            }
        }

        // ── Destination wide-radius search ───────────────────────────────────
        int destZoneIndex = (int) (totalDist / 60.0);
        if (zoneResultCounts.getOrDefault(destZoneIndex, 0) < 3) {
            int destRadius = Math.min(radius * 4, 25000);
            log.info("Destination zone sparse — wide search near dest (radius={})", destRadius);
            List<ScenicSpot> destNearby = googleProxy.searchNearbyScenic(
                    dest.lat, dest.lng, destRadius, entityPreferences);
            if (DebugConfig.SHOW_SELECTION_DEBUG) {
                destNearby.forEach(s -> s.setSearchSource("NB-DEST"));
            }
            for (ScenicSpot spot : destNearby) {
                if (spot.getRating() == 0 && spot.getUserRatingsTotal() == 0) {
                    continue;
                }
                if (seenPlaceIds.contains(spot.getPlaceId())) {
                    continue;
                }
                seenPlaceIds.add(spot.getPlaceId());
                spot.setDistFromStart(totalDist);
                spot.setScore(calculateScore(spot, path, totalDist, 10.0, dest));
                rawGoogleSpots.add(spot);
            }
        }

        // ── Convert Google spots → GooglePlaceCandidate ───────────────────────
        List<GooglePlaceCandidate> googleCandidates = rawGoogleSpots.stream().map(s -> {
            GooglePlaceCandidate c = new GooglePlaceCandidate();
            c.setPlaceId(s.getPlaceId());
            c.setName(s.getName());
            c.setLatitude(s.getLat());
            c.setLongitude(s.getLng());
            c.setAddress(s.getAddress());
            c.setRating(s.getRating());
            c.setUserRatingCount(s.getUserRatingsTotal());
            c.setDetourMinutes((double) s.getDetour());
            c.setDistFromStart(s.getDistFromStart());
            c.setEntityType(s.getEntityType());
            c.setOpeningHoursJson(s.getOpeningHoursJson());
            c.setSelectionDebugCode(s.getSearchSource());
            c.setSearchSource(s.getSearchSource());
            return c;
        }).collect(Collectors.toList());

        // ── Score & interleave per segment ────────────────────────────────────
        int numSegments = Math.max(2, Math.min(10, (int) (totalDist / 60.0)));
        double segmentLength = totalDist / numSegments;

        Map<Integer, List<GooglePlaceCandidate>> googleBySegment = new HashMap<>();
        for (int i = 0; i < numSegments; i++) {
            googleBySegment.put(i, new ArrayList<>());
        }
        for (GooglePlaceCandidate c : googleCandidates) {
            int seg = Math.min((int) (c.getDistFromStart() / segmentLength), numSegments - 1);
            googleBySegment.get(seg).add(c);
        }

        Map<Integer, List<OsmPlace>> osmBySegment = new HashMap<>();
        for (int i = 0; i < numSegments; i++) {
            osmBySegment.put(i, new ArrayList<>());
        }

        Set<String> addedIds = new HashSet<>();
        for (int seg = 0; seg < numSegments; seg++) {
            List<ScoredWaypoint> scored = waypointScoringService.scoreAndInterleave(
                    googleBySegment.get(seg), osmBySegment.get(seg), entityPreferences, 50);
            for (ScoredWaypoint sw : scored) {
                String uid = sw.getId() != null ? sw.getId() : sw.getName();
                if (uid == null || addedIds.contains(uid)) {
                    continue;
                }
                addedIds.add(uid);
                ScenicSpot spot = new ScenicSpot();
                spot.setName(sw.getName());
                spot.setAddress(sw.getAddress() != null ? sw.getAddress() : "");
                spot.setPlaceId(sw.getId() != null ? sw.getId() : "osm_" + sw.getName().hashCode());
                spot.setLat(sw.getLatitude() != null ? sw.getLatitude() : 0.0);
                spot.setLng(sw.getLongitude() != null ? sw.getLongitude() : 0.0);
                spot.setOpeningHoursJson(sw.getOpeningHoursJson());
                spot.setRating(sw.getRating() != null ? sw.getRating() : 0.0);
                spot.setUserRatingsTotal(sw.getUserRatingCount() != null ? sw.getUserRatingCount() : 0);
                spot.setScore(sw.getScore());
                spot.setDetour(sw.getDetourMinutes() != null ? sw.getDetourMinutes().intValue() : 0);
                spot.setEntityType(sw.getEntityType());
                spot.setDistFromStart(sw.getDistFromStart() != null ? sw.getDistFromStart() : 0.0);
                spot.setSelectionDebugCode(sw.getSelectionDebugCode());
                spot.setSearchSource(sw.getSearchSource());
                candidates.add(spot);
            }
        }

        log.info("findScenicSpotsAlongPath: {} total candidates (Google={}, segments={})",
                candidates.size(), googleCandidates.size(), numSegments);
        return candidates;
    }

    private int searchAndCollect(
            double lat, double lng, int searchRadius,
            List<String> nearbyTypes, List<String> keywordTypes,
            String nearbyTag, String keywordTag,
            double odometer, double totalDist, double routeEnhancementThreshold, LatLng dest, List<LatLng> path,
            Set<String> seenPlaceIds, List<ScenicSpot> rawGoogleSpots) {

        // Fetch nearby and keyword results
        List<ScenicSpot> nearbyResults = nearbyTypes.isEmpty()
                ? Collections.emptyList()
                : googleProxy.searchNearbyScenic(lat, lng, searchRadius, nearbyTypes);

        List<ScenicSpot> keywordResults = new ArrayList<>();
        Map<String, String> placeIdToKeyword = new HashMap<>();
        for (String entityType : keywordTypes) {
            String kw = GoogleApiProxyController.ENTITY_KEYWORDS.get(entityType);
            List<String> googleTypes = GooglePlacesTypeMapper.toGoogleTypes(List.of(entityType));
            List<ScenicSpot> kwSpots = googleProxy.searchTextScenic(lat, lng, searchRadius, kw, googleTypes);
            kwSpots.forEach(s -> placeIdToKeyword.put(s.getPlaceId(), kw));
            keywordResults.addAll(kwSpots);
        }

        // Tag debug source before merging
        if (DebugConfig.SHOW_SELECTION_DEBUG) {
            nearbyResults.forEach(s -> s.setSearchSource(nearbyTag));
            keywordResults.forEach(s -> s.setSearchSource(keywordTag));
        }

        // Merge and post-filter by actual distance — searchText locationBias is a soft hint
        // and can return results from anywhere in the country
        double searchRadiusKm = searchRadius / 1000.0;
        List<ScenicSpot> allNearby = Stream.concat(nearbyResults.stream(), keywordResults.stream())
                .filter(spot -> haversineKm(spot.getLat(), spot.getLng(), lat, lng) <= searchRadiusKm)
                .collect(Collectors.toList());

        // Post-filter keyword results by name — reject if none of the keyword terms
        // appear in the place name (guards against Google searchText fuzzy matching)
        allNearby = allNearby.stream()
                .filter(spot -> {
                    String kw = placeIdToKeyword.get(spot.getPlaceId());
                    if (kw == null) {
                        return true; // not a keyword result, keep it
                    }
                    String[] terms = kw.toLowerCase().split("\\s+");
                    String nameLower = spot.getName().toLowerCase();
                    for (String term : terms) {
                        if (nameLower.contains(term)) {
                            return true;
                        }
                    }
                    log.debug("Keyword name filter rejected: {} (keyword={})", spot.getName(), kw);
                    return false;
                })
                .collect(Collectors.toList());

        // Filter, score and collect qualified spots
        LatLng p1 = new LatLng(lat, lng);
        int qualifiedCount = 0;
        for (ScenicSpot spot : allNearby) {
            if (spot.getRating() == 0 && spot.getUserRatingsTotal() == 0) {
                continue;
            }
            int minReviews = MIN_REVIEWS.getOrDefault(spot.getEntityType(), 5);
            if (spot.getUserRatingsTotal() < minReviews) {
                continue;
            }
            if (seenPlaceIds.contains(spot.getPlaceId())) {
                continue;
            }
            seenPlaceIds.add(spot.getPlaceId());
            spot.setDistFromStart(odometer);

            // Source bonus — prioritizes user-selected types over fill candidates
            double sourceBonus = 0;
            if (spot.getSearchSource() != null) {
                sourceBonus = switch (spot.getSearchSource()) {
                    case "KW", "KW-WR" ->
                        30.0;
                    case "NB", "NB-WR" ->
                        20.0;
                    case "NB-DEST" ->
                        10.0;
                    default ->
                        0.0;
                };
            }

            spot.setScore(calculateScore(spot, path, totalDist, sourceBonus, dest));
            rawGoogleSpots.add(spot);
            if (DebugConfig.SHOW_SELECTION_DEBUG) {
                log.info("Qualified spot: {} at {},{} distFromStart={}km source={}",
                        spot.getName(), spot.getLat(), spot.getLng(),
                        String.format("%.1f", odometer), spot.getSearchSource());
            }
            qualifiedCount++;
        }
        return qualifiedCount;
    }

    private double calculateScore(
            ScenicSpot spot, List<LatLng> path, double totalDist, double sourceBonus, LatLng dest) {

// Find nearest path point for accurate detour calculation
        double minDistKm = Double.MAX_VALUE;
        for (LatLng point : path) {
            double d = haversineKm(spot.getLat(), spot.getLng(), point.lat, point.lng);
            if (d < minDistKm) {
                minDistKm = d;
            }
        }

        int estimatedDetourMins = (int) ((minDistKm * 2.0 / 80.0) * 60.0);
        spot.setDetour(estimatedDetourMins);

        double baseRating = spot.getRating() > 0 ? spot.getRating() : 2.5;
        double ratingScore = Math.max(0, (baseRating - 2.5) / 2.5 * 60.0);
        double popularityScore = Math.min(30.0, Math.log10(spot.getUserRatingsTotal() + 1) / Math.log10(100000) * 30.0);
        double distToDest = haversineKm(spot.getLat(), spot.getLng(), dest.lat, dest.lng);
        double arrivalBonus = (1.0 - (distToDest / totalDist)) * 5.0;
        double detourPenalty = estimatedDetourMins * 0.2;
        double score = ratingScore + popularityScore + arrivalBonus + sourceBonus - detourPenalty;
        return Math.max(0, score);
    }

    /**
     * Check if there's space for a new waypoint. Min distance scales with route
     * length — short routes use 1.5km floor, longer routes spread stops more
     * evenly (e.g. ~12km for a 600km route).
     */
    private boolean isSpaceAvailable(List<ScenicSpot> existing, ScenicSpot candidate, double totalPathLengthKm) {
        double minDistKm = Math.min(30.0, Math.max(1.5, totalPathLengthKm / 50.0));  // ← cap at 30km
        for (ScenicSpot s : existing) {
            double dist = haversine(s.getLat(), s.getLng(), candidate.getLat(), candidate.getLng());
            if (dist < minDistKm) {
                log.debug("      ~ too close to {}: {}km < {}km min",
                        s.getName(), String.format("%.1f", dist), String.format("%.1f", minDistKm));
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate total path length in km.
     */
    private double calculateTotalPathLength(List<LatLng> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            total += haversine(path.get(i).lat, path.get(i).lng,
                    path.get(i + 1).lat, path.get(i + 1).lng);
        }
        return total;
    }

    private String concatenateLegsPolyline(DirectionsRoute route) {
        List<com.google.maps.model.LatLng> allPoints = new ArrayList<>();
        for (DirectionsLeg leg : route.legs) {
            for (DirectionsStep step : leg.steps) {
                allPoints.addAll(step.polyline.decodePath());
            }
        }
        return new com.google.maps.model.EncodedPolyline(allPoints).getEncodedPath();
    }

    /**
     * Haversine distance calculation.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Generate Google Maps debug URL.
     */
    private String generateDebugUrl(LatLng origin, LatLng dest, List<ScenicSpot> waypoints) {
        StringBuilder url = new StringBuilder("https://www.google.com/maps/embed/v1/directions?");
        url.append("key=").append(googleMapsApiKey);
        url.append("&origin=").append(origin.lat).append(",").append(origin.lng);
        url.append("&destination=").append(dest.lat).append(",").append(dest.lng);
        url.append("&mode=driving");

        if (waypoints != null && !waypoints.isEmpty()) {
            String waypointStr = waypoints.stream()
                    .map(w -> (w.getName() + ", " + w.getAddress())
                    .replace(" ", "+")
                    .replace(",", "%2C")
                    .replace("&", "%26")
                    .replace("(", "%28")
                    .replace(")", "%29"))
                    .collect(Collectors.joining("|"));
            url.append("&waypoints=").append(waypointStr);
        }

        return url.toString();
    }

    private double minDistanceFromPathKm(double lat, double lng, List<CoordinateDto> path) {
        double minDist = Double.MAX_VALUE;
        for (CoordinateDto point : path) {
            double d = haversineKm(lat, lng, point.getLat(), point.getLng());
            if (d < minDist) {
                minDist = d;
            }
        }
        return minDist;
    }

    /**
     * Process steps from DirectionsResult.
     */
    private List<RouteStepSummaryDto> processSteps(DirectionsResult result) {
        List<RouteStepSummaryDto> steps = new ArrayList<>();

        if (result.routes != null && result.routes.length > 0) {
            var legs = result.routes[0].legs;

            for (var leg : legs) {
                for (var step : leg.steps) {
                    RouteStepSummaryDto stepDto = new RouteStepSummaryDto(
                            step.htmlInstructions,
                            step.distance.humanReadable,
                            step.duration.humanReadable
                    );
                    steps.add(stepDto);
                }
            }
        }

        return steps;
    }

    /**
     * Simple LatLng helper class.
     */
    @Data
    @AllArgsConstructor
    public static class LatLng {

        public double lat;
        public double lng;
    }
}
