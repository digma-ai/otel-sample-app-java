package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Componentpublic class MonitorService implements SmartLifecycle {

    private static final Logger logger = Logger.getLogger(MonitorService.class.getName());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread backgroundThread;
    
    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public synchronized void start() {
        if (isRunning()) {
            return;
        }

        var otelTracer = openTelemetry.getTracer("MonitorService");
        running.set(true);
        
        backgroundThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(5000);
                    Span span = otelTracer.spanBuilder("monitor").startSpan();

                    try {
                        logger.info("Background service is running...");
                        performMonitoring();
                    } catch (Exception e) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR);
                        logger.warning("Monitor service encountered an error: " + e.getMessage());
                    } finally {
                        span.end();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.start();
        logger.info("Background service started.");
    }@Override
public synchronized void stop() {
    if (!isRunning()) {
        return;
    }

    running.set(false);
    if (backgroundThread != null) {
        try {
            backgroundThread.interrupt();
            backgroundThread.join(5000); // Wait up to 5 seconds for the thread to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    logger.info("Background service stopped.");
}

@Override
public boolean isRunning() {
    return running.get();
}
}