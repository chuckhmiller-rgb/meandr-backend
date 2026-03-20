/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author chuck
 */
@Entity
@Table(name = "climbing_gyms", schema = "meandr")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder



public class ClimbingGym {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "country", length = 100)
    private String country = "USA";

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;

    @Column(name = "website")
    private String website;

    @Column(name = "opening_year")
    private Integer openingYear;

    @Column(name = "wall_height_feet", length = 20)
    private String wallHeightFeet;

    @Column(name = "square_feet")
    private Integer squareFeet;

    @Column(name = "has_bouldering", nullable = false)
    private boolean hasBouldering = false;

    @Column(name = "has_top_rope", nullable = false)
    private boolean hasTopRope = false;

    @Column(name = "has_lead", nullable = false)
    private boolean hasLead = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
