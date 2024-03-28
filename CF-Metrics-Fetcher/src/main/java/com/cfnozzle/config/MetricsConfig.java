package com.cfnozzle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;

@Configuration
public class MetricsConfig {
	// Define a Spring Bean for the MetricRegistry, which is used to collect and
	// manage metrics.
	@Bean
	public MetricRegistry metricRegistry() {
		// Create and return a new instance of MetricRegistry.
		return new MetricRegistry();
	}
}