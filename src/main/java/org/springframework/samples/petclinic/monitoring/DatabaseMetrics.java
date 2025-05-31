package org.springframework.samples.petclinic.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabaseMetrics {
    private final MeterRegistry registry;

    public DatabaseMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("execution(* org.springframework.samples.petclinic.activity.ClinicActivityController.*(..))")
    public Object measureQueryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder("clinic.activity.query.time")
                    .description("Time taken for clinic activity queries")
                    .tag("method", joinPoint.getSignature().getName())
                    .register(registry));
        }
    }
}