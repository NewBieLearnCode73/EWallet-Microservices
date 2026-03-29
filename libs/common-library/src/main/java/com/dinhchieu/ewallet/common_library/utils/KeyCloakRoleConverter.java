package com.dinhchieu.ewallet.common_library.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KeyCloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @SuppressWarnings("unchecked")
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Collection<String> roles = (Collection<String>) ((Map<String, Object>) jwt.getClaims()
        .getOrDefault("realm_access", Collections.emptyMap()))
        .getOrDefault("roles", Collections.emptyList());

    log.info("Roles extracted from JWT: {}", roles);

    return roles.stream().map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
        .collect(Collectors.toList());
  }

}