package com.sisgic.controller;

import com.sisgic.service.MediaFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sirve imágenes bajo /sigic2.0/media/**
 * Ejemplo: GET /sigic2.0/media/miimg.jpg
 */
@RestController
@CrossOrigin(origins = "*")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping("/media/**")
    public ResponseEntity<Resource> serveMedia(HttpServletRequest request) {
        try {
            String relativePath = extractRelativeMediaPath(request);
            if (relativePath.isBlank()) {
                return ResponseEntity.notFound().build();
            }

            Path file = mediaFileService.resolveMediaFile(relativePath);
            if (file == null || !Files.isRegularFile(file)) {
                log.warn("Media file not found for path '{}' (requested URI: {})",
                    relativePath, request.getRequestURI());
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(new FileSystemResource(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error serving media: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractRelativeMediaPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        String prefix = contextPath + "/media/";
        if (!uri.startsWith(prefix)) {
            return "";
        }
        String relative = uri.substring(prefix.length());
        return URLDecoder.decode(relative, StandardCharsets.UTF_8);
    }
}
