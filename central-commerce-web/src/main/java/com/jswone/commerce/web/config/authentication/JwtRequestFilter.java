package com.jswone.commerce.web.config.authentication;

import com.commercetools.api.models.customer.Customer;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.model.auth.JwtUserContext;
import com.jswone.commerce.core.rest.AccountMasterClient;
import com.jswone.commerce.core.service.UserTokenService;

import com.jswone.commerce.core.util.AuthorityMapper;
import com.jswone.commerce.core.util.JSWCustomerUtil;
import com.jswone.commerce.core.util.JwtParserUtil;
import com.jswone.commons.util.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.*;
import java.util.UUID;

import static com.jswone.commerce.core.constants.JWTConstants.*;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final int SESSION_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 days
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenUtil jwtTokenUtil;
    private final UserTokenService userTokenService;
    private final CommerceValueConfig commerceValueConfig;
    private final JwtParserUtil jwtParserUtil;
    private final AuthorityMapper authorityMapper;
    private final JSWCustomerUtil customerDAO;
    private final AccountMasterClient accountMasterService;


    public JwtRequestFilter(JwtTokenUtil jwtTokenUtil, UserTokenService userTokenService,
                            CommerceValueConfig commerceValueConfig, JwtParserUtil jwtParserUtil,
                            AuthorityMapper authorityMapper, JSWCustomerUtil customerDAO,
                            AccountMasterClient accountMasterService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userTokenService = userTokenService;
        this.commerceValueConfig = commerceValueConfig;
        this.jwtParserUtil = jwtParserUtil;
        this.authorityMapper = authorityMapper;
        this.customerDAO = customerDAO;
        this.accountMasterService = accountMasterService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        AuthenticationMode authenticationMode = this.getAuthenticationMode(request);

        try {
            if (authenticationMode == AuthenticationMode.JWT) {
                handleJwtAuthentication(request, response);
            } else if (authenticationMode == AuthenticationMode.X_API) {
                handleApiKeyAuthentication(request, response);
            } else if (authenticationMode == AuthenticationMode.GUEST) {
                handleGuestAuthentication(request, response);
            }
        } catch (Exception e) {
            if (e instanceof UserTokenException ute) {
                log.error("Authentication token error: ", e);
                response.sendError(ute.getHttpStatus().value(), e.getMessage());
                return;
            } else if (e instanceof CentralCommerceServiceException cse) {
                log.error("Authentication error: ", e);
                response.sendError(cse.getHttpStatus().value(), e.getMessage());
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
            String userId = (String) claims.get(USER_ID_CLAIM);
            MDC.put(USER_ID_CLAIM,userId);
            MDC.put(SF_ID_CLAIM, (String) claims.getOrDefault(SF_ID_CLAIM,null));
            MDC.put(USER_TYPE_CLAIM, (String) claims.getOrDefault(USER_TYPE_CLAIM,null));

            if (!userTokenService.userTokenExists(jwtAccessToken)) {
                throw new UserTokenException(TOKEN_EXPIRE_MESSAGE, HttpStatus.UNAUTHORIZED);
            }
            // Use pre-validated claims to build the user context — avoids re-decoding without signature verification
            JwtUserContext userContext = jwtParserUtil.extractUserContextFromClaims(claims);
            if (userContext == null) {
                throw new UserTokenException(INVALID_TOKEN_MESSAGE, HttpStatus.UNAUTHORIZED);
            }
            if("R".equalsIgnoreCase(userContext.getUserType()) && CollectionUtils.isEmpty(userContext.getPermissions())){
                Customer customer = customerDAO.getCustomerById(userContext.getUserId());
                if(Objects.nonNull(customer) && Objects.nonNull(customer.getId())){
                    Map<String,String> permissions = accountMasterService.getAdminPermissionMap();
                    userContext.setPermissions(permissions);
                }else {
                    throw new CentralCommerceServiceException(USER_NOT_PRESENT_MESSAGE, HttpStatus.UNAUTHORIZED);
                }
            }
            Collection<GrantedAuthority> authorities = authorityMapper.mapPermissions(userContext.getPermissions());
            // Retaining your fallback logic for now
            if (authorities == null || authorities.isEmpty()) {
                authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("REGUSER"));
            }
            // Set JwtUserContext as the secure Principal natively in Spring
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userContext, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            addTokenValuesToRequestAttributes(request, claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private void handleGuestAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        boolean cookiePresent = false;

        if (StringUtils.isBlank(sessionId) && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_ID_COOKIE_NAME.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    cookiePresent = true;
                    break;
                }
            }
        }

        if (StringUtils.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }

        if (!cookiePresent) {
            Cookie sessionCookie = new Cookie(SESSION_ID_COOKIE_NAME, sessionId);
            sessionCookie.setHttpOnly(true);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(SESSION_COOKIE_MAX_AGE);
            response.addCookie(sessionCookie);
        }

        MDC.put(USER_ID_CLAIM, sessionId);
        MDC.put(USER_TYPE_CLAIM, GUEST_USER_TYPE);

        //If token is null auth util consider it as guest user
        JwtUserContext userContext = jwtParserUtil.extractUserContext(null, sessionId);
        // Set JwtUserContext as the secure Principal natively in Spring
        Collection<GrantedAuthority> authorities = authorityMapper.mapPermissions(userContext.getPermissions());
        // Retaining your fallback logic for now
        if (authorities == null || authorities.isEmpty()) {
            authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("GUEST"));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userContext, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
                JwtUserContext userContext = jwtParserUtil.extractUserContext(null, userId);
                // Set JwtUserContext as the secure Principal natively in Spring
                Collection<GrantedAuthority> authorities =
                        authorityMapper.mapPermissions(userContext.getPermissions());
                // Retaining your fallback logic for now
                if (authorities == null || authorities.isEmpty()) {
                    authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("GUEST"));
                }

                // Set JwtUserContext as the secure Principal natively in Spring
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userContext, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
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
                .anyMatch(p -> PATH_MATCHER.match(p, request.getRequestURI()));
    }

    private AuthenticationMode getAuthenticationMode(HttpServletRequest request) {
        if (StringUtils.isNotBlank(request.getHeader(X_API_KEY))) {
            return AuthenticationMode.X_API;
        } else if (StringUtils.isNotBlank(request.getHeader(HttpHeaders.AUTHORIZATION))
                || StringUtils.isNotBlank(request.getHeader(ACCESS_TOKEN))) {
            return AuthenticationMode.JWT;
        } else {
            return AuthenticationMode.GUEST;
        }
    }
}
