package com.meandr.meandrDataServices.scheduler;

import com.meandr.meandrDataServices.repository.UserRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RouteCleanupScheduler {
    private final UserRouteRepository userRouteRepository;

    @Scheduled(cron = "0 0 2 * * *") // runs daily at 2am
    public void cleanupExpiredRoutes() {
        userRouteRepository.deleteExpiredRoutes(LocalDateTime.now());
    }
}