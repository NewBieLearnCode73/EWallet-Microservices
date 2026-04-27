package com.dinhchieu.ewallet.profile_service.repositories;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
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
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;

@DataJpaTest(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SecurityConfig.class })
})
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DisplayName("Profile Repository Tests")
public class ProfileRepositoryTests {
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
  @DisplayName("Test findById method")
  public void testFindById() {
    // Arrange
    UUID userId = UUID.randomUUID();

    Profile profile = Profile.builder()
        .id(userId)
        .fullName("John Doe")
        .phoneNumber("1234567890")
        .identityNumber("ID123456")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();

    // Act
    profileRepository.save(profile);
    Optional<Profile> foundProfile = profileRepository.findById(userId);

    // Assert
    Assertions.assertThat(foundProfile).isPresent();
    Assertions.assertThat(foundProfile.get().getId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("Test existsByPhoneNumber method")
  public void testExistsByPhoneNumber() {
    // Arrange
    String phoneNumber = "1234567890";

    Profile profile = Profile.builder()
        .id(UUID.randomUUID())
        .fullName("John Doe")
        .phoneNumber(phoneNumber)
        .identityNumber("ID123456")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();

    // Act
    profileRepository.save(profile);
    boolean exists = profileRepository.existsByPhoneNumber(phoneNumber);

    // Assert
    Assertions.assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Test existsByIdentityNumber method")
  public void testExistsByIdentityNumber() {
    // Arrange
    String identityNumber = "ID123456";

    Profile profile = Profile.builder()
        .id(UUID.randomUUID())
        .fullName("John Doe")
        .phoneNumber("1234567890")
        .identityNumber(identityNumber)
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();

    // Act
    profileRepository.save(profile);

    boolean exists = profileRepository.existsByIdentityNumber(identityNumber);

    // Assert
    Assertions.assertThat(exists).isTrue();
  }

}
