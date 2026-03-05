package com.poc.akeyless.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "akeyless")
public class AkeylessProperties {

    private String accessId;
    private String accessKey;
    private String apiUrl;

    public String getAccessId() { return accessId; }
    public void setAccessId(String accessId) { this.accessId = accessId; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
}
