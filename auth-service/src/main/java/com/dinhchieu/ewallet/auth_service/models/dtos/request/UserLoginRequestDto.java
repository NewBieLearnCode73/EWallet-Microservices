package com.dinhchieu.ewallet.auth_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequestDto {
  @NotBlank(message = "Tên đăng nhập không được để trống")
  private String username;
  @NotBlank(message = "Mật khẩu không được để trống")
  private String password;
}
