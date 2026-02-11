package com.sisgic.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Value("${pdfs.path:}")
    private String pdfsPathConfig;

    /**
     * Sube un archivo PDF y lo guarda en el directorio pdfs
     * @param file El archivo PDF a subir
     * @return La ruta relativa del archivo guardado en formato "PDF:pdfs/nombre_archivo.pdf"
     */
    @PostMapping("/upload-pdf")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Validar que el archivo no esté vacío
            if (file.isEmpty()) {
                response.put("error", "El archivo está vacío");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validar que sea un PDF
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                response.put("error", "El archivo debe ser un PDF");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Obtener el directorio de PDFs
            Path pdfsDirectory = getPdfsDirectory();
            if (pdfsDirectory == null) {
                response.put("error", "No se pudo determinar el directorio de PDFs. Configure pdfs.path en application.yml");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Asegurar que el directorio existe
            if (!Files.exists(pdfsDirectory)) {
                Files.createDirectories(pdfsDirectory);
            }
            
            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".pdf";
            }
            
            // Generar nombre único: usar UUID + extensión
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path targetPath = pdfsDirectory.resolve(uniqueFilename);
            
            // Guardar el archivo
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Retornar la ruta en el formato esperado: "PDF:pdfs/nombre_archivo.pdf"
            String relativePath = "PDF:pdfs/" + uniqueFilename;
            response.put("linkPDF", relativePath);
            response.put("filename", uniqueFilename);
            response.put("message", "Archivo subido correctamente");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "Error al guardar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Obtiene el directorio de PDFs configurado
     */
    private Path getPdfsDirectory() {
        try {
            Path pdfsPath;
            
            // Si hay una ruta configurada, usarla
            if (pdfsPathConfig != null && !pdfsPathConfig.trim().isEmpty()) {
                pdfsPath = Paths.get(pdfsPathConfig);
                if (Files.exists(pdfsPath) || Files.isSymbolicLink(pdfsPath)) {
                    return pdfsPath.toRealPath();
                }
                return pdfsPath.toAbsolutePath();
            }
            
            // Si no, intentar resolver automáticamente
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
            e.printStackTrace();
            return null;
        }
    }
}

