package com.cfnozzle.mock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.ProxyProperties;

@Component
public class ReadFromMapStreamMockImpl implements ReadFromMapMockStream {

	@Autowired
	FirehoseProperties firehoseProperties;

	@Autowired
	ProxyProperties proxyProperties;

	@Override
	public void readfromMockQueue(Event event) {
		String subscriberId = null;

		if (firehoseProperties.isEnbaleRestAPI())
			subscriberId = event.getSubscriberId();
		else
			subscriberId = firehoseProperties.getSubscriptionId();

		if (subscriberId != null) {
			// Create a runnable for reading from the mock queue
			ReadFromMapStreamMockRunnable r = new ReadFromMapStreamMockRunnable(proxyProperties, subscriberId);
			// Create a ScheduledExecutorService (thread pool) with a single thread
			ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
			// Schedule the runnable to run periodically
			threadPool.scheduleAtFixedRate(r, 5, firehoseProperties.getQueueFrequency(), TimeUnit.SECONDS);

			// Adding in a Map for Rest API, as Via rest api we need to shutdown as well,
			// when required so need to keep the Schedular object for shutdown.
			MockUtils.SchedularMockObjectList.put(subscriberId, threadPool);

		}

	}
}
