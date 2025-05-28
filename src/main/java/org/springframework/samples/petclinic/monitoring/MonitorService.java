package org.springframework.samples.petclinic.monitoring;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MonitorService implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    @PostConstruct
    public void init() {
        try {
            // Perform initialization
            logger.info("Initializing Monitor Service");
            isInitialized.set(true);
        } catch (Exception e) {
            logger.error("Failed to initialize Monitor Service", e);
            throw new MonitorServiceException("Failed to initialize Monitor Service", e);
        }
    }

    @CircuitBreaker(name = "monitorService", fallbackMethod = "fallbackMonitor")
    public MonitorStatus monitor() {
        if (!isInitialized.get()) {
            logger.error("Monitor Service not initialized");
            throw new IllegalStateException("Monitor Service not initialized");
        }

        try {
            // Perform monitoring logic
            return new MonitorStatus(true, "System operational");
        } catch (Exception e) {
            logger.error("Monitor operation failed", e);
            throw new MonitorServiceException("Monitor operation failed", e);
        }
    }

    public MonitorStatus fallbackMonitor(Exception e) {
        logger.warn("Using fallback monitor due to: {}", e.getMessage());
        return new MonitorStatus(false, "System in degraded state");
    }

    @Override
    public Health health() {
        if (isInitialized.get()) {
            return Health.up().build();
        }
        return Health.down().withDetail("reason", "Monitor Service not initialized").build();
    }

    public static class MonitorStatus {
        private final boolean operational;
        private final String message;

        public MonitorStatus(boolean operational, String message) {
            this.operational = operational;
            this.message = message;
        }

        public boolean isOperational() {
            return operational;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MonitorServiceException extends RuntimeException {
        public MonitorServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}