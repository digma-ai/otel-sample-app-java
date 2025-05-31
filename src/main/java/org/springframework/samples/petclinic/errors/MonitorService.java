package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Service that monitors the application state and health.
 * Implements SmartLifecycle for proper lifecycle management.
 */
@Component
public class MonitorService implements SmartLifecycle {

    private volatile boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        var otelTracer = openTelemetry.getTracer("MonitorService");
        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    System.out.println("Background service is running...");
                    monitor();
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    System.err.println("Error in monitor service: " + e.getMessage());
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
        System.out.println("Background service started.");
    }

    /**
     * Performs the actual monitoring logic.
     * Checks system health and records metrics.
     */
    private void monitor() {
        // Implement actual monitoring logic here
        // For now, just log the monitoring activity
        System.out.println("Monitoring system health...");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(5000); // Wait up to 5 seconds for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}