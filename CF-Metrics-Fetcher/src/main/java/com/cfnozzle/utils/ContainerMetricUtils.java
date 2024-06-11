package com.cfnozzle.utils;

import org.cloudfoundry.doppler.Envelope;

public class ContainerMetricUtils {
	public static String getMetricName(Envelope envelope, String suffix) {
		suffix = suffix.replaceAll("\\.", "_");
		return suffix;
		/*
		 * return getPcfMetricNamePrefix() + CONTAINER_PREFIX + METRICS_NAME_SEP +
		 * getOrigin(envelope) + METRICS_NAME_SEP + suffix;
		 */
	}
}
