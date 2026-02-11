import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UtilsService {

  /**
   * Procesa el linkPDF para construir la URL correcta del archivo PDF
   * Si el linkPDF comienza con "PDF:", extrae la parte después del prefijo
   * y construye la URL completa apuntando al backend
   * 
   * @param linkPDF El linkPDF del producto científico (puede ser "PDF:pdfs/archivo.pdf" o una URL completa)
   * @returns La URL completa del PDF o null si linkPDF es nulo/vacío
   */
  getPdfUrl(linkPDF: string | null | undefined): string | null {
    if (!linkPDF || linkPDF.trim() === '') {
      return null;
    }

    // Si comienza con "PDF:", extraer la parte después del prefijo
    if (linkPDF.startsWith('PDF:')) {
      const pdfPath = linkPDF.substring(4); // Extraer después de "PDF:"
      // Construir la URL completa apuntando al backend
      // El apiUrl es "http://localhost:8081/sigic2.0/api", necesitamos quitar "/api"
      const baseUrl = environment.apiUrl.replace('/api', '');
      return `${baseUrl}/${pdfPath}`;
    }

    // Si ya es una URL completa, retornarla tal cual
    return linkPDF;
  }
}









