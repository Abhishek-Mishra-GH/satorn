package com._xdev.satorn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified cache configuration using Redis for SATORN application
 * Implements minimal but essential caching for performance optimization
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfiguration {

        /**
         * Configure Redis cache manager with essential cache types
         */
        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                log.info("Initializing Redis Cache Manager");

                // Default cache configuration - 10 minutes TTL
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                .entryTtl(Duration.ofMinutes(10));

                // Configure specific caches with different TTLs
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Article caches with appropriate TTLs
                cacheConfigurations.put("synthesized-articles",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(15)));

                cacheConfigurations.put("trending-articles",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(5)));

                cacheConfigurations.put("top-credible-articles",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(10)));

                cacheConfigurations.put("article-detail",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(20)));

                cacheConfigurations.put("article-search",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(5)));

                cacheConfigurations.put("category-articles",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(10)));

                cacheConfigurations.put("article-statistics",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(5)));

                // RSS feed caches
                cacheConfigurations.put("rss-feeds",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(30)));

                cacheConfigurations.put("rss-feed-detail",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(30)));

                cacheConfigurations.put("rss-statistics",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(10)));

                // Rate limiter cache (frequently checked)
                cacheConfigurations.put("rate-limiter-status",
                                RedisCacheConfiguration.defaultCacheConfig()
                                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                                .entryTtl(Duration.ofMinutes(1)));

                log.info("Configured {} cache types with specific TTLs", cacheConfigurations.size());

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }

        /**
         * Keep the API functional even if Redis cache has stale/invalid payloads.
         */
        @Bean
        public CacheErrorHandler cacheErrorHandler() {
                return new CacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache,
                                        Object key) {
                                log.warn("Ignoring cache GET error for cache '{}' and key '{}': {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache,
                                        Object key, Object value) {
                                log.warn("Ignoring cache PUT error for cache '{}' and key '{}': {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache,
                                        Object key) {
                                log.warn("Ignoring cache EVICT error for cache '{}' and key '{}': {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                                log.warn("Ignoring cache CLEAR error for cache '{}': {}",
                                                cache.getName(), exception.getMessage());
                        }
                };
        }
}
