package com.dinhchieu.ewallet.auth_service.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinhchieu.ewallet.auth_service.clients.KeyCloakClient;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.UserLoginRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.UserRegisterRequestDto;
import com.dinhchieu.ewallet.auth_service.services.AuthService;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.security.filters.JwtBlacklistFilter;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
@TestPropertySource(properties = "keycloak.admin-client.server-url=http://localhost")
public class AuthControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private KeyCloakClient keyCloakClient;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private RedisConnectionFactory redisConnectionFactory;

  @MockitoBean
  private RedisTemplate<String, Object> redisTemplate;

  @MockitoBean
  private CacheManager cacheManager;

  @MockitoBean
  private TokenBlacklistService tokenBlacklistService;

  @MockitoBean
  private JwtBlacklistFilter jwtBlacklistFilter;

  @Test
  public void testRegister() throws Exception {
    UserRegisterRequestDto request = new UserRegisterRequestDto();
    request.setUsername("testuser");
    request.setPassword("testpassword");
    request.setEmail("testemail123@gmail.com");

    doNothing().when(authService).register(anyString(), anyString(), anyString());

    mockMvc.perform(post("/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Đăng kí tài khoản thành công"));
  }

  @Test
  public void testRegisterEmailAlreadyExists() throws Exception {
    UserRegisterRequestDto request = new UserRegisterRequestDto();
    request.setUsername("testuser");
    request.setPassword("testpassword");
    request.setEmail("existing@gmail.com");

    doThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS))
        .when(authService).register(anyString(), anyString(), anyString());

    mockMvc.perform(post("/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(2003))
        .andExpect(jsonPath("$.message").value("Email đã tồn tại trong hệ thống."));
  }

  @Test
  public void testLogin_Success() throws Exception {
    UserLoginRequestDto request = new UserLoginRequestDto("testuser", "testpassword");

    doNothing().when(authService).login(anyString(), anyString(), any(HttpServletResponse.class));

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Đăng nhập thành công"));
  }

  @Test
  public void testLogin_Failure() throws Exception {
    UserLoginRequestDto request = new UserLoginRequestDto("wronguser", "wrongpass");

    doThrow(new AppException(ErrorCode.UNAUTHENTICATED))
        .when(authService).login(anyString(), anyString(), any(HttpServletResponse.class));

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(1001))
        .andExpect(jsonPath("$.message").value("Xác thực thất bại"));
  }

  @Test
  public void testLogout_Success() throws Exception {
    doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

    mockMvc.perform(post("/logout"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
  }

  @Test
  public void testRefreshToken_Success() throws Exception {
    doNothing().when(authService).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));

    mockMvc.perform(post("/refresh-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Làm mới token thành công"));
  }

  @Test
  @WithMockUser
  public void testRevokeAllSessions_Success() throws Exception {
    doNothing().when(authService).revokeAllSessions(any(HttpServletRequest.class));

    mockMvc.perform(post("/revoke-all-sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hủy tất cả phiên đăng nhập thành công"));
  }

  @Test
  @WithMockUser(roles = "USER")
  public void testTestEndpoint_Success() throws Exception {
    mockMvc.perform(post("/test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Test endpoint"));
  }
}
