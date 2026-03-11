package com.dinhchieu.ewallet.profile_service.models.dtos.response;

import java.time.LocalDate;
import java.util.UUID;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponseDto {
  private UUID id;
  private String fullName;
  private String phoneNumber;
  private String identityNumber;
  private LocalDate dateOfBirth;
  private ProfileStatus status;
}
