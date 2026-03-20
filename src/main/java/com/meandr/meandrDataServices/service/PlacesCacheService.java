package com.meandr.meandrDataServices.service;

import com.meandr.meandrDataServices.model.ScenicSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacesCacheService {

    private final JdbcTemplate jdbcTemplate;

    private static final int CACHE_TTL_DAYS = 60;

    private final RowMapper<ScenicSpot> spotRowMapper = (rs, rowNum) -> {
        ScenicSpot spot = new ScenicSpot();
        spot.setPlaceId(rs.getString("place_id"));
        spot.setName(rs.getString("name"));
        spot.setLat(rs.getDouble("lat"));
        spot.setLng(rs.getDouble("lng"));
        spot.setRating(rs.getDouble("rating"));
        spot.setUserRatingsTotal(rs.getInt("user_ratings_total"));
        spot.setEntityType(rs.getString("entity_type"));
        spot.setAddress(rs.getString("address"));
        return spot;
    };

    /**
     * Look up cached places within a bounding box, filtered by entity types,
     * excluding expired entries.
     */
    public List<ScenicSpot> findNearby(
            double lat,
            double lng,
            int radiusMeters,
            List<String> entityTypes
    ) {
        // Bounding box pre-filter — rough but fast
        // 1 degree lat ≈ 111km, 1 degree lng ≈ 111km * cos(lat)
        double latDelta = (radiusMeters / 1000.0) / 111.0;
        double lngDelta = (radiusMeters / 1000.0) / (111.0 * Math.cos(Math.toRadians(lat)));

        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        String placeholders = String.join(",",
                entityTypes.stream().map(t -> "?").toArray(String[]::new));

        String sql = String.format("""
                SELECT place_id, name, lat, lng, rating, user_ratings_total,
                       entity_type, address
                FROM places_cache
                WHERE lat BETWEEN ? AND ?
                  AND lng BETWEEN ? AND ?
                  AND entity_type IN (%s)
                  AND cached_at > NOW() - INTERVAL %d DAY
                """, placeholders, CACHE_TTL_DAYS);

        Object[] params = buildParams(minLat, maxLat, minLng, maxLng, entityTypes);

        List<ScenicSpot> results = jdbcTemplate.query(sql, spotRowMapper, params);
        log.debug("Cache hit: {} spots near ({},{}) radius={}m", results.size(), lat, lng, radiusMeters);
        return results;
    }

    /**
     * Persist a list of spots returned from the Places API.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE to refresh rating and cached_at
     * if the place already exists.
     */
    public void saveAll(List<ScenicSpot> spots) {
        String sql = """
                INSERT INTO places_cache
                    (place_id, name, lat, lng, rating, user_ratings_total, entity_type, address, cached_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    rating             = VALUES(rating),
                    user_ratings_total = VALUES(user_ratings_total),
                    cached_at          = NOW()
                """;

        int saved = 0;
        for (ScenicSpot spot : spots) {
            if (spot.getPlaceId() == null || spot.getPlaceId().isBlank()) {
                continue;
            }
            jdbcTemplate.update(sql,
                    spot.getPlaceId(),
                    spot.getName(),
                    spot.getLat(),
                    spot.getLng(),
                    spot.getRating(),
                    spot.getUserRatingsTotal(),
                    spot.getEntityType(),
                    spot.getAddress()
            );
            saved++;
        }
        log.info("Saved {} spots to places_cache", saved);
    }

    /**
     * Purge all entries older than the TTL. Can be called from a scheduled job.
     */
    public int purgeExpired() {
        String sql = "DELETE FROM places_cache WHERE cached_at < NOW() - INTERVAL ? DAY";
        int deleted = jdbcTemplate.update(sql, CACHE_TTL_DAYS);
        log.info("Purged {} expired entries from places_cache", deleted);
        return deleted;
    }

    private Object[] buildParams(
            double minLat, double maxLat,
            double minLng, double maxLng,
            List<String> entityTypes
    ) {
        Object[] params = new Object[4 + entityTypes.size()];
        params[0] = minLat;
        params[1] = maxLat;
        params[2] = minLng;
        params[3] = maxLng;
        for (int i = 0; i < entityTypes.size(); i++) {
            params[4 + i] = entityTypes.get(i);
        }
        return params;
    }
}