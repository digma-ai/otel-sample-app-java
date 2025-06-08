package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

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
    try {
        // System health checks
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // CPU usage check
        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        
        // Log system metrics
        Logger.getLogger(getClass().getName()).info(
            String.format("Health Check - Memory Usage: %d MB, CPU Load: %.2f", 
                usedMemory / (1024 * 1024), 
                cpuLoad));
                
        // Additional checks can be added here
        
    } catch (Exception e) {
        Logger.getLogger(getClass().getName()).severe(
            "Error during system monitoring: " + e.getMessage());
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