package com.cfnozzle.service;

import org.springframework.stereotype.Service;

import com.cfnozzle.model.Event;

@Service
public interface ReadFromMapStream {

	void readFromQueue(Event event);

}
