package com.cfnozzle.utils;

public class Constants {
	public static final String FIREHOSE_NOZZLE = "cfnozzle-firehose-nozzle";
	public static final String PCF_PREFIX = "pcf";
	public static final String APP_METRICS_PREFIX = "app";
	public static final String CAFFEINE_PREFIX = "caffeine";

	public static final String METRICS_NAME_SEP = ".";

	public static final String TOTAL_SUFFIX = "total";
	public static final String DELTA_SUFFIX = "delta";

	public static final String CONTAINER_PREFIX = "container";
	public static final String CPU_PERCENTAGE_SUFFIX = "cpu_percentage";
	public static final String DISK_BYTES_SUFFIX = "disk_bytes";
	public static final String DISK_BYTES_QUOTA_SUFFIX = "disk_bytes_quota";
	public static final String MEMORY_BYTES_SUFFIX = "memory_bytes";
	public static final String MEMORY_BYTES_QUOTA_SUFFIX = "memory_bytes_quota";
	public static final String LOG_MESSAGE_SUFFIX = "log_message";

	public static final String APPLICATION_NAME = "applicationName";
	public static final String ORG = "org";
	public static final String SPACE = "space";
	public static final String APPLICATION_ID = "applicationId";
	public static final String INSTANCE_INDEX = "instanceIndex";
	public static final String DEPLOYMENT = "deployment";
	public static final String JOB = "job";
	public static final String CUSTOM_TAG_PREFIX = "customTag.";
	public static final String TAG_NAME_UDP_FORMAT = "pcf";
	public static final String DEFAULT_DATA_TYPE = "gauge";

	public static final String APP_NAME_CONSTANT = "app_name";
	public static final String SPACE_NAME_CONSTANT = "space_name";
	public static final String ORGS_NAME_CONSTANT = "organization_name";

	public static final String ORIGIN = "origin";
	public static final String IP = "ip";
	public static final String INDEX = "index";

	public static final String CONTAINER_TAGS_SEQUENCE = "process_id,product,org,origin,ip,index,organization_name,space,system_domain,app_name,instance_id,space_name,process_instance_id,organization_id,process_type,source_id,job,applicationId,instanceIndex,app_id,space_id,applicationName,deployment";
	public static final String VALUE_TAGS_SEQUENCE = "product,origin,ip,index,source_id,job,system_domain,deployment";
	public static final String EVENTS_TAGS_SEQUENCE = "product,origin,ip,index,source_id,job,system_domain,deployment";

}
