package com.sisgic.service;

import com.sisgic.entity.Documento;
import com.sisgic.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentoService {

    private static final int MAX_URL_LENGTH = 200;

    @Autowired
    private DocumentoRepository documentoRepository;

    public Optional<Documento> findByUrl(String url) {
        String normalized = normalizeUrl(url);
        if (normalized == null) {
            return Optional.empty();
        }
        return documentoRepository.findById(normalized);
    }

    /**
     * Stores image bytes in documento and returns the url key (max 200 chars).
     * Reuses existingUrl when provided and valid.
     */
    public String saveImage(byte[] data, String existingUrl, String extension) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data is required.");
        }

        String normalizedExisting = normalizeUrl(existingUrl);
        if (normalizedExisting != null && normalizedExisting.length() <= MAX_URL_LENGTH) {
            upsert(normalizedExisting, data);
            return normalizedExisting;
        }

        String generatedUrl = generateImageUrl(extension);
        upsert(generatedUrl, data);
        return generatedUrl;
    }

    public String saveImageForRrhh(Long rrhhId, byte[] data, String existingUrl, String extension) {
        String normalizedExisting = normalizeUrl(existingUrl);
        if (normalizedExisting != null && normalizedExisting.length() <= MAX_URL_LENGTH) {
            upsert(normalizedExisting, data);
            return normalizedExisting;
        }

        String ext = normalizeExtension(extension);
        String generatedUrl = "images/rrhh-" + rrhhId + "-" + UUID.randomUUID() + ext;
        if (generatedUrl.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Generated document url exceeds 200 characters.");
        }
        upsert(generatedUrl, data);
        return generatedUrl;
    }

    public void upsert(String url, byte[] data) {
        String normalized = normalizeUrl(url);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Document url is required.");
        }
        if (normalized.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Document url exceeds 200 characters.");
        }

        Documento documento = documentoRepository.findById(normalized).orElseGet(Documento::new);
        documento.setUrl(normalized);
        documento.setData(data);
        documentoRepository.save(documento);
    }

    public String resolveContentType(String url, byte[] data) {
        if (url != null) {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            if (lower.endsWith(".png")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            if (lower.endsWith(".webp")) {
                return "image/webp";
            }
            if (lower.endsWith(".gif")) {
                return MediaType.IMAGE_GIF_VALUE;
            }
        }

        if (data != null && data.length >= 4) {
            if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            if (data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46 && data.length >= 12
                && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
                return "image/webp";
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String generateImageUrl(String extension) {
        String ext = normalizeExtension(extension);
        return "images/" + UUID.randomUUID() + ext;
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return ".jpg";
        }
        String ext = extension.trim().toLowerCase(Locale.ROOT);
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        if (".jpeg".equals(ext)) {
            return ".jpg";
        }
        return ext;
    }

    public String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String normalized = url.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("media/")) {
            normalized = normalized.substring("media/".length());
        }
        return normalized.isBlank() ? null : normalized;
    }
}
