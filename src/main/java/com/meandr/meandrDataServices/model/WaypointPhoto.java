package com.meandr.meandrDataServices.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waypoint_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaypointPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "place_name")
    private String placeName;

    @Lob
    @Column(name = "image_data", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "notes")
    private String notes;

    @PrePersist
    public void prePersist() {
        takenAt = LocalDateTime.now();
    }
}
