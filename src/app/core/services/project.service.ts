import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { ProjectProductDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {

  constructor(private baseHttp: BaseHttpService) {}

  getProjects(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<ProjectProductDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<ProjectProductDTO>>('/projects', filters);
  }

  getProject(id: number): Observable<ProjectProductDTO> {
    return this.baseHttp.get<ProjectProductDTO>(`/projects/${id}`);
  }

  createProject(project: ProjectProductDTO): Observable<ProjectProductDTO> {
    return this.baseHttp.post<ProjectProductDTO>('/projects', project);
  }

  updateProject(id: number, project: ProjectProductDTO): Observable<ProjectProductDTO> {
    return this.baseHttp.put<ProjectProductDTO>(`/projects/${id}`, project);
  }

  deleteProject(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/projects/${id}`);
  }
}
