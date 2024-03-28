package com.cfnozzle.mock;

import org.springframework.stereotype.Service;

import com.cfnozzle.model.Event;

@Service
public interface MockConnector {
	  void mock(Event event);
	  String disconnectMockClients(String subscriberId);
	  void disconnectAllMockClients1();
	  boolean isSubscriberisOnMock(String subscriberId);
	  void healthCheckMockUpdate(String subscriberId);
}
