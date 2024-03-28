package com.cfnozzle.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.springframework.stereotype.Service;

@Service
public interface MetricsReporter {

	Counter registerCounter(String name);

	Timer registerTimer(String name);

	Meter registerMeter(String name);

}
