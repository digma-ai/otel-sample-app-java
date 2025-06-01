package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
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
    private static final String MONITOR_NAME = "MonitorService";
    private static final long MONITORING_INTERVAL = 5000;

    private boolean running = false;
    private Thread backgroundThread;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        if (openTelemetry == null) {
            logger.error("OpenTelemetry instance not initialized");
            throw new IllegalStateException("OpenTelemetry instance not initialized");
        }

        var otelTracer = openTelemetry.getTracer(MONITOR_NAME);
        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(MONITORING_INTERVAL);
                } catch (InterruptedException e) {
                    logger.warn("Monitoring thread interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }

                Span span = otelTracer.spanBuilder("monitor")
                    .setAttribute("service.name", MONITOR_NAME)
                    .setAttribute("monitoring.interval", MONITORING_INTERVAL)
                    .startSpan();

                try {
                    validateMonitoringState();
                    logger.debug("Background service is running...");
                    monitor();
                    span.setStatus(StatusCode.OK);
                } catch (InvalidPropertiesFormatException e) {
                    logger.error("Invalid properties in monitoring", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, "Invalid properties format");
                } catch (IllegalStateException e) {
                    logger.error("Monitoring state error", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, "Monitor failure");
                } catch (Exception e) {
                    logger.error("Unexpected error during monitoring", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, "Unexpected monitoring error");
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.start();
        logger.info("Background service started");
    }

    private void validateMonitoringState() {
        Objects.requireNonNull(openTelemetry, "OpenTelemetry must not be null");
        if (!running) {
            throw new IllegalStateException("Monitor service is not running");
        }
    }

    private void monitor() throws InvalidPropertiesFormatException {
        Span span = Span.current();
        span.setAttributes(
            Attributes.of(
                AttributeKey.stringKey("monitor.status"), "active",
                AttributeKey.longKey("monitor.timestamp"), System.currentTimeMillis()
            )
        );
        Utils.throwException(IllegalStateException.class, "monitor failure");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.interrupt();
                backgroundThread.join(MONITORING_INTERVAL);
                logger.info("Background service stopped gracefully");
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping background service", e);
                Thread.