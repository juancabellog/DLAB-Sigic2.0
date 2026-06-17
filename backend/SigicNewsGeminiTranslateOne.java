package cl.dlab.sigic.wix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee noticias en español desde {@code producto}/{@code noticias}/{@code textos},
 * traduce {@code title}, {@code excerpt} y {@code richText} (HTML) con Gemini (es → en)
 * e inserta en {@code textos} con {@code lenguaje='us'} y los mismos {@code codigotexto}.
 * <p>
 * Alineado con {@code REPLICAR_TRADUCCION_GEMINI.txt}.
 */
public class SigicNewsGeminiTranslateOne {

    private static final String DEFAULT_GEMINI_MODEL = "gemini-flash-lite-latest";
    private static final String GEMINI_ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final int DEFAULT_GEMINI_TIMEOUT_MS = 300_000;
    private static final int RICHTEXT_SPLIT_THRESHOLD_CHARS = 8_000;
    private static final int MAX_RETRIES = 5;
    /** Modelo Gemini (se configura en {@link #main} desde GEMINI_MODEL o gemini.model). */
    private static String geminiModel = DEFAULT_GEMINI_MODEL;
    /** Timeout por request Gemini (se configura en {@link #main}). */
    private static int geminiRequestTimeoutMs = DEFAULT_GEMINI_TIMEOUT_MS;
    private static final int ID_TIPO_TEXTO = 2;
    private static final String LANG_SOURCE = "es";
    private static final String LANG_TARGET = "us";
    private static final Pattern JSON_ARRAY_IN_TEXT = Pattern.compile("\\[[\\s\\S]*\\]");

    /** Solo noticias con textos en {@code es} y sin fila {@code us} para el título ({@code idTitulo}). */
    private static final String SQL_NEWS_ES = ""
            + "SELECT p.id, n.idTitulo, p.idDescripcion, p.idComentario, "
            + "t1.valor AS title, t2.valor AS excerpt, t3.valor AS richText "
            + "FROM producto p "
            + "JOIN noticias n ON p.id = n.id "
            + "JOIN textos t1 ON n.idTitulo = t1.codigotexto AND t1.idTipoTexto = 2 AND t1.lenguaje = 'es' "
            + "JOIN textos t2 ON p.idDescripcion = t2.codigotexto AND t2.idTipoTexto = 2 AND t2.lenguaje = 'es' "
            + "JOIN textos t3 ON p.idComentario = t3.codigotexto AND t3.idTipoTexto = 2 AND t3.lenguaje = 'es' "
            + "LEFT JOIN textos t4 ON n.idTitulo = t4.codigotexto AND t4.idTipoTexto = 2 AND t4.lenguaje = 'us' "
            + "WHERE t4.idTipoTexto IS NULL";

