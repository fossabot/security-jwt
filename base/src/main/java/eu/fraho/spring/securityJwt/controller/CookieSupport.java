/*
 * MIT Licence
 * Copyright (c) 2018 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.controller;

import eu.fraho.spring.securityJwt.config.CookieProperties;
import eu.fraho.spring.securityJwt.dto.AbstractToken;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public interface CookieSupport {
    default void addTokenCookieIfEnabled(HttpServletResponse response, @Nullable AbstractToken token, CookieProperties configuration) {
        if (configuration.isEnabled() && token != null) {
            Cookie cookie = new Cookie(configuration.getNames()[0], token.getToken());
            Optional.ofNullable(configuration.getDomain()).ifPresent(cookie::setDomain);
            Optional.ofNullable(configuration.getPath()).ifPresent(cookie::setPath);
            cookie.setSecure(configuration.isSecure());
            cookie.setHttpOnly(configuration.isHttpOnly());
            if (token.getExpiresIn() <= Integer.MAX_VALUE) {
                cookie.setMaxAge((int) token.getExpiresIn());
            }
            response.addCookie(cookie);
        }
    }
}
