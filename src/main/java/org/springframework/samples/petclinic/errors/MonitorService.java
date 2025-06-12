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
		// Track memory usage
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();
		
		// Track thread count
		int threadCount = Thread.activeCount();
		
		// Add OpenTelemetry metrics
		Meter meter = GlobalMeterProvider.get().get("system.metrics");
		meter.gaugeBuilder("memory.usage")
			.setDescription("Current memory usage")
			.setUnit("bytes")
			.buildWithCallback(result -> result.record(usedMemory));
		
		meter.gaugeBuilder("memory.max")
			.setDescription("Maximum memory available")
			.setUnit("bytes")
			.buildWithCallback(result -> result.record(maxMemory));
		
		meter.gaugeBuilder("thread.count")
			.setDescription("Current thread count")
			.setUnit("threads")
			.buildWithCallback(result -> result.record(threadCount));
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