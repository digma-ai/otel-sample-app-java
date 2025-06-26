package org.springframework.samples.petclinic.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MonitoringServiceImpl implements MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringServiceImpl.class);
    
    private final AtomicReference<ServiceState> serviceState = new AtomicReference<>(ServiceState.INITIALIZED);
    private final CircuitBreaker circuitBreaker;

    public MonitoringServiceImpl() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(5)
            .slidingWindowSize(10)
            .build();
        
        this.circuitBreaker = CircuitBreaker.of("monitoringService", config);
    }

    @Override
    public void startMonitoring() {
        try {
            if (!serviceState.compareAndSet(ServiceState.INITIALIZED, ServiceState.RUNNING)) {
                throw new IllegalStateException("Cannot start monitoring: Invalid state transition from " + 
                    serviceState.get() + " to RUNNING");
            }
            logger.info("Monitoring service started successfully");
        } catch (Exception e) {
            logger.error("Failed to start monitoring service", e);
            serviceState.set(ServiceState.ERROR);
            throw e;
        }
    }

    @Override
    public void stopMonitoring() {
        try {
            ServiceState currentState = serviceState.get();
            if (currentState != ServiceState.RUNNING) {
                throw new IllegalStateException("Cannot stop monitoring: Service is not running. Current state: " + currentState);
            }
            serviceState.set(ServiceState.STOPPED);
            logger.info("Monitoring service stopped successfully");
        } catch (Exception e) {
            logger.error("Failed to stop monitoring service", e);
            serviceState.set(ServiceState.ERROR);
            throw e;
        }
    }

    @Override
    public ServiceState getState() {
        return serviceState.get();
    }

    public enum ServiceState {
        INITIALIZED,
        RUNNING,
        STOPPED,
        ERROR
    }
}