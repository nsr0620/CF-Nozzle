package com.cfnozzle.props;

import java.util.List;

import org.cloudfoundry.doppler.EventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class FirehoseProperties {

	private List<EventType> eventTypes;
	private List<String> apps;
	private List<String> spaces;
	private List<String> organizations;

	private boolean filterExclude;
	private String subscriptionId;
	private int parallelism = 4;
	private long healthTimeCheck = 4;
	private boolean healthCheck = true;

	private boolean metricFilterEnable = true;

	// ReadFromMapStream frequency(in sec)
	private Long queueFrequency = 25L;

	// enabling Map Streaming(true/false- defaut- is false)
	private boolean enableQueue = true;

	private boolean enbaleRestAPI = true;

	public List<EventType> getEventTypes() {
		return eventTypes;
	}

	public void setEventTypes(List<EventType> eventTypes) {
		this.eventTypes = eventTypes;
	}

	public List<String> getApps() {
		return apps;
	}

	public void setApps(List<String> apps) {
		this.apps = apps;
	}

	public List<String> getSpaces() {
		return spaces;
	}

	public void setSpaces(List<String> spaces) {
		this.spaces = spaces;
	}

	public List<String> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<String> organizations) {
		this.organizations = organizations;
	}

	public boolean isFilterExclude() {
		return filterExclude;
	}

	public void setFilterExclude(boolean filterExclude) {
		this.filterExclude = filterExclude;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public int getParallelism() {
		return parallelism;
	}

	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	public long getHealthTimeCheck() {
		return healthTimeCheck*60000;
	}

	public void setHealthTimeCheck(long healthTimeCheck) {
		this.healthTimeCheck = healthTimeCheck;
	}

	public boolean isHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(boolean healthCheck) {
		this.healthCheck = healthCheck;
	}

	public boolean isMetricFilterEnable() {
		return metricFilterEnable;
	}

	public void setMetricFilterEnable(boolean metricFilterEnable) {
		this.metricFilterEnable = metricFilterEnable;
	}

	public Long getQueueFrequency() {
		return queueFrequency;
	}

	public void setQueueFrequency(Long queueFrequency) {
		this.queueFrequency = queueFrequency;
	}

	public boolean isEnableQueue() {
		return enableQueue;
	}

	public void setEnableQueue(boolean enableQueue) {
		this.enableQueue = enableQueue;
	}

	public boolean isEnbaleRestAPI() {
		return enbaleRestAPI;
	}

	public void setEnbaleRestAPI(boolean enbaleRestAPI) {
		this.enbaleRestAPI = enbaleRestAPI;
	}

	@Override
	public String toString() {
		return "FirehoseProperties [eventTypes=" + eventTypes + ", apps=" + apps + ", spaces=" + spaces
				+ ", organizations=" + organizations + ", filterExclude=" + filterExclude + ", subscriptionId="
				+ subscriptionId + ", parallelism=" + parallelism + ", healthTimeCheck=" + healthTimeCheck
				+ ", healthCheck=" + healthCheck + ", metricFilterEnable=" + metricFilterEnable + ", queueFrequency="
				+ queueFrequency + ", enableQueue=" + enableQueue + ", enbaleRestAPI=" + enbaleRestAPI + "]";
	}

	
	

}
