package com.cfnozzle.service;

//import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.cfnozzle.model.AppEnvelope;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.ProxyProperties;

public class LogUtil {
	private static LogUtil logUtilInstance = null;
	private static final Logger logger = Logger.getLogger(LogUtil.class.getCanonicalName());

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private StringBuilder batchTemplate = new StringBuilder();
	private long count = 0;
	private int callCounter = 0;
	private static ConcurrentLinkedQueue<StringBuilder> clq = new ConcurrentLinkedQueue<StringBuilder>();

	// properties
	private String url = "http://127.0.0.1:9200/netforest";
	private String indexprefix = "cloudfoundry";
	private int connecttimeout = 60000;
	private int readtimeout = 60000;
	private int ingestionthreads = 2;
	private int batchsize = 125;

	private LogUtil(LogStorageProperties logStorageProperties) {

		this.url = logStorageProperties.getLogUrl();
		this.indexprefix = logStorageProperties.getLogIndexprefix();
		this.connecttimeout = logStorageProperties.getLogConnecttimeout();
		this.readtimeout = logStorageProperties.getLogReadtimeout();
		this.ingestionthreads = logStorageProperties.getLogIngestionthreads();
		this.batchsize = logStorageProperties.getLogBatchsize();

		init();

	}

	public static synchronized LogUtil getInstance(LogStorageProperties logStorageProperties) {
		if (logUtilInstance == null)
			logUtilInstance = new LogUtil(logStorageProperties);

		return logUtilInstance;
	}

	private void init() {

		// Create Thread pool

		ExecutorService executor = Executors.newFixedThreadPool(ingestionthreads);

		for (int i = 0; i < ingestionthreads; i++) {
			executor.submit(new Runnable() {
				public void run() {

					while (true) {
						if (!clq.isEmpty()) {
							try {

								HttpURLConnection con;
								String POST_URL = url + "/_bulk";
								URL obj = new URL(POST_URL);
								con = (HttpURLConnection) obj.openConnection();
								con.setRequestMethod("PUT");
								con.setDoInput(true);
								con.setRequestProperty("content-type", "application/json");
								con.setRequestProperty("Accept", "application/json");
								con.setDoOutput(true);
								con.setConnectTimeout(connecttimeout);
								con.setReadTimeout(readtimeout);

								int responseCode = 0;

								try (OutputStream os = con.getOutputStream()) {
									StringBuilder out;
									if ((out = clq.poll()) != null) {
										byte[] input = out.toString().getBytes("utf-8");
										os.write(input, 0, input.length);

										responseCode = con.getResponseCode();
										logger.info("Response code from NF .." + responseCode);

									}
								} catch (Exception e) {
									logger.severe("Exception occured while sending URL request to NF, reason: " + e.getMessage());
								}

							} catch (Exception e) {
								logger.severe("Exception occured while sending request to NF , reason: " + e.getMessage());
							}

						}
					}
				}
			});
		}

	}

