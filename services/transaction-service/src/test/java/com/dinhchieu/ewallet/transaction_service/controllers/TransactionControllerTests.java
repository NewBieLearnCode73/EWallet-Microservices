package com.dinhchieu.ewallet.transaction_service.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinhchieu.ewallet.common_library.security.filters.JwtBlacklistFilter;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.InternalTransferRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionDepositRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionSearchRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionWithdrawRequestDto;
import com.dinhchieu.ewallet.transaction_service.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TransactionController.class)
@ContextConfiguration(classes = {TransactionController.class, TransactionControllerTests.MongoConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.schema-registry-url=http://localhost:8081",
    "spring.kafka.specific-avro-reader=true"
})
public class TransactionControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TransactionService transactionService;

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

  @TestConfiguration
  static class MongoConfig {
    @Bean
    public MongoTemplate mongoTemplate() {
      return Mockito.mock(MongoTemplate.class);
    }
  }

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
  @WithMockUser
  public void testGetTransactionById() throws Exception {
    String transactionId = "trans-123";
    when(transactionService.getTransactionById(transactionId)).thenReturn(null);

    mockMvc.perform(get("/" + transactionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy thông tin giao dịch thành công"));
  }

  @Test
  @WithMockUser
  public void testSearchTransactions() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    TransactionSearchRequestDto request = new TransactionSearchRequestDto();

    when(transactionService.searchTransactions(eq(userId.toString()), any(TransactionSearchRequestDto.class)))
        .thenReturn(null);

    mockMvc.perform(get("/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Tìm kiếm giao dịch thành công"));
  }

  @Test
  @WithMockUser
  public void testDeposit() throws Exception {
    TransactionDepositRequestDto request = new TransactionDepositRequestDto();
    request.setAmount(1000.0);
    request.setBankCode("BANK");
    request.setAccountNumber("1234567890");

    when(transactionService.processDepositFromBank(anyDouble(), anyString(), anyString()))
        .thenReturn(null);

    mockMvc.perform(post("/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Nạp tiền thành công"));
  }

  @Test
  @WithMockUser
  public void testWithdraw() throws Exception {
    TransactionWithdrawRequestDto request = new TransactionWithdrawRequestDto();
    request.setAmount(500.0);
    request.setBankCode("BANK");
    request.setAccountNumber("1234567890");

    when(transactionService.processWithdrawalFromBank(anyDouble(), anyString(), anyString()))
        .thenReturn(null);

    mockMvc.perform(post("/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Rút tiền thành công"));
  }

  @Test
  @WithMockUser
  public void testTransfer() throws Exception {
    InternalTransferRequestDto request = new InternalTransferRequestDto();
    request.setAmount(200.0);
    request.setDestinationWalletId(UUID.randomUUID().toString());

    when(transactionService.processInternalTransfer(anyDouble(), anyString()))
        .thenReturn(null);

    mockMvc.perform(post("/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Chuyển tiền thành công"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testAdminSearchAllTransactions() throws Exception {
    TransactionSearchRequestDto request = new TransactionSearchRequestDto();

    when(transactionService.adminSearchAllTransactions(any(TransactionSearchRequestDto.class)))
        .thenReturn(null);

    mockMvc.perform(get("/admin/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Admin tìm kiếm giao dịch thành công"));
  }
}
