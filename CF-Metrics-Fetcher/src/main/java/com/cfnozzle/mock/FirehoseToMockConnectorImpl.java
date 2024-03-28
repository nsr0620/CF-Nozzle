package com.cfnozzle.mock;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.utils.MetricUtils;

@Component
public class FirehoseToMockConnectorImpl implements MockConnector {

	private static final Logger logger = Logger.getLogger(FirehoseToMockConnectorImpl.class.getCanonicalName());

//	

	@Autowired
	ProxyProperties proxyProperties;

	@Autowired
	PcfProperties pcfProperties;

	@Autowired
	FirehoseProperties firehoseProperties;

	@Autowired
	LogStorageProperties logStorageProperties;

	@Autowired
	private MockConnector firehoseToMockConnector;

	@Override
	public void mock(Event event) {
		// Create a Runnable for mocking the Firehose events
		NozzleMockRunnable r = new NozzleMockRunnable(proxyProperties, pcfProperties, logStorageProperties,
				firehoseProperties, event, firehoseToMockConnector);

		// Create a thread pool for running the mock task periodically
		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
		// Schedule the mock task to run periodically with a fixed delay
		threadPool.scheduleAtFixedRate(r, 2, pcfProperties.getMockFrequency(), TimeUnit.SECONDS);

		// Adding in a Map for Rest API, as Via rest api we need to shutdown as well,
		// when required so need to keep the Schedular object for shutdown.
		if (firehoseProperties.isEnbaleRestAPI())
			MockUtils.disposableMockObjectList.put(event.getSubscriberId(), threadPool);
		else {

			if (firehoseProperties.getSubscriptionId() == null || firehoseProperties.getSubscriptionId().isEmpty())
				firehoseProperties.setSubscriptionId(getSubscriptionID());
			MockUtils.disposableMockObjectList.put(firehoseProperties.getSubscriptionId(), threadPool);
		}

	}

	private String getSubscriptionID() {
		Random random = new Random();
		int suffix = random.nextInt(100) + 1;// random number between 1 to 100
		StringBuilder stringBuilder = new StringBuilder("cfnozzle_nozzle_");
		stringBuilder.append(System.currentTimeMillis());
		stringBuilder.append("_");
		stringBuilder.append(suffix);
		return stringBuilder.toString();
	}

	@Override
	public String disconnectMockClients(String subscriberId) {
		String returnMsg = null;

		// Check if the subscriber is actively connected and being mocked

		if (isSubscriberisOnMock(subscriberId)) {
			try {

				// Attempt to shut down the Disposable client Connection for specified
				// subscriberId.
				ScheduledExecutorService obj = MockUtils.disposableMockObjectList.get(subscriberId);
				if (obj != null) {
					try {
						obj.shutdownNow();
					} catch (Exception e) {
						logger.severe("Exception occured while shutting down disposable mock object list, reason: "+ e.getMessage());
					}
					obj = null;
					MockUtils.disposableMockObjectList.remove(subscriberId);
				}

				// Attempt to shut down the Schedular Connection for specified subscriberId.this
				// is used to fetch the data from Queue and send to Agent
				if (firehoseProperties.isEnableQueue()) {
					obj = MockUtils.SchedularMockObjectList.get(subscriberId);
					try {
						obj.shutdownNow();
					} catch (Exception e) {
						logger.severe("Exception occured while shutting down scheduler mock object list, reason: "+ e.getMessage());
					}
					obj = null;
					MockUtils.SchedularMockObjectList.remove(subscriberId);
					MockUtils.mock_metrics_queue.remove(subscriberId);
				}

				// Remove the filters - metrics which was applied from Monitor UI for specific
				// subscriberId.
				if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isMetricFilterEnable())
					MetricUtils.removeMetricFilter(subscriberId);

				// Remove health
				MetricUtils.healthCheckMap.remove(subscriberId);

				returnMsg = "Nozzle unsubscribed for " + subscriberId;
			} catch (Exception e) {
				returnMsg = "Error Occured while unsubscribe the nozzle subscriber " + subscriberId + ",reason:"
						+ e.getMessage();

				MockUtils.disposableMockObjectList.remove(subscriberId);
				if (firehoseProperties.isEnableQueue()) {
					MockUtils.SchedularMockObjectList.remove(subscriberId);
					MockUtils.mock_metrics_queue.remove(subscriberId);
				}

			}

		} else {
			// If the subscriber is not actively connected and being mocked, remove it from
			// tracking lists
			MockUtils.disposableMockObjectList.remove(subscriberId);
			if (firehoseProperties.isEnableQueue()) {
				MockUtils.SchedularMockObjectList.remove(subscriberId);
				MockUtils.mock_metrics_queue.remove(subscriberId);
			}

			returnMsg = "Nozzle subscribe " + subscriberId
					+ " is not enabled, So skipping to stop unsubscriber nozzle event.";
		}

		return returnMsg;

	}

	@Override
	public boolean isSubscriberisOnMock(String subscriberId) {
		try {
			// Retrieve the ScheduledExecutorService associated with the subscriber from the
			// tracking list
			ScheduledExecutorService disposableObj = MockUtils.disposableMockObjectList.get(subscriberId);
			// Check if the disposable object exists and if the associated thread is active
			if (disposableObj != null && (!disposableObj.isShutdown() || !disposableObj.isTerminated()))
				return true; // The subscriber is actively connected and being mocked

		} catch (Exception e) {
			logger.severe("Exception occured during subscription validation." + e.getMessage());
			return false; // An exception occurred, indicating an issue with the subscription status
		}
		return false; // The subscriber is not actively connected and being mocked
	}

	@Override
	public void healthCheckMockUpdate(String subscriberId) {
		disconnectMockClients(subscriberId);
		MetricUtils.healthCheckMap.remove(subscriberId);

	}

	@Override
	public void disconnectAllMockClients1() {

		for (Map.Entry<String, ScheduledExecutorService> entry : MockUtils.disposableMockObjectList.entrySet()) {
			try {
				disconnectMockClients(entry.getKey());// Shutting down the thread.
			} catch (Exception e) {
				logger.severe("Exception occured during disConnectAll mocking threads." + e.getMessage());
			}

		}

		// Clear the available filter list in MetricUtils
		MetricUtils.availableFilterList.clear();
	}

}
