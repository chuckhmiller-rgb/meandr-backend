/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author charlesmiller
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
        
public class WaypointPhotoDto {
    private Long userId;
    private String userName;
    private Long routeId;
    private String placeId;
    private String name;
    private String imageData; // base64
    private String notes;
    
}
