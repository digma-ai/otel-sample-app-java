package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Component/**
 * Service that provides system monitoring capabilities with OpenTelemetry integration.
 * Implements SmartLifecycle for proper Spring lifecycle management.
 */
public class MonitorService implements SmartLifecycle {

    private boolean running = false;
    private Thread backgroundThread;
    
    @Autowired
    private OpenTelemetry openTelemetry;
    
    @Value("${monitor.error.injection.enabled:false}")
    private boolean errorInjectionEnabled;
    
    @Value("${monitor.interval.ms:5000}")
    private long monitorIntervalMs;

    /**
     * Starts the monitoring service background thread.
     * Creates spans for monitoring operations and handles errors appropriately.
     */
    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");

        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(monitorIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Span span = otelTracer.spanBuilder("monitor")
                    .setAttribute("errorInjection.enabled", errorInjectionEnabled)
                    .startSpan();

                try {
                    if (errorInjectionEnabled) {
                        throw new RuntimeException("Injected error for testing");
                    }
                    
                    System.out.println("Background service is running...");
                    monitor();
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, e.getMessage());
                    System.err.println("Error in monitoring service: " + e.getMessage());
                } finally {
                    span.end();
                }
            }
        }, "MonitorService-Thread");

        backgroundThread.setDaemon(true);
        backgroundThread.start();
        System.out.println("Background service started.");
    }/**
 * Monitors the system state and performs health checks.
 * Can be configured to inject errors for testing purposes.
 *
 * @throws InvalidPropertiesFormatException if monitoring configuration is invalid
 */
private void monitor() throws InvalidPropertiesFormatException {
    if (ErrorInjector.shouldInjectError("monitor")) {
        Utils.throwException(IllegalStateException.class, "Injected monitor failure");
    }
    // Add actual monitoring logic here
}

/**
 * Stops the background service and ensures clean shutdown.
 * Waits for background thread to complete before stopping.
 */
@Override
public void stop() {
    // Stop the background task
    running = false;
    if (backgroundThread != null) {
        try {
            backgroundThread.join(THREAD_TIMEOUT_MS); // Wait for the thread to finish with timeout
        } catch (InterruptedException e) {
            logger.warn("Background thread interrupted during shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
    logger.info("Background service stopped successfully.");
}

/**
 * Checks if the service is currently running.
 *
 * @return true if the service is running, false otherwise
 */
@Override
public boolean isRunning() {
    return running && backgroundThread != null && backgroundThread.isAlive();
}
}