package com.dinhchieu.ewallet.profile_service.repositories;

import java.time.LocalDate;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.dinhchieu.ewallet.common_library.config.SecurityConfig;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.KeyCloakRoleConverter;
import com.dinhchieu.ewallet.profile_service.models.entities.LinkedBankAccount;
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;

@DataJpaTest(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SecurityConfig.class })
})
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DisplayName("Linked Bank Account Repository Tests")
public class LinkedBankAccountTests {
  @Autowired
  private LinkedBankAccountRepository linkedBankAccountRepository;

  @Autowired
  private ProfileRepository profileRepository;

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

  @Test
  @DisplayName("Test existsByBankCodeAndAccountNumber method")
  public void testExistsByBankCodeAndAccountNumber() {

    // Arrange
    Profile profile = Profile.builder()
        .id(UUID.randomUUID())
        .fullName("John Doe")
        .phoneNumber("0123456789")
        .identityNumber("1234566799")
        .dateOfBirth(LocalDate.now())
        .build();

    Profile savedProfile = profileRepository.save(profile);

    String bankCode = "TCB";
    String accountNumber = "123456789";

    LinkedBankAccount linkedBankAccount = LinkedBankAccount.builder()
        .bankCode(bankCode)
        .accountNumber(accountNumber)
        .profile(savedProfile)
        .build();

    linkedBankAccountRepository.save(linkedBankAccount);

    // 2. Act
    boolean existsBefore = linkedBankAccountRepository.existsByBankCodeAndAccountNumber(bankCode, accountNumber);
    boolean existsWrongCode = linkedBankAccountRepository.existsByBankCodeAndAccountNumber("VCB", accountNumber);

    // 3. Assert
    Assertions.assertTrue(existsBefore);
    Assertions.assertFalse(existsWrongCode);
  }

}
