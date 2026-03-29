package com.dinhchieu.ewallet.auth_service.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.auth_service.models.dtos.request.UserLoginRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.UserRegisterRequestDto;
import com.dinhchieu.ewallet.auth_service.services.AuthService;
import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@AllArgsConstructor
@Slf4j
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  @PermitAll
  public ResponseEntity<BaseResponse<Object>> register(@Valid @RequestBody UserRegisterRequestDto request) {
    authService.register(request.getUsername(), request.getPassword(), request.getEmail());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.builder().message("Đăng kí tài khoản thành công").build());
  }

  @PostMapping("/login")
  @PermitAll
  public ResponseEntity<BaseResponse<Object>> login(@Valid @RequestBody UserLoginRequestDto request,
      HttpServletResponse response) {
    authService.login(request.getUsername(), request.getPassword(), response);
    return ResponseEntity.ok(BaseResponse.builder().message("Đăng nhập thành công").build());
  }

  @PostMapping("/logout")
  @PermitAll
  public ResponseEntity<BaseResponse<Object>> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
    return ResponseEntity.ok(BaseResponse.builder().message("Đăng xuất thành công").build());
  }

  @PostMapping("/refresh-token")
  @PermitAll
  public ResponseEntity<BaseResponse<Object>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    authService.refreshToken(request, response);
    return ResponseEntity.ok(BaseResponse.builder().message("Làm mới token thành công").build());
  }

  @PostMapping("/revoke-all-sessions")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> revokeAllSessions(HttpServletRequest request) {
    authService.revokeAllSessions(request);
    return ResponseEntity.ok(BaseResponse.builder().message("Hủy tất cả phiên đăng nhập thành công").build());
  }

  @PostMapping("/test")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<BaseResponse<Object>> test() {
    return ResponseEntity.ok(BaseResponse.builder().message("Test endpoint").build());
  }
}
