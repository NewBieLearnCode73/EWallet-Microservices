package com.dinhchieu.ewallet.profile_service.services.Impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileExistResponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileFullNameResponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileResponseDto;
import com.dinhchieu.ewallet.profile_service.models.entities.LinkedBankAccount;
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;
import com.dinhchieu.ewallet.profile_service.models.mappers.LinkedBankAccountMapper;
import com.dinhchieu.ewallet.profile_service.models.mappers.ProfileMapper;
import com.dinhchieu.ewallet.profile_service.repositories.LinkedBankAccountRepository;
import com.dinhchieu.ewallet.profile_service.repositories.ProfileRepository;
import com.dinhchieu.ewallet.profile_service.services.ProfileService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

  private final ProfileRepository profileRepository;
  private final LinkedBankAccountRepository linkedBankAccountRepository;
  private final ProfileMapper profileMapper;
  private final LinkedBankAccountMapper linkedBankAccountMapper;

  @Override
  @Cacheable(value = "profile_details", key = "#userId")
  public ProfileResponseDto getProfile(UUID userId) {
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));
    return profileMapper.toDto(profile);
  }

  @Override
  @Cacheable(value = "profile_details", key = "#userId")
  public ProfileResponseDto getProfileByUserId(UUID userId) {
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    return profileMapper.toDto(profile);
  }

  @Override
  @Cacheable(value = "profile_exists", key = "#userId")
  public ProfileExistResponseDto isProfileExists(UUID userId) {
    return ProfileExistResponseDto.builder()
        .exist(profileRepository.existsById(userId))
        .build();
  }

  @Override
  @Cacheable(value = "linked_bank_accounts", key = "#userId")
  public List<LinkedBankAccountsReponseDto> getMyLinkedBankAccounts(UUID userId) {

    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));

    return linkedBankAccountMapper.toLinkedBankAccountsResponse(profile.getLinkedBankAccounts());
  }

  @Override
  @Cacheable(value = "profile_full_name", key = "#userId")
  public ProfileFullNameResponseDto getProfileFullNameByUserId(UUID userId) {
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

    return ProfileFullNameResponseDto
        .builder()
        .fullName(profile.getFullName())
        .build();
  }

  @Override
  @CacheEvict(value = { "profile_details", "profile_exists", "linked_bank_accounts",
      "profile_full_name" }, key = "#userId")
  public void createProfile(UUID userId, ProfileCreationRequestDto request) {

    if (profileRepository.existsById(userId)) {
      log.warn("Profile already exists for user ID: {}", userId);
      throw new AppException(ErrorCode.PROFILE_ALREADY_EXISTS);
    }

    if (request.getDateOfBirth() != null && request.getDateOfBirth().plusYears(18).isAfter(LocalDate.now())) {
      log.warn("User is a minor: {}", request.getDateOfBirth());
      throw new AppException(ErrorCode.MINOR_NOT_ALLOWED);
    }

    if (profileRepository.existsByPhoneNumber(request.getPhoneNumber())) {
      log.warn("Phone number already exists: {}", request.getPhoneNumber());
      throw new AppException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
    }

    if (profileRepository.existsByIdentityNumber(request.getIdentityNumber())) {
      log.warn("Identity number already exists: {}", request.getIdentityNumber());
      throw new AppException(ErrorCode.IDENTITY_NUMBER_ALREADY_EXISTS);
    }

    Profile newProfile = Profile.builder()
        .id(userId)
        .fullName(request.getFullName())
        .phoneNumber(request.getPhoneNumber())
        .identityNumber(request.getIdentityNumber())
        .dateOfBirth(request.getDateOfBirth())
        .build();

    profileRepository.save(newProfile);
    log.info("Created new profile for user ID: {}", userId);
  }

  @Override
  @CacheEvict(value = { "profile_details", "profile_exists", "linked_bank_accounts",
      "profile_full_name" }, key = "#userId")
  public void updateProfile(UUID userId, ProfileUpdateRequestDto profileUpdateRequestDto) {
    Profile existingProfile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));

    if (profileUpdateRequestDto.getDateOfBirth() != null
        && profileUpdateRequestDto.getDateOfBirth().plusYears(18).isAfter(LocalDate.now())) {
      log.warn("User is a minor: {}", profileUpdateRequestDto.getDateOfBirth());
      throw new AppException(ErrorCode.MINOR_NOT_ALLOWED);
    }

    if (!existingProfile.getPhoneNumber().equals(profileUpdateRequestDto.getPhoneNumber())
        && profileRepository.existsByPhoneNumber(profileUpdateRequestDto.getPhoneNumber())) {
      log.warn("Phone number already exists: {}", profileUpdateRequestDto.getPhoneNumber());
      throw new AppException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
    }

    if (!existingProfile.getIdentityNumber().equals(profileUpdateRequestDto.getIdentityNumber())
        && profileRepository.existsByIdentityNumber(profileUpdateRequestDto.getIdentityNumber())) {
      log.warn("Identity number already exists: {}", profileUpdateRequestDto.getIdentityNumber());
      throw new AppException(ErrorCode.IDENTITY_NUMBER_ALREADY_EXISTS);
    }

    existingProfile.setFullName(profileUpdateRequestDto.getFullName());
    existingProfile.setPhoneNumber(profileUpdateRequestDto.getPhoneNumber());
    existingProfile.setIdentityNumber(profileUpdateRequestDto.getIdentityNumber());
    existingProfile.setDateOfBirth(profileUpdateRequestDto.getDateOfBirth());

    profileRepository.save(existingProfile);
    log.info("Updated profile for user ID: {}", userId);
  }

  @Override
  @CacheEvict(value = { "profile_details", "profile_exists", "linked_bank_accounts",
      "profile_full_name" }, key = "#userId")
  public void updateProfileStatus(String userId, ProfileStatus status) {
    UUID userUuid = UUID.fromString(userId);
    Profile profile = profileRepository.findById(userUuid)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

    profile.setStatus(status);
    profileRepository.save(profile);
    log.info("Admin updated profile status to {} for user ID: {}", status, userId);
  }

  @Override
  @CacheEvict(value = { "profile_details", "profile_exists", "linked_bank_accounts",
      "profile_full_name" }, key = "#userId")
  public void activateProfile(UUID userId) {
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));

    if (profile.getStatus() != ProfileStatus.INACTIVE) {
      log.warn("Cannot activate profile with status: {}", profile.getStatus());
      throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    profile.setStatus(ProfileStatus.ACTIVE);
    profileRepository.save(profile);
    log.info("Activated profile for user ID: {}", userId);
  }

  @Override
  @CacheEvict(value = { "profile_details", "profile_exists", "linked_bank_accounts",
      "profile_full_name" }, key = "#userId")
  @Transactional
  public void linkBankAccount(UUID userId, LinkedBankAccountLinkingRequestDto request) {

    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));

    if (linkedBankAccountRepository.existsByBankCodeAndAccountNumber(
        request.getBankCode(), request.getAccountNumber())) {
      throw new AppException(ErrorCode.BANK_ACCOUNT_ALREADY_LINKED);
    }

    LinkedBankAccount linkedBankAccount = LinkedBankAccount.builder()
        .bankCode(request.getBankCode())
        .accountNumber(request.getAccountNumber())
        .profile(profile)
        .build();

    linkedBankAccountRepository.save(linkedBankAccount);
    log.info("Linked bank account {} for user {}", request.getAccountNumber(), userId);
  }

}