	public synchronized void sendDataToNfdb(AppEnvelope envelope) {
		if (callCounter == 0) {
			createTimer();
			callCounter++;
		}

		try {
			String dateTimeStamp = new String(Instant.ofEpochMilli(getMillisTime(envelope.getEnvelope().getTimestamp()))
					.atZone(ZoneId.of("UTC")).format(formatter));
			String indexname = new String(indexprefix + "-" + dateTimeStamp.split("T")[0]);
			String nfType = new String("_doc");
			StringBuilder actionTemplate = new StringBuilder(
					"{\"index\": {\"_index\":\"" + indexname + "\",\"_type\":\"" + nfType + "\"}}\n");

			// if (envelope.getAppInfo().isPresent()) {

			batchTemplate = batchTemplate.append(actionTemplate).append("{\"message\":\""
					+ replaceTemplate(new StringBuilder(envelope.getEnvelope().getLogMessage().getMessage())).toString()
					+ "\"," + "\"@timestamp\":\"" + dateTimeStamp + "\"," + "\"organization\":\""
					+ envelope.getEnvelope().getTags().get("organization_name").toString() + "\"," + "\"space\":\""
					+ envelope.getEnvelope().getTags().get("space_name").toString() + "\"," + "\"applicationname\":\""
					+ envelope.getEnvelope().getTags().get("app_name").toString() + "\"}");
//			} else {
			// batchTemplate = batchTemplate.append(actionTemplate).append("{\"message\":\""
			// + replaceTemplate(new
			// StringBuilder(envelope.getEnvelope().getLogMessage().getMessage())).toString()
			// + "\"," + "\"@timestamp\":\"" + dateTimeStamp + "\"}");
			// }

			batchTemplate.append("\n");
		} catch (Exception e) {
		    logger.severe("Exception occured in Sending Data To NFDB, reason: " + e.getMessage());
		}
		count++;
		if (count == batchsize) {

			count = addBatchToQueue(batchTemplate);

		}

	}

	// create timer
	private void createTimer() {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (batchTemplate.length() > 0) {
					count = addBatchToQueue(batchTemplate);
				}
			}
		};

		timer.schedule(task, 60000, 60000);

	}

	// add batch to queue
	private int addBatchToQueue(StringBuilder batchTemplate) {
		clq.add(new StringBuilder(batchTemplate));
		batchTemplate.delete(0, batchTemplate.length());

		return 0;
	}

	// replace method to handle special characters in log message to avoid json
	// parse exception
	private StringBuilder replaceTemplate(StringBuilder s) {

		StringBuilder nReplace = new StringBuilder(Pattern.compile("\n").matcher(s).replaceAll("\\\\n"));
		StringBuilder tReplace = new StringBuilder(Pattern.compile("\t").matcher(nReplace).replaceAll(" "));
		StringBuilder rReplace = new StringBuilder(Pattern.compile("\r").matcher(tReplace).replaceAll(""));
		StringBuilder slashReplace = new StringBuilder(
				Pattern.compile("\\\\").matcher(rReplace).replaceAll("\\\\\\\\"));
		StringBuilder quoteReplace = new StringBuilder(
				Pattern.compile("\"").matcher(slashReplace).replaceAll("\\\\\""));

		return quoteReplace;
	}

	private long getMillisTime(long time) {
		long milliTime = time / 1000000;
		return milliTime;
	}

	public synchronized void SendingMockDataToNF(String organization_name, String space_name, String app_name,
			String message, long timestamp, ProxyProperties proxyProperties) {
		if (callCounter == 0) {
			createTimer();
			callCounter++;
		}

		try {
			String dateTimeStamp = new String(
					Instant.ofEpochMilli(getMillisTime(timestamp)).atZone(ZoneId.of("UTC")).format(formatter));
			String indexname = new String(indexprefix + "-" + dateTimeStamp.split("T")[0]);
			String nfType = new String("_doc");
			StringBuilder actionTemplate = new StringBuilder(
					"{\"index\": {\"_index\":\"" + indexname + "\",\"_type\":\"" + nfType + "\"}}\n");

			batchTemplate = batchTemplate.append(actionTemplate)
					.append("{\"message\":\"" + replaceTemplate(new StringBuilder(message)).toString() + "\","
							+ "\"@timestamp\":\"" + dateTimeStamp + "\"," + "\"organization\":\"" + organization_name
							+ "\"," + "\"space\":\"" + space_name + "\"," + "\"applicationname\":\"" + app_name
							+ "\"}");

			batchTemplate.append("\n");

			if (proxyProperties.isEnableLogs())
				logger.info(batchTemplate.toString());

		} catch (Exception e) {
			logger.severe("Exception occured whie sending mock data to NF, reason: " + e.getMessage());
		}
		count++;
		if (count == batchsize) {

			count = addBatchToQueue(batchTemplate);

		}

	}

}
