package com.dinhchieu.ewallet.transaction_service.config;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FeignConfig {

  @Bean
  public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String tokenValue = jwt.getTokenValue();

        if (tokenValue != null && !tokenValue.isEmpty()) {

          if (isTokenExpired(jwt)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
          }

          log.debug("Adding access token to Feign request");
          requestTemplate.header("Cookie", "access_token=" + tokenValue);
        } else {
          log.warn("Token value is empty or null");
          throw new AppException(ErrorCode.TOKEN_INVALID);
        }
      }
    };
  }

  /**
   * Only consider the token expired if it's actually expired or will expire
   * within the next 15 seconds.
   */
  private boolean isTokenExpired(Jwt jwt) {
    if (jwt.getExpiresAt() == null) {
      return true;
    }
    return jwt.getExpiresAt().isBefore(Instant.now().plusSeconds(10));
  }
}