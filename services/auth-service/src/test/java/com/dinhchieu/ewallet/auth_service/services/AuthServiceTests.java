package com.dinhchieu.ewallet.auth_service.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.dinhchieu.ewallet.auth_service.clients.KeyCloakClient;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLoginRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLogoutRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakRefreshTokenRequestDto;
import com.dinhchieu.ewallet.auth_service.services.Impl.AuthServiceImpl;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;

import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

  @InjectMocks
  private AuthServiceImpl authServiceImpl;

  @Mock
  private Keycloak keycloak;

  @Mock
  private KeyCloakClient keyCloakClient;

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private TokenBlacklistService blacklistService;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private RealmResource realmResource;

  @Mock
  private UsersResource usersResource;

  @Mock
  private UserResource userResource;

  @Captor
  private ArgumentCaptor<Cookie> cookieCaptor;

  @Captor
  private ArgumentCaptor<UserRepresentation> userRepresentationCaptor;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authServiceImpl, "keycloakRealm", "ewallet");
    ReflectionTestUtils.setField(authServiceImpl, "keycloakClientId", "auth-client");
    ReflectionTestUtils.setField(authServiceImpl, "keycloakClientSecret", "secret");
    ReflectionTestUtils.setField(authServiceImpl, "accessTokenExpiration", 3600);
    ReflectionTestUtils.setField(authServiceImpl, "refreshTokenExpiration", 7200);
  }

  @Test
  @DisplayName("Test Register Success: Create user when Keycloak returns 201")
  void registerShouldCreateUserWhenKeycloakReturnsCreated() {
    // Given
    mockRealmUsers();
    jakarta.ws.rs.core.Response keycloakResponse = org.mockito.Mockito.mock(jakarta.ws.rs.core.Response.class);
    when(usersResource.create(userRepresentationCaptor.capture())).thenReturn(keycloakResponse);
    when(keycloakResponse.getStatus()).thenReturn(201);

    // When
    assertDoesNotThrow(() -> authServiceImpl.register("alice", "password123", "alice@example.com"));

    // Then
    UserRepresentation user = userRepresentationCaptor.getValue();
    assertEquals("alice", user.getUsername());
    assertEquals("alice@example.com", user.getEmail());
    assertTrue(user.isEnabled());
    verify(keycloakResponse).close();
  }

  @Test
  @DisplayName("Test Register Failure: Throw EMAIL_ALREADY_EXISTS when Keycloak returns 409")
  void registerShouldThrowAppExceptionWhenKeycloakReturnsConflict() {
    // Given
    mockRealmUsers();
    jakarta.ws.rs.core.Response keycloakResponse = org.mockito.Mockito.mock(jakarta.ws.rs.core.Response.class);
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
    when(keycloakResponse.getStatus()).thenReturn(409);

    // When
    AppException appException = assertThrows(AppException.class,
        () -> authServiceImpl.register("alice", "password123", "alice@example.com"));

    // Then
    assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, appException.getErrorCode());
    verify(keycloakResponse).close();
  }

  @Test
  @DisplayName("Test Register Failure: Throw RuntimeException when Keycloak returns non-201 and non-409")
  void registerShouldThrowRuntimeExceptionWhenKeycloakReturnsUnexpectedStatus() {
    // Given
    mockRealmUsers();
    jakarta.ws.rs.core.Response keycloakResponse = org.mockito.Mockito.mock(jakarta.ws.rs.core.Response.class);
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
    when(keycloakResponse.getStatus()).thenReturn(500);

    // When
    RuntimeException runtimeException = assertThrows(RuntimeException.class,
        () -> authServiceImpl.register("alice", "password123", "alice@example.com"));

    // Then
    assertTrue(runtimeException.getMessage().contains("500"));
    verify(keycloakResponse).close();
  }

  @Test
  @DisplayName("Test Login Success: Add auth cookies when Keycloak returns tokens")
  void loginShouldSetAuthCookiesWhenKeycloakReturnsTokens() {
    // Given
    Map<String, Object> keycloakResponse = new HashMap<>();
    keycloakResponse.put("access_token", "access-token-value");
    keycloakResponse.put("refresh_token", "refresh-token-value");

    when(keyCloakClient.login(eq("ewallet"), any(KeyCloakLoginRequestDto.class))).thenReturn(keycloakResponse);

    // When
    authServiceImpl.login("alice", "password123", response);

    // Then
    verify(keyCloakClient).login(eq("ewallet"), any(KeyCloakLoginRequestDto.class));
    verify(response, times(2)).addCookie(cookieCaptor.capture());

    Map<String, Cookie> cookiesByName = new HashMap<>();
    for (Cookie cookie : cookieCaptor.getAllValues()) {
      cookiesByName.put(cookie.getName(), cookie);
    }

    assertEquals("access-token-value", cookiesByName.get("access_token").getValue());
    assertEquals("refresh-token-value", cookiesByName.get("refresh_token").getValue());
    assertEquals(3600, cookiesByName.get("access_token").getMaxAge());
    assertEquals(7200, cookiesByName.get("refresh_token").getMaxAge());
  }

  @Test
  @DisplayName("Test Login Failure: Throw UNAUTHENTICATED when Keycloak returns 4xx")
  void loginShouldThrowAppExceptionWhenKeycloakReturns4xx() {
    // Given
    FeignException unauthorizedException = createFeignException(401, "Unauthorized");
    when(keyCloakClient.login(eq("ewallet"), any(KeyCloakLoginRequestDto.class))).thenThrow(unauthorizedException);

    // When
    AppException appException = assertThrows(AppException.class,
        () -> authServiceImpl.login("alice", "wrong-password", response));

    // Then
    assertEquals(ErrorCode.UNAUTHENTICATED, appException.getErrorCode());
  }

  @Test
  @DisplayName("Test Login Failure: Rethrow FeignException when Keycloak returns 5xx")
  void loginShouldRethrowFeignExceptionWhenKeycloakReturns5xx() {
    // Given
    FeignException serverException = createFeignException(500, "Internal Server Error");
    when(keyCloakClient.login(eq("ewallet"), any(KeyCloakLoginRequestDto.class))).thenThrow(serverException);

    // When
    FeignException thrown = assertThrows(FeignException.class,
        () -> authServiceImpl.login("alice", "password123", response));

    // Then
    assertEquals(500, thrown.status());
  }

  @Test
  @DisplayName("Test Logout Success: Blacklist access token, call Keycloak logout, and clear auth cookies")
  void logoutShouldBlacklistAndCallKeycloakAndClearCookies() {
    // Given
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("access_token", "access-token-value"),
        new Cookie("refresh_token", "refresh-token-value")
    });

    Jwt jwt = Jwt.withTokenValue("access-token-value")
        .header("alg", "none")
        .claim("sub", "user-123")
        .claim("jti", "jwt-id-123")
        .build();
    when(jwtDecoder.decode("access-token-value")).thenReturn(jwt);
    when(keyCloakClient.logout(eq("ewallet"), any(KeyCloakLogoutRequestDto.class))).thenReturn(Map.of());

    // When
    authServiceImpl.logout(request, response);

    // Then
    verify(blacklistService).blacklistToken("jwt-id-123", 3600);
    verify(keyCloakClient).logout(eq("ewallet"), any(KeyCloakLogoutRequestDto.class));
    verify(response, times(2)).addCookie(any(Cookie.class));
  }

  @Test
  @DisplayName("Test Logout Without Tokens: Only clear cookies and skip blacklist or Keycloak calls")
  void logoutShouldOnlyClearCookiesWhenTokensAreMissing() {
    // Given
    when(request.getCookies()).thenReturn(null);

    // When
    authServiceImpl.logout(request, response);

    // Then
    verifyNoInteractions(jwtDecoder, blacklistService, keyCloakClient);
    verify(response, times(2)).addCookie(any(Cookie.class));
  }

  @Test
  @DisplayName("Test Refresh Token Success: Set new cookies when Keycloak returns tokens")
  void refreshTokenShouldSetNewCookiesWhenKeycloakReturnsTokens() {
    // Given
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("refresh_token", "refresh-token-value")
    });
    Map<String, Object> keycloakResponse = new HashMap<>();
    keycloakResponse.put("access_token", "new-access-token");
    keycloakResponse.put("refresh_token", "new-refresh-token");

    when(keyCloakClient.refreshToken(eq("ewallet"), any(KeyCloakRefreshTokenRequestDto.class)))
        .thenReturn(keycloakResponse);

    // When
    authServiceImpl.refreshToken(request, response);

    // Then
    verify(keyCloakClient).refreshToken(eq("ewallet"), any(KeyCloakRefreshTokenRequestDto.class));
    verify(response, times(2)).addCookie(cookieCaptor.capture());

    Map<String, Cookie> cookiesByName = new HashMap<>();
    for (Cookie cookie : cookieCaptor.getAllValues()) {
      cookiesByName.put(cookie.getName(), cookie);
    }

    assertEquals("new-access-token", cookiesByName.get("access_token").getValue());
    assertEquals("new-refresh-token", cookiesByName.get("refresh_token").getValue());
  }

  @Test
  @DisplayName("Test Refresh Token Failure: Throw MISSING_AUTHENTICATION when refresh token cookie is missing")
  void refreshTokenShouldThrowMissingAuthenticationWhenRefreshCookieMissing() {
    // Given
    when(request.getCookies()).thenReturn(null);

    // When
    AppException appException = assertThrows(AppException.class,
        () -> authServiceImpl.refreshToken(request, response));

    // Then
    assertEquals(ErrorCode.MISSING_AUTHENTICATION, appException.getErrorCode());
    verify(keyCloakClient, never()).refreshToken(eq("ewallet"), any(KeyCloakRefreshTokenRequestDto.class));
  }

  @Test
  @DisplayName("Test Refresh Token Failure: Throw UNAUTHENTICATED when Keycloak returns 4xx")
  void refreshTokenShouldThrowAppExceptionWhenKeycloakReturns4xx() {
    // Given
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("refresh_token", "refresh-token-value")
    });

    FeignException unauthorizedException = createFeignException(401, "Unauthorized");
    when(keyCloakClient.refreshToken(eq("ewallet"), any(KeyCloakRefreshTokenRequestDto.class)))
        .thenThrow(unauthorizedException);

    // When
    AppException appException = assertThrows(AppException.class,
        () -> authServiceImpl.refreshToken(request, response));

    // Then
    assertEquals(ErrorCode.UNAUTHENTICATED, appException.getErrorCode());
  }

  @Test
  @DisplayName("Test Refresh Token Failure: Rethrow FeignException when Keycloak returns 5xx")
  void refreshTokenShouldRethrowFeignExceptionWhenKeycloakReturns5xx() {
    // Given
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("refresh_token", "refresh-token-value")
    });

    FeignException serverException = createFeignException(500, "Internal Server Error");
    when(keyCloakClient.refreshToken(eq("ewallet"), any(KeyCloakRefreshTokenRequestDto.class)))
        .thenThrow(serverException);

    // When
    FeignException thrown = assertThrows(FeignException.class,
        () -> authServiceImpl.refreshToken(request, response));

    // Then
    assertEquals(500, thrown.status());
  }

  @Test
  @DisplayName("Test Revoke Sessions Failure: Throw MISSING_AUTHENTICATION when access token cookie is missing")
  void revokeAllSessionsShouldThrowMissingAuthenticationWhenAccessCookieMissing() {
    // Given
    when(request.getCookies()).thenReturn(null);

    // When
    AppException appException = assertThrows(AppException.class,
        () -> authServiceImpl.revokeAllSessions(request));

    // Then
    assertEquals(ErrorCode.MISSING_AUTHENTICATION, appException.getErrorCode());
  }

  @Test
  @DisplayName("Test Revoke Sessions Success: Logout all sessions in Keycloak and blacklist token")
  void revokeAllSessionsShouldLogoutUserAndBlacklistToken() {
    // Given
    mockRealmUsers();
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("access_token", "access-token-value")
    });
    Jwt jwt = Jwt.withTokenValue("access-token-value")
        .header("alg", "none")
        .claim("sub", "user-123")
        .claim("jti", "jwt-id-123")
        .build();

    when(jwtDecoder.decode("access-token-value")).thenReturn(jwt);
    when(usersResource.get("user-123")).thenReturn(userResource);

    // When
    authServiceImpl.revokeAllSessions(request);

    // Then
    verify(userResource).logout();
    verify(blacklistService).blacklistToken("jwt-id-123", 3600);
  }

  private void mockRealmUsers() {
    when(keycloak.realm("ewallet")).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
  }

  private FeignException createFeignException(int status, String reason) {
    Request request = Request.create(
        Request.HttpMethod.POST,
        "http://localhost/realms/ewallet/protocol/openid-connect/token",
        Map.of(),
        null,
        StandardCharsets.UTF_8,
        null);

    Response response = Response.builder()
        .status(status)
        .reason(reason)
        .request(request)
        .headers(Map.of())
        .body(new byte[0])
        .build();

    return FeignException.errorStatus("KeyCloakClient#login(String,KeyCloakLoginRequestDto)", response);
  }

}