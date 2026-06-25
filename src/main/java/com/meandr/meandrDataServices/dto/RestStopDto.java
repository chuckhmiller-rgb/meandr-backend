package com.meandr.meandrDataServices.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestStopDto {
    private double lat;
    private double lng;
    private String cityName;
    private double distFromStartKm;
}
