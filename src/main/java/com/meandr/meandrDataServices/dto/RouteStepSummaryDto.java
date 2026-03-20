/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

/**
 *
 * @author chuck
 */
public class RouteStepSummaryDto {
    private String instruction;
    private String distance;
    private String encodedPoints;

    // Standard Constructor
    public RouteStepSummaryDto(String instruction, String distance, String encodedPoints) {
        this.instruction = instruction;
        this.distance = distance;
        this.encodedPoints = encodedPoints;
    }

    // Default Constructor for JSON Serialization
    public RouteStepSummaryDto() {}

    // Getters and Setters
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getEncodedPoints() { return encodedPoints; }
    public void setEncodedPoints(String encodedPoints) { this.encodedPoints = encodedPoints; }
}
