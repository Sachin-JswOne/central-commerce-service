package com.jswone.commerce.core.util;

//import com.google.cloud.datastore.Key;
//import com.jswone.commerce.core.constant.JWTConstants;
import com.jswone.commerce.core.constant.JWTConstants;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Base64;

@Component
@Log4j2
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -2550185165626007488L;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${spring.cloud.gcp.datastore.project-id}")
    private String projectId;

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

    public String generateJWTHash(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(JWTConstants.SHA_512);
            byte[] hash = messageDigest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("error while generating token hash. error : {}", e);
            return null;
        }
    }


}
