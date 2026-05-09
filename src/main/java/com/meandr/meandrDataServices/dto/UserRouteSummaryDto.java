package com.meandr.meandrDataServices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteSummaryDto {
    private Long id;
    private String routeName;
    private String originName;
    private String destinationName;
    private Integer baseTripMins;
    private Integer addedMins;
    private Boolean isSaved;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer stopCount;
}