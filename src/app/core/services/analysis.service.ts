import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';

export interface GraphQueryRequest {
  type: number; // 1: RRHH, 3: Línea Investigación, 4: Cluster Productos, 5: Cluster Productos 2
  periods?: number[];
  rrhhTypes?: number[];
}

export interface GraphNode {
  id: number;
  _id?: number;
  name: string;
  category?: number;
  _category?: any;
  initials?: string;
  tiposRRHH?: number[];
  idArea?: number;
  descripcion?: string;
  hindex?: number;
  citations?: number;
  symbolSize?: number | number[];
  itemStyle?: any;
  label?: any;
  opacity?: number;
}

export interface GraphLink {
  source: number;
  target: number;
  weight?: number;
  lineStyle?: any;
}

export interface GraphCategory {
  id: number;
  name: string;
  descripcion?: string;
  color?: string;
  itemStyle?: any;
}

export interface GraphResponse {
  nodes: GraphNode[];
  links: GraphLink[];
  categories?: GraphCategory[];
}

export interface PublicationQueryRequest {
  filter: {
    type: number;
    periods: number[];
    from: number;
    to: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AnalysisService {

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Consulta un grafo según el tipo especificado
   */
  queryGraph(request: GraphQueryRequest): Observable<GraphResponse> {
    return this.baseHttp.post<GraphResponse>('/analysis/graph-query', request);
  }

  /**
   * Consulta publicaciones relacionadas a un enlace del grafo
   */
  queryPublications(request: PublicationQueryRequest): Observable<any[]> {
    return this.baseHttp.post<any[]>('/analysis/publication-query', request);
  }

  /**
   * Obtiene los períodos disponibles
   */
  getPeriods(): Observable<any[]> {
    return this.baseHttp.get<any[]>('/catalogs/periods');
  }

  /**
   * Obtiene los tipos de RRHH disponibles para filtros
   */
  getRRHHTypes(): Observable<any[]> {
    return this.baseHttp.get<any[]>('/catalogs/rrhh-types');
  }

  /**
   * Obtiene las áreas de investigación
   */
  getAreas(): Observable<any[]> {
    return this.baseHttp.get<any[]>('/catalogs/areas');
  }
}
