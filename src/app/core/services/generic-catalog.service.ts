import { Injectable } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { 
  CatalogType, 
  CatalogTypeInfo, 
  CatalogItem,
  TipoParticipacionDTO,
  TipoProductoDTO,
  EstadoProductoDTO,
  TipoInvestigadorDTO,
  RevistaDTO,
  TipoIndizacionDTO
} from '../models/catalog-types';
import { BaseHttpService } from './base-http.service';
import { TipoParticipacionDTO as BackendTipoParticipacionDTO, CreateTipoParticipacionDTO, TipoProductoDTO as BackendTipoProductoDTO, CreateTipoProductoDTO, JournalDTO, InstitucionDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class GenericCatalogService {
  
  private catalogTypes: CatalogTypeInfo[] = [
    {
      type: CatalogType.PARTICIPATION_TYPES,
      name: 'Participation Types',
      description: 'Tipos de participación en productos científicos',
      icon: 'group',
      route: '/catalogs/participation-types',
      apiEndpoint: '/catalogs/participation-types'
    },
    {
      type: CatalogType.PRODUCT_TYPES,
      name: 'Product Types',
      description: 'Tipos de productos científicos',
      icon: 'category',
      route: '/catalogs/product-types',
      apiEndpoint: '/catalogs/product-types'
    },
    {
      type: CatalogType.RESEARCHER_TYPES,
      name: 'Researcher Types',
      description: 'Tipos de investigadores',
      icon: 'person',
      route: '/catalogs/researcher-types',
      apiEndpoint: '/catalogs/researcher-types'
    },
    {
      type: CatalogType.JOURNALS,
      name: 'Journals',
      description: 'Revistas científicas',
      icon: 'book',
      route: '/catalogs/journals',
      apiEndpoint: '/catalogs/journals'
    }
  ];

  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Obtiene todos los tipos de catálogo disponibles
   */
  getCatalogTypes(): CatalogTypeInfo[] {
    return this.catalogTypes;
  }

  /**
   * Obtiene la información de un tipo de catálogo específico
   */
  getCatalogTypeInfo(type: CatalogType): CatalogTypeInfo | undefined {
    return this.catalogTypes.find(ct => ct.type === type);
  }

  /**
   * Obtiene todos los elementos de un catálogo específico
   */
  getItems(catalogType: CatalogType): Observable<CatalogItem[]> {
    const catalogInfo = this.getCatalogTypeInfo(catalogType);
    if (!catalogInfo) {
      return of([]);
    }

    // Para participation-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PARTICIPATION_TYPES) {
      return this.baseHttp.get<BackendTipoParticipacionDTO[]>(catalogInfo.apiEndpoint)
        .pipe(
          // Mapear los DTOs del backend a los DTOs del frontend
          map((backendItems: BackendTipoParticipacionDTO[]) => {
            console.log('🔍 Backend items received:', backendItems);
            return backendItems.map(item => {
              const mappedItem = {
                id: item.id,
                codigo: item.idDescripcion || '', // Usar idDescripcion como código
                nombre: item.descripcion || '', // Usar descripción traducida como nombre
                descripcion: item.descripcion || '', // Descripción traducida
                activo: true, // Agregar propiedad activo que requiere el frontend
                // Agregar los campos específicos de TipoParticipacionDTO
                idDescripcion: item.idDescripcion,
                idTipoProducto: item.idTipoProducto,
                tipoProductoNombre: item.tipoProductoNombre,
                aplicableProductos: 'ALL', // Valor por defecto
                puedeSerCorresponding: false // Valor por defecto
              } as CatalogItem;
              console.log('🔄 Mapped item:', mappedItem);
              return mappedItem;
            });
          })
        );
    }

    // Para product-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PRODUCT_TYPES) {
      console.log('🔄 GenericCatalogService: Loading product types from:', catalogInfo.apiEndpoint);
      return this.baseHttp.get<BackendTipoProductoDTO[]>(catalogInfo.apiEndpoint)
        .pipe(
          // Mapear los DTOs del backend a los DTOs del frontend
          map((backendItems: BackendTipoProductoDTO[]) => {
            console.log('📦 GenericCatalogService: Raw backend items:', backendItems);
            const mappedItems = backendItems.map(item => ({
              id: item.id,
              codigo: item.idDescripcion || '', // Usar idDescripcion como código
              nombre: item.descripcion || '', // Usar descripción traducida como nombre
              descripcion: item.descripcion || '', // Descripción traducida
              activo: true // Agregar propiedad activo que requiere el frontend
            } as CatalogItem));
            console.log('📦 GenericCatalogService: Mapped items:', mappedItems);
            return mappedItems;
          })
        );
    }

    // Para journals, usar el endpoint real del backend
    if (catalogType === CatalogType.JOURNALS) {
      console.log('🔄 GenericCatalogService: Loading journals from:', catalogInfo.apiEndpoint);
      return this.baseHttp.get<JournalDTO[]>(catalogInfo.apiEndpoint)
        .pipe(
          // Mapear los DTOs del backend a los DTOs del frontend
          map((backendItems: JournalDTO[]) => {
            console.log('🔍 Backend journals received:', backendItems);
            return backendItems.map(item => {
              const mappedItem = {
                id: item.id,
                codigo: item.idDescripcion || '', // Usar idDescripcion como código
                nombre: item.descripcion || '', // Usar descripción traducida como nombre
                descripcion: item.descripcion || '', // Descripción traducida
                activo: true, // Agregar propiedad activo que requiere el frontend
                // Agregar los campos específicos de RevistaDTO
                idDescripcion: item.idDescripcion,
                abbreviation: item.abbreviation,
                issn: item.issn
              } as CatalogItem;
              //console.log('🔄 Mapped journal item:', mappedItem);
              return mappedItem;
            });
          }),
          // Agregar catchError para manejar errores
          catchError(error => {
            console.error('❌ Error loading journals from backend:', error);
            console.log('🔄 Falling back to mock data for journals');
            // Si hay error, usar datos mock
            return of(this.getMockData(catalogType)).pipe(delay(300));
          })
        );
    }

    // Para otros tipos, usar datos mock por ahora
    return of(this.getMockData(catalogType)).pipe(delay(300));
  }

  /**
   * Obtiene un elemento específico de un catálogo
   */
  getItem(catalogType: CatalogType, id: number): Observable<CatalogItem | null> {
    const catalogInfo = this.getCatalogTypeInfo(catalogType);
    if (!catalogInfo) {
      return of(null);
    }

    // Para participation-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PARTICIPATION_TYPES) {
      return this.baseHttp.get<BackendTipoParticipacionDTO>(`${catalogInfo.apiEndpoint}/${id}`)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendItem: BackendTipoParticipacionDTO) => ({
            id: backendItem.id,
            codigo: backendItem.idDescripcion || '',
            nombre: backendItem.descripcion || '',
            descripcion: backendItem.descripcion || '',
            activo: true,
            // Agregar los campos específicos de TipoParticipacionDTO
            idDescripcion: backendItem.idDescripcion,
            idTipoProducto: backendItem.idTipoProducto,
            tipoProductoNombre: backendItem.tipoProductoNombre,
            aplicableProductos: 'ALL', // Valor por defecto
            puedeSerCorresponding: false // Valor por defecto
          } as CatalogItem))
        );
    }

    // Para product-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PRODUCT_TYPES) {
      return this.baseHttp.get<BackendTipoProductoDTO>(`${catalogInfo.apiEndpoint}/${id}`)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendItem: BackendTipoProductoDTO) => ({
            id: backendItem.id,
            codigo: backendItem.idDescripcion || '',
            nombre: backendItem.descripcion || '',
            descripcion: backendItem.descripcion || '',
            activo: true
          } as CatalogItem))
        );
    }

    // Para journals, usar el endpoint real del backend
    if (catalogType === CatalogType.JOURNALS) {
      return this.baseHttp.get<JournalDTO>(`${catalogInfo.apiEndpoint}/${id}`)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendItem: JournalDTO) => ({
            id: backendItem.id,
            codigo: backendItem.idDescripcion || '', // Usar idDescripcion como código
            nombre: backendItem.descripcion || '', // Usar descripción traducida como nombre
            descripcion: backendItem.descripcion || '', // Descripción traducida
            activo: true, // Agregar propiedad activo que requiere el frontend
            // Agregar los campos específicos de RevistaDTO
            idDescripcion: backendItem.idDescripcion,
            abbreviation: backendItem.abbreviation,
            issn: backendItem.issn
          } as CatalogItem))
        );
    }

    // Para otros tipos, buscar en datos mock
    const mockData = this.getMockData(catalogType);
    const item = mockData.find(item => item.id === id);
    return of(item || null).pipe(delay(300));
  }

  /**
   * Crea un nuevo elemento en un catálogo
   */
  createItem(catalogType: CatalogType, item: CatalogItem): Observable<CatalogItem> {
    const catalogInfo = this.getCatalogTypeInfo(catalogType);
    if (!catalogInfo) {
      throw new Error(`Tipo de catálogo no válido: ${catalogType}`);
    }

    // Para participation-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PARTICIPATION_TYPES) {
      // Convertir CatalogItem a CreateTipoParticipacionDTO
      const createDto: CreateTipoParticipacionDTO = {
        descripcion: item.nombre || item.descripcion || '',
        idTipoProducto: (item as any).idTipoProducto || 1 // Default a tipo 1 si no se especifica
      };
      
      return this.baseHttp.post<BackendTipoParticipacionDTO>(catalogInfo.apiEndpoint, createDto)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendResponse: BackendTipoParticipacionDTO) => ({
            id: backendResponse.id,
            codigo: backendResponse.idDescripcion || '',
            nombre: backendResponse.descripcion || '',
            descripcion: backendResponse.descripcion || '',
            activo: true
          } as CatalogItem))
        );
    }

    // Para product-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PRODUCT_TYPES) {
      // Convertir CatalogItem a CreateTipoProductoDTO
      const createDto: CreateTipoProductoDTO = {
        descripcion: item.nombre || item.descripcion || ''
      };
      
      return this.baseHttp.post<BackendTipoProductoDTO>(catalogInfo.apiEndpoint, createDto)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendResponse: BackendTipoProductoDTO) => ({
            id: backendResponse.id,
            codigo: backendResponse.idDescripcion || '',
            nombre: backendResponse.descripcion || '',
            descripcion: backendResponse.descripcion || '',
            activo: true
          } as CatalogItem))
        );
    }

    // Para otros tipos, simular creación
    const newItem = { ...item, id: Date.now() };
    return of(newItem).pipe(delay(300));
  }

  /**
   * Actualiza un elemento existente en un catálogo
   */
  updateItem(catalogType: CatalogType, id: number, item: CatalogItem): Observable<CatalogItem> {
    const catalogInfo = this.getCatalogTypeInfo(catalogType);
    if (!catalogInfo) {
      throw new Error(`Tipo de catálogo no válido: ${catalogType}`);
    }

    // Para participation-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PARTICIPATION_TYPES) {
      // Convertir CatalogItem a CreateTipoParticipacionDTO
      const updateDto: CreateTipoParticipacionDTO = {
        descripcion: item.nombre || item.descripcion || '',
        idTipoProducto: (item as any).idTipoProducto || 1 // Default a tipo 1 si no se especifica
      };
      
      return this.baseHttp.put<BackendTipoParticipacionDTO>(`${catalogInfo.apiEndpoint}/${id}`, updateDto)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendResponse: BackendTipoParticipacionDTO) => ({
            id: backendResponse.id,
            codigo: backendResponse.idDescripcion || '',
            nombre: backendResponse.descripcion || '',
            descripcion: backendResponse.descripcion || '',
            activo: true
          } as CatalogItem))
        );
    }

    // Para product-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PRODUCT_TYPES) {
      // Convertir CatalogItem a CreateTipoProductoDTO
      const updateDto: CreateTipoProductoDTO = {
        descripcion: item.nombre || item.descripcion || ''
      };
      
      return this.baseHttp.put<BackendTipoProductoDTO>(`${catalogInfo.apiEndpoint}/${id}`, updateDto)
        .pipe(
          // Mapear el DTO del backend al DTO del frontend
          map((backendResponse: BackendTipoProductoDTO) => ({
            id: backendResponse.id,
            codigo: backendResponse.idDescripcion || '',
            nombre: backendResponse.descripcion || '',
            descripcion: backendResponse.descripcion || '',
            activo: true
          } as CatalogItem))
        );
    }

    // Para otros tipos, simular actualización
    const updatedItem = { ...item, id };
    return of(updatedItem).pipe(delay(300));
  }

  /**
   * Elimina un elemento de un catálogo
   */
  deleteItem(catalogType: CatalogType, id: number): Observable<boolean> {
    const catalogInfo = this.getCatalogTypeInfo(catalogType);
    if (!catalogInfo) {
      throw new Error(`Tipo de catálogo no válido: ${catalogType}`);
    }

    // Para participation-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PARTICIPATION_TYPES) {
      return this.baseHttp.delete<boolean>(`${catalogInfo.apiEndpoint}/${id}`);
    }

    // Para product-types, usar el endpoint real del backend
    if (catalogType === CatalogType.PRODUCT_TYPES) {
      return this.baseHttp.delete<boolean>(`${catalogInfo.apiEndpoint}/${id}`);
    }

    // Para otros tipos, simular eliminación
    return of(true).pipe(delay(300));
  }

  /**
   * Obtiene datos mock para tipos de catálogo que aún no tienen endpoints
   */
  private getMockData(catalogType: CatalogType): CatalogItem[] {
    switch (catalogType) {
      case 'participation-types':
        return [
          {
            id: 1,
            nombre: 'Investigador Principal',
            descripcion: 'Responsable principal del proyecto de investigación',
            activo: true,
            idDescripcion: 'PARTICIPATION_001',
            idTipoProducto: 1,
            tipoProductoNombre: 'Research Projects'
          },
          {
            id: 2,
            nombre: 'Co-Autor',
            descripcion: 'Colaborador en la investigación',
            activo: true,
            idDescripcion: 'PARTICIPATION_002',
            idTipoProducto: 2,
            tipoProductoNombre: 'Publications'
          },
          {
            id: 3,
            nombre: 'Autor',
            descripcion: 'Autor principal de la publicación',
            activo: true,
            idDescripcion: 'PARTICIPATION_003',
            idTipoProducto: 2,
            tipoProductoNombre: 'Publications'
          }
        ];

      case 'product-types':
        return [
          {
            id: 1,
            nombre: 'Publications',
            descripcion: 'Scientific publications and articles',
            activo: true,
            idDescripcion: 'PRODUCT_001'
          }
        ];

      case 'researcher-types':
        return [
          {
            id: 1,
            codigo: 'PROFESSOR',
            nombre: 'Profesor',
            descripcion: 'Profesor universitario',
            activo: true,
            nivel: 'PROFESSOR',
            requiereAfiliacion: true
          },
          {
            id: 2,
            codigo: 'RESEARCHER',
            nombre: 'Investigador',
            descripcion: 'Investigador científico',
            activo: true,
            nivel: 'RESEARCHER',
            requiereAfiliacion: true
          },
          {
            id: 3,
            codigo: 'STUDENT',
            nombre: 'Estudiante',
            descripcion: 'Estudiante de posgrado',
            activo: true,
            nivel: 'GRADUATE',
            requiereAfiliacion: false
          }
        ];

      case 'journals':
        return [
          {
            id: 1,
            nombre: 'Nature',
            descripcion: 'Revista científica multidisciplinaria',
            activo: true,
            idDescripcion: 'JOURNAL_001',
            abbreviation: 'Nature',
            issn: '0028-0836'
          },
          {
            id: 2,
            nombre: 'Science',
            descripcion: 'Revista científica multidisciplinaria',
            activo: true,
            idDescripcion: 'JOURNAL_002',
            abbreviation: 'Science',
            issn: '0036-8075'
          }
        ];


      default:
        return [];
    }
  }

  /**
   * Obtiene todas las instituciones
   */
  getInstitutions(): Observable<InstitucionDTO[]> {
    return this.baseHttp.get<InstitucionDTO[]>('/catalogs/institutions').pipe(
      catchError(error => {
        console.error('Error loading institutions:', error);
        return of([]);
      })
    );
  }
}