package com.dinhchieu.ewallet.profile_service.models.dtos.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileExistResponseDto implements Serializable {
  private boolean exist;
}
