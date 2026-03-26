import { Injectable } from '@angular/core';

/**
 * Servicio utilitario para calcular el progressReport (período) basado en la fecha de inicio
 */
@Injectable({
  providedIn: 'root'
})
export class ProgressReportService {
  
  /**
   * Calcula el progressReport basado en la fecha de inicio
   * 
   * @param fechaInicio Fecha de inicio en formato string (YYYY-MM-DD)
   * @returns String del período ("1"-"5") o undefined si la fecha es inválida
   * 
   * Lógica:
   * - Period 1: fecha <= 2022-07-31
   * - Period 2: fecha <= 2023-07-31
   * - Period 3: fecha <= 2024-07-31
   * - Period 4: fecha <= 2025-07-31
   * - Period 5: fecha > 2025-07-31
   */
  calculateProgressReport(fechaInicio: string | null | undefined): string | undefined {
    if (!fechaInicio || fechaInicio.trim() === '') {
      return undefined;
    }
    
    try {
      const date = new Date(fechaInicio);
      const cutoff1 = new Date('2022-07-31');
      const cutoff2 = new Date('2023-07-31');
      const cutoff3 = new Date('2024-07-31');
      const cutoff4 = new Date('2025-07-31');
      
      // Comparar solo la fecha (sin hora)
      const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
      const cutoff1Only = new Date(cutoff1.getFullYear(), cutoff1.getMonth(), cutoff1.getDate());
      const cutoff2Only = new Date(cutoff2.getFullYear(), cutoff2.getMonth(), cutoff2.getDate());
      const cutoff3Only = new Date(cutoff3.getFullYear(), cutoff3.getMonth(), cutoff3.getDate());
      const cutoff4Only = new Date(cutoff4.getFullYear(), cutoff4.getMonth(), cutoff4.getDate());
      
      if (dateOnly <= cutoff1Only) {
        return '1';
      } else if (dateOnly <= cutoff2Only) {
        return '2';
      } else if (dateOnly <= cutoff3Only) {
        return '3';
      } else if (dateOnly <= cutoff4Only) {
        return '4';
      } else {
        return '5';
      }
    } catch (e) {
      console.error('Error calculating progressReport:', e);
      return undefined;
    }
  }
}
