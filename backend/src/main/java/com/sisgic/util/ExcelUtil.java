package com.sisgic.util;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;

public class ExcelUtil {
    
    private static final int AZ = 'Z' - 'A' + 1;
    
    public static Object getObject(XSSFCell cell) throws Exception {
        return getObject(cell, cell == null ? null : cell.getCellType());
    }
    
    public static Object getObject(XSSFCell cell, CellType type) throws Exception {
        if (cell == null) {
            return null;
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    return getObject(cellValue, cellValue.getCellType());
                } catch (Exception e) {
                    return getObject(cell, cell.getCachedFormulaResultType());
                }
            case NUMERIC:
                if (cell.getCellStyle() != null && cell.getCellStyle().getDataFormatString() != null) {
                    String format = cell.getCellStyle().getDataFormatString().toLowerCase();
                    if (format.endsWith("hh:mm:ss") || format.endsWith("hh:mm") 
                            || format.endsWith("mmm/yyyy") || format.endsWith("m/d/yy") 
                            || format.endsWith("mmm-yy")) {
                        return cell.getDateCellValue();
                    }
                }
                return cell.getNumericCellValue();
            default:
                return null;
        }
    }
    
    public static Object getObject(CellValue cell, CellType type) throws Exception {
        if (cell == null) {
            return null;
        }
        switch (type) {
            case STRING:
                return cell.getStringValue();
            case BOOLEAN:
                return cell.getBooleanValue();
            case NUMERIC:
                return cell.getNumberValue();
            default:
                return null;
        }
    }
    
    public static String getColumn(int index) {
        String result;
        int div = 'Z' - 'A';
        if (index > div) {
            int n = index / (div + 1) + 'A' - 1;
            int d = index % (div + 1) + 'A';
            result = String.valueOf((char) n) + (char) d;
        } else {
            result = String.valueOf((char) (index + 'A'));
        }
        return result;
    }
    
    public static String getCoordenadas(int col, int row) {
        return getColumn(col) + (row + 1);
    }
    
    public static int[] getCoordenadas(String data) throws Exception {
        int col, row;
        char c;
        if (data.length() == 1 || (c = data.charAt(1)) >= '0' && c <= '9') {
            col = data.charAt(0) - 'A';
            row = data.length() == 1 ? -1 : Integer.parseInt(data.substring(1)) - 1;
        } else {
            col = AZ * (data.charAt(0) - 'A' + 1) + data.charAt(1) - 'A';
            row = data.length() == 2 ? -1 : Integer.parseInt(data.substring(2)) - 1;
        }
        if (col < 0) {
            throw new Exception("Coordenadas invalidas: " + data);
        }
        return new int[]{col, row};
    }
}
