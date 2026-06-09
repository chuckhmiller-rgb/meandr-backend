package com.meandr.meandrDataServices.repository;

import com.meandr.meandrDataServices.model.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    List<UserRoute> findByUserNameOrderByCreatedAtDesc(String userName);

    List<UserRoute> findByUserNameAndIsSavedTrueOrderByCreatedAtDesc(String userName);

    List<UserRoute> findByUserNameAndIsSavedFalseOrderByCreatedAtDesc(String userName);
    
    List<UserRoute> findByUserNameAndDeletedFalseOrderByCreatedAtDesc(String userName);
    
    

    //List<UserRoute> findByUsernameAndDeletedFalse(String userName);

    @Query("SELECT r FROM UserRoute r WHERE r.deleted = true AND r.deletedAt < :cutoff")
    List<UserRoute> findSoftDeletedBefore(@Param(value = "cutoff") LocalDateTime cutoff);

    Optional<UserRoute> findByUserNameAndRouteName(String userName, String routeName);

    Optional<UserRoute> findByUserName(String userName);

    Optional<UserRoute> findByIdAndUserName(Long userId, String userName);
    
    List<UserRoute> findByIsSavedFalseAndCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM UserRoute r WHERE r.expiresAt < :now AND r.isSaved = false")
    void deleteExpiredRoutes(LocalDateTime now);
}
