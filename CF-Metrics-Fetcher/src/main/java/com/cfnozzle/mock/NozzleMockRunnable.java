package com.cfnozzle.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudfoundry.doppler.EventType;
import org.springframework.beans.factory.annotation.Autowired;

import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.utils.MetricUtils;

public class NozzleMockRunnable implements Runnable {
	ProxyProperties proxyProperties;
	FirehoseProperties firehoseProperties;
	LogStorageProperties logStorageProperties;
	PcfProperties pcfProperties;
	Event event;

	@Autowired
	com.cfnozzle.proxy.ProxyForwarder proxyForwarder;

	MockConnector firehoseToMockConnector;

	private static final Logger logger = Logger.getLogger(NozzleMockRunnable.class.getCanonicalName());

	public NozzleMockRunnable(ProxyProperties proxyProperties, PcfProperties pcfProperties,
			LogStorageProperties logStorageProperties, FirehoseProperties firehoseProperties, Event event,
			MockConnector firehoseToMockConnector) {
		super();
		this.proxyProperties = proxyProperties;
		this.pcfProperties = pcfProperties;
		this.logStorageProperties = logStorageProperties;
		this.firehoseProperties = firehoseProperties;
		this.firehoseToMockConnector = firehoseToMockConnector;
		this.event = event;
	}

	@Override
	public void run() {
		try {
			long timestamp = System.currentTimeMillis();
			List<String> data = new ArrayList<>();

			// for container based, stats will caputred from Stream in 15 sec, so donot need
			// to queue it.
			if (firehoseProperties.getEventTypes() != null
					&& firehoseProperties.getEventTypes().contains(EventType.CONTAINER_METRIC))
				MockUtils.getMockStats(MockConstant.CONTAINER_METRIC_MOCK_FILENAME, data, pcfProperties,
						firehoseProperties, timestamp, event);

			// if internal Queue enabled for value metric and counter metric
			if (firehoseProperties.isEnableQueue()) {
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.COUNTER_EVENT))
					MockUtils.pushtoInternalQueue(MockConstant.COUNTER_EVENT_MOCK_FILENAME, pcfProperties,
							firehoseProperties, timestamp, EventType.COUNTER_EVENT.toString(), event);
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.VALUE_METRIC))
					MockUtils.pushtoInternalQueue(MockConstant.VALUE_METRIC_MOCK_FILENAME, pcfProperties,
							firehoseProperties, timestamp, EventType.VALUE_METRIC.toString(), event);
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.ERROR))
					MockUtils.pushtoInternalQueue(MockConstant.ERROR_MOCK_FILENAME, pcfProperties, firehoseProperties,
							timestamp, EventType.ERROR.toString(), event);

				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.HTTP_START_STOP))
					MockUtils.pushtoInternalQueue(MockConstant.HTTP_START_STOP_FILENAME, pcfProperties,
							firehoseProperties, timestamp, EventType.HTTP_START_STOP.toString(), event);

			} else {
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.COUNTER_EVENT))
					MockUtils.getMockStats(MockConstant.COUNTER_EVENT_MOCK_FILENAME, data, pcfProperties,
							firehoseProperties, timestamp, event);
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.VALUE_METRIC))
					MockUtils.getMockStats(MockConstant.VALUE_METRIC_MOCK_FILENAME, data, pcfProperties,
							firehoseProperties, timestamp, event);
				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.ERROR))
					MockUtils.getMockStats(MockConstant.ERROR_MOCK_FILENAME, data, pcfProperties, firehoseProperties,
							timestamp, event);

				if (firehoseProperties.getEventTypes() != null
						&& firehoseProperties.getEventTypes().contains(EventType.HTTP_START_STOP))
					MockUtils.getMockStats(MockConstant.HTTP_START_STOP_FILENAME, data, pcfProperties,
							firehoseProperties, timestamp, event);
			}

			if (firehoseProperties.getEventTypes() != null
					&& firehoseProperties.getEventTypes().contains(EventType.LOG_MESSAGE))
				MockUtils.pushNFLogs(MockConstant.LOG_MESSAGE_FILENAME, proxyProperties, logStorageProperties);

			MockUtils.pushMockDatatoproxy(data, proxyProperties);

			if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isHealthCheck()) {

				for (Map.Entry<String, Long> entry : MetricUtils.healthCheckMap.entrySet()) {
					if (MetricUtils.istokenExpied(firehoseProperties, entry.getValue())) {
						logger.info("Health Check Failed , Shutting down the Mock threads.");
						firehoseToMockConnector.healthCheckMockUpdate(entry.getKey());
					}
				}

				if (MetricUtils.healthCheckMap.size() == 0) {
					MetricUtils.availableFilterList.clear();
				}

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error occured while getting data for event type, reason: {0} ", e.getMessage());
		}
	}

}
