package com.dinhchieu.ewallet.profile_service.models.dtos.request;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileStatusUpdateRequestDto {
  @NotNull(message = "Trạng thái không được để trống")
  private ProfileStatus status;
}
