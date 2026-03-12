package com.dinhchieu.ewallet.auth_service.clients;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLoginRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakLogoutRequestDto;
import com.dinhchieu.ewallet.auth_service.models.dtos.request.KeyCloakRefreshTokenRequestDto;

@FeignClient(name = "keycloak-client", url = "${keycloak.admin-client.server-url}")
public interface KeyCloakClient {

        @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        public Map<String, Object> login(
                        @PathVariable("realm") String realm,
                        @RequestBody KeyCloakLoginRequestDto params);

        @PostMapping(value = "/realms/{realm}/protocol/openid-connect/logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        public Map<String, Object> logout(
                        @PathVariable("realm") String realm,
                        @RequestBody KeyCloakLogoutRequestDto params);

        @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        public Map<String, Object> refreshToken(
                        @PathVariable("realm") String realm,
                        @RequestBody KeyCloakRefreshTokenRequestDto params);
}
