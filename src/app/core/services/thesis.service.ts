import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { TesisDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ThesisService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las tesis con paginación
   */
  getThesis(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<TesisDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<TesisDTO>>('/thesis', filters);
  }

  /**
   * Obtiene una tesis específica por ID
   */
  getThesisById(id: number): Observable<TesisDTO> {
    return this.baseHttp.get<TesisDTO>(`/thesis/${id}`);
  }

  /**
   * Crea una nueva tesis
   */
  createThesis(thesis: TesisDTO): Observable<TesisDTO> {
    return this.baseHttp.post<TesisDTO>('/thesis', thesis);
  }

  /**
   * Actualiza una tesis existente
   */
  updateThesis(id: number, thesis: TesisDTO): Observable<TesisDTO> {
    return this.baseHttp.put<TesisDTO>(`/thesis/${id}`, thesis);
  }

  /**
   * Elimina una tesis
   */
  deleteThesis(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/thesis/${id}`);
  }
}










