package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Component/**
 * A monitoring service that simulates error conditions for demonstration purposes.
 * This service intentionally throws IllegalStateException every 5 seconds
 * as part of the application's error simulation functionality.
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
	}/**
 * Monitors the system state. This method intentionally throws an IllegalStateException
 * every 5 seconds as part of the application's error simulation functionality.
 * The exception is used for demonstration purposes.
 *
 * @throws InvalidPropertiesFormatException if there's an invalid property format
 */
private void monitor() throws InvalidPropertiesFormatException {
	Utils.throwException(IllegalStateException.class,"monitor failure");
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