package com.sisgic.tools;

import com.sisgic.ScientificProductsPlatformApplication;
import com.sisgic.service.ResearcherMatchingService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Imports Awards from the productivity workbook. Safe by default: without
 * {@code --execute}, every row is validated but no database changes are made.
 */
public final class AwardExcelImportTool {

    private static final Path DEFAULT_FILE = Path.of(
        "/Users/manolocabello/Documents/dlab/sisgic/carga datos/6. FB210008 - Productivity report year 4.xlsx");
    private static final String DEFAULT_SHEET = "Awards";
    private static final long AWARD_PRODUCT_TYPE_ID = 21L;
    private static final long PARTICIPATION_TYPE_ID = 34L;
    private static final int TEXT_TYPE_ID = 2;
    private static final String TEXT_CODE_LOCK = "sisgic_textos_code_2";

    private static final String COL_AWARDEES = "Awardee(s) Name(s)";
    private static final String COL_AWARD_NAME = "Award Name";
    private static final String COL_YEAR = "Year";
    private static final String COL_CONTRIBUTION = "Contribution of the Awardee";
    private static final String COL_INSTITUTION = "Institution";
    private static final String COL_PROGRESS_REPORT = "Progress Report";
    private static final String COL_ANID_ID = "ID";

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        COL_AWARDEES,
        COL_AWARD_NAME,
        COL_YEAR,
        COL_CONTRIBUTION,
        COL_INSTITUTION,
        COL_PROGRESS_REPORT,
        COL_ANID_ID
    );

    private static final Pattern PARTICIPANT_SEPARATOR =
        Pattern.compile("\\s*(?:\\r?\\n|;)\\s*");
    private static final Pattern OTHER_INSTITUTION_PREFIX =
        Pattern.compile("(?i)^others?\\s*:\\s*");

    /**
     * Explicit Excel institution aliases → institucion.id
     * (keys must match {@link #normalize(String)} output).
     */
    private static final Map<String, Long> INSTITUTION_ALIASES = Map.of(
        // Excel → id=99 Fundación Ciencia & Vida
        "centro cientifico tecnologico de excelencia ciencia vida", 99L,
        // Excel → id=42 International Brain Research Organization
        "international brain research organization ibro", 42L,
        // Excel → id=15 Sociedad de Biologia Celular de Chile
        "sociedad de biologia celular de chile grupo bios", 15L,
        // Excel → id=823 Sociedad de Biología Celular de Chile
        "sociedad de biologia celular de chile", 15L,
        // Excel → id=821 SBCCH and Genexpress
        "genexpress", 821L,
        "sbcch", 821L
    );

    private AwardExcelImportTool() {
    }

    public static void main(String[] args) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
            printUsage();
            return;
        }

        if (options.help) {
            printUsage();
            return;
        }

        System.out.println();
        System.out.println("=== SISGIC · Importador de awards desde Excel ===");
        System.out.println("Archivo : " + options.file.toAbsolutePath().normalize());
        System.out.println("Hoja    : " + options.sheet);
        System.out.println("Modo    : " + (options.execute
            ? "EJECUCIÓN (escribe en DB)"
            : "DRY RUN (sin cambios)"));
        System.out.println();

        if (!Files.isRegularFile(options.file)) {
            fail("No existe el archivo: " + options.file);
            return;
        }

        ConfigurableApplicationContext springContext = null;
        try {
            List<ExcelAwardRow> rows;
            try (InputStream input = Files.newInputStream(options.file);
                 Workbook workbook = WorkbookFactory.create(input)) {
                rows = readRows(workbook, options.sheet);
            }

            DbConfig db = resolveDbConfig(options);
            System.out.println("DB      : " + maskUrl(db.url) + " (user=" + db.user + ")");
            System.out.println();

            springContext = new SpringApplicationBuilder(ScientificProductsPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run();
            ResearcherMatchingService matchingService =
                springContext.getBean(ResearcherMatchingService.class);
            matchingService.reloadRRHH();

            ImportSummary summary;
            try (Connection connection = DriverManager.getConnection(db.url, db.user, db.password)) {

                connection.setAutoCommit(true);
                ImportCatalog catalog = loadCatalog(connection);
                validateRequiredCatalog(catalog);
                summary = importRows(connection, catalog, matchingService, rows, options);
            }

            printSummary(summary, options.execute);
            if (summary.rejected > 0) {
                System.exit(2);
            }
        } catch (Exception e) {
            fail(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (springContext != null) {
                springContext.close();
            }
        }
    }

    private static ImportSummary importRows(
            Connection connection,
            ImportCatalog catalog,
            ResearcherMatchingService matchingService,
            List<ExcelAwardRow> rows,
            Options options) throws SQLException {

        ImportSummary summary = new ImportSummary();
        summary.total = rows.size();

        for (ExcelAwardRow excelRow : rows) {
            try {
                String codigoAnid = cleanNumericText(required(excelRow, COL_ANID_ID));
                Optional<Long> existingId = findAwardIdByCodigoAnid(connection, codigoAnid);
                if (existingId.isPresent()) {
                    summary.skipped++;
                    System.out.printf(
                        Locale.ROOT,
                        "[SKIP] row=%d ID=%s already exists (producto.id=%d)%n",
                        excelRow.rowNumber,
                        codigoAnid,
                        existingId.get()
                    );
                    continue;
                }

                ResolvedAward award =
                    resolveAward(excelRow, catalog, matchingService, codigoAnid);
                if (!options.execute) {
                    summary.valid++;
                    System.out.printf(
                        Locale.ROOT,
                        "[OK]   row=%d ID=%s award=%s awardees=%d institution=%s%n",
                        excelRow.rowNumber,
                        award.codigoAnid,
                        abbreviate(award.name, 60),
                        award.participants.size(),
                        abbreviate(award.institutionName, 45)
                    );
                    continue;
                }

                long productId = insertAwardTransaction(connection, award, options.username);
                summary.inserted++;
                System.out.printf(
                    Locale.ROOT,
                    "[ADD]  row=%d ID=%s producto.id=%d awardees=%d%n",
                    excelRow.rowNumber,
                    award.codigoAnid,
                    productId,
                    award.participants.size()
                );
            } catch (RowRejectedException e) {
                summary.rejected++;
                summary.errors.add("Row " + excelRow.rowNumber + ": " + e.getMessage());
                System.err.printf(
                    Locale.ROOT,
                    "[REJECT] row=%d ID=%s: %s%n",
                    excelRow.rowNumber,
                    safe(excelRow.text(COL_ANID_ID)),
                    e.getMessage()
                );
            } catch (Exception e) {
                summary.rejected++;
                summary.errors.add("Row " + excelRow.rowNumber + ": " + e.getMessage());
                System.err.printf(
                    Locale.ROOT,
                    "[ERROR] row=%d ID=%s: %s%n",
                    excelRow.rowNumber,
                    safe(excelRow.text(COL_ANID_ID)),
                    e.getMessage()
                );
            }
        }
        return summary;
    }

    private static ResolvedAward resolveAward(
            ExcelAwardRow row,
            ImportCatalog catalog,
            ResearcherMatchingService matchingService,
            String codigoAnid) {

        String name = required(row, COL_AWARD_NAME);
        int year = exactInteger(row, COL_YEAR);
        int maxYear = LocalDate.now().getYear() + 1;
        if (year < 1900 || year > maxYear) {
            throw reject(COL_YEAR + " must be between 1900 and " + maxYear + ": " + year);
        }

        String contribution = trimToNull(row.text(COL_CONTRIBUTION));
        String progressReport = trimToNull(row.text(COL_PROGRESS_REPORT));
        InstitutionResolution institution =
            resolveInstitution(required(row, COL_INSTITUTION), catalog.institutions);
        List<ResolvedParticipant> participants =
            resolveParticipants(required(row, COL_AWARDEES), matchingService);

        return new ResolvedAward(
            name,
            year,
            contribution,
            progressReport,
            codigoAnid,
            institution.id,
            institution.name,
            participants
        );
    }

    private static List<ResolvedParticipant> resolveParticipants(
            String raw,
            ResearcherMatchingService matchingService) {

        List<ResolvedParticipant> result = new ArrayList<>();
        Set<Long> usedResearcherIds = new LinkedHashSet<>();
        for (String participantText : PARTICIPANT_SEPARATOR.split(raw.trim())) {
            if (participantText.isBlank()) {
                continue;
            }
            String name = parseParticipantName(participantText);
            long rrhhId = uniqueResearcherMatch(name, matchingService);
            if (!usedResearcherIds.add(rrhhId)) {
                throw reject("Duplicate awardee in the same award: " + name);
            }
            result.add(new ResolvedParticipant(rrhhId));
        }
        if (result.isEmpty()) {
            throw reject(COL_AWARDEES + " is required");
        }
        return result;
    }

    private static String parseParticipantName(String raw) {
        int lastComma = raw.lastIndexOf(',');
        if (lastComma < 1 || lastComma == raw.length() - 1) {
            throw reject(
                "Invalid awardee format (expected Name, RRHH Type): " + raw);
        }
        String name = raw.substring(0, lastComma).trim();
        if (name.isEmpty()) {
            throw reject("Awardee name is empty: " + raw);
        }
        return name;
    }

    /**
     * Same acceptance criteria used by the project importer and OpenAlex:
     * accept UNICA, or MAS_DE_UNA only when the service selected an id.
     */
    private static long uniqueResearcherMatch(
            String name,
            ResearcherMatchingService matchingService) {

        ResearcherMatchingService.Resultado result = matchingService.encontrarMejor(name);
        if (result == null
                || result.estatus == ResearcherMatchingService.Estatus.SIN_COINCIDENCIAS
                || (result.estatus == ResearcherMatchingService.Estatus.MAS_DE_UNA
                    && result.getIdRRHH() == null)) {
            String detail = result != null
                    && result.detalle != null
                    && !result.detalle.isBlank()
                ? " candidates=[" + result.detalle + "]"
                : "";
            throw reject(
                "RRHH not found or ambiguous via ResearcherMatchingService: "
                    + name + detail);
        }
        Long id = result.getIdRRHH();
        if (id == null) {
            throw reject("RRHH match returned no id for: " + name);
        }
        return id;
    }

    private static InstitutionResolution resolveInstitution(
            String raw,
            Map<Long, String> institutions) {

        String name = OTHER_INSTITUTION_PREFIX.matcher(raw.trim()).replaceFirst("").trim();
        if (name.isEmpty()) {
            throw reject("Institution name is empty after Other:/Others: prefix");
        }
        String target = normalize(name);

        Long aliasId = INSTITUTION_ALIASES.get(target);
        if (aliasId != null) {
            String label = institutions.get(aliasId);
            if (label == null) {
                throw reject("Institution alias maps to missing id=" + aliasId
                    + " for: " + name);
            }
            return new InstitutionResolution(aliasId, label);
        }

        List<Map.Entry<Long, String>> matches = institutions.entrySet().stream()
            .filter(entry -> normalize(entry.getValue()).equals(target))
            .toList();
        if (matches.isEmpty()) {
            throw reject("Institution not found: " + name);
        }
        if (matches.size() > 1) {
            String ids = matches.stream()
                .map(entry -> String.valueOf(entry.getKey()))
                .collect(Collectors.joining(","));
            throw reject("Institution is ambiguous: " + name + " (ids=" + ids + ")");
        }
        Map.Entry<Long, String> match = matches.get(0);
        return new InstitutionResolution(match.getKey(), match.getValue());
    }

    private static long insertAwardTransaction(
            Connection connection,
            ResolvedAward award,
            String username) throws SQLException {

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        boolean lockAcquired = false;
        try {
            if (findAwardIdByCodigoAnid(connection, award.codigoAnid).isPresent()) {
                throw reject("codigoANID already exists: " + award.codigoAnid);
            }

            lockAcquired = acquireTextCodeLock(connection);
            if (!lockAcquired) {
                throw new SQLException("Could not acquire text-code generation lock");
            }

            String descriptionCode = nextTextCode(connection);
            insertBilingualText(connection, descriptionCode, award.name);

            String commentCode = null;
            if (award.contribution != null) {
                commentCode = nextTextCode(connection);
                insertBilingualText(connection, commentCode, award.contribution);
            }

            long productId =
                insertProduct(connection, award, descriptionCode, commentCode, username);
            insertAwardSubtype(connection, productId, award);
            insertParticipants(connection, productId, award.participants);

            connection.commit();
            return productId;
        } catch (Exception e) {
            connection.rollback();
            if (e instanceof SQLException sqlException) {
                throw sqlException;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new SQLException(e);
        } finally {
            if (lockAcquired) {
                releaseTextCodeLock(connection);
            }
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static long insertProduct(
            Connection connection,
            ResolvedAward award,
            String descriptionCode,
            String commentCode,
            String username) throws SQLException {

        String sql =
            "INSERT INTO producto (" +
            "idDescripcion, idComentario, fechaInicio, fechaTermino, idTipoProducto, " +
            "progressReport, codigoANID, created_at, updated_at, username" +
            ") VALUES (?, ?, ?, NULL, ?, ?, ?, NOW(), NOW(), ?)";
        try (PreparedStatement statement =
                 connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, descriptionCode);
            statement.setString(2, commentCode);
            statement.setObject(3, LocalDate.of(award.year, 1, 1));
            statement.setLong(4, AWARD_PRODUCT_TYPE_ID);
            statement.setString(5, award.progressReport);
            statement.setString(6, award.codigoAnid);
            statement.setString(7, trimToNull(username));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not insert producto");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return producto.id");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void insertAwardSubtype(
            Connection connection,
            long productId,
            ResolvedAward award) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO award (id, year, idinstitucion) VALUES (?, ?, ?)")) {
            statement.setLong(1, productId);
            statement.setInt(2, award.year);
            statement.setLong(3, award.institutionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not insert award");
            }
        }
    }

    private static void insertParticipants(
            Connection connection,
            long productId,
            List<ResolvedParticipant> participants) throws SQLException {

        String sql =
            "INSERT INTO rrhh_producto (" +
            "idRRHH, idProducto, id, orden, idTipoParticipacion, corresponding, " +
            "created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int order = 1;
            for (ResolvedParticipant participant : participants) {
                long nextId = nextParticipationId(connection, productId, participant.rrhhId);
                statement.setLong(1, participant.rrhhId);
                statement.setLong(2, productId);
                statement.setLong(3, nextId);
                statement.setInt(4, order++);
                statement.setLong(5, PARTICIPATION_TYPE_ID);
                statement.setString(6, "0");
                statement.addBatch();
            }
            for (int result : statement.executeBatch()) {
                if (result == Statement.EXECUTE_FAILED) {
                    throw new SQLException("Could not insert rrhh_producto");
                }
            }
        }
    }

    private static long nextParticipationId(
            Connection connection,
            long productId,
            long rrhhId) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM rrhh_producto " +
                "WHERE idProducto = ? AND idRRHH = ?")) {
            statement.setLong(1, productId);
            statement.setLong(2, rrhhId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Could not calculate rrhh_producto.id");
                }
                return rs.getLong(1);
            }
        }
    }

    private static Optional<Long> findAwardIdByCodigoAnid(
            Connection connection,
            String codigoAnid) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT a.id FROM award a " +
                "JOIN producto p ON p.id = a.id " +
                "WHERE LOWER(TRIM(p.codigoANID)) = LOWER(TRIM(?)) LIMIT 1")) {
            statement.setString(1, codigoAnid);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        }
    }

    private static ImportCatalog loadCatalog(Connection connection) throws SQLException {
        Map<Long, String> institutions = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, " +
                "COALESCE(NULLIF(TRIM(descripcion), ''), NULLIF(TRIM(idDescripcion), '')) " +
                "FROM v_institucion ORDER BY id");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString(2);
                if (label != null && !label.isBlank()) {
                    institutions.put(rs.getLong(1), label);
                }
            }
        }

        boolean hasAwardProductType = exists(
            connection,
            "SELECT 1 FROM v_tipo_producto WHERE id = ?",
            AWARD_PRODUCT_TYPE_ID
        );
        boolean hasParticipationType = exists(
            connection,
            "SELECT 1 FROM v_tipo_participacion WHERE id = ?",
            PARTICIPATION_TYPE_ID
        );
        return new ImportCatalog(
            institutions,
            hasAwardProductType,
            hasParticipationType
        );
    }

    private static boolean exists(Connection connection, String sql, long id)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void validateRequiredCatalog(ImportCatalog catalog) {
        if (!catalog.hasAwardProductType) {
            throw new IllegalStateException(
                "Missing product type id=" + AWARD_PRODUCT_TYPE_ID + " for Awards");
        }
        if (!catalog.hasParticipationType) {
            throw new IllegalStateException(
                "Missing participation type id=" + PARTICIPATION_TYPE_ID
                    + " in v_tipo_participacion");
        }
        if (catalog.institutions.isEmpty()) {
            throw new IllegalStateException("v_institucion is empty");
        }
    }

    private static List<ExcelAwardRow> readRows(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException(
                "Sheet not found: " + sheetName + ". Available: "
                    + java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
                        .mapToObj(workbook::getSheetName)
                        .collect(Collectors.joining(", "))
            );
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new IllegalArgumentException("The sheet has no header row");
        }

        DataFormatter formatter = new DataFormatter(Locale.US);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell, evaluator));
            if (!header.isBlank()) {
                columns.put(header, cell.getColumnIndex());
            }
        }

        List<String> missing = REQUIRED_HEADERS.stream()
            .filter(header -> !columns.containsKey(normalizeHeader(header)))
            .sorted()
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Missing required Excel columns: " + missing
                    + ". Available: " + String.join(", ", columns.keySet()));
        }

        List<ExcelAwardRow> rows = new ArrayList<>();
        for (int rowIndex = headerRow.getRowNum() + 1;
                rowIndex <= sheet.getLastRowNum();
                rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, ExcelCellValue> values = new HashMap<>();
            for (Map.Entry<String, Integer> column : columns.entrySet()) {
                Cell cell =
                    row.getCell(column.getValue(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                values.put(
                    column.getKey(),
                    ExcelCellValue.from(cell, formatter, evaluator)
                );
            }

            String name = values.get(normalizeHeader(COL_AWARD_NAME)).text;
            String id = values.get(normalizeHeader(COL_ANID_ID)).text;
            if ((name == null || name.isBlank()) && (id == null || id.isBlank())) {
                continue;
            }
            rows.add(new ExcelAwardRow(rowIndex + 1, values));
        }
        return rows;
    }

    private static int exactInteger(ExcelAwardRow row, String column) {
        ExcelCellValue cell = row.cell(column);
        String text = cell.text == null ? "" : cell.text.trim();
        if (text.isEmpty()) {
            throw reject(column + " is required");
        }
        try {
            BigDecimal number = cell.number != null
                ? cell.number
                : new BigDecimal(text.replace(",", ""));
            return number.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw reject(column + " must be an integer: " + text);
        }
    }

    private static String required(ExcelAwardRow row, String column) {
        String value = trimToNull(row.cell(column).text);
        if (value == null) {
            throw reject(column + " is required");
        }
        return value;
    }

    private static boolean acquireTextCodeLock(Connection connection) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            statement.setString(1, TEXT_CODE_LOCK);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static void releaseTextCodeLock(Connection connection) {
        try (PreparedStatement statement =
                 connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, TEXT_CODE_LOCK);
            statement.executeQuery();
        } catch (SQLException ignored) {
            // Closing the connection also releases the advisory lock.
        }
    }

    private static String nextTextCode(Connection connection) throws SQLException {
        String sql =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(codigotexto, 4) AS UNSIGNED)), 0) + 1 " +
            "FROM textos WHERE codigotexto LIKE 'TXT%' AND idTipoTexto = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, TEXT_TYPE_ID);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Could not generate textos.codigotexto");
                }
                return String.format(Locale.ROOT, "TXT%06d", rs.getLong(1));
            }
        }
    }

    private static void insertBilingualText(
            Connection connection,
            String code,
            String value) throws SQLException {

        String sql =
            "INSERT INTO textos " +
            "(lenguaje, codigotexto, idTipoTexto, valor, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String language : List.of("us", "es")) {
                statement.setString(1, language);
                statement.setString(2, code);
                statement.setInt(3, TEXT_TYPE_ID);
                statement.setString(4, value);
                statement.addBatch();
            }
            if (statement.executeBatch().length != 2) {
                throw new SQLException("Could not insert bilingual text");
            }
        }
    }

    private static String cleanNumericText(String value) {
        String trimmed = value.trim();
        try {
            BigDecimal number = new BigDecimal(trimmed.replace(",", ""));
            if (number.stripTrailingZeros().scale() <= 0) {
                return number.toBigIntegerExact().toString();
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
            // IDs can be alphanumeric.
        }
        return trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = java.text.Normalizer
            .normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{Alnum}]+", " ")
            .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private static RowRejectedException reject(String message) {
        return new RowRejectedException(message);
    }

    private static DbConfig resolveDbConfig(Options options) throws IOException {
        Properties yaml = loadDatasourceFromYaml();
        String url = firstNonBlank(
            options.url,
            System.getenv("SISGIC_DB_URL"),
            yaml.getProperty("url")
        );
        String user = firstNonBlank(
            options.user,
            System.getenv("SISGIC_DB_USER"),
            yaml.getProperty("username")
        );
        String password = firstNonBlank(
            options.password,
            System.getenv("SISGIC_DB_PASSWORD"),
            yaml.getProperty("password")
        );
        if (url == null || user == null || password == null) {
            throw new IllegalArgumentException(
                "Incomplete datasource configuration. Check application.yml "
                    + "or SISGIC_DB_* variables.");
        }
        return new DbConfig(url, user, password);
    }

    private static Properties loadDatasourceFromYaml() throws IOException {
        Path[] candidates = {
            Path.of("src/main/resources/application.yml"),
            Path.of("backend/src/main/resources/application.yml"),
            Path.of("application.yml")
        };
        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            String yaml = Files.readString(path);
            Properties values = new Properties();
            for (String key : List.of("url", "username", "password")) {
                String value = extractDatasourceScalar(yaml, key);
                if (value != null) {
                    values.setProperty(key, value);
                }
            }
            if (!values.isEmpty()) {
                return values;
            }
        }
        throw new IOException("application.yml with spring.datasource was not found");
    }

    private static String extractDatasourceScalar(String yaml, String key) {
        String[] lines = yaml.split("\\R");
        boolean inDatasource = false;
        int datasourceIndent = -1;
        for (String raw : lines) {
            String line = raw.replace("\t", "    ");
            String trimmed = line.trim();
            int indent = line.indexOf(trimmed);
            if (trimmed.equals("datasource:")) {
                inDatasource = true;
                datasourceIndent = indent;
                continue;
            }
            if (inDatasource && !trimmed.isEmpty() && !trimmed.startsWith("#")
                    && indent <= datasourceIndent) {
                inDatasource = false;
            }
            if (!inDatasource || !trimmed.startsWith(key + ":")) {
                continue;
            }
            String value = trimmed.substring(key.length() + 1).trim();
            int comment = value.indexOf(" #");
            if (comment >= 0) {
                value = value.substring(0, comment).trim();
            }
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        return Arrays.stream(values)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    private static String maskUrl(String url) {
        if (url == null) {
            return "";
        }
        int scheme = url.indexOf("://");
        int slash = scheme >= 0 ? url.indexOf('/', scheme + 3) : -1;
        return slash >= 0
            ? url.substring(0, scheme + 3) + "***" + url.substring(slash)
            : url;
    }

    private static void printSummary(ImportSummary summary, boolean execute) {
        System.out.println();
        System.out.println("=== Resultado ===");
        System.out.println("Filas leídas       : " + summary.total);
        if (execute) {
            System.out.println("Awards insertados  : " + summary.inserted);
        } else {
            System.out.println("Filas válidas      : " + summary.valid);
        }
        System.out.println("Duplicados omitidos: " + summary.skipped);
        System.out.println("Filas rechazadas   : " + summary.rejected);
        if (!summary.errors.isEmpty()) {
            System.out.println();
            System.out.println("Errores:");
            summary.errors.forEach(error -> System.out.println("  - " + error));
        }
        if (!execute && summary.rejected == 0) {
            System.out.println();
            System.out.println("Dry run correcto. Ejecuta con --execute para insertar.");
        }
    }

    private static void printUsage() {
        System.out.println("""
            Uso:
              ./import-awards.sh [--dry-run] [--execute] [opciones]

            Opciones:
              --execute              Inserta awards (sin esta opción es dry-run)
              --dry-run              Valida sin escribir (comportamiento por defecto)
              --file=/ruta/file.xlsx Archivo Excel
              --sheet=Awards         Nombre de la hoja
              --username=usuario     Username asociado a producto (opcional)
              --url=jdbc:mysql://... Override de datasource
              --user=usuario         Override de datasource
              --password=clave       Override de datasource
              --help                 Muestra esta ayuda
            """);
    }

    private static void fail(String message) {
        System.err.println("ERROR: " + message);
    }

    private record DbConfig(String url, String user, String password) {
    }

    private record InstitutionResolution(long id, String name) {
    }

    private record ResolvedParticipant(long rrhhId) {
    }

    private record ImportCatalog(
        Map<Long, String> institutions,
        boolean hasAwardProductType,
        boolean hasParticipationType
    ) {
    }

    private record ResolvedAward(
        String name,
        int year,
        String contribution,
        String progressReport,
        String codigoAnid,
        long institutionId,
        String institutionName,
        List<ResolvedParticipant> participants
    ) {
    }

    private static final class ExcelCellValue {
        private final String text;
        private final BigDecimal number;

        private ExcelCellValue(String text, BigDecimal number) {
            this.text = text;
            this.number = number;
        }

        private static ExcelCellValue from(
                Cell cell,
                DataFormatter formatter,
                FormulaEvaluator evaluator) {

            if (cell == null) {
                return new ExcelCellValue("", null);
            }
            String text = formatter.formatCellValue(cell, evaluator).trim();
            CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
            BigDecimal number = null;
            if (type == CellType.NUMERIC) {
                number = BigDecimal.valueOf(cell.getNumericCellValue());
            }
            return new ExcelCellValue(text, number);
        }
    }

    private static final class ExcelAwardRow {
        private final int rowNumber;
        private final Map<String, ExcelCellValue> values;

        private ExcelAwardRow(int rowNumber, Map<String, ExcelCellValue> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }

        private ExcelCellValue cell(String header) {
            return values.getOrDefault(
                normalizeHeader(header),
                new ExcelCellValue("", null)
            );
        }

        private String text(String header) {
            return cell(header).text;
        }
    }

    private static final class ImportSummary {
        private int total;
        private int valid;
        private int inserted;
        private int skipped;
        private int rejected;
        private final List<String> errors = new ArrayList<>();
    }

    private static final class RowRejectedException extends RuntimeException {
        private RowRejectedException(String message) {
            super(message);
        }
    }

    private static final class Options {
        private final Path file;
        private final String sheet;
        private final boolean execute;
        private final boolean help;
        private final String username;
        private final String url;
        private final String user;
        private final String password;

        private Options(
                Path file,
                String sheet,
                boolean execute,
                boolean help,
                String username,
                String url,
                String user,
                String password) {
            this.file = file;
            this.sheet = sheet;
            this.execute = execute;
            this.help = help;
            this.username = username;
            this.url = url;
            this.user = user;
            this.password = password;
        }

        private static Options parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            boolean execute = false;
            boolean help = false;
            for (String arg : args == null ? new String[0] : args) {
                if ("--execute".equals(arg)) {
                    execute = true;
                } else if ("--dry-run".equals(arg)) {
                    execute = false;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if (arg != null && arg.startsWith("--") && arg.contains("=")) {
                    int equals = arg.indexOf('=');
                    values.put(arg.substring(2, equals), arg.substring(equals + 1));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new Options(
                Path.of(firstNonBlank(values.get("file"), DEFAULT_FILE.toString())),
                firstNonBlank(values.get("sheet"), DEFAULT_SHEET),
                execute,
                help,
                values.get("username"),
                values.get("url"),
                values.get("user"),
                values.get("password")
            );
        }
    }
}
