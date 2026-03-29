package com.dinhchieu.ewallet.common_library.enums;

import lombok.Getter;

@Getter
public enum ProfileStatus {

  ACTIVE("ACTIVE", "Tài khoản đã được kích hoạt"),
  INACTIVE("INACTIVE", "Tài khoản chưa được kích hoạt"),
  SUSPENDED("SUSPENDED", "Tài khoản bị treo"),
  LOCKED("LOCKED", "Tài khoản bị khóa");

  private final String value;
  private final String description;

  ProfileStatus(String value, String description) {
    this.value = value;
    this.description = description;
  }
}
