package com.dinhchieu.ewallet.auth_service.models.dtos.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyCloakLogoutRequestDto {
  private String client_id;
  private String client_secret;
  private String refresh_token;
}
