package com.sisgic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private List<String> apikeys = new ArrayList<>();
    private String apikey = "";
    private String model = "";
    private int timeoutMs = 300_000;

    public List<String> getApikeys() {
        return apikeys;
    }

    public void setApikeys(List<String> apikeys) {
        this.apikeys = apikeys != null ? apikeys : new ArrayList<>();
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getModel() {
        return model;
    }

    public String requireModel() {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("gemini.model is not configured (set it in application.yml)");
        }
        return model.trim();
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
