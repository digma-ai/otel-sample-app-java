package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MonitorService implements SmartLifecycle, HealthIndicator {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread backgroundThread;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final int FAILURE_THRESHOLD = 3;
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private long lastFailureTime;
    private final long RESET_TIMEOUT = 60000;

    @Autowired
    private OpenTelemetry openTelemetry;

    public class MonitorServiceException extends RuntimeException {
        public MonitorServiceException(String message) {
            super(message);
        }
        public MonitorServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            var otelTracer = openTelemetry.getTracer("MonitorService");

            backgroundThread = new Thread(() -> {
                while (running.get()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new MonitorServiceException("Monitor service interrupted", e);
                    }

                    Span span = otelTracer.spanBuilder("monitor").startSpan();
                    try {
                        if (!circuitOpen.get()) {
                            System.out.println("Background service is running...");
                            monitor();
                            failureCount.set(0);
                        } else if ((System.currentTimeMillis() - lastFailureTime) > RESET_TIMEOUT) {
                            circuitOpen.set(false);
                            failureCount.set(0);
                        }
                    } catch (Exception e) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR);
                        handleFailure(e);
                    } finally {
                        span.end();
                    }
                }
            });

            backgroundThread.start();
            System.out.println("Background service started.");
        }
    }

    private void handleFailure(Exception e) {
        int currentFailures = failureCount.incrementAndGet();
        if (currentFailures >= FAILURE_THRESHOLD) {
            circuitOpen.set(true);
            lastFailureTime = System.currentTimeMillis();
            throw new MonitorServiceException("Circuit breaker opened due to multiple failures", e);
        }
    }

    private void monitor() {
        if (circuitOpen.get()) {
            throw new MonitorServiceException("Circuit breaker is open");
        }
        try {
            Utils.throwException(IllegalStateException.class, "monitor failure");
        } catch (Exception e) {
            throw new MonitorServiceException("Monitor operation failed", e);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (backgroundThread != null) {
                try {
                    backgroundThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new MonitorServiceException("Error stopping monitor service", e);
                }
            }
            System.out.println("Background service stopped.");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get