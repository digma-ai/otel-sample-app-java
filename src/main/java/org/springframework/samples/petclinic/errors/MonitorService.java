package org.springframework.samples.petclinic.errors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

/**
 * Monitor Service with Error Simulation Capability
 * 
 * This service includes an intentional error simulation feature that throws exceptions
 * periodically when enabled. This is useful for testing error handling, monitoring,
 * and alerting systems in different environments.
 * 
 * The error simulation can be controlled via the 'monitor.simulation.enabled' property:
 * - Set to 'true' to enable error simulation
 * - Set to 'false' to disable error simulation (default)
 * 
 * When enabled, the service will throw a simulated IllegalStateException every 5 seconds.
 * These exceptions are marked with metadata to distinguish them from real errors.
 */
@Service
@Slf4j
public class MonitorService implements SmartLifecycle {
    
    @Value("${monitor.simulation.enabled:false}")
    private boolean simulationEnabled;

    private boolean running = false;
    private Thread backgroundThread;
    
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");
        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                
                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    if (simulationEnabled) {
                        log.info("Error simulation is enabled - throwing simulated exception");
                        Utils.throwSimulatedException();
                    }
                    log.debug("Monitor check completed successfully");
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    throw e;
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.start();
        log.info("Background service started with simulation {}", simulationEnabled ? "enabled" : "disabled");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}