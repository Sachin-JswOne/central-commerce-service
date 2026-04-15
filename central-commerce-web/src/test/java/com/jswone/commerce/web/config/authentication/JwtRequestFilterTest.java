package com.jswone.commerce.web.config.authentication;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.service.UserTokenService;
import com.jswone.commons.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
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
import static org.mockito.Mockito.mock;
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

    private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() {
        jwtRequestFilter = new JwtRequestFilter(jwtTokenUtil, userTokenService, commerceValueConfig);
        clearSecurityContext();
    }

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Test
    void shouldCreateGuestSessionWhenNoAuthHeadersPresent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtRequestFilter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertTrue(isValidUuid(MDC.get(USER_ID_CLAIM)));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("GUEST", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals(MDC.get(USER_ID_CLAIM), ((User) authentication.getPrincipal()).getUsername());
        verifyNoInteractions(jwtTokenUtil, userTokenService, commerceValueConfig);
    }

    @Test
    void shouldUseProvidedSessionIdForGuestAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        request.addHeader(SESSION_ID_HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtRequestFilter.doFilterInternal(request, response, mock(FilterChain.class));

        assertEquals(200, response.getStatus());
        assertEquals("abc-123", MDC.get(USER_ID_CLAIM));
        assertEquals(GUEST_USER_TYPE, MDC.get(USER_TYPE_CLAIM));
        assertEquals(
                "abc-123",
                ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
    }

    @Test
    void shouldAuthenticateJwtRequestFromAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> claims = Map.of(
                USER_ID_CLAIM, "user-123",
                USER_TYPE_CLAIM, "R",
                SF_ID_CLAIM, "sf-456");

        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, mock(FilterChain.class));

        assertEquals(200, response.getStatus());
        assertEquals("user-123", MDC.get(USER_ID_CLAIM));
        assertEquals("R", MDC.get(USER_TYPE_CLAIM));
        assertEquals("sf-456", MDC.get(SF_ID_CLAIM));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("REGUSER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("user-123", ((User) authentication.getPrincipal()).getUsername());
    }

    @Test
    void shouldKeepApiKeyAuthenticationFlowUnchanged() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        request.addHeader(X_API_KEY, "commerce-service-v1-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(commerceValueConfig.getX_API_KEY_COMMERCE_SERVICE()).thenReturn("commerce-service-v1-token");

        jwtRequestFilter.doFilterInternal(request, response, mock(FilterChain.class));

        assertEquals(200, response.getStatus());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("REGUSER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("commerce", ((User) authentication.getPrincipal()).getUsername());
        assertFalse(MDC.getCopyOfContextMap() != null && MDC.getCopyOfContextMap().containsKey(USER_ID_CLAIM));
        verifyNoInteractions(jwtTokenUtil, userTokenService);
    }

    @Test
    void shouldAuthenticateJwtRequestFromAccessTokenHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        request.addHeader(ACCESS_TOKEN, "jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> claims = Map.of(USER_ID_CLAIM, "user-789", USER_TYPE_CLAIM, "R");

        when(jwtTokenUtil.validateAndGetAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(userTokenService.userTokenExists("jwt-token")).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, mock(FilterChain.class));

        assertEquals(200, response.getStatus());
        assertEquals("user-789", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
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
