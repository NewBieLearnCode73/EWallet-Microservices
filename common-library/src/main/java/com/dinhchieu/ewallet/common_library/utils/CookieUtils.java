package com.dinhchieu.ewallet.common_library.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CookieUtils {

  /**
   * Helper method to clear authentication cookies
   * 
   * @param response the HttpServletResponse to clear cookies
   */

  public static void clearAuthCookies(HttpServletResponse response) {
    Cookie accessTokenCookie = setCookie("access_token", null, 0, true, "/");
    Cookie refreshTokenCookie = setCookie("refresh_token", null, 0, true, "/");
    response.addCookie(accessTokenCookie);
    response.addCookie(refreshTokenCookie);
  }

  /**
   * Helper method to set a cookie
   * 
   * @param name     the name of the cookie
   * @param value    the value of the cookie
   * @param maxAge   the maximum age of the cookie
   * @param httpOnly whether the cookie is HTTP-only
   * @param path     the path of the cookie
   * @return the created Cookie object
   */
  public static Cookie setCookie(String name, String value, int maxAge, boolean httpOnly, String path) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(httpOnly);
    cookie.setPath(path);
    cookie.setMaxAge(maxAge);
    return cookie;
  }
}
