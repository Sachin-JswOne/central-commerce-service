package com.jswone.commerce.web.config.authentication;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.service.UserTokenService;
import com.jswone.commerce.core.util.JwtTokenUtil;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.*;

import static com.jswone.commerce.core.constants.JWTConstants.*;

@Component
@Log4j2
public class JwtRequestFilter extends OncePerRequestFilter {

    private JwtTokenUtil jwtTokenUtil;
    private final com.jswone.commerce.core.service.UserTokenService userTokenService;
    private final CommerceValueConfig commerceValueConfig;


    public JwtRequestFilter(JwtTokenUtil jwtTokenUtil, UserTokenService userTokenService, CommerceValueConfig commerceValueConfig) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userTokenService = userTokenService;
        this.commerceValueConfig = commerceValueConfig;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        AuthenticationMode authenticationMode = this.getAuthenticationMode(request);

        if (authenticationMode == null) {
            log.error("Authentication mode is null for request: {}", request.getRequestURI());
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Authentication header is missing in the request");
            return;
        }

        try {
            if (authenticationMode == AuthenticationMode.JWT) {
                handleJwtAuthentication(request, response);
            } else if (authenticationMode == AuthenticationMode.X_API) {
                handleApiKeyAuthentication(request, response);
            }
        } catch (Exception e) {
            if (Objects.nonNull(e.getMessage()) && e.getMessage().contains(TOKEN_EXPIRE_MESSAGE)
                    || Objects.nonNull(e.getMessage())
                            && e.getMessage().contains(INVALID_TOKEN_MESSAGE)
                    || Objects.nonNull(e.getMessage())
                            && e.getMessage().contains(TOKEN_NOT_PRESENT_MESSAGE)) {
                log.error("Unexpected error during authentication: ", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }
            log.error("Unexpected error during authentication: ", e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            return;
        }

        chain.doFilter(request, response);
    }

    private void handleJwtAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String jwtAccessToken = extractJWTTokenFromRequest(request);
        if (jwtAccessToken == null) {
            throw new UserTokenException(TOKEN_NOT_PRESENT_MESSAGE, HttpStatus.UNAUTHORIZED);
        }

        try {
            Map<?, ?> claims = jwtTokenUtil.validateAndGetAllClaimsFromToken(jwtAccessToken);
            if (claims == null || !claims.containsKey(USER_ID_CLAIM)) {
                throw new UserTokenException(INVALID_TOKEN_MESSAGE, HttpStatus.UNAUTHORIZED);
            }

            if (!userTokenService.userTokenExists(jwtAccessToken)) {
                throw new UserTokenException(TOKEN_EXPIRE_MESSAGE, HttpStatus.UNAUTHORIZED);
            }

            String userId = (String) claims.get(USER_ID_CLAIM);
            UserDetails userDetails = new User(userId, jwtAccessToken, getAuthorities());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            addTokenValuesToRequestAttributes(request, claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private void handleApiKeyAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        String xApiKey = request.getHeader(X_API_KEY);
        if (StringUtils.isEmpty(xApiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key cannot be empty");
            return;
        }

        if (xApiKey.equals(commerceValueConfig.getX_API_KEY_COMMERCE_SERVICE())) {
            List<String> apiKeyParts = Arrays.asList(commerceValueConfig.getX_API_KEY_COMMERCE_SERVICE().split("-"));
            if (apiKeyParts.size() >= 3) {
                String userId = apiKeyParts.get(0);
                String token = apiKeyParts.get(2);
                UserDetails userDetails = new User(userId, token, getAuthorities());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                addTokenValuesToRequestAttributes(request, new HashMap<>());
            } else {
                log.error("Invalid API key format");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
        }
    }

    private String extractJWTTokenFromRequest(HttpServletRequest request) {
        String jwtAccessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (jwtAccessToken != null) {
            jwtAccessToken = (jwtAccessToken.length() > 7) ? jwtAccessToken.substring(7) : null;
        } else {
            jwtAccessToken = request.getHeader(ACCESS_TOKEN);
        }
        return jwtAccessToken;
    }

    private Collection<? extends GrantedAuthority> getAuthorities() {
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList();
        authorities.add(new SimpleGrantedAuthority("REGUSER"));
        // add actual authorities when RBAC implemented
        return authorities;
    }

    private void addTokenValuesToRequestAttributes(HttpServletRequest request, Map claims) {
        if (claims.containsKey(CORRELATION_ID_CLAIM)) {
            request.setAttribute(
                    CORRELATION_ID_CLAIM,
                    claims.get(CORRELATION_ID_CLAIM));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return excludeUrlPatterns.stream()
                .anyMatch(p -> new AntPathMatcher().match(p, request.getRequestURI()));
    }

    private AuthenticationMode getAuthenticationMode(HttpServletRequest request) {
        if (request.getHeader(X_API_KEY) != null) {
            return AuthenticationMode.X_API;
        } else if (request.getHeader(ACCESS_TOKEN) != null) {
            return AuthenticationMode.JWT;
        } else {
            return null;
        }
    }
}
