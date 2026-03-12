package com.dinhchieu.ewallet.auth_service.models.dtos.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyCloakLoginRequestDto {
  @Builder.Default
  private String grant_type = "password";

  private String client_id;

  private String client_secret;

  private String username;

  private String password;
}
