package com.dinhchieu.ewallet.common_library.security.filters;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.security.services.TokenBlacklistService;
import com.dinhchieu.ewallet.common_library.utils.CookieUtils;
import com.dinhchieu.ewallet.common_library.utils.KeyCloakRoleConverter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtBlacklistFilter extends OncePerRequestFilter {
  @Autowired
  private TokenBlacklistService blacklistService;
  @Autowired
  private JwtDecoder jwtDecoder;
  @Autowired
  private HandlerExceptionResolver handlerExceptionResolver;
  @Autowired
  private KeyCloakRoleConverter keyCloakRoleConverter;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Cookie accessTokenCookie = WebUtils.getCookie(request, "access_token");

    if (accessTokenCookie != null && !accessTokenCookie.getValue().isEmpty()) {
      try {
        String tokenValue = accessTokenCookie.getValue();
        Jwt jwt = jwtDecoder.decode(tokenValue);
        String jti = jwt.getId();

        log.info("Checking token in blacklist: {}", jti);

        if (blacklistService.isBlacklisted(jti)) {
          log.warn("Blocked blacklisted token: {}", jti);
          CookieUtils.clearAuthCookies(response);
          handlerExceptionResolver.resolveException(request, response, null,
              new AppException(ErrorCode.TOKEN_INVALID));
          return;
        }

        Collection<GrantedAuthority> authorities = keyCloakRoleConverter.convert(jwt);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(jwt, null,
            authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (Exception e) {
        log.error("Token validation failed: {}", e.getMessage());
        SecurityContextHolder.clearContext();
        handlerExceptionResolver.resolveException(request, response, null,
            new AppException(ErrorCode.TOKEN_INVALID));
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
