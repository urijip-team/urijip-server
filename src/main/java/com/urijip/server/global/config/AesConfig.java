package com.urijip.server.global.config;

import com.urijip.server.global.security.AesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AesProperties.class)
public class AesConfig {

}
