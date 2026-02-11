// Base interface for all catalog items
export interface BaseCatalogItem {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
}

// Participation Types (updated to match backend structure)
export interface TipoParticipacionDTO {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  idDescripcion: string;
  idTipoProducto: number;
  tipoProductoNombre: string;
  aplicableProductos: string;
  puedeSerCorresponding: boolean;
}

// Product Types (updated to match backend structure)
export interface TipoProductoDTO {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  idDescripcion: string;
}

// Product Status
export interface EstadoProductoDTO extends BaseCatalogItem {
  codigo: string;
  color: string;
  esEstadoFinal: boolean;
  orden: number;
}

// Researcher Types
export interface TipoInvestigadorDTO extends BaseCatalogItem {
  codigo: string;
  nivel: 'UNDERGRADUATE' | 'GRADUATE' | 'POSTGRADUATE' | 'PROFESSOR' | 'RESEARCHER';
  requiereAfiliacion: boolean;
}

// Journals
export interface RevistaDTO {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  idDescripcion: string;
  abbreviation: string;
  issn: string;
}

// Index Types
export interface TipoIndizacionDTO extends BaseCatalogItem {
  codigo: string;
  categoria: 'INTERNATIONAL' | 'NATIONAL' | 'REGIONAL';
  url?: string;
  activo: boolean;
}

// Union type for all catalog types
export type CatalogItem = 
  | TipoParticipacionDTO 
  | TipoProductoDTO 
  | EstadoProductoDTO 
  | TipoInvestigadorDTO 
  | RevistaDTO 
  | TipoIndizacionDTO;

// Catalog type definitions
export enum CatalogType {
  PARTICIPATION_TYPES = 'participation-types',
  PRODUCT_TYPES = 'product-types',
  RESEARCHER_TYPES = 'researcher-types',
  JOURNALS = 'journals'
}

// Catalog type metadata
export interface CatalogTypeInfo {
  type: CatalogType;
  name: string;
  description: string;
  icon: string;
  route: string;
  apiEndpoint: string;
}

