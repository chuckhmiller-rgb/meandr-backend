/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.repository;

/**
 *
 * @author chuck
 */
// 3. Repository

import com.meandr.meandrDataServices.model.Park;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkRepository extends JpaRepository<Park, String> {

    public List<Park> findParkByName(String parkName);
    
    public List<Park> findAll(Specification<Park> spec);
    
    public List<Park> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat,
            Double minLon, Double maxLon);

    
}
