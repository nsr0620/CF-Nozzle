package com.cfnozzle.utils;

import static com.cfnozzle.utils.Constants.APPLICATION_ID;
import static com.cfnozzle.utils.Constants.APPLICATION_NAME;
import static com.cfnozzle.utils.Constants.DEPLOYMENT;
import static com.cfnozzle.utils.Constants.INDEX;
import static com.cfnozzle.utils.Constants.INSTANCE_INDEX;
import static com.cfnozzle.utils.Constants.IP;
import static com.cfnozzle.utils.Constants.JOB;
import static com.cfnozzle.utils.Constants.METRICS_NAME_SEP;
import static com.cfnozzle.utils.Constants.ORG;
import static com.cfnozzle.utils.Constants.ORIGIN;
import static com.cfnozzle.utils.Constants.PCF_PREFIX;
import static com.cfnozzle.utils.Constants.SPACE;
import static com.cfnozzle.utils.Constants.TOTAL_SUFFIX;
import static org.cloudfoundry.doppler.EventType.CONTAINER_METRIC;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;

import com.cfnozzle.model.AppEnvelope;
import com.cfnozzle.model.AppInfo;
import com.cfnozzle.props.FirehoseProperties;

public class MetricUtils {

	private static String INET_ADDR_LOCAL_HOST_NAME;

	static {
		try {
			INET_ADDR_LOCAL_HOST_NAME = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			INET_ADDR_LOCAL_HOST_NAME = "unknown";
		}
	}

	public static long getTimestamp(Envelope envelope) {

		if (envelope == null || envelope.getTimestamp() == null) {
			return System.currentTimeMillis();
		} else {
			return envelope.getTimestamp();
		}
	}

	public static String getSource(Envelope envelope) {

		if (!isBlank(envelope.getIp())) {
			return envelope.getIp();
		}
		if (!isBlank(envelope.getJob())) {
			return envelope.getJob();
		}

		return INET_ADDR_LOCAL_HOST_NAME;
	}

	public static String getPcfMetricNamePrefix() {
		return PCF_PREFIX + METRICS_NAME_SEP;
	}

	public static String getOrigin(Envelope envelope) {
		// Note - Don't invoke envelope.getOrigin elsewhere in the code
		// because in future we might convert origin to lower_case
		return envelope.getOrigin();
	}

	public static Map<String, String> getTags(AppEnvelope appEnvelope) {
		Envelope envelope = appEnvelope.getEnvelope();
		Map<String, String> map = new TreeMap<String, String>();

		if (envelope.getOrigin() != null && envelope.getOrigin().length() > 0) {
			map.put(ORIGIN, Objects.toString(envelope.getOrigin()));
		}
		if (envelope.getIp() != null && envelope.getIp().length() > 0) {
			map.put(IP, Objects.toString(envelope.getIp()));
		}
		if (envelope.getIndex() != null && envelope.getIndex().length() > 0) {
			map.put(INDEX, Objects.toString(envelope.getIndex()));
		}

		if (envelope.getDeployment() != null && envelope.getDeployment().length() > 0) {
			map.put(DEPLOYMENT, Objects.toString(envelope.getDeployment()));
		}
		if (envelope.getJob() != null && envelope.getJob().length() > 0) {
			map.put(JOB, Objects.toString(envelope.getJob()));
		}

		if (envelope.getEventType().equals(CONTAINER_METRIC)) {

			String applicationId = envelope.getContainerMetric().getApplicationId();
			map.put(APPLICATION_ID, applicationId);
			map.put(INSTANCE_INDEX, String.valueOf(envelope.getContainerMetric().getInstanceIndex().toString()));
		}

		Optional<AppInfo> optionalAppInfo = appEnvelope.getAppInfo();
		if (optionalAppInfo.isPresent()) {
			AppInfo appInfo = optionalAppInfo.get();
			String applicationName = appInfo.getApplicationName();
			if (applicationName != null && applicationName.length() > 0) {
				map.put(APPLICATION_NAME, applicationName);
			}
			String org = appInfo.getOrg();
			if (org != null && org.length() > 0) {
				map.put(ORG, org);
			}
			String space = appInfo.getSpace();
			if (space != null && space.length() > 0) {
				map.put(SPACE, space);
			}
		}

		// Add all pre-existing PCF envelope tags ...
		map.putAll(envelope.getTags());
		
		try {
			map.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
		} catch (Exception e) {
			// TODO: handle exception
		}
		return map;
	}

