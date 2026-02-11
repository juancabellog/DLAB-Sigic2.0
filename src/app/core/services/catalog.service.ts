import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TipoParticipacionDTO } from '../models/catalog-types';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private readonly API_URL = `${environment.apiUrl}/catalogs`;
  private participationTypesCache = new Map<string, TipoParticipacionDTO[]>();
  private cacheTimeout = 300000; // 5 minutos

  constructor(private http: HttpClient) {}

  getParticipationTypes(productType?: string): Observable<TipoParticipacionDTO[]> {
    const cacheKey = productType || 'all';

    console.log("CatalogService: getParticipationTypes - cacheKey: " + cacheKey);

    // Retornar desde cache si está disponible
    if (this.participationTypesCache.has(cacheKey)) {
      return of(this.participationTypesCache.get(cacheKey)!);
    }
    
    // Mapear productType string a ID numérico del tipo de producto
    const productTypeId = this.getProductTypeId(productType);
    
    console.log("CatalogService: getParticipationTypes - productTypeId: " + productTypeId);
    const url = productTypeId 
      ? `${this.API_URL}/participation-types/by-product-type/${productTypeId}`
      : `${this.API_URL}/participation-types`;
    
    return this.http.get<TipoParticipacionDTO[]>(url).pipe(
      tap(types => {
        // Guardar en cache con timestamp
        this.participationTypesCache.set(cacheKey, types);
        
        // Limpiar cache después del timeout
        setTimeout(() => {
          this.participationTypesCache.delete(cacheKey);
        }, this.cacheTimeout);
      }),
      catchError(error => {
        console.error('Error loading participation types:', error);
        // Retornar valores por defecto en caso de error
        return of(this.getDefaultParticipationTypes());
      })
    );
  }

  /**
   * Mapea el nombre del tipo de producto (string) al ID numérico
   * @param productType Nombre del tipo de producto (ej: 'publications', 'projects', etc.)
   * @returns ID numérico del tipo de producto o undefined si no se encuentra
   */
  private getProductTypeId(productType?: string): number | undefined {
    if (!productType) {
      return undefined;
    }
    
    // Mapeo de nombres de tipos de producto a sus IDs
    const productTypeMap: { [key: string]: number } = {
      'publications': 3,
      'projects': 2,
      'outreach-activities': 1, // DIFUSION
      'scientific-collaborations': 12, // COLLABORATIONS - ID correcto según el usuario
      'postdoctoral-fellows': 14, // BECAPOSTDOCTO - ID correcto según el usuario
      'thesis-students': 11, // TESIS
      'technology-transfer': 10, // TRANSF_TECNOLOGICA
      'scientific-events': 15 // ORGANIZACION_EVENTOS_CIENTIFICOS - ID correcto según el usuario
    };
    
    return productTypeMap[productType.toLowerCase()];
  }

  getCorrespondingEligibleTypes(): Observable<TipoParticipacionDTO[]> {
    return this.http.get<TipoParticipacionDTO[]>(`${this.API_URL}/participation-types/corresponding-eligible`);
  }

  refreshParticipationTypes(): void {
    this.participationTypesCache.clear();
  }

  private getDefaultParticipationTypes(): TipoParticipacionDTO[] {
    // Valores por defecto en caso de que el servicio falle
    return [
      { 
        id: 1, 
        nombre: 'Author', 
        descripcion: 'Main author of the publication',
        activo: true,
        idDescripcion: 'PARTICIPATION_001',
        idTipoProducto: 1,
        tipoProductoNombre: 'Publications',
        puedeSerCorresponding: true,
        aplicableProductos: 'PUBLICATIONS'
      },
      { 
        id: 2, 
        nombre: 'Co-Author', 
        descripcion: 'Collaborating author',
        activo: true,
        idDescripcion: 'PARTICIPATION_002',
        idTipoProducto: 1,
        tipoProductoNombre: 'Publications',
        puedeSerCorresponding: true,
        aplicableProductos: 'PUBLICATIONS'
      },
      { 
        id: 3, 
        nombre: 'Principal Investigator', 
        descripcion: 'Lead researcher',
        activo: true,
        idDescripcion: 'PARTICIPATION_003',
        idTipoProducto: 2,
        tipoProductoNombre: 'Research Projects',
        puedeSerCorresponding: false,
        aplicableProductos: 'ALL'
      },
      { 
        id: 4, 
        nombre: 'Co-Investigator', 
        descripcion: 'Collaborating researcher',
        activo: true,
        idDescripcion: 'PARTICIPATION_004',
        idTipoProducto: 2,
        tipoProductoNombre: 'Research Projects',
        puedeSerCorresponding: false,
        aplicableProductos: 'ALL'
      }
    ];
  }
}



