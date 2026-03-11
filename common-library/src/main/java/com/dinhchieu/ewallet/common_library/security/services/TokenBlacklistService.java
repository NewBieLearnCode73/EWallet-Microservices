package com.dinhchieu.ewallet.common_library.security.services;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
@AllArgsConstructor
@Data
public class TokenBlacklistService {

  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * This method adds a token's JTI (JWT ID) to the Redis cache with a specified
   * time
   * to live (TTL). The JTI is used as the key in Redis, and the value can be a
   * simple
   * string (e.g., "invalid") to indicate that the token is blacklisted. The TTL
   * ensures
   * that the blacklisted token will automatically expire from Redis after the
   * specified
   * duration, which helps to manage the size of the blacklist and ensures that
   * tokens are
   * not blacklisted indefinitely. This method is typically called when a user
   * logs out or when a token is revoked, allowing the application to prevent the
   * use of that token for authentication or authorization in the future.
   * 
   * @param jti the JWT ID of the token to be blacklisted
   * @param TTL the time to live (in seconds) for the blacklisted token in Redis
   */
  public void blacklistToken(String jti, long TTL) {
    redisTemplate.opsForValue().set(jti, "invalid", TTL, TimeUnit.SECONDS);
  }

  /**
   * This method checks if a token is blacklisted by looking up its JTI (JWT ID)
   * in Redis. It returns true if the JTI exists in Redis, indicating that the
   * token is blacklisted, and false otherwise. This allows the application to
   * quickly determine if a token has been blacklisted and should be rejected
   * during authentication or authorization processes.
   * 
   * @param jti the JWT ID of the token to check for blacklisting
   * @return true if the token is blacklisted, false otherwise
   */
  public boolean isBlacklisted(String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(jti));
  }
}
