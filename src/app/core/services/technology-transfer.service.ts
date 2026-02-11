import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { TransferenciaTecnologicaDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class TechnologyTransferService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todas las transferencias tecnológicas con paginación
   */
  getTechnologyTransfers(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<TransferenciaTecnologicaDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<TransferenciaTecnologicaDTO>>('/technology-transfer', filters);
  }

  /**
   * Obtiene una transferencia tecnológica específica por ID
   */
  getTechnologyTransfer(id: number): Observable<TransferenciaTecnologicaDTO> {
    return this.baseHttp.get<TransferenciaTecnologicaDTO>(`/technology-transfer/${id}`);
  }

  /**
   * Crea una nueva transferencia tecnológica
   */
  createTechnologyTransfer(transfer: TransferenciaTecnologicaDTO): Observable<TransferenciaTecnologicaDTO> {
    return this.baseHttp.post<TransferenciaTecnologicaDTO>('/technology-transfer', transfer);
  }

  /**
   * Actualiza una transferencia tecnológica existente
   */
  updateTechnologyTransfer(id: number, transfer: TransferenciaTecnologicaDTO): Observable<TransferenciaTecnologicaDTO> {
    return this.baseHttp.put<TransferenciaTecnologicaDTO>(`/technology-transfer/${id}`, transfer);
  }

  /**
   * Elimina una transferencia tecnológica
   */
  deleteTechnologyTransfer(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/technology-transfer/${id}`);
  }
}










