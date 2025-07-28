package org.springframework.samples.petclinic.system;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitor service that implements proper health check and lifecycle management.
 */
@Service
public class MonitorService implements SmartLifecycle, HealthIndicator {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Initialize monitoring resources here
            healthy.set(true);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // Cleanup monitoring resources here
            healthy.set(false);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    /**
     * Monitor method that performs health check instead of throwing exception
     * @return current health status
     */
    public Health monitor() {
        return health();
    }

    @Override
    public Health health() {
        if (!isRunning()) {
            return Health.down()
                .withDetail("message", "Monitor service is not running")
                .build();
        }

        if (healthy.get()) {
            return Health.up()
                .withDetail("message", "Monitor service is healthy")
                .build();
        } else {
            return Health.down()
                .withDetail("message", "Monitor service is unhealthy")
                .build();
        }
    }

    /**
     * Update the health status of the monitor
     * @param isHealthy new health status
     */
    public void setHealth(boolean isHealthy) {
        healthy.set(isHealthy);
    }
}