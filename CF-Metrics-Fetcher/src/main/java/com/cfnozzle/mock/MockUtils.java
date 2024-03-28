package com.cfnozzle.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.core.io.ClassPathResource;

import com.cfnozzle.model.Event;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.service.LogUtil;
import com.cfnozzle.utils.MetricUtils;
import com.cfnozzle.utils.UDPConnection;

public class MockUtils {

	private MockUtils() {

	}

	private static final Logger logger = Logger.getLogger(MockUtils.class.getCanonicalName());

	// public static Map<String, String> mock_metrics_queue = new
	// ConcurrentHashMap<>();

	public static Map<String, Map<String, String>> mock_metrics_queue = new ConcurrentHashMap<>();

	public static Map<String, ScheduledExecutorService> disposableMockObjectList = new ConcurrentHashMap<>();

	public static Map<String, ScheduledExecutorService> SchedularMockObjectList = new ConcurrentHashMap<>();

	// pushtoMockQueue method pushes mock data to the mock_metrics_queue
	public static void pushtoMockQueue(String origion, String metricName, String data, String subscriberID) {
		// Retrieve the map of mock data for the given subscriberID from the
		// mock_metrics_queue.
		Map<String, String> mockmetricMap = mock_metrics_queue.get(subscriberID);

		// Check if the map doesn't exist for the subscriberID.
		if (mockmetricMap == null) {

			// Create a new map to store mock data.
			mockmetricMap = new HashMap<>();

			// Generate a unique metric string based on metricName and origin.
			mockmetricMap.put(MetricUtils.generateKeyForQueue(metricName, origion), data);

			// Put the map into the mock_metrics_queue with the subscriberID as the key.
			mock_metrics_queue.put(subscriberID, mockmetricMap);
		} else {
			// If the map already exists for the subscriberID, add the new data to the
			// existing map.

			// Generate a unique metric string based on metricName and origin.
			// Put the mock data into the map with the unique metric string as the key.
			mockmetricMap.put(MetricUtils.generateKeyForQueue(metricName, origion), data);

		}

	}

	// getEnvelopFromMockMetricQueue retrieves mock data from the mock_metrics_queue
	public static Collection<String> getEnvelopFromMockMetricQeuue(String subscriberId) {

		if (mock_metrics_queue.get(subscriberId) != null)
			return mock_metrics_queue.get(subscriberId).values();
		else
			return Collections.emptyList();

	}

