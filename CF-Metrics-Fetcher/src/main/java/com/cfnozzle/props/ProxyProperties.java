package com.cfnozzle.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class ProxyProperties {
	private String proxyHostname;
	private int proxyPort;
	private boolean enableLogs;
	private int packetbatchSize = 30;
	
	public String getProxyHostname() {
		return proxyHostname;
	}
	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}
	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public boolean isEnableLogs() {
		return enableLogs;
	}
	public void setEnableLogs(boolean enableLogs) {
		this.enableLogs = enableLogs;
	}
	public int getPacketbatchSize() {
		return packetbatchSize;
	}
	public void setPacketbatchSize(int packetbatchSize) {
		this.packetbatchSize = packetbatchSize;
	}
	@Override
	public String toString() {
		return "ProxyProperties [proxyHostname=" + proxyHostname + ", proxyPort=" + proxyPort + ", enableLogs="
				+ enableLogs + ", packetbatchSize=" + packetbatchSize + "]";
	}
	
	
}
	