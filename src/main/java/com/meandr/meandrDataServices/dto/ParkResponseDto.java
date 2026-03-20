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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkResponseDto {

    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String countryCode;
    private String managingAgency;
    private String stateProvince;
    private String designation;
    private BigDecimal areaKm2;
    private Integer establishedYear;      // using Integer for easier JSON
    private Integer annualVisitors;
    private BigDecimal entranceFeeUsd;
    private String bestFor;
    private String difficultyLevel;

    private boolean isNationalPark;
    private boolean isStatePark;
    private boolean isCountyPark;
    private boolean isCityPark;

    /*private boolean hasRestrooms;
    private boolean hasParking;
    private boolean hasCamping;
    private boolean hasLodging;
    private boolean hasRangerPrograms;*/

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // deletedAt usually not exposed in public API
}


