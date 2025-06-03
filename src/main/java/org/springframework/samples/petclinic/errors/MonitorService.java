package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

@Component
public class MonitorService implements SmartLifecycle {

    private boolean running = false;
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
                    throw new RuntimeException(e);
                }
                Span span = otelTracer.spanBuilder("monitor").startSpan();

                try {
                    System.out.println("Background service is running...");
                    monitor();
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        // Start the background thread
        backgroundThread.start();
        System.out.println("Background service started.");
    }

    private void monitor() {
        // Get memory information
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = ((double) heapUsed / heapMax) * 100;

        // Get thread information
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        
        // Log monitoring metrics
        System.out.printf("Health Check - Memory: %.2f%% used, Threads: %d active%n", 
            heapUsagePercent, threadCount);

        // Add warning if memory usage is high
        if (heapUsagePercent > 80) {
            System.out.println("WARNING: High memory usage detected!");
        }
    }

    @Override
    public void stop() {
        // Stop the background task
        running = false;
        if (backgroundThread != null) {
            try {
                backgroundThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Background service stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}