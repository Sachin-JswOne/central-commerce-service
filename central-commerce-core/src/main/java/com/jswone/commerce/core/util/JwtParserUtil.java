package com.jswone.commerce.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.model.auth.JwtUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtParserUtil {

    private final ObjectMapper objectMapper;

    /**
     * Extracts the user context from a JWT token.
     * @param token The raw JWT token string
     * @return JwtUserContext
     */
    public JwtUserContext extractUserContext(String token, String sessionId) {
        if (token == null || token.isEmpty()) {
            JwtUserContext jwtUserContext = new JwtUserContext();
            jwtUserContext.setUserType("G");
            jwtUserContext.setStoreKey("msme");
            jwtUserContext.setUserId(sessionId);
            jwtUserContext.setAnonymous(true);
            return jwtUserContext;
        }
        
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, JwtUserContext.class);
            
        } catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            throw new IllegalArgumentException("Invalid JWT token payload", e);
        }
    }

    /**
     * Builds a JwtUserContext from pre-validated claims returned by JwtTokenUtil.
     * Use this instead of extractUserContext(token, ...) for JWT paths so the payload
     * is never decoded a second time without signature verification.
     */
    public JwtUserContext extractUserContextFromClaims(Map<?, ?> validatedClaims) {
        try {
            return objectMapper.convertValue(validatedClaims, JwtUserContext.class);
        } catch (Exception e) {
            log.error("Failed to map validated JWT claims to user context", e);
            throw new IllegalArgumentException("Failed to map JWT claims to user context", e);
        }
    }

    /**
     * Extracts permissions map from the JWT token.
     * @param token The raw JWT token string
     * @return Map of permissions
     */
    public Map<String, String> extractPermissions(String token) {
        JwtUserContext context = extractUserContext(token, null);
        if (context.getPermissions() == null) {
            return Collections.emptyMap();
        }
        return context.getPermissions();
    }
}
