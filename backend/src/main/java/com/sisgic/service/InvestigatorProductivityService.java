package com.sisgic.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

/**
 * Servicio para generar el reporte de Productividad de Investigadores.
 * Adaptado de ImpactProductivityTool.java, pero obteniendo datos directamente de SQL.
 */
@Service
@Transactional(readOnly = true)
public class InvestigatorProductivityService {

    // Constantes para índices de columnas del archivo de Productividad
    private static final int PRODUCTIVITY_DATA_START_ROW = 3; // Fila 4 de Excel (0-based)
    private static final int COL_CORRELATIVO = 1; // Columna B
    private static final int COL_INVESTIGADOR = 2; // Columna C
    private static final int COL_TIPO_INVESTIGADOR = 3; // Columna D: Principal / Asociado
    private static final int COL_BX_GRADIENT = 75;
    private static final int COL_BY_GRADIENT = 76;
    private static final int COL_BZ_GRADIENT = 77;
    private static final int COL_CA_GRADIENT = 78;
    private static final int COL_CB_GRADIENT = 79;
    private static final int COL_CC_GRADIENT = 80;
    private static final int COL_CD_GRADIENT = 81;
    private static final int COL_CE_GRADIENT = 82;
    private static final int COL_CF_GRADIENT = 83;

    // Columnas por período
    private static final int P1_COL_A_PPAL = 4;
    private static final int P1_COL_IMP_PPAL = 5;
    private static final int P1_COL_A_SEC = 6;
    private static final int P1_COL_IMP_SEC = 7;
    private static final int P2_COL_A_PPAL = 17;
    private static final int P2_COL_IMP_PPAL = 18;
    private static final int P2_COL_A_SEC = 19;
    private static final int P2_COL_IMP_SEC = 20;
    private static final int P3_COL_A_PPAL = 30;
    private static final int P3_COL_IMP_PPAL = 31;
    private static final int P3_COL_A_SEC = 32;
    private static final int P3_COL_IMP_SEC = 33;
    private static final int P4_COL_A_PPAL = 43;
    private static final int P4_COL_IMP_PPAL = 44;
    private static final int P4_COL_A_SEC = 45;
    private static final int P4_COL_IMP_SEC = 46;
    private static final int P5_COL_A_PPAL = 59;
    private static final int P5_COL_IMP_PPAL = 60;
    private static final int P5_COL_A_SEC = 61;
    private static final int P5_COL_IMP_SEC = 62;

    // Columnas de tesis por período
    private static final int P1_COL_TESIS_TUTOR = 14;
    private static final int P1_COL_TESIS_COTUTOR = 15;
    private static final int P2_COL_TESIS_TUTOR = 27;
    private static final int P2_COL_TESIS_COTUTOR = 28;
    private static final int P3_COL_TESIS_TUTOR = 40;
    private static final int P3_COL_TESIS_COTUTOR = 41;
    private static final int P4_COL_TESIS_EN_CURSO_TUTOR = 53;
    private static final int P4_COL_TESIS_TERMINADAS_TUTOR = 54;
    private static final int P4_COL_TESIS_POSTDOCTORAL_TUTOR = 55;
    private static final int P4_COL_TESIS_COTUTOR = 57;
    private static final int P5_COL_TESIS_EN_CURSO_TUTOR = 69;
    private static final int P5_COL_TESIS_TERMINADAS_TUTOR = 70;
    private static final int P5_COL_TESIS_POSTDOCTORAL_TUTOR = 71;
    private static final int P5_COL_TESIS_COTUTOR = 73;
    private static final int COL_TOTAL_TESIS_TUTOR = 87;
    private static final int COL_TOTAL_TESIS_COTUTOR = 88;

    // Proyectos
    private static final int COL_PROYECTOS_CANT_NACIONAL = 90;
    private static final int COL_PROYECTOS_CANT_INTERNACIONAL = 91;
    private static final int COL_PROYECTOS_MONTO_NACIONAL = 93;
    private static final int COL_PROYECTOS_MONTO_INTERNACIONAL = 94;


    // Proyectos Excel
    private static final String SHEET_PROYECTOS = "Proyectos";
    private static final int PROYECTOS_ROW_DATA_START = 1;
    private static final int PROYECTOS_COL_NOMBRE = 2;
    private static final int PROYECTOS_COL_TIPO = 3;
    private static final int PROYECTOS_COL_CLP = 14;
    private static final String PROYECTO_INTERNACIONAL = "PROYECTO INTERNACIONAL";

    // Gradientes
    private static final String GRADIENT_NEUTRAL_HEX = "C6EFCE";
    private static final String GRADIENT_VERDE_OSCURO_HEX = "006100";
    private static final String GRADIENT_GRIS_HEX = "D3D3D3";
    private static final double GRADIENT_NEUTRAL_EPSILON = 1e-9;


    @PersistenceContext
    private EntityManager entityManager;

    // Clases internas para estadísticas
    private static class PublicationStats {
        int principalCount = 0;
        double principalIFSum = 0.0;
        int secondaryCount = 0;
        double secondaryIFSum = 0.0;

        void addPrincipal(double ifProm) {
            principalCount++;
            principalIFSum += ifProm;
        }

        void addSecondary(double ifProm) {
            secondaryCount++;
            secondaryIFSum += ifProm;
        }
    }

    private static class ThesisStats {
        Map<Integer, Integer> tutorCountByPeriod = new HashMap<>();
        Map<Integer, Integer> cotutorCountByPeriod = new HashMap<>();
        int enCursoTutor = 0;
        int terminadasTutor = 0;
        int postdoctoralesTutor = 0;
        int cotutor = 0;
        int enCursoTutorP5 = 0;
        int terminadasTutorP5 = 0;
        int postdoctoralesTutorP5 = 0;
        int cotutorP5 = 0;
        Set<String> uniqueTutorThesisIds = new HashSet<>();
        Set<String> uniqueCotutorThesisIds = new HashSet<>();
    }

    private static class ProjectStats {
        int countNational = 0;
        int countInternational = 0;
        double sumCLP = 0.0;
        double sumUSD = 0.0;

        void add(ProjectStats other) {
            if (other == null) return;
            countNational += other.countNational;
            countInternational += other.countInternational;
            sumCLP += other.sumCLP;
            sumUSD += other.sumUSD;
        }
    }

    private static class RowSnapshot {
        int originalRowIndex;
        String tipo;
        double sortKeyCD = Double.MAX_VALUE;
        Map<Integer, CellType> cellTypes = new HashMap<>();
        Map<Integer, Double> numericValues = new HashMap<>();
        Map<Integer, String> stringValues = new HashMap<>();
        Map<Integer, String> formulas = new HashMap<>();
        Map<Integer, CellStyle> styles = new HashMap<>();

