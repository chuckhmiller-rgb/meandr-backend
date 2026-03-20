/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

/**
 *
 * @author chuck
 */

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClimbingGymResponseDto {
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Location Info
    private String city;
    private String state;
    private String country;
    private String address;

    // Gym Details
    private String website;
    private Integer openingYear;
    private BigDecimal wallHeightFeet;
    private Integer squareFeet;

    // Amenities
    private boolean hasBouldering;
    private boolean hasTopRope;
    private boolean hasLead;

    private String description;
}
