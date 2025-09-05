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
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Span span = otelTracer.spanBuilder("system.health.check").startSpan();
                try {
                    span.setAttribute("component", "system-monitor");
                    performHealthCheck();
                    span.setStatus(StatusCode.OK);
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    span.setAttribute("error", true);
                    span.setAttribute("error.message", e.getMessage());
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }private void monitor() {
		try {
			// Basic system health monitoring
			Runtime runtime = Runtime.getRuntime();
			long usedMemory = runtime.totalMemory() - runtime.freeMemory();
			long maxMemory = runtime.maxMemory();
			double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
			
			// Record metrics using OpenTelemetry
			Meter meter = GlobalOpenTelemetry.getMeter("monitor-service");
			meter.gaugeBuilder("memory.used").ofLongs().buildWithCallback(m -> m.record(usedMemory));
			meter.gaugeBuilder("memory.max").ofLongs().buildWithCallback(m -> m.record(maxMemory));
			meter.gaugeBuilder("cpu.load").buildWithCallback(m -> m.record(cpuLoad));
			
		} catch (Exception e) {
			// Log the error and create a span to record the failure
			Span span = GlobalOpenTelemetry.getTracer("monitor-service")
				.spanBuilder("monitor.error")
				.setStatus(StatusCode.ERROR)
				.startSpan();
			span.recordException(e);
			span.end();
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
		return running;
	}
}