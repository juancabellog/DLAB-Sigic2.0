import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { BecariosPostdoctoralesDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class PostdoctoralFellowsService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todos los becarios postdoctorales con paginación
   */
  getPostdoctoralFellows(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<BecariosPostdoctoralesDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<BecariosPostdoctoralesDTO>>('/postdoctoral-fellows', filters);
  }

  /**
   * Obtiene un becario postdoctoral específico por ID
   */
  getPostdoctoralFellow(id: number): Observable<BecariosPostdoctoralesDTO> {
    return this.baseHttp.get<BecariosPostdoctoralesDTO>(`/postdoctoral-fellows/${id}`);
  }

  /**
   * Crea un nuevo becario postdoctoral
   */
  createPostdoctoralFellow(fellow: BecariosPostdoctoralesDTO): Observable<BecariosPostdoctoralesDTO> {
    return this.baseHttp.post<BecariosPostdoctoralesDTO>('/postdoctoral-fellows', fellow);
  }

  /**
   * Actualiza un becario postdoctoral existente
   */
  updatePostdoctoralFellow(id: number, fellow: BecariosPostdoctoralesDTO): Observable<BecariosPostdoctoralesDTO> {
    return this.baseHttp.put<BecariosPostdoctoralesDTO>(`/postdoctoral-fellows/${id}`, fellow);
  }

  /**
   * Elimina un becario postdoctoral
   */
  deletePostdoctoralFellow(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/postdoctoral-fellows/${id}`);
  }

  /**
   * Exporta los becarios postdoctorales visibles a Excel
   */
  exportPostdoctoralFellowsToExcel(params: { sort: string; direction: 'ASC' | 'DESC' }): Observable<Blob> {
    const { sort, direction } = params;
    return this.baseHttp.getFile(`/postdoctoral-fellows/export?sortBy=${sort}&sortDir=${direction}`);
  }
}










