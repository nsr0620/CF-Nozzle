package com.cfnozzle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;



@Component
public class ServerPortCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
 
	
	@Value("${server_port}")
	private int server_port;
	
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(server_port);
    }
}
