package com.dinhchieu.ewallet.auth_service.models.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequestDto {
  @NotBlank(message = "Tên đăng nhập không được để trống")
  @Size(min = 3, max = 50, message = "Tên đăng nhập phải có từ 3 đến 50 ký tự")
  private String username;

  @NotBlank(message = "Mật khẩu không được để trống")
  @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự")
  private String password;

  @NotBlank(message = "Email không được để trống")
  @Size(max = 50, message = "Email không được vượt quá 50 ký tự")
  @Email(message = "Email không đúng định dạng. Vui lòng kiểm tra lại.", regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
  private String email;
}
