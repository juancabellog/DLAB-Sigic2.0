package com.sisgic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${pdfs.path:}")
    private String pdfsPathConfig;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // IMPORTANTE: Los handlers más específicos deben registrarse ANTES de los genéricos
        // Si /** se registra primero, capturará todas las peticiones incluyendo /pdfs/**
        
        // Servir PDFs desde el directorio pdfs (debe ir ANTES del handler /**)
        String pdfsPath;
        try {
            File d = new File(".");
            System.out.println("=== WebConfig: Current directory: " + d.getAbsolutePath());
            
            // Primero verificar si hay una ruta configurada en application.yml
            if (pdfsPathConfig != null && !pdfsPathConfig.trim().isEmpty()) {
                Path configPath = Paths.get(pdfsPathConfig);
                if (Files.exists(configPath) || Files.isSymbolicLink(configPath)) {
                    Path resolvedPath = configPath.toRealPath();
                    pdfsPath = resolvedPath.toUri().toString();
                    System.out.println("=== WebConfig: Using configured path, resolved to: " + pdfsPath);
                } else {
                    // Si la ruta configurada no existe, intentar como absoluta
                    pdfsPath = configPath.toAbsolutePath().toUri().toString();
                    System.out.println("=== WebConfig: Using configured path (absolute): " + pdfsPath);
                }
            } else {
                // Si no hay configuración, intentar resolver automáticamente
                Path backendPdfsPath = Paths.get("backend/pdfs").toAbsolutePath();
                System.out.println("=== WebConfig: Trying backend/pdfs path: " + backendPdfsPath);
                
                if (Files.exists(backendPdfsPath) || Files.isSymbolicLink(backendPdfsPath)) {
                    // Resolver el link simbólico si existe
                    Path resolvedPath = backendPdfsPath.toRealPath();
                    pdfsPath = resolvedPath.toUri().toString();
                    System.out.println("=== WebConfig: Found backend/pdfs, resolved to: " + pdfsPath);
                } else {
                    // Si no existe, intentar desde el directorio actual
                    Path currentPdfsPath = Paths.get("pdfs").toAbsolutePath();
                    System.out.println("=== WebConfig: Trying pdfs path: " + currentPdfsPath);
                    
                    if (Files.exists(currentPdfsPath) || Files.isSymbolicLink(currentPdfsPath)) {
                        Path resolvedPath = currentPdfsPath.toRealPath();
                        pdfsPath = resolvedPath.toUri().toString();
                        System.out.println("=== WebConfig: Found pdfs, resolved to: " + pdfsPath);
                    } else {
                        // Si no se encuentra, lanzar excepción para que el administrador configure la ruta
                        throw new RuntimeException("No se pudo encontrar el directorio de PDFs. " +
                                "Por favor configure la propiedad 'pdfs.path' en application.yml o " +
                                "asegúrese de que existe el directorio 'backend/pdfs' o 'pdfs'.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("=== WebConfig: ERROR al configurar el directorio de PDFs: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al configurar el directorio de PDFs. " +
                    "Por favor configure la propiedad 'pdfs.path' en application.yml", e);
        }
        
        // Asegurar que el path termine con /
        if (!pdfsPath.endsWith("/")) {
            pdfsPath += "/";
        }
        
        System.out.println("=== WebConfig: Registering PDF handler for /pdfs/** -> " + pdfsPath);
        registry.addResourceHandler("/pdfs/**")
                .addResourceLocations(pdfsPath)
                .setCachePeriod(3600); // 1 hora de caché para PDFs
        
        // Servir archivos estáticos del frontend (debe ir DESPUÉS de handlers específicos)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(31556926); // 1 año de caché
    }
}



