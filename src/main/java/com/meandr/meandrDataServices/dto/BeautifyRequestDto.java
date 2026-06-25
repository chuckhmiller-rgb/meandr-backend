package com.meandr.meandrDataServices.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BeautifyRequestDto {

    private List<List<Double>> selectedRouteCoords;
    private CoordinateDto origin;
    private CoordinateDto destination;
    private String originName;
    private String destinationName;
    private double routeEnhancementThreshold;
    private int radius;
    private List<String> entityPreferences;
    private int dwellTimePerStop = 5;
    private boolean avoidHighways = false;
    private boolean avoidTolls = false;
    private boolean excludeOrigin = false;
    private boolean excludeDest = false;
    private List<String> includeKeywords;
    private List<String> excludeKeywords;
    private String restStopCadence;
}
