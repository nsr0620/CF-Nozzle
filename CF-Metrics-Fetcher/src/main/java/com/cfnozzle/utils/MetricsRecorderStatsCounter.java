package com.cfnozzle.utils;

import static com.cfnozzle.utils.Constants.CAFFEINE_PREFIX;
import static com.cfnozzle.utils.Constants.METRICS_NAME_SEP;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.cfnozzle.service.MetricsReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;

public class MetricsRecorderStatsCounter implements StatsCounter {
	private final Meter hitCount;
	private final Meter missCount;
	private final Meter loadSuccessCount;
	private final Meter loadFailureCount;
	private final Timer totalLoadTime;
	private final Meter evictionCount;
	private final Meter evictionWeight;

	public MetricsRecorderStatsCounter(MetricsReporter metricsReporter) {
		final String metricsPrefix = CAFFEINE_PREFIX + METRICS_NAME_SEP;
		hitCount = metricsReporter.registerMeter(metricsPrefix + "hits");
		missCount = metricsReporter.registerMeter(metricsPrefix + "misses");
		totalLoadTime = metricsReporter.registerTimer(metricsPrefix + "loads");
		loadSuccessCount = metricsReporter.registerMeter(metricsPrefix + "loads-success");
		loadFailureCount = metricsReporter.registerMeter(metricsPrefix + "loads-failure");
		evictionCount = metricsReporter.registerMeter(metricsPrefix + "evictions");
		evictionWeight = metricsReporter.registerMeter(metricsPrefix + "evictions-weight");
	}

	@Override
	public void recordHits(int count) {
		hitCount.mark(count);
	}

	@Override
	public void recordMisses(int count) {
		missCount.mark(count);
	}

	@Override
	public void recordLoadSuccess(long loadTime) {
		loadSuccessCount.mark();
		totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
	}

	@Override
	public void recordLoadFailure(long loadTime) {
		loadFailureCount.mark();
		totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
	}

	@Override
	public void recordEviction() {
		// This method is scheduled for removal in version 3.0 in favor of
		// recordEviction(weight)
		recordEviction(1);
	}

	@Override
	public void recordEviction(int weight) {
		evictionCount.mark();
		evictionWeight.mark(weight);
	}

	@Nonnull
	@Override
	public CacheStats snapshot() {
		return new CacheStats(hitCount.getCount(), missCount.getCount(), loadSuccessCount.getCount(),
				loadFailureCount.getCount(), totalLoadTime.getCount(), evictionCount.getCount(),
				evictionWeight.getCount());
	}

	@Override
	public String toString() {
		return snapshot().toString();
	}
}
