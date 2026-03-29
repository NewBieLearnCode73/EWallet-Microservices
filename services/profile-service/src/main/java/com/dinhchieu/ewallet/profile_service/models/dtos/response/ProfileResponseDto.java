package com.dinhchieu.ewallet.profile_service.models.dtos.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.dinhchieu.ewallet.common_library.enums.ProfileStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponseDto implements Serializable {
  private UUID id;
  private String fullName;
  private String phoneNumber;
  private String identityNumber;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate dateOfBirth;
  private ProfileStatus status;
}
