package com.dinhchieu.ewallet.profile_service.services;

import java.util.List;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileExistResponseDto;
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
   * Check if a profile exists for the given user ID.
   * 
   * @param userId the ID of the user to check for profile existence
   * 
   * @return true if a profile exists for the given user ID, false otherwise
   */
  public ProfileExistResponseDto isProfileExists(String userId);

  /**
   * Admin: update the status of any user's profile (ACTIVE, SUSPENDED,
   * LOCKED...).
   * 
   * @param userId the target user ID
   * @param status the new status to set
   */
  public void updateProfileStatus(String userId, ProfileStatus status);

  /**
   * Link a bank account to the profile of the currently authenticated user.
   * 
   * @param request the request containing bank account information
   */
  public void linkBankAccount(LinkedBankAccountLinkingRequestDto request);

  /**
   * Get the list of bank accounts linked to the profile of the currently
   * authenticated user.
   * 
   * @return List of LinkedBankAccountsReponseDto representing the linked bank
   *         accounts
   */
  public List<LinkedBankAccountsReponseDto> getMyLinkedBankAccounts();

}
