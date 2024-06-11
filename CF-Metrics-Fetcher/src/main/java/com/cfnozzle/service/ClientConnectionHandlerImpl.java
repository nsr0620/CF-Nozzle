package com.cfnozzle.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.proxy.ProxyForwarderImpl;
import com.cfnozzle.utils.MetricUtils;

import reactor.core.Disposable;

@Component
public class ClientConnectionHandlerImpl implements ClientConnectionHandler {
	
	private static final Logger logger = Logger.getLogger(ClientConnectionHandlerImpl.class.getCanonicalName());

	public static Map<String, Disposable> disposableObjectList = new ConcurrentHashMap<>();
	public static Map<String, ScheduledExecutorService> SchedularObjectList = new ConcurrentHashMap<>();

	@Autowired
	FirehoseProperties firehoseProperties;

	@Override
	public boolean isSubscriberisOn(String subscriberId) {
		try {
			// Check if a subscriber is connected based on their disposable object
			Disposable disposableObj = disposableObjectList.get(subscriberId);

			if (disposableObj != null && !disposableObj.isDisposed())
				return true;

		} catch (Exception e) {
			logger.severe("Exception occured whike check subscriber exist or not, reason: " + e.getMessage());
			return false;

		}
		return false;
	}

	@Override
	public String disconnectClients(String subscriberId) {
		String returnMsg = null;
		if (isSubscriberisOn(subscriberId)) {

			Disposable disposableObj = null;
			ScheduledExecutorService schedularObj = null;

			disposableObj = disposableObjectList.get(subscriberId);
			if (disposableObj != null) {
				try {
					disposableObjectList.get(subscriberId).dispose();
				} catch (Exception e) {
				}
				disposableObjectList.remove(subscriberId);
				disposableObj = null;
			}

			if (firehoseProperties.isEnableQueue()) {

				schedularObj = SchedularObjectList.get(subscriberId);
				if (schedularObj != null) {
					try {
						schedularObj.shutdownNow();
					} catch (Exception e) {
					}
					SchedularObjectList.remove(subscriberId);
					disposableObj = null;
				}
				MetricUtils.metricQueue.remove(subscriberId);
			}

			// Remove the filters - metrics which was applied from Monitor UI for specific
			// subscriberId.
			if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isMetricFilterEnable())
				MetricUtils.removeMetricFilter(subscriberId);

			MetricUtils.healthCheckMap.remove(subscriberId);

		} else {
			disposableObjectList.remove(subscriberId);

			if (firehoseProperties.isEnableQueue()) {
				SchedularObjectList.remove(subscriberId);
				MetricUtils.metricQueue.remove(subscriberId);

			}

			returnMsg = "Nozzle subscribe " + subscriberId
					+ " is not enabled, So skipping to stop unsubscriber nozzle event.";
		}

		return returnMsg;
	}

//	@Override
//	public void disconnectAllClients1() {
//
//		for (Map.Entry<String, Disposable> entry : disposableObjectList.entrySet()) {
//
//			try {
//				disconnectClients(entry.getKey());// Shutdown the thread
//			} catch (Exception e) {
//				logger.severe("Exception occured while disconneting all mock objects, reason: "+ e.getMessage());
//			}
//		}
//
//		// Clear the available filter list in MetricUtils
//		MetricUtils.availableFilterList.clear();
//
//	}

	@Override
	public void healthCheckUpdation(String subscriberId) {
		disconnectClients(subscriberId);
		MetricUtils.healthCheckMap.remove(subscriberId);

	}

}
