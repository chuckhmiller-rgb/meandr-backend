/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;


import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 *
 * @author chuck
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteDto {
    private String userName;
    private String routeName;
    private List<RouteStopDto> stops;
    

    

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStopDto {
        private String placeId;
        private String placeName;
        private Float placeLat;
        private Float placeLon;

       
    }        
}