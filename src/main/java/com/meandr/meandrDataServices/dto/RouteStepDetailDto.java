/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;


import com.google.maps.model.LatLng;
import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author chuck
 */
public class RouteStepDetailDto {
    private String instruction;
    private String distance;
    private String duration;
    private List<LatLng> points;

    // Standard Constructor
    public RouteStepDetailDto(String instruction, String distance, String duration, List<LatLng> points) {
        this.instruction = instruction;
        this.distance = distance;
        this.duration = duration;
        this.points = points != null ? points : new ArrayList<>();
    }

    // Default Constructor for Serialization
    public RouteStepDetailDto() {
        this.points = new ArrayList<>();
    }

    // Getters and Setters
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public List<LatLng> getPoints() { return points; }
    public void setPoints(List<LatLng> points) { this.points = points; }
}
