package com.dinhchieu.ewallet.auth_service.services.Impl;

import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import com.dinhchieu.ewallet.auth_service.clients.KeyCloakClient;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLoginRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLogoutRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakRefreshTokenRequestDto;
import com.dinhchieu.ewallet.auth_service.services.AuthService;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.CookieUtils;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final Keycloak keycloak;
  private final TokenBlacklistService blacklistService;
  private final JwtDecoder jwtDecoder;
  private final KeyCloakClient keyCloakClient;

  @Value("${keycloak.admin-client.realm}")
  private String keycloakRealm;
  @Value("${keycloak.admin-client.client-id}")
  private String keycloakClientId;
  @Value("${keycloak.admin-client.client-secret}")
  private String keycloakClientSecret;
  @Value("${token.expiration.access-token}")
  private int accessTokenExpiration;
  @Value("${token.expiration.refresh-token}")
  private int refreshTokenExpiration;

  @Override
  @Retry(name = "authServiceRetry", fallbackMethod = "authFallback")
  @CircuitBreaker(name = "authServiceCircuitBreaker")
  public void register(String username, String password, String email) {
    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(email);
    user.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    user.setCredentials(java.util.Collections.singletonList(credential));

    try (Response response = keycloak.realm(keycloakRealm).users().create(user)) {
      if (response.getStatus() == 409) {
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }
      if (response.getStatus() != 201) {
        // Throw for Resilience to catch and trigger fallback
        throw new RuntimeException("Keycloak creation failed with status: " + response.getStatus());
      }
    }
  }

  @Override
  @Retry(name = "authServiceRetry", fallbackMethod = "authFallback")
  @CircuitBreaker(name = "authServiceCircuitBreaker")
  public void login(String username, String password, HttpServletResponse httpServletResponse) {
    KeyCloakLoginRequestDto params = KeyCloakLoginRequestDto.builder()
        .client_id(keycloakClientId)
        .client_secret(keycloakClientSecret)
        .username(username)
        .password(password)
        .build();

    try {
      Map<String, Object> keycloakResponse = keyCloakClient.login(keycloakRealm, params);

      if (keycloakResponse != null) {
        String accessToken = (String) keycloakResponse.get("access_token");
        String refreshToken = (String) keycloakResponse.get("refresh_token");

        Cookie accessTokenCookie = CookieUtils.setCookie("access_token", accessToken, accessTokenExpiration, true, "/");
        Cookie refreshTokenCookie = CookieUtils.setCookie("refresh_token", refreshToken, refreshTokenExpiration, true,
            "/");

        httpServletResponse.addCookie(accessTokenCookie);
        httpServletResponse.addCookie(refreshTokenCookie);
      }
    } catch (FeignException e) {
      if (e.status() >= 400 && e.status() < 500) {
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      }
      throw e;
    }
  }

  @Override
  @Retry(name = "authServiceRetry", fallbackMethod = "authFallback")
  @CircuitBreaker(name = "authServiceCircuitBreaker")
  public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
    Cookie refreshTokenCookie = WebUtils.getCookie(httpServletRequest, "refresh_token");
    Cookie accessTokenCookie = WebUtils.getCookie(httpServletRequest, "access_token");

    if (accessTokenCookie != null && !accessTokenCookie.getValue().isEmpty()) {
      try {
        blacklistService.blacklistToken(jwtDecoder.decode(accessTokenCookie.getValue()).getId(), accessTokenExpiration);
      } catch (Exception e) {
        log.warn("Blacklist local failed: {}", e.getMessage());
      }
    }

    if (refreshTokenCookie != null && !refreshTokenCookie.getValue().isEmpty()) {
      KeyCloakLogoutRequestDto params = KeyCloakLogoutRequestDto.builder()
          .client_id(keycloakClientId)
          .client_secret(keycloakClientSecret)
          .refresh_token(refreshTokenCookie.getValue())
          .build();

      keyCloakClient.logout(keycloakRealm, params);
    }

    CookieUtils.clearAuthCookies(httpServletResponse);
  }

  @Override
  @Retry(name = "authServiceRetry", fallbackMethod = "authFallback")
  @CircuitBreaker(name = "authServiceCircuitBreaker")
  public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
    Cookie requestRefreshTokenCookie = WebUtils.getCookie(request, "refresh_token");
    if (requestRefreshTokenCookie == null)
      throw new AppException(ErrorCode.MISSING_AUTHENTICATION);

    KeyCloakRefreshTokenRequestDto params = KeyCloakRefreshTokenRequestDto.builder()
        .client_id(keycloakClientId)
        .client_secret(keycloakClientSecret)
        .refresh_token(requestRefreshTokenCookie.getValue())
        .build();

    try {
      Map<String, Object> keycloakResponse = keyCloakClient.refreshToken(keycloakRealm, params);
      if (keycloakResponse != null) {
        String newAccessToken = (String) keycloakResponse.get("access_token");
        String newRefreshToken = (String) keycloakResponse.get("refresh_token");
        response.addCookie(CookieUtils.setCookie("access_token", newAccessToken, accessTokenExpiration, true, "/"));
        response.addCookie(CookieUtils.setCookie("refresh_token", newRefreshToken, refreshTokenExpiration, true, "/"));
      }
    } catch (FeignException e) {
      if (e.status() >= 400 && e.status() < 500)
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      throw e;
    }
  }

  @Override
  @Retry(name = "authServiceRetry", fallbackMethod = "authFallback")
  @CircuitBreaker(name = "authServiceCircuitBreaker")
  public void revokeAllSessions(HttpServletRequest request) {
    Cookie accessTokenCookie = WebUtils.getCookie(request, "access_token");
    if (accessTokenCookie == null)
      throw new AppException(ErrorCode.MISSING_AUTHENTICATION);

    Jwt accessToken = jwtDecoder.decode(accessTokenCookie.getValue());
    keycloak.realm(keycloakRealm).users().get(accessToken.getSubject()).logout();
    blacklistService.blacklistToken(accessToken.getId(), accessTokenExpiration);
  }

  public void authFallback(Throwable t) {
    log.error("RESILIENCE KÍCH HOẠT - Lỗi: {}", t.getMessage());
    throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
  }
}
