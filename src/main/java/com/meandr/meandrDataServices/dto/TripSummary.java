package com.meandr.meandrDataServices.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripSummary {

    @JsonProperty("originalDurationMins")
    private long originalDurationMins;

    @JsonProperty("additionalMeandrMins")
    private double additionalMeandrMins;

    @JsonProperty("enhancementBudgetMins")
    private double enhancementBudgetMins;  // baselineDuration * (threshold/100)

    @JsonProperty("waypointCount")
    private int waypointCount;

    @JsonProperty("requestedEnhancement")
    private double requestedEnhancement;   // what user asked for (e.g. 30.0)

    /**
     * Actual enhancement delivered: detour time as % of baseline duration.
     * actualEnhancement = (additionalMeandrMins / originalDurationMins) * 100
     */
    @JsonProperty("actualEnhancement")
    public double getActualEnhancement() {
        return originalDurationMins > 0
                ? (additionalMeandrMins / originalDurationMins) * 100.0
                : 0.0;
    }

    /**
     * What percentage of the enhancement budget was used.
     */
    @JsonProperty("budgetUtilization")
    public double getBudgetUtilization() {
        return enhancementBudgetMins > 0
                ? (additionalMeandrMins / enhancementBudgetMins) * 100.0
                : 0.0;
    }
}