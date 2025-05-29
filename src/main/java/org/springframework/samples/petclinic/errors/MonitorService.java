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

@Component
public class MonitorService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private enum SystemStatus {
        UNINITIALIZED,
        STARTING,
        RUNNING,
        FAILED,
        STOPPED
    }

    private SystemStatus systemStatus = SystemStatus.UNINITIALIZED;
    private Thread backgroundThread;
    private volatile boolean running = false;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 3;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        if (systemStatus == SystemStatus.RUNNING) {
            logger.warn("Monitor service is already running");
            return;
        }

        systemStatus = SystemStatus.STARTING;
        var otelTracer = openTelemetry.getTracer("MonitorService");
        running = true;
        consecutiveFailures = 0;

        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.warn("Monitor thread interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }

                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    if (validateState()) {
                        monitor();
                        systemStatus = SystemStatus.RUNNING;
                        consecutiveFailures = 0;
                    }
                } catch (Exception e) {
                    handleMonitorError(span, e);
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.start();
        logger.info("Monitor service started successfully");
    }

    private boolean validateState() {
        if (systemStatus == SystemStatus.FAILED && consecutiveFailures >= MAX_FAILURES) {
            logger.error("Monitor service is in failed state with too many consecutive failures");
            return false;
        }
        return true;
    }

    private void handleMonitorError(Span span, Exception e) {
        consecutiveFailures++;
        systemStatus = SystemStatus.FAILED;
        span.recordException(e);
        span.setStatus(StatusCode.ERROR);
        logger.error("Monitor service encountered an error (attempt {}/{}): {}", 
            consecutiveFailures, MAX_FAILURES, e.getMessage(), e);

        if (consecutiveFailures >= MAX_FAILURES) {
            logger.error("Monitor service exceeded maximum consecutive failures, stopping service");
            stop();
        }
    }

    private void monitor() throws InvalidPropertiesFormatException {
        if (systemStatus == SystemStatus.FAILED) {
            logger.warn("Attempting recovery from failed state");
        }
        Utils.throwException(IllegalStateException.class, "monitor failure");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.interrupt();
                backgroundThread.join(5000);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping monitor service", e);
                Thread.currentThread().interrupt();
            }
        }
        systemStatus = SystemStatus.STOPPED;
        logger.info("Monitor service stopped");
    }

    @Override
    public boolean isRunning() {
        return systemStatus == SystemStatus.RUNNING && running && backgroundThread != null && backgroundThread.isAlive();
    }
}