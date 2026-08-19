package com.sisgic.controller;

import com.sisgic.dto.AgendaDTO;
import com.sisgic.dto.CategoryDTO;
import com.sisgic.dto.CreateCategoryRequest;
import com.sisgic.dto.EstadoNoticiaDTO;
import com.sisgic.dto.TipoEventoAgendaDTO;
import com.sisgic.dto.TranslateNewsRequest;
import com.sisgic.dto.TranslateNewsResponse;
import com.sisgic.service.AgendaService;
import com.sisgic.service.GeminiTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
@CrossOrigin(origins = "*")
public class AgendaController {

    private static final Logger log = LoggerFactory.getLogger(AgendaController.class);

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private GeminiTranslationService geminiTranslationService;

    @GetMapping
    public ResponseEntity<Page<AgendaDTO>> listAgenda(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaInicio") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String title) {

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        return ResponseEntity.ok(agendaService.findAll(estadoId, fromDate, toDate, location, title, pageable));
    }

    @GetMapping("/states")
    public ResponseEntity<List<EstadoNoticiaDTO>> listStates() {
        return ResponseEntity.ok(agendaService.listEstados());
    }

    @GetMapping("/event-types")
    public ResponseEntity<List<TipoEventoAgendaDTO>> listEventTypes() {
        return ResponseEntity.ok(agendaService.listEventTypes());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        return ResponseEntity.ok(agendaService.listCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CreateCategoryRequest request) {
        try {
            return ResponseEntity.ok(agendaService.createCategory(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendaDTO> getAgenda(@PathVariable Long id) {
        return agendaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AgendaDTO> createAgenda(@RequestBody AgendaDTO dto) {
        try {
            return ResponseEntity.ok(agendaService.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendaDTO> updateAgenda(@PathVariable Long id, @RequestBody AgendaDTO dto) {
        try {
            return agendaService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgenda(@PathVariable Long id) {
        if (agendaService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AgendaDTO> publishAgenda(@PathVariable Long id) {
        return agendaService.publish(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<AgendaDTO> unpublishAgenda(@PathVariable Long id) {
        return agendaService.unpublish(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<AgendaDTO> duplicateAgenda(@PathVariable Long id) {
        return agendaService.duplicate(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translateAgenda(@RequestBody TranslateNewsRequest req) {
        if (!geminiTranslationService.isConfigured()) {
            log.error("POST /api/agenda/translate rejected: Gemini not configured ({} API keys loaded). "
                + "Check gemini.apikeys in application.yml or GEMINI_API_KEY env var.",
                geminiTranslationService.getConfiguredKeyCount());
            return ResponseEntity.status(503)
                .body(Map.of("error", "Translation service is not configured"));
        }

        boolean enToEs = isEnToEs(req.getDirection());
        String title = enToEs ? req.getTitleEn() : req.getTitleEs();
        String summary = enToEs ? req.getSummaryEn() : req.getSummaryEs();
        String body = enToEs ? req.getBodyEn() : req.getBodyEs();

        if (isBlank(title) && isBlank(summary) && isBlank(body)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", enToEs
                    ? "English content is required for translation to Spanish"
                    : "Spanish content is required for translation to English"));
        }

        log.info("POST /api/agenda/translate: direction={}, title={} chars, summary={} chars, body={} chars",
            enToEs ? "en_to_es" : "es_to_en", textLength(title), textLength(summary), textLength(body));
        try {
            GeminiTranslationService.TranslationResult result = geminiTranslationService.translate(
                title, summary, body,
                enToEs
                    ? GeminiTranslationService.TranslationDirection.EN_TO_ES
                    : GeminiTranslationService.TranslationDirection.ES_TO_EN);
            if (enToEs) {
                return ResponseEntity.ok(TranslateNewsResponse.fromSpanish(
                    result.titleEn(), result.excerptEn(), result.bodyEn()));
            }
            return ResponseEntity.ok(TranslateNewsResponse.fromEnglish(
                result.titleEn(), result.excerptEn(), result.bodyEn()));
        } catch (IllegalStateException e) {
            log.error("POST /api/agenda/translate: {}", e.getMessage(), e);
            return ResponseEntity.status(503)
                .body(Map.of("error", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Agenda translation interrupted", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Translation was interrupted"));
        } catch (Exception e) {
            log.error("Agenda translation failed", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy);
        return sortDir.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();
    }

    private static int textLength(String value) {
        return value != null ? value.length() : 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isEnToEs(String direction) {
        return direction != null && "en_to_es".equalsIgnoreCase(direction.trim());
    }
}
