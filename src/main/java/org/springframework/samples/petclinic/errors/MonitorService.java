package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

/**
 * Service responsible for monitoring system health and generating test exceptions when configured.
 * This component implements SmartLifecycle to automatically start monitoring when the application starts.
 * It uses OpenTelemetry for tracing and monitoring capabilities.
 *
 * @author Spring Pet Clinic Team
 */
@Component/**
 * Service responsible for monitoring system health and performance metrics.
 * Runs periodic checks in the background and reports metrics using OpenTelemetry.
 */
public class MonitorService implements SmartLifecycle {

    private boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;
    
    @Value("${monitor.test.exceptions.enabled:false}")
    private boolean testExceptionsEnabled;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");

        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                Span span = null;
                try {
                    Thread.sleep(5000);
                    span = otelTracer.spanBuilder("monitor").startSpan();
                    
                    System.out.println("Background service is running...");
                    monitor();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Monitor service interrupted: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    if (span != null) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR);
                    }
                    System.err.println("Error in monitor service: " + e.getMessage());
                } finally {
                    if (span != null) {
                        span.end();
                    }
                }
            }
        }, "MonitorService-Thread");

        backgroundThread.setDaemon(true);
        backgroundThread.start();
        System.out.println("Background service started.");
    }    /**
     * Monitors the system state and performs necessary checks.
     * If test mode is enabled, throws test exceptions as configured.
     * Otherwise performs actual monitoring tasks.
     *
     * @throws InvalidPropertiesFormatException if monitoring configuration is invalid
     */
    private void monitor() throws InvalidPropertiesFormatException {
        try {
            if (throwTestExceptions) {
                Utils.throwException(IllegalStateException.class, "monitor failure");
            }
            
            // Perform actual monitoring tasks
            performHealthCheck();
            checkResources();
            logSystemStatus();
            
        } catch (IllegalStateException e) {
            if (throwTestExceptions) {
                throw e;
            }
            logger.error("Monitor encountered an illegal state: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during monitoring: {}", e.getMessage());
            // Allow monitor to continue running despite errors
        }
    }

    @Override
    public void stop() {
        // Stop the background task
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join(5000); // Wait up to 5 seconds for the thread to finish
                if (backgroundThread.isAlive()) {
                    logger.warn("Background thread did not stop within timeout");
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping background thread");
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running && (backgroundThread != null && backgroundThread.isAlive());
    }
}