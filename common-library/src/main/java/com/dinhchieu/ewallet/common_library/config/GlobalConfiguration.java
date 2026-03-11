package com.dinhchieu.ewallet.common_library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.dinhchieu.ewallet.common_library.utils.KeyCloakRoleConverter;

@Configuration
public class GlobalConfiguration {
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public KeyCloakRoleConverter keycloakRoleConverter() {
    return new KeyCloakRoleConverter();
  }
}