    private static final String SQL_UPSERT_TEXTO = ""
            + "INSERT INTO textos (lenguaje, codigotexto, valor, idTipoTexto) VALUES (?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE valor = VALUES(valor)";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        Properties props = loadResourceProperties("wix.properties");
        geminiModel = firstNonEmpty(
                System.getenv("GEMINI_MODEL"),
                props.getProperty("gemini.model"),
                DEFAULT_GEMINI_MODEL
        ).trim();
        geminiRequestTimeoutMs = parsePositiveInt(firstNonEmpty(
                System.getenv("SIGIC_TRANSLATION_GEMINI_TIMEOUT_MS"),
                props.getProperty("sigic.translation.geminiTimeoutMs")
        ), DEFAULT_GEMINI_TIMEOUT_MS);
        long pauseBetweenMs = parsePositiveInt(firstNonEmpty(
                System.getenv("SIGIC_TRANSLATION_PAUSE_MS"),
                props.getProperty("sigic.translation.pauseMs")
        ), 2_000);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(60))
                .build();

        boolean dryRun = isTruthy(System.getenv("SIGIC_TRANSLATION_DRY_RUN"));
        boolean writeTxt = isTruthy(firstNonEmpty(
                System.getenv("SIGIC_TRANSLATION_WRITE_TXT"),
                props.getProperty("sigic.translation.writeTxt")
        ));
        int limit = parseOptionalLimit(firstNonEmpty(
                System.getenv("SIGIC_TRANSLATION_LIMIT"),
                props.getProperty("sigic.translation.limit")
        ));

        try {
            String geminiKey = resolveRequired("GEMINI_API_KEY", "gemini.apikey", props);
            String mysqlUrl = resolveRequired("MYSQL_URL", "mysql.url", props);
            String mysqlUser = resolveRequired("MYSQL_USER", "mysql.user", props);
            String mysqlPassword = resolveRequired("MYSQL_PASSWORD", "mysql.password", props);
            Path outputFile = Paths.get(firstNonEmpty(
                    System.getenv("SIGIC_TRANSLATION_OUTPUT_FILE"),
                    props.getProperty("sigic.translation.outputFile"),
                    "v_news_translation_us.txt"
            )).toAbsolutePath().normalize();

            List<NewsRow> rows = fetchNewsRowsEs(mysqlUrl, mysqlUser, mysqlPassword, limit);
            System.err.println("Noticias sin traducción us (pendientes): " + rows.size()
                    + ", modelo Gemini=" + geminiModel
                    + ", timeout Gemini=" + geminiRequestTimeoutMs + " ms");
            if (rows.isEmpty()) {
                System.out.println("Sin filas que procesar.");
                return;
            }
            if (dryRun) {
                System.err.println("SIGIC_TRANSLATION_DRY_RUN=true: no se escribe en MySQL.");
            }

            int ok = 0;
            int err = 0;
            StringBuilder txtReport = writeTxt ? new StringBuilder() : null;

            for (int i = 0; i < rows.size(); i++) {
                NewsRow row = rows.get(i);
                try {
                    System.err.println("Traduciendo id=" + row.id() + " (" + (i + 1) + "/" + rows.size() + ")...");
                    Map<String, String> translated = translateFieldsEsToEn(httpClient, geminiKey, row);
                    if (!dryRun) {
                        insertTranslationsUs(mysqlUrl, mysqlUser, mysqlPassword, row, translated);
                    }
                    if (txtReport != null) {
                        appendRowToTxt(txtReport, row, translated);
                    }
                    ok++;
                    System.err.println("OK producto/noticia id=" + row.id());
                    if (pauseBetweenMs > 0 && i + 1 < rows.size()) {
                        Thread.sleep(pauseBetweenMs);
                    }
                } catch (Exception e) {
                    err++;
                    System.err.println("ERR id=" + row.id() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            if (txtReport != null) {
                Files.writeString(outputFile, txtReport.toString(), StandardCharsets.UTF_8);
                System.out.println("Reporte opcional: " + outputFile);
            }
            System.out.println("Procesados: " + ok + ", errores: " + err
                    + (dryRun ? " (dry-run, sin INSERT)" : ""));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            shutdownMysqlCleanupThread();
            deregisterJdbcDrivers();
        }
    }

    /**
     * @param idTitulo     {@code noticias.idTitulo} → título
     * @param idDescripcion {@code producto.idDescripcion} → excerpt
     * @param idComentario  {@code producto.idComentario} → richText (HTML)
     */
    private record NewsRow(
            long id,
            String idTitulo,
            String idDescripcion,
            String idComentario,
            String title,
            String excerpt,
            String richText
    ) {
    }

    private static List<NewsRow> fetchNewsRowsEs(
            String mysqlUrl,
            String mysqlUser,
            String mysqlPassword,
            int limit
    ) throws Exception {
        String sql = SQL_NEWS_ES;
        if (limit > 0) {
            sql = sql + " LIMIT " + limit;
        }
        List<NewsRow> out = new ArrayList<>();
        tryLoadMysqlDriver();
        try (Connection con = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new NewsRow(
                        rs.getLong("id"),
                        nullToEmpty(rs.getString("idTitulo")),
                        nullToEmpty(rs.getString("idDescripcion")),
                        nullToEmpty(rs.getString("idComentario")),
                        nullToEmpty(rs.getString("title")),
                        nullToEmpty(rs.getString("excerpt")),
                        nullToEmpty(rs.getString("richText"))
                ));
            }
        }
        return out;
    }

    private static void insertTranslationsUs(
            String mysqlUrl,
            String mysqlUser,
            String mysqlPassword,
            NewsRow row,
            Map<String, String> en
    ) throws SQLException, ClassNotFoundException {
        tryLoadMysqlDriver();
        try (Connection con = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(SQL_UPSERT_TEXTO)) {
                upsertTexto(ps, row.idTitulo(), en.get("title"));
                upsertTexto(ps, row.idDescripcion(), en.get("excerpt"));
                upsertTexto(ps, row.idComentario(), en.get("richText"));
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    private static void upsertTexto(PreparedStatement ps, String codigo, String valor) throws SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new SQLException("codigotexto vacío");
        }
        ps.setString(1, LANG_TARGET);
        ps.setString(2, truncate(codigo.trim(), 200));
        ps.setString(3, valor != null ? valor : "");
        ps.setInt(4, ID_TIPO_TEXTO);
        ps.executeUpdate();
    }

    private static Map<String, String> translateFieldsEsToEn(HttpClient client, String apiKey, NewsRow row)
            throws IOException, InterruptedException {
        Map<String, String> out = new LinkedHashMap<>();
        if (row.richText().length() > RICHTEXT_SPLIT_THRESHOLD_CHARS) {
            System.err.println("  richText largo (" + row.richText().length()
                    + " chars): title+excerpt y richText en llamadas separadas a Gemini");
            out.putAll(translateBatch(client, apiKey, List.of(
                    new TranslationTask("title", row.title()),
                    new TranslationTask("excerpt", row.excerpt())
            )));
            out.putAll(translateBatch(client, apiKey, List.of(
                    new TranslationTask("richText", row.richText())
            )));
        } else {
            out.putAll(translateBatch(client, apiKey, List.of(
                    new TranslationTask("title", row.title()),
                    new TranslationTask("excerpt", row.excerpt()),
                    new TranslationTask("richText", row.richText())
            )));
        }
        return out;
    }

    private static Map<String, String> translateBatch(HttpClient client, String apiKey, List<TranslationTask> tasks)
            throws IOException, InterruptedException {
        String prompt = buildPromptSpanishToEnglish();
        String requestBody = buildGeminiRequestBody(prompt, tasks);
        String endpoint = String.format(Locale.ROOT, GEMINI_ENDPOINT_TEMPLATE, geminiModel, apiKey);

        String responseJson = postGeminiWithRetries(client, endpoint, requestBody);
        Map<String, String> byId = parseGeminiTranslationResponse(responseJson);

        Map<String, String> out = new LinkedHashMap<>();
        for (TranslationTask t : tasks) {
            String translated = byId.get(t.id());
            if (translated == null) {
                throw new IOException("Gemini no devolvió traducción para id=" + t.id());
            }
            out.put(t.id(), normalizeInstitutionalNames(translated));
        }
        return out;
    }

    private static String buildPromptSpanishToEnglish() {
        return """
                You are a senior translator for a Chilean life-sciences web platform.
                Translate from Spanish to English with high semantic fidelity.
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
                """;
    }

    private static String buildGeminiRequestBody(String prompt, List<TranslationTask> tasks) throws IOException {
        ArrayNode input = MAPPER.createArrayNode();
        for (TranslationTask t : tasks) {
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

    private static String postGeminiWithRetries(HttpClient client, String endpoint, String body)
            throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(java.time.Duration.ofMillis(geminiRequestTimeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = response.statusCode();
                if (code >= 200 && code < 300) {
                    return response.body();
                }
                if (isRetryableStatus(code) && attempt < MAX_RETRIES) {
                    System.err.println("Gemini HTTP " + code + ", reintento " + attempt + "/" + MAX_RETRIES);
                    Thread.sleep(2000L * attempt);
                    continue;
                }
                last = new IOException("Gemini HTTP " + code + ": " + truncate(response.body(), 500));
                if (!isRetryableStatus(code)) {
                    throw last;
                }
            } catch (java.net.http.HttpTimeoutException e) {
                last = new IOException("Gemini timeout tras " + geminiRequestTimeoutMs + " ms", e);
                if (attempt < MAX_RETRIES) {
                    System.err.println("Gemini timeout, reintento " + attempt + "/" + MAX_RETRIES);
                    Thread.sleep(2000L * attempt);
                    continue;
                }
                throw last;
            }
        }
        throw last != null ? last : new IOException("Gemini sin respuesta tras reintentos");
    }

    private static boolean isRetryableStatus(int code) {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }

    private static Map<String, String> parseGeminiTranslationResponse(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new IOException("Respuesta Gemini sin candidates[0].content.parts[0].text");
        }

        JsonNode parsed = tryParseTranslationArray(text);
        if (parsed == null) {
            Matcher m = JSON_ARRAY_IN_TEXT.matcher(text);
            if (m.find()) {
                parsed = tryParseTranslationArray(m.group());
            }
        }
        if (parsed == null || !parsed.isArray()) {
            throw new IOException("No se pudo parsear array de traducciones: " + truncate(text, 300));
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (JsonNode item : parsed) {
            if (!item.isObject()) {
                continue;
            }
            String id = item.path("id").isMissingNode() || !item.path("id").isTextual()
                    ? null
                    : item.path("id").asText();
            JsonNode textNode = item.get("text");
            if (id != null && textNode != null && textNode.isTextual()) {
                map.put(id, textNode.asText());
            }
        }
        return map;
    }

    private static JsonNode tryParseTranslationArray(String text) {
        try {
            JsonNode node = MAPPER.readTree(text.trim());
            if (node.isArray()) {
                return node;
            }
            JsonNode translations = node.get("translations");
            if (translations != null && translations.isArray()) {
                return translations;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalizeInstitutionalNames(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s
                .replace("Science & Life Center", "Centro Ciencia & Vida")
                .replace("Science and Life Center", "Centro Ciencia & Vida")
                .replace("Science & Life Foundation", "Fundacion Ciencia & Vida")
                .replace("Science and Life Foundation", "Fundacion Ciencia & Vida")
                .replace("Center for Science & Life", "Centro Ciencia & Vida")
                .replace("Center for Science and Life", "Centro Ciencia & Vida")
                .replace("San Sebastian University", "Universidad San Sebastian");
    }

    private static void appendRowToTxt(StringBuilder sb, NewsRow row, Map<String, String> en) {
        sb.append("=== noticia id=").append(row.id()).append(" (").append(LANG_SOURCE)
                .append(" -> ").append(LANG_TARGET).append(") ===\n");
        sb.append("idTitulo: ").append(row.idTitulo()).append("\n");
        sb.append("idDescripcion: ").append(row.idDescripcion()).append("\n");
        sb.append("idComentario: ").append(row.idComentario()).append("\n\n");
        appendFieldBlock(sb, "title", row.title(), en.get("title"));
        appendFieldBlock(sb, "excerpt", row.excerpt(), en.get("excerpt"));
        appendFieldBlock(sb, "richText", row.richText(), en.get("richText"));
    }

    private static void appendFieldBlock(StringBuilder sb, String name, String es, String en) {
        sb.append("--- ").append(name).append(" (").append(LANG_SOURCE).append(") ---\n");
        sb.append(es != null ? es : "").append("\n\n");
        sb.append("--- ").append(name).append(" (").append(LANG_TARGET).append(") ---\n");
        sb.append(en != null ? en : "").append("\n\n");
    }

    private record TranslationTask(String id, String text) {
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int parseOptionalLimit(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        int n = Integer.parseInt(raw.trim());
        return n > 0 ? n : 0;
    }

    private static int parsePositiveInt(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        int n = Integer.parseInt(raw.trim());
        return n > 0 ? n : defaultValue;
    }

    private static boolean isTruthy(String v) {
        if (v == null) {
            return false;
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        return "1".equals(t) || "true".equals(t) || "yes".equals(t);
    }

    private static void tryLoadMysqlDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Class.forName("com.mysql.jdbc.Driver");
        }
    }

    private static void shutdownMysqlCleanupThread() {
        try {
            Class<?> cleanupClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            cleanupClass.getMethod("checkedShutdown").invoke(null);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            System.err.println("Aviso: no se pudo cerrar AbandonedConnectionCleanupThread: " + e.getMessage());
        }
    }

    private static void deregisterJdbcDrivers() {
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver.getClass().getName().startsWith("com.mysql.")) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: no se pudieron desregistrar drivers JDBC: " + e.getMessage());
        }
    }

    private static Properties loadResourceProperties(String resourceName) {
        Properties props = new Properties();
        try (InputStream in = SigicNewsGeminiTranslateOne.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + resourceName, e);
        }
        return props;
    }

    private static String resolveRequired(String envKey, String propKey, Properties props) {
        String v = firstNonEmpty(System.getenv(envKey), props.getProperty(propKey));
        if (v == null) {
            throw new IllegalArgumentException("Falta " + envKey + " o " + propKey);
        }
        return v;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
