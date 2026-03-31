import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { PublicacionDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class PublicationService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las publicaciones con paginación
   */
  getPublications(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<PublicacionDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<PublicacionDTO>>('/publications', filters);
  }

  /**
   * Obtiene una publicación específica por ID
   */
  getPublication(id: number): Observable<PublicacionDTO> {
    return this.baseHttp.get<PublicacionDTO>(`/publications/${id}`);
  }

  /**
   * Crea una nueva publicación
   */
  createPublication(publication: PublicacionDTO): Observable<PublicacionDTO> {
    return this.baseHttp.post<PublicacionDTO>('/publications', publication);
  }

  /**
   * Actualiza una publicación existente
   */
  updatePublication(id: number, publication: PublicacionDTO): Observable<PublicacionDTO> {
    return this.baseHttp.put<PublicacionDTO>(`/publications/${id}`, publication);
  }

  /**
   * Elimina una publicación
   */
  deletePublication(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/publications/${id}`);
  }

  /**
   * Busca publicaciones por DOI
   */
  searchByDOI(doi: string): Observable<PublicacionDTO | null> {
    return this.baseHttp.get<PublicacionDTO>(`/publications/search/doi/${encodeURIComponent(doi)}`);
  }

  /**
   * Obtiene publicaciones por año
   */
  getPublicationsByYear(year: number): Observable<PublicacionDTO[]> {
    return this.baseHttp.get<PublicacionDTO[]>(`/publications/year/${year}`);
  }

  /**
   * Obtiene publicaciones por investigador
   */
  getPublicationsByResearcher(researcherId: number): Observable<PublicacionDTO[]> {
    return this.baseHttp.get<PublicacionDTO[]>(`/publications/researcher/${researcherId}`);
  }

  /**
   * Obtiene publicaciones por proyecto
   */
  getPublicationsByProject(projectId: number): Observable<PublicacionDTO[]> {
    return this.baseHttp.get<PublicacionDTO[]>(`/publications/project/${projectId}`);
  }

  /**
   * Obtiene estadísticas de publicaciones
   */
  getPublicationStats(): Observable<any> {
    return this.baseHttp.get<any>('/publications/stats');
  }

  /**
   * Exporta publicaciones a diferentes formatos
   */
  exportPublications(format: 'excel' | 'pdf' | 'csv', filters?: SearchFiltersDTO): Observable<Blob> {
    const params = { ...filters, format };
    return this.baseHttp.get<Blob>('/publications/export', params);
  }

  /**
   * Importa publicaciones desde un archivo
   */
  importPublications(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    
    return this.baseHttp.post<any>('/publications/import', formData);
  }

  /**
   * Valida un DOI
   */
  validateDOI(doi: string): Observable<{ valid: boolean; message?: string }> {
    return this.baseHttp.get<{ valid: boolean; message?: string }>(`/publications/validate-doi/${encodeURIComponent(doi)}`);
  }

  /**
   * Obtiene información de una publicación desde un DOI externo
   */
  getPublicationInfoFromDOI(doi: string): Observable<any> {
    return this.baseHttp.get<any>(`/publications/doi-info/${encodeURIComponent(doi)}`);
  }

  /**
   * Obtiene los factores de impacto de un journal para un año específico
   */
  getImpactFactors(journalId: number, year: number): Observable<{ factorImpacto: number; factorImpactoPromedio: number }> {
    return this.baseHttp.get<{ factorImpacto: number; factorImpactoPromedio: number }>(
      `/publications/impact-factors?journalId=${journalId}&year=${year}`
    );
  }

  /**
   * Exporta publicaciones visibles a Excel
   */
  exportPublicationsToExcel(params: { sort: string; direction: 'ASC' | 'DESC' }): Observable<Blob> {
    const { sort, direction } = params;
    return this.baseHttp.getFile(`/publications/export?sortBy=${sort}&sortDir=${direction}`);
  }

  /**
   * Obtiene el preview de una publicación desde OpenAlex usando su DOI
   */
  getPublicationPreview(doi: string): Observable<any> {
    // La respuesta puede ser:
    // - { exists: true, publicationId: number, title: string, journal: string, year: number }
    // - { exists: false, preview: PublicationPreviewDTO }
    return this.baseHttp.get<any>(
      `/publications/preview?doi=${encodeURIComponent(doi)}`
    );
  }

  /**
   * Importa una publicación desde DOI con las decisiones del usuario
   */
  importPublicationFromDoi(request: import('../models/backend-dtos').PublicationImportRequestDTO): Observable<PublicacionDTO> {
    return this.baseHttp.post<PublicacionDTO>('/publications/import-from-doi', request);
  }
}



