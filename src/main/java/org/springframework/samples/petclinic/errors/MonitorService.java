package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Component
public class MonitorService implements SmartLifecycle {

	private boolean running = false;
	private Thread backgroundThread;
	@Autowired
	private OpenTelemetry openTelemetry;
	private int consecutiveFailures = 0;
	private static final int MAX_FAILURES = 3;
	private static final long CIRCUIT_BREAKER_RESET_TIME = 60000; // 1 minute
	private long lastFailureTime = 0;

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

	private void monitor() throws InvalidPropertiesFormatException {
		// Circuit breaker pattern
		if (consecutiveFailures >= MAX_FAILURES) {
			if (System.currentTimeMillis() - lastFailureTime < CIRCUIT_BREAKER_RESET_TIME) {
				System.out.println("Circuit breaker is open. Skipping monitoring.");
				return;
			} else {
				// Reset circuit breaker
				consecutiveFailures = 0;
			}
		}

		try {
			// Perform monitoring tasks
			performHealthCheck();
			// If successful, reset failure count
			consecutiveFailures = 0;
		} catch (Exception e) {
			consecutiveFailures++;
			lastFailureTime = System.currentTimeMillis();
			System.out.println("Monitor failure occurred. Consecutive failures: " + consecutiveFailures);
			throw e;
		}
	}

	private void performHealthCheck() {
		// Add actual health check logic here
		// For now, we'll just simulate a successful check
		System.out.println("Health check completed successfully");
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