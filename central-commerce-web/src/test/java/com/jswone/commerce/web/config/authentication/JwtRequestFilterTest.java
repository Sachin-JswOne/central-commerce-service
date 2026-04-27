package com.jswone.commerce.web.config.authentication;

import com.commercetools.api.models.customer.Customer;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.model.auth.JwtUserContext;
import com.jswone.commerce.core.rest.AccountMasterClient;
import com.jswone.commerce.core.service.UserTokenService;
import com.jswone.commerce.core.util.AuthorityMapper;
import com.jswone.commerce.core.util.JSWCustomerUtil;
import com.jswone.commerce.core.util.JwtParserUtil;
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
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private JwtParserUtil jwtParserUtil;

    @Mock
    private AuthorityMapper authorityMapper;

    @Mock
    private JSWCustomerUtil customerDAO;

    @Mock
    private AccountMasterClient accountMasterService;

    private JwtRequestFilter jwtRequestFilter;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        jwtRequestFilter = new JwtRequestFilter(jwtTokenUtil, userTokenService, commerceValueConfig, jwtParserUtil, authorityMapper, customerDAO, accountMasterService);
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
        JwtUserContext jwtUserContext = new JwtUserContext();
        jwtUserContext.setUserType("G");
        jwtUserContext.setStoreKey("msme");
        jwtUserContext.setSfUserId("sf-456");
        jwtUserContext.setUserId("user-123");
        when(jwtParserUtil.extractUserContext(isNull(), anyString())).thenReturn(jwtUserContext);
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertTrue(isValidUuid(MDC.get(USER_ID_CLAIM)));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("GUEST", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("user-123", ((JwtUserContext) authentication.getPrincipal()).getUserId());
        verifyNoInteractions(jwtTokenUtil, userTokenService, commerceValueConfig);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldUseProvidedSessionIdForGuestAuthentication() throws ServletException, IOException {
        headers.put(SESSION_ID_HEADER, "abc-123");
        JwtUserContext jwtUserContext = new JwtUserContext();
        jwtUserContext.setUserType("G");
        jwtUserContext.setUserId("abc-123");

        when(jwtParserUtil.extractUserContext(any(), any()))
                .thenReturn(jwtUserContext);
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("abc-123", MDC.get(USER_ID_CLAIM));
        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertEquals("abc-123", ((JwtUserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateJwtRequestFromAuthorizationHeader() throws ServletException, IOException {
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
        Claims claims = new DefaultClaims();
        claims.put(USER_ID_CLAIM, "user-123");
        claims.put(SF_ID_CLAIM, "sf-456");
        claims.put(USER_TYPE_CLAIM, "G");

        JwtUserContext jwtUserContext = new JwtUserContext();
        jwtUserContext.setUserType("G");
        jwtUserContext.setStoreKey("msme");
        jwtUserContext.setSfUserId("sf-456");
        jwtUserContext.setUserId("user-123");

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer jwt-token");
        when(request.getAttribute(USER_ID_CLAIM)).thenReturn("user-123");
        when(request.getAttribute(SF_ID_CLAIM)).thenReturn("sf-456");
        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);
        when(jwtParserUtil.extractUserContext(any(), any())).thenReturn(jwtUserContext);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("user-123", request.getAttribute(USER_ID_CLAIM));
        assertEquals("sf-456", request.getAttribute(SF_ID_CLAIM));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("REGUSER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("user-123", ((JwtUserContext) authentication.getPrincipal()).getUserId());
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
        claims.put(USER_TYPE_CLAIM, "G");

        JwtUserContext jwtUserContext = new JwtUserContext();
        jwtUserContext.setUserType("G");
        jwtUserContext.setStoreKey("msme");
        jwtUserContext.setUserId("user-789");

        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);
        when(jwtParserUtil.extractUserContext(any(), any())).thenReturn(jwtUserContext);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertEquals("user-789", ((JwtUserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId());
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
