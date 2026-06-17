import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { PaginatedResponseDTO, ParticipacionEventoCientificoDTO, SearchFiltersDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class ParticipationScientificEventsService {
  constructor(private baseHttp: BaseHttpService) {}

  getParticipationScientificEvents(
    filters?: SearchFiltersDTO
  ): Observable<PaginatedResponseDTO<ParticipacionEventoCientificoDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<ParticipacionEventoCientificoDTO>>(
      '/participation-scientific-events',
      filters
    );
  }

  getParticipationScientificEvent(id: number): Observable<ParticipacionEventoCientificoDTO> {
    return this.baseHttp.get<ParticipacionEventoCientificoDTO>(`/participation-scientific-events/${id}`);
  }

  createParticipationScientificEvent(item: ParticipacionEventoCientificoDTO): Observable<ParticipacionEventoCientificoDTO> {
    return this.baseHttp.post<ParticipacionEventoCientificoDTO>('/participation-scientific-events', item);
  }

  updateParticipationScientificEvent(id: number, item: ParticipacionEventoCientificoDTO): Observable<ParticipacionEventoCientificoDTO> {
    return this.baseHttp.put<ParticipacionEventoCientificoDTO>(`/participation-scientific-events/${id}`, item);
  }

  deleteParticipationScientificEvent(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/participation-scientific-events/${id}`);
  }

  /**
   * Exporta las participaciones en eventos científicos visibles a Excel
   */
  exportParticipationScientificEventsToExcel(params: {
    sort: string;
    direction: 'ASC' | 'DESC';
  }): Observable<Blob> {
    const { sort, direction } = params;
    return this.baseHttp.getFile(
      `/participation-scientific-events/export?sortBy=${sort}&sortDir=${direction}`
    );
  }
}
