package org.springframework.samples.petclinic.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000L;

    public enum SystemStatus {
        INITIALIZING,
        RUNNING,
        STOPPED,
        ERROR
    }

    private final AtomicReference<SystemStatus> status = new AtomicReference<>(SystemStatus.STOPPED);

    @Retryable(maxAttempts = MAX_RETRIES, backoff = @Backoff(delay = RETRY_DELAY))
    public void monitor() {
        try {
            validateState();
            // Perform monitoring logic here
            status.set(SystemStatus.RUNNING);
        } catch (IllegalStateException e) {
            handleError("Monitor service failed to start", e);
            throw e;
        } catch (Exception e) {
            handleError("Unexpected error during monitoring", e);
            throw new RuntimeException("Monitor service encountered an unexpected error", e);
        }
    }

    private void validateState() {
        SystemStatus currentStatus = status.get();
        if (currentStatus == SystemStatus.ERROR) {
            throw new IllegalStateException("Monitor service is in ERROR state");
        }
        if (currentStatus == SystemStatus.RUNNING) {
            throw new IllegalStateException("Monitor service is already running");
        }
        status.set(SystemStatus.INITIALIZING);
    }

    private void handleError(String message, Exception e) {
        logger.error(message, e);
        status.set(SystemStatus.ERROR);
    }

    public boolean isRunning() {
        return status.get() == SystemStatus.RUNNING;
    }

    public void stop() {
        status.set(SystemStatus.STOPPED);
        logger.info("Monitor service stopped");
    }

    public SystemStatus getStatus() {
        return status.get();
    }
}