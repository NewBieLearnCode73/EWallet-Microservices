package com.dinhchieu.ewallet.common_library.config;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class SharedResilienceConfig {
  @Bean
  public CircuitBreakerConfig sharedCircuitBreakerConfig() {
    return CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(20)
        .failureRateThreshold(50.0f)
        .waitDurationInOpenState(Duration.ofSeconds(20))
        .permittedNumberOfCallsInHalfOpenState(3)
        .slowCallRateThreshold(100.0f)
        .slowCallDurationThreshold(Duration.ofSeconds(2))
        .recordExceptions(IOException.class, TimeoutException.class,
            java.net.ConnectException.class)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
    return CircuitBreakerRegistry.of(defaultCircuitBreakerConfig);
  }

  @Bean
  public RetryConfig sharedRetryConfig() {
    return RetryConfig.custom()
        .maxAttempts(2)
        .waitDuration(Duration.ofMillis(500))
        .retryExceptions(IOException.class, TimeoutException.class)
        .build();
  }

  @Bean
  public RetryRegistry retryRegistry(RetryConfig defaultRetryConfig) {
    return RetryRegistry.of(defaultRetryConfig);
  }
}
