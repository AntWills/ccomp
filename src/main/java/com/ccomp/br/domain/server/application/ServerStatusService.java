package com.ccomp.br.domain.server.application;

import com.ccomp.br.domain.server.dto.ServerStatusResponse;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class ServerStatusService {
    private final Clock clock;

    public ServerStatusService(Clock clock) {
        this.clock = clock;
    }

    public ServerStatusResponse getStatus() {
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryMax = runtime.maxMemory();
        double memoryUsedPercent = memoryMax > 0 ? (double) memoryUsed * 100 / memoryMax : 0;

        Double cpuUsagePercent = null;
        var operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystem instanceof OperatingSystemMXBean extendedOperatingSystem) {
            double cpuLoad = extendedOperatingSystem.getCpuLoad();
            if (cpuLoad >= 0) {
                cpuUsagePercent = cpuLoad * 100;
            }
        }

        Instant now = clock.instant();
        LocalDateTime serverLocalTime = LocalDateTime.ofInstant(now, clock.getZone());
        return new ServerStatusResponse(
                now, serverLocalTime, clock.getZone().getId(),
                memoryUsed, memoryMax, memoryUsedPercent, cpuUsagePercent);
    }
}
