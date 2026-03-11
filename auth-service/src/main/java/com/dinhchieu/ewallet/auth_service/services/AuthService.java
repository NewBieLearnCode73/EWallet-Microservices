package com.dinhchieu.ewallet.auth_service.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
  /**
   * Register a new user
   * 
   * @param username the username
   * @param password the password
   * @param email    the email
   */
  void register(String username, String password, String email);

  /**
   * Login a user and set access token and refresh token in HttpOnly cookies
   * 
   * @param username the username
   * @param password the password
   * @param response the HttpServletResponse to set cookies
   */
  void login(String username, String password, HttpServletResponse response);

  /**
   * Logout a user by clearing the access token and refresh token cookies
   * 
   * @param request  the HttpServletRequest to read cookies
   * @param response the HttpServletResponse to clear cookies
   */
  void logout(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse);

  /**
   * Refresh the access token using the refresh token from cookies and set the new
   * access token in HttpOnly cookies
   * 
   * @param request
   * @param response
   */
  void refreshToken(HttpServletRequest request, HttpServletResponse response);

  /**
   * Revoke all sessions for the currently authenticated user, effectively logging
   * them out from all devices.
   * 
   * @param request the HttpServletRequest to identify the user and their sessions
   */
  void revokeAllSessions(HttpServletRequest request);

}
