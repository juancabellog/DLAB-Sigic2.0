import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { OrganizacionEventosCientificosDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ScientificEventsService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las organizaciones de eventos científicos con paginación
   */
  getScientificEvents(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<OrganizacionEventosCientificosDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<OrganizacionEventosCientificosDTO>>('/scientific-events', filters);
  }

  /**
   * Obtiene una organización de evento científico específica por ID
   */
  getScientificEvent(id: number): Observable<OrganizacionEventosCientificosDTO> {
    return this.baseHttp.get<OrganizacionEventosCientificosDTO>(`/scientific-events/${id}`);
  }

  /**
   * Crea una nueva organización de evento científico
   */
  createScientificEvent(event: OrganizacionEventosCientificosDTO): Observable<OrganizacionEventosCientificosDTO> {
    return this.baseHttp.post<OrganizacionEventosCientificosDTO>('/scientific-events', event);
  }

  /**
   * Actualiza una organización de evento científico existente
   */
  updateScientificEvent(id: number, event: OrganizacionEventosCientificosDTO): Observable<OrganizacionEventosCientificosDTO> {
    return this.baseHttp.put<OrganizacionEventosCientificosDTO>(`/scientific-events/${id}`, event);
  }

  /**
   * Elimina una organización de evento científico
   */
  deleteScientificEvent(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/scientific-events/${id}`);
  }

  /**
   * Exporta los eventos científicos visibles a Excel
   */
  exportScientificEventsToExcel(params: { sort: string; direction: 'ASC' | 'DESC' }): Observable<Blob> {
    const { sort, direction } = params;
    return this.baseHttp.getFile(`/scientific-events/export?sortBy=${sort}&sortDir=${direction}`);
  }
}










