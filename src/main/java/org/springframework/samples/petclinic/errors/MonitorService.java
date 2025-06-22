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
	}private void monitor() throws InvalidPropertiesFormatException {
		// Check CPU usage
		Double cpuLoad = SystemMonitor.getCPULoad();
		if (cpuLoad > 90) {
			logger.warning("High CPU usage detected: " + cpuLoad + "%");
		}

		// Check memory usage
		Long memoryUsage = SystemMonitor.getMemoryUsage();
		if (memoryUsage > 85) {
			logger.warning("High memory usage detected: " + memoryUsage + "%");
		}

		// Check disk space
		Long diskSpace = SystemMonitor.getDiskSpace();
		if (diskSpace < 10) {
			logger.warning("Low disk space detected: " + diskSpace + "% remaining");
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
				logger.error("Error while stopping background thread", e);
			}
		}
		logger.info("Background service stopped.");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}