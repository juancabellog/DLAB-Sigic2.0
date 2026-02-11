import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { ProyectoDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todos los proyectos con paginación
   */
  getProjects(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<ProyectoDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<ProyectoDTO>>('/projects', filters);
  }

  /**
   * Obtiene un proyecto específico por código
   */
  getProject(codigo: string): Observable<ProyectoDTO> {
    return this.baseHttp.get<ProyectoDTO>(`/projects/${codigo}`);
  }

  /**
   * Crea un nuevo proyecto
   */
  createProject(project: ProyectoDTO): Observable<ProyectoDTO> {
    return this.baseHttp.post<ProyectoDTO>('/projects', project);
  }

  /**
   * Actualiza un proyecto existente
   */
  updateProject(codigo: string, project: ProyectoDTO): Observable<ProyectoDTO> {
    return this.baseHttp.put<ProyectoDTO>(`/projects/${codigo}`, project);
  }

  /**
   * Elimina un proyecto
   */
  deleteProject(codigo: string): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/projects/${codigo}`);
  }

  /**
   * Busca proyectos por nombre o código
   */
  searchProjects(query: string): Observable<ProyectoDTO[]> {
    return this.baseHttp.get<ProyectoDTO[]>(`/projects/search?q=${encodeURIComponent(query)}`);
  }

  /**
   * Obtiene proyectos por investigador principal
   */
  getProjectsByPrincipalInvestigator(researcherId: number): Observable<ProyectoDTO[]> {
    return this.baseHttp.get<ProyectoDTO[]>(`/projects/principal-investigator/${researcherId}`);
  }

  /**
   * Obtiene proyectos por estado
   */
  getProjectsByStatus(status: string): Observable<ProyectoDTO[]> {
    return this.baseHttp.get<ProyectoDTO[]>(`/projects/status/${status}`);
  }

  /**
   * Obtiene proyectos activos
   */
  getActiveProjects(): Observable<ProyectoDTO[]> {
    return this.baseHttp.get<ProyectoDTO[]>('/projects/active');
  }

  /**
   * Obtiene proyectos por rango de fechas
   */
  getProjectsByDateRange(startDate: string, endDate: string): Observable<ProyectoDTO[]> {
    return this.baseHttp.get<ProyectoDTO[]>(`/projects/date-range?start=${startDate}&end=${endDate}`);
  }

  /**
   * Obtiene estadísticas de proyectos
   */
  getProjectStats(): Observable<any> {
    return this.baseHttp.get<any>('/projects/stats');
  }

  /**
   * Exporta proyectos a diferentes formatos
   */
  exportProjects(format: 'excel' | 'pdf' | 'csv', filters?: SearchFiltersDTO): Observable<Blob> {
    const params = { ...filters, format };
    return this.baseHttp.get<Blob>('/projects/export', params);
  }

  /**
   * Importa proyectos desde un archivo
   */
  importProjects(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    
    return this.baseHttp.post<any>('/projects/import', formData);
  }

  /**
   * Valida un código de proyecto
   */
  validateProjectCode(code: string): Observable<{ valid: boolean; message?: string }> {
    return this.baseHttp.get<{ valid: boolean; message?: string }>(`/projects/validate-code/${encodeURIComponent(code)}`);
  }

  /**
   * Obtiene el resumen de un proyecto
   */
  getProjectSummary(id: number): Observable<any> {
    return this.baseHttp.get<any>(`/projects/${id}/summary`);
  }

  /**
   * Actualiza el estado de un proyecto
   */
  updateProjectStatus(id: number, status: string): Observable<ProyectoDTO> {
    return this.baseHttp.patch<ProyectoDTO>(`/projects/${id}/status`, { status });
  }

  /**
   * Obtiene los productos asociados a un proyecto
   */
  getProjectProducts(id: number): Observable<any[]> {
    return this.baseHttp.get<any[]>(`/projects/${id}/products`);
  }

  /**
   * Asocia un producto a un proyecto
   */
  associateProductToProject(projectId: number, productId: number, percentage: number): Observable<any> {
    return this.baseHttp.post<any>(`/projects/${projectId}/products`, { productId, percentage });
  }

  /**
   * Desasocia un producto de un proyecto
   */
  disassociateProductFromProject(projectId: number, productId: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/projects/${projectId}/products/${productId}`);
  }
}

