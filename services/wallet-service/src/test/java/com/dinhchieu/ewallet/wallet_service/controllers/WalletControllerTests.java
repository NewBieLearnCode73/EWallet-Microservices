package com.dinhchieu.ewallet.wallet_service.controllers;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinhchieu.ewallet.common_library.security.filters.JwtBlacklistFilter;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletExistResponseDto;
import com.dinhchieu.ewallet.wallet_service.services.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = WalletController.class)
@ContextConfiguration(classes = { WalletController.class })
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.schema-registry-url=http://localhost:8081",
    "spring.kafka.specific-avro-reader=true"
})
public class WalletControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WalletService walletService;

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

  private MockedStatic<SecurityUtils> mockedSecurityUtils;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  public void testIsWalletExists() throws Exception {
    WalletExistResponseDto response = WalletExistResponseDto.builder()
        .exist(true)
        .build();
    when(walletService.isWalletExist(userId)).thenReturn(response);

    mockMvc.perform(get("/exists/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Kiểm tra tồn tại ví thành công"))
        .andExpect(jsonPath("$.data.exist").value(true));
  }

  @Test
  @WithMockUser
  public void testGetBalance() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    WalletBalanceResponseDto balance = new WalletBalanceResponseDto();
    when(walletService.getBalance(userId)).thenReturn(balance);

    mockMvc.perform(get("/balance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy số dư ví thành công"));
  }

  @Test
  @WithMockUser
  public void testActivateWallet() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    doNothing().when(walletService).activeWallet(userId);

    mockMvc.perform(patch("/activate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Kích hoạt ví thành công"));
  }
}
