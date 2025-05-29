package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MonitorService implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private volatile SystemStatus status = SystemStatus.STOPPED;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;

    public enum SystemStatus {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        ERROR
    }

    @Override
    public void start() {
        logger.info("Attempting to start MonitorService");
        if (!validateStateTransition(SystemStatus.STARTING)) {
            logger.error("Invalid state transition to STARTING from {}", status);
            return;
        }
        status = SystemStatus.STARTING;
        var otelTracer = openTelemetry.getTracer("MonitorService");
        backgroundThread = new Thread(() -> {
            status = SystemStatus.RUNNING;
            logger.info("MonitorService background thread started");
            while (status == SystemStatus.RUNNING) {
                try {
                    Thread.sleep(5000);
                    Span span = otelTracer.spanBuilder("monitor").startSpan();
                    try {
                        executeWithRetry();
                    } catch (Exception e) {
                        logger.error("Unrecoverable error in monitor operation", e);
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR);
                        status = SystemStatus.ERROR;
                    } finally {
                        span.end();
                    }
                } catch (InterruptedException e) {
                    logger.warn("Monitor thread interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        backgroundThread.start();
        logger.info("MonitorService started successfully");
    }

    private void executeWithRetry() {
        AtomicInteger retryCount = new AtomicInteger(0);
        while (retryCount.get() < MAX_RETRIES) {
            try {
                monitor();
                return;
            } catch (Exception e) {
                logger.warn("Monitor operation failed, attempt {}/{}", retryCount.incrementAndGet(), MAX_RETRIES, e);
                if (retryCount.get() >= MAX_RETRIES) {
                    logger.error("Max retry attempts reached", e);
                    throw new RuntimeException("Failed after " + MAX_RETRIES + " attempts", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }

    private void monitor() throws InvalidPropertiesFormatException {
        logger.debug("Executing monitor operation");
        try {
            Utils.throwException(IllegalStateException.class, "monitor failure");
        } catch (Exception e) {
            logger.error("Monitor operation failed", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        logger.info("Attempting to stop MonitorService");
        if (!validateStateTransition(SystemStatus.STOPPING)) {
            logger.error("Invalid state transition to STOPPING from {}", status);
            return;
        }
        status = SystemStatus.STOPPING;
        if (backgroundThread != null) {