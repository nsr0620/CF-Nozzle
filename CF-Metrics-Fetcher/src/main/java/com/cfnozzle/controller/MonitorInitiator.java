package com.cfnozzle.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cfnozzle.mock.MockConnector;
import com.cfnozzle.mock.MockUtils;
import com.cfnozzle.mock.ReadFromMapMockStream;
import com.cfnozzle.model.AppEnvelope;
import com.cfnozzle.model.Datum;
import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.service.ClientConnectionHandler;
import com.cfnozzle.service.ClientConnectionHandlerImpl;
import com.cfnozzle.service.FirehoseToProxyConnector;
import com.cfnozzle.service.ReadFromMapStream;
import com.cfnozzle.utils.ErrorMessages;
import com.cfnozzle.utils.MetricUtils;

@RestController
public class MonitorInitiator {

	// Initialize a logger for this class
	Logger log = LoggerFactory.getLogger(MonitorInitiator.class);

	// Autowire properties and services for dependency injection
	@Autowired
	FirehoseProperties firehoseProperties;

	@Autowired
	ClientConnectionHandler clientConnectionHandler;

	@Autowired
	PcfProperties pcf;
	
	@Autowired
	ProxyProperties proxyProperties;

	@Autowired
	private MockConnector firehoseToMockConnector;

	@Autowired
	private ReadFromMapMockStream readFromMapMockStream;

	@Autowired
	private ReadFromMapStream readFromMapStream;

	@Autowired
	private FirehoseToProxyConnector firehoseTocfnozzleProxyConnector;

