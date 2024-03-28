package com.cfnozzle.mock;

import org.springframework.stereotype.Service;

import com.cfnozzle.model.Event;

@Service
public interface ReadFromMapMockStream {

	void readfromMockQueue(Event event);

}
