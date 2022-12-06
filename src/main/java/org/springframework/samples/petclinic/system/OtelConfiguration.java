package org.springframework.samples.petclinic.system;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder(100)
public class OtelConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(OtelConfiguration.class);

	@ConditionalOnMissingBean(OpenTelemetry.class)
	@Bean
	public OpenTelemetry getGlobalOtel() {
		OpenTelemetry otel = GlobalOpenTelemetry.get();
		logger.info("Loaded GlobalOpenTelemetry with class='{}'", otel.getClass().getName());
		return otel;
	}

}
