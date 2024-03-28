package com.cfnozzle.utils;

import java.util.HashMap;
import java.util.Map;

public class Datatypes {

	public static Map<String, String> dataTypesMapping = new HashMap<String, String>();

	static {
		dataTypesMapping.put("CONTAINER_METRIC", "gauge");
		dataTypesMapping.put("COUNTER_EVENT", "gauge");
		dataTypesMapping.put("ERROR", "gauge");
		dataTypesMapping.put("HTTP_START_STOP", "timeseries");
		dataTypesMapping.put("LOG_MESSAGE", "log");
		dataTypesMapping.put("VALUE_METRIC", "gauge");
	}

}
