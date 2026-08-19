package com.sisgic.controller;

import com.sisgic.dto.CategoryDTO;
import com.sisgic.dto.CreateCategoryRequest;
import com.sisgic.dto.CreateTagRequest;
import com.sisgic.dto.EstadoNoticiaDTO;
import com.sisgic.dto.NoticiaDTO;
import com.sisgic.dto.TagDTO;
import com.sisgic.dto.TranslateNewsRequest;
import com.sisgic.dto.TranslateNewsResponse;
import com.sisgic.service.GeminiTranslationService;
import com.sisgic.service.NoticiaService;
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
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NoticiaController {

    private static final Logger log = LoggerFactory.getLogger(NoticiaController.class);

    @Autowired
    private NoticiaService noticiaService;

    @Autowired
    private GeminiTranslationService geminiTranslationService;

    /**
     * Listado administrativo con filtros.
     */
    @GetMapping
    public ResponseEntity<Page<NoticiaDTO>> listNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaTermino") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String title) {

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        Page<NoticiaDTO> result = noticiaService.findAll(estadoId, tagId, categoryId, fromDate, toDate, title, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Listado público (solo publicadas).
     */
    @GetMapping("/public")
    public ResponseEntity<Page<NoticiaDTO>> listPublicNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastPublishedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String title) {

        String sortField = "lastPublishedDate".equals(sortBy) ? "fechaTermino" : sortBy;
        Pageable pageable = PageRequest.of(page, size, buildSort(sortField, sortDir));
        Page<NoticiaDTO> result = noticiaService.findPublished(tagId, categoryId, fromDate, toDate, title, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/states")
    public ResponseEntity<List<EstadoNoticiaDTO>> listStates() {
        return ResponseEntity.ok(noticiaService.listEstados());
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TagDTO>> listTags() {
        return ResponseEntity.ok(noticiaService.listTags());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        return ResponseEntity.ok(noticiaService.listCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CreateCategoryRequest request) {
        try {
            return ResponseEntity.ok(noticiaService.createCategory(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/tags")
    public ResponseEntity<TagDTO> createTag(@RequestBody CreateTagRequest request) {
        try {
            return ResponseEntity.ok(noticiaService.createTag(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticiaDTO> getNews(@PathVariable Long id) {
        return noticiaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<NoticiaDTO> getPublicNews(@PathVariable Long id) {
        return noticiaService.findPublishedById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NoticiaDTO> createNews(@RequestBody NoticiaDTO dto) {
        try {
            return ResponseEntity.ok(noticiaService.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoticiaDTO> updateNews(@PathVariable Long id, @RequestBody NoticiaDTO dto) {
        try {
            return noticiaService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        if (noticiaService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{id}/visit")
    public ResponseEntity<Map<String, Boolean>> registerVisit(@PathVariable Long id) {
        if (noticiaService.findPublishedById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        boolean updated = noticiaService.incrementVisitas(id);
        return updated ? ResponseEntity.ok(Map.of("success", true)) : ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{id}/like")
    public ResponseEntity<Map<String, Boolean>> registerLike(@PathVariable Long id) {
        if (noticiaService.findPublishedById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        boolean updated = noticiaService.incrementLikes(id);
        return updated ? ResponseEntity.ok(Map.of("success", true)) : ResponseEntity.notFound().build();
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translateNews(@RequestBody TranslateNewsRequest req) {
        if (!geminiTranslationService.isConfigured()) {
            log.error("POST /api/news/translate rejected: Gemini not configured ({} API keys loaded). "
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

        log.info("POST /api/news/translate: direction={}, title={} chars, summary={} chars, body={} chars",
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
            log.error("POST /api/news/translate: {}", e.getMessage(), e);
            return ResponseEntity.status(503)
                    .body(Map.of("error", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Translation interrupted", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Translation was interrupted"));
        } catch (Exception e) {
            log.error("Translation failed", e);
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
