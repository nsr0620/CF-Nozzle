package com.cfnozzle.proxy;

import static com.cfnozzle.utils.Constants.CPU_PERCENTAGE_SUFFIX;
import static com.cfnozzle.utils.Constants.DISK_BYTES_QUOTA_SUFFIX;
import static com.cfnozzle.utils.Constants.DISK_BYTES_SUFFIX;
import static com.cfnozzle.utils.Constants.MEMORY_BYTES_QUOTA_SUFFIX;
import static com.cfnozzle.utils.Constants.MEMORY_BYTES_SUFFIX;
import static com.cfnozzle.utils.Constants.TOTAL_SUFFIX;
import static com.cfnozzle.utils.MetricUtils.getSource;
import static com.cfnozzle.utils.MetricUtils.getTags;
import static com.cfnozzle.utils.MetricUtils.getTimestamp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.AppEnvelope;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.service.ClientConnectionHandler;
import com.cfnozzle.service.LogUtil;
import com.cfnozzle.service.MetricsReporter;
import com.cfnozzle.utils.ContainerMetricUtils;
import com.cfnozzle.utils.CounterEventUtils;
import com.cfnozzle.utils.MetricUtils;
import com.cfnozzle.utils.UDPConnection;
import com.cfnozzle.utils.ValueMetricUtils;
import com.codahale.metrics.Counter;

@Component
public class ProxyForwarderImpl implements ProxyForwarder {

	private static final Logger logger = Logger.getLogger(ProxyForwarderImpl.class.getCanonicalName());
	private final Counter numMetricsSent;
	private final Counter numValueMetricReceived;
	private final Counter numCounterEventReceived;
	private final Counter numContainerMetricReceived;
	private final Counter metricsSendFailure;

	@Autowired
	private ClientConnectionHandler clientConnectionHandler;

	private ProxyProperties proxyProperties;
	private LogStorageProperties logStorageProperties;
	private FirehoseProperties firehoseProperties;


	private long intialVal = System.currentTimeMillis();

	public ProxyForwarderImpl(MetricsReporter metricsReporter, ProxyProperties proxyProperties,
			LogStorageProperties logStorageProperties, FirehoseProperties firehoseProperties) throws IOException {

		logger.info(String.format("Forwarding PCF metrics to  proxy at %s:%s", proxyProperties.getProxyHostname(),
				proxyProperties.getProxyPort()));

		this.proxyProperties = proxyProperties;
		this.logStorageProperties = logStorageProperties;
		this.firehoseProperties = firehoseProperties;

		numMetricsSent = metricsReporter.registerCounter("total-metrics-sent");
		metricsSendFailure = metricsReporter.registerCounter("metrics-send-failure");
		numValueMetricReceived = metricsReporter.registerCounter("value-metric-received");
		numCounterEventReceived = metricsReporter.registerCounter("counter-event-received");
		numContainerMetricReceived = metricsReporter.registerCounter("container-metric-received");
	}

