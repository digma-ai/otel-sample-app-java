package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

/**
 * Monitor Service that intentionally throws exceptions for demonstration purposes.
 * This service is designed to showcase error handling and monitoring capabilities
 * in a demo environment. The exceptions thrown here are EXPECTED and are part of
 * the demonstration - they should not be treated as actual errors.
 *
 * The service throws an exception every 5 seconds (configurable) to demonstrate:
 * 1. Error handling patterns
 * 2. OpenTelemetry integration
 * 3. Monitoring and observability features
 */
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
					// Demo exception frequency - throws every 5 seconds
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

	/**
	 * Intentionally throws an exception for demonstration purposes.
	 * This is an expected behavior used to showcase error handling.
	 * 
	 * @throws InvalidPropertiesFormatException Demo exception
	 */
	private void monitor() throws InvalidPropertiesFormatException {
		Utils.throwException(IllegalStateException.class, "monitor failure - Demo Exception");
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