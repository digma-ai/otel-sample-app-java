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
	@Autowired
	private Logger logger;

	@Override
	public void start() {
		var otelTracer = openTelemetry.getTracer("MonitorService");

		running = true;
		backgroundThread = new Thread(() -> {
			while (running) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					logger.error("Monitor service interrupted", e);
					Thread.currentThread().interrupt();
					return;
				}
				Span span = otelTracer.spanBuilder("monitor").startSpan();

				try {
					logger.info("Background service is running...");
					monitor();
				} catch (IllegalStateException e) {
					logger.error("Illegal state detected in monitor service", e);
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
					recoverFromError();
				} catch (Exception e) {
					logger.error("Unexpected error in monitor service", e);
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		logger.info("Background service started.");
	}private void monitor() {
		try {
			if (!running) {
				throw new IllegalStateException("Service is not running");
			}
			// Monitor logic here
		} catch (IllegalStateException e) {
			Logger.getLogger(MonitorService.class.getName()).severe("Monitor failure: " + e.getMessage());
			try {
				// Recovery mechanism
				restart();
			} catch (Exception recoveryEx) {
				Logger.getLogger(MonitorService.class.getName()).severe("Recovery failed: " + recoveryEx.getMessage());
			}
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
		Logger.getLogger(MonitorService.class.getName()).info("Background service stopped.");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}