	private static boolean isBlank(@Nullable String s) {
		if (s == null || s.isEmpty()) {
			return true;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static void addingTags(StringBuilder builder, Map<String, String> tags, String key, String eventType) {

		String obj = tags.get(key);
		if (obj != null) {
			builder.append(obj.replaceAll("\\.", "_"));
			builder.append(METRICS_NAME_SEP);
		}
	}

	public static StringBuilder getStatsFromTags(Map<String, String> tags, String metricName, long metricValue, Long timestamp,
			String eventType) {

		try {
			tags.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
		} catch (Exception e2) {
		}

		String tagsArr[] = null;
		StringBuilder metricBuffer = new StringBuilder();

		if (eventType.equalsIgnoreCase(EventType.CONTAINER_METRIC.toString()))
			tagsArr = Constants.CONTAINER_TAGS_SEQUENCE.split(",");
		else if (eventType.equalsIgnoreCase(EventType.VALUE_METRIC.toString()))
			tagsArr = Constants.VALUE_TAGS_SEQUENCE.split(",");
		else if (eventType.equalsIgnoreCase(EventType.COUNTER_EVENT.toString()))
			tagsArr = Constants.EVENTS_TAGS_SEQUENCE.split(",");

		if (tagsArr != null) {

			String datatype = Datatypes.dataTypesMapping.get(eventType);
			if (datatype == null)
				datatype = Constants.DEFAULT_DATA_TYPE;
			metricBuffer.append(datatype);
			metricBuffer.append(".");
			metricBuffer.append(Constants.TAG_NAME_UDP_FORMAT);
			metricBuffer.append(".");

			for (String key : tagsArr)
				addingTags(metricBuffer, tags, key, eventType);
			metricBuffer.append(metricName);
			metricBuffer.append("|");
			metricBuffer.append(metricValue);
			metricBuffer.append("|");
			metricBuffer.append(timestamp);
			metricBuffer.append("\n");
		}

		return metricBuffer;

	}

	static Random random = new Random();

	public static int getRandomNumber() {
		return random.nextInt(1000) + 1;// random number between 1 to 10
	}

	public static boolean dynamicLogging() {

		int suffix = getRandomNumber();
		if (suffix <= 5) {
			return true;
		} else {
			return false;
		}
	}

	public static Map<String, Map<String, AppEnvelope>> metricQueue = new ConcurrentHashMap<>();

	public static void pushToQueue(AppEnvelope appEnvelope, String subscriberId) {

		Map<String, AppEnvelope> metricMap = metricQueue.get(subscriberId);

		String metricName = null;
		String origion = null;
		try {

			if (appEnvelope.getEnvelope().getEventType() == EventType.VALUE_METRIC)
				metricName = ValueMetricUtils.getMetricName(appEnvelope.getEnvelope());
			else
				metricName = CounterEventUtils.getMetricName(appEnvelope.getEnvelope(), TOTAL_SUFFIX);

			origion = appEnvelope.getEnvelope().getOrigin();

		} catch (Exception e) {

		}

		if (metricName != null && origion != null) {
			if (metricMap == null) {
				metricMap = new ConcurrentHashMap<>();

				metricMap.put(MetricUtils.generateKeyForQueue(metricName, origion), appEnvelope);

				metricQueue.put(subscriberId, metricMap);
			} else {

				metricMap.put(MetricUtils.generateKeyForQueue(metricName, origion), appEnvelope);

			}

		}

	}

	public static void removeAllMtericesFromQueue() {
		metricQueue.clear();
	}

	public static Collection<AppEnvelope> getMetricKeyFromQueue(String subscriberId) {

		if (metricQueue.get(subscriberId) != null)
			return metricQueue.get(subscriberId).values();
		else
			return Collections.emptyList();

	}

	public static Map<String, Set<String>> availableFilterList = new HashMap<String, Set<String>>();

	public static Map<String, Long> healthCheckMap = new ConcurrentHashMap<String, Long>();
	public static Set<String> metricConfigurationList = Collections.synchronizedSet(new HashSet<String>());

	static { // container
		metricConfigurationList.add("cpu_percentage");
		metricConfigurationList.add("disk_bytes");
		metricConfigurationList.add("disk_bytes_quota");
		metricConfigurationList.add("memory_bytes");
		metricConfigurationList.add("memory_bytes_quota");

		// counter Event
		metricConfigurationList.add("ingress_total");
		metricConfigurationList.add("promhttp_metric_handler_errors_total_total");
		metricConfigurationList.add("go_gc_duration_seconds_count_total");
		metricConfigurationList.add("go_memstats_alloc_bytes_total_total");
		metricConfigurationList.add("go_memstats_mallocs_total_total");
		metricConfigurationList.add("binding_refresh_count_total");
		metricConfigurationList.add("go_memstats_frees_total_total");
		metricConfigurationList.add("go_memstats_lookups_total_total");
		metricConfigurationList.add("dropped_total");
		metricConfigurationList.add("origin_mappings_total");
		metricConfigurationList.add("egress_total");
		metricConfigurationList.add("num_scrapes_total");
		metricConfigurationList.add("invalid_metric_label_total");
		metricConfigurationList.add("binding_refresh_error_total");
		metricConfigurationList.add("failed_scrapes_total");
		metricConfigurationList.add("scrape_targets_total_total");
		metricConfigurationList.add("modified_tags_total");
		metricConfigurationList.add("failed_scrapes_total_total");
		metricConfigurationList.add("log_cache_ingress_total");
		metricConfigurationList.add("sent_total");
		metricConfigurationList.add("log_cache_expired_total");
		metricConfigurationList.add("log_cache_egress_total");
		metricConfigurationList.add("log_cache_promql_timeout_total");
		metricConfigurationList.add("UptimeRequestCount_total");
		metricConfigurationList.add("InternalPoliciesRequestCount_total");
		metricConfigurationList.add("bad_gateways_total");
		metricConfigurationList.add("backend_exhausted_conns_total");
		metricConfigurationList.add("backend_invalid_id_total");
		metricConfigurationList.add("backend_invalid_tls_cert_total");
		metricConfigurationList.add("backend_tls_handshake_failed_total");
		metricConfigurationList.add("rejected_requests_total");
		metricConfigurationList.add("total_requests_total");
		metricConfigurationList.add("responses_total");
		metricConfigurationList.add("responses_2xx_total");
		metricConfigurationList.add("responses_3xx_total");
		metricConfigurationList.add("responses_4xx_total");
		metricConfigurationList.add("responses_5xx_total");
		metricConfigurationList.add("responses_xxx_total");
		metricConfigurationList.add("routed_app_requests_total");
		metricConfigurationList.add("routes_pruned_total");
		metricConfigurationList.add("websocket_failures_total");
		metricConfigurationList.add("websocket_upgrades_total");
		metricConfigurationList.add("requests_CloudController_total");
		metricConfigurationList.add("requests_uaa_total");
		metricConfigurationList.add("registry_message_uaa_total");
		metricConfigurationList.add("registry_message_total");
		metricConfigurationList.add("requests_route-emitter_total");
		metricConfigurationList.add("metric_registrar_orchestrator_registration_fetch_errors_total");
		metricConfigurationList.add("metric_registrar_orchestrator_log_worker_list_errors_total");
		metricConfigurationList.add("metric_registrar_orchestrator_secure-endpoint_worker_list_errors_total");
		metricConfigurationList.add("metric_registrar_orchestrator_endpoint_worker_list_errors_total");
		metricConfigurationList.add("metric_registrar_log_worker_event_emission_errors_total");
		metricConfigurationList.add("metric_registrar_log_worker_envelopes_sent_total");
		metricConfigurationList.add("ConvergenceTasksPruned_total");
		metricConfigurationList.add("ConvergenceTasksKicked_total");
		metricConfigurationList.add("metric_registrar_endpoint_worker_event_emission_errors_total");
		metricConfigurationList.add("metric_registrar_endpoint_worker_envelopes_sent_total");
		metricConfigurationList.add("HTTPRouteNATSMessagesEmitted_total");
		metricConfigurationList.add("InternalRouteNATSMessagesEmitted_total");
		metricConfigurationList.add("RoutesSynced_total");
		metricConfigurationList.add("doppler_proxy_slow_consumer_total");
		metricConfigurationList.add("doppler_proxy_log_cache_failure_total");
		metricConfigurationList.add("registry_message_route-emitter_total");
		metricConfigurationList.add("registry_message_blobstore_total");
		metricConfigurationList.add("registry_message_NetworkPolicyServer_total");
		metricConfigurationList.add("registry_message_CloudController_total");
		metricConfigurationList.add("registry_message_MySQLProxy_total");
		metricConfigurationList.add("LeasesRenewRequestCount_total");
		metricConfigurationList.add("LeasesIndexRequestCount_total");
		metricConfigurationList.add("ConvergenceLRPRuns_total");
		metricConfigurationList.add("ConvergenceTaskRuns_total");
		metricConfigurationList.add("query_error_total");
		metricConfigurationList.add("log_router_disconnects_total");
		metricConfigurationList.add("log_router_connects_total");
		metricConfigurationList.add("rejected_streams_total");
		metricConfigurationList.add("RoutesRegistered_total");
		metricConfigurationList.add("RoutesUnregistered_total");
		metricConfigurationList.add("requests_MySQLProxy_total");
		metricConfigurationList.add("RequestCount_total");
		metricConfigurationList.add("AuctioneerLRPAuctionsStarted_total");
		metricConfigurationList.add("AuctioneerTaskAuctionsStarted_total");
		metricConfigurationList.add("AuctioneerLRPAuctionsFailed_total");
		metricConfigurationList.add("AuctioneerTaskAuctionsFailed_total");
		metricConfigurationList.add("CredCreationSucceededCount_total");
		metricConfigurationList.add("metric_registrar_log_worker_log_envelopes_received_total");
		metricConfigurationList.add("process_cpu_seconds_total_total");
		metricConfigurationList.add("ContainerCompletedCount_total");

		// Value Metric
		metricConfigurationList.add("requests_outstanding_gauge_gauge");
		metricConfigurationList.add("latency_ms");

		metricConfigurationList.add("latency_uaa_ms");
		metricConfigurationList.add("route_lookup_time_ns");
		metricConfigurationList.add("requests_outstanding_counter");
		metricConfigurationList.add("requests_completed_counter");
		metricConfigurationList.add("http_status_2XX_counter");
		metricConfigurationList.add("numCPUS_count");
		metricConfigurationList.add("numGoRoutines_count");
		metricConfigurationList.add("memoryStats_numBytesAllocatedHeap_count");
		metricConfigurationList.add("memoryStats_numBytesAllocatedStack_count");
		metricConfigurationList.add("memoryStats_numBytesAllocated_count");
		metricConfigurationList.add("memoryStats_numMallocs_count");
		metricConfigurationList.add("memoryStats_numFrees_count");
		metricConfigurationList.add("memoryStats_lastGCPauseTimeNS_count");
		metricConfigurationList.add("latency_CloudController_ms");
		metricConfigurationList.add("uptime_seconds");
		metricConfigurationList.add("total_http_routes_gauge");
		metricConfigurationList.add("total_http_subscriptions_gauge");
		metricConfigurationList.add("total_tcp_routes_gauge");
		metricConfigurationList.add("total_tcp_subscriptions_gauge");
		metricConfigurationList.add("total_token_errors_gauge");
		metricConfigurationList.add("key_refresh_events_gauge");
		metricConfigurationList.add("process_resident_memory_bytes_");
		metricConfigurationList.add("process_virtual_memory_bytes_");
		metricConfigurationList.add("process_virtual_memory_max_bytes_");
		metricConfigurationList.add("go_memstats_heap_sys_bytes_");
		metricConfigurationList.add("process_max_fds_");
		metricConfigurationList.add("go_info_");
		metricConfigurationList.add("go_memstats_heap_objects_");
		metricConfigurationList.add("go_memstats_other_sys_bytes_");
		metricConfigurationList.add("go_memstats_heap_released_bytes_");
		metricConfigurationList.add("process_start_time_seconds_");
		metricConfigurationList.add("go_gc_duration_seconds_sum_");
		metricConfigurationList.add("go_gc_duration_seconds_");
		metricConfigurationList.add("go_memstats_gc_cpu_fraction_");
		metricConfigurationList.add("go_memstats_gc_sys_bytes_");
		metricConfigurationList.add("go_memstats_buck_hash_sys_bytes_");
		metricConfigurationList.add("go_memstats_stack_sys_bytes_");
		metricConfigurationList.add("process_open_fds_");
		metricConfigurationList.add("go_memstats_mspan_inuse_bytes_");
		metricConfigurationList.add("go_memstats_mspan_sys_bytes_");
		metricConfigurationList.add("go_memstats_next_gc_bytes_");
		metricConfigurationList.add("go_memstats_sys_bytes_");
		metricConfigurationList.add("go_threads_");
		metricConfigurationList.add("go_memstats_mcache_inuse_bytes_");
		metricConfigurationList.add("go_memstats_heap_idle_bytes_");
		metricConfigurationList.add("go_memstats_heap_alloc_bytes_");
		metricConfigurationList.add("go_memstats_heap_inuse_bytes_");
		metricConfigurationList.add("go_memstats_last_gc_time_seconds_");
		metricConfigurationList.add("go_memstats_alloc_bytes_");
		metricConfigurationList.add("go_memstats_stack_inuse_bytes_");
		metricConfigurationList.add("go_goroutines_");
		metricConfigurationList.add("go_memstats_mcache_sys_bytes_");
		metricConfigurationList.add("aggregate_drains_count");
		metricConfigurationList.add("invalid_drains_total");
		metricConfigurationList.add("blacklisted_drains_total");
		metricConfigurationList.add("drains_count");
		metricConfigurationList.add("latency_for_last_binding_refresh_ms");
		metricConfigurationList.add("active_drains_count");
		metricConfigurationList.add("last_total_scrape_duration_ms");
		metricConfigurationList.add("last_total_attempted_scrapes_total");
		metricConfigurationList.add("last_total_failed_scrapes_total");
		metricConfigurationList.add("LastCAPIV4LogAccessLatency_");
		metricConfigurationList.add("LastCAPIV2ServiceInstancesLatency_");
		metricConfigurationList.add("LastCAPIV3AppsLatency_");
		metricConfigurationList.add("LastUAALatency_");
		metricConfigurationList.add("LastCAPIV2ListServiceInstancesLatency_");
		metricConfigurationList.add("doppler_v2_streams_");
		metricConfigurationList.add("last_binding_refresh_count_");
		metricConfigurationList.add("doppler_connections_");
		metricConfigurationList.add("average_envelopes_bytes/minute");
		metricConfigurationList.add("cached_bindings_");
		metricConfigurationList.add("log_cache_heap_in_use_memory_bytes");
		metricConfigurationList.add("log_cache_promql_instant_query_time_milliseconds");
		metricConfigurationList.add("log_cache_total_system_memory_bytes");
		metricConfigurationList.add("leadership_status_");
		metricConfigurationList.add("log_cache_memory_utilization_percentage");
		metricConfigurationList.add("log_cache_truncation_duration_milliseconds");
		metricConfigurationList.add("log_cache_promql_range_query_time_milliseconds");
		metricConfigurationList.add("log_cache_store_size_entries");
		metricConfigurationList.add("log_cache_cache_period_milliseconds");
		metricConfigurationList.add("log_cache_available_system_memory_bytes");
		metricConfigurationList.add("log_cache_uptime_seconds");
		metricConfigurationList.add("cf_auth_proxy_last_capiv3_apps_by_name_latency_nanoseconds");
		metricConfigurationList.add("cf_auth_proxy_last_capiv3_apps_latency_nanoseconds");
		metricConfigurationList.add("cf_auth_proxy_last_capiv3_list_service_instances_latency_nanoseconds");
		metricConfigurationList.add("vitals_jvm_cpu_load_gauge");
		metricConfigurationList.add("vitals_jvm_thread_count_gauge");
		metricConfigurationList.add("vitals_jvm_non-heap_init_gauge");
		metricConfigurationList.add("vitals_jvm_non-heap_committed_gauge");
		metricConfigurationList.add("vitals_jvm_non-heap_used_gauge");
		metricConfigurationList.add("vitals_jvm_non-heap_max_gauge");
		metricConfigurationList.add("vitals_jvm_heap_init_gauge");
		metricConfigurationList.add("vitals_jvm_heap_committed_gauge");
		metricConfigurationList.add("vitals_jvm_heap_used_gauge");
		metricConfigurationList.add("vitals_jvm_heap_max_gauge");
		metricConfigurationList.add("memoryStats_numBytesAllocatedHeap_Bytes");
		metricConfigurationList.add("memoryStats_numBytesAllocatedStack_Bytes");
		metricConfigurationList.add("memoryStats_lastGCPauseTimeNS_ns");
		metricConfigurationList.add("numGoRoutines_Count");
		metricConfigurationList.add("deployments_update_duration_ms");
		metricConfigurationList.add("UptimeRequestTime_ms");
		metricConfigurationList.add("StoreByGuidsSuccessTime_ms");
		metricConfigurationList.add("InternalPoliciesRequestTime_ms");
		metricConfigurationList.add("latency_route-emitter_ms");
		metricConfigurationList.add("subscriptions_subscriptions");
		metricConfigurationList.add("system_disk_system_inode_percent_Percent");
		metricConfigurationList.add("system_disk_ephemeral_inode_percent_Percent");
		metricConfigurationList.add("system_load_1m_Load");
		metricConfigurationList.add("system_swap_percent_Percent");
		metricConfigurationList.add("system_mem_percent_Percent");
		metricConfigurationList.add("system_mem_kb_Kb");
		metricConfigurationList.add("system_healthy_b");
		metricConfigurationList.add("system_disk_ephemeral_percent_Percent");
		metricConfigurationList.add("system_cpu_user_Load");
		metricConfigurationList.add("system_disk_system_percent_Percent");
		metricConfigurationList.add("system_cpu_sys_Load");
		metricConfigurationList.add("system_swap_kb_Kb");
		metricConfigurationList.add("system_cpu_wait_Load");
		metricConfigurationList.add("job_queue_length_cc-control-0_gauge");
		metricConfigurationList.add("job_queue_length_cc-generic_gauge");
		metricConfigurationList.add("job_queue_length_total_gauge");
		metricConfigurationList.add("thread_info_thread_count_gauge");
		metricConfigurationList.add("thread_info_event_machine_connection_count_gauge");
		metricConfigurationList.add("thread_info_event_machine_threadqueue_size_gauge");
		metricConfigurationList.add("thread_info_event_machine_threadqueue_num_waiting_gauge");
		metricConfigurationList.add("thread_info_event_machine_resultqueue_size_gauge");
		metricConfigurationList.add("thread_info_event_machine_resultqueue_num_waiting_gauge");
		metricConfigurationList.add("failed_job_count_cc-control-0_gauge");
		metricConfigurationList.add("failed_job_count_cc-generic_gauge");
		metricConfigurationList.add("failed_job_count_total_gauge");
		metricConfigurationList.add("audit_service_principal_not_found_count_gauge");
		metricConfigurationList.add("audit_service_client_authentication_failure_count_gauge");
		metricConfigurationList.add("audit_service_user_authentication_count_gauge");
		metricConfigurationList.add("audit_service_user_authentication_failure_count_gauge");
		metricConfigurationList.add("audit_service_user_not_found_count_gauge");
		metricConfigurationList.add("audit_service_principal_authentication_failure_count_gauge");
		metricConfigurationList.add("audit_service_user_password_failures_gauge");
		metricConfigurationList.add("audit_service_client_authentication_count_gauge");
		metricConfigurationList.add("audit_service_user_password_changes_gauge");
		metricConfigurationList.add("vitals_uptime_gauge");
		metricConfigurationList.add("vitals_cpu_gauge");
		metricConfigurationList.add("vitals_mem_bytes_gauge");
		metricConfigurationList.add("vitals_cpu_load_avg_gauge");
		metricConfigurationList.add("vitals_mem_used_bytes_gauge");
		metricConfigurationList.add("vitals_mem_free_bytes_gauge");
		metricConfigurationList.add("vitals_num_cores_gauge");
		metricConfigurationList.add("log_count_off_gauge");
		metricConfigurationList.add("log_count_fatal_gauge");
		metricConfigurationList.add("log_count_error_gauge");
		metricConfigurationList.add("log_count_warn_gauge");
		metricConfigurationList.add("log_count_info_gauge");
		metricConfigurationList.add("log_count_debug_gauge");
		metricConfigurationList.add("log_count_debug1_gauge");
		metricConfigurationList.add("log_count_debug2_gauge");
		metricConfigurationList.add("log_count_all_gauge");
		metricConfigurationList.add("tasks_running_count_gauge");
		metricConfigurationList.add("tasks_running_memory_in_mb_gauge");
		metricConfigurationList.add("deployments_deploying_gauge");
		metricConfigurationList.add("file_descriptors_file");
		metricConfigurationList.add("buffered_messages_message");
		metricConfigurationList.add("total_dropped_messages_message");
		metricConfigurationList.add("requests_global_completed_time_gauge");
		metricConfigurationList.add("requests_global_completed_count_counter");
		metricConfigurationList.add("requests_global_unhealthy_count_counter");
		metricConfigurationList.add("requests_global_unhealthy_time_gauge");
		metricConfigurationList.add("requests_global_status_1xx_count_counter");
		metricConfigurationList.add("requests_global_status_2xx_count_counter");
		metricConfigurationList.add("requests_global_status_3xx_count_counter");
		metricConfigurationList.add("requests_global_status_4xx_count_counter");
		metricConfigurationList.add("requests_global_status_5xx_count_counter");
		metricConfigurationList.add("database_global_completed_time_gauge");
		metricConfigurationList.add("database_global_completed_count_counter");
		metricConfigurationList.add("database_global_unhealthy_count_counter");
		metricConfigurationList.add("database_global_unhealthy_time_gauge");
		metricConfigurationList.add("server_inflight_count_gauge");
		metricConfigurationList.add("server_up_time_gauge");
		metricConfigurationList.add("server_idle_time_gauge");
		metricConfigurationList.add("requests_uaa_global_metrics_completed_count_gauge");
		metricConfigurationList.add("requests_uaa_global_metrics_completed_time_gauge");
		metricConfigurationList.add("requests_oauth-oidc_completed_count_gauge");
		metricConfigurationList.add("requests_oauth-oidc_completed_time_gauge");
		metricConfigurationList.add("requests_clients_completed_count_gauge");
		metricConfigurationList.add("requests_clients_completed_time_gauge");
		metricConfigurationList.add("dnsRequest_request");
		metricConfigurationList.add("maxRouteMessageTimePerInterval_ms");
		metricConfigurationList.add("registerMessagesReceived_ms");
		metricConfigurationList.add("system_cpu_sys_Percent");
		metricConfigurationList.add("system_cpu_wait_Percent");
		metricConfigurationList.add("system_network_drop_in_Packets");
		metricConfigurationList.add("system_disk_persistent_percent_Percent");
		metricConfigurationList.add("system_disk_persistent_write_time_ms");
		metricConfigurationList.add("system_load_5m_Load");
		metricConfigurationList.add("system_network_packets_received_Packets");
		metricConfigurationList.add("system_network_tcp_curr_estab_");
		metricConfigurationList.add("system_disk_persistent_read_bytes_Bytes");
		metricConfigurationList.add("system_disk_persistent_write_bytes_Bytes");
		metricConfigurationList.add("system_disk_system_read_bytes_Bytes");
		metricConfigurationList.add("system_disk_ephemeral_read_bytes_Bytes");
		metricConfigurationList.add("system_disk_persistent_read_time_ms");
		metricConfigurationList.add("system_disk_system_inode_percent_Percent");
		metricConfigurationList.add("system_disk_system_write_bytes_Bytes");
		metricConfigurationList.add("system_mem_percent_Percent");
		metricConfigurationList.add("system_cpu_core_idle_Percent");
		metricConfigurationList.add("system_cpu_core_user_Percent");
		metricConfigurationList.add("system_disk_ephemeral_inode_percent_Percent");
		metricConfigurationList.add("system_swap_kb_KiB");
		metricConfigurationList.add("system_network_bytes_sent_Bytes");
		metricConfigurationList.add("system_network_drop_out_Packets");
		metricConfigurationList.add("system_network_udp_in_errors_");
		metricConfigurationList.add("system_healthy_");
		metricConfigurationList.add("system_mem_kb_KiB");
		metricConfigurationList.add("system_network_bytes_received_Bytes");
		metricConfigurationList.add("system_network_tcp_active_opens_");
		metricConfigurationList.add("system_cpu_core_wait_Percent");
		metricConfigurationList.add("system_disk_system_io_time_ms");
		metricConfigurationList.add("system_disk_system_write_time_ms");
		metricConfigurationList.add("system_load_15m_Load");
		metricConfigurationList.add("system_load_1m_Load");
		metricConfigurationList.add("system_network_tcp_retrans_segs_");
		metricConfigurationList.add("system_network_udp_lite_in_errors_");
		metricConfigurationList.add("system_disk_ephemeral_read_time_ms");
		metricConfigurationList.add("system_disk_persistent_inode_percent_Percent");
		metricConfigurationList.add("system_disk_system_percent_Percent");
		metricConfigurationList.add("system_disk_ephemeral_write_bytes_Bytes");
		metricConfigurationList.add("system_network_error_in_Frames");
		metricConfigurationList.add("system_network_error_out_Frames");
		metricConfigurationList.add("system_network_udp_no_ports_");
		metricConfigurationList.add("system_swap_percent_Percent");
		metricConfigurationList.add("system_cpu_idle_Percent");
		metricConfigurationList.add("system_cpu_user_Percent");
		metricConfigurationList.add("system_disk_ephemeral_io_time_ms");
		metricConfigurationList.add("system_disk_persistent_io_time_ms");
		metricConfigurationList.add("system_disk_system_read_time_ms");
		metricConfigurationList.add("system_network_ip_forwarding_");
		metricConfigurationList.add("system_network_packets_sent_Packets");
		metricConfigurationList.add("system_cpu_core_sys_Percent");
		metricConfigurationList.add("system_disk_ephemeral_percent_Percent");
		metricConfigurationList.add("system_disk_ephemeral_write_time_ms");
		metricConfigurationList.add("vitals_vm_cpu_count_gauge");
		metricConfigurationList.add("vitals_vm_cpu_load_gauge");
		metricConfigurationList.add("vitals_vm_memory_total_gauge");
		metricConfigurationList.add("vitals_vm_memory_committed_gauge");
		metricConfigurationList.add("vitals_vm_memory_free_gauge");
		metricConfigurationList.add("absolute_usage_nanoseconds");
		metricConfigurationList.add("absolute_entitlement_nanoseconds");
		metricConfigurationList.add("container_age_nanoseconds");
		metricConfigurationList.add("spike_end_seconds");
		metricConfigurationList.add("spike_start_seconds");
		metricConfigurationList.add("metric_registrar_orchestrator_goroutines_goroutines");
		metricConfigurationList.add("metric_registrar_orchestrator_log_worker_count_workers");
		metricConfigurationList.add("metric_registrar_orchestrator_registered_secure_metrics_endpoints_apps");
		metricConfigurationList.add("metric_registrar_orchestrator_secure-endpoint_worker_count_workers");
		metricConfigurationList.add("metric_registrar_orchestrator_registered_metrics_endpoints_apps");
		metricConfigurationList.add("metric_registrar_orchestrator_registered_structured_loggers_apps");
		metricConfigurationList.add("metric_registrar_orchestrator_endpoint_worker_count_workers");
		metricConfigurationList.add("GardenHealthCheckFailed_Metric");
		metricConfigurationList.add("RepBulkSyncDuration_nanos");
		metricConfigurationList.add("metric_registrar_log_worker_goroutines_goroutines");
		metricConfigurationList.add("metric_registrar_log_worker_registered_apps_apps");
		metricConfigurationList.add("TasksStarted_Metric");
		metricConfigurationList.add("TasksFailed_Metric");
		metricConfigurationList.add("TasksSucceeded_Metric");
		metricConfigurationList.add("TasksPending_Metric");
		metricConfigurationList.add("TasksRunning_Metric");
		metricConfigurationList.add("TasksCompleted_Metric");
		metricConfigurationList.add("TasksResolving_Metric");
		metricConfigurationList.add("ConvergenceTaskDuration_nanos");
		metricConfigurationList.add("ConvergenceLRPDuration_nanos");
		metricConfigurationList.add("Domain_cf-apps_Metric");
		metricConfigurationList.add("Domain_cf-tasks_Metric");
		metricConfigurationList.add("LRPsUnclaimed_Metric");
		metricConfigurationList.add("LRPsClaimed_Metric");
		metricConfigurationList.add("LRPsRunning_Metric");
		metricConfigurationList.add("CrashedActualLRPs_Metric");
		metricConfigurationList.add("LRPsMissing_Metric");
		metricConfigurationList.add("LRPsExtra_Metric");
		metricConfigurationList.add("SuspectRunningActualLRPs_Metric");
		metricConfigurationList.add("SuspectClaimedActualLRPs_Metric");
		metricConfigurationList.add("LRPsDesired_Metric");
		metricConfigurationList.add("CrashingDesiredLRPs_Metric");
		metricConfigurationList.add("PresentCells_Metric");
		metricConfigurationList.add("SuspectCells_Metric");
		metricConfigurationList.add("metric_registrar_endpoint_worker_goroutines_goroutines");
		metricConfigurationList.add("metric_registrar_endpoint_worker_registered_apps_apps");
		metricConfigurationList.add("StoreAllSuccessTime_ms");
		metricConfigurationList.add("totalPolicies_");
		metricConfigurationList.add("DBOpenConnections_");
		metricConfigurationList.add("DBQueriesTotal_");
		metricConfigurationList.add("DBQueriesSucceeded_");
		metricConfigurationList.add("DBQueriesFailed_");
		metricConfigurationList.add("DBQueriesInFlight_");
		metricConfigurationList.add("DBQueryDurationMax_seconds");
		metricConfigurationList.add("RoutesTotal_Metric");
		metricConfigurationList.add("totalLeases_");
		metricConfigurationList.add("freeLeases_");
		metricConfigurationList.add("staleLeases_");
		metricConfigurationList.add("doppler_proxy_firehoses_connections");
		metricConfigurationList.add("doppler_proxy_app_streams_connections");
		metricConfigurationList.add("doppler_proxy_recent_logs_latency_ms");
		metricConfigurationList.add("/mysql/system/persistent_disk_inodes_free_kb");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_used_kb");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_free_kb");
		metricConfigurationList.add("/mysql/system/persistent_disk_used_percent_percentage");
		metricConfigurationList.add("/mysql/system/persistent_disk_used_kb");
		metricConfigurationList.add("/mysql/system/persistent_disk_inodes_used_percent_percentage");
		metricConfigurationList.add("/mysql/system/persistent_disk_inodes_used_kb");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_inodes_free_kb");
		metricConfigurationList.add("/mysql/system/persistent_disk_free_kb");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_used_percent_percentage");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_inodes_used_percent_percentage");
		metricConfigurationList.add("/mysql/system/ephemeral_disk_inodes_used_kb");
		metricConfigurationList.add("/mysql/performance/cpu_utilization_percent_percentage");
		metricConfigurationList.add("/mysql/available_boolean");
		metricConfigurationList.add("/mysql/net/connections_connection");
		metricConfigurationList.add("/mysql/performance/com_replace_select_query");
		metricConfigurationList.add("/mysql/performance/table_locks_waited_number");
		metricConfigurationList.add("/mysql/performance/com_insert_select_query");
		metricConfigurationList.add("/mysql/performance/threads_running_thread");
		metricConfigurationList.add("/mysql/performance/opened_tables_integer");
		metricConfigurationList.add("/mysql/performance/opened_table_definitions_integer");
		metricConfigurationList.add("/mysql/performance/queries_metric");
		metricConfigurationList.add("/mysql/performance/com_delete_query");
		metricConfigurationList.add("/mysql/performance/created_tmp_disk_tables_table");
		metricConfigurationList.add("/mysql/performance/threads_connected_connection");
		metricConfigurationList.add("/mysql/innodb/buffer_pool_pages_total_page");
		metricConfigurationList.add("/mysql/innodb/row_lock_current_waits_lock");
		metricConfigurationList.add("/mysql/innodb/row_lock_waits_event");
		metricConfigurationList.add("/mysql/performance/created_tmp_files_file");
		metricConfigurationList.add("/mysql/performance/com_select_query");
		metricConfigurationList.add("/mysql/performance/open_files_file");
		metricConfigurationList.add("/mysql/performance/open_tables_integer");
		metricConfigurationList.add("/mysql/innodb/data_written_byte");
		metricConfigurationList.add("/mysql/innodb/os_log_fsyncs_event");
		metricConfigurationList.add("/mysql/performance/com_insert_query");
		metricConfigurationList.add("/mysql/performance/qcache_hits_hit");
		metricConfigurationList.add("/mysql/performance/slow_queries_query");
		metricConfigurationList.add("/mysql/performance/created_tmp_tables_table");
		metricConfigurationList.add("/mysql/performance/open_table_definitions_integer");
		metricConfigurationList.add("/mysql/performance/questions_metric");
		metricConfigurationList.add("/mysql/innodb/buffer_pool_pages_free_page");
		metricConfigurationList.add("/mysql/innodb/data_read_byte");
		metricConfigurationList.add("/mysql/performance/com_update_multi_query");
		metricConfigurationList.add("/mysql/innodb/row_lock_time_millisecond");
		metricConfigurationList.add("/mysql/performance/com_delete_multi_query");
		metricConfigurationList.add("/mysql/performance/com_update_query");
		metricConfigurationList.add("/mysql/performance/queries_delta_metric");
		metricConfigurationList.add("/mysql/innodb/buffer_pool_pages_data_page");
		metricConfigurationList.add("/mysql/net/max_used_connections_connection");
		metricConfigurationList.add("/mysql/variables/max_connections_integer");
		metricConfigurationList.add("/mysql/variables/read_only_boolean");
		metricConfigurationList.add("/mysql/variables/open_files_limit_integer");
		metricConfigurationList.add("/mysql/galera/wsrep_ready_number");
		metricConfigurationList.add("/mysql/galera/wsrep_cluster_status_number");
		metricConfigurationList.add("/mysql/galera/wsrep_flow_control_sent_number");
		metricConfigurationList.add("/mysql/galera/wsrep_flow_control_recv_number");
		metricConfigurationList.add("/mysql/galera/wsrep_local_send_queue_float");
		metricConfigurationList.add("/mysql/galera/wsrep_local_recv_queue_float");
		metricConfigurationList.add("/mysql/galera/wsrep_local_index_float");
		metricConfigurationList.add("/mysql/galera/wsrep_local_state_float");
		metricConfigurationList.add("/mysql/galera/wsrep_flow_control_paused_float");
		metricConfigurationList.add("/mysql/galera/wsrep_cluster_size_node");
		metricConfigurationList.add("total_routes_");
		metricConfigurationList.add("ms_since_last_registry_update_ms");
		metricConfigurationList.add("LeasesRenewRequestTime_ms");
		metricConfigurationList.add("LeasesIndexRequestTime_ms");
		metricConfigurationList.add("subscriptions_total");
		metricConfigurationList.add("system_disk_persistent_inode_percent_Percent");
		metricConfigurationList.add("system_disk_persistent_percent_Percent");
		metricConfigurationList.add("latency_MySQLProxy_ms");
		metricConfigurationList.add("diego_sync_invalid_desired_lrps_gauge");
		metricConfigurationList.add("diego_sync_duration_ms");
		metricConfigurationList.add("staging_requested_counter");
		metricConfigurationList.add("RequestLatency_nanos");
		metricConfigurationList.add("AuctioneerFetchStatesDuration_nanos");
		metricConfigurationList.add("GardenContainerCreationSucceededDuration_nanos");
		metricConfigurationList.add("CredCreationSucceededDuration_nanos");
		metricConfigurationList.add("ContainerSetupSucceededDuration_nanos");
		metricConfigurationList.add("HHTTPRouteCount_Metric");
		metricConfigurationList.add("TCPRouteCount_Metric");
		metricConfigurationList.add("RouteEmitterSyncDuration_nanos");
		metricConfigurationList.add("DBOpenConnections_Metric");
		metricConfigurationList.add("DBWaitDuration_nanos");
		metricConfigurationList.add("DBWaitCount_Metric");
		metricConfigurationList.add("DBQueriesTotal_Metric");
		metricConfigurationList.add("DBQueriesSucceeded_Metric");
		metricConfigurationList.add("DBQueriesFailed_Metric");
		metricConfigurationList.add("DBQueriesInFlight_Metric");
		metricConfigurationList.add("DBQueryDurationMax_nanos");
		metricConfigurationList.add("RequestsStarted_Metric");
		metricConfigurationList.add("RequestsSucceeded_Metric");
		metricConfigurationList.add("RequestsFailed_Metric");
		metricConfigurationList.add("RequestsInFlight_Metric");
		metricConfigurationList.add("RequestsCancelled_Metric");
		metricConfigurationList.add("RequestLatencyMax_nanos");
		metricConfigurationList.add("ActiveLocks_Metric");
		metricConfigurationList.add("LocksExpired_Metric");
		metricConfigurationList.add("PresenceExpired_Metric");
		metricConfigurationList.add("ActivePresences_Metric");
		metricConfigurationList.add("usage_service_delayed_job_failures_");
		metricConfigurationList.add("usage_service_app_usage_event_cc_lag_seconds_");
		metricConfigurationList.add("LockHeld_Metric");
		metricConfigurationList.add("OpenFileDescriptors_Metric");
		metricConfigurationList.add("CapacityTotalMemory_MiB");
		metricConfigurationList.add("CapacityTotalDisk_MiB");
		metricConfigurationList.add("CapacityTotalContainers_Metric");
		metricConfigurationList.add("CapacityRemainingMemory_MiB");
		metricConfigurationList.add("CapacityRemainingDisk_MiB");
		metricConfigurationList.add("CapacityRemainingContainers_Metric");
		metricConfigurationList.add("CapacityAllocatedMemory_MiB");
		metricConfigurationList.add("CapacityAllocatedDisk_MiB");
		metricConfigurationList.add("ContainerUsageMemory_MiB");
		metricConfigurationList.add("ContainerUsageDisk_MiB");
		metricConfigurationList.add("ContainerCount_Metric");
		metricConfigurationList.add("StartingContainerCount_Metric");
		metricConfigurationList.add("staging_failed_counter");
		metricConfigurationList.add("staging_failed_duration_ms");
		metricConfigurationList.add("GardenContainerDestructionSucceededDuration_nanos");

	}

