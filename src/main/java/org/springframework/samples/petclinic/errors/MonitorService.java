package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

/**
 * A demonstration service that generates artificial errors for monitoring and observability testing.
 * This component is used to showcase error handling and monitoring capabilities in a demo environment.
 * It should not be used in production systems.
 */
@Component/**
 * A demonstration service class that simulates a monitoring system.
 * This class is intended for educational and demonstration purposes only
 * and should not be used in production environments.
 * 
 * The service runs a background thread that periodically executes monitoring
 * operations to showcase OpenTelemetry instrumentation patterns.
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
				// Add demo attribute to the span
				span.setAttribute("demo.service", "true");

				try {
					System.out.println("Background service is running...");
					monitor();
				} catch (Exception e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
					// Add demo error attribute
					span.setAttribute("demo.error", "true");
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		System.out.println("Background service started.");
	}    /**
     * Demonstration method that simulates a monitoring failure.
     * This method is intended to showcase error handling and monitoring patterns.
     * It always throws an exception as part of the demonstration.
     *
     * @throws InvalidPropertiesFormatException when the monitor simulation fails
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