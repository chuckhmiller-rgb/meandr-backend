package com.meandr.meandrDataServices.preferences;

/**
 * User preference tier for an entity type.
 * Maps directly to a scoring weight multiplier used by WaypointScoringService.
 */
public enum PreferenceTier {
    LOVE      (1.00),
    LIKE      (0.65),
    INTERESTED(0.35);

    public final double weight;

    PreferenceTier(double weight) {
        this.weight = weight;
    }
}