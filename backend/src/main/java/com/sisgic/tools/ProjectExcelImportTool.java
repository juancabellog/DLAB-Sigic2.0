package com.sisgic.tools;

import com.sisgic.ScientificProductsPlatformApplication;
import com.sisgic.service.ResearcherMatchingService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
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
 * Terminal utility that imports scientific projects from the "Projects" sheet.
 *
 * <p>By default it performs a dry run. Use {@code --execute} to write data.</p>
 *
 * <pre>
 *   ./import-projects.sh
 *   ./import-projects.sh --execute
 *   ./import-projects.sh --file="/path/report.xlsx" --sheet=Projects --execute
 * </pre>
 *
 * <p>Researcher resolution uses {@link ResearcherMatchingService} (same rules as DOI/OpenAlex matching).
 * Datasource values are read from {@code application.yml}; environment variables
 * {@code SISGIC_DB_URL}, {@code SISGIC_DB_USER}, and {@code SISGIC_DB_PASSWORD}
 * or command-line arguments can override them.</p>
 */
public final class ProjectExcelImportTool {

    private static final Path DEFAULT_FILE = Path.of(
        "/Users/manolocabello/Documents/dlab/sisgic/carga datos/6. FB210008 - Productivity report year 4.xlsx");
    private static final String DEFAULT_SHEET = "Projects";
    private static final long PROJECT_PRODUCT_TYPE_ID = 19L;
    private static final long OTHER_FUNDING_TYPE_ID = 7L;
    private static final int TEXT_TYPE_ID = 2;
    private static final String TEXT_CODE_LOCK = "sisgic_textos_code_2";

