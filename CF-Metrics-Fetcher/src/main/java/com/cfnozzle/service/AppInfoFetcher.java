package com.cfnozzle.service;

import org.springframework.stereotype.Service;
import com.cfnozzle.model.AppInfo;
import java.util.Optional;
import reactor.core.publisher.Mono;

@Service
public interface AppInfoFetcher {
	Mono<Optional<AppInfo>> fetch(String applicationId);
}
