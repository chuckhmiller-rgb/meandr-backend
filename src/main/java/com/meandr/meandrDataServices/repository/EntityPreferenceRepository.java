package com.meandr.meandrDataServices.repository;

import com.meandr.meandrDataServices.preferences.EntitySource;
import com.meandr.meandrDataServices.preferences.PreferenceTier;
import com.meandr.meandrDataServices.model.UserEntityPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityPreferenceRepository extends JpaRepository<UserEntityPreference, Long> {

    List<UserEntityPreference> findByUserId(Long userId);

    List<UserEntityPreference> findByUserIdAndSource(Long userId, EntitySource source);

    List<UserEntityPreference> findByUserIdAndTier(Long userId, PreferenceTier tier);

    @Modifying
    @Query("DELETE FROM UserEntityPreference p WHERE p.userId = :userId")
    void deleteAllByUserId(Long userId);
}