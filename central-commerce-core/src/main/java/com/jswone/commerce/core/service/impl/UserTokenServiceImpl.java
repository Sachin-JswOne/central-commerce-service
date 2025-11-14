package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.UserTokenEntity;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.repository.UserTokenRepository;
import com.jswone.commerce.core.service.UserTokenService;
import com.jswone.commerce.core.util.JwtTokenUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class UserTokenServiceImpl implements UserTokenService {

    private final UserTokenRepository userTokenRepository;
    private final JwtTokenUtil jwtAuthTokenUtil;


    public UserTokenServiceImpl(UserTokenRepository userTokenRepository,
                                JwtTokenUtil jwtAuthTokenUtil) {
        this.userTokenRepository = userTokenRepository;
        this.jwtAuthTokenUtil = jwtAuthTokenUtil;
    }

    @Override
    public boolean saveUserToken(UserTokenEntity userTokenEntity) {
        log.info("Saving user token for userId: {}", userTokenEntity.getCustomerId());

        try {
            // Firestore save() returns Mono<UserTokenEntity>
            userTokenRepository.save(userTokenEntity).block();
            return true;

        } catch (Exception e) {
            log.error("Error saving user token entity in Firestore", e);
            throw new UserTokenException("Error saving user token entity in Firestore", e);
        }
    }

    @Override
    public boolean userTokenExists(String jwtToken) {
        try {
            // Generate hash — this becomes the Firestore doc ID
            String jwtTokenHash = jwtAuthTokenUtil.generateJWTHash(jwtToken);

            if (jwtTokenHash == null) {
                return false;
            }

            Boolean exists = userTokenRepository.existsById(jwtTokenHash).block();
            return exists != null && exists;

        } catch (Exception e) {
            log.error("Exception during JWT token reuse validation", e);
            throw new UserTokenException("Exception during JWT token reuse validation", e);
        }
    }

    @Override
    public void deleteUserToken(UserTokenEntity userTokenEntity) {
        log.info("Deleting user token for userId: {}", userTokenEntity.getCustomerId());

        try {
            userTokenRepository.deleteById(userTokenEntity.getTokenHash()).block();

        } catch (Exception e) {
            log.error("Error deleting token for userId: {}", userTokenEntity.getCustomerId(), e);
            throw new UserTokenException(
                    "Error deleting token for userId: " + userTokenEntity.getCustomerId(), e);
        }
    }
}
