/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.service;

/**
 *
 * @author chuck
 */
import com.meandr.meandrDataServices.dto.ClimbingGymResponseDto;
import com.meandr.meandrDataServices.model.ClimbingGym;
import com.meandr.meandrDataServices.model.Park;
import com.meandr.meandrDataServices.repository.ClimbingGymRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class ClimbingGymService {

    private final ClimbingGymRepository gymRepository;

    /**
     * Retrieves all gyms that have not been soft-deleted. With @Where(clause =
     * "deleted_at IS NULL") on the Entity, this becomes the default behavior
     * for findAll().
     *
     * @return
     */
    public List<ClimbingGym> findAllActive() {
        return gymRepository.findByDeletedAtIsNull();
    }

    public List<ClimbingGym> getAllGyms() {
        return gymRepository.findAll();
    }

    public List<ClimbingGymResponseDto> getGymByName(String gymName) {
        return gymRepository.findGymByName(gymName)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Transactional update with Optimistic Locking handling. This prevents the
     * StaleObjectStateException from crashing the app.
     *
     * @param name
     * @param updatedData
     * @return
     */
    @Transactional
    public ClimbingGymResponseDto updateGym(String name, ClimbingGym updatedData) {
        try {
            ClimbingGym gym = gymRepository.findById(name)
                    .orElseThrow(() -> new RuntimeException("Gym not found: " + name));

            // Map fields from updatedData to gym (excluding ID and Version fields)
            gym.setCity(updatedData.getCity());
            gym.setAddress(updatedData.getAddress());
            // ... map other fields ...

            return toResponseDTO(gymRepository.save(gym));
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // This happens if updated_at changed between read and write
            throw new RuntimeException("The gym was updated by another user. Please refresh and try again.");
        }
    }

    public List<ClimbingGymResponseDto> getGymsByCountry(String countryInput) {
        return gymRepository.findAll().stream()
                .filter(gym -> countryInput.equalsIgnoreCase(gym.getCountry()))
                .map(this::toResponseDTO)
                .toList();
    }
    
    public List<ClimbingGym> findClimbingGymsByInBoundingBox(       //UsingSpecification
            double minLon, double minLat,
            double maxLon, double maxLat) {

        Specification<ClimbingGym> spec = (root, query, cb) -> {
            Predicate lonPredicate = cb.between(
                root.get("longitude"), minLon, maxLon);
            
            Predicate latPredicate = cb.between(
                root.get("latitude"), minLat, maxLat);
            
            return cb.and(lonPredicate, latPredicate);
        };

        return gymRepository.findAll(spec);
    }

    /**
     * Soft delete implementation.
     *
     * @param name
     */
    @Transactional
    public void softDeleteGym(String name) {
        gymRepository.findById(name).ifPresent(gym -> {
            gym.setDeletedAt(LocalDateTime.now());
            gymRepository.save(gym);
        });
    }

    private ClimbingGymResponseDto toResponseDTO(ClimbingGym gym) {
        // Assume your DTO has a builder or standard constructor
        return ClimbingGymResponseDto.builder()
                .name(gym.getName())
                .city(gym.getCity())
                .state(gym.getState())
                .country(gym.getCountry())
                // ... fill other fields
                .build();
    }
}
