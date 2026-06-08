package com.meandr.meandrDataServices.scheduler;

import com.meandr.meandrDataServices.model.UserRoute;
import com.meandr.meandrDataServices.repository.UserRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteCleanupScheduler {

    private final UserRouteRepository userRouteRepository;

    @Scheduled(cron = "0 0 2 * * *") // runs daily at 2am
    public void cleanupExpiredRoutes() {
        userRouteRepository.deleteExpiredRoutes(LocalDateTime.now());
    }

    @Scheduled(cron = "0 5 2 * * *") // 2:05am
    public void hardDeleteOldRoutes() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        List<UserRoute> toDelete = userRouteRepository.findSoftDeletedBefore(cutoff);
        userRouteRepository.deleteAll(toDelete);
        System.out.println("RouteCleanup: hard deleted " + toDelete.size() + " routes older than 90 days");
    }
}
