package com.dinhchieu.ewallet.common_library.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class RedisConfig {

  /**
   * This method configures the Redis cache to use JSON serialization for values
   * and disables caching of null values. It returns a RedisCacheConfiguration
   * object that can be used by the RedisCacheManager to manage cache settings.
   */
  @SuppressWarnings("removal")
  @Bean
  public RedisCacheConfiguration redisCacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();
  }

  /**
   * This method creates a RedisTemplate bean that is used to interact with Redis.
   * It sets the connection factory, key serializer, and value serializer for the
   * template. The key serializer is set to use the default string serializer,
   * while the value serializer is set to use the
   * GenericJackson2JsonRedisSerializer, which allows for JSON serialization of
   * values stored in Redis. The configured RedisTemplate is returned for use in
   * the application.
   * 
   * @param redisConnectionFactory the RedisConnectionFactory to be used by the
   *                               RedisTemplate
   * @return a configured RedisTemplate for interacting with Redis
   */
  @SuppressWarnings("removal")
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(template.getStringSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }
}
