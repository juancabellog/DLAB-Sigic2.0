package com.sisgic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.sisgic.service.AnalysisService;
import com.sisgic.service.ReportService;
import com.sisgic.service.InvestigatorProductivityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private InvestigatorProductivityService investigatorProductivityService;

    /**
     * Consulta un grafo según el tipo especificado
     * @param request Mapa con: type (int), periods (List<Integer>), rrhhTypes (List<Integer>)
     * @return Grafo con nodos, enlaces y categorías
     */
    @PostMapping("/graph-query")
    public ResponseEntity<Map<String, Object>> queryGraph(@RequestBody Map<String, Object> request) {
        try {
            // El método queryGraph es el equivalente al método consultar del servicio antiguo
            Map<String, Object> result = analysisService.queryGraph(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Log para debugging
            return ResponseEntity.status(500).body(createErrorResponse("Error processing graph query: " + e.getMessage()));
        }
    }

    /**
     * Consulta publicaciones relacionadas a un enlace del grafo
     * @param request Mapa con: filter.type, filter.periods, filter.from, filter.to
     * @return Lista de publicaciones
     */
    @PostMapping("/publication-query")
    public ResponseEntity<List<Map<String, Object>>> queryPublications(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> filter = (Map<String, Object>) request.get("filter");
            List<Map<String, Object>> publications = analysisService.queryPublications(filter);
            return ResponseEntity.ok(publications);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Genera un reporte Excel según el idReporte
     * @param request Mapa con: idReport (Long) - 3 para Scientific Impact Report, 1 para Research Performance Report
     * @return Archivo Excel descargable
     */
    @PostMapping("/generate-report")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, Object> request) {
        System.out.println("========== GENERATE REPORT REQUEST ==========");
        System.out.println("Request received: " + request);
        System.out.println("Request class: " + (request != null ? request.getClass().getName() : "null"));
        
        try {
            Object idReportObj = request.get("idReport");
            System.out.println("idReportObj: " + idReportObj + " (type: " + (idReportObj != null ? idReportObj.getClass().getName() : "null") + ")");
            
            if (idReportObj == null) {
                System.err.println("ERROR: idReport is null in request");
                return ResponseEntity.badRequest().body(createErrorResponse("idReport is required"));
            }
            
            Long idReport;
            if (idReportObj instanceof Number) {
                idReport = ((Number) idReportObj).longValue();
            } else {
                idReport = Long.parseLong(idReportObj.toString());
            }
            
            System.out.println("Parsed idReport: " + idReport);
            System.out.println("Calling reportService.generateReport...");
            
            byte[] excelFile = reportService.generateReport(idReport);
            
            System.out.println("Report generated successfully, size: " + excelFile.length + " bytes");
            
            // Determinar nombre del archivo según el tipo de reporte
            String filename = "report.xlsx";
            if (idReport == 3) {
                filename = "scientific-impact-report.xlsx";
            } else if (idReport == 1) {
                filename = "research-performance-report.xlsx";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);
                    
        } catch (IllegalArgumentException e) {
            System.err.println("========== ILLEGAL ARGUMENT EXCEPTION ==========");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("================================================");
            return ResponseEntity.status(400).body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            System.err.println("========== ILLEGAL STATE EXCEPTION ==========");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==============================================");
            return ResponseEntity.status(400).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("========== GENERAL EXCEPTION ==========");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("======================================");
            return ResponseEntity.status(500).body(createErrorResponse("Error generating report: " + e.getMessage()));
        } finally {
            System.out.println("========== END GENERATE REPORT ==========");
        }
    }

    /**
     * Genera el reporte de Productividad de Investigadores
     * @param projectsFile Archivo Excel con datos de proyectos (multipart/form-data)
     * @return Archivo Excel descargable
     */
    @PostMapping(value = "/generate-investigator-productivity-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateInvestigatorProductivityReport(
            @RequestParam("projectsFile") MultipartFile projectsFile) {
        System.out.println("========== GENERATE INVESTIGATOR PRODUCTIVITY REPORT REQUEST ==========");
        System.out.println("Projects file received: " + (projectsFile != null ? projectsFile.getOriginalFilename() : "null"));
        
        try {
            if (projectsFile == null || projectsFile.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("El archivo de proyectos es requerido"));
            }

            System.out.println("Calling investigatorProductivityService.generateReport...");
            byte[] excelFile = investigatorProductivityService.generateReport(projectsFile);
            
            System.out.println("Report generated successfully, size: " + excelFile.length + " bytes");
            
            String filename = "investigator-productivity-report.xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);
                    
        } catch (IllegalArgumentException e) {
            System.err.println("========== ILLEGAL ARGUMENT EXCEPTION ==========");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("================================================");
            return ResponseEntity.status(400).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("========== GENERAL EXCEPTION ==========");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("======================================");
            return ResponseEntity.status(500).body(createErrorResponse("Error generando reporte: " + e.getMessage()));
        } finally {
            System.out.println("========== END GENERATE INVESTIGATOR PRODUCTIVITY REPORT ==========");
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
