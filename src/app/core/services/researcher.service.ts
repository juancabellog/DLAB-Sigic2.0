import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { RRHHDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ResearcherService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todos los investigadores con paginación
   */
  getResearchers(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<RRHHDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<RRHHDTO>>('/researchers', filters);
  }

  /**
   * Obtiene un investigador específico por ID
   */
  getResearcher(id: number): Observable<RRHHDTO> {
    return this.baseHttp.get<RRHHDTO>(`/researchers/${id}`);
  }

  /**
   * Crea un nuevo investigador
   */
  createResearcher(researcher: any): Observable<RRHHDTO> {
    return this.baseHttp.post<RRHHDTO>('/researchers', researcher);
  }
  
  /**
   * Obtiene los tipos de investigadores disponibles
   */
  getResearcherTypes(): Observable<any[]> {
    return this.baseHttp.get<any[]>('/researchers/types');
  }

  /**
   * Actualiza un investigador existente
   */
  updateResearcher(id: number, researcher: RRHHDTO): Observable<RRHHDTO> {
    return this.baseHttp.put<RRHHDTO>(`/researchers/${id}`, researcher);
  }

  /**
   * Elimina un investigador
   */
  deleteResearcher(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/researchers/${id}`);
  }

  /**
   * Busca investigadores por término
   */
  searchResearchers(searchTerm: string): Observable<RRHHDTO[]> {
    return this.baseHttp.get<RRHHDTO[]>(`/researchers/search?q=${encodeURIComponent(searchTerm)}`);
  }

  /**
   * Busca el mejor match de investigador usando el servicio avanzado de matching.
   * Devuelve a lo más un RRHHDTO.
   */
  matchResearcherByName(name: string): Observable<RRHHDTO[]> {
    return this.baseHttp.get<RRHHDTO[]>(`/researchers/match?name=${encodeURIComponent(name)}`);
  }

  /**
   * Obtiene estadísticas de investigadores
   */
  getResearcherStats(): Observable<any> {
    return this.baseHttp.get<any>('/researchers/stats');
  }
}