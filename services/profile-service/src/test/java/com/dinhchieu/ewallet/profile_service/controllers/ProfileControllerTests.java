package com.dinhchieu.ewallet.profile_service.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.dinhchieu.ewallet.common_library.security.filters.JwtBlacklistFilter;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.LinkedBankAccountLinkingRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileCreationRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileStatusUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.request.ProfileUpdateRequestDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileExistResponseDto;
import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileFullNameResponseDto;
import com.dinhchieu.ewallet.profile_service.services.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProfileController.class, excludeAutoConfiguration = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class ProfileControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProfileService profileService;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private RedisConnectionFactory redisConnectionFactory;

  @MockitoBean
  private RedisTemplate<String, Object> redisTemplate;

  @MockitoBean
  private CacheManager cacheManager;

  @MockitoBean
  private TokenBlacklistService tokenBlacklistService;

  @MockitoBean
  private JwtBlacklistFilter jwtBlacklistFilter;

  private MockedStatic<SecurityUtils> mockedSecurityUtils;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  public void testGetProfileById() throws Exception {
    ProfileFullNameResponseDto response = ProfileFullNameResponseDto.builder()
        .fullName("Full Name")
        .build();
    when(profileService.getProfileFullNameByUserId(userId)).thenReturn(response);

    mockMvc.perform(get("/fullname/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy thông tin cá nhân thành công"))
        .andExpect(jsonPath("$.data.fullName").value("Full Name"));
  }

  @Test
  @WithMockUser
  public void testGetMyProfile() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    when(profileService.getProfile(userId)).thenReturn(null);

    mockMvc.perform(get("/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy thông tin cá nhân thành công"));
  }

  @Test
  @WithMockUser
  public void testCreateMyProfile() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    ProfileCreationRequestDto request = ProfileCreationRequestDto.builder()
        .fullName("Test User")
        .phoneNumber("0123456789")
        .identityNumber("123456789")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();

    doNothing().when(profileService).createProfile(eq(userId), any(ProfileCreationRequestDto.class));

    mockMvc.perform(post("/me")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Tạo hồ sơ cá nhân thành công"));
  }

  @Test
  @WithMockUser
  public void testUpdateMyProfile() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
        .fullName("Updated Name")
        .phoneNumber("0987654321")
        .identityNumber("987654321")
        .dateOfBirth(LocalDate.of(1991, 2, 2))
        .build();

    doNothing().when(profileService).updateProfile(eq(userId), any(ProfileUpdateRequestDto.class));

    mockMvc.perform(put("/me")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ cá nhân thành công"));
  }

  @Test
  @WithMockUser
  public void testActivateMyProfile() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    doNothing().when(profileService).activateProfile(userId);

    mockMvc.perform(patch("/me/activate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Kích hoạt hồ sơ thành công"));
  }

  @Test
  @WithMockUser
  public void testGetMyLinkedBankAccounts() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    when(profileService.getMyLinkedBankAccounts(userId)).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/me/linked-bank-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy danh sách các tài khoản ngân hàng liên kết thành công"));
  }

  @Test
  @WithMockUser
  public void testLinkBankAccount() throws Exception {
    when(SecurityUtils.getAuthenticatedUserId()).thenReturn(userId);
    LinkedBankAccountLinkingRequestDto request = LinkedBankAccountLinkingRequestDto.builder()
        .bankCode("BANK")
        .accountNumber("1234567890")
        .build();

    doNothing().when(profileService).linkBankAccount(eq(userId), any(LinkedBankAccountLinkingRequestDto.class));

    mockMvc.perform(post("/me/linked-bank-accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Liên kết tài khoản ngân hàng thành công"));
  }

  @Test
  @WithMockUser
  public void testIsProfileExists() throws Exception {
    ProfileExistResponseDto response = ProfileExistResponseDto.builder()
        .exist(true)
        .build();
    when(profileService.isProfileExists(userId)).thenReturn(response);

    mockMvc.perform(get("/exists/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Kiểm tra tồn tại hồ sơ thành công"))
        .andExpect(jsonPath("$.data.exist").value(true));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testGetProfileByUserId() throws Exception {
    when(profileService.getProfileByUserId(userId)).thenReturn(null);

    mockMvc.perform(get("/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lấy thông tin cá nhân thành công"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testUpdateProfileStatus() throws Exception {
    ProfileStatusUpdateRequestDto request = new ProfileStatusUpdateRequestDto(ProfileStatus.ACTIVE);
    doNothing().when(profileService).updateProfileStatus(userId.toString(), ProfileStatus.ACTIVE);

    mockMvc.perform(patch("/admin/" + userId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Cập nhật trạng thái hồ sơ thành công"));
  }
}
