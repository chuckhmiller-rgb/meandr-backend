package com.meandr.meandrDataServices.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_order")
    private Integer stopOrder;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "place_address")
    private String placeAddress;

    @Column(name = "place_lat")
    private Float placeLat;

    @Column(name = "place_lon")
    private Float placeLon;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "detour_mins")
    private Integer detourMins;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "reviews_total")
    private Integer reviewsTotal;

    @Column(name = "opening_hours_json", columnDefinition = "TEXT")
    private String openingHoursJson;

    @Column(name = "utc_offset_minutes", columnDefinition = "TEXT")
    private String utcOffsetMinutes;
    
    @Column(name = "google_photo", columnDefinition = "TEXT")
    private String googlePhoto;

    @Column(name = "dist_from_start")
    private Double distFromStart;

    @Column(name = "is_rest_zone_addition")
    private Boolean isRestZoneAddition;
    
    @Column(name = "score")
    private Double score;

    @Column(name = "is_manually_added")
    private Boolean isManuallyAdded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    @JsonIgnore
    private UserRoute route;
}
