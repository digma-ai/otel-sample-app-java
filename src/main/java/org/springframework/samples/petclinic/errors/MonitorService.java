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
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING
    }

    private State currentState = State.STOPPED;
    private Thread backgroundThread;
    
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");
        
        synchronized(this) {
            if (currentState != State.STOPPED) {
                return;
            }
            currentState = State.STARTING;
        }

        backgroundThread = new Thread(() -> {
            currentState = State.RUNNING;
            while (currentState == State.RUNNING) {
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
                    retryMonitor();
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

    private void retryMonitor() {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                monitor();
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts == MAX_RETRIES) {
                    logger.error("Monitor failed after {} retries", MAX_RETRIES, e);
                    throw e;
                }
                logger.warn("Monitor attempt {} failed, retrying...", attempts, e);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }

    private void monitor() {
        logger.debug("Performing monitoring check");
    }

    @Override
    public void stop() {
        synchronized(this) {
            if (currentState != State.RUNNING) {
                return;
            }
            currentState = State.STOPPING;
        }

        if (backgroundThread != null) {
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping monitor service", e);
                Thread.currentThread().interrupt();
            }
        }
        
        currentState = State.STOPPED;
        logger.info("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return currentState == State.RUNNING;
    }
}