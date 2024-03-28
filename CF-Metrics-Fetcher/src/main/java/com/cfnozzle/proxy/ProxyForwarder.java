package com.cfnozzle.proxy;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cfnozzle.model.AppEnvelope;

@Component
public interface ProxyForwarder {
	void forward(AppEnvelope envelope);
	
	 void forwardAll(Collection<AppEnvelope> appEnvelopeList);

}
