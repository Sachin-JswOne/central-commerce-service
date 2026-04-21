package com.jswone.commerce.web.config.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookieFilterTest {

    private final CookieFilter cookieFilter = new CookieFilter();

    @Test
    void shouldKeepSessionCookieWhileDeletingOthers() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("jsw_session_id", "session-123"),
                new Cookie("legacy_cookie", "legacy")
        });

        cookieFilter.doFilterInternal(request, response, chain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(1)).addCookie(cookieCaptor.capture());
        verify(chain).doFilter(request, response);

        List<Cookie> deletedCookies = cookieCaptor.getAllValues();
        assertEquals(1, deletedCookies.size());
        assertEquals("legacy_cookie", deletedCookies.getFirst().getName());
        assertEquals(0, deletedCookies.getFirst().getMaxAge());
        assertTrue(deletedCookies.stream().noneMatch(cookie -> "jsw_session_id".equals(cookie.getName())));
    }
}
