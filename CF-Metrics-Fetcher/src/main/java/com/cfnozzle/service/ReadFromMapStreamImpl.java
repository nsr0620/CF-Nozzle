package com.cfnozzle.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.proxy.ProxyForwarder;

@Component
public class ReadFromMapStreamImpl implements ReadFromMapStream{

	private static final Logger logger = Logger.getLogger(
			ReadFromMapStreamImpl.class.getCanonicalName());
	
	@Autowired
	 FirehoseProperties firehoseProperties;
	@Autowired
	ProxyForwarder proxyForwarder;
	
	@Override
	public void readFromQueue(Event event) {
		logger.log(Level.INFO, "Ready to fetch the data from service");
		
		String subscriberId=null;
		
	    // Determine the subscriber ID based on whether REST API is enabled
		if(firehoseProperties.isEnbaleRestAPI())
			subscriberId=event.getSubscriberId();
		else
			subscriberId=firehoseProperties.getSubscriptionId();
		
		if(subscriberId != null)
		{
	        // Create a Runnable to read from the map stream with the given subscriber ID
			ReadFromMapStreamRunnable r=new ReadFromMapStreamRunnable(subscriberId, proxyForwarder);
			ScheduledExecutorService  threadPool = Executors.newScheduledThreadPool(1);
			
	        // Schedule the execution of the Runnable with a fixed rate and queue frequency
			threadPool.scheduleAtFixedRate(r, 5,firehoseProperties.getQueueFrequency(), TimeUnit.SECONDS);
			
	        // Store the ScheduledExecutorService in a map for later management
			ClientConnectionHandlerImpl.SchedularObjectList.put(subscriberId, threadPool) ;
		}
			
	}
}
