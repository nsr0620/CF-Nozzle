package com.cfnozzle.service;

import static com.cfnozzle.utils.Constants.APP_NAME_CONSTANT;
import static com.cfnozzle.utils.Constants.CPU_PERCENTAGE_SUFFIX;
import static com.cfnozzle.utils.Constants.FIREHOSE_NOZZLE;
import static com.cfnozzle.utils.Constants.ORGS_NAME_CONSTANT;
import static com.cfnozzle.utils.Constants.SPACE_NAME_CONSTANT;
import static com.cfnozzle.utils.Constants.TOTAL_SUFFIX;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.AppEnvelope;
import com.cfnozzle.model.Event;
import com.cfnozzle.props.AppInfoProperties;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.proxy.ProxyForwarder;
import com.cfnozzle.utils.ContainerMetricUtils;
import com.cfnozzle.utils.CounterEventUtils;
import com.cfnozzle.utils.MetricUtils;
import com.cfnozzle.utils.ValueMetricUtils;
import com.codahale.metrics.Counter;

import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class FirehoseToProxyConnectorImpl implements FirehoseToProxyConnector {

	private static final Logger logger = Logger.getLogger(FirehoseToProxyConnectorImpl.class.getCanonicalName());

	private final DopplerClient dopplerClient;
	private final FirehoseProperties firehoseProperties;
	private final AppInfoProperties appInfoProperties;
	private final ProxyForwarder proxyForwarder;
	private final AppInfoFetcher appInfoFetcher;
	private final Counter numProcessedEvents;
	private final Counter numUnprocessedEvents;
	private final Counter numFetchAppInfoEnvelope;
	private final Counter numEmptyAppInfoEnvelope;

	@Autowired
	PcfProperties pcfProperties;
	private final Counter numSubscribeAppInfoEnvelope;

	@Autowired
	ClientConnectionHandler clientConnectionHandler;

	public FirehoseToProxyConnectorImpl(MetricsReporter metricsReporter, DopplerClient dopplerClient,
			FirehoseProperties firehoseProperties, AppInfoProperties appInfoProperties, ProxyForwarder proxyForwarder,
			AppInfoFetcher appInfoFetcher) {

		this.dopplerClient = dopplerClient;
		this.firehoseProperties = firehoseProperties;
		this.appInfoProperties = appInfoProperties;
		this.proxyForwarder = proxyForwarder;
		this.appInfoFetcher = appInfoFetcher;
		numProcessedEvents = metricsReporter.registerCounter("processed-events");
		numUnprocessedEvents = metricsReporter.registerCounter("unprocessed-events");
		numFetchAppInfoEnvelope = metricsReporter.registerCounter("fetch-app-info-envelope");
		numEmptyAppInfoEnvelope = metricsReporter.registerCounter("empty-app-info-envelope");
		numSubscribeAppInfoEnvelope = metricsReporter.registerCounter("subscribe-app-info-envelope");
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
	public synchronized void connect(Event event) {

		String subscriptionId = null;

		// Check if the REST API is enabled
		if (!firehoseProperties.isEnbaleRestAPI()) {
			// Generate and set a subscription ID if not already defined
			if (firehoseProperties.getSubscriptionId() == null || firehoseProperties.getSubscriptionId().isEmpty())
				firehoseProperties.setSubscriptionId(getSubscriptionID());

			// Set the subscriptionId to the generated or predefined value
			subscriptionId = firehoseProperties.getSubscriptionId();

		} else {
			// Set the subscriptionId to the subscriber's ID from the incoming event
			subscriptionId = event.getSubscriberId();
		}
		// Check if a subscriptionId is provided
		if (subscriptionId == null) {
			logger.log(Level.INFO, "SubscriberID is mandatory.");
		} else {
			// Check if the subscriber is already running with the given subscriptionId
			if (firehoseProperties.isEnbaleRestAPI() && clientConnectionHandler.isSubscriberisOn(subscriptionId)) {
				logger.log(Level.INFO, "Subscriber is already running with " + subscriptionId
						+ " ,So skipping to subscribe Nozzle again.");

			} else {
				try {

					// Log information about connecting to the Firehose with the subscriptionId and
					// event types
					logger.log(Level.INFO,
							"Connecting to firehose using subscription id:" + subscriptionId + ", for event types: "
									+ firehoseProperties.getEventTypes().toString() + ",in parallel "
									+ firehoseProperties.getParallelism());
					// Create a FirehoseRequest for the specified subscriptionId
					FirehoseRequest request = FirehoseRequest.builder().subscriptionId(subscriptionId).build();

					// Subscribe to the Firehose stream, apply filters and processing, and forward data
					Disposable nozzleSubscriberOj = dopplerClient.firehose(request)
							.subscribeOn(Schedulers.newParallel(FIREHOSE_NOZZLE, firehoseProperties.getParallelism()))
							.filter(envelope -> {

								// Filter and process events based on their type
								boolean ret = filterEventType(envelope.getEventType());
								if (ret) {
									numProcessedEvents.inc();
								} else {
									numUnprocessedEvents.inc();
								}
								return ret;
							}).filter(envelope -> {
								// Filter and process events based on metric filters
								if (firehoseProperties.isMetricFilterEnable()) {
									if (firehoseProperties.isEnbaleRestAPI()) {
										
										return filterByMetric(envelope,
												MetricUtils.availableFilterList.get(event.getSubscriberId()),
												firehoseProperties);
									} else
										return filterByMetric(envelope, MetricUtils.metricConfigurationList,
												firehoseProperties);
								}
								return true;
							}).filter(envelope -> {
								// Filter events based on application, spaces, and orgs (for CONTAINER_METRIC
								// events)
								if (envelope.getEventType() == EventType.CONTAINER_METRIC) {
									return filterByAppsSpacesOrgs(envelope, event);

								} else
									return true;
							}).flatMap(envelope -> {
								// Fetch application information and create an AppEnvelope
								if (appInfoProperties.isFetchAppInfo()
										&& envelope.getEventType() == EventType.CONTAINER_METRIC) {
									numFetchAppInfoEnvelope.inc();
									return appInfoFetcher.fetch(envelope.getContainerMetric().getApplicationId())
											.map(optionalAppInfo -> new AppEnvelope(envelope, optionalAppInfo));
								} else {
									numEmptyAppInfoEnvelope.inc();
									return Mono.just(new AppEnvelope(envelope, Optional.empty()));
								}
							}).subscribe(appEnvelope -> {
								// Subscribe to processed AppEnvelope data and forward it to the appropriate
								// destination
								numSubscribeAppInfoEnvelope.inc();

								// Check if the metric data should be pushed to a queue
								if (firehoseProperties.isEnableQueue()) {
									if (appEnvelope.getEnvelope().getEventType() == EventType.COUNTER_EVENT
											|| appEnvelope.getEnvelope().getEventType() == EventType.VALUE_METRIC) {

										if (firehoseProperties.isEnbaleRestAPI())
											MetricUtils.pushToQueue(appEnvelope, event.getSubscriberId());
										else
											MetricUtils.pushToQueue(appEnvelope,
													firehoseProperties.getSubscriptionId());

									} else
										// Forward non-metric events to the proxy forwarder
										proxyForwarder.forward(appEnvelope);
								} else
									// Forward events directly to the proxy forwarder
									proxyForwarder.forward(appEnvelope);
							});

					if (firehoseProperties.isEnbaleRestAPI())
						// Store the Disposable object for the subscriber with the subscriptionId
						ClientConnectionHandlerImpl.disposableObjectList.put(subscriptionId, nozzleSubscriberOj);

				} catch (IllegalStateException e) {
					if (e.getMessage().startsWith("Required field not set:")) {
						connect(event);
					} else {
						logger.log(Level.SEVERE, "cfnozzle firehose nozzle failed.", e);
						throw e;
					}
				} catch (Throwable t) {
					logger.log(Level.SEVERE, "cfnozzle firehose nozzle failed.", t);
					throw t;
				}

			}

		}

	}

	private boolean filterByMetric(@Nullable Envelope envelope, Set<String> metricList,
			FirehoseProperties firehoseProperties) {

		if (metricList == null || metricList.isEmpty() || envelope == null)
			return false;

		// Determine the event type within the envelope and apply metric filtering
		// accordingly
		if (envelope.getEventType() == EventType.CONTAINER_METRIC) {
			if (metricList.contains(MetricUtils.generateKeyForMetricFilter(
					ContainerMetricUtils.getMetricName(envelope, CPU_PERCENTAGE_SUFFIX), null,
					firehoseProperties.isEnbaleRestAPI())))
				return true;

		} else if (envelope.getEventType() == EventType.VALUE_METRIC) {
			// Check if the metricList contains the specific value metric for the origin (if
			// provided)
			if (metricList.contains(MetricUtils.generateKeyForMetricFilter(ValueMetricUtils.getMetricName(envelope),
					envelope.getOrigin(), firehoseProperties.isEnbaleRestAPI())))
				return true;

		} else if (envelope.getEventType() == EventType.COUNTER_EVENT) {
			
			if (metricList.contains(
					MetricUtils.generateKeyForMetricFilter(CounterEventUtils.getMetricName(envelope, TOTAL_SUFFIX),
							envelope.getOrigin(), firehoseProperties.isEnbaleRestAPI()))) {
				return true;
				}
		} else {
			// No specific filtering for other event types
			return true;
			}

		// If no match is found, return false to indicate that the envelope should be
		// filtered out
		return false;

	}

	private boolean filterEventType(@Nullable EventType eventType) {
		if (firehoseProperties.getEventTypes() == null || firehoseProperties.getEventTypes().isEmpty()) {
			return false;
		}
		return firehoseProperties.getEventTypes().contains(eventType);
	}

	private boolean filterByAppsSpacesOrgs(@Nullable Envelope envelope, Event event) {

		boolean proceding = true;

		List<String> orgsList = null;
		List<String> spaceList = null;
		List<String> appList = null;

		try {
			// Determine whether to use the event's orgs, spaces, and apps lists or the
			// configuration properties
			if (firehoseProperties.isEnbaleRestAPI()) {
				orgsList = event.getOrgsList();
				spaceList = event.getSpaceList();
				appList = event.getAppList();
			} else {
				orgsList = firehoseProperties.getOrganizations();
				spaceList = firehoseProperties.getSpaces();
				appList = firehoseProperties.getApps();
			}

			// filter for organization
			if (orgsList != null && !orgsList.isEmpty()) {
				if (envelope != null && envelope.getTags() != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !orgsList.contains(envelope.getTags().get(ORGS_NAME_CONSTANT));
					else
						proceding = orgsList.contains(envelope.getTags().get(ORGS_NAME_CONSTANT));

					if (!proceding)
						return false;
				} else {
					logger.warning("Organization Tags are empty for  " + envelope);
					return false;
				}
			}

			// filter for spaces
			if (spaceList != null && !spaceList.isEmpty()) {
				if (envelope != null && envelope.getTags() != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !spaceList.contains(envelope.getTags().get(SPACE_NAME_CONSTANT));
					else
						proceding = spaceList.contains(envelope.getTags().get(SPACE_NAME_CONSTANT));

					if (!proceding)
						return false;
				} else {
					logger.warning("Space Tags are empty for  " + envelope);
					return false;
				}
			}

			// filter for Apps
			if (appList != null && !appList.isEmpty()) {
				if (envelope != null && envelope.getTags() != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !appList.contains(envelope.getTags().get(APP_NAME_CONSTANT));
					else
						proceding = appList.contains(envelope.getTags().get(APP_NAME_CONSTANT));

					if (!proceding)
						return false;
				} else {
					logger.warning("APP Tags are empty for  " + envelope);
					return false;
				}
			}

		} catch (Exception e) {
			proceding = false;
			logger.warning(
					"Exception occured while filtering the Stats via Orgs/Spaces/App, Reason: " + e.getMessage());
		}
		return proceding;
	}

}
