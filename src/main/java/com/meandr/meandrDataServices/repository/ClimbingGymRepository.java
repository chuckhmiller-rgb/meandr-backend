/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.repository;

/**
 *
 * @author chuck
 */

import com.meandr.meandrDataServices.model.ClimbingGym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

@Repository
public interface ClimbingGymRepository extends JpaRepository<ClimbingGym, String> {
    // Custom query to find gyms by city
    List<ClimbingGym> findByCityIgnoreCase(String city);
    
    public List<ClimbingGym> findAll(Specification<ClimbingGym> spec);
    
    // Find only active (not deleted) gyms
    public List<ClimbingGym> findByDeletedAtIsNull();

    public List<ClimbingGym> findGymByName(String gymName);
    
}
