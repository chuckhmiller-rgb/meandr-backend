package com.meandr.meandrDataServices.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "user_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "origin_name")
    private String originName;

    @Column(name = "destination_name")
    private String destinationName;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lng")
    private Double originLng;

    @Column(name = "dest_lat")
    private Double destLat;

    @Column(name = "dest_lng")
    private Double destLng;

    @Column(name = "master_polyline", columnDefinition = "MEDIUMTEXT")
    private String masterPolyline;

    @Column(name = "base_trip_mins")
    private Integer baseTripMins;

    @Column(name = "added_mins")
    private Integer addedMins;

    @Column(name = "mf")
    private Integer mf;

    @Column(name = "avoid_highways")
    private Boolean avoidHighways = false;

    @Column(name = "avoid_tolls")
    private Boolean avoidTolls = false;

    @Column(name = "exclude_origin")
    private Boolean excludeOrigin = false;

    @Column(name = "exclude_dest")
    private Boolean excludeDest = false;

    @Column(name = "entity_preferences", columnDefinition = "TEXT")
    private String entityPreferences;

    @Column(name = "is_saved")
    private Boolean isSaved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "rejected_stops", columnDefinition = "JSON")
    private String rejectedStops;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stop_order ASC")
    @Builder.Default
    private List<RouteStop> stops = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (!Boolean.TRUE.equals(isSaved)) {
            expiresAt = LocalDateTime.now().plusDays(30);
        }
    }
}
