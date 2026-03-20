/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.meandr.meandrDataServices.repository;

import com.meandr.meandrDataServices.model.UserRoute;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author chuck
 */
@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {
    
    @Query(value = "SELECT ST_Length(path, 'Statute mile') FROM user_routes WHERE id = :id", nativeQuery = true)
    Double getRouteLengthInMiles(Long id);
    
    @Query(value = "SELECT ST_Length(path, 'Statute mile') FROM user_routes WHERE user_name = :userName", nativeQuery = true)
    Double getRouteLengthInMilesByuserName(String userName);

    List<UserRoute> findByuserName(String userName);
    
    // Fetches the route only if the ID matches AND it belongs to the specific user
    @EntityGraph(attributePaths = {"stops"})
    Optional<UserRoute> findByIdAndUserName(Long id, String userName);
    
    @EntityGraph(attributePaths = {"stops"})
    Optional<UserRoute> findByUserNameAndRouteName(String userName, String routeName);


}
