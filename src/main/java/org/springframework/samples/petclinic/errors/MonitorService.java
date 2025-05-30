package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class MonitorService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private volatile boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");

        running = true;
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
                    logger.info("Background service is running...");
                    monitor();
                } catch (Exception e) {
                    logger.error("Error in monitor service", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.start();
        logger.info("Background service started.");
    }

    private void monitor() {
        if (!running) {
            logger.warn("Monitor called while service is not running");
            return;
        }
        logger.debug("Performing monitoring check");
    }

    @Override
    public void stop() {
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping background thread", e);
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}