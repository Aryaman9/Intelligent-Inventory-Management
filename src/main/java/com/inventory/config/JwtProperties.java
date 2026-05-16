package com.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessTokenSecret;
    private String refreshTokenSecret;
    private long accessTokenExpirationMs = 900000L;
    private long refreshTokenExpirationMs = 604800000L;
}
