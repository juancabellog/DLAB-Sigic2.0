import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { AwardProductDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class AwardService {

  constructor(private baseHttp: BaseHttpService) {}

  getAwards(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<AwardProductDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<AwardProductDTO>>('/awards', filters);
  }

  getAward(id: number): Observable<AwardProductDTO> {
    return this.baseHttp.get<AwardProductDTO>(`/awards/${id}`);
  }

  createAward(award: AwardProductDTO): Observable<AwardProductDTO> {
    return this.baseHttp.post<AwardProductDTO>('/awards', award);
  }

  updateAward(id: number, award: AwardProductDTO): Observable<AwardProductDTO> {
    return this.baseHttp.put<AwardProductDTO>(`/awards/${id}`, award);
  }

  deleteAward(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/awards/${id}`);
  }
}
