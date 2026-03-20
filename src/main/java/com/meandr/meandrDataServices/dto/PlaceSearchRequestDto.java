/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author chuck
 */
@Data
public class PlaceSearchRequestDto {

    @Schema(
            description = "The search query for Google Places",
            example = "{\n"
            + "  \"textQuery\": \"climbing gyms in Southeastern United States\",\n"
            + "  \"locationBias\": {\n"
            + "    \"rectangle\": {\n"
            + "      \"low\": { \"latitude\": 24.396308, \"longitude\": -91.513078 },\n"
            + "      \"high\": { \"latitude\": 39.466012, \"longitude\": -75.459503 }\n"
            + "    }\n"
            + "  }\n"
            + "}" // This pre-fills the box
    )
    private String textQuery;
    private String pageToken;

    // getter and setter
    public String getTextQuery() {
        return textQuery;
    }

    public void setTextQuery(String textQuery) {
        this.textQuery = textQuery;
    }
}
