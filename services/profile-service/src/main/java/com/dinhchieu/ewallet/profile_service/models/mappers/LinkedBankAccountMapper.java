package com.dinhchieu.ewallet.profile_service.models.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.dinhchieu.ewallet.profile_service.models.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.profile_service.models.entities.LinkedBankAccount;

@Mapper(componentModel = "spring")
public interface LinkedBankAccountMapper {
  List<LinkedBankAccountsReponseDto> toLinkedBankAccountsResponse(List<LinkedBankAccount> linkedBankAccounts);
}