	public static boolean istokenExpied(FirehoseProperties firehoseProperties, long timestamp) {
		if (System.currentTimeMillis() - timestamp > firehoseProperties.getHealthTimeCheck()) {
			return true;
		}
		return false;
	}

	public static String getOriginName(String pattern) {
		String origin = "";
		if (!pattern.contains("App_Name")) {
			// Use double backslashes to escape a single backslash in the regex
			String[] strArr = pattern.split("/");

			// Check if the split array has enough elements
			if (strArr.length >= 4) {
				origin = strArr[2].replaceAll("^\\.+|\\.+$", "");
				return origin;
			}
		}
		return null;
	}

	public static String generateKeyForQueue(String metricName, String origin) {

		if (origin == null || origin.isEmpty()) {
			return metricName;
		} else {
			return origin + metricName;
		}

	}

	public static String generateKeyForMetricFilter(String metricName, String origin, boolean isRestEnabled) {
		if (isRestEnabled) {

			if (origin == null || origin.isEmpty()) {
				return metricName;
			} else {
				return origin + metricName;
			}
		} else {
			return metricName;
		}
	}

	public static void removeMetricFilter(String subscriberId) {
		if (subscriberId != null && !subscriberId.isEmpty()) {
			MetricUtils.availableFilterList.remove(subscriberId);
		}

	}

}
