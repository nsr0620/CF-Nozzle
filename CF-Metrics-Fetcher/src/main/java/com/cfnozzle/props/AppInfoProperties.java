package com.cfnozzle.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class AppInfoProperties {

	private boolean fetchAppInfo = true;
	private int appInfoCacheSize = 5000;
	private int cacheExpireIntervalHours = 6;

	public boolean isFetchAppInfo() {
		return fetchAppInfo;
	}

	public void setFetchAppInfo(boolean fetchAppInfo) {
		this.fetchAppInfo = fetchAppInfo;
	}

	public int getAppInfoCacheSize() {
		return appInfoCacheSize;
	}

	public void setAppInfoCacheSize(int appInfoCacheSize) {
		this.appInfoCacheSize = appInfoCacheSize;
	}

	public int getCacheExpireIntervalHours() {
		return cacheExpireIntervalHours;
	}

	public void setCacheExpireIntervalHours(int cacheExpireIntervalHours) {
		this.cacheExpireIntervalHours = cacheExpireIntervalHours;
	}
}
