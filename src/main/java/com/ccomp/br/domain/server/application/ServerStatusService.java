package com.ccomp.br.domain.server.application;

import com.ccomp.br.domain.server.dto.ServerStatusResponse;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;

@Service
public class ServerStatusService {
    public ServerStatusResponse getStatus() {
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryMax = runtime.maxMemory();

        Double cpuUsagePercent = null;
        java.lang.management.OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean extendedOperatingSystem) {
            double cpuLoad = extendedOperatingSystem.getCpuLoad();
            if (cpuLoad >= 0) {
                cpuUsagePercent = cpuLoad * 100;
            }
        }

        return new ServerStatusResponse(Instant.now(), memoryUsed, memoryMax, cpuUsagePercent);
    }
}
