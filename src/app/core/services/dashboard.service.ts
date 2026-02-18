import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';

export interface ImpactMetricsUpdate {
  lastUpdate: string | null;
  status: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene la fecha de última actualización de los factores de impacto
   */
  getImpactMetricsUpdate(): Observable<ImpactMetricsUpdate> {
    return this.baseHttp.get<ImpactMetricsUpdate>('/dashboard/impact-metrics-update');
  }
}
