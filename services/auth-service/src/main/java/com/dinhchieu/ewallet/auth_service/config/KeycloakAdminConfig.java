package com.dinhchieu.ewallet.auth_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {
  @Value("${keycloak.admin-client.server-url}")
  private String serverUrl;
  @Value("${keycloak.admin-client.realm}")
  private String realm;
  @Value("${keycloak.admin-client.client-id}")
  private String clientId;
  @Value("${keycloak.admin-client.client-secret}")
  private String clientSecret;

  /**
   * Bean support for Keycloak Admin Client, which allows the application to
   * interact with Keycloak's admin REST API.
   * 
   * @return a configured Keycloak instance for admin operationss.
   */

  @Bean
  public Keycloak keycloak() {
    return KeycloakBuilder.builder().serverUrl(serverUrl).realm(realm)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .build();
  }
}
