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

  @Value("${keycloak.admin-client.server-url}")
  private String keycloakServerUrl;

  @Value("${token.expiration.access-token}")
  private int accessTokenExpiration;

  @Value("${token.expiration.refresh-token}")
  private int refreshTokenExpiration;

  @Override
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
        log.warn("Email already exists: {}", email);
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }

      if (response.getStatus() != 201) {
        log.error("Failed to register user: HTTP {}", response.getStatus());
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      log.error("Exception during user registration: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

  }

  @Override
  public void login(String username, String password, HttpServletResponse httpServletResponse) {

    try {
      KeyCloakLoginRequestDto params = KeyCloakLoginRequestDto.builder()
          .client_id(keycloakClientId)
          .client_secret(keycloakClientSecret)
          .username(username)
          .password(password)
          .build();

      Map<String, Object> keycloakResponse = keyCloakClient.login(
          keycloakRealm,
          params);

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
        log.warn("Keycloak login từ chối (HTTP {}): {}", e.status(), username);
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      }
      log.error("Keycloak login lỗi server (HTTP {}): {}", e.status(), e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    } catch (Exception e) {
      log.error("Lỗi không xác định khi login: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  @Override
  public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

    Cookie refreshTokenCookie = WebUtils.getCookie(httpServletRequest, "refresh_token");
    Cookie accessTokenCookie = WebUtils.getCookie(httpServletRequest, "access_token");

    if (accessTokenCookie != null && !accessTokenCookie.getValue().isEmpty()) {
      try {
        blacklistService.blacklistToken(jwtDecoder.decode(accessTokenCookie.getValue()).getId(),
            accessTokenExpiration);
      } catch (Exception e) {
        log.warn("Không thể blacklist access token khi logout: {}", e.getMessage());
      }
    }

    if (refreshTokenCookie == null || refreshTokenCookie.getValue().isEmpty()) {
      CookieUtils.clearAuthCookies(httpServletResponse);
      return;
    }

    try {
      KeyCloakLogoutRequestDto params = KeyCloakLogoutRequestDto.builder()
          .client_id(keycloakClientId)
          .client_secret(keycloakClientSecret)
          .refresh_token(refreshTokenCookie.getValue())
          .build();

      keyCloakClient.logout(keycloakRealm, params);

    } catch (FeignException e) {
      log.error("Lỗi từ Keycloak khi logout (Token có thể đã chết): HTTP {}", e.status());
    } catch (Exception e) {
      log.error("Lỗi không xác định khi gọi Keycloak: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    } finally {
      CookieUtils.clearAuthCookies(httpServletResponse);
    }
  }

  @Override
  public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
    Cookie requestRefreshTokenCookie = WebUtils.getCookie(request, "refresh_token");

    if (requestRefreshTokenCookie == null || requestRefreshTokenCookie.getValue().isEmpty()) {
      throw new AppException(ErrorCode.MISSING_AUTHENTICATION);
    }

    String refreshToken = requestRefreshTokenCookie.getValue();

    try {

      KeyCloakRefreshTokenRequestDto params = KeyCloakRefreshTokenRequestDto.builder()
          .client_id(keycloakClientId)
          .client_secret(keycloakClientSecret)
          .refresh_token(refreshToken)
          .build();

      Map<String, Object> keycloakResponse = keyCloakClient.refreshToken(
          keycloakRealm,
          params);

      if (keycloakResponse != null) {
        String newAccessToken = (String) keycloakResponse.get("access_token");
        String newRefreshToken = (String) keycloakResponse.get("refresh_token");

        Cookie accessTokenCookie = CookieUtils.setCookie("access_token", newAccessToken, accessTokenExpiration, true,
            "/");
        Cookie refreshTokenCookie = CookieUtils.setCookie("refresh_token", newRefreshToken, refreshTokenExpiration,
            true, "/");

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
      }

    } catch (FeignException e) {
      if (e.status() >= 400 && e.status() < 500) {
        log.warn("Keycloak refresh token từ chối (HTTP {})", e.status());
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      }
      log.error("Lỗi từ Keycloak khi refresh token: HTTP {}", e.status());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    } catch (Exception e) {
      log.error("Lỗi không xác định khi gọi Keycloak: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  @Override
  public void revokeAllSessions(HttpServletRequest request) {
    Cookie accessTokenCookie = WebUtils.getCookie(request, "access_token");
    if (accessTokenCookie == null || accessTokenCookie.getValue().isEmpty())
      throw new AppException(ErrorCode.MISSING_AUTHENTICATION);

    try {
      Jwt accessToken = jwtDecoder.decode(accessTokenCookie.getValue());
      String userId = accessToken.getSubject();

      keycloak.realm(keycloakRealm).users().get(userId).logout();

      blacklistService.blacklistToken(accessToken.getId(), accessTokenExpiration);

      log.info("Đã thu hồi tất cả phiên cho user ID: {}", userId);

    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      log.error("Lỗi khi thu hồi tất cả phiên: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

}
