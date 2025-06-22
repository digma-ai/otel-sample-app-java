package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Componentpublic class MonitorService implements SmartLifecycle {

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
	}private void monitor() {
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    
    double memoryUsagePercent = ((double) usedMemory / totalMemory) * 100;
    
    System.out.printf("Memory Usage: %.2f%% (Used: %d MB, Total: %d MB)%n", 
        memoryUsagePercent,
        usedMemory / (1024 * 1024),
        totalMemory / (1024 * 1024));
    
    // Demo different states based on memory usage
    if (memoryUsagePercent > 80) {
        System.out.println("Warning: High memory usage detected");
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
    return false;
}
}