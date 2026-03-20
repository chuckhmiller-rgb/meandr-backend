package com.meandr.meandrDataServices.service;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteBeautifierService {

    private final GoogleApiProxyController googleProxy;
    private final OsmService osmService;
    private final WaypointScoringService waypointScoringService;
    private final GeoApiContext context;

    // FIX 2: Cap candidates per segment to prevent urban density monopoly
    private static final int MAX_CANDIDATES_PER_SEGMENT = 10;

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
            int dwellTimePerStop
    ) throws Exception {

        final double maxAdditionalMinutes = routeEnhancementThreshold;

        log.info("Beautifying route: maxAdditionalMinutes={}, avoidHighways={}, avoidTolls={}",
                maxAdditionalMinutes, avoidHighways, avoidTolls);

        com.google.maps.model.LatLng googleOrigin
                = new com.google.maps.model.LatLng(origin.getLat(), origin.getLng());
        com.google.maps.model.LatLng googleDest
                = new com.google.maps.model.LatLng(dest.getLat(), dest.getLng());

        // --- Step 1: Always fetch fastest route for reference ---
        DirectionsResult fastestResult = DirectionsApi.newRequest(context)
                .origin(googleOrigin)
                .destination(googleDest)
                .alternatives(false)
                .mode(TravelMode.DRIVING)
                .await();

        if (fastestResult.routes == null || fastestResult.routes.length == 0) {
            throw new RuntimeException("No baseline route found between origin and destination");
        }

        long fastestRouteMins = fastestResult.routes[0].legs[0].duration.inSeconds / 60;
        log.info("Fastest route duration: {} mins", fastestRouteMins);

        // --- Step 2: Fetch the base route to beautify ---
        DirectionsApiRequest baseRequest = DirectionsApi.newRequest(context)
                .origin(googleOrigin)
                .destination(googleDest)
                .alternatives(true)
                .mode(TravelMode.DRIVING);

        // Build restrictions list dynamically
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
            throw new RuntimeException("No routes found for beautification");
        }

        // --- Step 3: Use the base route duration as the enhancement baseline ---
        long baselineDurationMins = baseResult.routes[0].legs[0].duration.inSeconds / 60;
        log.info("Base route duration (avoidHighways={}, avoidTolls={}): {} mins (fastest was {} mins)",
                avoidHighways, avoidTolls, baselineDurationMins, fastestRouteMins);

        // Convert absolute minutes to percentage of the base route duration
        double enhancementPct = (maxAdditionalMinutes / baselineDurationMins) * 100.0;
        log.info("Enhancement: {} additional mins = {}% of base route",
                maxAdditionalMinutes, String.format("%.1f", enhancementPct));

        // --- Step 4: Select the best route within enhancement budget ---
        double maxAcceptableMins = baselineDurationMins * (1 + enhancementPct / 100.0);

        com.google.maps.model.DirectionsRoute selectedRoute = Arrays.stream(baseResult.routes)
                .filter(r -> (r.legs[0].duration.inSeconds / 60.0) <= maxAcceptableMins)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(
                "No routes available within your enhancement budget of %.0f mins. "
                + "Try increasing your threshold.", maxAcceptableMins)));

        long selectedRouteMins = selectedRoute.legs[0].duration.inSeconds / 60;
        log.info("Selected route: {} mins (base: {} mins, max acceptable: {} mins)",
                selectedRouteMins, baselineDurationMins, maxAcceptableMins);

        // --- Step 5: Decode and beautify ---
        List<CoordinateDto> routeCoords = decodePolylineToCoordinates(
                selectedRoute.overviewPolyline.getEncodedPath()
        );

        log.info("Decoded route into {} coordinate points", routeCoords.size());

        return beautifyRoute(
                routeCoords,
                baselineDurationMins,
                enhancementPct,
                radius,
                entityPreferences,
                dwellTimePerStop,
                selectedRoute.overviewPolyline.getEncodedPath()
        );
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
     * @return
     */
    public BeautifiedRouteResponseDto beautifyRoute(
            List<CoordinateDto> routeCoords,
            long baselineDurationMins,
            double routeEnhancementThreshold,
            int radius,
            List<String> entityPreferences,
            int dwellTimePerStop,
            String encodedPolyline
    ) {
        List<LatLng> path = routeCoords.stream()
                .map(coord -> new LatLng(coord.getLat(), coord.getLng()))
                .collect(Collectors.toList());

        double totalPathLength = calculateTotalPathLength(path);
        LatLng originPoint = path.get(0);
        LatLng destinationPoint = path.get(path.size() - 1);

        int samplingStep = Math.max(1, path.size() / 50);
        int dynamicRadius = Math.min(10000, (int) (radius * (1 + (routeEnhancementThreshold / 1000.0))));

        List<ScenicSpot> candidates = findScenicSpotsAlongPath(
                path,
                samplingStep,
                dynamicRadius,
                entityPreferences,
                totalPathLength,
                destinationPoint,
                (int) routeEnhancementThreshold
        );

        List<ScenicSpot> topCandidates = getEscalatedSelection(
                candidates,
                totalPathLength,
                baselineDurationMins,
                routeEnhancementThreshold,
                dwellTimePerStop
        );

        log.info("Selected {} waypoints from {} candidates", topCandidates.size(), candidates.size());

        // Attempt routing with self-healing, fall back to polyline-only on failure
        RoutingResultWithWaypoints routing;
        try {
            routing = fetchBeautifiedPathDetails(originPoint, destinationPoint, topCandidates);
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
            List<ScenicSpot> waypoints
    ) throws Exception {

        // Enforce Google's 23 waypoint limit upfront, keeping highest-scoring
        List<ScenicSpot> workingWaypoints = waypoints.stream()
                .sorted(Comparator.comparingDouble(ScenicSpot::getScore).reversed())
                .limit(23)
                .sorted(Comparator.comparingDouble(ScenicSpot::getDistFromStart))
                .collect(Collectors.toCollection(ArrayList::new));

        if (workingWaypoints.size() < waypoints.size()) {
            log.info("Capped waypoints from {} to 23 (Google limit), kept highest-scoring",
                    waypoints.size());
        }

        workingWaypoints.forEach(s -> log.info("Waypoint: {} placeId={}", s.getName(), s.getPlaceId()));

        com.google.maps.model.LatLng googleOrigin
                = new com.google.maps.model.LatLng(origin.lat, origin.lng);
        com.google.maps.model.LatLng googleDest
                = new com.google.maps.model.LatLng(dest.lat, dest.lng);

        while (!workingWaypoints.isEmpty()) {
            DirectionsApiRequest request = DirectionsApi.newRequest(context)
                    .origin(googleOrigin)
                    .destination(googleDest)
                    .mode(TravelMode.DRIVING);

            String[] waypointStrings = workingWaypoints.stream()
                    .map(s -> s.getPlaceId() != null && !s.getPlaceId().isEmpty()
                    ? "place_id:" + s.getPlaceId()
                    : s.getLat() + "," + s.getLng())
                    .toArray(String[]::new);
            request.waypoints(waypointStrings);

            try {
                DirectionsResult result = request.await();
                if (result.routes.length > 0) {
                    log.info("Routing succeeded with {} waypoints", workingWaypoints.size());
                    return new RoutingResultWithWaypoints(
                            result.routes[0].overviewPolyline.getEncodedPath(),
                            generateDebugUrl(origin, dest, workingWaypoints),
                            processSteps(result),
                            workingWaypoints
                    );
                }
            } catch (com.google.maps.errors.ZeroResultsException e) {
                // Remove lowest-scoring waypoint and retry
                ScenicSpot removed = workingWaypoints.stream()
                        .min(Comparator.comparingDouble(ScenicSpot::getScore))
                        .get();
                workingWaypoints.remove(removed);
                log.warn("Google rejected route — removed lowest-scoring waypoint '{}' (score={}). {} remaining.",
                        removed.getName(), String.format("%.1f", removed.getScore()), workingWaypoints.size());
            }
        }

        // Last attempt: direct route with no waypoints
        try {
            DirectionsResult result = DirectionsApi.newRequest(context)
                    .origin(googleOrigin)
                    .destination(googleDest)
                    .mode(TravelMode.DRIVING)
                    .await();
            if (result.routes.length > 0) {
                log.warn("Fell back to direct route — all waypoints rejected");
                return new RoutingResultWithWaypoints(
                        result.routes[0].overviewPolyline.getEncodedPath(),
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

    /**
     * Get escalated selection with budget.
     *
     * @param allFoundSpots
     * @param totalPathLength
     * @param originalTripMins
     * @param routeEnhancementThreshold
     * @param dwellTimePerStop
     * @return
     */
    public List<ScenicSpot> getEscalatedSelection(
            List<ScenicSpot> allFoundSpots,
            double totalPathLength,
            long originalTripMins,
            double routeEnhancementThreshold,
            int dwellTimePerStop
    ) {
        double totalTimeBudget = originalTripMins * (routeEnhancementThreshold / 100.0);
        int numSegments = Math.max(2, Math.min(10, (int) (totalPathLength / 60.0)));
        double budgetPerSegment = totalTimeBudget / numSegments;
        double segmentLength = totalPathLength / numSegments;

        log.info("=== ESCALATED SELECTION START ===");
        log.info("Route: totalLength={} km, baseTrip={} mins, enhancement={}%, totalBudget={} mins",
                String.format("%.1f", totalPathLength), originalTripMins,
                routeEnhancementThreshold, String.format("%.1f", totalTimeBudget));
        log.info("Segments: count={}, {} km each, {} mins budget each",
                numSegments, String.format("%.1f", segmentLength), String.format("%.1f", budgetPerSegment));
        log.info("Candidates: {} total spots to evaluate", allFoundSpots.size());

        // Group candidates by segment
        Map<Integer, List<ScenicSpot>> bySegment = new HashMap<>();
        for (int i = 0; i < numSegments; i++) {
            bySegment.put(i, new ArrayList<>());
        }

        for (ScenicSpot spot : allFoundSpots) {
            int seg = Math.min((int) (spot.getDistFromStart() / segmentLength), numSegments - 1);
            spot.setSegmentIndex(seg);
            bySegment.get(seg).add(spot);
            bySegment.get(seg).sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        }

        // FIX 2: Cap candidates per segment to prevent dense urban segments monopolizing selection
        for (int i = 0; i < numSegments; i++) {
            List<ScenicSpot> segCandidates = bySegment.get(i);
            if (segCandidates.size() > MAX_CANDIDATES_PER_SEGMENT) {
                // Already sorted by score descending above — just trim
                bySegment.put(i, new ArrayList<>(segCandidates.subList(0, MAX_CANDIDATES_PER_SEGMENT)));
                log.info("  Segment {}: trimmed from {} to {} candidates (density cap)",
                        i, segCandidates.size(), MAX_CANDIDATES_PER_SEGMENT);
            }
        }

        // Log candidate distribution
        for (int i = 0; i < numSegments; i++) {
            log.info("  Segment {}: [{} km - {} km] — {} candidates",
                    i,
                    String.format("%.1f", i * segmentLength),
                    String.format("%.1f", (i + 1) * segmentLength),
                    bySegment.get(i).size());
        }

        double[] segmentBudget = new double[numSegments];
        double[] segmentSpent = new double[numSegments];
        Arrays.fill(segmentBudget, budgetPerSegment);

        List<ScenicSpot> finalSelection = new ArrayList<>();

        // --- Pass 1: Initial per-segment selection ---
        log.info("--- PASS 1: Initial per-segment selection ---");
        for (int seg = 0; seg < numSegments; seg++) {
            final int currentSeg = seg;
            segmentSpent[seg] = runSegmentSelection(
                    seg, bySegment.get(seg), segmentBudget[seg],
                    finalSelection, dwellTimePerStop, segmentSpent[seg], totalPathLength);
            double consumed = segmentSpent[seg];
            double unspent = segmentBudget[seg] - segmentSpent[seg];
            log.info("  Segment {}: consumed={} mins, unspent={} mins, waypoints selected={}",
                    seg,
                    String.format("%.1f", consumed),
                    String.format("%.1f", unspent),
                    finalSelection.stream().filter(s -> s.getSegmentIndex() == currentSeg).count());
        }

        long pass1Count = finalSelection.size();
        double pass1Spent = Arrays.stream(segmentSpent).sum();
        log.info("Pass 1 complete: {} waypoints selected, {}/{} mins budget used ({}%)",
                pass1Count,
                String.format("%.1f", pass1Spent),
                String.format("%.1f", totalTimeBudget),
                Math.round((pass1Spent / totalTimeBudget) * 100));

        // --- Pass 2: Budget diffusion (BFS outward from unspent segments) ---
        log.info("--- PASS 2: Budget diffusion ---");
        boolean anyDiffused = true;
        int diffusionRound = 0;
        Set<Integer> exhaustedSources = new HashSet<>();

        while (anyDiffused) {
            anyDiffused = false;
            diffusionRound++;

            if (diffusionRound > numSegments * 2) {
                log.warn("Diffusion round limit reached ({}) — breaking to prevent infinite loop", diffusionRound);
                break;
            }

            log.info("  Diffusion round {}:", diffusionRound);

            // Empty segments first, partial segments after
            List<Integer> diffusionSources = new ArrayList<>();
            for (int seg = 0; seg < numSegments; seg++) {
                if (exhaustedSources.contains(seg)) {
                    continue;
                }
                double unspent = segmentBudget[seg] - segmentSpent[seg];
                if (unspent <= 0) {
                    continue;
                }
                if (segmentSpent[seg] == 0) {
                    diffusionSources.add(0, seg);
                    log.info("    Queued segment {} as EMPTY source ({} mins unspent)",
                            seg, String.format("%.1f", unspent));
                } else {
                    diffusionSources.add(seg);
                    log.info("    Queued segment {} as PARTIAL source ({} mins unspent)",
                            seg, String.format("%.1f", unspent));
                }
            }

            if (diffusionSources.isEmpty()) {
                log.info("  No segments with unspent budget — diffusion complete");
                break;
            }

            for (int source : diffusionSources) {
                double unspent = segmentBudget[source] - segmentSpent[source];
                if (unspent <= 0) {
                    continue;
                }

                log.info("    Diffusing from segment {} ({} mins unspent):",
                        source, String.format("%.1f", unspent));

                int maxRadius = numSegments - 1;
                boolean sourceDiffused = false;

                for (int radius = 1; radius <= maxRadius; radius++) {
                    double consumedAtRadius = 0;
                    int[] neighbors = {source - radius, source + radius};

                    for (int neighbor : neighbors) {
                        if (neighbor < 0 || neighbor >= numSegments) {
                            continue;
                        }
                        final int currentNeighbor = neighbor;

                        // Offer the FULL unspent amount — neighbor takes only what it can spend
                        double allocating = unspent;
                        segmentBudget[neighbor] += allocating;

                        log.info("        Segment {}: allocating {} mins (budget now {})",
                                neighbor,
                                String.format("%.1f", allocating),
                                String.format("%.1f", segmentBudget[neighbor]));

                        double before = segmentSpent[neighbor];
                        segmentSpent[neighbor] = runSegmentSelection(
                                neighbor, bySegment.get(neighbor), segmentBudget[neighbor],
                                finalSelection, dwellTimePerStop, segmentSpent[neighbor], totalPathLength);
                        double consumed = segmentSpent[neighbor] - before;
                        consumedAtRadius += consumed;

                        if (consumed > 0) {
                            double unclaimed = allocating - consumed;
                            // FIX 3: Reclaim unused allocation so it doesn't inflate the budget
                            segmentBudget[neighbor] -= unclaimed;
                            unspent -= consumed;
                            anyDiffused = true;
                            sourceDiffused = true;
                            log.info("        Segment {}: consumed {} mins, reclaimed {} mins — now has {} waypoints",
                                    neighbor,
                                    String.format("%.1f", consumed),
                                    String.format("%.1f", unclaimed),
                                    finalSelection.stream()
                                            .filter(s -> s.getSegmentIndex() == currentNeighbor)
                                            .count());
                        } else {
                            // Nothing consumed — reclaim allocation, leave segmentSpent untouched
                            segmentBudget[neighbor] -= allocating;
                            log.info("        Segment {}: consumed nothing — budget fully reclaimed", neighbor);
                        }
                    }

                    if (consumedAtRadius == 0) {
                        log.info("      Radius {} yielded nothing — expanding to radius {}", radius, radius + 1);
                    } else {
                        // FIX 3: Only credit the source segment for what was actually consumed
                        segmentSpent[source] = Math.min(segmentBudget[source],
                                segmentSpent[source] + consumedAtRadius);
                        log.info("      Radius {} consumed {} mins — stopping diffusion from segment {}",
                                radius, String.format("%.1f", consumedAtRadius), source);
                        break;
                    }
                }

                if (!sourceDiffused) {
                    // No neighbor can absorb budget — mark truly exhausted without faking segmentSpent
                    exhaustedSources.add(source);
                    log.info("    Segment {}: no neighbors absorbed budget at any radius — marked as exhausted", source);
                }
            }
        }

        double finalSpent = finalSelection.stream()
                .mapToDouble(s -> s.getDetour() + dwellTimePerStop)
                .sum();
        log.info("=== ESCALATED SELECTION COMPLETE ===");
        log.info("Waypoints: {} selected | Actual time cost: {}/{} mins ({}%) | Diffusion added: {}",
                finalSelection.size(),
                String.format("%.1f", finalSpent),
                String.format("%.1f", totalTimeBudget),
                Math.round((finalSpent / totalTimeBudget) * 100),
                finalSelection.size() - pass1Count);

        for (int seg = 0; seg < numSegments; seg++) {
            final int currentSeg = seg;
            log.info("  Segment {}: {} waypoints, {}/{} mins spent",
                    seg,
                    finalSelection.stream().filter(s -> s.getSegmentIndex() == currentSeg).count(),
                    String.format("%.1f", segmentSpent[seg]),
                    String.format("%.1f", segmentBudget[seg]));
        }

        finalSelection.sort(Comparator.comparingDouble(ScenicSpot::getDistFromStart));
        return finalSelection;
    }

    private double runSegmentSelection(
            int segIndex,
            List<ScenicSpot> candidates,
            double budget,
            List<ScenicSpot> finalSelection,
            int dwellTimePerStop,
            double alreadySpent,
            double totalPathLengthKm
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

            double cost = spot.getDetour() + dwellTimePerStop;
            if (spent + cost <= budget) {
                if (isSpaceAvailable(finalSelection, spot, totalPathLengthKm)) {
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
            double routeEnhancementThreshold
    ) {
        List<ScenicSpot> candidates = new ArrayList<>();
        Set<String> seenPlaceIds = new HashSet<>();
        double odometer = 0;
        double lastSearchOdometer = 0;

        List<ScenicSpot> rawGoogleSpots = new ArrayList<>();
        Map<Integer, Integer> zoneResultCounts = new HashMap<>();

        // ── Google Places pass ────────────────────────────────────────────────
        for (int i = 0; i < path.size() - 1; i += samplingStep) {
            LatLng p1 = path.get(i);
            LatLng p2 = path.get(Math.min(i + samplingStep, path.size() - 1));
            double stepDist = haversine(p1.lat, p1.lng, p2.lat, p2.lng);
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

                List<ScenicSpot> nearby = googleProxy.searchNearbyScenic(
                        p1.lat, p1.lng, adaptiveRadius, entityPreferences);

                int qualifiedCount = 0;
                for (ScenicSpot spot : nearby) {
                    if (spot.getRating() == 0 && spot.getUserRatingsTotal() == 0) {
                        log.debug("Filtered zero-rating result: {}", spot.getName());
                        continue;
                    }
                    if (seenPlaceIds.contains(spot.getPlaceId())) {
                        continue;
                    }
                    seenPlaceIds.add(spot.getPlaceId());
                    spot.setDistFromStart(odometer);
                    double score = calculateScore(spot, p1, totalDist, routeEnhancementThreshold, dest);
                    spot.setScore(score);
                    rawGoogleSpots.add(spot);
                    qualifiedCount++;
                }
                zoneResultCounts.merge(zoneIndex, qualifiedCount, Integer::sum);
            }
        }

        // Destination wide-radius search
        int destZoneIndex = (int) (totalDist / 60.0);
        if (zoneResultCounts.getOrDefault(destZoneIndex, 0) < 3) {
            int destRadius = Math.min(radius * 4, 25000);
            log.info("Destination zone sparse — wide search near dest (radius={})", destRadius);
            List<ScenicSpot> destNearby = googleProxy.searchNearbyScenic(
                    dest.lat, dest.lng, destRadius, entityPreferences);
            for (ScenicSpot spot : destNearby) {
                if (spot.getRating() == 0 && spot.getUserRatingsTotal() == 0) {
                    continue;
                }
                if (seenPlaceIds.contains(spot.getPlaceId())) {
                    continue;
                }
                seenPlaceIds.add(spot.getPlaceId());
                spot.setDistFromStart(totalDist);
                double score = calculateScore(spot, dest, totalDist, routeEnhancementThreshold, dest);
                spot.setScore(score);
                rawGoogleSpots.add(spot);
            }
        }

        // ── OSM pass ─────────────────────────────────────────────────────────
        List<OsmSearchRequest.LatLng> osmPoints = new ArrayList<>();
        for (int i = 0; i < path.size(); i += Math.max(1, samplingStep * 2)) {
            osmPoints.add(new OsmSearchRequest.LatLng(path.get(i).lat, path.get(i).lng));
        }

        // Map string preference IDs → OsmEntityType enum values (unknown strings skipped)
        List<OsmEntityType> osmTypes = entityPreferences.stream()
                .map(s -> {
                    try {
                        return OsmEntityType.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());

        // Use default scenic set if no preferences match OSM types
        if (osmTypes.isEmpty()) {
            osmTypes = List.of(
                    OsmEntityType.WATERFALL, OsmEntityType.SCENIC_OVERLOOK,
                    OsmEntityType.TRAILHEAD, OsmEntityType.RUINS,
                    OsmEntityType.PEAK, OsmEntityType.NATURE_RESERVE,
                    OsmEntityType.ARCHAEOLOGICAL_SITE, OsmEntityType.ATTRACTION
            );
        }

        OsmSearchRequest osmRequest = new OsmSearchRequest();
        osmRequest.setRoutePoints(osmPoints);
        osmRequest.setEntityTypes(osmTypes);
        osmRequest.setCorridorRadiusMiles(radius / 1609.344);
        osmRequest.setMaxResults(100);

        List<OsmPlace> osmPlaces = Collections.emptyList();
        /* OSM stubbed out
        try {
            osmPlaces = osmService.searchAlongRoute(osmRequest);
            log.info("OSM returned {} candidates", osmPlaces.size());
        } catch (Exception e) {
            log.warn("OSM search failed — proceeding with Google-only results: {}", e.getMessage());
        }   */

        // ── Snap OSM places to route for accurate along-route position ────────
        // OsmService sets distanceFromRouteMiles as perpendicular off-route distance.
        // We need the along-route odometer position for correct segment bucketing
        // and for distFromStart on the final ScenicSpot.
        // Walk the path once per OSM place and find the nearest path point.
        for (OsmPlace p : osmPlaces) {
            double minDist = Double.MAX_VALUE;
            double bestOdometer = 0;
            double odo = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                odo += haversine(path.get(i).lat, path.get(i).lng,
                        path.get(i + 1).lat, path.get(i + 1).lng);
                double d = haversine(p.getLatitude(), p.getLongitude(),
                        path.get(i).lat, path.get(i).lng);
                if (d < minDist) {
                    minDist = d;
                    bestOdometer = odo;
                }
            }
            // Estimate detour from perpendicular distance: round-trip at 80 km/h
            p.setDetourMinutes(minDist * 2.0 / 80.0 * 60.0);
            // Repurpose distanceFromRouteMiles to hold along-route km for bucketing
            p.setDistanceFromRouteMiles(bestOdometer);
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
            c.setEntityType(s.getEntityType());  // ← add this
            return c;
        }).collect(Collectors.toList());

        // ── Score & interleave per segment ────────────────────────────────────
        // Use same segment count as getEscalatedSelection for consistent bucketing
        int numSegments = Math.max(2, Math.min(10, (int) (totalDist / 60.0)));
        double segmentLength = totalDist / numSegments;

        // Bucket Google candidates by segment
        Map<Integer, List<GooglePlaceCandidate>> googleBySegment = new HashMap<>();
        for (int i = 0; i < numSegments; i++) {
            googleBySegment.put(i, new ArrayList<>());
        }
        for (GooglePlaceCandidate c : googleCandidates) {
            int seg = Math.min((int) (c.getDistFromStart() / segmentLength), numSegments - 1);
            googleBySegment.get(seg).add(c);
        }

        // Bucket OSM candidates by segment using snapped along-route position
        Map<Integer, List<OsmPlace>> osmBySegment = new HashMap<>();
        for (int i = 0; i < numSegments; i++) {
            osmBySegment.put(i, new ArrayList<>());
        }
        for (OsmPlace p : osmPlaces) {
            int seg = Math.min((int) (p.getDistanceFromRouteMiles() / segmentLength), numSegments - 1);
            osmBySegment.get(seg).add(p);
        }

        // Score and interleave within each segment, then flatten in segment order
        // so getEscalatedSelection sees a geographically ordered candidate list
        Set<String> addedIds = new HashSet<>();
        for (int seg = 0; seg < numSegments; seg++) {
            List<ScoredWaypoint> scored = waypointScoringService.scoreAndInterleave(
                    googleBySegment.get(seg),
                    osmBySegment.get(seg),
                    entityPreferences,
                    50
            );
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
                spot.setRating(sw.getRating() != null ? sw.getRating() : 0.0);
                spot.setUserRatingsTotal(sw.getUserRatingCount() != null ? sw.getUserRatingCount() : 0);
                spot.setScore(sw.getScore());
                spot.setDetour(sw.getDetourMinutes() != null ? sw.getDetourMinutes().intValue() : 0);
                spot.setEntityType(sw.getEntityType());
                // distFromStart holds along-route km (set during Google pass or OSM snapping above)
                spot.setDistFromStart(sw.getDistFromStart() != null ? sw.getDistFromStart() : 0.0);
                candidates.add(spot);
            }
        }

        log.info("findScenicSpotsAlongPath: {} total candidates (Google={}, OSM={}, segments={})",
                candidates.size(), googleCandidates.size(), osmPlaces.size(), numSegments);
        return candidates;
    }

    /**
     * Calculate score for a scenic spot. FIX 1: arrivalBonus was inverted — now
     * correctly rewards spots closer to destination.
     */
    private double calculateScore(
            ScenicSpot spot,
            LatLng roadPoint,
            double totalDist,
            double routeEnhancementThreshold,
            LatLng dest
    ) {
        double detourDistanceKm = haversine(roadPoint.lat, roadPoint.lng, spot.getLat(), spot.getLng());
        // FIX 5: Use highway speed (80 km/h) for detour estimation, not 30 km/h city speed.
        // A spot 10km from the road point on a highway-adjacent route costs ~15 mins round-trip,
        // not 40 mins. The old formula was making desert/highway stops appear far too expensive.
        int estimatedDetourMins = (int) ((detourDistanceKm * 2.0 / 80.0) * 60.0);
        spot.setDetour(estimatedDetourMins);

        double detourPenalty = 0;
        if (estimatedDetourMins > routeEnhancementThreshold) {
            detourPenalty = (estimatedDetourMins - routeEnhancementThreshold) * 5.0;
        }

        double baseRating = spot.getRating() > 0 ? spot.getRating() : 2.5;
        double logReviews = Math.log10(spot.getUserRatingsTotal() + 1);

        double distToDest = haversine(spot.getLat(), spot.getLng(), dest.lat, dest.lng);
        // FIX 1: Higher bonus for spots closer to destination (further along the route).
        double arrivalBonus = (1.0 - (distToDest / totalDist)) * 2.0;

        return (baseRating * 3) + logReviews + arrivalBonus - detourPenalty;
    }

    /**
     * Check if there's space for a new waypoint. Min distance scales with route
     * length — short routes use 1.5km floor, longer routes spread stops more
     * evenly (e.g. ~12km for a 600km route).
     */
    private boolean isSpaceAvailable(List<ScenicSpot> existing, ScenicSpot candidate, double totalPathLengthKm) {
        double minDistKm = Math.max(1.5, totalPathLengthKm / 50.0);
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
