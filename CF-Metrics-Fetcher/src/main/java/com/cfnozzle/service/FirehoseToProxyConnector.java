package com.cfnozzle.service;

import org.springframework.stereotype.Service;

import com.cfnozzle.model.Event;

@Service
public interface FirehoseToProxyConnector {

	void connect(Event event);

}
