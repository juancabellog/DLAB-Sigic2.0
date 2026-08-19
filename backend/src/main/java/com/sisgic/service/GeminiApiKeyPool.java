package com.sisgic.service;

import com.sisgic.config.GeminiProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin pool of Gemini API keys. Rotates on rate-limit / overload responses (503, 429).
 */
@Component
public class GeminiApiKeyPool {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiKeyPool.class);

    private final GeminiProperties properties;
    private final AtomicInteger nextIndex = new AtomicInteger(0);
    private List<String> keys = List.of();

    public GeminiApiKeyPool(GeminiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        List<String> resolved = new ArrayList<>();
        if (properties.getApikeys() != null) {
            properties.getApikeys().stream()
                .filter(this::isUsableKey)
                .map(String::trim)
                .forEach(resolved::add);
        }
        if (resolved.isEmpty()) {
            resolved.addAll(keysFromEnvList());
        }
        if (resolved.isEmpty() && isUsableKey(properties.getApikey())) {
            resolved.add(properties.getApikey().trim());
        }
        keys = Collections.unmodifiableList(resolved);
        if (!keys.isEmpty()) {
            log.info("Gemini API key pool initialized with {} key(s), model={}",
                keys.size(), properties.getModel());
        } else {
            log.warn("Gemini API key pool is empty (set GEMINI_APIKEYS, GEMINI_API_KEY, or gemini.apikey)");
        }
    }

    private List<String> keysFromEnvList() {
        String raw = System.getenv("GEMINI_APIKEYS");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (isUsableKey(part)) {
                parsed.add(part.trim());
            }
        }
        return parsed;
    }

    public boolean isConfigured() {
        return !keys.isEmpty();
    }

    public int size() {
        return keys.size();
    }

    /** Keys in pool order starting from the current round-robin position. */
    public List<String> keysStartingFromCurrent() {
        if (keys.isEmpty()) {
            return List.of();
        }
        int start = Math.floorMod(nextIndex.get(), keys.size());
        List<String> ordered = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            ordered.add(keys.get((start + i) % keys.size()));
        }
        return ordered;
    }

    /** Advance round-robin cursor after a successful call on the given key. */
    public void markSuccess(String keyUsed) {
        int idx = keys.indexOf(keyUsed);
        if (idx >= 0) {
            nextIndex.set((idx + 1) % keys.size());
        }
    }

    public static String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return "..." + key.substring(key.length() - 4);
    }

    private boolean isUsableKey(String key) {
        return key != null && !key.isBlank();
    }
}