        double getSortKeyCD() {
            return sortKeyCD;
        }
    }

    /**
     * Genera el reporte de productividad de investigadores.
     * 
     * @param projectsFile Archivo Excel con datos de proyectos (MultipartFile)
     * @return Array de bytes del Excel generado
     */
    @Transactional(readOnly = false)
    public byte[] generateReport(MultipartFile projectsFile) throws Exception {
        System.out.println("InvestigatorProductivityService.generateReport: Iniciando generación de reporte");

        // 1. Obtener datos de ScientificImpactReport directamente desde SQL
        System.out.println("Obteniendo datos de Impact Factor desde ScientificImpactReport...");
        Map<String, String> researcherType = new HashMap<>();
        Map<String, Map<Integer, PublicationStats>> stats = loadImpactDataFromSQL(researcherType);

        // 2. Obtener datos de thesis_report directamente desde SQL
        System.out.println("Obteniendo datos de tesis desde thesis_report...");
        Map<String, ThesisStats> thesisStats = loadThesisDataFromSQL();

        // 3. Procesar archivo de proyectos
        System.out.println("Procesando archivo de proyectos...");
        Map<String, ProjectStats> projectStats = loadProjectData(projectsFile);

        System.out.println("Procesando " + stats.size() + " investigadores únicos (publicaciones)");
        System.out.println("Procesando " + thesisStats.size() + " investigadores únicos (tesis)");
        System.out.println("Procesando " + projectStats.size() + " investigadores con datos de proyectos");

        // 4. Leer template y generar reporte
        System.out.println("Generando reporte Excel...");
        return generateProductivityWorkbook(stats, thesisStats, projectStats, researcherType);
    }

