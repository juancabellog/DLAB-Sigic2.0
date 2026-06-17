package com.sisgic.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MediaFileService {

    private static final Logger log = LoggerFactory.getLogger(MediaFileService.class);

    @Value("${media.path:}")
    private String mediaPathConfig;

    private Path resolvedMediaDirectory;

    @PostConstruct
    void init() throws IOException {
        this.resolvedMediaDirectory = resolveMediaDirectory();
        log.info("Media directory resolved to: {}", resolvedMediaDirectory);
    }

    /**
     * Directorio base donde están los archivos (p. ej. .../backend/media o .../media).
     */
    public Path getMediaDirectory() throws IOException {
        if (resolvedMediaDirectory != null && Files.isDirectory(resolvedMediaDirectory)) {
            return resolvedMediaDirectory;
        }
        resolvedMediaDirectory = resolveMediaDirectory();
        return resolvedMediaDirectory;
    }

    public String getMediaDirectoryUri() throws IOException {
        String uri = getMediaDirectory().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    /**
     * Busca el archivo en el directorio media configurado y en ubicaciones alternativas habituales.
     */
    public Path resolveMediaFile(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }

        String cleanName = normalizeRelativePath(filename);
        if (cleanName.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        for (Path base : collectMediaDirectoryCandidates()) {
            if (!Files.isDirectory(base)) {
                continue;
            }
            Path resolved = base.resolve(cleanName).normalize();
            if (!resolved.startsWith(base)) {
                continue;
            }
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }

        return null;
    }

    private String normalizeRelativePath(String filename) {
        String clean = filename.replace('\\', '/').trim();
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (clean.startsWith("media/")) {
            clean = clean.substring("media/".length());
        }
        return clean;
    }

    private Path resolveMediaDirectory() throws IOException {
        for (Path candidate : collectMediaDirectoryCandidates()) {
            if (Files.isDirectory(candidate)) {
                return candidate.normalize();
            }
        }
        Path fallback = Paths.get("media").toAbsolutePath().normalize();
        Files.createDirectories(fallback);
        log.warn("No existing media directory found; created {}", fallback);
        return fallback;
    }

    private List<Path> collectMediaDirectoryCandidates() {
        Set<String> seen = new LinkedHashSet<>();
        List<Path> candidates = new ArrayList<>();

        addCandidate(candidates, seen, mediaPathConfig);
        addCandidate(candidates, seen, "media");
        addCandidate(candidates, seen, "backend/media");
        addCandidate(candidates, seen, "../media");
        addCandidate(candidates, seen, "../backend/media");

        if (mediaPathConfig != null && !mediaPathConfig.isBlank()) {
            addCandidate(candidates, seen, "../" + mediaPathConfig);
        }

        return candidates;
    }

    private void addCandidate(List<Path> candidates, Set<String> seen, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        Path absolute = Paths.get(path).toAbsolutePath().normalize();
        String key = absolute.toString();
        if (seen.add(key)) {
            candidates.add(absolute);
        }
    }
}
