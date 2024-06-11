package com.cfnozzle.utils;

import org.cloudfoundry.doppler.Envelope;

public class CounterEventUtils {
	public static String getMetricName(Envelope envelope, String suffix) {

		String val = envelope.getCounterEvent().getName() + "_" + suffix;
		val = val.replaceAll("\\.", "_");
		return val;
		/*
		 * return getPcfMetricNamePrefix() + getOrigin(envelope) + METRICS_NAME_SEP +
		 * envelope.getCounterEvent().getName() + METRICS_NAME_SEP + suffix;
		 */
	}
}