    private static final String COL_RESEARCHERS = "Researchers that participate in the Project";
    private static final String COL_TITLE = "Project Title";
    private static final String COL_PROJECT_CODE = "Project Code";
    private static final String COL_FUNDING = "Funding Source";
    private static final String COL_PROJECT_TYPE = "Project Type";
    private static final String COL_TRADE_ASSOCIATIONS = "Name of Trade and Regional Associations";
    private static final String COL_AWARD_DATE = "Award Date";
    private static final String COL_DURATION = "Duration (months)";
    private static final String COL_TOTAL_AMOUNT = "Total amount of the project (M$)";
    private static final String COL_CENTER_AMOUNT = "Total amount awarded by the Center (M$)";
    private static final String COL_START_DATE = "Start Date";
    private static final String COL_END_DATE = "Ending Date";
    private static final String COL_RESEARCH_LINE = "Name of Research Line";
    private static final String COL_PROGRESS_REPORT = "Progress Report";
    private static final String COL_ANID_ID = "ID";

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        COL_RESEARCHERS,
        COL_TITLE,
        COL_PROJECT_CODE,
        COL_FUNDING,
        COL_PROJECT_TYPE,
        COL_AWARD_DATE,
        COL_DURATION,
        COL_TOTAL_AMOUNT,
        COL_CENTER_AMOUNT,
        COL_START_DATE,
        COL_END_DATE,
        COL_RESEARCH_LINE,
        COL_PROGRESS_REPORT,
        COL_ANID_ID
    );

    private static final Pattern PARTICIPANT_SEPARATOR = Pattern.compile("\\s*(?:\\r?\\n|;)\\s*");
    private static final Set<String> GENDER_TOKENS = Set.of(
        "male", "female", "masculino", "femenino", "m", "f"
    );

    /**
     * Explicit Excel research-line aliases → v_cluster.id
     * (keys must match {@link #normalize(String)} output).
     */
    private static final Map<String, Long> CLUSTER_ALIASES = Map.of(
        // Excel: "Viruses, microbes, and infection"
        // v_cluster id=2: "Cluster II. Virus, microbes and infection"
        "viruses microbes and infection", 2L
    );

    private static final List<DateTimeFormatter> FULL_DATE_FORMATTERS = List.of(
        formatter("d MMM uuuu"),
        formatter("d MMMM uuuu"),
        formatter("d/M/uuuu"),
        formatter("d-M-uuuu"),
        DateTimeFormatter.ISO_LOCAL_DATE
    );

    private static final List<DateTimeFormatter> MONTH_DATE_FORMATTERS = List.of(
        monthFormatter("MMM uuuu"),
        monthFormatter("MMMM uuuu")
    );

    private ProjectExcelImportTool() {
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
        System.out.println("=== SISGIC · Importador de proyectos desde Excel ===");
        System.out.println("Archivo : " + options.file.toAbsolutePath().normalize());
        System.out.println("Hoja    : " + options.sheet);
        System.out.println("Modo    : " + (options.execute ? "EJECUCIÓN (escribe en DB)" : "DRY RUN (sin cambios)"));
        System.out.println();

        if (!Files.isRegularFile(options.file)) {
            fail("No existe el archivo: " + options.file);
            return;
        }

        ConfigurableApplicationContext springContext = null;
        try {
            DbConfig db = resolveDbConfig(options);
            System.out.println("DB      : " + maskUrl(db.url) + " (user=" + db.user + ")");
            System.out.println();

            // Boot Spring (no web) to reuse ResearcherMatchingService for RRHH resolution.
            springContext = new SpringApplicationBuilder(ScientificProductsPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run();
            ResearcherMatchingService matchingService = springContext.getBean(ResearcherMatchingService.class);
            matchingService.reloadRRHH();

            ImportSummary summary;
            try (Connection connection = DriverManager.getConnection(db.url, db.user, db.password);
                 InputStream input = Files.newInputStream(options.file);
                 Workbook workbook = WorkbookFactory.create(input)) {

                connection.setAutoCommit(true);
                ImportCatalog catalog = loadCatalog(connection);
                validateRequiredCatalog(catalog);
                List<ExcelProjectRow> rows = readRows(workbook, options.sheet);
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
            List<ExcelProjectRow> rows,
            Options options) throws SQLException {

        ImportSummary summary = new ImportSummary();
        summary.total = rows.size();

        for (ExcelProjectRow excelRow : rows) {
            try {
                // Duplicate policy: existing Project Code is omitted without attempting to
                // revalidate or modify the existing project.
                String rowProjectCode = cleanNumericText(required(excelRow, COL_PROJECT_CODE));
                Optional<Long> existingId = findProjectIdByCode(connection, rowProjectCode);
                if (existingId.isPresent()) {
                    summary.skipped++;
                    System.out.printf(
                        Locale.ROOT,
                        "[SKIP] row=%d code=%s already exists (producto.id=%d)%n",
                        excelRow.rowNumber,
                        rowProjectCode,
                        existingId.get()
                    );
                    continue;
                }

                ResolvedProject project = resolveProject(excelRow, catalog, matchingService);

                if (!options.execute) {
                    summary.valid++;
                    System.out.printf(
                        Locale.ROOT,
                        "[OK]   row=%d code=%s title=%s participants=%d%n",
                        excelRow.rowNumber,
                        project.projectCode,
                        abbreviate(project.title, 70),
                        project.participants.size()
                    );
                    continue;
                }

                long productId = insertProjectTransaction(connection, project, options.username);
                summary.inserted++;
                System.out.printf(
                    Locale.ROOT,
                    "[ADD]  row=%d code=%s producto.id=%d participants=%d%n",
                    excelRow.rowNumber,
                    project.projectCode,
                    productId,
                    project.participants.size()
                );
            } catch (RowRejectedException e) {
                summary.rejected++;
                summary.errors.add("Row " + excelRow.rowNumber + ": " + e.getMessage());
                System.err.printf(
                    Locale.ROOT,
                    "[REJECT] row=%d code=%s: %s%n",
                    excelRow.rowNumber,
                    safe(excelRow.text(COL_PROJECT_CODE)),
                    e.getMessage()
                );
            } catch (Exception e) {
                summary.rejected++;
                summary.errors.add("Row " + excelRow.rowNumber + ": " + e.getMessage());
                System.err.printf(
                    Locale.ROOT,
                    "[ERROR] row=%d code=%s: %s%n",
                    excelRow.rowNumber,
                    safe(excelRow.text(COL_PROJECT_CODE)),
                    e.getMessage()
                );
            }
        }
        return summary;
    }

    private static ResolvedProject resolveProject(
            ExcelProjectRow row,
            ImportCatalog catalog,
            ResearcherMatchingService matchingService) {
        String title = required(row, COL_TITLE);
        String projectCode = cleanNumericText(required(row, COL_PROJECT_CODE));
        String fundingSource = required(row, COL_FUNDING);
        String projectTypeText = required(row, COL_PROJECT_TYPE);

        FundingResolution funding = resolveFunding(fundingSource, catalog.fundingTypes);
        ProjectTypesResolution projectTypes = resolveProjectTypes(projectTypeText, catalog.projectTypes);
        String clusters = resolveClusters(required(row, COL_RESEARCH_LINE), catalog.clusters);
        List<ResolvedParticipant> participants = resolveParticipants(
            row.text(COL_RESEARCHERS),
            matchingService,
            catalog.participationTypes
        );

        LocalDate awardDate = parseAwardDate(row.cell(COL_AWARD_DATE), COL_AWARD_DATE);
        LocalDate startDate = parseFullDate(row.cell(COL_START_DATE), COL_START_DATE);
        LocalDate endDate = parseFullDate(row.cell(COL_END_DATE), COL_END_DATE);
        if (endDate.isBefore(startDate)) {
            throw reject("Ending Date is before Start Date");
        }

        int duration = positiveInteger(row, COL_DURATION);
        int totalAmount = nonNegativeInteger(row, COL_TOTAL_AMOUNT);
        int centerAmount = nonNegativeInteger(row, COL_CENTER_AMOUNT);
        String progressReport = cleanNumericText(required(row, COL_PROGRESS_REPORT));
        String codigoAnid = cleanNumericText(required(row, COL_ANID_ID));

        return new ResolvedProject(
            title,
            projectCode,
            funding.id,
            funding.otherFundingType,
            projectTypes.ids,
            projectTypes.otherText,
            trimToNull(row.text(COL_TRADE_ASSOCIATIONS)),
            awardDate,
            duration,
            totalAmount,
            centerAmount,
            startDate,
            endDate,
            progressReport,
            codigoAnid,
            clusters,
            participants
        );
    }

    private static long insertProjectTransaction(
            Connection connection,
            ResolvedProject project,
            String username) throws SQLException {

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        boolean lockAcquired = false;
        try {
            if (findProjectIdByCode(connection, project.projectCode).isPresent()) {
                throw reject("Project Code already exists: " + project.projectCode);
            }

            lockAcquired = acquireTextCodeLock(connection);
            if (!lockAcquired) {
                throw new SQLException("Could not acquire text-code generation lock");
            }

            String descriptionCode = nextTextCode(connection);
            insertBilingualText(connection, descriptionCode, project.title);
            long productId = insertProduct(connection, project, descriptionCode, username);
            insertProjectSubtype(connection, productId, project);
            insertParticipants(connection, productId, project.participants);

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
            ResolvedProject project,
            String descriptionCode,
            String username) throws SQLException {

        String sql =
            "INSERT INTO producto (" +
            "idDescripcion, fechaInicio, fechaTermino, idTipoProducto, progressReport, " +
            "codigoANID, basal, cluster, created_at, updated_at, username" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, descriptionCode);
            statement.setObject(2, project.startDate);
            statement.setObject(3, project.endDate);
            statement.setLong(4, PROJECT_PRODUCT_TYPE_ID);
            statement.setString(5, project.progressReport);
            statement.setString(6, project.codigoAnid);
            statement.setString(7, "S");
            statement.setString(8, project.clusterIds);
            statement.setString(9, trimToNull(username));
            int inserted = statement.executeUpdate();
            if (inserted != 1) {
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

    private static void insertProjectSubtype(
            Connection connection,
            long productId,
            ResolvedProject project) throws SQLException {

        String sql =
            "INSERT INTO proyecto (" +
            "id, projectCode, awardDate, duration, totalAmount, totalAmountCenter, " +
            "idFundingtype, otherFundingType, projectTypes, otherProjectType, " +
            "nameSocialOrganizations, namePublicSectorEntities, namePrivateSectorEntities, " +
            "nameTradeRegionalAssociations, nameSTEntities" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, ?, NULL)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, project.projectCode);
            statement.setObject(3, project.awardDate);
            statement.setInt(4, project.duration);
            statement.setInt(5, project.totalAmount);
            statement.setInt(6, project.totalAmountCenter);
            statement.setLong(7, project.fundingTypeId);
            statement.setString(8, project.otherFundingType);
            statement.setString(9, project.projectTypeIds);
            statement.setString(10, project.otherProjectType);
            statement.setString(11, project.nameTradeRegionalAssociations);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not insert proyecto");
            }
        }
    }

    private static void insertParticipants(
            Connection connection,
            long productId,
            List<ResolvedParticipant> participants) throws SQLException {

        String sql =
            "INSERT INTO rrhh_producto (" +
            "idRRHH, idProducto, id, orden, idTipoParticipacion, corresponding, created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int order = 1;
            for (ResolvedParticipant participant : participants) {
                long nextId = nextParticipationId(connection, productId, participant.rrhhId);
                statement.setLong(1, participant.rrhhId);
                statement.setLong(2, productId);
                statement.setLong(3, nextId);
                statement.setInt(4, order++);
                statement.setLong(5, participant.participationTypeId);
                statement.setString(6, "0");
                statement.addBatch();
            }
            if (!participants.isEmpty()) {
                int[] results = statement.executeBatch();
                for (int result : results) {
                    if (result == Statement.EXECUTE_FAILED) {
                        throw new SQLException("Could not insert rrhh_producto");
                    }
                }
            }
        }
    }

    private static long nextParticipationId(Connection connection, long productId, long rrhhId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM rrhh_producto WHERE idProducto = ? AND idRRHH = ?")) {
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

    private static boolean acquireTextCodeLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            statement.setString(1, TEXT_CODE_LOCK);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static void releaseTextCodeLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, TEXT_CODE_LOCK);
            statement.executeQuery();
        } catch (SQLException ignored) {
            // Connection close also releases the advisory lock.
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
            "INSERT INTO textos (lenguaje, codigotexto, idTipoTexto, valor, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String language : List.of("us", "es")) {
                statement.setString(1, language);
                statement.setString(2, code);
                statement.setInt(3, TEXT_TYPE_ID);
                statement.setString(4, value);
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            if (results.length != 2) {
                throw new SQLException("Could not insert bilingual title in textos");
            }
        }
    }

    private static Optional<Long> findProjectIdByCode(Connection connection, String code)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM proyecto WHERE LOWER(TRIM(projectCode)) = LOWER(TRIM(?)) LIMIT 1")) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        }
    }

    private static ImportCatalog loadCatalog(Connection connection) throws SQLException {
        Map<Long, String> funding = queryIdLabel(
            connection,
            "SELECT id, idDescripcion FROM fundingtype ORDER BY id"
        );
        Map<Long, String> projectTypes = queryIdLabel(
            connection,
            "SELECT id, idDescripcion FROM tipoproyecto ORDER BY id"
        );
        Map<Long, String> clusters = queryIdLabel(
            connection,
            "SELECT id, descripcion FROM v_cluster ORDER BY id"
        );
        Map<Long, String> participationTypes = queryIdLabel(
            connection,
            "SELECT id, descripcion FROM v_tipo_participacion " +
                "WHERE idTipoProducto = " + PROJECT_PRODUCT_TYPE_ID + " ORDER BY id"
        );

        boolean hasProjectProductType;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM v_tipo_producto WHERE id = ?")) {
            statement.setLong(1, PROJECT_PRODUCT_TYPE_ID);
            try (ResultSet rs = statement.executeQuery()) {
                hasProjectProductType = rs.next();
            }
        }

        return new ImportCatalog(
            funding,
            projectTypes,
            clusters,
            participationTypes,
            hasProjectProductType
        );
    }

    private static Map<Long, String> queryIdLabel(Connection connection, String sql)
            throws SQLException {
        Map<Long, String> values = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                values.put(rs.getLong(1), rs.getString(2));
            }
        }
        return values;
    }

    private static void validateRequiredCatalog(ImportCatalog catalog) {
        if (!catalog.hasProjectProductType) {
            throw new IllegalStateException(
                "Missing product type id=" + PROJECT_PRODUCT_TYPE_ID + " for Projects");
        }
        if (!catalog.fundingTypes.containsKey(OTHER_FUNDING_TYPE_ID)) {
            throw new IllegalStateException(
                "Missing Funding Type id=" + OTHER_FUNDING_TYPE_ID + " (Other)");
        }
        if (catalog.projectTypes.isEmpty()) {
            throw new IllegalStateException("Project Type catalog is empty");
        }
        if (catalog.clusters.isEmpty()) {
            throw new IllegalStateException("v_cluster is empty");
        }
        if (catalog.participationTypes.isEmpty()) {
            throw new IllegalStateException(
                "No participation types configured for idTipoProducto=" + PROJECT_PRODUCT_TYPE_ID);
        }
    }

    private static FundingResolution resolveFunding(
            String source,
            Map<Long, String> fundingTypes) {

        String normalized = normalize(source);
        if (normalized.startsWith("fuente internacional")) {
            return new FundingResolution(OTHER_FUNDING_TYPE_ID, source.trim());
        }

        if (normalized.contains("fondequip")) {
            List<Long> matches = fundingTypes.entrySet().stream()
                .filter(entry -> normalize(entry.getValue()).contains("fondequip"))
                .map(Map.Entry::getKey)
                .toList();
            if (matches.size() == 1) {
                return new FundingResolution(matches.get(0), null);
            }
            if (matches.isEmpty()) {
                throw reject("Funding Source FONDEQUIP was not found in fundingtype");
            }
            throw reject("Funding Source FONDEQUIP is ambiguous in fundingtype");
        }

        long id = uniqueExactMatch("Funding Source", source, fundingTypes);
        String other = id == OTHER_FUNDING_TYPE_ID ? source.trim() : null;
        return new FundingResolution(id, other);
    }

    private static ProjectTypesResolution resolveProjectTypes(
            String raw,
            Map<Long, String> catalog) {

        List<Long> ids = new ArrayList<>();
        List<String> otherLabels = new ArrayList<>();
        for (String token : raw.split(",")) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            long id = uniqueExactMatch("Project Type", value, catalog);
            if (!ids.contains(id)) {
                ids.add(id);
            }
            if (normalize(catalog.get(id)).equals("other")) {
                otherLabels.add(value);
            }
        }
        if (ids.isEmpty()) {
            throw reject("Project Type is empty");
        }
        return new ProjectTypesResolution(
            ids.stream().map(String::valueOf).collect(Collectors.joining(",")),
            otherLabels.isEmpty() ? null : String.join(", ", otherLabels)
        );
    }

    private static String resolveClusters(String raw, Map<Long, String> catalog) {
        List<Long> ids = new ArrayList<>();
        for (String token : raw.split(";")) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            String target = normalize(value);

            Long aliasId = CLUSTER_ALIASES.get(target);
            if (aliasId != null) {
                if (!catalog.containsKey(aliasId)) {
                    throw reject("Cluster alias maps to missing v_cluster.id=" + aliasId
                        + " for research line: " + value);
                }
                if (!ids.contains(aliasId)) {
                    ids.add(aliasId);
                }
                continue;
            }

            List<Long> matches = catalog.entrySet().stream()
                .filter(entry -> {
                    String label = normalize(entry.getValue());
                    String withoutPrefix = stripClusterPrefix(label);
                    return label.equals(target)
                        || withoutPrefix.equals(target)
                        || label.endsWith(" " + target);
                })
                .map(Map.Entry::getKey)
                .toList();
            if (matches.isEmpty()) {
                throw reject("Research Line/Cluster not found: " + value);
            }
            if (matches.size() > 1) {
                throw reject("Research Line/Cluster is ambiguous: " + value);
            }
            if (!ids.contains(matches.get(0))) {
                ids.add(matches.get(0));
            }
        }
        if (ids.isEmpty()) {
            throw reject("Name of Research Line is empty");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String stripClusterPrefix(String value) {
        return value.replaceFirst("^cluster\\s+[ivxlcdm0-9]+\\s*", "").trim();
    }

    private static List<ResolvedParticipant> resolveParticipants(
            String raw,
            ResearcherMatchingService matchingService,
            Map<Long, String> participationTypes) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<ResolvedParticipant> result = new ArrayList<>();
        Set<Long> usedResearcherIds = new LinkedHashSet<>();
        for (String participantText : PARTICIPANT_SEPARATOR.split(raw.trim())) {
            if (participantText.isBlank()) {
                continue;
            }
            ParsedParticipant parsed = parseParticipant(participantText);
            long rrhhId = uniqueResearcherMatch(parsed.name, matchingService);
            long participationTypeId = uniqueExactMatch(
                "Participation Type",
                parsed.participationType,
                participationTypes
            );
            if (!usedResearcherIds.add(rrhhId)) {
                throw reject("Duplicate participant in the same project: " + parsed.name);
            }
            result.add(new ResolvedParticipant(rrhhId, participationTypeId));
        }
        return result;
    }

    private static ParsedParticipant parseParticipant(String raw) {
        List<String> parts = Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));

        if (parts.size() < 2) {
            throw reject(
                "Invalid participant format (expected Name, Participation Type[, Gender]): " + raw);
        }

        String gender = null;
        if (GENDER_TOKENS.contains(normalize(parts.get(parts.size() - 1)))) {
            gender = parts.remove(parts.size() - 1);
        }
        if (parts.size() < 2) {
            throw reject("Participation Type missing for participant: " + raw);
        }

        String participationType = parts.remove(parts.size() - 1);
        String name = String.join(", ", parts).trim();
        if (name.isEmpty()) {
            throw reject("Participant name is empty: " + raw);
        }
        return new ParsedParticipant(name, participationType, gender);
    }

    /**
     * Resolves researchers with {@link ResearcherMatchingService#encontrarMejor(String)},
     * using the same acceptance rules as OpenAlex / {@code /api/researchers/match}:
     * accept UNICA, or MAS_DE_UNA when a preferred {@code elegido} exists; otherwise reject.
     */
    private static long uniqueResearcherMatch(
            String name,
            ResearcherMatchingService matchingService) {

        ResearcherMatchingService.Resultado resultado = matchingService.encontrarMejor(name);
        if (resultado == null
            || resultado.estatus == ResearcherMatchingService.Estatus.SIN_COINCIDENCIAS
            || (resultado.estatus == ResearcherMatchingService.Estatus.MAS_DE_UNA
                && resultado.getIdRRHH() == null)) {
            String detail = resultado != null && resultado.detalle != null && !resultado.detalle.isBlank()
                ? " candidates=[" + resultado.detalle + "]"
                : "";
            throw reject("RRHH not found or ambiguous via ResearcherMatchingService: " + name + detail);
        }

        Long id = resultado.getIdRRHH();
        if (id == null) {
            throw reject("RRHH match returned no id for: " + name);
        }
        return id;
    }

    private static long uniqueExactMatch(
            String catalogName,
            String value,
            Map<Long, String> catalog) {

        String target = normalize(value);
        List<Long> matches = catalog.entrySet().stream()
            .filter(entry -> normalize(entry.getValue()).equals(target))
            .map(Map.Entry::getKey)
            .toList();
        if (matches.isEmpty()) {
            throw reject(catalogName + " not found: " + value);
        }
        if (matches.size() > 1) {
            throw reject(catalogName + " is ambiguous: " + value);
        }
        return matches.get(0);
    }

    private static List<ExcelProjectRow> readRows(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException(
                "Sheet not found: " + sheetName + ". Available: " +
                    java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
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
            throw new IllegalArgumentException("Missing required Excel columns: " + missing);
        }

        List<ExcelProjectRow> rows = new ArrayList<>();
        for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, ExcelCellValue> values = new HashMap<>();
            for (Map.Entry<String, Integer> column : columns.entrySet()) {
                Cell cell = row.getCell(column.getValue(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                values.put(column.getKey(), ExcelCellValue.from(cell, formatter, evaluator));
            }

            String title = values.get(normalizeHeader(COL_TITLE)).text;
            String projectCode = values.get(normalizeHeader(COL_PROJECT_CODE)).text;
            if ((title == null || title.isBlank()) && (projectCode == null || projectCode.isBlank())) {
                continue;
            }
            rows.add(new ExcelProjectRow(rowIndex + 1, values));
        }
        return rows;
    }

    private static int positiveInteger(ExcelProjectRow row, String column) {
        int value = exactInteger(row, column);
        if (value <= 0) {
            throw reject(column + " must be positive");
        }
        return value;
    }

    private static int nonNegativeInteger(ExcelProjectRow row, String column) {
        int value = exactInteger(row, column);
        if (value < 0) {
            throw reject(column + " must be non-negative");
        }
        return value;
    }

    private static int exactInteger(ExcelProjectRow row, String column) {
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

    private static LocalDate parseAwardDate(ExcelCellValue cell, String column) {
        if (cell.date != null) {
            return cell.date.withDayOfMonth(1);
        }
        String text = required(cell, column);
        for (DateTimeFormatter formatter : MONTH_DATE_FORMATTERS) {
            try {
                return YearMonth.parse(text, formatter).atDay(1);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        try {
            return parseFullDate(cell, column).withDayOfMonth(1);
        } catch (RowRejectedException ignored) {
            throw reject("Invalid Award Date (expected month and year): " + text);
        }
    }

    private static LocalDate parseFullDate(ExcelCellValue cell, String column) {
        if (cell.date != null) {
            return cell.date;
        }
        String text = required(cell, column);
        for (DateTimeFormatter formatter : FULL_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        throw reject("Invalid " + column + ": " + text);
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.ENGLISH);
    }

    private static DateTimeFormatter monthFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter(Locale.ENGLISH);
    }

    private static String required(ExcelProjectRow row, String column) {
        return required(row.cell(column), column);
    }

    private static String required(ExcelCellValue cell, String column) {
        String value = cell != null ? trimToNull(cell.text) : null;
        if (value == null) {
            throw reject(column + " is required");
        }
        return value;
    }

    private static String cleanNumericText(String value) {
        String trimmed = value.trim();
        try {
            BigDecimal number = new BigDecimal(trimmed.replace(",", ""));
            if (number.stripTrailingZeros().scale() <= 0) {
                return number.toBigIntegerExact().toString();
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
            // Keep original string (project codes may be alphanumeric).
        }
        return trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
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
                "Incomplete datasource configuration. Check application.yml or SISGIC_DB_* variables.");
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
            System.out.println("Proyectos insertados: " + summary.inserted);
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
              ./import-projects.sh [--dry-run] [--execute] [opciones]

            Opciones:
              --execute              Inserta proyectos (sin esta opción es dry-run)
              --dry-run              Valida sin escribir (comportamiento por defecto)
              --file=/ruta/file.xlsx Archivo Excel
              --sheet=Projects       Nombre de la hoja
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

    private record FundingResolution(long id, String otherFundingType) {
    }

    private record ProjectTypesResolution(String ids, String otherText) {
    }

    private record ParsedParticipant(String name, String participationType, String gender) {
    }

    private record ResolvedParticipant(long rrhhId, long participationTypeId) {
    }

    private record ImportCatalog(
        Map<Long, String> fundingTypes,
        Map<Long, String> projectTypes,
        Map<Long, String> clusters,
        Map<Long, String> participationTypes,
        boolean hasProjectProductType
    ) {
    }

    private record ResolvedProject(
        String title,
        String projectCode,
        long fundingTypeId,
        String otherFundingType,
        String projectTypeIds,
        String otherProjectType,
        String nameTradeRegionalAssociations,
        LocalDate awardDate,
        int duration,
        int totalAmount,
        int totalAmountCenter,
        LocalDate startDate,
        LocalDate endDate,
        String progressReport,
        String codigoAnid,
        String clusterIds,
        List<ResolvedParticipant> participants
    ) {
    }

    private static final class ExcelCellValue {
        private final String text;
        private final BigDecimal number;
        private final LocalDate date;

        private ExcelCellValue(String text, BigDecimal number, LocalDate date) {
            this.text = text;
            this.number = number;
            this.date = date;
        }

        private static ExcelCellValue from(
                Cell cell,
                DataFormatter formatter,
                FormulaEvaluator evaluator) {

            if (cell == null) {
                return new ExcelCellValue("", null, null);
            }
            String text = formatter.formatCellValue(cell, evaluator).trim();
            CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
            BigDecimal number = null;
            LocalDate date = null;
            if (type == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    date = cell.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    number = BigDecimal.valueOf(cell.getNumericCellValue());
                }
            }
            return new ExcelCellValue(text, number, date);
        }
    }

    private static final class ExcelProjectRow {
        private final int rowNumber;
        private final Map<String, ExcelCellValue> values;

        private ExcelProjectRow(int rowNumber, Map<String, ExcelCellValue> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }

        private ExcelCellValue cell(String header) {
            return values.getOrDefault(
                normalizeHeader(header),
                new ExcelCellValue("", null, null)
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
