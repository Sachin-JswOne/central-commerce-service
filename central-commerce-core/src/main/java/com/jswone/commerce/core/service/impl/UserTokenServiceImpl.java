package com.jswone.commerce.core.service.impl;

import com.google.cloud.datastore.Key;
import com.jswone.commerce.core.constants.JWTConstants;
import com.jswone.commerce.core.entity.UserTokenEntity;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.repository.UserTokenRepository;
import com.jswone.commerce.core.service.UserTokenService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserTokenServiceImpl implements UserTokenService {

  private static final Logger log = LogManager.getLogger(UserTokenServiceImpl.class);
  private final UserTokenRepository userTokenRepository;

  @Value("${spring.cloud.gcp.datastore.project-id}")
  private String projectId;

  public UserTokenServiceImpl(UserTokenRepository userTokenRepository) {
    this.userTokenRepository = userTokenRepository;
  }

  public boolean saveUserToken(UserTokenEntity userTokenEntity) {
    log.info(
        " request to save user token details for user id : {}", userTokenEntity.getCustomerId());

    try {
      UserTokenEntity savedUserTokenEntity =
          (UserTokenEntity) this.userTokenRepository.save(userTokenEntity);
      return savedUserTokenEntity != null;
    } catch (Exception e) {
      log.error(" error while saving user token entity  in datastore");
      throw new CentralCommerceServiceException(
          "error while saving user token entity  in datastore", e.getCause());
    }
  }

  public boolean userTokenExists(Key jwtTokenHash) {
    return this.userTokenRepository.existsById(jwtTokenHash);
  }

  public boolean userTokenExists(String jwtToken) {
    try {
      Key tokenHashKey = null;
      String jwtTokenHash = generateJWTHash(jwtToken);
      if (jwtTokenHash != null) {
        tokenHashKey = keyBuilder("user_auth_token_store", jwtTokenHash);
      }

      if (tokenHashKey != null) {
        boolean isUserTokenFound = this.userTokenRepository.existsById(tokenHashKey);
        return isUserTokenFound;
      } else {
        return false;
      }
    } catch (Exception e) {
      log.error("exception while jwt token re-use validation : {}", e);
      throw new CentralCommerceServiceException(
          "exception while jwt token re-use validation", e.getCause());
    }
  }

  public void deleteUserToken(UserTokenEntity userTokenEntity) {
    log.info(" request to delete user token for user id : {} ", userTokenEntity.getCustomerId());

    try {
      this.userTokenRepository.deleteById(userTokenEntity.getTokenHash());
    } catch (Exception e) {
      log.error("error while deleting token for userId : {}", userTokenEntity.getCustomerId());
      throw new CentralCommerceServiceException(
          "error while deleting token for userId : ".concat(userTokenEntity.getCustomerId()),
          e.getCause());
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

  public Key keyBuilder(String entityName, String value) {
    return Key.newBuilder(this.projectId, entityName, value).build();
  }
}
