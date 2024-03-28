package com.cfnozzle.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cfnozzle.proxy.ProxyForwarder;
import com.cfnozzle.utils.MetricUtils;

public class ReadFromMapStreamRunnable implements Runnable{
	
	private static final Logger logger = Logger.getLogger(ReadFromMapStreamRunnable.class.getCanonicalName());
	
	 ProxyForwarder proxyForwarder;
	
	String subscriberid;
	
	public ReadFromMapStreamRunnable (String subscriberid,ProxyForwarder proxyForwarder) {
	
		this.subscriberid=subscriberid;
		this.proxyForwarder=proxyForwarder;
	}

	@Override
	public void run() {

		try {

			try {

				if (MetricUtils.metricQueue.get(subscriberid) != null)
					logger.log(Level.INFO, "Queue size for subscriberId " + subscriberid + " is "
							+ MetricUtils.metricQueue.get(subscriberid).size());
				else
					logger.log(Level.INFO, "Queue is empty for subscriberId - " + subscriberid);
			} catch (Exception e) {

			}

			proxyForwarder.forwardAll(MetricUtils.getMetricKeyFromQueue(subscriberid));

		} catch (Exception e) {
			logger.log(Level.WARNING, "Can't send data to cfnozzle proxy! from Map Stream.", e);
		}
	}

}
