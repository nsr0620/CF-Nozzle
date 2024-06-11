package com.cfnozzle.props;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class PcfProperties {

	private String pcfuser;
	private String pcfpassword;
	private String pcfhost;
	private boolean skipSslValidation = true;
	// Used for simulate
	private boolean mocking;
	// Mock response frequency(in sec)
	private Long mockFrequency = 15L;
	private List<String> mockFilter;
	
	public String getPcfuser() {
		return pcfuser;
	}
	public void setPcfuser(String pcfuser) {
		this.pcfuser = pcfuser;
	}
	public String getPcfpassword() {
		return pcfpassword;
	}
	public void setPcfpassword(String pcfpassword) {
		this.pcfpassword = pcfpassword;
	}
	public String getPcfhost() {
		return pcfhost;
	}
	public void setPcfhost(String pcfhost) {
		this.pcfhost = pcfhost;
	}
	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}
	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}
	public boolean isMocking() {
		return mocking;
	}
	public void setMocking(boolean mocking) {
		this.mocking = mocking;
	}
	public Long getMockFrequency() {
		return mockFrequency;
	}
	public void setMockFrequency(Long mockFrequency) {
		this.mockFrequency = mockFrequency;
	}
	public List<String> getMockFilter() {
		return mockFilter;
	}
	public void setMockFilter(List<String> mockFilter) {
		this.mockFilter = mockFilter;
	}
	@Override
	public String toString() {
		return "PcfProperties [pcfuser=" + pcfuser + ", pcfpassword=" + pcfpassword + ", pcfhost=" + pcfhost
				+ ", skipSslValidation=" + skipSslValidation + ", mocking=" + mocking + ", mockFrequency="
				+ mockFrequency + ", mockFilter=" + mockFilter + "]";
	}
	
	
	

}