	@Override
	public void forward(AppEnvelope appEnvelope) {
		
		// Get the Envelope from the AppEnvelope
		Envelope envelope = appEnvelope.getEnvelope();
		// Determine the event type within the Envelope and perform actions accordingly
		switch (envelope.getEventType()) {
		case VALUE_METRIC:
			// Increment the counter for received value metrics
			numValueMetricReceived.inc();
			// Send the value metric data to the appropriate destination
			sendContainerMetrices(ValueMetricUtils.getMetricName(envelope), envelope.getValueMetric().value().longValue(),
					getTimestamp(envelope), getSource(envelope), getTags(appEnvelope), envelope);
			return;
		case COUNTER_EVENT:
			// Increment the counter for received counter events
			numCounterEventReceived.inc();
			// Send total counter data
			sendContainerMetrices(CounterEventUtils.getMetricName(envelope, TOTAL_SUFFIX), envelope.getCounterEvent().getTotal(),
					getTimestamp(envelope), getSource(envelope), getTags(appEnvelope), envelope);
			// Send delta counter data
//			send(CounterEventUtils.getMetricName(envelope,DELTA_SUFFIX),envelope.getCounterEvent().getDelta(),
//					getTimestamp(envelope),getSource(envelope), getTags(appEnvelope),envelope);
			return;
		case CONTAINER_METRIC:
			// Increment the counter for received container metrics
			numContainerMetricReceived.inc();

			// Send CPU percentage metric data
			sendContainerMetrices(ContainerMetricUtils.getMetricName(envelope, CPU_PERCENTAGE_SUFFIX),
					envelope.getContainerMetric().getCpuPercentage().longValue(), getTimestamp(envelope),
					getSource(envelope), getTags(appEnvelope), envelope);

			// Send disk bytes metric data
			sendContainerMetrices(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_SUFFIX),
					envelope.getContainerMetric().getDiskBytes(), getTimestamp(envelope), getSource(envelope),
					getTags(appEnvelope), envelope);

			// Send disk bytes quota metric data
			sendContainerMetrices(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_QUOTA_SUFFIX),
					envelope.getContainerMetric().getDiskBytesQuota(), getTimestamp(envelope), getSource(envelope),
					getTags(appEnvelope), envelope);

