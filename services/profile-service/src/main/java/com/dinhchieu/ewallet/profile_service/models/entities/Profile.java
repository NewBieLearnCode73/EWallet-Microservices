package com.dinhchieu.ewallet.profile_service.models.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
public class Profile implements Serializable {
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
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate dateOfBirth;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ProfileStatus status = ProfileStatus.INACTIVE;

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
  private List<LinkedBankAccount> linkedBankAccounts;
}