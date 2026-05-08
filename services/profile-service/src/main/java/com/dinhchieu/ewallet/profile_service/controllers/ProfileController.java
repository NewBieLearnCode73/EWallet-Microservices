package com.dinhchieu.ewallet.profile_service.controllers;

import java.util.UUID;

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
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileStatusUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.services.ProfileService;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class ProfileController {

        private final ProfileService profileService;

        @GetMapping("/fullname/{userId}")
        @PermitAll
        public ResponseEntity<BaseResponse<Object>> getProfileById(@PathVariable("userId") String userId) {
                UUID userUuid = UUID.fromString(userId);

                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Lấy thông tin cá nhân thành công")
                                .data(profileService.getProfileFullNameByUserId(userUuid))
                                .build());
        }

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> getMyProfile(HttpServletRequest request) {
                UUID userId = SecurityUtils.getAuthenticatedUserId();

                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Lấy thông tin cá nhân thành công")
                                .data(profileService.getProfile(userId))
                                .build());
        }

        @PostMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> createMyProfile(
                        @Valid @RequestBody ProfileCreationRequestDto request) {
                UUID userId = SecurityUtils.getAuthenticatedUserId();

                profileService.createProfile(userId, request);
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Tạo hồ sơ cá nhân thành công")
                                .build());
        }

        @PutMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> updateMyProfile(
                        @Valid @RequestBody ProfileUpdateRequestDto request) {
                UUID userId = SecurityUtils.getAuthenticatedUserId();

                profileService.updateProfile(userId, request);
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Cập nhật hồ sơ cá nhân thành công")
                                .build());
        }

        @PatchMapping("/me/activate")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> activateMyProfile() {
                UUID userId = SecurityUtils.getAuthenticatedUserId();

                profileService.activateProfile(userId);
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Kích hoạt hồ sơ thành công")
                                .build());
        }

        @GetMapping("/me/linked-bank-accounts")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> getMyLinkedBankAccounts() {
                UUID userId = SecurityUtils.getAuthenticatedUserId();

                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Lấy danh sách các tài khoản ngân hàng liên kết thành công")
                                .data(profileService.getMyLinkedBankAccounts(userId))
                                .build());
        }

        @PostMapping("/me/linked-bank-accounts")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> linkBankAccount(
                        @Valid @RequestBody LinkedBankAccountLinkingRequestDto request) {

                UUID userId = SecurityUtils.getAuthenticatedUserId();

                profileService.linkBankAccount(userId, request);
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Liên kết tài khoản ngân hàng thành công")
                                .build());
        }

        @GetMapping("/exists/{userId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<BaseResponse<Object>> isProfileExists(@PathVariable("userId") String userId) {
                UUID userUuid = UUID.fromString(userId);

                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Kiểm tra tồn tại hồ sơ thành công")
                                .data(profileService.isProfileExists(
                                                userUuid))
                                .build());
        }

        @GetMapping("/{userId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<BaseResponse<Object>> getProfileByUserId(@PathVariable("userId") String userId) {
                UUID userUuid = UUID.fromString(userId);
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Lấy thông tin cá nhân thành công")
                                .data(profileService.getProfileByUserId(
                                                userUuid))
                                .build());
        }

        @PatchMapping("/admin/{userId}/status")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<BaseResponse<Object>> updateProfileStatus(
                        @PathVariable("userId") String userId,
                        @Valid @RequestBody ProfileStatusUpdateRequestDto request) {
                profileService.updateProfileStatus(userId, request.getStatus());
                return ResponseEntity.ok(BaseResponse.builder()
                                .message("Cập nhật trạng thái hồ sơ thành công")
                                .build());
        }

}
