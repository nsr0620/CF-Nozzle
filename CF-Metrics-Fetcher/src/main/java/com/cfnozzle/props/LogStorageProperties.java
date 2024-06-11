package com.cfnozzle.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class LogStorageProperties {
	private String logUrl = "http://127.0.0.1:7112";
	private String logIndexprefix = "cloudfoundry";
	private int logConnecttimeout = 60000;
	private int logReadtimeout = 60000;
	private int logIngestionthreads = 2;
	private int logBatchsize = 125;
	public String getLogUrl() {
		return logUrl;
	}
	public void setLogUrl(String logUrl) {
		this.logUrl = logUrl;
	}
	public String getLogIndexprefix() {
		return logIndexprefix;
	}
	public void setLogIndexprefix(String logIndexprefix) {
		this.logIndexprefix = logIndexprefix;
	}
	public int getLogConnecttimeout() {
		return logConnecttimeout;
	}
	public void setLogConnecttimeout(int logConnecttimeout) {
		this.logConnecttimeout = logConnecttimeout;
	}
	public int getLogReadtimeout() {
		return logReadtimeout;
	}
	public void setLogReadtimeout(int logReadtimeout) {
		this.logReadtimeout = logReadtimeout;
	}
	public int getLogIngestionthreads() {
		return logIngestionthreads;
	}
	public void setLogIngestionthreads(int logIngestionthreads) {
		this.logIngestionthreads = logIngestionthreads;
	}
	public int getLogBatchsize() {
		return logBatchsize;
	}
	public void setLogBatchsize(int logBatchsize) {
		this.logBatchsize = logBatchsize;
	}
	@Override
	public String toString() {
		return "LogStorageProperties [logUrl=" + logUrl + ", logIndexprefix=" + logIndexprefix + ", logConnecttimeout="
				+ logConnecttimeout + ", logReadtimeout=" + logReadtimeout + ", logIngestionthreads="
				+ logIngestionthreads + ", logBatchsize=" + logBatchsize + "]";
	}
	
	
}
