package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Component/**
 * Monitor service that performs periodic system monitoring.
 * Implements SmartLifecycle for proper Spring lifecycle management.
 */
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
                    Thread.sleep(30000); // 30 second interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                }
                
                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    System.out.println("Background service is running...");
                    monitor();
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    // Log the error for better visibility
                    System.err.println("Error in monitoring service: " + e.getMessage());
                } finally {
                    span.end();
                }
            }
        }, "MonitorService-Thread");

        // Start the background thread
        backgroundThread.start();
        System.out.println("Background service started.");
    }
private void monitor() throws MonitoringException {
		try {
			// Collect actual monitoring metrics
			Map<String, Double> metrics = new HashMap<>();
			metrics.put("cpu_usage", getCpuUsage());
			metrics.put("memory_usage", getMemoryUsage());
			metrics.put("disk_usage", getDiskUsage());

			// Log metrics
			logMetrics(metrics);

			// Simulate errors if configured
			if (configuration.isErrorSimulationEnabled()) {
				Utils.throwException(IllegalStateException.class, "Simulated monitor failure");
			}
		} catch (Exception e) {
			throw new MonitoringException("Failed to collect monitoring metrics", e);
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
				logger.error("Interrupted while stopping monitoring service", e);
			}
		}
		logger.info("Background service stopped.");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}