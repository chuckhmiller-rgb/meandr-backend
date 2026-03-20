/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author chuck
 */
@Entity
@Table(name = "route_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placeId;
    private String placeName;
    private String placeAddress;
    private Float placeLat;
    private Float placeLon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private UserRoute route;
    
    public Long getId() {
        return id;
    }

    public void setId(Long ID) {
        this.id = ID;
    }
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeID) {
        this.placeId = placeID;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeNAME) {
        this.placeName = placeNAME;
    }
    
    public Float getPlaceLat() {
        return placeLat;
    }
    
    public void setPlaceLat(Float placeLAT) {
        this.placeLat = placeLAT;
    }
    
    public Float getPlaceLon() {
        return placeLon;
    }
    
    public void setPlaceLon(Float placeLON) {
        this.placeLon = placeLON;
    }

    
    
    
    
}