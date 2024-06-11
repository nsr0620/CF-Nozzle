package com.cfnozzle.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cfnozzle.props.ProxyProperties;

public class ReadFromMapStreamMockRunnable implements Runnable {

	private static final Logger logger = Logger.getLogger(ReadFromMapStreamMockRunnable.class.getCanonicalName());

	ProxyProperties proxyProperties;
	String subscriberID;

	public ProxyProperties getProxyProperties() {
		return proxyProperties;
	}

	public void setProxyProperties(ProxyProperties proxyProperties) {
		this.proxyProperties = proxyProperties;
	}

	public ReadFromMapStreamMockRunnable(ProxyProperties proxyProperties, String subscriberID) {
		super();

		this.proxyProperties = proxyProperties;
		this.subscriberID = subscriberID;
	}

	@Override
	public void run() {

		try {
			List<String> data = new ArrayList<>();

			logger.log(Level.INFO, "Ready to fetch from Queue :" + MockUtils.mock_metrics_queue.size());
			for (String appEnvelop : MockUtils.getEnvelopFromMockMetricQeuue(subscriberID))
				data.add(appEnvelop);

			MockUtils.pushMockDatatoproxy(data, proxyProperties);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Can't send data to cfnozzle proxy! from Map Stream.", e);
		}

	}

}
