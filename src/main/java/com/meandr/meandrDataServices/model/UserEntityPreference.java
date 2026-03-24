package com.meandr.meandrDataServices.model;

import com.meandr.meandrDataServices.preferences.EntitySource;
import com.meandr.meandrDataServices.preferences.PreferenceTier;
import jakarta.persistence.*;

/**
 * Persisted preference for a single entity type for a single user.
 * One row per (userId, entityTypeId) pair.
 */
@Entity
@Table(name = "user_entity_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_type_id"}))
public class UserEntityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Either a Google Places type string (e.g. "restaurant")
     * or an OsmEntityType.name() (e.g. "WATERFALL").
     */
    @Column(name = "entity_type_id", nullable = false, length = 64)
    private String entityTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private EntitySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 16)
    private PreferenceTier tier;

    public UserEntityPreference() {}

    public UserEntityPreference(Long userId, String entityTypeId,
                                 EntitySource source, PreferenceTier tier) {
        this.userId       = userId;
        this.entityTypeId = entityTypeId;
        this.source       = source;
        this.tier         = tier;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEntityTypeId() { return entityTypeId; }
    public void setEntityTypeId(String entityTypeId) { this.entityTypeId = entityTypeId; }

    public EntitySource getSource() { return source; }
    public void setSource(EntitySource source) { this.source = source; }

    public PreferenceTier getTier() { return tier; }
    public void setTier(PreferenceTier tier) { this.tier = tier; }
}