package com.sisgic.controller;

import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import com.sisgic.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/anid-export")
@CrossOrigin(origins = "*")
public class AnidExportController {

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private OrganizacionEventosCientificosRepository organizacionEventosCientificosRepository;

    @Autowired
    private TesisRepository tesisRepository;

    @Autowired
    private BecariosPostdoctoralesRepository becariosPostdoctoralesRepository;

    @Autowired
    private DifusionRepository difusionRepository;

    @Autowired
    private ColaboracionRepository colaboracionRepository;

    @Autowired
    private TransferenciaTecnologicaRepository transferenciaTecnologicaRepository;

    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private VIndexTypeRepository vIndexTypeRepository;

    @Autowired
    private FundingTypeRepository fundingTypeRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private TipoSectorRepository tipoSectorRepository;

    @Autowired
    private VClusterRepository vClusterRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private TipoDifusionRepository tipoDifusionRepository;

    @Autowired
    private PublicoObjetivoRepository publicoObjetivoRepository;

    @Autowired
    private TipoTransferenciaRepository tipoTransferenciaRepository;

    @Autowired
    private CategoriaTransferenciaRepository categoriaTransferenciaRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private UserService userService;

    @Value("${pdfs.path:}")
    private String pdfsPathConfig;

    @GetMapping("/pdfs-zip")
    public void downloadPdfsZip(HttpServletResponse response) {
        try {
            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);

            Path pdfsDirectory = resolvePdfsDirectory();
            if (pdfsDirectory == null) {
                response.sendError(500, "PDF directory is not configured.");
                return;
            }

            List<? extends ProductoCientifico> publicaciones = publicacionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<? extends ProductoCientifico> eventos = organizacionEventosCientificosRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Tesis> tesis = tesisRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<BecariosPostdoctorales> postdoctorales = becariosPostdoctoralesRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Difusion> outreach = difusionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Colaboracion> colaboraciones = colaboracionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<? extends ProductoCientifico> transferencias = transferenciaTecnologicaRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();

            String fileName = "anid-supporting-files.zip";
            response.setContentType("application/zip");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
                // Create all folders even if they end up empty.
                createDirectoryEntry(zipOut, "Publications");
                createDirectoryEntry(zipOut, "Scientific_Events");
                createDirectoryEntry(zipOut, "Thesis_Students");
                createDirectoryEntry(zipOut, "Postdoctoral_Fellows");
                createDirectoryEntry(zipOut, "Outreach_Activities");
                createDirectoryEntry(zipOut, "Scientific_Collaborations");
                createDirectoryEntry(zipOut, "Technology_Transfer");

                addPdfsForSheet(zipOut, "Publications", publicaciones, pdfsDirectory);
                addPdfsForSheet(zipOut, "Scientific_Events", eventos, pdfsDirectory);
                addPdfsForSheet(zipOut, "Thesis_Students", tesis, pdfsDirectory);
                addPdfsForSheet(zipOut, "Postdoctoral_Fellows", postdoctorales, pdfsDirectory);
                addPdfsForSheet(zipOut, "Outreach_Activities", outreach, pdfsDirectory);
                addPdfsForSheet(zipOut, "Scientific_Collaborations", colaboraciones, pdfsDirectory);
                addPdfsForSheet(zipOut, "Technology_Transfer", transferencias, pdfsDirectory);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating ANID PDFs ZIP", e);
        }
    }

    @GetMapping("/excel-workbook")
    @Transactional(readOnly = true)
    public void generateExcelWorkbook(HttpServletResponse response) {
        try {
            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);

            List<Publicacion> publicaciones = publicacionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<OrganizacionEventosCientificos> eventos = organizacionEventosCientificosRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Tesis> tesis = tesisRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<BecariosPostdoctorales> postdoctorales = becariosPostdoctoralesRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Difusion> outreach = difusionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<Colaboracion> colaboraciones = colaboracionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();
            List<? extends ProductoCientifico> transferencias = transferenciaTecnologicaRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged()).getContent();

            Path pdfsDirectory = resolvePdfsDirectory();

            // Split scientific events into two tabs based on participation role IDs.
            // Organizer role: 14 (Organization of Scientific Events)
            // Speaker role: 24 (Participation in Scientific Events)
            List<OrganizacionEventosCientificos> participationEvents = new ArrayList<>();
            List<OrganizacionEventosCientificos> organizationEvents = new ArrayList<>();
            for (OrganizacionEventosCientificos ev : eventos) {
                boolean hasOrganizer = false;
                boolean hasSpeaker = false;
                List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(ev.getId());
                for (ParticipacionProducto pp : parts) {
                    if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) continue;
                    long tipoParticipacionId = pp.getTipoParticipacion().getId();
                    if (tipoParticipacionId == 14L) hasOrganizer = true;
                    if (tipoParticipacionId == 24L) hasSpeaker = true;
                }
                if (hasSpeaker) participationEvents.add(ev);
                if (hasOrganizer) organizationEvents.add(ev);
            }

            List<ParticipationScientificEventGroupedRow> participationGroupedRows =
                buildGroupedParticipationScientificEventRows(participationEvents);

            Workbook workbook = new XSSFWorkbook();
            Sheet summary = workbook.createSheet("Summary");

            int summaryRow = 0;
            Row h0 = summary.createRow(summaryRow++);
            h0.createCell(0).setCellValue("Product Type");
            h0.createCell(1).setCellValue("Total Records");
            h0.createCell(2).setCellValue("PDFs Available");

            // Summary counts
            int pubsPdfCount = countPdfsAvailable(publicaciones, pdfsDirectory);
            int participationEventsPdfCount = countPdfsAvailable(participationEvents, pdfsDirectory);
            int organizationEventsPdfCount = countPdfsAvailable(organizationEvents, pdfsDirectory);
            int tesisPdfCount = countPdfsAvailable(tesis, pdfsDirectory);
            int postPdfCount = countPdfsAvailable(postdoctorales, pdfsDirectory);
            int outreachPdfCount = countPdfsAvailable(outreach, pdfsDirectory);
            int collabPdfCount = countPdfsAvailable(colaboraciones, pdfsDirectory);
            int transferPdfCount = countPdfsAvailable(transferencias, pdfsDirectory);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Publications");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(publicaciones.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(pubsPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Participation in Scientific Events");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(participationGroupedRows.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(participationEventsPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Organization of Scientific Events");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(organizationEvents.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(organizationEventsPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Thesis Students");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(tesis.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(tesisPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Postdoctoral Fellows");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(postdoctorales.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(postPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Outreach Activities");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(outreach.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(outreachPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Scientific Collaborations");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(colaboraciones.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(collabPdfCount);

            summary.createRow(summaryRow++).createCell(0).setCellValue("Technology Transfer");
            summary.getRow(summaryRow - 1).createCell(1).setCellValue(transferencias.size());
            summary.getRow(summaryRow - 1).createCell(2).setCellValue(transferPdfCount);

            // Sheets
            createPublicationsSheet(workbook, "Publications", publicaciones, pdfsDirectory);
            createParticipationScientificEventsSheet(workbook, "Part_Scientific_Events", participationGroupedRows);
            createOrganizationScientificEventsSheet(workbook, "Organization_Scientific_Events", organizationEvents);
            createThesisStudentsSheet(workbook, "Thesis_Students", tesis);
            createPostdoctoralFellowsSheet(workbook, "Postdoctoral_Fellows", postdoctorales);
            createOutreachActivitiesSheet(workbook, "Outreach_Activities", outreach);
            createCollaborationsSheet(workbook, "Scientific_Collaborations", colaboraciones);
            createTechnologyTransferSheet(workbook, "Technology_Transfer", transferencias);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=anid-export-workbook.xlsx");

            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating ANID Excel workbook", e);
        }
    }

    private void createProductSheet(
        Workbook workbook,
        String sheetName,
        List<? extends ProductoCientifico> items,
        Path pdfsDirectory
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("ANID Code");
        header.createCell(2).setCellValue("Name (descripcion)");
        header.createCell(3).setCellValue("Description (comentario)");
        header.createCell(4).setCellValue("Basal");
        header.createCell(5).setCellValue("Progress Report (Period)");
        header.createCell(6).setCellValue("Clusters");
        header.createCell(7).setCellValue("Start Date");
        header.createCell(8).setCellValue("End Date");
        header.createCell(9).setCellValue("PDF Available");

        // Translate descripcion/comentario codes to "us"
        List<String> codes = new ArrayList<>();
        for (ProductoCientifico p : items) {
            if (p.getDescripcion() != null) codes.add(p.getDescripcion());
            if (p.getComentario() != null) codes.add(p.getComentario());
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codes, 2, "us");

        for (ProductoCientifico p : items) {
            Row row = sheet.createRow(rowIdx++);

            boolean pdfAvailable = isPdfAvailable(p.getId(), p.getLinkPDF(), pdfsDirectory);
            String basal = p.getBasal() != null && (p.getBasal().equals('S') || p.getBasal().equals('s') || p.getBasal().equals('1'))
                ? "Si"
                : "No";

            row.createCell(0).setCellValue(p.getId() != null ? p.getId().toString() : "");
            row.createCell(1).setCellValue(p.getCodigoANID() != null ? p.getCodigoANID() : "");
            row.createCell(2).setCellValue(resolveText(textosMap, p.getDescripcion()));
            row.createCell(3).setCellValue(resolveText(textosMap, p.getComentario()));
            row.createCell(4).setCellValue(basal);
            row.createCell(5).setCellValue(p.getProgressReport() != null ? p.getProgressReport() : "");
            row.createCell(6).setCellValue(convertClustersToRoman(p.getCluster()));
            row.createCell(7).setCellValue(p.getFechaInicio() != null ? p.getFechaInicio().toString() : "");
            row.createCell(8).setCellValue(p.getFechaTermino() != null ? p.getFechaTermino().toString() : "");
            row.createCell(9).setCellValue(pdfAvailable ? "Yes" : "No");
        }
    }

    private void createPublicationsSheet(
        Workbook workbook,
        String sheetName,
        List<Publicacion> publicaciones,
        Path pdfsDirectory
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);

        header.createCell(0).setCellValue("ID (id interno)");
        header.createCell(1).setCellValue("DOI");
        header.createCell(2).setCellValue("Authors");
        header.createCell(3).setCellValue("Article Title");
        header.createCell(4).setCellValue("Journal Name");
        header.createCell(5).setCellValue("ISSN");
        header.createCell(6).setCellValue("Volume");
        header.createCell(7).setCellValue("Year Published");
        header.createCell(8).setCellValue("First page");
        header.createCell(9).setCellValue("Last page");
        header.createCell(10).setCellValue("Indexed/Not Indexed");
        header.createCell(11).setCellValue("Funding");
        header.createCell(12).setCellValue("Name of Research Line");
        header.createCell(13).setCellValue("Progress report");
        header.createCell(14).setCellValue("Participants of the Center that collaborate in the Article");
        header.createCell(15).setCellValue("Main Researchers");
        header.createCell(16).setCellValue("Associated Researchers");
        header.createCell(17).setCellValue("Postdoctoral Fellows");
        header.createCell(18).setCellValue("Thesis Students");
        header.createCell(19).setCellValue("Other External Researchers");

        // Catalog maps for index types & funding types.
        Map<Long, String> indexDescriptions = new HashMap<>();
        for (VIndexType t : vIndexTypeRepository.findAllByOrderByIdAsc()) {
            if (t.getId() != null) indexDescriptions.put(t.getId(), t.getDescripcion());
        }

        Map<Long, String> fundingDescriptions = new HashMap<>();
        for (FundingType ft : fundingTypeRepository.findAll()) {
            if (ft.getId() != null) fundingDescriptions.put(ft.getId(), ft.getIdDescripcion());
        }

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) clusterDescriptions.put(c.getId(), c.getDescripcion());
        }

        // Translate article title codes and journal name codes to "us".
        Set<String> codesSet = new HashSet<>();
        for (Publicacion p : publicaciones) {
            if (p.getDescripcion() != null) codesSet.add(p.getDescripcion());
            if (p.getJournal() != null && p.getJournal().getIdDescripcion() != null) {
                codesSet.add(p.getJournal().getIdDescripcion());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(codesSet), 2, "us");

        for (Publicacion p : publicaciones) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(p.getId() != null ? p.getId().toString() : "");
            row.createCell(1).setCellValue(p.getDoi() != null ? p.getDoi() : "");

            // Fetch participants for author formatting and S/N flags.
            List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(p.getId());

            List<String> authors = new ArrayList<>();
            Set<Long> uniqueAuthorRrhhIds = new HashSet<>();

            Set<Long> centerCollaboratorRrhhIds = new HashSet<>();

            boolean mainResearchers = false; // tipoParticipacion.id = 1 among center collaborators
            boolean associatedResearchers = false; // tipoRRHH.id = 4
            boolean postdoctoralFellows = false; // tipoRRHH.id = 7
            boolean thesisStudents = false; // tipoRRHH.id in (7,8,9)
            boolean otherExternalResearchers = false; // tipoRRHH.id in (32,60)

            for (ParticipacionProducto pp : parts) {
                if (pp == null || pp.getRrhh() == null) continue;

                RRHH rrhh = pp.getRrhh();
                Long rrhhId = rrhh.getId();
                Long tipoRRHHId = rrhh.getTipoRRHH() != null ? rrhh.getTipoRRHH().getId() : null;

                boolean isCenter = tipoRRHHId == null || (tipoRRHHId != 32L && tipoRRHHId != 60L);
                if (isCenter && rrhhId != null) {
                    centerCollaboratorRrhhIds.add(rrhhId);
                }

                if (tipoRRHHId != null) {
                    if (tipoRRHHId == 4L) associatedResearchers = true;
                    if (tipoRRHHId == 7L) postdoctoralFellows = true;
                    if (tipoRRHHId == 7L || tipoRRHHId == 8L || tipoRRHHId == 9L) thesisStudents = true;
                    if (tipoRRHHId == 32L || tipoRRHHId == 60L) otherExternalResearchers = true;
                }

                if (isCenter && pp.getTipoParticipacion() != null && pp.getTipoParticipacion().getId() != null) {
                    if (pp.getTipoParticipacion().getId() == 1L) {
                        mainResearchers = true;
                    }
                }

                // Authors list (unique by rrhhId)
                if (rrhhId != null && !uniqueAuthorRrhhIds.contains(rrhhId)) {
                    authors.add(formatAuthor(rrhh));
                    uniqueAuthorRrhhIds.add(rrhhId);
                }
            }

            String indexedLabel = resolveIndexesLabel(p.getIndexs(), indexDescriptions);
            String fundingLabel = resolveFundingLabel(p.getFunding(), fundingDescriptions);
            String researchLine = resolveClusterDescriptions(p.getCluster(), clusterDescriptions);

            row.createCell(2).setCellValue(String.join("; ", authors));
            row.createCell(3).setCellValue(resolveText(textosMap, p.getDescripcion()));
            row.createCell(4).setCellValue(p.getJournal() != null ? resolveText(textosMap, p.getJournal().getIdDescripcion()) : "");
            row.createCell(5).setCellValue(p.getJournal() != null && p.getJournal().getIssn() != null ? p.getJournal().getIssn() : "");
            row.createCell(6).setCellValue(p.getVolume() != null ? p.getVolume() : "");
            row.createCell(7).setCellValue(p.getYearPublished() != null ? p.getYearPublished() : 0);
            row.createCell(8).setCellValue(p.getFirstpage() != null ? p.getFirstpage() : "");
            row.createCell(9).setCellValue(p.getLastpage() != null ? p.getLastpage() : "");
            row.createCell(10).setCellValue(indexedLabel);
            row.createCell(11).setCellValue(fundingLabel);
            row.createCell(12).setCellValue(researchLine);
            row.createCell(13).setCellValue(p.getProgressReport() != null ? p.getProgressReport() : "");
            row.createCell(14).setCellValue(centerCollaboratorRrhhIds.size());
            row.createCell(15).setCellValue(mainResearchers ? "S" : "N");
            row.createCell(16).setCellValue(associatedResearchers ? "S" : "N");
            row.createCell(17).setCellValue(postdoctoralFellows ? "S" : "N");
            row.createCell(18).setCellValue(thesisStudents ? "S" : "N");
            row.createCell(19).setCellValue(otherExternalResearchers ? "S" : "N");
        }
    }

    private String formatAuthor(RRHH rrhh) {
        if (rrhh == null) return "";
        String primerApellido = rrhh.getPrimerApellido() != null ? rrhh.getPrimerApellido().trim() : "";
        String primerNombre = rrhh.getPrimerNombre() != null ? rrhh.getPrimerNombre().trim() : "";
        String segundoNombre = rrhh.getSegundoNombre() != null ? rrhh.getSegundoNombre().trim() : "";

        String secondInitial = segundoNombre.isEmpty() ? "" : segundoNombre.substring(0, 1);
        String nombreFormatted = primerNombre + (secondInitial.isEmpty() ? "" : (" " + secondInitial));

        if (!primerApellido.isEmpty() && !nombreFormatted.isEmpty()) {
            return primerApellido + ", " + nombreFormatted;
        }
        return primerApellido.isEmpty() ? nombreFormatted : primerApellido;
    }

    private String resolveIndexesLabel(String rawIndexs, Map<Long, String> indexDescriptions) {
        if (rawIndexs == null || rawIndexs.trim().isEmpty()) return "";

        String trimmed = rawIndexs.trim();
        final long OTHER_INDEX_ID = 10L;

        try {
            if (trimmed.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> arr = mapper.readValue(trimmed, List.class);

                List<String> labels = new ArrayList<>();
                for (Object obj : arr) {
                    if (obj == null) continue;

                    Long id = null;
                    String text = null;
                    if (obj instanceof Number) {
                        id = ((Number) obj).longValue();
                    } else if (obj instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) obj;
                        Object idObj = m.get("id");
                        Object textObj = m.get("text");
                        if (idObj instanceof Number) id = ((Number) idObj).longValue();
                        if (textObj != null) text = textObj.toString();
                    }

                    if (id == null) continue;

                    if (id == OTHER_INDEX_ID && text != null && !text.trim().isEmpty()) {
                        labels.add(text.trim());
                    } else {
                        labels.add(indexDescriptions.getOrDefault(id, String.valueOf(id)));
                    }
                }
                return String.join(", ", labels);
            }
        } catch (Exception ignored) {
            // fallback below
        }

        // Backward compatibility fallback: accept "1,2,3"
        try {
            String[] parts = trimmed.split(",");
            List<String> labels = new ArrayList<>();
            for (String part : parts) {
                String s = part.trim();
                if (s.isEmpty()) continue;
                labels.add(indexDescriptions.getOrDefault(Long.parseLong(s), s));
            }
            return String.join(", ", labels);
        } catch (Exception e) {
            return trimmed;
        }
    }

    private String resolveFundingLabel(String rawFunding, Map<Long, String> fundingDescriptions) {
        if (rawFunding == null || rawFunding.trim().isEmpty()) return "";

        String trimmed = rawFunding.trim();
        try {
            if (trimmed.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> arr = mapper.readValue(trimmed, List.class);

                List<String> labels = new ArrayList<>();
                for (Object obj : arr) {
                    if (obj == null) continue;

                    Long id = null;
                    String text = null;
                    if (obj instanceof Number) {
                        id = ((Number) obj).longValue();
                    } else if (obj instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) obj;
                        Object idObj = m.get("id");
                        Object textObj = m.get("text");
                        if (idObj instanceof Number) id = ((Number) idObj).longValue();
                        if (textObj != null) text = textObj.toString();
                    }

                    if (id == null) continue;
                    String base = fundingDescriptions.getOrDefault(id, String.valueOf(id));

                    if (text != null && !text.trim().isEmpty()) {
                        labels.add(base + " (" + text.trim() + ")");
                    } else {
                        labels.add(base);
                    }
                }

                return String.join(", ", labels);
            }
        } catch (Exception ignored) {
            // fallback below
        }

        // Backward compatibility fallback: accept "1,2,3"
        try {
            String[] parts = trimmed.split(",");
            List<String> labels = new ArrayList<>();
            for (String part : parts) {
                String s = part.trim();
                if (s.isEmpty()) continue;
                labels.add(fundingDescriptions.getOrDefault(Long.parseLong(s), s));
            }
            return String.join(", ", labels);
        } catch (Exception e) {
            return trimmed;
        }
    }

    private void createCollaborationsSheet(
        Workbook workbook,
        String sheetName,
        List<Colaboracion> colaboraciones
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Activity Type");
        header.createCell(2).setCellValue("Activity Name");
        header.createCell(3).setCellValue("Name of People Involved");
        header.createCell(4).setCellValue("Institution with which the Center collaborates");
        header.createCell(5).setCellValue("Country of Origin");
        header.createCell(6).setCellValue("City of Origin");
        header.createCell(7).setCellValue("Country of Destination");
        header.createCell(8).setCellValue("City of Destination");
        header.createCell(9).setCellValue("Beginning Date");
        header.createCell(10).setCellValue("Ending Date");
        header.createCell(11).setCellValue("Name of Research Line");
        header.createCell(12).setCellValue("Progress report");

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) clusterDescriptions.put(c.getId(), c.getDescripcion());
        }

        Map<String, String> countryCodeToDescCode = new HashMap<>();
        for (Pais p : paisRepository.findAllByOrderByIdDescripcionAsc()) {
            if (p.getCodigo() != null) {
                countryCodeToDescCode.put(p.getCodigo(), p.getIdDescripcion());
            }
        }

        Set<String> textCodes = new HashSet<>();
        for (Colaboracion c : colaboraciones) {
            if (c.getDescripcion() != null) textCodes.add(c.getDescripcion());
            if (c.getTipoColaboracion() != null && c.getTipoColaboracion().getIdDescripcion() != null) {
                textCodes.add(c.getTipoColaboracion().getIdDescripcion());
            }
            if (c.getInstitucion() != null && c.getInstitucion().getIdDescripcion() != null) {
                textCodes.add(c.getInstitucion().getIdDescripcion());
            }
            if (c.getPaisOrigen() != null && c.getPaisOrigen().getIdDescripcion() != null) {
                textCodes.add(c.getPaisOrigen().getIdDescripcion());
            }
            if (c.getCodigoPaisDestino() != null) {
                String destinoCode = countryCodeToDescCode.get(c.getCodigoPaisDestino());
                if (destinoCode != null) textCodes.add(destinoCode);
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (Colaboracion c : colaboraciones) {
            // Ensure lazy relations are initialized for export context.
            Colaboracion full = c.getId() != null
                ? colaboracionRepository.findByIdWithRelations(c.getId()).orElse(c)
                : c;

            Row row = sheet.createRow(rowIdx++);

            List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(full.getId());
            List<String> fullNames = new ArrayList<>();
            Set<Long> seenRrhh = new HashSet<>();
            for (ParticipacionProducto pp : participaciones) {
                if (pp == null || pp.getRrhh() == null) continue;
                RRHH rrhh = pp.getRrhh();
                if (rrhh.getId() != null && !seenRrhh.add(rrhh.getId())) continue;
                if (rrhh.getFullname() != null && !rrhh.getFullname().trim().isEmpty()) {
                    fullNames.add(rrhh.getFullname().trim());
                }
            }

            String institution = "";
            if (full.getInstitucion() != null) {
                if (full.getInstitucion().getDescripcion() != null && !full.getInstitucion().getDescripcion().trim().isEmpty()) {
                    institution = full.getInstitucion().getDescripcion().trim();
                } else {
                    institution = resolveText(textosMap, full.getInstitucion().getIdDescripcion());
                }
            }

            String countryOrigin = full.getPaisOrigen() != null
                ? resolveText(textosMap, full.getPaisOrigen().getIdDescripcion())
                : "";

            String countryDestination = "";
            if (full.getCodigoPaisDestino() != null) {
                String destinoCode = countryCodeToDescCode.get(full.getCodigoPaisDestino());
                countryDestination = resolveText(textosMap, destinoCode);
            }

            row.createCell(0).setCellValue(full.getId() != null ? full.getId().toString() : "");
            row.createCell(1).setCellValue(full.getTipoColaboracion() != null
                ? resolveText(textosMap, full.getTipoColaboracion().getIdDescripcion())
                : "");
            row.createCell(2).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(3).setCellValue(String.join("; ", fullNames));
            row.createCell(4).setCellValue(institution);
            row.createCell(5).setCellValue(countryOrigin);
            row.createCell(6).setCellValue(full.getCiudadOrigen() != null ? full.getCiudadOrigen() : "");
            row.createCell(7).setCellValue(countryDestination);
            row.createCell(8).setCellValue(full.getCiudadDestino() != null ? full.getCiudadDestino() : "");
            row.createCell(9).setCellValue(formatDateDDMMYYYY(full.getFechaInicio()));
            row.createCell(10).setCellValue(formatDateDDMMYYYY(full.getFechaTermino()));
            row.createCell(11).setCellValue(resolveClusterDescriptions(full.getCluster(), clusterDescriptions));
            row.createCell(12).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    private String formatDateDDMMYYYY(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * One export row per logical event: same tipoEvento, descripción code, país, ciudad, fechaInicio.
     * Speakers (participación 24) from all merged products are de-duplicated by RRHH id and split by RRHH type.
     */
    private List<ParticipationScientificEventGroupedRow> buildGroupedParticipationScientificEventRows(
        List<OrganizacionEventosCientificos> participationEvents
    ) {
        Map<ParticipationEventGroupKey, ParticipationScientificEventGroupedRow> map = new LinkedHashMap<>();

        for (OrganizacionEventosCientificos ev : participationEvents) {
            if (ev.getId() == null) {
                continue;
            }
            OrganizacionEventosCientificos full = organizacionEventosCientificosRepository
                .findByIdWithRelations(ev.getId()).orElse(ev);

            Long tipoEventoId = full.getTipoEvento() != null ? full.getTipoEvento().getId() : null;
            String descripcionCode = full.getDescripcion() != null ? full.getDescripcion() : "";
            String paisCodigo = full.getPais() != null ? full.getPais().getCodigo() : "";
            String ciudad = full.getCiudad() != null ? full.getCiudad() : "";
            LocalDate fechaInicio = full.getFechaInicio();

            ParticipationEventGroupKey key = new ParticipationEventGroupKey(
                tipoEventoId, descripcionCode, paisCodigo, ciudad, fechaInicio
            );

            ParticipationScientificEventGroupedRow agg = map.computeIfAbsent(
                key, k -> new ParticipationScientificEventGroupedRow()
            );
            if (agg.representative == null) {
                agg.representative = full;
            }
            if (!agg.productIds.contains(full.getId())) {
                agg.productIds.add(full.getId());
            }
            mergeSpeakerParticipationsForExport(agg, full.getId());
        }

        return new ArrayList<>(map.values());
    }

    private void mergeSpeakerParticipationsForExport(ParticipationScientificEventGroupedRow row, Long productId) {
        List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(productId);
        for (ParticipacionProducto pp : parts) {
            if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) {
                continue;
            }
            if (pp.getTipoParticipacion().getId() != 24L) {
                continue;
            }
            RRHH rrhh = pp.getRrhh();
            if (rrhh == null) {
                continue;
            }
            String name = rrhh.getFullname() != null ? rrhh.getFullname().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            Long rrhhId = rrhh.getId();
            if (rrhhId == null) {
                continue;
            }

            Long tipoRrhhId = null;
            if (rrhh.getTipoRRHH() != null && rrhh.getTipoRRHH().getId() != null) {
                tipoRrhhId = rrhh.getTipoRRHH().getId();
            }
            boolean isExternalOther = tipoRrhhId != null && (tipoRrhhId == 32L || tipoRrhhId == 60L);
            if (isExternalOther) {
                row.otherSpeakerNames.putIfAbsent(rrhhId, name);
            } else {
                row.centerSpeakerNames.putIfAbsent(rrhhId, name);
            }
        }
    }

    private void createParticipationScientificEventsSheet(
        Workbook workbook,
        String sheetName,
        List<ParticipationScientificEventGroupedRow> groupedRows
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID (id interno)");
        header.createCell(1).setCellValue("Type of Event");
        header.createCell(2).setCellValue("Event Name");
        header.createCell(3).setCellValue("Country");
        header.createCell(4).setCellValue("City");
        header.createCell(5).setCellValue("Title");
        header.createCell(6).setCellValue("Organizer");
        header.createCell(7).setCellValue("Name of Person Involved (Part of the Center)");
        header.createCell(8).setCellValue("Other Participants (Not part of the Center)");
        header.createCell(9).setCellValue("Start Date");
        header.createCell(10).setCellValue("Ending Date");
        header.createCell(11).setCellValue("Name of Research Line");
        header.createCell(12).setCellValue("Progress report");

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) {
                clusterDescriptions.put(c.getId(), c.getDescripcion());
            }
        }

        Set<String> textCodes = new HashSet<>();
        for (ParticipationScientificEventGroupedRow grp : groupedRows) {
            OrganizacionEventosCientificos rep = grp.representative;
            if (rep == null) {
                continue;
            }
            if (rep.getDescripcion() != null) {
                textCodes.add(rep.getDescripcion());
            }
            if (rep.getComentario() != null) {
                textCodes.add(rep.getComentario());
            }
            if (rep.getTipoEvento() != null && rep.getTipoEvento().getIdDescripcion() != null) {
                textCodes.add(rep.getTipoEvento().getIdDescripcion());
            }
            if (rep.getPais() != null && rep.getPais().getIdDescripcion() != null) {
                textCodes.add(rep.getPais().getIdDescripcion());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (ParticipationScientificEventGroupedRow grp : groupedRows) {
            OrganizacionEventosCientificos full = grp.representative;
            if (full == null) {
                continue;
            }

            List<String> idParts = new ArrayList<>();
            for (Long pid : grp.productIds) {
                if (pid != null) {
                    idParts.add(pid.toString());
                }
            }
            String idsCell = String.join("; ", idParts);

            String typeOfEvent = "";
            if (full.getTipoEvento() != null) {
                if (full.getTipoEvento().getDescripcion() != null
                    && !full.getTipoEvento().getDescripcion().trim().isEmpty()) {
                    typeOfEvent = full.getTipoEvento().getDescripcion().trim();
                } else {
                    typeOfEvent = resolveText(textosMap, full.getTipoEvento().getIdDescripcion());
                }
            }

            String country = full.getPais() != null
                ? resolveText(textosMap, full.getPais().getIdDescripcion())
                : "";

            String centerSpeakers = String.join("; ", grp.centerSpeakerNames.values());
            String otherSpeakers = String.join("; ", grp.otherSpeakerNames.values());
            String organizers = resolveOrganizerNamesForEventGroup(grp.productIds);

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(idsCell);
            row.createCell(1).setCellValue(typeOfEvent);
            row.createCell(2).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(3).setCellValue(country);
            row.createCell(4).setCellValue(full.getCiudad() != null ? full.getCiudad() : "");
            row.createCell(5).setCellValue(resolveText(textosMap, full.getComentario()));
            row.createCell(6).setCellValue(organizers);
            row.createCell(7).setCellValue(centerSpeakers);
            row.createCell(8).setCellValue(otherSpeakers);
            row.createCell(9).setCellValue(formatDateDDMMYYYY(full.getFechaInicio()));
            row.createCell(10).setCellValue(formatDateDDMMYYYY(full.getFechaTermino()));
            row.createCell(11).setCellValue(resolveClusterDescriptions(full.getCluster(), clusterDescriptions));
            row.createCell(12).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    private String resolveOrganizerNamesForEventGroup(List<Long> productIds) {
        LinkedHashMap<Long, String> organizersById = new LinkedHashMap<>();
        for (Long productId : productIds) {
            if (productId == null) continue;
            List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(productId);
            for (ParticipacionProducto pp : parts) {
                if (pp == null || pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) continue;
                if (pp.getTipoParticipacion().getId() != 14L) continue;
                RRHH rrhh = pp.getRrhh();
                if (rrhh == null || rrhh.getId() == null) continue;
                String name = rrhh.getFullname() != null ? rrhh.getFullname().trim() : "";
                if (name.isEmpty()) continue;
                organizersById.putIfAbsent(rrhh.getId(), name);
            }
        }
        return String.join("; ", organizersById.values());
    }

    /**
     * Scientific events where at least one participant has tipo participación = 14 (organizer).
     * Columns per ANID Organization of Scientific Events specification.
     */
    private void createOrganizationScientificEventsSheet(
        Workbook workbook,
        String sheetName,
        List<OrganizacionEventosCientificos> organizationEvents
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID (id interno)");
        header.createCell(1).setCellValue("Type of Event");
        header.createCell(2).setCellValue("Event Name");
        header.createCell(3).setCellValue("Country");
        header.createCell(4).setCellValue("City");
        header.createCell(5).setCellValue("Start Date");
        header.createCell(6).setCellValue("Ending Date");
        header.createCell(7).setCellValue("Number of Participants");
        header.createCell(8).setCellValue("Name of Research Line");
        header.createCell(9).setCellValue("Progress report");

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) {
                clusterDescriptions.put(c.getId(), c.getDescripcion());
            }
        }

        Set<String> textCodes = new HashSet<>();
        List<OrganizacionEventosCientificos> resolved = new ArrayList<>();
        for (OrganizacionEventosCientificos ev : organizationEvents) {
            OrganizacionEventosCientificos full = ev.getId() != null
                ? organizacionEventosCientificosRepository.findByIdWithRelations(ev.getId()).orElse(ev)
                : ev;
            resolved.add(full);
            if (full.getDescripcion() != null) {
                textCodes.add(full.getDescripcion());
            }
            if (full.getTipoEvento() != null && full.getTipoEvento().getIdDescripcion() != null) {
                textCodes.add(full.getTipoEvento().getIdDescripcion());
            }
            if (full.getPais() != null && full.getPais().getIdDescripcion() != null) {
                textCodes.add(full.getPais().getIdDescripcion());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (OrganizacionEventosCientificos full : resolved) {
            Row row = sheet.createRow(rowIdx++);

            String typeOfEvent = "";
            if (full.getTipoEvento() != null) {
                if (full.getTipoEvento().getDescripcion() != null
                    && !full.getTipoEvento().getDescripcion().trim().isEmpty()) {
                    typeOfEvent = full.getTipoEvento().getDescripcion().trim();
                } else {
                    typeOfEvent = resolveText(textosMap, full.getTipoEvento().getIdDescripcion());
                }
            }

            String country = full.getPais() != null
                ? resolveText(textosMap, full.getPais().getIdDescripcion())
                : "";

            row.createCell(0).setCellValue(full.getId() != null ? full.getId().toString() : "");
            row.createCell(1).setCellValue(typeOfEvent);
            row.createCell(2).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(3).setCellValue(country);
            row.createCell(4).setCellValue(full.getCiudad() != null ? full.getCiudad() : "");
            row.createCell(5).setCellValue(formatDateDDMMYYYY(full.getFechaInicio()));
            row.createCell(6).setCellValue(formatDateDDMMYYYY(full.getFechaTermino()));
            if (full.getNumParticipantes() != null) {
                row.createCell(7).setCellValue(full.getNumParticipantes());
            } else {
                row.createCell(7).setCellValue("");
            }
            row.createCell(8).setCellValue(resolveClusterDescriptions(full.getCluster(), clusterDescriptions));
            row.createCell(9).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    /**
     * ANID workbook — Thesis Students tab (columns per institutional export spec).
     */
    private void createThesisStudentsSheet(Workbook workbook, String sheetName, List<Tesis> tesisList) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        String[] headers = new String[] {
            "Id",
            "Student Name",
            "RUN",
            "Gender",
            "Thesis Status",
            "Thesis Title",
            "Academic Degree",
            "Full name of the Degree",
            "Tutor and/or Co-Tutor's Name (Part of the Center)",
            "Tutor and/or Co-Tutor's Name (Not Part of the Center)",
            "Institution(s) that gives the degree",
            "Other Institution(s) (If not found above)",
            "Resources provided by the Center",
            "Year in which student starts program",
            "Year in which thesis starts",
            "Year in which thesis ends",
            "Name of Research Line",
            "Progress report"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) {
                clusterDescriptions.put(c.getId(), c.getDescripcion());
            }
        }

        Set<String> textCodes = new HashSet<>();
        for (Tesis t : tesisList) {
            Tesis full = t.getId() != null
                ? tesisRepository.findByIdWithRelations(t.getId()).orElse(t)
                : t;
            if (full.getDescripcion() != null) {
                textCodes.add(full.getDescripcion());
            }
            if (full.getEstadoTesis() != null && full.getEstadoTesis().getIdDescripcion() != null) {
                textCodes.add(full.getEstadoTesis().getIdDescripcion());
            }
            if (full.getGradoAcademico() != null && full.getGradoAcademico().getIdDescripcion() != null) {
                textCodes.add(full.getGradoAcademico().getIdDescripcion());
            }
            if (full.getInstitucionOG() != null && full.getInstitucionOG().getIdDescripcion() != null) {
                textCodes.add(full.getInstitucionOG().getIdDescripcion());
            }
            if (full.getInstitucion() != null && full.getInstitucion().getIdDescripcion() != null) {
                textCodes.add(full.getInstitucion().getIdDescripcion());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (Tesis t : tesisList) {
            if (t.getId() == null) {
                continue;
            }
            Tesis full = tesisRepository.findByIdWithRelations(t.getId()).orElse(t);

            String[] studentFields = buildThesisStudentPersonFields(full.getId());
            String tutorsCenter = formatThesisTutorNames(full.getId(), true);
            String tutorsOther = formatThesisTutorNames(full.getId(), false);

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(full.getId().toString());
            row.createCell(1).setCellValue(studentFields[0]);
            row.createCell(2).setCellValue(studentFields[1]);
            row.createCell(3).setCellValue(studentFields[2]);
            row.createCell(4).setCellValue(resolveCatalogEntityLabel(full.getEstadoTesis(), textosMap));
            row.createCell(5).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(6).setCellValue(resolveCatalogEntityLabel(full.getGradoAcademico(), textosMap));
            row.createCell(7).setCellValue(
                full.getNombreCompletoTitulo() != null ? full.getNombreCompletoTitulo() : ""
            );
            row.createCell(8).setCellValue(tutorsCenter);
            row.createCell(9).setCellValue(tutorsOther);
            row.createCell(10).setCellValue(resolveInstitucionLabel(full.getInstitucionOG(), textosMap));
            row.createCell(11).setCellValue(resolveInstitucionLabel(full.getInstitucion(), textosMap));
            row.createCell(12).setCellValue("");
            row.createCell(13).setCellValue(formatYearYYYY(full.getFechaInicioPrograma()));
            row.createCell(14).setCellValue(formatYearYYYY(full.getFechaInicio()));
            row.createCell(15).setCellValue(formatYearYYYY(full.getFechaTermino()));
            row.createCell(16).setCellValue(resolveClusterDescriptions(full.getCluster(), clusterDescriptions));
            row.createCell(17).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    private String[] buildThesisStudentPersonFields(Long productId) {
        List<String> names = new ArrayList<>();
        List<String> runs = new ArrayList<>();
        List<String> genders = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();

        List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(productId);
        for (ParticipacionProducto pp : parts) {
            if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) {
                continue;
            }
            if (pp.getTipoParticipacion().getId() != 7L) {
                continue;
            }
            RRHH r = pp.getRrhh();
            if (r == null) {
                continue;
            }
            String dedup = r.getId() != null
                ? "id:" + r.getId()
                : "k:"
                    + (r.getFullname() != null ? r.getFullname() : "")
                    + "|"
                    + (r.getIdRecurso() != null ? r.getIdRecurso() : "");
            if (!seenKeys.add(dedup)) {
                continue;
            }
            if (r.getFullname() != null && !r.getFullname().trim().isEmpty()) {
                names.add(r.getFullname().trim());
            } else {
                names.add("");
            }
            runs.add(r.getIdRecurso() != null ? r.getIdRecurso().trim() : "");
            genders.add(r.getCodigoGenero() != null ? r.getCodigoGenero().trim() : "");
        }
        return new String[] {
            String.join("; ", names),
            String.join("; ", runs),
            String.join("; ", genders)
        };
    }

    /**
     * Tutors: participation types 12 (supervisor) and 13 (co-supervisor).
     * @param center true = RRHH type not 32/60 (or null type); false = only 32 or 60.
     */
    private String formatThesisTutorNames(Long productId, boolean center) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        List<ParticipacionProducto> parts = participacionProductoRepository.findByProductoId(productId);
        for (ParticipacionProducto pp : parts) {
            if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) {
                continue;
            }
            long tp = pp.getTipoParticipacion().getId();
            if (tp != 12L && tp != 13L) {
                continue;
            }
            RRHH rrhh = pp.getRrhh();
            if (rrhh == null) {
                continue;
            }
            Long tipoRrhhId = null;
            if (rrhh.getTipoRRHH() != null && rrhh.getTipoRRHH().getId() != null) {
                tipoRrhhId = rrhh.getTipoRRHH().getId();
            }
            boolean external = tipoRrhhId != null && (tipoRrhhId == 32L || tipoRrhhId == 60L);
            if (center && external) {
                continue;
            }
            if (!center && !external) {
                continue;
            }
            String name = rrhh.getFullname() != null ? rrhh.getFullname().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            String suffix = tp == 12L ? " (Thesis Supervisor)" : " (Co-supervisor)";
            Long rid = rrhh.getId();
            String key = (rid != null ? rid.toString() : name) + "_" + tp;
            ordered.putIfAbsent(key, name + suffix);
        }
        return String.join("; ", ordered.values());
    }

    private String formatYearYYYY(LocalDate d) {
        return d == null ? "" : Integer.toString(d.getYear());
    }

    private String resolveInstitucionLabel(Institucion inst, Map<String, String> textosMap) {
        if (inst == null) {
            return "";
        }
        if (inst.getDescripcion() != null && !inst.getDescripcion().trim().isEmpty()) {
            return inst.getDescripcion().trim();
        }
        return resolveText(textosMap, inst.getIdDescripcion());
    }

    /** Estado tesis / grado académico: descripcion column or i18n from idDescripcion. */
    private String resolveCatalogEntityLabel(Object entity, Map<String, String> textosMap) {
        if (entity == null) {
            return "";
        }
        if (entity instanceof EstadoTesis e) {
            if (e.getDescripcion() != null && !e.getDescripcion().trim().isEmpty()) {
                return e.getDescripcion().trim();
            }
            return resolveText(textosMap, e.getIdDescripcion());
        }
        if (entity instanceof GradoAcademico g) {
            if (g.getDescripcion() != null && !g.getDescripcion().trim().isEmpty()) {
                return g.getDescripcion().trim();
            }
            return resolveText(textosMap, g.getIdDescripcion());
        }
        return "";
    }

    /**
     * ANID workbook — Postdoctoral Fellows tab.
     */
    private void createPostdoctoralFellowsSheet(
        Workbook workbook,
        String sheetName,
        List<BecariosPostdoctorales> fellows
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        String[] headers = new String[] {
            "Id",
            "Name of Postdoctoral Fellow",
            "RUN",
            "Gender",
            "Research Topic",
            "Supervisor's Name (Part of the Center)",
            "Tutor and/or Co-Tutor's Name (Not Part of the Center)",
            "Resources provided by the Center",
            "Funding Source",
            "Start Year",
            "Ending Year",
            "Postdoctoral Inserted",
            "Indicate the name of the institution where it was inserted",
            "Name of Research Line",
            "Progress report"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        Map<Long, String> fundingCodeById = new HashMap<>();
        for (FundingType ft : fundingTypeRepository.findAll()) {
            if (ft.getId() != null && ft.getIdDescripcion() != null) {
                fundingCodeById.put(ft.getId(), ft.getIdDescripcion());
            }
        }
        Map<Long, String> resourceCodeById = new HashMap<>();
        for (Resource res : resourceRepository.findAll()) {
            if (res.getId() != null && res.getIdDescripcion() != null) {
                resourceCodeById.put(res.getId(), res.getIdDescripcion());
            }
        }

        Set<String> textCodes = new HashSet<>();
        textCodes.addAll(fundingCodeById.values());
        textCodes.addAll(resourceCodeById.values());
        for (TipoSector ts : tipoSectorRepository.findAllByOrderByIdAsc()) {
            if (ts.getIdDescripcion() != null) {
                textCodes.add(ts.getIdDescripcion());
            }
        }

        for (BecariosPostdoctorales f : fellows) {
            BecariosPostdoctorales full = f.getId() != null
                ? becariosPostdoctoralesRepository.findByIdWithRelations(f.getId()).orElse(f)
                : f;
            if (full.getDescripcion() != null) {
                textCodes.add(full.getDescripcion());
            }
            if (full.getInstitucion() != null && full.getInstitucion().getIdDescripcion() != null) {
                textCodes.add(full.getInstitucion().getIdDescripcion());
            }
        }

        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        Map<Long, String> fundingLabelById = new HashMap<>();
        for (Map.Entry<Long, String> e : fundingCodeById.entrySet()) {
            fundingLabelById.put(e.getKey(), resolveText(textosMap, e.getValue()));
        }
        Map<Long, String> resourceLabelById = new HashMap<>();
        for (Map.Entry<Long, String> e : resourceCodeById.entrySet()) {
            resourceLabelById.put(e.getKey(), resolveText(textosMap, e.getValue()));
        }

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster vc : vClusterRepository.findAllByOrderByIdAsc()) {
            if (vc.getId() != null) {
                clusterDescriptions.put(vc.getId(), vc.getDescripcion());
            }
        }

        for (BecariosPostdoctorales f : fellows) {
            if (f.getId() == null) {
                continue;
            }
            BecariosPostdoctorales full = becariosPostdoctoralesRepository.findByIdWithRelations(f.getId()).orElse(f);

            String[] person = buildPostdoctoralFellowPersonFields(full.getId());
            String supervisorsCenter = formatPostdoctoralSupervisorNames(full.getId(), true);
            String supervisorsOther = formatPostdoctoralSupervisorNames(full.getId(), false);

            String tipoSectorLabel = "";
            if (full.getTipoSector() != null && full.getTipoSector().getIdDescripcion() != null) {
                tipoSectorLabel = resolveText(textosMap, full.getTipoSector().getIdDescripcion());
            }

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(full.getId().toString());
            row.createCell(1).setCellValue(person[0]);
            row.createCell(2).setCellValue(person[1]);
            row.createCell(3).setCellValue(person[2]);
            row.createCell(4).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(5).setCellValue(supervisorsCenter);
            row.createCell(6).setCellValue(supervisorsOther);
            row.createCell(7).setCellValue(
                resolvePostdoctoralResourcesLabel(full.getResources(), resourceLabelById, "; ")
            );
            row.createCell(8).setCellValue(
                resolvePostdoctoralFundingLabel(full.getFundingSource(), fundingLabelById, "; ")
            );
            row.createCell(9).setCellValue(formatYearYYYY(full.getFechaInicio()));
            row.createCell(10).setCellValue(formatYearYYYY(full.getFechaTermino()));
            row.createCell(11).setCellValue(tipoSectorLabel);
            row.createCell(12).setCellValue(resolveInstitucionLabel(full.getInstitucion(), textosMap));
            row.createCell(13).setCellValue(resolveClusterDescriptions(full.getCluster(), clusterDescriptions));
            row.createCell(14).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    private String[] buildPostdoctoralFellowPersonFields(Long productId) {
        List<String> names = new ArrayList<>();
        List<String> runs = new ArrayList<>();
        List<String> genders = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();

        for (ParticipacionProducto pp : participacionProductoRepository.findByProductoId(productId)) {
            if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) {
                continue;
            }
            if (pp.getTipoParticipacion().getId() != 19L) {
                continue;
            }
            RRHH r = pp.getRrhh();
            if (r == null) {
                continue;
            }
            String dedup = r.getId() != null
                ? "id:" + r.getId()
                : "k:"
                    + (r.getFullname() != null ? r.getFullname() : "")
                    + "|"
                    + (r.getIdRecurso() != null ? r.getIdRecurso() : "");
            if (!seenKeys.add(dedup)) {
                continue;
            }
            names.add(r.getFullname() != null && !r.getFullname().trim().isEmpty()
                ? r.getFullname().trim()
                : "");
            runs.add(r.getIdRecurso() != null ? r.getIdRecurso().trim() : "");
            genders.add(r.getCodigoGenero() != null ? r.getCodigoGenero().trim() : "");
        }
        return new String[] {
            String.join("; ", names),
            String.join("; ", runs),
            String.join("; ", genders)
        };
    }

    /**
     * Supervisors: participation 17 (principal), 18 (supervisor).
     */
    private String formatPostdoctoralSupervisorNames(Long productId, boolean center) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (ParticipacionProducto pp : participacionProductoRepository.findByProductoId(productId)) {
            if (pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) {
                continue;
            }
            long tp = pp.getTipoParticipacion().getId();
            if (tp != 17L && tp != 18L) {
                continue;
            }
            RRHH rrhh = pp.getRrhh();
            if (rrhh == null) {
                continue;
            }
            Long tipoRrhhId = null;
            if (rrhh.getTipoRRHH() != null && rrhh.getTipoRRHH().getId() != null) {
                tipoRrhhId = rrhh.getTipoRRHH().getId();
            }
            boolean external = tipoRrhhId != null && (tipoRrhhId == 32L || tipoRrhhId == 60L);
            if (center && external) {
                continue;
            }
            if (!center && !external) {
                continue;
            }
            String name = rrhh.getFullname() != null ? rrhh.getFullname().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            String suffix = tp == 17L ? " (Principal Supervisor)" : " (Supervisor)";
            Long rid = rrhh.getId();
            String key = (rid != null ? rid.toString() : name) + "_" + tp;
            ordered.putIfAbsent(key, name + suffix);
        }
        return String.join("; ", ordered.values());
    }

    private String resolvePostdoctoralResourcesLabel(
        String rawResources,
        Map<Long, String> resourceLabels,
        String separator
    ) {
        if (rawResources == null || rawResources.trim().isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String part : rawResources.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                long id = Long.parseLong(s);
                labels.add(resourceLabels.getOrDefault(id, s));
            } catch (NumberFormatException e) {
                labels.add(s);
            }
        }
        return String.join(separator, labels);
    }

    private String resolvePostdoctoralFundingLabel(
        String rawFunding,
        Map<Long, String> fundingLabels,
        String joinSeparator
    ) {
        if (rawFunding == null || rawFunding.trim().isEmpty()) {
            return "";
        }
        String raw = rawFunding.trim();
        try {
            if (raw.startsWith("[")) {
                List<String> labels = new ArrayList<>();
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> arr = mapper.readValue(raw, List.class);
                for (Object obj : arr) {
                    if (obj instanceof Map<?, ?> m) {
                        Object idObj = m.get("id");
                        Object textObj = m.get("text");
                        if (idObj instanceof Number) {
                            long id = ((Number) idObj).longValue();
                            String base = fundingLabels.getOrDefault(id, String.valueOf(id));
                            if (textObj != null && !textObj.toString().trim().isEmpty()) {
                                labels.add(base + " (" + textObj.toString().trim() + ")");
                            } else {
                                labels.add(base);
                            }
                        }
                    }
                }
                return String.join(joinSeparator, labels);
            }
            List<String> labels = new ArrayList<>();
            for (String part : raw.split(",")) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(s);
                    labels.add(fundingLabels.getOrDefault(id, s));
                } catch (NumberFormatException e) {
                    labels.add(s);
                }
            }
            return String.join(joinSeparator, labels);
        } catch (Exception e) {
            return raw;
        }
    }

    private void createTechnologyTransferSheet(
        Workbook workbook,
        String sheetName,
        List<? extends ProductoCientifico> transferenciasRaw
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID (id interno)");
        header.createCell(1).setCellValue("Category of Transfer");
        header.createCell(2).setCellValue("Type of Transfer");
        header.createCell(3).setCellValue("Transfer Products");
        header.createCell(4).setCellValue("Name of Beneficiary Institution");
        header.createCell(5).setCellValue("Country");
        header.createCell(6).setCellValue("City");
        header.createCell(7).setCellValue("Place/Region");
        header.createCell(8).setCellValue("Date(Year)");
        header.createCell(9).setCellValue("Name of Research Line");
        header.createCell(10).setCellValue("Progress report");

        Map<Long, String> tipoTransferenciaCodeById = new HashMap<>();
        for (TipoTransferencia tt : tipoTransferenciaRepository.findAllByOrderByIdAsc()) {
            if (tt.getId() != null) tipoTransferenciaCodeById.put(tt.getId(), tt.getIdDescripcion());
        }

        Map<Long, String> categoriaTransferenciaCodeById = new HashMap<>();
        for (CategoriaTransferencia c : categoriaTransferenciaRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) categoriaTransferenciaCodeById.put(c.getId(), c.getIdDescripcion());
        }

        Map<Long, String> clusterDescriptions = new HashMap<>();
        for (VCluster c : vClusterRepository.findAllByOrderByIdAsc()) {
            if (c.getId() != null) clusterDescriptions.put(c.getId(), c.getDescripcion());
        }

        Set<String> textCodes = new HashSet<>();
        for (ProductoCientifico p : transferenciasRaw) {
            if (!(p instanceof TransferenciaTecnologica)) continue;
            TransferenciaTecnologica t = (TransferenciaTecnologica) p;
            if (t.getTipoTransferencia() != null && t.getTipoTransferencia().getIdDescripcion() != null) {
                textCodes.add(t.getTipoTransferencia().getIdDescripcion());
            }
            if (t.getInstitucion() != null && t.getInstitucion().getIdDescripcion() != null) {
                textCodes.add(t.getInstitucion().getIdDescripcion());
            }
            if (t.getPais() != null && t.getPais().getIdDescripcion() != null) {
                textCodes.add(t.getPais().getIdDescripcion());
            }
            if (t.getCategoriaTransferencia() != null && !t.getCategoriaTransferencia().trim().isEmpty()) {
                String raw = t.getCategoriaTransferencia().trim();
                if (raw.startsWith("[")) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        List<?> arr = mapper.readValue(raw, List.class);
                        for (Object obj : arr) {
                            Long id = null;
                            if (obj instanceof Number) {
                                id = ((Number) obj).longValue();
                            } else if (obj instanceof Map) {
                                Object idObj = ((Map<?, ?>) obj).get("id");
                                if (idObj instanceof Number) id = ((Number) idObj).longValue();
                            }
                            if (id != null) {
                                String code = categoriaTransferenciaCodeById.get(id);
                                if (code != null) textCodes.add(code);
                            }
                        }
                    } catch (Exception ignored) {}
                } else {
                    for (String part : raw.split(",")) {
                        String s = part.trim();
                        if (s.isEmpty()) continue;
                        try {
                            long id = Long.parseLong(s);
                            String code = categoriaTransferenciaCodeById.get(id);
                            if (code != null) textCodes.add(code);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (ProductoCientifico p : transferenciasRaw) {
            if (!(p instanceof TransferenciaTecnologica)) continue;
            TransferenciaTecnologica t = (TransferenciaTecnologica) p;

            TransferenciaTecnologica full = t.getId() != null
                ? transferenciaTecnologicaRepository.findByIdWithRelations(t.getId()).orElse(t)
                : t;

            String categoryOfTransfer = resolveTransferCategoriesLabel(full.getCategoriaTransferencia(), categoriaTransferenciaCodeById, textosMap);

            String typeOfTransfer = "";
            if (full.getTipoTransferencia() != null) {
                String code = full.getTipoTransferencia().getIdDescripcion();
                typeOfTransfer = resolveText(textosMap, code);
                if (typeOfTransfer.equals(code)) {
                    Long id = full.getTipoTransferencia().getId();
                    if (id != null) {
                        typeOfTransfer = resolveText(textosMap, tipoTransferenciaCodeById.get(id));
                    }
                }
            }

            String institution = "";
            if (full.getInstitucion() != null) {
                if (full.getInstitucion().getDescripcion() != null && !full.getInstitucion().getDescripcion().trim().isEmpty()) {
                    institution = full.getInstitucion().getDescripcion().trim();
                } else {
                    institution = resolveText(textosMap, full.getInstitucion().getIdDescripcion());
                }
            }

            String country = full.getPais() != null
                ? resolveText(textosMap, full.getPais().getIdDescripcion())
                : "";

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(full.getId() != null ? full.getId().toString() : "");
            row.createCell(1).setCellValue(categoryOfTransfer);
            row.createCell(2).setCellValue(typeOfTransfer);
            row.createCell(3).setCellValue(""); // Transfer Products: empty for now
            row.createCell(4).setCellValue(institution);
            row.createCell(5).setCellValue(country);
            row.createCell(6).setCellValue(full.getCiudad() != null ? full.getCiudad() : "");
            row.createCell(7).setCellValue(full.getRegion() != null ? full.getRegion() : "");
            row.createCell(8).setCellValue(full.getAgno() != null ? full.getAgno() : 0);
            // For Technology Transfer keep comma-separated clusters as requested.
            row.createCell(9).setCellValue(resolveClusterDescriptionsWithSeparator(full.getCluster(), clusterDescriptions, ", "));
            row.createCell(10).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
        }
    }

    private void createOutreachActivitiesSheet(
        Workbook workbook,
        String sheetName,
        List<Difusion> outreach
    ) {
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID (id interno)");
        header.createCell(1).setCellValue("Activity Type");
        header.createCell(2).setCellValue("Activity Name");
        header.createCell(3).setCellValue("Activity Description");
        header.createCell(4).setCellValue("Date");
        header.createCell(5).setCellValue("Attendants Amount");
        header.createCell(6).setCellValue("Duration (Days)");
        header.createCell(7).setCellValue("Country");
        header.createCell(8).setCellValue("Place/Region");
        header.createCell(9).setCellValue("City");
        header.createCell(10).setCellValue("Target Audiences");
        header.createCell(11).setCellValue("Name of the main Responsible");
        header.createCell(12).setCellValue("Progress report");
        header.createCell(13).setCellValue("Link");

        Map<Long, String> tipoDifusionDescCodeById = new HashMap<>();
        for (TipoDifusion td : tipoDifusionRepository.findAllByOrderByIdAsc()) {
            if (td.getId() != null) tipoDifusionDescCodeById.put(td.getId(), td.getIdDescripcion());
        }

        Map<Long, String> publicoObjetivoDescCodeById = new HashMap<>();
        for (PublicoObjetivo po : publicoObjetivoRepository.findAllByOrderByIdAsc()) {
            if (po.getId() != null) publicoObjetivoDescCodeById.put(po.getId(), po.getIdDescripcion());
        }

        Set<String> textCodes = new HashSet<>();
        for (Difusion d : outreach) {
            if (d.getDescripcion() != null) textCodes.add(d.getDescripcion());
            if (d.getComentario() != null) textCodes.add(d.getComentario());
            if (d.getTipoDifusion() != null && d.getTipoDifusion().getIdDescripcion() != null) {
                textCodes.add(d.getTipoDifusion().getIdDescripcion());
            }
            if (d.getPais() != null && d.getPais().getIdDescripcion() != null) {
                textCodes.add(d.getPais().getIdDescripcion());
            }
            if (d.getPublicoObjetivo() != null && !d.getPublicoObjetivo().trim().isEmpty()) {
                String[] ids = d.getPublicoObjetivo().split(",");
                for (String idStr : ids) {
                    String s = idStr.trim();
                    if (s.isEmpty()) continue;
                    try {
                        long id = Long.parseLong(s);
                        String code = publicoObjetivoDescCodeById.get(id);
                        if (code != null) textCodes.add(code);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(new ArrayList<>(textCodes), 2, "us");

        for (Difusion d : outreach) {
            Difusion full = d.getId() != null
                ? difusionRepository.findByIdWithRelations(d.getId()).orElse(d)
                : d;

            Row row = sheet.createRow(rowIdx++);

            String activityType = "";
            if (full.getTipoDifusion() != null) {
                String code = full.getTipoDifusion().getIdDescripcion();
                activityType = resolveText(textosMap, code);
                if (activityType.equals(code)) {
                    Long id = full.getTipoDifusion().getId();
                    if (id != null) {
                        activityType = resolveText(textosMap, tipoDifusionDescCodeById.get(id));
                    }
                }
            }

            String country = full.getPais() != null
                ? resolveText(textosMap, full.getPais().getIdDescripcion())
                : "";

            String targetAudiences = "";
            if (full.getPublicoObjetivo() != null && !full.getPublicoObjetivo().trim().isEmpty()) {
                List<String> labels = new ArrayList<>();
                for (String idStr : full.getPublicoObjetivo().split(",")) {
                    String s = idStr.trim();
                    if (s.isEmpty()) continue;
                    try {
                        long id = Long.parseLong(s);
                        String code = publicoObjetivoDescCodeById.get(id);
                        if (code != null) {
                            labels.add(resolveText(textosMap, code));
                        } else {
                            labels.add(s);
                        }
                    } catch (NumberFormatException e) {
                        labels.add(s);
                    }
                }
                targetAudiences = String.join("; ", labels);
            }

            List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(full.getId());
            List<String> mainResponsibles = new ArrayList<>();
            Set<Long> seenRrhh = new HashSet<>();
            for (ParticipacionProducto pp : participaciones) {
                if (pp == null || pp.getRrhh() == null || pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null) continue;
                if (pp.getTipoParticipacion().getId() != 20L) continue;
                RRHH rrhh = pp.getRrhh();
                if (rrhh.getId() != null && !seenRrhh.add(rrhh.getId())) continue;
                if (rrhh.getFullname() != null && !rrhh.getFullname().trim().isEmpty()) {
                    mainResponsibles.add(rrhh.getFullname().trim());
                }
            }

            row.createCell(0).setCellValue(full.getId() != null ? full.getId().toString() : "");
            row.createCell(1).setCellValue(activityType);
            row.createCell(2).setCellValue(resolveText(textosMap, full.getDescripcion()));
            row.createCell(3).setCellValue(resolveText(textosMap, full.getComentario()));
            row.createCell(4).setCellValue(formatDateDDMMYYYY(full.getFechaInicio()));
            row.createCell(5).setCellValue(full.getNumAsistentes() != null ? full.getNumAsistentes() : 0);
            row.createCell(6).setCellValue(full.getDuracion() != null ? full.getDuracion() : 0);
            row.createCell(7).setCellValue(country);
            row.createCell(8).setCellValue(full.getLugar() != null ? full.getLugar() : "");
            row.createCell(9).setCellValue(full.getCiudad() != null ? full.getCiudad() : "");
            row.createCell(10).setCellValue(targetAudiences);
            row.createCell(11).setCellValue(String.join("; ", mainResponsibles));
            row.createCell(12).setCellValue(full.getProgressReport() != null ? full.getProgressReport() : "");
            row.createCell(13).setCellValue(full.getLink() != null ? full.getLink() : "");
        }
    }

    private String resolveClusterDescriptions(String rawClusters, Map<Long, String> clusterDescriptions) {
        return resolveClusterDescriptionsWithSeparator(rawClusters, clusterDescriptions, "; ");
    }

    private String resolveClusterDescriptionsWithSeparator(String rawClusters, Map<Long, String> clusterDescriptions, String separator) {
        if (rawClusters == null || rawClusters.trim().isEmpty()) return "";
        String[] parts = rawClusters.split(",");
        List<String> labels = new ArrayList<>();
        for (String part : parts) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            try {
                long id = Long.parseLong(s);
                labels.add(clusterDescriptions.getOrDefault(id, s));
            } catch (NumberFormatException e) {
                labels.add(s);
            }
        }
        return String.join(separator, labels);
    }

    private String resolveTransferCategoriesLabel(String rawCategories, Map<Long, String> categoriaTransferenciaCodeById, Map<String, String> textosMap) {
        if (rawCategories == null || rawCategories.trim().isEmpty()) return "";

        String raw = rawCategories.trim();
        List<String> labels = new ArrayList<>();

        if (raw.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> arr = mapper.readValue(raw, List.class);
                for (Object obj : arr) {
                    Long id = null;
                    if (obj instanceof Number) {
                        id = ((Number) obj).longValue();
                    } else if (obj instanceof Map) {
                        Object idObj = ((Map<?, ?>) obj).get("id");
                        if (idObj instanceof Number) id = ((Number) idObj).longValue();
                    }
                    if (id != null) {
                        String code = categoriaTransferenciaCodeById.get(id);
                        labels.add(resolveText(textosMap, code));
                    }
                }
                return String.join("; ", labels);
            } catch (Exception ignored) {
                // fallback to comma parsing
            }
        }

        for (String part : raw.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            try {
                long id = Long.parseLong(s);
                String code = categoriaTransferenciaCodeById.get(id);
                labels.add(resolveText(textosMap, code));
            } catch (NumberFormatException e) {
                labels.add(s);
            }
        }
        return String.join("; ", labels);
    }

    private void addPdfsForSheet(
        ZipOutputStream zipOut,
        String folderName,
        List<? extends ProductoCientifico> items,
        Path pdfsDirectory
    ) throws Exception {
        for (ProductoCientifico p : items) {
            if (p.getId() == null) continue;
            if (p.getLinkPDF() == null || p.getLinkPDF().trim().isEmpty()) continue;

            Path pdfPath = resolvePdfPath(p.getLinkPDF(), pdfsDirectory);
            if (pdfPath == null || !Files.exists(pdfPath)) continue;

            String entryName = folderName + "/" + p.getId() + ".pdf";
            ZipEntry entry = new ZipEntry(entryName);
            zipOut.putNextEntry(entry);

            try (InputStream in = Files.newInputStream(pdfPath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }
            }
            zipOut.closeEntry();
        }
    }

    private void createDirectoryEntry(ZipOutputStream zipOut, String folderName) throws Exception {
        // Explicit directory entries make empty folders visible across unzip tools.
        String normalized = folderName.endsWith("/") ? folderName : folderName + "/";
        ZipEntry entry = new ZipEntry(normalized);
        zipOut.putNextEntry(entry);
        zipOut.closeEntry();
    }

    private int countPdfsAvailable(List<? extends ProductoCientifico> items, Path pdfsDirectory) {
        if (pdfsDirectory == null) return 0;
        int count = 0;
        for (ProductoCientifico p : items) {
            if (isPdfAvailable(p.getId(), p.getLinkPDF(), pdfsDirectory)) count++;
        }
        return count;
    }

    private boolean isPdfAvailable(Long id, String linkPDF, Path pdfsDirectory) {
        if (id == null || pdfsDirectory == null) return false;
        if (linkPDF == null || linkPDF.trim().isEmpty()) return false;
        Path path = resolvePdfPath(linkPDF, pdfsDirectory);
        return path != null && Files.exists(path);
    }

    private Path resolvePdfPath(String linkPDF, Path pdfsDirectory) {
        if (linkPDF == null || pdfsDirectory == null) return null;
        String fileName;
        if (linkPDF.startsWith("PDF:")) {
            String path = linkPDF.substring(4);
            fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
        } else {
            fileName = linkPDF.contains("/") ? linkPDF.substring(linkPDF.lastIndexOf("/") + 1) : linkPDF;
        }
        if (fileName == null || fileName.trim().isEmpty()) return null;
        return pdfsDirectory.resolve(fileName);
    }

    private String resolveText(Map<String, String> textosMap, String code) {
        if (code == null) return "";
        return textosMap.getOrDefault(code, code);
    }

    private String convertClustersToRoman(String clusters) {
        if (clusters == null || clusters.trim().isEmpty()) return "";
        String[] parts = clusters.split(",");
        List<String> romans = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            romans.add(mapClusterLabel(s));
        }
        return String.join(", ", romans);
    }

    private String mapClusterLabel(String clusterId) {
        switch (clusterId) {
            case "1": return "I";
            case "2": return "II";
            case "3": return "III";
            case "4": return "IV";
            case "5": return "V";
            default: return clusterId;
        }
    }

    private Path resolvePdfsDirectory() {
        try {
            if (pdfsPathConfig != null && !pdfsPathConfig.trim().isEmpty()) {
                Path configPath = Paths.get(pdfsPathConfig);
                if (Files.exists(configPath) || Files.isSymbolicLink(configPath)) {
                    return configPath.toRealPath();
                }
                return configPath.toAbsolutePath();
            }

            Path backendPdfsPath = Paths.get("backend/pdfs").toAbsolutePath();
            if (Files.exists(backendPdfsPath) || Files.isSymbolicLink(backendPdfsPath)) {
                return backendPdfsPath.toRealPath();
            }

            Path currentPdfsPath = Paths.get("pdfs").toAbsolutePath();
            if (Files.exists(currentPdfsPath) || Files.isSymbolicLink(currentPdfsPath)) {
                return currentPdfsPath.toRealPath();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class ParticipationEventGroupKey {
        private final Long tipoEventoId;
        private final String descripcionCode;
        private final String paisCodigo;
        private final String ciudad;
        private final LocalDate fechaInicio;

        ParticipationEventGroupKey(
            Long tipoEventoId,
            String descripcionCode,
            String paisCodigo,
            String ciudad,
            LocalDate fechaInicio
        ) {
            this.tipoEventoId = tipoEventoId;
            this.descripcionCode = descripcionCode;
            this.paisCodigo = paisCodigo;
            this.ciudad = ciudad;
            this.fechaInicio = fechaInicio;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ParticipationEventGroupKey that = (ParticipationEventGroupKey) o;
            return Objects.equals(tipoEventoId, that.tipoEventoId)
                && Objects.equals(descripcionCode, that.descripcionCode)
                && Objects.equals(paisCodigo, that.paisCodigo)
                && Objects.equals(ciudad, that.ciudad)
                && Objects.equals(fechaInicio, that.fechaInicio);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tipoEventoId, descripcionCode, paisCodigo, ciudad, fechaInicio);
        }
    }

    private static final class ParticipationScientificEventGroupedRow {
        OrganizacionEventosCientificos representative;
        final List<Long> productIds = new ArrayList<>();
        final LinkedHashMap<Long, String> centerSpeakerNames = new LinkedHashMap<>();
        final LinkedHashMap<Long, String> otherSpeakerNames = new LinkedHashMap<>();
    }
}

