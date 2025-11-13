package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Log4j2
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -2550185165626007488L;

    @Value("${jwt.secret}")
    private String secret;

    public String getJWTTokenForSession() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String jwtToken = null;
        if (principal instanceof UserDetails) {
            jwtToken = ((UserDetails) principal).getPassword();
        }

        return jwtToken;
    }

    public Claims validateAndGetAllClaimsFromToken(String token)
            throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException,
            SignatureException, IllegalArgumentException {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    public static String getUserIdForSession() {
        try {
            UserDetails principal =
                    (UserDetails)
                            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal.getUsername().contains("Commerce")) {
                throw new CentralCommerceServiceException(
                        "JWT validation failed : Please provide access token instead of X-API-KEY");
            }
            return principal.getUsername();
        } catch (Exception e) {
            throw new CentralCommerceServiceException(e.getLocalizedMessage());
        }
    }
}
