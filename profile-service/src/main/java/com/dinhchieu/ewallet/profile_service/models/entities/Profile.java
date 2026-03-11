package com.dinhchieu.ewallet.profile_service.models.entities;

import java.time.LocalDate;
import java.util.UUID;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(name = "profiles")
public class Profile {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "phone_number", nullable = false, unique = true)
  private String phoneNumber;

  @Column(name = "identity_number", nullable = false, unique = true)
  private String identityNumber;

  @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ProfileStatus status = ProfileStatus.INACTIVE;
}