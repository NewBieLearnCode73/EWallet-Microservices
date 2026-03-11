package com.dinhchieu.ewallet.profile_service.services;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileResponseDto;

public interface ProfileService {
  /**
   * Get profile information of the currently authenticated user. The user ID is
   * extracted from the JWT token's "sub" claim, and then the corresponding
   * profile is retrieved from the database. If the user is not authenticated, if
   * the token is invalid, or if the profile does not exist, appropriate
   * exceptions are thrown.
   * 
   * @return ProfileResponseDto
   */
  public ProfileResponseDto getProfile();

  /**
   * Get profile information by user ID.
   * 
   * @param userId the ID of the user whose profile is to be retrieved
   * @return ProfileResponseDto
   */
  public ProfileResponseDto getProfileByUserId(String userId);

  /**
   * Create a new profile for the currently authenticated user.
   * 
   */
  public void createProfile(ProfileCreationRequestDto profileCreationRequestDto);

  /**
   * Update the profile of the currently authenticated user.
   * 
   */
  public void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

  /**
   * Activate the profile of the currently authenticated user (INACTIVE → ACTIVE).
   * Throws an exception if the profile is not in INACTIVE state.
   */
  public void activateProfile();

  /**
   * Admin: update the status of any user's profile (ACTIVE, SUSPENDED,
   * LOCKED...).
   * 
   * @param userId the target user ID
   * @param status the new status to set
   */
  public void updateProfileStatus(String userId, ProfileStatus status);
}
