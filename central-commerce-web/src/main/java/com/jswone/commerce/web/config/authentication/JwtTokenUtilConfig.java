package com.jswone.commerce.web.config.authentication;

import com.jswone.commons.util.JwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtTokenUtilConfig {

  @Bean
  public JwtTokenUtil jwtTokenUtil() {
    return new JwtTokenUtil();
  }
}
