package com.ai.interviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url = "https://eiwqkbnhdzbtqjvlvxji.supabase.co";
    private String anonKey = "";
    private String jwtSecret = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAnonKey() {
        return anonKey;
    }

    public void setAnonKey(String anonKey) {
        this.anonKey = anonKey;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isConfigured() {
        return anonKey != null && !anonKey.trim().isEmpty() && !anonKey.contains("YOUR_SUPABASE_ANON_KEY");
    }
}
