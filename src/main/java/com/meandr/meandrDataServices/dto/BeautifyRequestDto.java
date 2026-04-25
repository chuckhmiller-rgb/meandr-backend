package com.meandr.meandrDataServices.dto;

import java.util.List;

/**
 * @author chuck
 */
public class BeautifyRequestDto {

    private List<List<Double>> selectedRouteCoords;
    private CoordinateDto origin;
    private CoordinateDto destination;
    private double routeEnhancementThreshold;
    private int radius;
    private List<String> entityPreferences;
    private int dwellTimePerStop = 5;
    private boolean avoidHighways = false;
    private boolean avoidTolls = false;
    private boolean excludeOrigin = false;
    private boolean excludeDest = false;

    public List<List<Double>> getSelectedRouteCoords() {
        return selectedRouteCoords;
    }

    public void setSelectedRouteCoords(List<List<Double>> selectedRouteCoords) {
        this.selectedRouteCoords = selectedRouteCoords;
    }

    public CoordinateDto getOrigin() {
        return origin;
    }

    public void setOrigin(CoordinateDto origin) {
        this.origin = origin;
    }

    public CoordinateDto getDestination() {
        return destination;
    }

    public void setDestination(CoordinateDto destination) {
        this.destination = destination;
    }

    public double getRouteEnhancementThreshold() {
        return routeEnhancementThreshold;
    }

    public void setRouteEnhancementThreshold(double routeEnhancementThreshold) {
        this.routeEnhancementThreshold = routeEnhancementThreshold;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public List<String> getEntityPreferences() {
        return entityPreferences;
    }

    public void setEntityPreferences(List<String> entityPreferences) {
        this.entityPreferences = entityPreferences;
    }

    public int getDwellTimePerStop() {
        return dwellTimePerStop;
    }

    public void setDwellTimePerStop(int dwellTimePerStop) {
        this.dwellTimePerStop = dwellTimePerStop;
    }

    public boolean isAvoidHighways() {
        return avoidHighways;
    }

    public void setAvoidHighways(boolean avoidHighways) {
        this.avoidHighways = avoidHighways;
    }

    public boolean isAvoidTolls() {
        return avoidTolls;
    }

    public void setAvoidTollsHighways(boolean avoidTolls) {
        this.avoidTolls = avoidTolls;
    }

    public boolean isExcludeOrigin() {
        return excludeOrigin;
    }

    public void setExcludeOrigin(boolean excludeOrigin) {
        this.excludeOrigin = excludeOrigin;
    }

    public boolean isExcludeDest() {
        return excludeDest;
    }

    public void setExcludeDest(boolean excludeDest) {
        this.excludeDest = excludeDest;
    }
}
