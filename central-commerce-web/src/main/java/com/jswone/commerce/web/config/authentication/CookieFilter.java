package com.jswone.commerce.web.config.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.Objects;

import static com.jswone.commerce.core.constants.JWTConstants.SESSION_ID_COOKIE_NAME;

@Component
@Slf4j
public class CookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,@NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.info(" cookie filter chain called ..");
        if (Objects.nonNull(request.getCookies())){
            for (Cookie cookie : request.getCookies()) {
                String cookieName = cookie.getName();
                if (SESSION_ID_COOKIE_NAME.equals(cookieName)) {
                    continue;
                }
                Cookie cookieToDelete = new Cookie(cookieName, "");
                cookieToDelete.setMaxAge(0);
                response.addCookie(cookieToDelete);
            }
        }
        else {
            log.info("No cookies to delete. Request Cookies are null");
        }
        filterChain.doFilter(request, response);
    }
}
