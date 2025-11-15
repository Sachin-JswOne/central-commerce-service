package com.jswone.commerce.core.service;

import com.jswone.commerce.core.entity.UserTokenEntity;

public interface UserTokenService {
  boolean saveUserToken(UserTokenEntity userTokenEntity);

  boolean userTokenExists(String jwtToken);

  void deleteUserToken(UserTokenEntity userTokenEntity);
}
