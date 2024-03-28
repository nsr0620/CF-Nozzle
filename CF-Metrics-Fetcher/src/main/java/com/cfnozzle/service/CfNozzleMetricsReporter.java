package com.cfnozzle.service;

import static com.cfnozzle.utils.Constants.APP_METRICS_PREFIX;
import static com.cfnozzle.utils.Constants.FIREHOSE_NOZZLE;
import static com.cfnozzle.utils.Constants.METRICS_NAME_SEP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;

@Component
public class CfNozzleMetricsReporter implements MetricsReporter {

	@Autowired
	private MetricRegistry metricRegistry;

	@VisibleForTesting
	public void setMetricRegistry(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	@Override
	public Counter registerCounter(String name) {
		return metricRegistry
				.counter(FIREHOSE_NOZZLE + METRICS_NAME_SEP + APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
	}

	@Override
	public Timer registerTimer(String name) {
		return metricRegistry.timer(FIREHOSE_NOZZLE + METRICS_NAME_SEP + APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
	}

	@Override
	public Meter registerMeter(String name) {
		return metricRegistry.meter(FIREHOSE_NOZZLE + METRICS_NAME_SEP + APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
	}
}
