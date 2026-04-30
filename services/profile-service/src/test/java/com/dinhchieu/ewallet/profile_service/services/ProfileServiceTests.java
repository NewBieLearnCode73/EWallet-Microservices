package com.dinhchieu.ewallet.profile_service.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileResponseDto;
import com.dinhchieu.ewallet.profile_service.models.entities.LinkedBankAccount;
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;
import com.dinhchieu.ewallet.profile_service.models.mappers.LinkedBankAccountMapper;
import com.dinhchieu.ewallet.profile_service.models.mappers.ProfileMapper;
import com.dinhchieu.ewallet.profile_service.repositories.LinkedBankAccountRepository;
import com.dinhchieu.ewallet.profile_service.repositories.ProfileRepository;
import com.dinhchieu.ewallet.profile_service.services.Impl.ProfileServiceImpl;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTests {

  @Mock
  private LinkedBankAccountRepository linkedBankAccountRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private ProfileMapper profileMapper;

  @Mock
  private LinkedBankAccountMapper linkedBankAccountMapper;

  @InjectMocks
  private ProfileServiceImpl profileService;

  @Test
  @DisplayName("Test getProfile - Success")
  void testGetProfile_Success() {
    // Arrange
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder()
        .id(userId)
        .fullName("John Doe")
        .phoneNumber("1234567890")
        .identityNumber("ID123456")
        .build();
    ProfileResponseDto expectedResponse = new ProfileResponseDto();
    profile.setId(userId);

    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
    when(profileMapper.toDto(profile)).thenReturn(expectedResponse);

    // Act
    ProfileResponseDto actualResponse = profileService.getProfile(userId);

    // Assert
    Assertions.assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  @DisplayName("Test getProfile - Profile Not Found")
  void testGetProfile_ProfileNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    Assertions.assertThatThrownBy(() -> profileService.getProfile(userId))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_PROFILE_NOT_EXIST);
  }

  @Test
  @DisplayName("Test getProfileByUserId - Success")
  void testGetProfileByUserId_Success() {
    UUID userId = UUID.randomUUID();
    Profile profile = new Profile();
    ProfileResponseDto expectedResponse = new ProfileResponseDto();

    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
    when(profileMapper.toDto(profile)).thenReturn(expectedResponse);

    ProfileResponseDto actualResponse = profileService.getProfileByUserId(userId);

    Assertions.assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  @DisplayName("Test isProfileExists - Profile Exists")
  void testIsProfileExists_ProfileExists() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(profileRepository.existsById(userId)).thenReturn(true);

    // Act
    var response = profileService.isProfileExists(userId);

    // Assert
    Assertions.assertThat(response.isExist()).isTrue();
  }

  @Test
  @DisplayName("Test isProfileExists - Profile Does Not Exist")
  void testIsProfileExists_ProfileDoesNotExist() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(profileRepository.existsById(userId)).thenReturn(false);

    // Act
    var response = profileService.isProfileExists(userId);

    // Assert
    Assertions.assertThat(response.isExist()).isFalse();
  }

  @Test
  @DisplayName("Test Get Linked Bank Accounts - Success")
  void testGetMyLinkedBankAccounts_Success() {
    // Arrange
    UUID userId = UUID.randomUUID();
    LinkedBankAccount linkedBankAccount = LinkedBankAccount.builder()
        .bankCode("TCB")
        .accountNumber("123456789")
        .build();

    Profile profile = Profile.builder()
        .id(userId)
        .fullName("John Doe")
        .phoneNumber("1234567890")
        .identityNumber("ID123456")
        .linkedBankAccounts(List.of(linkedBankAccount))
        .build();

    LinkedBankAccountsReponseDto responseDto = new LinkedBankAccountsReponseDto();

    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
    when(linkedBankAccountMapper.toLinkedBankAccountsResponse(profile.getLinkedBankAccounts()))
        .thenReturn(List.of(responseDto));

    // Act
    var response = profileService.getMyLinkedBankAccounts(userId);

    // Assert
    Assertions.assertThat(response).isNotNull();
    Assertions.assertThat(response).hasSize(1);
  }

  @Test
  @DisplayName("Test Get Linked Bank Accounts - Profile Not Found")
  void testGetMyLinkedBankAccounts_ProfileNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    Assertions.assertThatThrownBy(() -> profileService.getMyLinkedBankAccounts(userId))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_PROFILE_NOT_EXIST);
  }

  @Test
  @DisplayName("Test Get Profile Full Name - Success")
  void testGetProfileFullNameByUserId_Success() {
    // Arrange
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder()
        .id(userId)
        .fullName("John Doe")
        .build();

    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

    // Act
    var response = profileService.getProfileFullNameByUserId(userId);

    // Assert
    Assertions.assertThat(response).isNotNull();
    Assertions.assertThat(response.getFullName()).isEqualTo("John Doe");
  }

  @Test
  @DisplayName("Test Get Profile Full Name - Profile Not Found")
  void testGetProfileFullNameByUserId_ProfileNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    Assertions.assertThatThrownBy(() -> profileService.getProfileFullNameByUserId(userId))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_PROFILE_NOT_FOUND);
  }

  @Test
  @DisplayName("Test createProfile - Success")
  void testCreateProfile_Success() {
    UUID userId = UUID.randomUUID();
    ProfileCreationRequestDto request = ProfileCreationRequestDto.builder()
        .fullName("John Doe")
        .phoneNumber("0123456789")
        .identityNumber("123456789")
        .dateOfBirth(LocalDate.now().minusYears(20))
        .build();

    when(profileRepository.existsById(userId)).thenReturn(false);
    when(profileRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
    when(profileRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(false);

    profileService.createProfile(userId, request);

    verify(profileRepository, times(1)).save(any(Profile.class));
  }

  @Test
  @DisplayName("Test createProfile - Already Exists")
  void testCreateProfile_AlreadyExists() {
    UUID userId = UUID.randomUUID();
    when(profileRepository.existsById(userId)).thenReturn(true);

    Assertions.assertThatThrownBy(() -> profileService.createProfile(userId, new ProfileCreationRequestDto()))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PROFILE_ALREADY_EXISTS);
  }

  @Test
  @DisplayName("Test updateProfile - Success")
  void testUpdateProfile_Success() {
    UUID userId = UUID.randomUUID();
    Profile existingProfile = Profile.builder()
        .phoneNumber("0000000000")
        .identityNumber("000000000")
        .build();
    ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
        .fullName("New Name")
        .phoneNumber("0123456789")
        .identityNumber("123456789")
        .dateOfBirth(LocalDate.now().minusYears(25))
        .build();

    when(profileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
    when(profileRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
    when(profileRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(false);

    profileService.updateProfile(userId, request);

    verify(profileRepository, times(1)).save(existingProfile);
    Assertions.assertThat(existingProfile.getFullName()).isEqualTo("New Name");
  }

  @Test
  @DisplayName("Test updateProfileStatus - Success")
  void testUpdateProfileStatus_Success() {
    UUID userId = UUID.randomUUID();
    Profile profile = new Profile();
    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

    profileService.updateProfileStatus(userId.toString(), ProfileStatus.ACTIVE);

    Assertions.assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    verify(profileRepository, times(1)).save(profile);
  }

  @Test
  @DisplayName("Test activateProfile - Success")
  void testActivateProfile_Success() {
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder().status(ProfileStatus.INACTIVE).build();
    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

    profileService.activateProfile(userId);

    Assertions.assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    verify(profileRepository, times(1)).save(profile);
  }

  @Test
  @DisplayName("Test activateProfile - Invalid Status")
  void testActivateProfile_InvalidStatus() {
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder().status(ProfileStatus.ACTIVE).build();
    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

    Assertions.assertThatThrownBy(() -> profileService.activateProfile(userId))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  }

  @Test
  @DisplayName("Test linkBankAccount - Success")
  void testLinkBankAccount_Success() {
    UUID userId = UUID.randomUUID();
    Profile profile = new Profile();
    LinkedBankAccountLinkingRequestDto request = LinkedBankAccountLinkingRequestDto.builder()
        .bankCode("VCB")
        .accountNumber("999999999")
        .build();

    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
    when(
        linkedBankAccountRepository.existsByBankCodeAndAccountNumber(request.getBankCode(), request.getAccountNumber()))
        .thenReturn(false);

    profileService.linkBankAccount(userId, request);

    verify(linkedBankAccountRepository, times(1)).save(any(LinkedBankAccount.class));
  }

}
