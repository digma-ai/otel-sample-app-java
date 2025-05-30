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
import java.util.Objects;

@Component
public class MonitorService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        if (isRunning()) {
            throw new IllegalStateException("Monitor service is already running");
        }
        if (openTelemetry == null) {
            throw new IllegalStateException("OpenTelemetry instance not initialized");
        }

        var otelTracer = openTelemetry.getTracer("MonitorService");
        logger.info("Starting monitor service");

        try {
            running = true;
            backgroundThread = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.warn("Monitor service interrupted during sleep", e);
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Span span = otelTracer.spanBuilder("monitor").startSpan();
                    try {
                        logger.debug("Executing monitor cycle");
                        monitor();
                        logger.info("Background service is running...");
                    } catch (IllegalStateException e) {
                        logger.error("Monitor operation failed due to illegal state", e);
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR, "Monitor operation failed: " + e.getMessage());
                    } catch (Exception e) {
                        logger.error("Unexpected error during monitoring", e);
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR, "Unexpected monitoring error: " + e.getMessage());
                    } finally {
                        span.end();
                    }
                }
            });

            backgroundThread.start();
            logger.info("Background service started successfully");
        } catch (Exception e) {
            running = false;
            logger.error("Failed to start monitor service", e);
            throw new IllegalStateException("Failed to start monitor service", e);
        }
    }

    private void monitor() throws InvalidPropertiesFormatException {
        if (!running) {
            throw new IllegalStateException("Cannot monitor when service is not running");
        }
        try {
            Utils.throwException(IllegalStateException.class, "monitor failure");
        } catch (IllegalStateException e) {
            logger.error("Monitor operation failed", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            logger.warn("Attempt to stop non-running monitor service");
            return;
        }

        logger.info("Stopping monitor service");
        try {
            running = false;
            if (backgroundThread != null) {
                try {
                    backgroundThread.join(5000);
                    if (backgroundThread.isAlive()) {
                        logger.warn("Background thread did not terminate gracefully");
                        backgroundThread.interrupt();
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while stopping monitor service", e);
                    Thread.currentThread().interrupt();
                }
            }
            logger.info("Background service stopped successfully");
        } catch (Exception e) {
            logger.error("Error while stopping monitor service", e);
            throw new IllegalStateException("Failed to stop monitor service", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running && (backgroun