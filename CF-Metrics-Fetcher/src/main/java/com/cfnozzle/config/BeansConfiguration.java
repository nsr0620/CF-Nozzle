package com.cfnozzle.config;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cfnozzle.mock.MockConnector;
import com.cfnozzle.mock.ReadFromMapMockStream;
import com.cfnozzle.model.Event;
import com.cfnozzle.props.AppInfoProperties;
import com.cfnozzle.props.FirehoseProperties;
import com.cfnozzle.props.LogStorageProperties;
import com.cfnozzle.props.PcfProperties;
import com.cfnozzle.props.ProxyProperties;
import com.cfnozzle.service.FirehoseToProxyConnector;
import com.cfnozzle.service.ReadFromMapStream;

@Configuration
@EnableConfigurationProperties({ PcfProperties.class, ProxyProperties.class, FirehoseProperties.class,
		AppInfoProperties.class, LogStorageProperties.class })
public class BeansConfiguration {

	private static final String NETTY_MAX_CONNECTIONS_PROP = "reactor.ipc.netty.pool.maxConnections";

	@Autowired
	private FirehoseToProxyConnector firehoseTocfnozzleProxyConnector;

	@Autowired
	private ReadFromMapMockStream readFromMapMockStream;

	@Autowired
	private ReadFromMapStream readFromMapStream;

	@Autowired
	private MockConnector firehoseToMockConnector;

	@Autowired
	PcfProperties pcf;

	@Autowired
	FirehoseProperties firehoseProperties;

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {

			if (!firehoseProperties.isEnbaleRestAPI()) {
				if (pcf.isMocking()) {
					// Used for simulation, when the application is in mocking mode
					firehoseToMockConnector.mock(new Event());
					if (firehoseProperties.isEnableQueue())
						readFromMapMockStream.readfromMockQueue(new Event());
				} else {
					// Connect to the cfnozzle Proxy for handling real Firehose events
					firehoseTocfnozzleProxyConnector.connect(new Event());
					if (firehoseProperties.isEnableQueue())
						readFromMapStream.readFromQueue(new Event());
				}
			}
		};
	}

	@Bean
	public DefaultConnectionContext connectionContext(PcfProperties pcfProperties) {
		// Set property if not already set
		System.setProperty(NETTY_MAX_CONNECTIONS_PROP, System.getProperty(NETTY_MAX_CONNECTIONS_PROP, "32"));
		System.setProperty("reactor.netty.http.server.accessLogEnabled",
				System.getProperty("reactor.netty.http.server.accessLogEnabled", "true"));
		// Create a connection context with Cloud Foundry API host and SSL validation
		// settings
		return DefaultConnectionContext.builder().apiHost(pcfProperties.getPcfhost())
				.skipSslValidation(pcfProperties.isSkipSslValidation()).build();
	}

	@Bean
	public PasswordGrantTokenProvider tokenProvider(PcfProperties pcfProperties) {
		// Create a token provider for authentication with Cloud Foundry using username
		// and password
		return PasswordGrantTokenProvider.builder().username(pcfProperties.getPcfuser())
				.password(pcfProperties.getPcfpassword()).build();
	}

	@Bean
	public ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		// Create a Cloud Foundry client for interacting with the Cloud Foundry API
		return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	public ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		// Create a Doppler client for handling Firehose events
		return ReactorDopplerClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
	}

}
