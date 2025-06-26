package org.springframework.samples.petclinic.monitoring;

public interface MonitoringService {
    void startMonitoring();
    void stopMonitoring();
    MonitoringServiceImpl.ServiceState getState();
}