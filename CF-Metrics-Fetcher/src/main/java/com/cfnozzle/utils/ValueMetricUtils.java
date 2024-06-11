package com.cfnozzle.utils;

import org.cloudfoundry.doppler.Envelope;

public class ValueMetricUtils {
	public static String getMetricName(Envelope envelope) {

		String val = envelope.getValueMetric().getName() + "_" + envelope.getValueMetric().getUnit();
		val = val.replaceAll("\\.", "_");
		return val;
		/*
		 * return getPcfMetricNamePrefix() + getOrigin(envelope) + METRICS_NAME_SEP +
		 * envelope.getValueMetric().getName() + METRICS_NAME_SEP +
		 * envelope.getValueMetric().getUnit();
		 */
	}
}