    /**
     * Obtiene datos de ScientificImpactReport directamente desde SQL.
     * Mapea: fullname (COL_NOMBRE), tiporrhh (COL_TIPO_INVESTIGADOR_IF), tipoParticipacion (COL_TIPO_PARTICIPACION),
     * corresponding (COL_CORRESPONDIENTE), progressReport (COL_PERIODO), basal (COL_BASAL), avgFI (COL_IF_PROM)
     */
    private Map<String, Map<Integer, PublicationStats>> loadImpactDataFromSQL(Map<String, String> researcherType) {
        Map<String, Map<Integer, PublicationStats>> result = new HashMap<>();
        if (researcherType == null) researcherType = new HashMap<>();

        String sql = "SELECT fullname, tiporrhh, tipoParticipacion, corresponding, progressReport, basal, avgFI " +
                     "FROM ScientificImpactReport " +
                     "WHERE fullname IS NOT NULL AND fullname != '' AND avgFI IS NOT NULL";

        try {
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();

            int rowCount = 0;
            for (Object[] row : rows) {
                String nombre = row[0] != null ? row[0].toString().trim() : null;
                String tipoInvestigador = row[1] != null ? row[1].toString() : null;
                String tipoParticipacion = row[2] != null ? row[2].toString() : null;
                String correspondiente = row[3] != null ? row[3].toString() : null;
                Object periodoObj = row[4]; // progressReport
                // Object basalObj = row[5]; // Basal field available if needed for filtering
                Object avgFIObj = row[6];

                if (nombre == null || nombre.isEmpty() || avgFIObj == null) {
                    continue;
                }

                // Mapear tipo de investigador
                String tipoParaPlanilla = mapTipoInvestigadorParaPlanilla(tipoInvestigador);
                if (tipoParaPlanilla != null) {
                    researcherType.putIfAbsent(nombre, tipoParaPlanilla);
                }

                // Parsear período
                Integer periodo = null;
                if (periodoObj != null) {
                    try {
                        if (periodoObj instanceof Number) {
                            periodo = ((Number) periodoObj).intValue();
                        } else {
                            periodo = Integer.parseInt(periodoObj.toString());
                        }
                        if (periodo < 1 || periodo > 5) {
                            System.out.println("Advertencia: Período fuera de rango (1-5): " + periodo);
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Advertencia: No se pudo parsear período: " + periodoObj);
                        continue;
                    }
                }

                // Parsear IF Prom
                double ifProm = 0.0;
                if (avgFIObj != null) {
                    try {
                        if (avgFIObj instanceof Number) {
                            ifProm = ((Number) avgFIObj).doubleValue();
                        } else {
                            ifProm = Double.parseDouble(avgFIObj.toString());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Advertencia: No se pudo parsear IF Prom: " + avgFIObj);
                    }
                }

                // Normalizar strings para comparación
                String tipoPartNormalized = normalizeString(tipoParticipacion);
                String corrNormalized = normalizeString(correspondiente);

                // Clasificar como principal o secundaria
                boolean isPrincipal = !"co-author".equals(tipoPartNormalized) || "si".equals(corrNormalized);

                // Actualizar estadísticas
                result.computeIfAbsent(nombre, k -> new HashMap<>())
                      .computeIfAbsent(periodo, k -> new PublicationStats());

                PublicationStats pubStats = result.get(nombre).get(periodo);
                if (isPrincipal) {
                    pubStats.addPrincipal(ifProm);
                } else {
                    pubStats.addSecondary(ifProm);
                }

                rowCount++;
            }

            System.out.println("Procesadas " + rowCount + " publicaciones válidas desde ScientificImpactReport");
        } catch (Exception e) {
            System.err.println("Error al obtener datos de ScientificImpactReport: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al obtener datos de ScientificImpactReport: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Obtiene datos de thesis_report directamente desde SQL.
     * Mapea: fullname, idTipoParticipacion (12=tutor, 13=cotutor), idThesis, periodo,
     * idEstadoTesis (1=terminada, 2=en progreso), idGradoAcademico (1=pregrado, 2=master, 3=postgrado)
     */
    private Map<String, ThesisStats> loadThesisDataFromSQL() {
        Map<String, ThesisStats> result = new HashMap<>();

        String sql = "SELECT fullname, idTipoParticipacion, idThesis, periodo, idEstadoTesis, idGradoAcademico " +
                     "FROM thesis_report " +
                     "WHERE fullname IS NOT NULL AND fullname != '' AND idThesis IS NOT NULL AND idThesis != ''";

        try {
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();

            int processedCount = 0;
            for (Object[] row : rows) {
                String nombre = row[0] != null ? row[0].toString().trim() : null;
                Object tipoObj = row[1];
                String idTesis = row[2] != null ? row[2].toString().trim() : null;
                Object periodoObj = row[3];
                Object estadoObj = row[4];
                Object gradoObj = row[5];

                if (nombre == null || nombre.isEmpty() || idTesis == null || idTesis.isEmpty()) {
                    continue;
                }

                // Parsear tipo: 12=tutor, 13=cotutor
                int tipo;
                try {
                    if (tipoObj instanceof Number) {
                        tipo = ((Number) tipoObj).intValue();
                    } else {
                        tipo = Integer.parseInt(tipoObj.toString());
                    }
                    if (tipo != 12 && tipo != 13) {
                        continue;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

                // Parsear período(s) - progressReport puede venir como "1,2,3"
                Set<Integer> periodosSet = new LinkedHashSet<>();
                try {
                    if (periodoObj instanceof Number) {
                        int p = ((Number) periodoObj).intValue();
                        if (p >= 1 && p <= 5) {
                            periodosSet.add(p);
                        }
                    } else if (periodoObj != null) {
                        String raw = periodoObj.toString();
                        for (String part : raw.split(",")) {
                            if (part == null) continue;
                            // Extraer solo dígitos (soporta casos tipo "5.." o "2023,5..")
                            String digits = part.replaceAll("\\D+", "");
                            if (digits == null || digits.isEmpty()) continue;
                            int p = Integer.parseInt(digits);
                            if (p >= 1 && p <= 5) {
                                periodosSet.add(p);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Si no se puede parsear, periodosSet quedará vacío y se hará continue
                }
                if (periodosSet.isEmpty()) {
                    continue;
                }

                // Parsear estado: 1=terminada, 2=en progreso
                int estado;
                try {
                    if (estadoObj instanceof Number) {
                        estado = ((Number) estadoObj).intValue();
                    } else {
                        estado = Integer.parseInt(estadoObj.toString());
                    }
                    if (estado != 1 && estado != 2) {
                        continue;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

                // Parsear grado: 1=pregrado, 2=master, 3=postgrado
                int grado;
                try {
                    if (gradoObj instanceof Number) {
                        grado = ((Number) gradoObj).intValue();
                    } else {
                        grado = Integer.parseInt(gradoObj.toString());
                    }
                    if (grado < 1 || grado > 3) {
                        continue;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

                // Obtener o crear estadísticas del investigador
                ThesisStats stats = result.computeIfAbsent(nombre, k -> new ThesisStats());

                boolean isTutor = (tipo == 12);
                boolean isCotutor = (tipo == 13);

                for (int periodo : periodosSet) {
                    if (periodo == 4) {
                        // Período 4: lógica especial
                        if (isTutor) {
                            if (estado == 2) { // En progreso
                                stats.enCursoTutor++;
                            } else if (estado == 1) { // Terminada
                                stats.terminadasTutor++;
                            }
                            if (grado == 3) { // Postdoctoral
                                stats.postdoctoralesTutor++;
                            }
                            stats.uniqueTutorThesisIds.add(idTesis);
                        } else if (isCotutor) {
                            stats.cotutor++;
                            stats.uniqueCotutorThesisIds.add(idTesis);
                        }
                    } else if (periodo == 5) {
                        // Período 5: mismo desglose que P4
                        if (isTutor) {
                            if (estado == 2) { // En progreso
                                stats.enCursoTutorP5++;
                            } else if (estado == 1) { // Terminada
                                stats.terminadasTutorP5++;
                            }
                            if (grado == 3) { // Postdoctoral
                                stats.postdoctoralesTutorP5++;
                            }
                            stats.uniqueTutorThesisIds.add(idTesis);
                        } else if (isCotutor) {
                            stats.cotutorP5++;
                            stats.uniqueCotutorThesisIds.add(idTesis);
                        }
                    } else {
                        // Períodos 1-3: contar tutor y cotutor por período
                        if (isTutor) {
                            stats.tutorCountByPeriod.put(periodo,
                                stats.tutorCountByPeriod.getOrDefault(periodo, 0) + 1);
                            stats.uniqueTutorThesisIds.add(idTesis);
                        } else if (isCotutor) {
                            stats.cotutorCountByPeriod.put(periodo,
                                stats.cotutorCountByPeriod.getOrDefault(periodo, 0) + 1);
                            stats.uniqueCotutorThesisIds.add(idTesis);
                        }
                    }
                }

                processedCount++;
            }

            System.out.println("Procesadas " + processedCount + " tesis válidas desde thesis_report");
            System.out.println("Investigadores con tesis: " + result.size());
        } catch (Exception e) {
            System.err.println("Error al obtener datos de thesis_report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al obtener datos de thesis_report: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Procesa el archivo Excel de proyectos subido.
     */
    private Map<String, ProjectStats> loadProjectData(MultipartFile projectsFile) throws IOException {
        Map<String, ProjectStats> result = new HashMap<>();

        if (projectsFile == null || projectsFile.isEmpty()) {
            System.out.println("Advertencia: No se proporcionó archivo de proyectos, se omiten datos de proyectos");
            return result;
        }

        try (Workbook workbook = new XSSFWorkbook(projectsFile.getInputStream())) {
            Sheet sheet = workbook.getSheet(SHEET_PROYECTOS);
            if (sheet == null) {
                System.out.println("Advertencia: No se encontró la hoja '" + SHEET_PROYECTOS + "', se omiten datos de proyectos");
                return result;
            }

            int rowCount = 0;
            for (int r = PROYECTOS_ROW_DATA_START; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String nombre = getCellValueAsString(row, PROYECTOS_COL_NOMBRE);
                if (nombre == null || nombre.trim().isEmpty()) continue;

                nombre = nombre.trim();

                String tipo = getCellValueAsString(row, PROYECTOS_COL_TIPO);
                boolean esInternacional = tipo != null && PROYECTO_INTERNACIONAL.equalsIgnoreCase(tipo.trim());

                double clp = parseCellNumeric(row.getCell(PROYECTOS_COL_CLP));

                ProjectStats stats = result.computeIfAbsent(nombre, k -> new ProjectStats());
                if (esInternacional) {
                    stats.countInternational++;
                    stats.sumUSD += clp;
                } else {
                    stats.countNational++;
                    stats.sumCLP += clp;
                }
                rowCount++;
            }

            System.out.println("Procesadas " + rowCount + " filas de proyectos; investigadores con datos: " + result.size());
        }

        return result;
    }

    /**
     * Genera el archivo Excel de productividad a partir del template.
     */
    private byte[] generateProductivityWorkbook(
            Map<String, Map<Integer, PublicationStats>> stats,
            Map<String, ThesisStats> thesisStats,
            Map<String, ProjectStats> projectStats,
            Map<String, String> researcherType) throws IOException {

        // Leer template
        String templatePath = "tpl/Productividad Investigadores Asociados-tpl.xlsx";
        java.nio.file.Path path = Paths.get(templatePath);
        if (!path.toFile().exists()) {
            throw new IOException("Template no encontrado en: " + templatePath);
        }

        try (FileInputStream fis = new FileInputStream(path.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("El archivo no contiene hojas");
            }

            // Encontrar fila de totales
            int totalRowIndex = -1;
            for (int i = PRODUCTIVITY_DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String cellValue = getCellValueAsString(row, COL_INVESTIGADOR);
                    if ("TOTAL".equals(cellValue)) {
                        totalRowIndex = i;
                        break;
                    }
                }
            }

            if (totalRowIndex == -1) {
                throw new IOException("No se encontró la fila TOTAL en el archivo");
            }

            // Obtener fila plantilla
            Row templateRow = sheet.getRow(PRODUCTIVITY_DATA_START_ROW);
            if (templateRow == null) {
                throw new IOException("No se encontró la fila plantilla (fila 4)");
            }

            // Crear mapa de investigadores existentes
            Map<String, Row> researcherRows = new HashMap<>();
            for (int i = PRODUCTIVITY_DATA_START_ROW + 1; i < totalRowIndex; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String nombre = getCellValueAsString(row, COL_INVESTIGADOR);
                if (nombre != null && !nombre.trim().isEmpty() && !"TOTAL".equals(nombre)) {
                    researcherRows.put(nombre.trim(), row);
                }
            }

            boolean templateRowUsed = false;

            // Procesar cada investigador de las estadísticas
            for (Map.Entry<String, Map<Integer, PublicationStats>> entry : stats.entrySet()) {
                String nombre = entry.getKey();
                Map<Integer, PublicationStats> periodStats = entry.getValue();

                Row researcherRow = researcherRows.get(nombre);

                if (researcherRow == null) {
                    if (!templateRowUsed) {
                        System.out.println("Reemplazando fila plantilla con investigador: " + nombre);
                        researcherRow = replaceTemplateRowWithData(sheet, templateRow, nombre);
                        templateRowUsed = true;
                    } else {
                        System.out.println("Creando nueva fila para investigador: " + nombre);
                        researcherRow = createResearcherRow(sheet, templateRow, totalRowIndex, nombre);
                    }
                    researcherRows.put(nombre, researcherRow);
                }

                // Actualizar valores para cada período
                updatePeriodData(researcherRow, 1, periodStats.get(1), P1_COL_A_PPAL, P1_COL_IMP_PPAL, P1_COL_A_SEC, P1_COL_IMP_SEC);
                updatePeriodData(researcherRow, 2, periodStats.get(2), P2_COL_A_PPAL, P2_COL_IMP_PPAL, P2_COL_A_SEC, P2_COL_IMP_SEC);
                updatePeriodData(researcherRow, 3, periodStats.get(3), P3_COL_A_PPAL, P3_COL_IMP_PPAL, P3_COL_A_SEC, P3_COL_IMP_SEC);
                updatePeriodData(researcherRow, 4, periodStats.get(4), P4_COL_A_PPAL, P4_COL_IMP_PPAL, P4_COL_A_SEC, P4_COL_IMP_SEC);
                updatePeriodData(researcherRow, 5, periodStats.get(5), P5_COL_A_PPAL, P5_COL_IMP_PPAL, P5_COL_A_SEC, P5_COL_IMP_SEC);
            }

            // Si la fila plantilla no se usó, eliminarla
            if (!templateRowUsed) {
                System.out.println("Eliminando fila plantilla no utilizada");
                sheet.removeRow(templateRow);
                int lastRow = sheet.getLastRowNum();
                if (PRODUCTIVITY_DATA_START_ROW <= lastRow) {
                    sheet.shiftRows(PRODUCTIVITY_DATA_START_ROW + 1, lastRow, -1);
                }
                totalRowIndex--;
            }

            // Recalcular totalRowIndex
            totalRowIndex = -1;
            for (int i = PRODUCTIVITY_DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String cellValue = getCellValueAsString(row, COL_INVESTIGADOR);
                    if ("TOTAL".equals(cellValue)) {
                        totalRowIndex = i;
                        break;
                    }
                }
            }

            if (totalRowIndex == -1) {
                throw new IOException("No se encontró la fila TOTAL después de procesar los datos");
            }

            // Renumerar correlativos
            int currentCorrelativo = 1;
            for (int i = PRODUCTIVITY_DATA_START_ROW; i < totalRowIndex; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String nombre = getCellValueAsString(row, COL_INVESTIGADOR);
                if (nombre != null && !nombre.trim().isEmpty() && !"TOTAL".equals(nombre)) {
                    setCellValue(row, COL_CORRELATIVO, currentCorrelativo);
                    currentCorrelativo++;
                }
            }

            // Actualizar datos de tesis
            System.out.println("Actualizando datos de tesis...");
            updateThesisData(sheet, thesisStats, totalRowIndex);

            // Actualizar datos de proyectos
            System.out.println("Actualizando datos de proyectos...");
            Map<String, ProjectStats> projectStatsByPlanilla = aggregateProjectStatsByPlanillaNames(projectStats, researcherRows.keySet());
            updateProjectData(sheet, researcherRows, projectStatsByPlanilla);

            // Columna D: Principal / Asociado
            Map<String, String> tipoPorPlanilla = aggregateResearcherTypeByPlanillaNames(researcherType, researcherRows.keySet());
            for (Map.Entry<String, Row> entry : researcherRows.entrySet()) {
                String tipo = tipoPorPlanilla.get(entry.getKey());
                if (tipo != null && !tipo.isEmpty()) {
                    setCellValue(entry.getValue(), COL_TIPO_INVESTIGADOR, tipo);
                }
            }

            // Evaluar fórmulas antes de reordenar
            FormulaEvaluator evaluatorForReorder = workbook.getCreationHelper().createFormulaEvaluator();
            evaluatorForReorder.clearAllCachedResultValues();
            for (int r = PRODUCTIVITY_DATA_START_ROW; r < totalRowIndex; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        evaluatorForReorder.evaluateFormulaCell(cell);
                    }
                }
            }

            // Ordenar filas: Principal (ordenados por CD), línea en blanco, Asociado (ordenados por CD)
            reorderRowsByTipo(sheet, templateRow, totalRowIndex, evaluatorForReorder);

            // Recalcular totalRowIndex tras insertar la fila en blanco
            totalRowIndex = -1;
            for (int i = PRODUCTIVITY_DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && "TOTAL".equals(getCellValueAsString(row, COL_INVESTIGADOR))) {
                    totalRowIndex = i;
                    break;
                }
            }
            if (totalRowIndex == -1) {
                throw new IOException("No se encontró la fila TOTAL después de reordenar");
            }

            int firstTableSize = totalRowIndex - PRODUCTIVITY_DATA_START_ROW + 1;
            int gapRows = 3;
            int copyStartRow = totalRowIndex + 1 + gapRows;

            // Copiar la tabla completa 3 filas más abajo
            copyTableDown(sheet, PRODUCTIVITY_DATA_START_ROW, firstTableSize, gapRows);

            // Evaluar fórmulas de toda la hoja
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.clearAllCachedResultValues();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        evaluator.evaluateFormulaCell(cell);
                    }
                }
            }

            // Reevaluar solo la tabla copiada
            int secondTableEnd = copyStartRow + firstTableSize;
            for (int pass = 0; pass < 3; pass++) {
                for (int r = copyStartRow; r < secondTableEnd; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.FORMULA) {
                            evaluator.evaluateFormulaCell(cell);
                        }
                    }
                }
            }

            // Ordenar solo los datos de la tabla copiada por BY
            int secondTableDataEnd = copyStartRow + firstTableSize - 1;
            reorderTableInRangeByColumn(sheet, copyStartRow, secondTableDataEnd, COL_BY_GRADIENT, evaluator);

            // Reevaluar la tabla copiada tras el reorden
            evaluator.clearAllCachedResultValues();
            for (int r = copyStartRow; r < secondTableEnd; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        evaluator.evaluateFormulaCell(cell);
                    }
                }
            }

            // Formato condicional por gradiente: primera tabla
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_BX_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_BY_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_BZ_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CA_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CB_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CC_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CD_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CE_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, PRODUCTIVITY_DATA_START_ROW, totalRowIndex, COL_CF_GRADIENT);

            // Aplicar gradientes de nuevo a la tabla copiada
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_BX_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_BY_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_BZ_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CA_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CB_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CC_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CD_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CE_GRADIENT);
            applyGradientToColumn(sheet, workbook, evaluator, copyStartRow, secondTableEnd, COL_CF_GRADIENT);

            // Guardar archivo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // Métodos auxiliares (continúan en el siguiente bloque debido a la extensión...)

    private void updateThesisData(Sheet sheet, Map<String, ThesisStats> thesisStats, int totalRowIndex) {
        Map<String, Row> researcherRows = new HashMap<>();
        
        for (int i = PRODUCTIVITY_DATA_START_ROW; i < totalRowIndex; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String nombre = getCellValueAsString(row, COL_INVESTIGADOR);
            if (nombre != null && !nombre.trim().isEmpty() && !"TOTAL".equals(nombre)) {
                researcherRows.put(nombre.trim(), row);
            }
        }

        for (Map.Entry<String, ThesisStats> entry : thesisStats.entrySet()) {
            String nombre = entry.getKey();
            ThesisStats stats = entry.getValue();

            Row researcherRow = researcherRows.get(nombre);
            if (researcherRow == null) {
                System.out.println("Advertencia: Investigador " + nombre + " no encontrado en archivo de productividad para tesis");
                continue;
            }

            // Período 1: tutor y cotutor
            setCellValue(researcherRow, P1_COL_TESIS_TUTOR, stats.tutorCountByPeriod.getOrDefault(1, 0));
            setCellValue(researcherRow, P1_COL_TESIS_COTUTOR, stats.cotutorCountByPeriod.getOrDefault(1, 0));
            // Período 2: tutor y cotutor
            setCellValue(researcherRow, P2_COL_TESIS_TUTOR, stats.tutorCountByPeriod.getOrDefault(2, 0));
            setCellValue(researcherRow, P2_COL_TESIS_COTUTOR, stats.cotutorCountByPeriod.getOrDefault(2, 0));
            // Período 3: tutor y cotutor
            setCellValue(researcherRow, P3_COL_TESIS_TUTOR, stats.tutorCountByPeriod.getOrDefault(3, 0));
            setCellValue(researcherRow, P3_COL_TESIS_COTUTOR, stats.cotutorCountByPeriod.getOrDefault(3, 0));
            // Período 4: desglose especial
            setCellValue(researcherRow, P4_COL_TESIS_EN_CURSO_TUTOR, stats.enCursoTutor);
            setCellValue(researcherRow, P4_COL_TESIS_TERMINADAS_TUTOR, stats.terminadasTutor);
            setCellValue(researcherRow, P4_COL_TESIS_POSTDOCTORAL_TUTOR, stats.postdoctoralesTutor);
            setCellValue(researcherRow, P4_COL_TESIS_COTUTOR, stats.cotutor);
            // Período 5: desglose especial
            setCellValue(researcherRow, P5_COL_TESIS_EN_CURSO_TUTOR, stats.enCursoTutorP5);
            setCellValue(researcherRow, P5_COL_TESIS_TERMINADAS_TUTOR, stats.terminadasTutorP5);
            setCellValue(researcherRow, P5_COL_TESIS_POSTDOCTORAL_TUTOR, stats.postdoctoralesTutorP5);
            setCellValue(researcherRow, P5_COL_TESIS_COTUTOR, stats.cotutorP5);
            // Totales
            setCellValue(researcherRow, COL_TOTAL_TESIS_TUTOR, stats.uniqueTutorThesisIds.size());
            setCellValue(researcherRow, COL_TOTAL_TESIS_COTUTOR, stats.uniqueCotutorThesisIds.size());
        }
    }

    private void reorderRowsByTipo(Sheet sheet, Row templateRow, int totalRowIndex, FormulaEvaluator evaluator) {
        int start = PRODUCTIVITY_DATA_START_ROW;
        int numRows = totalRowIndex - start;
        if (numRows <= 0) return;

        List<RowSnapshot> snapshots = new ArrayList<>();
        int maxCol = 0;
        for (int r = start; r < totalRowIndex; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String tipo = getCellValueAsString(row, COL_TIPO_INVESTIGADOR);
            if (tipo != null) tipo = tipo.trim();
            RowSnapshot snap = readRowSnapshot(sheet, row, r);
            snap.tipo = tipo;
            Double cdValue = getCellValueAsDouble(row, COL_CF_GRADIENT, evaluator);
            snap.sortKeyCD = cdValue != null ? cdValue : Double.MIN_VALUE;
            snapshots.add(snap);
            if (row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
        }

        List<RowSnapshot> principal = new ArrayList<>();
        List<RowSnapshot> asociado = new ArrayList<>();
        List<RowSnapshot> otros = new ArrayList<>();
        for (RowSnapshot s : snapshots) {
            if ("Principal".equalsIgnoreCase(s.tipo)) principal.add(s);
            else if ("Asociado".equalsIgnoreCase(s.tipo)) asociado.add(s);
            else otros.add(s);
        }
        principal.sort(Comparator.comparingDouble(RowSnapshot::getSortKeyCD).reversed());
        asociado.sort(Comparator.comparingDouble(RowSnapshot::getSortKeyCD).reversed());
        List<RowSnapshot> ordered = new ArrayList<>(principal);
        ordered.add(null); // fila en blanco
        ordered.addAll(asociado);
        ordered.addAll(otros);

        RowSnapshot blankTemplate = templateRow != null ? readRowSnapshot(sheet, templateRow, templateRow.getRowNum()) : null;

        sheet.shiftRows(totalRowIndex, sheet.getLastRowNum(), 1);

        int correlativo = 1;
        for (int i = 0; i < ordered.size(); i++) {
            int targetRowIndex = start + i;
            Row targetRow = sheet.getRow(targetRowIndex);
            if (targetRow == null) targetRow = sheet.createRow(targetRowIndex);

            RowSnapshot snap = ordered.get(i);
            if (snap == null) {
                copyBlankRowFromSnapshot(sheet, blankTemplate, targetRow, maxCol);
            } else {
                writeRowSnapshot(sheet, snap, targetRow, targetRowIndex, maxCol);
                setCellValue(targetRow, COL_CORRELATIVO, correlativo++);
            }
        }
    }

    private void copyTableDown(Sheet sheet, int startRow, int tableSize, int gapRows) {
        if (tableSize <= 0) return;
        int maxCol = 0;
        for (int r = startRow; r < startRow + tableSize; r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
        }
        int copyStart = startRow + tableSize + gapRows;
        for (int k = 0; k < gapRows; k++) {
            sheet.createRow(startRow + tableSize + k);
        }
        for (int i = 0; i < tableSize; i++) {
            Row sourceRow = sheet.getRow(startRow + i);
            if (sourceRow == null) continue;
            RowSnapshot snap = readRowSnapshot(sheet, sourceRow, startRow + i);
            Row targetRow = sheet.createRow(copyStart + i);
            writeRowSnapshot(sheet, snap, targetRow, copyStart + i, maxCol);
        }
    }

    private void reorderTableInRangeByColumn(Sheet sheet, int rangeStart, int rangeEnd, int sortColumn, FormulaEvaluator evaluator) {
        int size = rangeEnd - rangeStart;
        if (size <= 0) return;
        int maxCol = 0;
        for (int r = rangeStart; r < rangeEnd; r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
        }
        List<RowSnapshot> snapshots = new ArrayList<>();
        for (int r = rangeStart; r < rangeEnd; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String tipo = getCellValueAsString(row, COL_TIPO_INVESTIGADOR);
            if (tipo != null) tipo = tipo.trim();
            RowSnapshot snap = readRowSnapshot(sheet, row, r);
            snap.tipo = tipo;
            Double sortVal = getCellValueAsDouble(row, sortColumn, evaluator);
            snap.sortKeyCD = sortVal != null ? sortVal : Double.MIN_VALUE;
            snapshots.add(snap);
        }
        List<RowSnapshot> principal = new ArrayList<>();
        List<RowSnapshot> asociado = new ArrayList<>();
        List<RowSnapshot> blank = new ArrayList<>();
        List<RowSnapshot> otros = new ArrayList<>();
        for (RowSnapshot s : snapshots) {
            String nombre = s.stringValues != null ? s.stringValues.get(COL_INVESTIGADOR) : null;
            boolean esBlanco = nombre == null || nombre.trim().isEmpty();
            if ("Principal".equalsIgnoreCase(s.tipo)) principal.add(s);
            else if ("Asociado".equalsIgnoreCase(s.tipo)) asociado.add(s);
            else if (esBlanco && blank.isEmpty()) blank.add(s);
            else otros.add(s);
        }
        principal.sort(Comparator.comparingDouble(RowSnapshot::getSortKeyCD).reversed());
        asociado.sort(Comparator.comparingDouble(RowSnapshot::getSortKeyCD).reversed());
        List<RowSnapshot> ordered = new ArrayList<>(principal);
        ordered.addAll(blank);
        ordered.addAll(asociado);
        ordered.addAll(otros);
        RowSnapshot blankTemplate = snapshots.isEmpty() ? null : snapshots.get(0);
        int correlativo = 1;
        for (int i = 0; i < ordered.size(); i++) {
            int targetRowIndex = rangeStart + i;
            Row targetRow = sheet.getRow(targetRowIndex);
            if (targetRow == null) targetRow = sheet.createRow(targetRowIndex);
            RowSnapshot snap = ordered.get(i);
            if (snap != null && blank.contains(snap)) {
                copyBlankRowFromSnapshot(sheet, blankTemplate, targetRow, maxCol);
            } else if (snap != null) {
                writeRowSnapshot(sheet, snap, targetRow, targetRowIndex, maxCol);
                setCellValue(targetRow, COL_CORRELATIVO, correlativo++);
            }
        }
    }

    private RowSnapshot readRowSnapshot(Sheet sheet, Row row, int rowIndex) {
        RowSnapshot s = new RowSnapshot();
        s.originalRowIndex = rowIndex;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            s.cellTypes.put(c, cell.getCellType());
            s.styles.put(c, cell.getCellStyle());
            if (cell.getCellType() == CellType.NUMERIC) {
                s.numericValues.put(c, cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                s.stringValues.put(c, cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.FORMULA) {
                s.formulas.put(c, cell.getCellFormula());
            } else if (cell.getCellType() == CellType.BOOLEAN) {
                s.numericValues.put(c, cell.getBooleanCellValue() ? 1.0 : 0.0);
            }
        }
        return s;
    }

    private void writeRowSnapshot(Sheet sheet, RowSnapshot s, Row targetRow, int targetRowIndex, int maxCol) {
        for (int c = 0; c < maxCol; c++) {
            CellType type = s.cellTypes.get(c);
            if (type == null) continue;
            Cell targetCell = targetRow.getCell(c);
            if (targetCell == null) targetCell = targetRow.createCell(c);
            if (s.styles.get(c) != null) targetCell.setCellStyle(s.styles.get(c));
            if (type == CellType.FORMULA) {
                String formula = s.formulas.get(c);
                if (formula != null) {
                    String adjusted = adjustFormulaRowReferences(formula, s.originalRowIndex, targetRowIndex);
                    targetCell.setCellFormula(adjusted);
                }
            } else if (type == CellType.NUMERIC && s.numericValues.containsKey(c)) {
                targetCell.setCellValue(s.numericValues.get(c));
            } else if (type == CellType.STRING && s.stringValues.containsKey(c)) {
                targetCell.setCellValue(s.stringValues.get(c));
            } else if (type == CellType.BOOLEAN && s.numericValues.containsKey(c)) {
                targetCell.setCellValue(s.numericValues.get(c) != 0);
            }
        }
    }

    private void copyBlankRowFromSnapshot(Sheet sheet, RowSnapshot styleSource, Row targetRow, int maxCol) {
        for (int c = 0; c < maxCol; c++) {
            Cell target = targetRow.getCell(c);
            if (target == null) target = targetRow.createCell(c);
            if (styleSource != null && styleSource.styles.get(c) != null) {
                target.setCellStyle(styleSource.styles.get(c));
            }
            target.setBlank();
        }
    }

    private void updateProjectData(Sheet sheet, Map<String, Row> researcherRows, Map<String, ProjectStats> projectStats) {
        if (projectStats == null) return;
        for (Map.Entry<String, Row> entry : researcherRows.entrySet()) {
            String nombre = entry.getKey();
            Row row = entry.getValue();
            ProjectStats stats = projectStats.get(nombre);
            int cantNac = stats != null ? stats.countNational : 0;
            int cantInt = stats != null ? stats.countInternational : 0;
            double montoNac = stats != null ? stats.sumCLP : 0.0;
            double montoInt = stats != null ? stats.sumUSD : 0.0;
            setCellValue(row, COL_PROYECTOS_CANT_NACIONAL, cantNac);
            setCellValue(row, COL_PROYECTOS_CANT_INTERNACIONAL, cantInt);
            setCellValue(row, COL_PROYECTOS_MONTO_NACIONAL, montoNac);
            setCellValue(row, COL_PROYECTOS_MONTO_INTERNACIONAL, montoInt);
        }
    }

    private Row replaceTemplateRowWithData(Sheet sheet, Row templateRow, String nombre) {
        Cell correlativoCell = templateRow.getCell(COL_CORRELATIVO);
        if (correlativoCell != null && correlativoCell.getCellType() == CellType.NUMERIC) {
            correlativoCell.setCellValue(0);
        }
        Cell nombreCell = templateRow.getCell(COL_INVESTIGADOR);
        if (nombreCell == null) {
            nombreCell = templateRow.createCell(COL_INVESTIGADOR);
            if (templateRow.getCell(COL_CORRELATIVO) != null && 
                templateRow.getCell(COL_CORRELATIVO).getCellStyle() != null) {
                nombreCell.setCellStyle(templateRow.getCell(COL_CORRELATIVO).getCellStyle());
            }
        }
        nombreCell.setCellValue(nombre);
        return templateRow;
    }

    private Row createResearcherRow(Sheet sheet, Row templateRow, int totalRowIndex, String nombre) {
        sheet.shiftRows(totalRowIndex, sheet.getLastRowNum(), 1);
        Row newRow = sheet.createRow(totalRowIndex);

        for (int colIndex = 0; colIndex < templateRow.getLastCellNum(); colIndex++) {
            Cell templateCell = templateRow.getCell(colIndex);
            if (templateCell == null) continue;

            Cell newCell = newRow.createCell(colIndex);

            if (templateCell.getCellStyle() != null) {
                newCell.setCellStyle(templateCell.getCellStyle());
            }

            if (templateCell.getCellType() == CellType.FORMULA) {
                String formula = templateCell.getCellFormula();
                String adjustedFormula = adjustFormulaRowReferences(formula, templateRow.getRowNum(), newRow.getRowNum());
                newCell.setCellFormula(adjustedFormula);
            } else if (colIndex == COL_CORRELATIVO) {
                // No copiar el valor del correlativo
            } else if (templateCell.getCellType() == CellType.NUMERIC) {
                newCell.setCellValue(templateCell.getNumericCellValue());
            } else if (templateCell.getCellType() == CellType.STRING) {
                newCell.setCellValue(templateCell.getStringCellValue());
            } else if (templateCell.getCellType() == CellType.BOOLEAN) {
                newCell.setCellValue(templateCell.getBooleanCellValue());
            }

            int columnWidth = sheet.getColumnWidth(colIndex);
            if (columnWidth > 0) {
                sheet.setColumnWidth(colIndex, columnWidth);
            }
        }

        Cell nombreCell = newRow.getCell(COL_INVESTIGADOR);
        if (nombreCell == null) {
            nombreCell = newRow.createCell(COL_INVESTIGADOR);
        }
        nombreCell.setCellValue(nombre);

        return newRow;
    }

    private String adjustFormulaRowReferences(String formula, int sourceRow, int targetRow) {
        int rowDiff = targetRow - sourceRow;
        if (rowDiff == 0) {
            return formula;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < formula.length()) {
            char c = formula.charAt(i);
            if (Character.isLetter(c)) {
                int colStart = i;
                while (i < formula.length() && Character.isLetter(formula.charAt(i))) {
                    i++;
                }
                String colPart = formula.substring(colStart, i);

                if (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                    int rowStart = i;
                    while (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                        i++;
                    }
                    String rowStr = formula.substring(rowStart, i);
                    try {
                        int rowNum = Integer.parseInt(rowStr) - 1;
                        int newRowNum = rowNum + rowDiff + 1;
                        result.append(colPart).append(newRowNum);
                    } catch (NumberFormatException e) {
                        result.append(colPart).append(rowStr);
                    }
                } else {
                    result.append(colPart);
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private void updatePeriodData(Row row, int periodo, PublicationStats stats,
                                 int colAPpal, int colImpPpal, int colASec, int colImpSec) {
        if (stats == null) {
            setCellValue(row, colAPpal, 0);
            setCellValue(row, colImpPpal, 0.0);
            setCellValue(row, colASec, 0);
            setCellValue(row, colImpSec, 0.0);
            return;
        }

        setCellValue(row, colAPpal, stats.principalCount);
        setCellValue(row, colImpPpal, stats.principalIFSum);
        setCellValue(row, colASec, stats.secondaryCount);
        setCellValue(row, colImpSec, stats.secondaryIFSum);
    }

    private void setCellValue(Row row, int colIndex, int value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value);
    }

    private void setCellValue(Row row, int colIndex, double value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value);
    }

    private void setCellValue(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private String getCellValueAsString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    switch (cell.getCachedFormulaResultType()) {
                        case STRING:
                            return cell.getRichStringCellValue().getString();
                        case NUMERIC:
                            double numValue = cell.getNumericCellValue();
                            if (numValue == (long) numValue) {
                                return String.valueOf((long) numValue);
                            } else {
                                return String.valueOf(numValue);
                            }
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    return "";
                }
            default:
                return "";
        }
    }

    private double parseCellNumeric(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue();
            if (s == null || s.trim().isEmpty()) return 0.0;
            try {
                return Double.parseDouble(s.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private Set<String> nameToTokens(String name) {
        if (name == null || name.trim().isEmpty()) return Collections.emptySet();
        String n = normalizeString(name).replace("-", " ").replaceAll("\\s+", " ").trim();
        if (n.isEmpty()) return Collections.emptySet();
        Set<String> tokens = new HashSet<>();
        for (String t : n.split(" ")) {
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    private String findBestMatchingPlanillaName(String projectName, Set<String> planillaNames) {
        Set<String> projectTokens = nameToTokens(projectName);
        if (projectTokens.isEmpty()) return null;

        String best = null;
        int bestTokenCount = Integer.MAX_VALUE;

        for (String planillaName : planillaNames) {
            Set<String> planillaTokens = nameToTokens(planillaName);
            if (!planillaTokens.containsAll(projectTokens)) continue;
            int n = planillaTokens.size();
            if (n < bestTokenCount) {
                bestTokenCount = n;
                best = planillaName;
            }
        }
        return best;
    }

    private Map<String, ProjectStats> aggregateProjectStatsByPlanillaNames(
            Map<String, ProjectStats> projectStats, Set<String> planillaNames) {
        Map<String, ProjectStats> aggregated = new HashMap<>();
        if (projectStats == null || planillaNames == null) return aggregated;

        for (Map.Entry<String, ProjectStats> entry : projectStats.entrySet()) {
            String projectName = entry.getKey();
            ProjectStats stats = entry.getValue();
            String planillaName = findBestMatchingPlanillaName(projectName, planillaNames);
            if (planillaName == null) continue;
            aggregated.computeIfAbsent(planillaName, k -> new ProjectStats()).add(stats);
        }
        return aggregated;
    }

    private String mapTipoInvestigadorParaPlanilla(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) return null;
        String t = normalizeString(tipo);
        if (t.contains("principal")) return "Principal";
        if (t.contains("asociado")) return "Asociado";
        return null;
    }

    private Map<String, String> aggregateResearcherTypeByPlanillaNames(
            Map<String, String> researcherType, Set<String> planillaNames) {
        Map<String, String> aggregated = new HashMap<>();
        if (researcherType == null || planillaNames == null) return aggregated;
        for (Map.Entry<String, String> entry : researcherType.entrySet()) {
            String impactName = entry.getKey();
            String type = entry.getValue();
            String planillaName = findBestMatchingPlanillaName(impactName, planillaNames);
            if (planillaName != null) aggregated.put(planillaName, type);
        }
        return aggregated;
    }

    private void applyGradientToColumn(Sheet sheet, Workbook workbook, FormulaEvaluator evaluator,
                                      int firstRow, int lastRowExclusive, int colIndex) {
        if (!(workbook instanceof XSSFWorkbook)) return;

        List<Double> values = new ArrayList<>();
        for (int r = firstRow; r < lastRowExclusive; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Double v = getCellValueAsDouble(row, colIndex, evaluator);
            if (v != null) values.add(v);
        }

        if (values.isEmpty()) return;

        // Especificación ANID:
        // - mínimo de la columna => gris
        // - máximo de la columna => verde oscuro
        // - intermedio => gradiente lineal entre gris y verde oscuro
        double minVal = values.stream().min(Double::compareTo).orElse(0.0);
        double maxVal = values.stream().max(Double::compareTo).orElse(0.0);
        double span = maxVal - minVal;

        Map<String, CellStyle> styleCache = new HashMap<>();
        for (int r = firstRow; r < lastRowExclusive; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Double value = getCellValueAsDouble(row, colIndex, evaluator);
            if (value == null) continue;

            String hex;
            double ratio;
            if (span <= GRADIENT_NEUTRAL_EPSILON) {
                ratio = 0.0; // todos iguales => color mínimo (gris)
            } else {
                ratio = (value - minVal) / span;
                ratio = Math.min(1.0, Math.max(0.0, ratio));
            }
            // Dos tramos para que el color "neutral" aparezca en el punto medio (ratio=0.5)
            if (ratio <= 0.5) {
                double t = ratio * 2.0; // 0..1
                hex = interpolateHexColor(GRADIENT_GRIS_HEX, GRADIENT_NEUTRAL_HEX, t);
            } else {
                double t = (ratio - 0.5) * 2.0; // 0..1
                hex = interpolateHexColor(GRADIENT_NEUTRAL_HEX, GRADIENT_VERDE_OSCURO_HEX, t);
            }

            CellStyle style = getOrCreateGradientStyle((XSSFWorkbook) workbook, hex, styleCache);
            Cell cell = row.getCell(colIndex);
            if (cell == null) cell = row.createCell(colIndex);
            cell.setCellStyle(style);
        }
    }

    private Double getCellValueAsDouble(Row row, int colIndex, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            try {
                CellValue cv = evaluator.evaluate(cell);
                if (cv != null && cv.getCellType() == CellType.NUMERIC) {
                    return cv.getNumberValue();
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getNumericCellValue();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String interpolateHexColor(String hex1, String hex2, double ratio) {
        int r1 = Integer.parseInt(hex1.substring(0, 2), 16);
        int g1 = Integer.parseInt(hex1.substring(2, 4), 16);
        int b1 = Integer.parseInt(hex1.substring(4, 6), 16);
        int r2 = Integer.parseInt(hex2.substring(0, 2), 16);
        int g2 = Integer.parseInt(hex2.substring(2, 4), 16);
        int b2 = Integer.parseInt(hex2.substring(4, 6), 16);
        int r = (int) Math.round(r1 + (r2 - r1) * ratio);
        int g = (int) Math.round(g1 + (g2 - g1) * ratio);
        int b = (int) Math.round(b1 + (b2 - b1) * ratio);
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));
        return String.format("%02X%02X%02X", r, g, b);
    }

    private CellStyle getOrCreateGradientStyle(XSSFWorkbook workbook, String hex, Map<String, CellStyle> cache) {
        if (cache.containsKey(hex)) return cache.get(hex);
        byte[] rgb = new byte[]{
                (byte) (Integer.parseInt(hex.substring(0, 2), 16) & 0xFF),
                (byte) (Integer.parseInt(hex.substring(2, 4), 16) & 0xFF),
                (byte) (Integer.parseInt(hex.substring(4, 6), 16) & 0xFF)
        };
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        XSSFColor color = new XSSFColor(rgb, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Importante: al cambiar a un nuevo CellStyle se pierde el formato numérico original del template.
        // Forzamos que los valores se rendericen con 1 decimal (ej: 1.0), tal como pide ANID/template.
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
        cache.put(hex, style);
        return style;
    }

    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        String normalized = Normalizer.normalize(str.trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }
}
