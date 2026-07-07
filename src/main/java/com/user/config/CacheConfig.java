package com.user.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration.
 *
 * <p>All TTL / max-size values are read from {@code application.properties}:
 * <pre>
 *   cache.categories.ttl-seconds=300
 *   cache.categories.max-size=500
 *   cache.search.ttl-seconds=60
 *   cache.search.max-size=200
 * </pre>
 */
@Configuration
public class CacheConfig {

	private static final Logger logger = LogManager.getLogger(CacheConfig.class);

	// ── categories cache ─────────────────────────────────────────────────────────
	@Value("${cache.categories.ttl-seconds:300}")
	private long categoriesTtlSeconds;

	@Value("${cache.categories.max-size:500}")
	private long categoriesMaxSize;

	// ── product search cache ─────────────────────────────────────────────────────
	@Value("${cache.search.ttl-seconds:60}")
	private long searchTtlSeconds;

	@Value("${cache.search.max-size:200}")
	private long searchMaxSize;

	@Bean
	public CacheManager cacheManager() {
		logger.info("Initialising caches — categories: ttl={}s maxSize={} | productSearch: ttl={}s maxSize={}",
				categoriesTtlSeconds, categoriesMaxSize, searchTtlSeconds, searchMaxSize);

		CaffeineCacheManager manager = new CaffeineCacheManager();

		// Each cache gets its own Caffeine spec via registerCustomCache
		manager.registerCustomCache("categories",
				Caffeine.newBuilder()
						.maximumSize(categoriesMaxSize)
						.expireAfterWrite(categoriesTtlSeconds, TimeUnit.SECONDS)
						.build());

		manager.registerCustomCache("productSearch",
				Caffeine.newBuilder()
						.maximumSize(searchMaxSize)
						.expireAfterWrite(searchTtlSeconds, TimeUnit.SECONDS)
						.build());

		return manager;
	}

}
