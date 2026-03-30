package com.fashionshop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.momo")
@Data
public class MoMoConfig {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String redirectUrl;
    private String ipnUrl;
}
