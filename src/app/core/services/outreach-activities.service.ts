import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { DifusionDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class OutreachActivitiesService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las actividades de difusión con paginación
   */
  getOutreachActivities(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<DifusionDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<DifusionDTO>>('/outreach-activities', filters);
  }

  /**
   * Obtiene una actividad de difusión específica por ID
   */
  getOutreachActivity(id: number): Observable<DifusionDTO> {
    return this.baseHttp.get<DifusionDTO>(`/outreach-activities/${id}`);
  }

  /**
   * Crea una nueva actividad de difusión
   */
  createOutreachActivity(activity: DifusionDTO): Observable<DifusionDTO> {
    return this.baseHttp.post<DifusionDTO>('/outreach-activities', activity);
  }

  /**
   * Actualiza una actividad de difusión existente
   */
  updateOutreachActivity(id: number, activity: DifusionDTO): Observable<DifusionDTO> {
    return this.baseHttp.put<DifusionDTO>(`/outreach-activities/${id}`, activity);
  }

  /**
   * Elimina una actividad de difusión
   */
  deleteOutreachActivity(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/outreach-activities/${id}`);
  }
}