	// Handle POST request to start a monitoring nozzle
	@PostMapping("/nozzle/start")
	public String nozzleStart(@RequestBody Event event) {
		log.info("Received request of Nozzle start for subscriber - " + event.getSubscriberId());
		if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isHealthCheck()) {

			// Check if the Subscriber ID is provided and not empty
			if (event.getSubscriberId() == null || event.getSubscriberId().trim().equals("")) {
				log.info(ErrorMessages.SUBSCRIBER_ID_ERROR_MSG);
				return ErrorMessages.SUBSCRIBER_ID_ERROR_MSG;
			}

			boolean alreadySubscribed = true;
			if (pcf.isMocking())
				alreadySubscribed = firehoseToMockConnector.isSubscriberisOnMock(event.getSubscriberId());
			else
				alreadySubscribed = clientConnectionHandler.isSubscriberisOn(event.getSubscriberId());

			if (alreadySubscribed) {
				log.info("SubscriberId is already enabled for " + event.getSubscriberId()
						+ " , Please provide different SubscriberId to continue.");
				return "SubscriberId is already enabled for " + event.getSubscriberId()
						+ " , Please provide different SubscriberId to continue.";
			}

			// Store the current time for health checking
			MetricUtils.healthCheckMap.put(event.getSubscriberId(), System.currentTimeMillis());

			List<String> apps = null;
			List<String> spaces = null;
			List<String> orgs = null;

			if (event.getApps() != null && !event.getApps().trim().equals("")) {
				apps = Arrays.asList(event.getApps().split("\\s*,\\s*"));
				if (apps != null && !apps.isEmpty())
					event.setAppList(apps);
			}

			if (event.getSpaces() != null && !event.getSpaces().trim().equals("")) {
				spaces = Arrays.asList(event.getSpaces().split("\\s*,\\s*"));
				if (spaces != null && !spaces.isEmpty())
					event.setSpaceList(spaces);
			}

			if (event.getOrgs() != null && !event.getOrgs().trim().equals("")) {
				orgs = Arrays.asList(event.getOrgs().split("\\s*,\\s*"));
				if (orgs != null && !orgs.isEmpty())
					event.setOrgsList(orgs);
			}

			if (pcf.isMocking()) {
				// Used for simulation - which will get the metrices from stream
				firehoseToMockConnector.mock(event);

				// Use to get the metrices data from the queue(for value and Counter events
				// only)
				if (firehoseProperties.isEnableQueue())
					readFromMapMockStream.readfromMockQueue(event);
			} else {
				firehoseTocfnozzleProxyConnector.connect(event);
				if (firehoseProperties.isEnableQueue())
					readFromMapStream.readFromQueue(event);
			}

			log.info("Nozzle triggered Successfully for subscriberId - " + event.getSubscriberId());
			return "Nozzle triggered Successfully for subscriberId - " + event.getSubscriberId();

		} else {
			log.info(ErrorMessages.REST_API_DISABLED_MSG);
			return ErrorMessages.REST_API_DISABLED_MSG;
		}

	}

	// Handle POST request to stop a monitoring nozzle
	@PostMapping("/nozzle/stop")
	public String nozzleStop(@RequestBody Event event) {
		log.info("Received request of Nozzle stop for subscriber - " + event.getSubscriberId());
		String returnMsg = null;
		if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isHealthCheck()) {
			// Check if the Subscriber ID is provided and not empty
			if (event.getSubscriberId() == null || event.getSubscriberId().trim().equals("")) {
				log.info(ErrorMessages.SUBSCRIBER_ID_ERROR_MSG);
				return ErrorMessages.SUBSCRIBER_ID_ERROR_MSG;
			}

			if (pcf.isMocking()) {
				returnMsg = firehoseToMockConnector.disconnectMockClients(event.getSubscriberId());
			} else {
				returnMsg = clientConnectionHandler.disconnectClients(event.getSubscriberId());
			}

		} else {
			returnMsg = ErrorMessages.REST_API_DISABLED_MSG;
		}

		log.info(returnMsg);
		return returnMsg;
	}

	// Handle GET request to get the size of the monitoring nozzle
	@GetMapping("/nozzle/size")
	public int nozzleSize(@RequestParam(name = "subscriberId", required = false) String subscriptionID) {

		log.info("Received request of Nozzle size check");
		if (firehoseProperties.isEnbaleRestAPI() && firehoseProperties.isHealthCheck()) {
			if (pcf.isMocking()) {
				if (subscriptionID != null && !subscriptionID.isEmpty()) {
					// KEY_LIST

					log.info("Metric Queue for SubscriptionID "+subscriptionID+" = " + MetricUtils.metricQueue.get(subscriptionID));
				} else {
					// KEY_LIST
					log.info("Metric Queue = " + MetricUtils.metricQueue);

				}
				log.info("Mocking filterlist details - " + MetricUtils.availableFilterList);
				log.info("Mock disposable client keys - " + MockUtils.disposableMockObjectList.keySet());
				log.info("Mocking Queue details - " + MockUtils.SchedularMockObjectList.keySet());
				log.info("Health - " + MetricUtils.healthCheckMap);

				return MockUtils.disposableMockObjectList.size();
			} else {
				if (subscriptionID != null && !subscriptionID.isEmpty()) {

					log.info("Metric Queue for SubscriptionID "+subscriptionID+" = " + MetricUtils.metricQueue.get(subscriptionID));

				} else {
					log.info("Metric Queue = " + MetricUtils.metricQueue);
				}
				log.info("filterlist details -> " + MetricUtils.availableFilterList);
				log.info("Disposable DisposableClient Key List is -> "
						+ ClientConnectionHandlerImpl.disposableObjectList.keySet());
				log.info("Queue DisposableClient Key List is -> "
						+ ClientConnectionHandlerImpl.SchedularObjectList.keySet());
				log.info("Health -> " + MetricUtils.healthCheckMap);

				return ClientConnectionHandlerImpl.disposableObjectList.size();
			}
		} else {
			log.info(ErrorMessages.REST_API_DISABLED_MSG);
			return -1;
		}

	}

	// Handle POST request to enable metric filters for a Subscriber
	@PostMapping("/nozzle/enableMetric")
	public String setDataInMap(@RequestBody Event event) {
		
		log.info("Received request of enable metric filter for subscriber - " + event.getSubscriberId());
		if (firehoseProperties.isMetricFilterEnable() && firehoseProperties.isEnbaleRestAPI()) {
			// Check if the Subscriber ID is provided and not empty
			if (event.getSubscriberId() == null || event.getSubscriberId().trim().equals("")) {
				log.info(ErrorMessages.SUBSCRIBER_ID_ERROR_MSG);
				return ErrorMessages.SUBSCRIBER_ID_ERROR_MSG;
			}
			if (event.getData() != null && !event.getData().isEmpty()) {

				Set<String> metricSet = MetricUtils.availableFilterList.get(event.getSubscriberId());

				if (metricSet == null) {

					metricSet = new HashSet<>();

					for (Datum obj : event.getData()) {
						if (obj.getPattern() != null && !obj.getPattern().trim().isEmpty()) {
							String origin = MetricUtils.getOriginName(obj.getPattern());
							metricSet.add(MetricUtils.generateKeyForMetricFilter(obj.getMetricName(), origin, true));
						} else {
							metricSet.add(MetricUtils.generateKeyForMetricFilter(obj.getMetricName(), null, true));
						}
					}

					MetricUtils.availableFilterList.put(event.getSubscriberId(), metricSet);

				} else {
					for (Datum obj : event.getData()) {
						if (obj.getPattern() != null && !obj.getPattern().trim().isEmpty()) {
							String origin = MetricUtils.getOriginName(obj.getPattern());
							metricSet.add(MetricUtils.generateKeyForMetricFilter(obj.getMetricName(), origin, true));
						}
					}
				}

			} else {
				log.info(ErrorMessages.METRIC_FILTER_MANDATORY);
				return ErrorMessages.METRIC_FILTER_MANDATORY;
			}

		} else {
			log.info(ErrorMessages.REST_API_DISABLED_MSG);
			return ErrorMessages.REST_API_DISABLED_MSG;
		}
		log.info("Filters enabled successfully for subscriberId -" + event.getSubscriberId());
		return "Filters enabled successfully for subscriberId -" + event.getSubscriberId();
	}

	// Handle POST request to perform a health check
	@PostMapping("/nozzle/healthcheck")
	public byte healthCheck(@RequestBody Event event) {
		if (firehoseProperties.isHealthCheck() && firehoseProperties.isEnbaleRestAPI()) {
			// Check if the Subscriber ID is provided and not empty
			if (event.getSubscriberId() == null || event.getSubscriberId().trim().equals("")) {
				log.info(ErrorMessages.SUBSCRIBER_ID_ERROR_MSG);
				return 2;
			}
			if (MetricUtils.healthCheckMap.containsKey(event.getSubscriberId())) {
				MetricUtils.healthCheckMap.put(event.getSubscriberId(), System.currentTimeMillis());
				log.info("Healthcheck triggered successfully for subscriberId - " + event.getSubscriberId());
				return 1;
			} else {
				log.info("Subscriber Id not found for subscriber - " + event.getSubscriberId());
				return 0;
			}
		}
		log.info(ErrorMessages.REST_API_DISABLED_MSG);
		return -1;
	}

	// Handle POST request to disable metric filters for a Subscriber
	@PostMapping("/nozzle/disableMetric")
	public String clearDataInMap(@RequestBody Event event) {
		log.info("Received request of disable metric filter for subscriber - " + event.getSubscriberId());
		if (firehoseProperties.isHealthCheck() && firehoseProperties.isEnbaleRestAPI()) {
			// Check if the Subscriber ID is provided and not empty
			if (event.getSubscriberId() == null || event.getSubscriberId().trim().equals("")) {
				log.info(ErrorMessages.SUBSCRIBER_ID_ERROR_MSG);
				return ErrorMessages.SUBSCRIBER_ID_ERROR_MSG;
			}
			Set<String> metricSet = MetricUtils.availableFilterList.get(event.getSubscriberId());

			if (metricSet != null) {

				if (event.getData() != null && !event.getData().isEmpty()) {

					for (Datum obj : event.getData()) {
						String origin = "";
						if (obj.getPattern() != null && !obj.getPattern().trim().isEmpty()) {
							origin = MetricUtils.getOriginName(obj.getPattern());
							metricSet.remove(MetricUtils.generateKeyForMetricFilter(obj.getMetricName(), origin, true));
						} else {
							metricSet.remove(MetricUtils.generateKeyForMetricFilter(obj.getMetricName(), null, true));
						}
						if (firehoseProperties.isEnableQueue() && origin != null && !origin.trim().isEmpty()) {

							if (pcf.isMocking()) {

								Map<String, String> map = MockUtils.mock_metrics_queue.get(event.getSubscriberId());

								if (map != null)
									map.remove(MetricUtils.generateKeyForQueue(obj.getMetricName(), origin));

								/*
								 * MockUtils.mock_metrics_queue.get(event.getSubscriberId())
								 * .remove(MetricUtils.generateKeyForQueue(obj.getMetricName(), origin));
								 */
							} else {

								Map<String, AppEnvelope> map = MetricUtils.metricQueue.get(event.getSubscriberId());

								if (map != null)
									map.remove(MetricUtils.generateKeyForQueue(obj.getMetricName(), origin));

								/*
								 * MetricUtils.metricQueue.get(event.getSubscriberId())
								 * .remove(MetricUtils.generateKeyForQueue(obj.getMetricName(), origin));
								 */
							}
						}
					}
				} else {
					log.info(ErrorMessages.METRIC_FILTER_CLEAR_MANDATORY);
					return ErrorMessages.METRIC_FILTER_CLEAR_MANDATORY;
				}

			} else {
				log.info("clearDataInMap triggered , No Subscriber ID found for " + event.getSubscriberId());
			}

		} else {
			log.info(ErrorMessages.REST_API_DISABLED_MSG);
			return ErrorMessages.REST_API_DISABLED_MSG;
		}
		log.info("Filters disabled successfully for subscriberId - " + event.getSubscriberId());
		return "Filters disabled successfully for subscriberId - " + event.getSubscriberId();
	}

}
