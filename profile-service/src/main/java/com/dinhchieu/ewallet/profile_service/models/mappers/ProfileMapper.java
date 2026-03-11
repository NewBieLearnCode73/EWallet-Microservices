package com.dinhchieu.ewallet.profile_service.models.mappers;

import org.mapstruct.Mapper;

import com.dinhchieu.ewallet.profile_service.models.dtos.response.ProfileResponseDto;
import com.dinhchieu.ewallet.profile_service.models.entities.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
  Profile toEntity(ProfileResponseDto dto);

  ProfileResponseDto toDto(Profile entity);

}
