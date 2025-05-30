package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class MonitorService implements SmartLifecycle {

    public enum ServiceState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        ERROR
    }

    private ServiceState currentState = ServiceState.STOPPED;
    private Thread backgroundThread;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");
        currentState = ServiceState.STARTING;
        
        backgroundThread = new Thread(() -> {
            currentState = ServiceState.RUNNING;
            while (currentState == ServiceState.RUNNING) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    System.out.println("Background service is running...");
                    monitorWithRetry();
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    currentState = ServiceState.ERROR;
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.start();
        System.out.println("Background service started.");
    }

    private void monitorWithRetry() {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                monitor();
                return;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (lastException != null) {
            currentState = ServiceState.ERROR;
            throw new RuntimeException("Monitor failed after " + MAX_RETRIES + " attempts", lastException);
        }
    }

    private void monitor() {
        try {
            performMonitoring();
        } catch (Exception e) {
            throw new RuntimeException("Monitoring operation failed", e);
        }
    }

    private void performMonitoring() {
        System.out.println("Performing monitoring checks...");
    }

    @Override
    public void stop() {
        currentState = ServiceState.STOPPING;
        if (backgroundThread != null) {
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        currentState = ServiceState.STOPPED;
        System.out.println("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return currentState == ServiceState.RUNNING;
    }
}