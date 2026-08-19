package com.sisgic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sisgic.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiTranslationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiTranslationService.class);

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final int RICHTEXT_SPLIT_THRESHOLD = 8_000;
    private static final int MAX_ROUNDS = 5;
    private static final Pattern JSON_ARRAY_IN_TEXT = Pattern.compile("\\[[\\s\\S]*\\]");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GeminiApiKeyPool apiKeyPool;
    private final GeminiProperties geminiProperties;

    private final HttpClient httpClient;

    public GeminiTranslationService(GeminiApiKeyPool apiKeyPool, GeminiProperties geminiProperties) {
        this.apiKeyPool = apiKeyPool;
        this.geminiProperties = geminiProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    @PostConstruct
    void logGeminiConfiguration() {
        log.info("Gemini translation service ready: model={}, timeoutMs={}, apiKeys={}",
            geminiProperties.getModel(),
            geminiProperties.getTimeoutMs(),
            apiKeyPool.size());
        if (geminiProperties.getModel() == null || geminiProperties.getModel().isBlank()) {
            log.warn("gemini.model is not set in application.yml — translation calls will fail");
        }
    }

    public boolean isConfigured() {
        return apiKeyPool.isConfigured();
    }

    public int getConfiguredKeyCount() {
        return apiKeyPool.size();
    }

    public enum TranslationDirection {
        ES_TO_EN,
        EN_TO_ES
    }

    /** Field names keep *En suffix for compatibility; values are target-language text. */
    public record TranslationResult(String titleEn, String excerptEn, String bodyEn) {}

    public TranslationResult translate(String titleEs, String excerptEs, String bodyEs)
            throws IOException, InterruptedException {
        return translate(titleEs, excerptEs, bodyEs, TranslationDirection.ES_TO_EN);
    }

    public TranslationResult translate(String title, String excerpt, String body, TranslationDirection direction)
            throws IOException, InterruptedException {
        if (!isConfigured()) {
            log.error("Gemini translate called but API key pool is empty (configured keys: {}). "
                + "Set gemini.apikeys in application.yml or gemini.apikey / GEMINI_API_KEY.",
                getConfiguredKeyCount());
            throw new IllegalStateException("Gemini API key is not configured (gemini.apikeys)");
        }

        TranslationDirection dir = direction != null ? direction : TranslationDirection.ES_TO_EN;
        int titleLen = safe(title).length();
        int excerptLen = safe(excerpt).length();
        int bodyLen = safe(body).length();
        log.info("Gemini translate started: direction={}, title={} chars, summary={} chars, body={} chars, model={}, keys={}",
            dir, titleLen, excerptLen, bodyLen, geminiProperties.requireModel(), getConfiguredKeyCount());

        try {
            return doTranslate(title, excerpt, body, bodyLen, dir);
        } catch (IOException | InterruptedException e) {
            log.error("Gemini translate failed: direction={}, title={} chars, summary={} chars, body={} chars — {}",
                dir, titleLen, excerptLen, bodyLen, e.getMessage(), e);
            throw e;
        }
    }

    private TranslationResult doTranslate(String title, String excerpt, String body, int bodyLen,
                                          TranslationDirection direction)
            throws IOException, InterruptedException {

        Map<String, String> translated;
        boolean bodyIsLong = bodyLen > RICHTEXT_SPLIT_THRESHOLD;

        if (bodyIsLong) {
            log.info("Body is long ({} chars), splitting into two Gemini calls", bodyLen);
            translated = new LinkedHashMap<>();
            translated.putAll(translateBatch(List.of(
                    new Task("title", safe(title)),
                    new Task("excerpt", safe(excerpt))
            ), direction));
            translated.putAll(translateBatch(List.of(
                    new Task("richText", safe(body))
            ), direction));
        } else {
            translated = translateBatch(List.of(
                    new Task("title", safe(title)),
                    new Task("excerpt", safe(excerpt)),
                    new Task("richText", safe(body))
            ), direction);
        }

        log.info("Gemini translate completed successfully (direction={})", direction);
        return new TranslationResult(
                normalizeInstitutionalNames(translated.getOrDefault("title", "")),
                normalizeInstitutionalNames(translated.getOrDefault("excerpt", "")),
                normalizeInstitutionalNames(translated.getOrDefault("richText", ""))
        );
    }

    private record Task(String id, String text) {}

    private Map<String, String> translateBatch(List<Task> tasks, TranslationDirection direction)
            throws IOException, InterruptedException {
        String prompt = buildPrompt(direction);
        String requestBody = buildRequestBody(prompt, tasks);
        String responseJson = postWithKeyPool(requestBody);
        Map<String, String> byId = parseResponse(responseJson);

        Map<String, String> out = new LinkedHashMap<>();
        for (Task t : tasks) {
            String value = byId.get(t.id());
            if (value == null) {
                throw new IOException("Gemini did not return translation for id=" + t.id());
            }
            out.put(t.id(), value);
        }
        return out;
    }

    private String buildPrompt(TranslationDirection direction) {
        String directionLine = direction == TranslationDirection.EN_TO_ES
                ? "Translate from English to Spanish with high semantic fidelity."
                : "Translate from Spanish to English with high semantic fidelity.";
        return """
                You are a senior translator for a Chilean life-sciences web platform.
                %s
                Rules:
                - Keep HTML tags as-is, including class attributes, and preserve their positions.
                - Translate only human-readable text nodes inside HTML; do not translate tag names, attribute names, or attribute values such as href URLs, src URLs, ids, or classes.
                - Translate the complete content. Do not summarize, shorten, omit paragraphs, or replace the body with an excerpt.
                - Do not translate URLs, emails, phone numbers, slugs, ids, dates, filenames, or numeric content.
                - Preserve institutional names such as Centro Ciencia & Vida, Fundacion Ciencia & Vida, Universidad San Sebastian, USS, CeBiB, CORFO, ANID, i3S, and Institut Curie.
                - Keep Markdown-like separators and punctuation structure.
                - Return compact, publication-grade language.
                - Output strictly JSON with this shape:
                [{ "id": "task_id", "text": "translated text" }].
                Translate these fields in a single JSON array in the same order as provided:
                """.formatted(directionLine);
    }

    private String buildRequestBody(String prompt, List<Task> tasks) throws IOException {
        ArrayNode input = MAPPER.createArrayNode();
        for (Task t : tasks) {
            ObjectNode o = input.addObject();
            o.put("id", t.id());
            o.put("text", t.text());
        }
        String fullPrompt = prompt + "\n" + MAPPER.writeValueAsString(input);

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", fullPrompt);

        ObjectNode gen = root.putObject("generationConfig");
        gen.put("responseMimeType", "application/json");
        gen.put("temperature", 0.2);
        gen.put("topP", 0.95);
        gen.put("topK", 40);

        return MAPPER.writeValueAsString(root);
    }

    private String postWithKeyPool(String body) throws IOException, InterruptedException {
        List<String> keysToTry = apiKeyPool.keysStartingFromCurrent();
        if (keysToTry.isEmpty()) {
            throw new IllegalStateException("Gemini API key pool is empty");
        }

        IOException lastError = null;
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            for (String apiKey : keysToTry) {
                String endpoint = String.format(Locale.ROOT, ENDPOINT_TEMPLATE,
                    geminiProperties.requireModel(), apiKey);
                try {
                    String responseBody = sendOnce(endpoint, body, apiKey);
                    apiKeyPool.markSuccess(apiKey);
                    return responseBody;
                } catch (RateLimitedException e) {
                    lastError = e;
                    log.warn("Gemini HTTP {} with key {}, trying next key in pool ({}/{} keys)",
                        e.statusCode, GeminiApiKeyPool.maskKey(apiKey), keysToTry.indexOf(apiKey) + 1, keysToTry.size());
                } catch (RetryableGeminiException e) {
                    lastError = e;
                    log.warn("Gemini HTTP {} with key {}, retry round {}/{}",
                        e.statusCode, GeminiApiKeyPool.maskKey(apiKey), round, MAX_ROUNDS);
                    break;
                }
            }
            if (round < MAX_ROUNDS) {
                long backoffMs = 2000L * round;
                log.warn("Gemini pool exhausted on round {}, waiting {} ms before retry", round, backoffMs);
                Thread.sleep(backoffMs);
            }
        }
        log.error("Gemini failed after {} round(s) across {} API key(s). Last error: {}",
            MAX_ROUNDS, keysToTry.size(),
            lastError != null ? lastError.getMessage() : "unknown", lastError);
        throw lastError != null ? lastError : new IOException("Gemini failed after trying all API keys");
    }

    private String sendOnce(String endpoint, String body, String apiKey)
            throws IOException, InterruptedException, RateLimitedException, RetryableGeminiException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return response.body();
            }
            String detail = truncate(response.body(), 500);
            if (shouldRotateKey(code)) {
                throw new RateLimitedException(code, "Gemini HTTP " + code + ": " + detail);
            }
            if (isRetryable(code)) {
                throw new RetryableGeminiException(code, "Gemini HTTP " + code + ": " + detail);
            }
            log.error("Gemini non-retryable HTTP {} (model={}, key={}): {}",
                code, geminiProperties.requireModel(), GeminiApiKeyPool.maskKey(apiKey), detail);
            throw new IOException("Gemini HTTP " + code + ": " + detail);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RetryableGeminiException(0, "Gemini timeout after " + geminiProperties.getTimeoutMs() + " ms", e);
        }
    }

    /** 503 overload and 429 rate limit — rotate to next API key immediately. */
    private static boolean shouldRotateKey(int code) {
        return code == 429 || code == 503;
    }

    private static final class RateLimitedException extends IOException {
        final int statusCode;

        RateLimitedException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    private static final class RetryableGeminiException extends IOException {
        final int statusCode;

        RetryableGeminiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        RetryableGeminiException(int statusCode, String message, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }
    }

    private static boolean isRetryable(int code) {
        return code == 500 || code == 502 || code == 504;
    }

    private Map<String, String> parseResponse(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);
        String text = root.path("candidates").path(0).path("content")
                .path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new IOException("Gemini response missing candidates[0].content.parts[0].text");
        }

        JsonNode parsed = tryParseArray(text);
        if (parsed == null) {
            Matcher m = JSON_ARRAY_IN_TEXT.matcher(text);
            if (m.find()) {
                parsed = tryParseArray(m.group());
            }
        }
        if (parsed == null || !parsed.isArray()) {
            throw new IOException("Could not parse Gemini translation array: " + truncate(text, 300));
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (JsonNode item : parsed) {
            if (!item.isObject()) continue;
            String id = item.path("id").isTextual() ? item.path("id").asText() : null;
            JsonNode textNode = item.get("text");
            if (id != null && textNode != null && textNode.isTextual()) {
                map.put(id, textNode.asText());
            }
        }
        return map;
    }

    private static JsonNode tryParseArray(String text) {
        try {
            JsonNode node = MAPPER.readTree(text.trim());
            if (node.isArray()) return node;
            JsonNode translations = node.get("translations");
            if (translations != null && translations.isArray()) return translations;
        } catch (Exception ignored) {}
        return null;
    }

    private static String normalizeInstitutionalNames(String s) {
        if (s == null || s.isEmpty()) return s;
        return s
                .replace("Science & Life Center", "Centro Ciencia & Vida")
                .replace("Science and Life Center", "Centro Ciencia & Vida")
                .replace("Science & Life Foundation", "Fundacion Ciencia & Vida")
                .replace("Science and Life Foundation", "Fundacion Ciencia & Vida")
                .replace("Center for Science & Life", "Centro Ciencia & Vida")
                .replace("Center for Science and Life", "Centro Ciencia & Vida")
                .replace("San Sebastian University", "Universidad San Sebastian");
    }

    private static String safe(String s) { return s != null ? s : ""; }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
