package com.meandr.meandrDataServices.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "views_rating")
    private Integer viewsRating;

    @Column(name = "traffic_rating")
    private Integer trafficRating;

    @Column(name = "experience_rating")
    private Integer experienceRating;

    @Column(name = "journal_entry", columnDefinition = "TEXT")
    private String journalEntry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}