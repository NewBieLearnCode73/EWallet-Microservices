package com.dinhchieu.ewallet.profile_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileStatusUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.services.ProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class ProfileController {

  private final ProfileService profileService;

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> getMyProfile(HttpServletRequest request) {
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Lấy thông tin cá nhân thành công")
        .data(profileService.getProfile())
        .build());
  }

  @PostMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> createMyProfile(
      @Valid @RequestBody ProfileCreationRequestDto request) {
    profileService.createProfile(request);
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Tạo hồ sơ cá nhân thành công")
        .build());
  }

  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> updateMyProfile(
      @Valid @RequestBody ProfileUpdateRequestDto request) {
    profileService.updateProfile(request);
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Cập nhật hồ sơ cá nhân thành công")
        .build());
  }

  @PatchMapping("/me/activate")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> activateMyProfile() {
    profileService.activateProfile();
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Kích hoạt hồ sơ thành công")
        .build());
  }

  @GetMapping("/me/linked-bank-accounts")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> getMyLinkedBankAccounts() {
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Lấy danh sách các tài khoản ngân hàng liên kết thành công")
        .data(profileService.getMyLinkedBankAccounts())
        .build());
  }

  @PostMapping("/me/linked-bank-accounts")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> linkBankAccount(
      @Valid @RequestBody LinkedBankAccountLinkingRequestDto request) {
    profileService.linkBankAccount(request);
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Liên kết tài khoản ngân hàng thành công")
        .build());
  }

  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BaseResponse<Object>> getProfileByUserId(@PathVariable String userId) {
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Lấy thông tin cá nhân thành công")
        .data(profileService.getProfileByUserId(userId))
        .build());
  }

  @PatchMapping("/admin/{userId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BaseResponse<Object>> updateProfileStatus(
      @PathVariable String userId,
      @Valid @RequestBody ProfileStatusUpdateRequestDto request) {
    profileService.updateProfileStatus(userId, request.getStatus());
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Cập nhật trạng thái hồ sơ thành công")
        .build());
  }

}
