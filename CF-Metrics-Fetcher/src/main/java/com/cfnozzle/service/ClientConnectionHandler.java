package com.cfnozzle.service;

import org.springframework.stereotype.Service;

@Service
public interface ClientConnectionHandler {

	boolean isSubscriberisOn(String subscriberId);

	String disconnectClients(String subscriberId);

//	void disconnectAllClients1();

	void healthCheckUpdation(String subscriberId);

}
