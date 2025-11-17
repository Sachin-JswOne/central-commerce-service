package com.jswone.commerce.web.config.advice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.UUID;

@Component
public class TracingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String randomId = UUID.randomUUID().toString();
        String traceId = request.getHeader("trace_id");
        if (traceId == null || traceId.equals("")) {
            traceId = randomId;
        }

        MDC.put("traceId", traceId);
        MDC.put("spanId", randomId);
        filterChain.doFilter(request, response);
    }
}
