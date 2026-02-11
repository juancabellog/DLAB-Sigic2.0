package com.sisgic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio para manejar operaciones con archivos PDF
 */
@Service
public class PdfFileService {

    @Value("${pdfs.path:}")
    private String pdfsPathConfig;

    /**
     * Elimina un archivo PDF basado en el linkPDF
     * @param linkPDF El linkPDF en formato "PDF:pdfs/nombre_archivo.pdf"
     * @return true si el archivo fue eliminado, false si no existía o hubo un error
     */
    public boolean deletePdfFile(String linkPDF) {
        if (linkPDF == null || linkPDF.trim().isEmpty()) {
            return false;
        }

        try {
            // Extraer el nombre del archivo del linkPDF
            // Formato: "PDF:pdfs/nombre_archivo.pdf"
            String fileName;
            if (linkPDF.startsWith("PDF:")) {
                String path = linkPDF.substring(4); // Remover "PDF:"
                // Extraer solo el nombre del archivo (última parte después de /)
                if (path.contains("/")) {
                    fileName = path.substring(path.lastIndexOf("/") + 1);
                } else {
                    fileName = path;
                }
            } else {
                // Si no tiene el prefijo "PDF:", asumir que es solo el nombre del archivo
                if (linkPDF.contains("/")) {
                    fileName = linkPDF.substring(linkPDF.lastIndexOf("/") + 1);
                } else {
                    fileName = linkPDF;
                }
            }

            // Obtener el directorio de PDFs
            Path pdfsDirectory = getPdfsDirectory();
            if (pdfsDirectory == null) {
                System.err.println("PdfFileService: No se pudo determinar el directorio de PDFs");
                return false;
            }

            // Construir la ruta completa del archivo
            Path filePath = pdfsDirectory.resolve(fileName);

            // Verificar si el archivo existe y eliminarlo
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("PdfFileService: Archivo PDF eliminado: " + filePath);
                return true;
            } else {
                System.out.println("PdfFileService: El archivo PDF no existe: " + filePath);
                return false;
            }

        } catch (IOException e) {
            System.err.println("PdfFileService: Error al eliminar el archivo PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("PdfFileService: Error inesperado al eliminar el archivo PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
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









