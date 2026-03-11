package com.dinhchieu.ewallet.common_library.utils;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtils {

  /**
   * Get the authenticated user's ID from the security context. This method
   * assumes that the user is authenticated
   * and that the JWT token contains a valid UUID in the "sub" claim. If the user
   * is not authenticated or if the token is invalid, an AppException with the
   * appropriate error code will be thrown.
   * 
   * @return the authenticated user's ID as a UUID
   */
  public static UUID getAuthenticatedUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException e) {
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
  }
}
