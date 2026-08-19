package com.sisgic.controller;

import com.sisgic.entity.Documento;
import com.sisgic.service.DocumentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Serves binary content stored in the legacy documento table.
 * RRHH.urlImagen and similar fields reference documento.url.
 */
@RestController
@CrossOrigin(origins = "*")
public class DocumentoController {

    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);

    @Autowired
    private DocumentoService documentoService;

    @GetMapping("/images/**")
    public ResponseEntity<byte[]> serveImage(HttpServletRequest request) {
        return serveDocumento(request, "/images/");
    }

    private ResponseEntity<byte[]> serveDocumento(HttpServletRequest request, String prefixAfterContext) {
        try {
            String documentUrl = extractDocumentUrl(request, prefixAfterContext);
            if (documentUrl.isBlank()) {
                return ResponseEntity.notFound().build();
            }

            Optional<Documento> documento = documentoService.findByUrl(documentUrl);
            if (documento.isEmpty() || documento.get().getData() == null || documento.get().getData().length == 0) {
                log.debug("Documento not found for url '{}'", documentUrl);
                return ResponseEntity.notFound().build();
            }

            Documento doc = documento.get();
            String contentType = documentoService.resolveContentType(doc.getUrl(), doc.getData());
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(doc.getData());
        } catch (Exception e) {
            log.error("Error serving documento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractDocumentUrl(HttpServletRequest request, String pathPrefix) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        String fullPrefix = contextPath + pathPrefix;
        if (!uri.startsWith(fullPrefix)) {
            return "";
        }
        String relative = uri.substring(fullPrefix.length());
        String decoded = URLDecoder.decode(relative, StandardCharsets.UTF_8);
        if (decoded.isBlank()) {
            return "";
        }
        String folder = pathPrefix.replaceAll("^/|/$", "");
        return folder + "/" + decoded;
    }
}
