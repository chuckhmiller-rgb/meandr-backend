/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.service;

/**
 *
 * @author chuck
 */
// 4. Service (minimal version)

import com.meandr.meandrDataServices.dto.ParkResponseDto;
import com.meandr.meandrDataServices.model.Park;
import com.meandr.meandrDataServices.repository.ParkRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class ParkService {

    private final ParkRepository parkRepository;

    public List<ParkResponseDto> getAllParks() {
        return parkRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ParkResponseDto> getParkByName(String parkName) {
        return parkRepository.findParkByName(parkName).stream()
                .map(this::toResponseDTO)
                .toList();
    }
    
    public List<Park> findParksInBoundingBox(       //UsingSpecification
            double minLon, double minLat,
            double maxLon, double maxLat) {

        Specification<Park> spec = (root, query, cb) -> {
            Predicate lonPredicate = cb.between(
                root.get("longitude"), minLon, maxLon);
            
            Predicate latPredicate = cb.between(
                root.get("latitude"), minLat, maxLat);
            
            return cb.and(lonPredicate, latPredicate);
        };

        return parkRepository.findAll(spec);
    }
    

    private ParkResponseDto toResponseDTO(Park park) {
        return ParkResponseDto.builder()
                .name(park.getName())
                .latitude(park.getLatitude())
                .longitude(park.getLongitude())
                .countryCode(park.getCountryCode())
                .managingAgency(park.getManagingAgency())
                .stateProvince(park.getStateProvince())
                .designation(park.getDesignation())
                .areaKm2(park.getAreaKm2())
                .establishedYear(park.getEstablishedYear() != null ? park.getEstablishedYear().intValue() : null)
                .annualVisitors(park.getAnnualVisitors())
                .entranceFeeUsd(park.getEntranceFeeUsd())
                .bestFor(park.getBestFor())
                .difficultyLevel(park.getDifficultyLevel())
                .isNationalPark(Boolean.TRUE.equals(park.getIsNationalPark()))
                .isStatePark(Boolean.TRUE.equals(park.getIsStatePark()))
                .isCountyPark(Boolean.TRUE.equals(park.getIsCountyPark()))
                .isCityPark(Boolean.TRUE.equals(park.getIsCityPark()))
                /*.hasRestrooms(Boolean.TRUE.equals(park.getHasRestrooms()))
                .hasParking(Boolean.TRUE.equals(park.getHasParking()))
                .hasCamping(Boolean.TRUE.equals(park.getHasCamping()))
                .hasLodging(Boolean.TRUE.equals(park.getHasLodging()))
                .hasRangerPrograms(Boolean.TRUE.equals(park.getHasRangerPrograms()))*/
                .createdAt(park.getCreatedAt())
                .updatedAt(park.getUpdatedAt())
                .build();
    }
}