	// filterByAppsSpacesOrgsMock filters mock data based on organizations, spaces,
	// and apps
	public static boolean filterByAppsSpacesOrgsMock(FirehoseProperties firehoseProperties, String organizationName,
			String spaceName, String appname, Event event) {

		boolean proceding = true;

		List<String> orgsList = null;
		List<String> spaceList = null;
		List<String> appList = null;

		try {

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
				if (organizationName != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !orgsList.contains(organizationName);
					else
						proceding = orgsList.contains(organizationName);

					if (!proceding)
						return false;
				} else {
					logger.warning("Organization name is empty for  " + organizationName);
					return false;
				}
			}

			// filter for spaces
			if (spaceList != null && !spaceList.isEmpty()) {
				if (spaceName != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !spaceList.contains(spaceName);
					else
						proceding = spaceList.contains(spaceName);

					if (!proceding)
						return false;
				} else {
					logger.warning("Space Names are empty for  " + spaceName);
					return false;
				}
			}

			// filter for Apps
			if (appList != null && !appList.isEmpty()) {
				if (appname != null) {
					if (firehoseProperties.isFilterExclude())
						proceding = !appList.contains(appname);
					else
						proceding = appList.contains(appname);

					if (!proceding)
						return false;
				} else {
					logger.warning("APP Names are empty for  " + appname);
					return false;
				}
			}

		} catch (Exception e) {
			proceding = false;
			logger.warning(
					"Exception occured while filtering the mock Stats via Orgs/Spaces/App, Reason: " + e.getMessage());
		}
		return proceding;

	}

	// pushNFLogs method pushes logs to NF
	public static void pushNFLogs(String fileName, ProxyProperties proxyProperties,
			LogStorageProperties logStorageProperties) {
		BufferedReader br = null;

		try {
			// Read the mock data from the specified file using ClassPathResource.
			ClassPathResource mockResource = new ClassPathResource(fileName);
			// Log information if dynamic logging is enabled.
			if (MetricUtils.dynamicLogging())
				logger.info("Sending mock data to NF ");

			br = new BufferedReader(new InputStreamReader(mockResource.getInputStream()));
			String st;
			while ((st = br.readLine()) != null) {

				if (st != null && !st.trim().equalsIgnoreCase("")) {
					// Split the line into components based on the specified delimiter.
					String[] nfData = st.split(MockConstant.LOG_MESSAGE_SPLITTER);

					if (nfData != null && nfData.length == 4) {
						LogUtil logUtil = LogUtil.getInstance(logStorageProperties);
						// Extract relevant data and call the method to send data to NF.
						logUtil.SendingMockDataToNF(nfData[0], nfData[1], nfData[2], nfData[3],
								System.currentTimeMillis() * 1000000, proxyProperties);
					}
				}

			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception occured during pushNFLogs, reason:  " + e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE,
							"Exception occured during pushNFLogs while doing br close, reason:  " + e.getMessage());
				}
			}
			br = null;
		}

	}

	// metricFilter checks if a metric should be filtered
	private static boolean metricFilter(String origin, String metricName, FirehoseProperties firehoseProperties,
			String subscriberID) {

		if (firehoseProperties.isEnbaleRestAPI()) {
			// Check if the metric exists in the available filter list based on
			// subscriberID.
			Set<String> metricSet = MetricUtils.availableFilterList.get(subscriberID);

			if (metricSet != null) {
				// Check if the metric (generated metric string) is in the filter list.
				if (metricSet.contains(MetricUtils.generateKeyForMetricFilter(metricName, origin,
						firehoseProperties.isEnbaleRestAPI()))) {
					return true;

				}
			}

		} else {
			// Check if the metric name exists in the metric configuration list.
			if (MetricUtils.metricConfigurationList.contains(metricName)) {
				return true;

			}
		}

		return false;

	}

	// getMockStats retrieves mock statistics
	public static void getMockStats(String fileName, List<String> data, PcfProperties pcfProperties,
			FirehoseProperties firehoseProperties, long timestamp, Event event) {
		BufferedReader br = null;

		try {
			// Load the mock data file from the classpath resources.
			ClassPathResource mockResource = new ClassPathResource(fileName);
			// Initialize a BufferedReader to read data from the file.
			br = new BufferedReader(new InputStreamReader(mockResource.getInputStream()));

			String st;
			String originName;

			while ((st = br.readLine()) != null) {

				if (st == null || st.trim().equalsIgnoreCase(""))
					continue;
				// Check if metric filtering is enabled.
				if (firehoseProperties.isMetricFilterEnable()) {
					if (!fileName.equals(MockConstant.CONTAINER_METRIC_MOCK_FILENAME)) {
						originName = MockUtils.getOrigionMock(st);
					} else {
						originName = "";
					}
					// Check if the metric should be filtered based on the metric filter logic.
					if (!metricFilter(originName, MockUtils.getMetricNameMock(st), firehoseProperties,
							event.getSubscriberId()))
						continue;
				}

				if (fileName.equals(MockConstant.CONTAINER_METRIC_MOCK_FILENAME)) {
					String organizationName = null;
					String spaceName = null;
					String appName = null;
					String[] lineSplitter = st.split("\\.");

					try {

						if (lineSplitter != null && lineSplitter.length > 11) {
							organizationName = lineSplitter[8];
							spaceName = lineSplitter[9];
							appName = lineSplitter[11];
						}

					} catch (Exception e) {
						logger.severe("Exception occured while getting oganizationName, SpaceName, AppName from lineSplitter, reason: " + e.getMessage());
					}

					if (!MockUtils.filterByAppsSpacesOrgsMock(firehoseProperties, organizationName, spaceName, appName,
							event))
						continue;

				}

				boolean validData = false;

				if (pcfProperties.getMockFilter() != null && !pcfProperties.getMockFilter().isEmpty()) {

					for (String iterateObj : pcfProperties.getMockFilter()) {
						if (st.contains(iterateObj)) {
							validData = true;
							break;
						}
					}
				} else
					validData = true;

				if (validData) {
					StringBuilder sb = new StringBuilder();
					String arrData[] = st.split("\\|");
					if (arrData != null) {

						int newVal = arrData.length - 2;
						for (int i = 0; i < arrData.length - 1; i++) {

							if (i == newVal && arrData[i].equalsIgnoreCase("0"))
								sb.append(MetricUtils.getRandomNumber());
							else
								sb.append(arrData[i]);

							sb.append("|");
						}

						sb.append(timestamp);
						sb.append("\n");
						data.add(sb.toString());

					}

				} // validation ends

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception occured while getMockStats, " + e.getMessage());

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Exception occured while getMockStats, closing br, " + e.getMessage());
				}
			}

			br = null;
		}

	}

	// getMetricNameMock extracts metric name from a data string
	public static String getMetricNameMock(String data) {

		String[] metricSplit = data.split("\\.");
		String metricName = null;
		if (metricSplit != null) {
			// Split the last part of metricSplit by the pipe character ('|').
			String[] metricArray = metricSplit[metricSplit.length - 1].split("\\|");
			if (metricArray != null)
				metricName = metricArray[0];
		}

		return metricName;
	}

	// getOrigionMock extracts origin from a data string
	public static String getOrigionMock(String data) {

		String[] origionArr = data.split("\\.");
		String origion = null;
		if (origionArr != null && origionArr.length > 3) {

			origion = origionArr[3]; // The fourth element in the array is the origin.
		}

		return origion;
	}

	// pushtoInternalQueue pushes data to the internal mock queue
	public static void pushtoInternalQueue(String fileName, PcfProperties pcfProperties,
			FirehoseProperties firehoseProperties, long timestamp, String eventType, Event event) {
		BufferedReader br = null;
		List<String> data = new ArrayList<>();

		try {
			ClassPathResource mockResource = new ClassPathResource(fileName);
			br = new BufferedReader(new InputStreamReader(mockResource.getInputStream()));
			String st;
			String originName = "";
			while ((st = br.readLine()) != null) {
				if (st == null || st.trim().equalsIgnoreCase("")) {
					continue;
				}
				// Check if metric filtering is enabled and apply filtering logic.
				if (firehoseProperties.isMetricFilterEnable()) {
					if (!fileName.equals(MockConstant.CONTAINER_METRIC_MOCK_FILENAME)) {
						originName = MockUtils.getOrigionMock(st);
					} else {
						originName = "";
					}
					if (!metricFilter(originName, MockUtils.getMetricNameMock(st), firehoseProperties,
							event.getSubscriberId())) {
						continue; // Skip the data if it doesn't pass the metric filter.
					}
				}
				boolean validData = false;
				// Check if the data contains any filters from the PCF properties.
				if (pcfProperties.getMockFilter() != null && !pcfProperties.getMockFilter().isEmpty()) {

					for (String iterateObj : pcfProperties.getMockFilter()) {
						if (st.contains(iterateObj)) {
							validData = true; // Data contains a filter, mark it as valid.
							break;
						}
					}
				} else
					validData = true; // No filters specified, consider all data as valid.

				if (validData) {
					StringBuilder sb = new StringBuilder();
					String arrData[] = st.split("\\|");
					if (arrData != null) {

						int newVal = arrData.length - 2;
						for (int i = 0; i < arrData.length - 1; i++) {

							if (i == newVal && arrData[i].equalsIgnoreCase("0"))
								sb.append(MetricUtils.getRandomNumber()); // Replace "0" with a random number.
							else
								sb.append(arrData[i]);

							sb.append("|");
						}

						sb.append(timestamp); // Append the timestamp.
						sb.append("\n");
						data.add(sb.toString());

					}

				} // validation ends

			}

			// pushing to Queue
			String subscriberIde = null;
			if (firehoseProperties.isEnbaleRestAPI())
				subscriberIde = event.getSubscriberId();
			else
				subscriberIde = firehoseProperties.getSubscriptionId();

			for (String dataObj : data) {
				MockUtils.pushtoMockQueue(MockUtils.getOrigionMock(dataObj), MockUtils.getMetricNameMock(dataObj),
						dataObj, subscriberIde);
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception occured while doing pushtoInternalQueue, " + e.getMessage());

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE, e.getMessage());

				}
			}

			br = null;
		}

	}

	// pushMockDatatoproxy sends mock data to a proxy
	public static void pushMockDatatoproxy(List<String> data, ProxyProperties proxyProperties) {

		int counter = 0; // Initialize a counter to keep track of the number of data items accumulated.
		StringBuilder sbDataSet = new StringBuilder(); // Initialize a StringBuilder to accumulate data.

		for (String stats : data) {
			if (counter >= proxyProperties.getPacketbatchSize()) {
				// When the accumulated data exceeds the batch size, send it.
				if (sendMessages(sbDataSet, proxyProperties)) {
					sbDataSet = null;
					sbDataSet = new StringBuilder();
					sbDataSet.append(stats);
					counter = 1;
				} else {
					logger.log(Level.WARNING,
							"Buffer is not null, but still data didnot sent over UDP for " + data.size());
				}
			} else {
				sbDataSet.append(stats); // Append the current data to the accumulated data.
				counter++;
			}
		}
		if (sbDataSet.length() > 0) {
			// Send any remaining accumulated data.
			if (sendMessages(sbDataSet, proxyProperties)) {
				sbDataSet = null;
				sbDataSet = new StringBuilder();
				counter = 0;
			} else {
				logger.log(Level.WARNING,
						"Buffer is not null, but still data didnot sent remaning data over UDP. " + data.size());
			}
		}
	}

	// sendMessages sends data over UDP to the target
	public static boolean sendMessages(StringBuilder sbDataSet, ProxyProperties proxyProperties) {
		String targetIp = proxyProperties.getProxyHostname(); // Replace with the
		int targetPort = proxyProperties.getProxyPort(); // Replace with the actual port
		boolean returnType = true;

		if (MetricUtils.dynamicLogging())
			logger.info("Sending mock data over " + targetIp + ",port=" + targetPort);

		InetAddress targetAddress = UDPConnection.getInetAddress(targetIp);

		if (targetAddress != null) {
			byte[] sendData = null;
			sendData = sbDataSet.toString().getBytes();
			DatagramPacket packet = new DatagramPacket(sendData, sendData.length, targetAddress, targetPort); // Create
			DatagramSocket udp = UDPConnection.getUDPConnection(); // Get a UDP connection (DatagramSocket).

			if (udp != null) {
				try {
					udp.send(packet); // Send the data over UDP.
					if (proxyProperties.isEnableLogs())
					{
						logger.info("*******************MOCK SAMPLES***************************");
						logger.info(sbDataSet.toString());
					}
						

				} catch (Exception e) {
					logger.severe("Exception occured while sending message, Reason: " + e.getMessage());
					returnType = false;
				}
			} else {
				logger.log(Level.WARNING, "UDP is NUll , So skipping to send the data over the UDP");
				returnType = false;
			}

		} else {
			logger.log(Level.WARNING, "targetAddress is null");
			returnType = false;

		}
		return returnType; // Return the success/failure status of the send operation.
	}

}
