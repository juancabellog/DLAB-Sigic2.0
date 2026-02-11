import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { ColaboracionDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ScientificCollaborationsService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las colaboraciones científicas con paginación
   */
  getScientificCollaborations(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<ColaboracionDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<ColaboracionDTO>>('/scientific-collaborations', filters);
  }

  /**
   * Obtiene una colaboración científica específica por ID
   */
  getScientificCollaboration(id: number): Observable<ColaboracionDTO> {
    return this.baseHttp.get<ColaboracionDTO>(`/scientific-collaborations/${id}`);
  }

  /**
   * Crea una nueva colaboración científica
   */
  createScientificCollaboration(collaboration: ColaboracionDTO): Observable<ColaboracionDTO> {
    return this.baseHttp.post<ColaboracionDTO>('/scientific-collaborations', collaboration);
  }

  /**
   * Actualiza una colaboración científica existente
   */
  updateScientificCollaboration(id: number, collaboration: ColaboracionDTO): Observable<ColaboracionDTO> {
    return this.baseHttp.put<ColaboracionDTO>(`/scientific-collaborations/${id}`, collaboration);
  }

  /**
   * Elimina una colaboración científica
   */
  deleteScientificCollaboration(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/scientific-collaborations/${id}`);
  }
}










