package com.jswone.commerce.web.config.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CookieFilterTest {

    private final CookieFilter cookieFilter = new CookieFilter();

    @Test
    void shouldKeepSessionCookieWhileDeletingOthers() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalogue/products");
        request.setCookies(new Cookie("jsw_session_id", "session-123"), new Cookie("legacy_cookie", "legacy"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        cookieFilter.doFilterInternal(request, response, chain);

        assertEquals(1, response.getCookies().length);
        assertEquals("legacy_cookie", response.getCookies()[0].getName());
        assertEquals(0, response.getCookies()[0].getMaxAge());
        assertFalse(Arrays.stream(response.getCookies()).anyMatch(cookie -> "jsw_session_id".equals(cookie.getName())));
        assertTrue(Arrays.stream(request.getCookies()).anyMatch(cookie -> "jsw_session_id".equals(cookie.getName())));
    }
}