			// Send memory bytes metric data
			sendContainerMetrices(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_SUFFIX),
					envelope.getContainerMetric().getMemoryBytes(), getTimestamp(envelope), getSource(envelope),
					getTags(appEnvelope), envelope);

			// Send memory bytes quota metric data
			sendContainerMetrices(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_QUOTA_SUFFIX),
					envelope.getContainerMetric().getMemoryBytesQuota(), getTimestamp(envelope), getSource(envelope),
					getTags(appEnvelope), envelope);
			return;
		case LOG_MESSAGE:
			// Increment the counter for received log messages
			numContainerMetricReceived.inc();
			// Process and send log data to a specified destination
			LogUtil logUtil = LogUtil.getInstance(logStorageProperties);
			logUtil.sendDataToNfdb(appEnvelope);
			return;
		case ERROR:
		case HTTP_START_STOP:
			// No action needed for error or HTTP events
			return;
		}
	}
	
	
	@Override
	public void forwardAll(Collection<AppEnvelope> appEnvelopeList) {
		healthCheckValidation();
		
		int counter = 0; // Initialize a counter to keep track of the number of data items accumulated.
		StringBuilder sbDataSet = new StringBuilder(); // Initialize a StringBuilder to accumulate data.
		
		Iterator<AppEnvelope> iterator = appEnvelopeList.iterator();
		while (iterator.hasNext()) {
			AppEnvelope appEnvelope = iterator.next();

			StringBuilder statsdata=null;
			
			if(appEnvelope==null || appEnvelope.getEnvelope()==null)
				continue;
			
			if(appEnvelope.getEnvelope().getEventType() == EventType.COUNTER_EVENT)
			 statsdata = MetricUtils.getStatsFromTags(getTags(appEnvelope), CounterEventUtils.getMetricName(appEnvelope.getEnvelope(), TOTAL_SUFFIX), appEnvelope.getEnvelope().getCounterEvent().getTotal(), getTimestamp(appEnvelope.getEnvelope()),appEnvelope.getEnvelope().getEventType().toString());
			else if (appEnvelope.getEnvelope().getEventType() == EventType.VALUE_METRIC)
			 statsdata = MetricUtils.getStatsFromTags(getTags(appEnvelope), ValueMetricUtils.getMetricName(appEnvelope.getEnvelope()), appEnvelope.getEnvelope().getValueMetric().value().longValue(), getTimestamp(appEnvelope.getEnvelope()),appEnvelope.getEnvelope().getEventType().toString());
			else
				continue;
			
			if (counter >= proxyProperties.getPacketbatchSize()) {
				if (sendMessage(sbDataSet, proxyProperties)) {
					sbDataSet = null;
					sbDataSet = new StringBuilder();
					sbDataSet.append(statsdata);
					counter = 1;
				} else {
					logger.log(Level.WARNING, "Not able to send the data....");
				}
			} else {
				sbDataSet.append(statsdata); // Append the current data to the accumulated data.
				counter++;
			}
		}
		
		if (sbDataSet.length() > 0) {
			// Send any remaining accumulated data.
			if (sendMessage(sbDataSet, proxyProperties)) {
				sbDataSet = null;
				sbDataSet = new StringBuilder();
				counter = 0;
			} else {
				logger.log(Level.WARNING, "Not able to send the data....");
			}
		}
			
			
		
	}

	// Send metric data to the appropriate destination
	private void sendContainerMetrices(String metricName, long metricValue, Long timestamp, String source, Map<String, String> tags,
			Envelope envelope) {

		healthCheckValidation();
		convertTimetoMillISec(timestamp);
		StringBuilder statsdata = MetricUtils.getStatsFromTags(tags, metricName, metricValue, timestamp,envelope.getEventType().toString());
		sendMessage(statsdata, proxyProperties);
		
	}
	
	private void convertTimetoMillISec(Long timestamp)
	{
		try {
			// Convert timestamp to milliseconds if it is in nanoseconds or microseconds
			int len = timestamp.toString().length();
			if (len == 19) { // nanoseconds -> convert to milliseconds
				timestamp /= 1000000;
			} else if (len == 16) {// microseconds -> convert to milliseconds
				timestamp /= 1000;
			}
			
		} catch (IllegalArgumentException e) {
			// Log a warning if data cannot be sent to the cfnozzle proxy
			logger.log(Level.WARNING, "Can't send data to cfnozzle proxy!", e);
			// Increment the counter for failed metric sends
			metricsSendFailure.inc();
		}
	}
	
	
	
	private void healthCheckValidation()
	{
		try {

			if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isHealthCheck()) {

				for (Map.Entry<String, Long> entry : MetricUtils.healthCheckMap.entrySet()) {
					if (MetricUtils.istokenExpied(firehoseProperties, entry.getValue())) {
						logger.info("Health Check Failed , Shutting down the Mock threads.");
						clientConnectionHandler.healthCheckUpdation(entry.getKey());
					}
				}

				if (MetricUtils.healthCheckMap.size() == 0) {
					MetricUtils.availableFilterList.clear();

				}

			}

		} catch (Exception e) {
			logger.severe("Error occured while sending to UDP, reason; " + e.getMessage());
		}
	}

	public boolean sendMessage(StringBuilder statsdata,  ProxyProperties proxyProperties) {

		if (statsdata != null ) {
				String targetIp = proxyProperties.getProxyHostname();
				int targetPort = proxyProperties.getProxyPort();

				if (MetricUtils.dynamicLogging())
					logger.info("Ready to send over " + targetIp + ",port=" + targetPort);
	
				InetAddress targetAddress = UDPConnection.getInetAddress(targetIp);

				if (targetAddress != null) {

					// Iterate over the batch of metric data and send each data point individually
					
						byte[] sendData = null;
						sendData = statsdata.toString().getBytes();
						DatagramPacket packet = new DatagramPacket(sendData, sendData.length, targetAddress,
								targetPort);
						DatagramSocket udp = UDPConnection.getUDPConnection();

						if (udp != null) {
							try {

								// Send the metric data over UDP
								udp.send(packet);

								// Log the metric data if logging is enabled
								if (proxyProperties.isEnableLogs()) {
									logger.info("********************** SAMPLES**********************");
									logger.info("old=" + statsdata.toString());
									logger.log(Level.INFO, "new=" + statsdata.toString());

								}

								return true;

							} catch (Exception e) {
								logger.warning("Error occured while sending data over UDP: " + e.getMessage());
								return false;
							}
						} else
							logger.info("UDP is NUll , So skipping to send the data over the UDP");
						return false;

				} else {
					logger.info("targetAddress is null");
					return false;

				}

		}
		return false;

	}

	

}
