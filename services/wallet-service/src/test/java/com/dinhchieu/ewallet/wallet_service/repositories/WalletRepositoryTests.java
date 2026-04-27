package com.dinhchieu.ewallet.wallet_service.repositories;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.dinhchieu.ewallet.common_library.config.SecurityConfig;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.KeyCloakRoleConverter;
import com.dinhchieu.ewallet.wallet_service.enums.CurrencyType;
import com.dinhchieu.ewallet.wallet_service.enums.WalletStatus;
import com.dinhchieu.ewallet.wallet_service.models.entities.Wallet;
import com.dinhchieu.ewallet.wallet_service.sagas.handler.WalletKafkaListener;
import com.dinhchieu.ewallet.wallet_service.sagas.outbox.OutboxProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SecurityConfig.class })
})
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
    "spring.kafka.specific-avro-reader=false"
})
@DisplayName("Wallet Repository Tests")
public class WalletRepositoryTests {

  @Autowired
  private WalletRepository walletRepository;

  @MockitoBean
  private RedisConnectionFactory redisConnectionFactory;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean(name = "cacheManager")
  private CacheManager cacheManager;

  @MockitoBean(name = "handlerExceptionResolver")
  public HandlerExceptionResolver handlerExceptionResolver;

  @MockitoBean
  private KeyCloakRoleConverter keyCloakRoleConverter;

  @MockitoBean
  private TokenBlacklistService tokenBlacklistService;

  @MockitoBean
  private OutboxProcessor outboxProcessor;

  @MockitoBean
  private WalletKafkaListener walletKafkaListener;

  @MockitoBean
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Test findByIdWithLock method")
  public void testFindByIdWithLock() {
    String walletId = UUID.randomUUID().toString();

    Wallet wallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("10000.00"))
        .currency(CurrencyType.VND)
        .status(WalletStatus.ACTIVE)
        .build();

    Wallet savedWallet = walletRepository.save(wallet);

    Wallet foundWallet = walletRepository.findByIdWithLock(savedWallet.getId()).orElse(null);

    Assertions.assertNotNull(foundWallet);
  }

}
