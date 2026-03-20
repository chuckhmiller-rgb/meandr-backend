package com.meandr.meandrDataServices.preferences;

import com.meandr.meandrDataServices.osm.model.OsmEntityType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EntityPreferenceService {

    private final EntityPreferenceRepository repo;

    public EntityPreferenceService(EntityPreferenceRepository repo) {
        this.repo = repo;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Replace all preferences for a user in one transaction.
     * Called from registration and profile save.
     *
     * @param userId  internal user ID
     * @param entries list of { entityTypeId, tier } from the frontend
     */
    @Transactional
    public void savePreferences(Long userId, List<PreferenceEntry> entries) {
        repo.deleteAllByUserId(userId);

        List<UserEntityPreference> prefs = entries.stream()
                .filter(e -> e.getTier() != null && e.getEntityTypeId() != null)
                .map(e -> new UserEntityPreference(
                        userId,
                        e.getEntityTypeId(),
                        resolveSource(e.getEntityTypeId()),
                        e.getTier()
                ))
                .collect(Collectors.toList());

        repo.saveAll(prefs);
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Load all preferences for a user as a flat list of PreferenceEntry.
     * Used to restore UI state.
     */
    public List<PreferenceEntry> loadPreferences(Long userId) {
        return repo.findByUserId(userId).stream()
                .map(p -> new PreferenceEntry(p.getEntityTypeId(), p.getTier()))
                .collect(Collectors.toList());
    }

    /**
     * Build a weight map for use by WaypointScoringService.
     * Returns: entityTypeId (lowercase) → tier weight (0.35–1.0)
     */
    public Map<String, Double> buildWeightMap(Long userId) {
        Map<String, Double> weights = new HashMap<>();
        for (UserEntityPreference p : repo.findByUserId(userId)) {
            weights.put(p.getEntityTypeId().toLowerCase(), p.getTier().weight);
        }
        return weights;
    }

    /**
     * Build a weight map from a raw list of entries (for unauthenticated/guest users
     * whose prefs come in the request body rather than being persisted).
     */
    public Map<String, Double> buildWeightMapFromEntries(List<PreferenceEntry> entries) {
        Map<String, Double> weights = new HashMap<>();
        if (entries == null) return weights;
        for (PreferenceEntry e : entries) {
            if (e.getEntityTypeId() != null && e.getTier() != null) {
                weights.put(e.getEntityTypeId().toLowerCase(), e.getTier().weight);
            }
        }
        return weights;
    }

    /**
     * Return all categories with their types and the user's current tier for each.
     * Used to populate the full preference picker UI.
     */
    public List<CategoryPreferenceView> buildCategoryView(Long userId) {
        Map<String, PreferenceTier> tierByType = new HashMap<>();
        if (userId != null) {
            for (UserEntityPreference p : repo.findByUserId(userId)) {
                tierByType.put(p.getEntityTypeId(), p.getTier());
            }
        }
        return buildCategoryViewFromMap(tierByType);
    }

    public List<CategoryPreferenceView> buildCategoryViewFromEntries(List<PreferenceEntry> entries) {
        Map<String, PreferenceTier> tierByType = new HashMap<>();
        if (entries != null) {
            for (PreferenceEntry e : entries) {
                if (e.getEntityTypeId() != null && e.getTier() != null) {
                    tierByType.put(e.getEntityTypeId(), e.getTier());
                }
            }
        }
        return buildCategoryViewFromMap(tierByType);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<CategoryPreferenceView> buildCategoryViewFromMap(Map<String, PreferenceTier> tierByType) {
        List<CategoryPreferenceView> result = new ArrayList<>();

        for (EntityCategory cat : EntityCategory.values()) {
            List<TypePreferenceView> types = new ArrayList<>();

            for (String typeId : cat.entityTypeIds) {
                PreferenceTier tier = tierByType.get(typeId);
                String emoji = resolveEmoji(typeId);
                String label = resolveLabel(typeId);
                types.add(new TypePreferenceView(typeId, label, emoji, tier));
            }

            long loveCount       = types.stream().filter(t -> t.getTier() == PreferenceTier.LOVE).count();
            long likeCount       = types.stream().filter(t -> t.getTier() == PreferenceTier.LIKE).count();
            long interestedCount = types.stream().filter(t -> t.getTier() == PreferenceTier.INTERESTED).count();

            result.add(new CategoryPreferenceView(
                    cat.name(), cat.displayName, cat.emoji,
                    types, loveCount, likeCount, interestedCount));
        }
        return result;
    }

    private EntitySource resolveSource(String entityTypeId) {
        try {
            OsmEntityType.valueOf(entityTypeId.toUpperCase());
            return EntitySource.OSM;
        } catch (IllegalArgumentException e) {
            return EntitySource.GOOGLE;
        }
    }

    private String resolveEmoji(String typeId) {
        try {
            return OsmEntityType.valueOf(typeId).emoji;
        } catch (IllegalArgumentException e) {
            // Google Places type — use a reasonable default
            return switch (typeId) {
                case "restaurant", "meal_takeaway" -> "🍽️";
                case "cafe"            -> "☕";
                case "bar"             -> "🍺";
                case "bakery"          -> "🥐";
                case "night_club"      -> "🎶";
                case "museum"          -> "🏛️";
                case "art_gallery"     -> "🎨";
                case "park"            -> "🌳";
                case "campground"      -> "⛺";
                case "rv_park"         -> "🚐";
                case "tourist_attraction" -> "✨";
                case "natural_feature" -> "🌿";
                case "zoo"             -> "🦁";
                case "aquarium"        -> "🐠";
                case "library"         -> "📚";
                case "performing_arts_theater" -> "🎭";
                case "gas_station"     -> "⛽";
                case "lodging"         -> "🏨";
                case "convenience_store" -> "🏪";
                default                -> "📍";
            };
        }
    }

    private String resolveLabel(String typeId) {
        try {
            return OsmEntityType.valueOf(typeId).displayName;
        } catch (IllegalArgumentException e) {
            return typeId.replace("_", " ")
                    .substring(0, 1).toUpperCase()
                    + typeId.replace("_", " ").substring(1);
        }
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    public static class PreferenceEntry {
        private String entityTypeId;
        private PreferenceTier tier;

        public PreferenceEntry() {}
        public PreferenceEntry(String entityTypeId, PreferenceTier tier) {
            this.entityTypeId = entityTypeId;
            this.tier = tier;
        }

        public String getEntityTypeId() { return entityTypeId; }
        public void setEntityTypeId(String entityTypeId) { this.entityTypeId = entityTypeId; }
        public PreferenceTier getTier() { return tier; }
        public void setTier(PreferenceTier tier) { this.tier = tier; }
    }

    public static class TypePreferenceView {
        private String id;
        private String label;
        private String emoji;
        private PreferenceTier tier; // null = not selected

        public TypePreferenceView(String id, String label, String emoji, PreferenceTier tier) {
            this.id = id; this.label = label; this.emoji = emoji; this.tier = tier;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getEmoji() { return emoji; }
        public PreferenceTier getTier() { return tier; }
    }

    public static class CategoryPreferenceView {
        private String id;
        private String displayName;
        private String emoji;
        private List<TypePreferenceView> types;
        private long loveCount;
        private long likeCount;
        private long interestedCount;

        public CategoryPreferenceView(String id, String displayName, String emoji,
                                       List<TypePreferenceView> types,
                                       long loveCount, long likeCount, long interestedCount) {
            this.id = id; this.displayName = displayName; this.emoji = emoji;
            this.types = types;
            this.loveCount = loveCount; this.likeCount = likeCount;
            this.interestedCount = interestedCount;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
        public List<TypePreferenceView> getTypes() { return types; }
        public long getLoveCount() { return loveCount; }
        public long getLikeCount() { return likeCount; }
        public long getInterestedCount() { return interestedCount; }

        public String getSummary() {
            List<String> parts = new ArrayList<>();
            if (loveCount > 0) parts.add(loveCount + " loved");
            if (likeCount > 0) parts.add(likeCount + " liked");
            if (interestedCount > 0) parts.add(interestedCount + " interested");
            return parts.isEmpty() ? "None selected" : String.join(" · ", parts);
        }
    }
}