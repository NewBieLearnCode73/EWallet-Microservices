package com.dinhchieu.ewallet.auth_service.services.Impl;

import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.WebUtils;

import com.dinhchieu.ewallet.auth_service.services.AuthService;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.CookieUtils;

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
  private final RestTemplate restTemplate;
  private final TokenBlacklistService blacklistService;
  private final JwtDecoder jwtDecoder;

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
      if (response.getStatus() != 201) {
        log.error("Failed to register user: HTTP {}", response.getStatus());
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }

      if (response.getStatus() == 409) {
        log.warn("Email already exists: {}", email);
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }
    } catch (Exception e) {
      log.error("Exception during user registration: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

  }

  @Override
  public void login(String username, String password, HttpServletResponse httpServletResponse) {

    try {

      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("grant_type", "password");
      formData.add("client_id", keycloakClientId);
      formData.add("client_secret", keycloakClientSecret);
      formData.add("username", username);
      formData.add("password", password);

      Map<String, Object> keycloakResponse = callKeyMapcloakEndpoint("/token", formData);

      if (keycloakResponse != null) {
        String accessToken = (String) keycloakResponse.get("access_token");
        String refreshToken = (String) keycloakResponse.get("refresh_token");

        Cookie accessTokenCookie = CookieUtils.setCookie("access_token", accessToken, accessTokenExpiration, true, "/");
        Cookie refreshTokenCookie = CookieUtils.setCookie("refresh_token", refreshToken, refreshTokenExpiration, true,
            "/");

        httpServletResponse.addCookie(accessTokenCookie);
        httpServletResponse.addCookie(refreshTokenCookie);
      }

    } catch (HttpClientErrorException e) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    } catch (Exception e) {
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

      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("client_id", keycloakClientId);
      formData.add("client_secret", keycloakClientSecret);
      formData.add("refresh_token", refreshTokenCookie.getValue());

      callKeyMapcloakEndpoint("/logout", formData);

    } catch (HttpClientErrorException e) {
      log.error("Lỗi từ Keycloak khi logout (Token có thể đã chết): {}", e.getStatusCode());
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

      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("grant_type", "refresh_token");
      formData.add("client_id", keycloakClientId);
      formData.add("client_secret", keycloakClientSecret);
      formData.add("refresh_token", refreshToken);

      Map<String, Object> keycloakResponse = callKeyMapcloakEndpoint("/token", formData);

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

    } catch (HttpClientErrorException e) {
      log.error("Lỗi từ Keycloak khi refresh token: {}", e.getStatusCode());
      throw new AppException(ErrorCode.UNAUTHENTICATED);
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

  private Map<String, Object> callKeyMapcloakEndpoint(String tailUrl, MultiValueMap<String, String> formData) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    String url = keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect" + tailUrl;

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
    return restTemplate.postForObject(url, request, Map.class);
  }

}
