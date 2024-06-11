package com.cfnozzle.service;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.util.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cfnozzle.model.AppInfo;
import com.cfnozzle.props.AppInfoProperties;
import com.cfnozzle.utils.MetricsRecorderStatsCounter;
import com.codahale.metrics.Counter;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Component
public class CachedAppInfoFetcher implements AppInfoFetcher {
	private static final Logger logger = Logger.getLogger(CachedAppInfoFetcher.class.getCanonicalName());

	/**
	 * How long to wait to receive AppInfo from PCF
	 */
	private static final int PCF_FETCH_TIMEOUT_SECONDS = 120;
	private final AsyncLoadingCache<String, Optional<AppInfo>> cache;

	@Autowired
	private CloudFoundryClient cfClient;

	private final Counter numFetchAppInfo;
	private final Counter numFetchAppInfoError;

	public CachedAppInfoFetcher(MetricsReporter metricsReporter, AppInfoProperties appInfoProperties) {

		// Create a cache for storing AppInfo with expiration and maximum size
		cache = Caffeine.newBuilder().expireAfterWrite(appInfoProperties.getCacheExpireIntervalHours(), TimeUnit.HOURS)
				.maximumSize(appInfoProperties.getAppInfoCacheSize())
				.recordStats(() -> new MetricsRecorderStatsCounter(metricsReporter))
				.buildAsync((key, executor) -> fetchFromPcf(key).toFuture());
		// Register counters for metrics reporting
		numFetchAppInfo = metricsReporter.registerCounter("fetch-app-info-from-pcf");
		numFetchAppInfoError = metricsReporter.registerCounter("fetch-app-info-error");
	}

	@Override
	public Mono<Optional<AppInfo>> fetch(String applicationId) {
		// Retrieve AppInfo from the cache, or fetch it if not available
		return Mono.fromFuture(cache.get(applicationId));
	}

	private Mono<Optional<AppInfo>> fetchFromPcf(String applicationId) {
		numFetchAppInfo.inc();
		// Fetch application details from PCF
		return getApplication(applicationId)
				.flatMap(app -> getSpace(app.getSpaceId()).map(space -> Tuples.of(space, app)))
				.flatMap(function((space, app) -> getOrganization(space.getOrganizationId())
						.map(org -> Optional.of(new AppInfo(app.getName(), org.getName(), space.getName())))))
				.timeout(Duration.ofSeconds(PCF_FETCH_TIMEOUT_SECONDS)).onErrorResume(t -> {
					numFetchAppInfoError.inc();
					logger.log(Level.WARNING, "Unable to fetch app details for applicationId: " + applicationId, t);

					return Mono.just(Optional.empty());
				});
	}

	private Mono<ApplicationEntity> getApplication(String applicationId) {
		// Retrieve the application entity from PCF
		return cfClient.applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build())
				.map(ResourceUtils::getEntity);
	}

	private Mono<SpaceEntity> getSpace(String spaceId) {
		// Retrieve the space entity from PCF
		return cfClient.spaces().get(GetSpaceRequest.builder().spaceId(spaceId).build()).map(ResourceUtils::getEntity);
	}

	private Mono<OrganizationEntity> getOrganization(String orgId) {
		// Retrieve the organization entity from PCF
		return cfClient.organizations().get(GetOrganizationRequest.builder().organizationId(orgId).build())
				.map(ResourceUtils::getEntity);
	}

}
