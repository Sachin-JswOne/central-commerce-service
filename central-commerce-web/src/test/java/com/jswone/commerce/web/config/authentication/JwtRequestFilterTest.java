package com.jswone.commerce.web.config.authentication;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.service.UserTokenService;
import com.jswone.commons.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jswone.commerce.core.constants.JWTConstants.ACCESS_TOKEN;
import static com.jswone.commerce.core.constants.JWTConstants.GUEST_USER_TYPE;
import static com.jswone.commerce.core.constants.JWTConstants.SESSION_ID_HEADER;
import static com.jswone.commerce.core.constants.JWTConstants.SF_ID_CLAIM;
import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;
import static com.jswone.commerce.core.constants.JWTConstants.USER_TYPE_CLAIM;
import static com.jswone.commerce.core.constants.JWTConstants.X_API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserTokenService userTokenService;

    @Mock
    private CommerceValueConfig commerceValueConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtRequestFilter jwtRequestFilter;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        jwtRequestFilter = new JwtRequestFilter(jwtTokenUtil, userTokenService, commerceValueConfig);
        clearSecurityContext();
        headers = new HashMap<>();
        when(request.getHeader(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> headers.get(invocation.getArgument(0, String.class)));
    }

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Test
    void shouldCreateGuestSessionWhenNoAuthHeadersPresent() throws ServletException, IOException {
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertTrue(isValidUuid(MDC.get(USER_ID_CLAIM)));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("GUEST", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals(MDC.get(USER_ID_CLAIM), ((User) authentication.getPrincipal()).getUsername());
        verifyNoInteractions(jwtTokenUtil, userTokenService, commerceValueConfig);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldUseProvidedSessionIdForGuestAuthentication() throws ServletException, IOException {
        headers.put(SESSION_ID_HEADER, "abc-123");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("abc-123", MDC.get(USER_ID_CLAIM));
        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertEquals(
                "abc-123",
                ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateJwtRequestFromAuthorizationHeader() throws ServletException, IOException {
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
        Claims claims = new DefaultClaims();
        claims.put(USER_ID_CLAIM, "user-123");
        claims.put(USER_TYPE_CLAIM, "R");
        claims.put(SF_ID_CLAIM, "sf-456");

        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("user-123", MDC.get(USER_ID_CLAIM));
        assertEquals("R", MDC.get(USER_TYPE_CLAIM));
        assertEquals("sf-456", MDC.get(SF_ID_CLAIM));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("REGUSER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("user-123", ((User) authentication.getPrincipal()).getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldKeepApiKeyAuthenticationFlowUnchanged() throws ServletException, IOException {
        headers.put(X_API_KEY, "commerce-service-v1-token");

        when(commerceValueConfig.getX_API_KEY_COMMERCE_SERVICE()).thenReturn("commerce-service-v1-token");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("REGUSER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("commerce", ((User) authentication.getPrincipal()).getUsername());
        assertFalse(MDC.getCopyOfContextMap() != null && MDC.getCopyOfContextMap().containsKey(USER_ID_CLAIM));
        verifyNoInteractions(jwtTokenUtil, userTokenService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateJwtRequestFromAccessTokenHeader() throws ServletException, IOException {
        headers.put(ACCESS_TOKEN, "jwt-token");
        Claims claims = new DefaultClaims();
        claims.put(USER_ID_CLAIM, "user-789");
        claims.put(USER_TYPE_CLAIM, "R");

        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("user-789", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
        verify(filterChain).doFilter(request, response);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
