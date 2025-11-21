package com.jswone.commerce.web.config.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.Objects;

@Component
@Log4j2
public class CookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info(" cookie filter chain called ..");
        if (Objects.nonNull(request.getCookies())){
            for (Cookie cookie : request.getCookies()) {
                String cookieName = cookie.getName();
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
