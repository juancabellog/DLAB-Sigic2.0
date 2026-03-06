package com.sisgic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

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
     * Descarga un archivo PDF desde una URL y lo guarda en el directorio de PDFs.
     * Retorna el valor que debe almacenarse en linkPDF con el formato:
     * "PDF:pdfs/{nombre_archivo}.pdf"
     *
     * @param pdfUrl URL directa al PDF (por ejemplo, desde OpenAlex primary_location.pdf_url)
     * @return String para linkPDF o null si falla la descarga/almacenamiento
     */
    public String downloadAndSavePdfFromUrl(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            return null;
        }

        InputStream inputStream = null;
        try {
            Path pdfsDirectory = getPdfsDirectory();
            if (pdfsDirectory == null) {
                System.err.println("PdfFileService: No se pudo determinar el directorio de PDFs para guardar el archivo");
                return null;
            }

            // Asegurar que el directorio exista
            if (!Files.exists(pdfsDirectory)) {
                Files.createDirectories(pdfsDirectory);
            }

            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path targetPath = pdfsDirectory.resolve(fileName);

            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Sisgic-Backend/1.0");
            connection.setRequestProperty("Accept", "application/pdf,application/octet-stream;q=0.9,*/*;q=0.8");

            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                // Seguir redirecciones manuales si es necesario
                String redirectUrl = connection.getHeaderField("Location");
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    connection.disconnect();
                    url = new URL(redirectUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "Sisgic-Backend/1.0");
                    connection.setRequestProperty("Accept", "application/pdf,application/octet-stream;q=0.9,*/*;q=0.8");
                    status = connection.getResponseCode();
                }
            }

            if (status != HttpURLConnection.HTTP_OK) {
                System.err.println("PdfFileService: Respuesta HTTP no OK al intentar descargar PDF: " + status + " desde " + pdfUrl);
                connection.disconnect();
                return null;
            }

            inputStream = connection.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            // Leer los primeros bytes para verificar si realmente es un PDF (%PDF-)
            byte[] header = new byte[5];
            int bytesRead = bufferedInputStream.read(header);
            if (bytesRead < 5) {
                System.err.println("PdfFileService: Respuesta demasiado corta al intentar descargar PDF desde " + pdfUrl);
                connection.disconnect();
                return null;
            }

            String headerString = new String(header, 0, bytesRead, StandardCharsets.US_ASCII);
            if (!headerString.startsWith("%PDF-")) {
                System.err.println("PdfFileService: El contenido descargado no parece ser un PDF (header: " + headerString + ") desde " + pdfUrl);
                connection.disconnect();
                return null;
            }

            // Es un PDF válido, escribir encabezado + resto del stream al archivo
            try (OutputStream outputStream = Files.newOutputStream(
                    targetPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                outputStream.write(header, 0, bytesRead);

                byte[] buffer = new byte[8192];
                int len;
                while ((len = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            }

            connection.disconnect();

            System.out.println("PdfFileService: PDF descargado y guardado en " + targetPath);

            // Siempre usar el prefijo lógico "pdfs/" para el linkPDF, independiente
            // de si físicamente se guardó en "backend/pdfs" o "pdfs"
            String logicalPath = "PDF:pdfs/" + fileName;
            return logicalPath;
        } catch (Exception e) {
            System.err.println("PdfFileService: Error al descargar/guardar PDF desde URL: " + pdfUrl);
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
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









