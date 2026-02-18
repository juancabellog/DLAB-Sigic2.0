package com.sisgic.service;

import com.sisgic.entity.DetalleReporte;
import com.sisgic.entity.Reporte;
import com.sisgic.repository.DetalleReporteRepository;
import com.sisgic.repository.ReporteRepository;
import com.sisgic.util.ExcelUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private DetalleReporteRepository detalleReporteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Genera un reporte Excel basado en el idReporte
     * Adaptado de ReportesService.generateReport
     */
    @Transactional(readOnly = false)
    public byte[] generateReport(Long idReporte) throws Exception {
        System.out.println("ReportService.generateReport called with idReporte: " + idReporte);
        
        // Obtener el reporte y su template Excel
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> {
                    System.err.println("Reporte no encontrado con id: " + idReporte);
                    return new IllegalArgumentException("Reporte no encontrado con id: " + idReporte);
                });

        System.out.println("Reporte encontrado: " + reporte.getDescripcion());
        
        if (reporte.getExcelFile() == null) {
            System.err.println("El reporte con id " + idReporte + " no tiene un template Excel");
            throw new IllegalStateException("El reporte no tiene un template Excel");
        }
        
        System.out.println("Excel template size (raw): " + reporte.getExcelFile().length + " bytes");
        
        // Deserializar el archivo Excel desde la base de datos
        byte[] excelFileBytes;
        try {
            System.out.println("Deserializing Excel file from database...");
            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(reporte.getExcelFile()));
            Object deserializedValue = oi.readObject();
            oi.close();
            
            // El objeto deserializado debería ser un byte[]
            if (deserializedValue instanceof byte[]) {
                excelFileBytes = (byte[]) deserializedValue;
                System.out.println("Excel file deserialized, size: " + excelFileBytes.length + " bytes");
            } else {
                System.err.println("ERROR: El objeto deserializado no es un byte[], es: " + deserializedValue.getClass().getName());
                throw new IllegalStateException("El archivo Excel deserializado no es del tipo esperado.");
            }
        } catch (Exception e) {
            System.err.println("ERROR deserializing Excel file: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Error al deserializar el archivo Excel desde la base de datos: " + e.getMessage(), e);
        }
        
        // Validar que el archivo sea un Excel válido (solo verifica la firma ZIP, no intenta abrirlo)
        if (!hasValidZipSignature(excelFileBytes)) {
            System.err.println("ERROR: El archivo no tiene la firma ZIP válida de un archivo .xlsx");
            System.err.println("First 20 bytes (hex): " + bytesToHex(excelFileBytes, 20));
            throw new IllegalStateException("El archivo Excel almacenado no es válido. El archivo debe ser un .xlsx válido. Por favor, verifique y re-suba el archivo correcto.");
        }

        // Obtener los detalles del reporte
        System.out.println("Fetching detalles for idReporte: " + idReporte);
        List<DetalleReporte> detalles;
        try {
            detalles = detalleReporteRepository.findByReporteId(idReporte);
            System.out.println("Found " + detalles.size() + " detalles");
        } catch (Exception e) {
            System.err.println("ERROR fetching detalles: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error fetching report details: " + e.getMessage(), e);
        }

        // Convertir detalles a formato HashMap para compatibilidad con el código original
        ArrayList<HashMap<String, Object>> details = new ArrayList<>();
        for (DetalleReporte detalle : detalles) {
            System.out.println("Processing detalle id: " + detalle.getId() + ", cell: " + detalle.getCell() + ", rowMode: " + detalle.getRowMode());
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("id", detalle.getId());
            detailMap.put("idReporte", detalle.getIdReporte());
            detailMap.put("sqlQuery", detalle.getSqlQuery());
            detailMap.put("cell", detalle.getCell());
            detailMap.put("descripcion", detalle.getDescripcion());
            detailMap.put("rowMode", detalle.getRowMode());
            details.add(detailMap);
        }

        System.out.println("Total details converted: " + details.size());
        System.out.println("Calling writeReportToBytes...");

        // Generar el Excel
        try {
            byte[] result = writeReportToBytes(excelFileBytes, details);
            System.out.println("writeReportToBytes completed, result size: " + result.length + " bytes");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in writeReportToBytes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private byte[] writeReportToBytes(byte[] excelTemplate, ArrayList<HashMap<String, Object>> details) throws Exception {
        System.out.println("writeReportToBytes: template size=" + excelTemplate.length + ", details count=" + details.size());
        try {
            System.out.println("Calling writeReport...");
            XSSFWorkbook book = writeReport(excelTemplate, details);
            System.out.println("writeReport completed, writing to ByteArrayOutputStream...");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            book.write(bo);
            byte[] result = bo.toByteArray();
            System.out.println("ByteArrayOutputStream written, result size: " + result.length + " bytes");
            book.close();
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in writeReportToBytes: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private XSSFWorkbook writeReport(byte[] excelTemplate, ArrayList<HashMap<String, Object>> details) throws Exception {
        System.out.println("writeReport: Creating XSSFWorkbook from template...");
        XSSFWorkbook book;
        XSSFSheet sheet;
        try {
            book = new XSSFWorkbook(new ByteArrayInputStream(excelTemplate));
            System.out.println("XSSFWorkbook created, getting sheet 0...");
            sheet = book.getSheetAt(0);
            System.out.println("Sheet obtained: " + sheet.getSheetName());
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            System.err.println("ERROR: El archivo Excel no es válido o está corrupto.");
            System.err.println("File size: " + excelTemplate.length + " bytes");
            System.err.println("First 20 bytes (hex): " + bytesToHex(excelTemplate, 20));
            System.err.println("Error details: " + e.getMessage());
            throw new IllegalStateException("El archivo Excel almacenado en la base de datos no es válido o está corrupto. Por favor, verifique que el archivo sea un .xlsx válido y re-súbalo si es necesario.", e);
        } catch (Exception e) {
            System.err.println("ERROR creating XSSFWorkbook: " + e.getClass().getName() + ": " + e.getMessage());
            System.err.println("File size: " + excelTemplate.length + " bytes");
            System.err.println("First 20 bytes (hex): " + bytesToHex(excelTemplate, 20));
            e.printStackTrace();
            throw new IllegalStateException("Error al procesar el archivo Excel: " + e.getMessage(), e);
        }

        if (details.isEmpty()) {
            System.out.println("Details is empty, returning book as-is");
            return book;
        }

        System.out.println("Details not empty, checking rowMode...");
        Boolean rowMode = details.get(0).get("rowMode") != null && (Boolean) details.get(0).get("rowMode");
        System.out.println("rowMode: " + rowMode);

        if (rowMode) {
            // Modo fila: ejecutar query masiva y llenar múltiples filas
            System.out.println("Row mode: executing masive SQL query...");
            String sqlQuery = (String) details.get(0).get("sqlQuery");
            System.out.println("SQL Query: " + sqlQuery);
            ArrayList<HashMap<String, Object>> results = executeMasiveSql(sqlQuery);
            System.out.println("SQL query executed, got " + results.size() + " results");
            for (int r = 0; r < results.size(); r++) {
                HashMap<String, Object> hs = results.get(r);
                for (int i = 1; i < details.size(); i++) {
                    HashMap<String, Object> item = details.get(i);
                    String cell = (String) item.get("cell");
                    String sql = (String) item.get("sqlQuery");
                    int[] coord = ExcelUtil.getCoordenadas(cell);
                    
                    if (r > 0) {
                        int _r = coord[1] + r;
                        if (i == 1) {
                            sheet.createRow(_r);
                        }
                        if (sheet.getRow(_r) != null && sheet.getRow(coord[1]) != null 
                                && sheet.getRow(coord[1]).getCell(coord[0]) != null) {
                            sheet.getRow(_r).createCell(coord[0])
                                    .setCellStyle(sheet.getRow(coord[1]).getCell(coord[0]).getCellStyle());
                            coord[1] = _r;
                        }
                    }
                    
                    if (sheet.getRow(coord[1]) != null && sheet.getRow(coord[1]).getCell(coord[0]) != null) {
                        // En modo rowMode, sql contiene la referencia a la columna como {1}, {2}, etc.
                        Object value = sql != null ? hs.get(sql.trim()) : null;
                        setValue(sheet, coord, value);
                    }
                }
            }
            
            // Actualizar rango de tabla si existe
            if (sheet.getTables().size() > 0) {
                XSSFTable table = sheet.getTables().get(0);
                String newRange = String.format("%s%d:%s%d",
                        ExcelUtil.getColumn(table.getStartColIndex()),
                        table.getStartRowIndex() + 1,
                        ExcelUtil.getColumn(table.getEndColIndex()),
                        table.getStartRowIndex() + results.size() + 1);
                table.getCTTable().setRef(newRange);
            }
        } else {
            // Modo celda: ejecutar queries individuales
            System.out.println("Cell mode: executing individual SQL queries...");
            for (HashMap<String, Object> item : details) {
                String cell = (String) item.get("cell");
                if (cell == null) {
                    System.out.println("Skipping item with null cell");
                    continue;
                }
                
                System.out.println("Processing cell: " + cell);
                int[] coord = ExcelUtil.getCoordenadas(cell);
                String sql = ((String) item.get("sqlQuery")).trim();
                System.out.println("SQL: " + sql);
                
                Object value = sql.equals("-1") ? -1 : executeSql(sql);
                System.out.println("SQL result: " + value);
                setValue(sheet, coord, value);
            }
            System.out.println("Cell mode processing completed");
        }
        
        System.out.println("writeReport completed successfully");
        return book;
    }
    
    private String bytesToHex(byte[] bytes, int length) {
        int len = Math.min(length, bytes.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
    
    /**
     * Verifica que el archivo tenga la firma ZIP válida (primeros 2 bytes deben ser 0x50 0x4B)
     * No intenta abrir el archivo, solo verifica la firma
     */
    private boolean hasValidZipSignature(byte[] fileData) {
        if (fileData == null || fileData.length < 2) {
            return false;
        }
        
        // Un archivo .xlsx es un archivo ZIP, debe comenzar con "PK" (0x50 0x4B)
        return fileData[0] == 0x50 && fileData[1] == 0x4B;
    }

    private Object executeSql(String sql) {
        System.out.println("executeSql: " + sql);
        try {
            Query query = entityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();
            System.out.println("executeSql result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in executeSql: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private ArrayList<HashMap<String, Object>> executeMasiveSql(String sql) {
        System.out.println("executeMasiveSql: " + sql);
        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = query.getResultList();
            System.out.println("executeMasiveSql: got " + results.size() + " rows");
            
            ArrayList<HashMap<String, Object>> data = new ArrayList<>();
            for (int rowIdx = 0; rowIdx < results.size(); rowIdx++) {
                Object[] row = results.get(rowIdx);
                HashMap<String, Object> rowMap = new HashMap<>();
                int columnCount = row.length;
                System.out.println("Row " + rowIdx + ": " + columnCount + " columns");
                for (int i = 0; i < columnCount; i++) {
                    // Usar formato {1}, {2}, etc. como en el código original
                    String key = "{" + (i + 1) + "}";
                    rowMap.put(key, row[i]);
                }
                data.add(rowMap);
            }
            System.out.println("executeMasiveSql: processed " + data.size() + " rows");
            return data;
        } catch (Exception e) {
            System.err.println("ERROR in executeMasiveSql: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void setValue(XSSFSheet sheet, int[] coord, Object value) {
        if (sheet.getRow(coord[1]) == null || sheet.getRow(coord[1]).getCell(coord[0]) == null) {
            return;
        }
        
        if (value == null) {
            return;
        }
        
        if (value instanceof Double) {
            sheet.getRow(coord[1]).getCell(coord[0]).setCellValue((Double) value);
        } else if (value instanceof BigDecimal) {
            sheet.getRow(coord[1]).getCell(coord[0]).setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Integer) {
            sheet.getRow(coord[1]).getCell(coord[0]).setCellValue((Integer) value);
        } else if (value instanceof Long) {
            sheet.getRow(coord[1]).getCell(coord[0]).setCellValue((Long) value);
        } else {
            sheet.getRow(coord[1]).getCell(coord[0]).setCellValue(value.toString());
        }
    }
    
}
