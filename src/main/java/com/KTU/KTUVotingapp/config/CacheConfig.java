package com.KTU.KTUVotingapp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration optimized for high concurrency (1500+ users).
 * Uses Caffeine cache with optimized settings for performance.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("candidates", "results");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Builds Caffeine cache with optimized settings for high concurrency:
     * - Maximum size: 5000 entries (increased for high traffic)
     * - Expire after write: 2 minutes (faster refresh)
     * - Expire after access: 1 minute (evicts unused entries)
     * - Records statistics for monitoring
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .initialCapacity(100);
    }
}

