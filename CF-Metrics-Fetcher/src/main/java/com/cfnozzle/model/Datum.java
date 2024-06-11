package com.cfnozzle.model;

public class Datum {

	public String metricName;
	public String pattern;

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String toString() {
		return "Datum [metricName=" + metricName + ", pattern=" + pattern + "]";
	}

}
