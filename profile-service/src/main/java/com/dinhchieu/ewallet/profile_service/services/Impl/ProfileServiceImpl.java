package com.dinhchieu.ewallet.profile_service.services.Impl;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileResponseDto;
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;
import com.dinhchieu.ewallet.profile_service.models.mappers.ProfileMapper;
import com.dinhchieu.ewallet.profile_service.repositories.ProfileRepository;
import com.dinhchieu.ewallet.profile_service.services.ProfileService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

  private final ProfileRepository profileRepository;
  private final ProfileMapper profileMapper;

  @Override
  public ProfileResponseDto getProfile() {
    UUID userId = SecurityUtils.getAuthenticatedUserId();
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_EXIST));
    return profileMapper.toDto(profile);
  }

  @Override
  public void createProfile(ProfileCreationRequestDto request) {
    UUID userId = SecurityUtils.getAuthenticatedUserId();

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
  public void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto) {
    UUID userId = SecurityUtils.getAuthenticatedUserId();

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
  public ProfileResponseDto getProfileByUserId(String userId) {
    UUID userUuid = UUID.fromString(userId);
    Profile profile = profileRepository.findById(userUuid)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    return profileMapper.toDto(profile);
  }

  @Override
  public void activateProfile() {
    UUID userId = SecurityUtils.getAuthenticatedUserId();
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
  public void updateProfileStatus(String userId, ProfileStatus status) {
    UUID userUuid = UUID.fromString(userId);
    Profile profile = profileRepository.findById(userUuid)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

    profile.setStatus(status);
    profileRepository.save(profile);
    log.info("Admin updated profile status to {} for user ID: {}", status, userId);
  }

}
