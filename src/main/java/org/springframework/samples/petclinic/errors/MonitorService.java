package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MonitorService implements SmartLifecycle {

    public enum SystemStatus {
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        ERROR
    }

    private volatile SystemStatus currentStatus = SystemStatus.STOPPED;
    private volatile boolean running = false;
    private Thread backgroundThread;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final int MAX_RETRIES = 3;
    private final long RETRY_DELAY_MS = 1000;
    private volatile Exception lastError;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");
        currentStatus = SystemStatus.STARTING;
        running = true;
        retryCount.set(0);
        lastError = null;

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
                    currentStatus = SystemStatus.RUNNING;
                    monitor();
                    retryCount.set(0);
                } catch (Exception e) {
                    lastError = e;
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    currentStatus = SystemStatus.ERROR;

                    if (retryCount.incrementAndGet() <= MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * retryCount.get());
                            continue;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.start();
        System.out.println("Background service started.");
    }

    private void monitor() throws InvalidPropertiesFormatException {
        try {
            Utils.throwException(IllegalStateException.class, "monitor failure");
        } catch (Exception e) {
            throw new InvalidPropertiesFormatException("Monitor operation failed: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        currentStatus = SystemStatus.STOPPING;
        running = false;
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        currentStatus = SystemStatus.STOPPED;
        System.out.println("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running && currentStatus == SystemStatus.RUNNING;
    }

    public SystemStatus getCurrentStatus() {
        return currentStatus;
    }

    public Exception getLastError() {
        return lastError;
    }
}