// ========================================
// BACKEND DTOs - Coinciden con las entidades del backend
// ========================================

// Base DTO para productos científicos
export interface ProductoCientificoDTO {
  id?: number;
  descripcion?: string;
  comentario?: string;
  fechaInicio?: string; // LocalDate como string
  fechaTermino?: string; // LocalDate como string
  tipoProducto?: TipoProductoDTO;
  urlDocumento?: string;
  linkVisualizacion?: string;
  linkPDF?: string;
  progressReport?: number;
  estadoProducto?: EstadoProductoDTO;
  codigoANID?: string;
  basal?: string; // Character como string
  lineasInvestigacion?: string; // JSON string
  participantesNombres?: string; // Nombres de participantes concatenados por coma (campo calculado)
  createdAt?: string; // LocalDateTime como string
  updatedAt?: string; // LocalDateTime como string
}

// DTO para Participante (formato backend)
export interface ParticipanteDTO {
  rrhhId?: number;
  tipoParticipacionId?: number;
  orden?: number;
  corresponding?: boolean;
  idRRHHProducto?: number; // ID de la participación en rrhh_producto
}

// DTO para Afiliacion
export interface AfiliacionDTO {
  idRRHH?: number;
  idProducto?: number;
  idRRHHProducto?: number;
  id?: number;
  idInstitucion?: number;
  texto?: string;
  nombreInstitucion?: string; // Para mostrar en el frontend
}

// DTO para Publicación
export interface PublicacionDTO extends ProductoCientificoDTO {
  journal?: JournalDTO;
  volume?: string;
  yearPublished?: number;
  firstpage?: string;
  lastpage?: string;
  indexs?: string; // JSON string
  funding?: string; // JSON string;
  doi?: string;
  numCitas?: number;
  factorImpacto?: number; // Factor de Impacto del Journal
  factorImpactoPromedio?: number; // Factor de Impacto Promedio del Journal
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para TipoEvento
export interface TipoEventoDTO {
  id?: number;
  idDescripcion?: string;
  descripcion?: string;
  createdAt?: string;
  updatedAt?: string;
}

// DTO para País
export interface PaisDTO {
  codigo?: string;
  idDescripcion?: string;
}

// DTO para OrganizacionEventosCientificos
export interface OrganizacionEventosCientificosDTO extends ProductoCientificoDTO {
  tipoEvento?: TipoEventoDTO;
  pais?: PaisDTO;
  ciudad?: string;
  numParticipantes?: number;
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para Institucion
export interface InstitucionDTO {
  id?: number;
  idDescripcion?: string; // Mantener por compatibilidad
  descripcion?: string; // Texto traducido desde la vista
}

// DTO para GradoAcademico
export interface GradoAcademicoDTO {
  id?: number;
  idDescripcion?: string;
  descripcion?: string;
}

// DTO para EstadoTesis
export interface EstadoTesisDTO {
  id?: number;
  idDescripcion?: string;
  descripcion?: string;
}

// DTO para TipoSector
export interface TipoSectorDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para Tesis
export interface TesisDTO extends ProductoCientificoDTO {
  institucionOG?: InstitucionDTO; // Institución que otorga el título
  gradoAcademico?: GradoAcademicoDTO;
  institucion?: InstitucionDTO; // Institución donde se insertó el estudiante
  estadoTesis?: EstadoTesisDTO;
  fechaInicioPrograma?: string; // LocalDate como string
  nombreCompletoTitulo?: string;
  tipoSector?: string; // JSON string o lista separada por comas
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
  estudiante?: string; // Campo calculado: nombre del estudiante (rol 7)
}

// DTO para TipoTransferencia
export interface TipoTransferenciaDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para CategoriaTransferencia
export interface CategoriaTransferenciaDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para TransferenciaTecnologica
export interface TransferenciaTecnologicaDTO extends ProductoCientificoDTO {
  institucion?: InstitucionDTO;
  tipoTransferencia?: TipoTransferenciaDTO;
  categoriaTransferencia?: string; // JSON string o lista separada por comas
  ciudad?: string;
  region?: string;
  agno?: number;
  pais?: PaisDTO;
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para Resource
export interface ResourceDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para FundingType
export interface FundingTypeDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para BecariosPostdoctorales
export interface BecariosPostdoctoralesDTO extends ProductoCientificoDTO {
  postdoctoralFellowName?: string; // Campo calculado: nombre del becario postdoctoral (rol 19)
  institucion?: InstitucionDTO;
  fundingSource?: string; // JSON string o lista separada por comas
  tipoSector?: TipoSectorDTO;
  resources?: string; // JSON string o lista separada por comas
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para TipoDifusion
export interface TipoDifusionDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para PublicoObjetivo
export interface PublicoObjetivoDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para Difusion (Outreach Activities)
export interface DifusionDTO extends ProductoCientificoDTO {
  tipoDifusion?: TipoDifusionDTO;
  pais?: PaisDTO;
  lugar?: string;
  numAsistentes?: number;
  duracion?: number;
  publicoObjetivo?: string; // Lista separada por comas de IDs
  ciudad?: string;
  link?: string;
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para TipoColaboracion
export interface TipoColaboracionDTO {
  id?: number;
  idDescripcion?: string;
}

// DTO para Colaboracion (Scientific Collaborations)
export interface ColaboracionDTO extends ProductoCientificoDTO {
  tipoColaboracion?: TipoColaboracionDTO;
  institucion?: InstitucionDTO;
  paisOrigen?: PaisDTO;
  ciudadOrigen?: string;
  codigoPaisDestino?: string;
  ciudadDestino?: string;
  participantes?: ParticipanteDTO[]; // Participantes en formato backend
}

// DTO para Tipo de Participación (Nueva estructura)
export interface TipoParticipacionDTO {
  id?: number;
  idDescripcion?: string; // Referencia a textos.codigoTexto
  idTipoProducto?: number; // Referencia a tipoproducto.id
  descripcion?: string; // Valor traducido desde textos
  tipoProductoNombre?: string; // Nombre del tipo de producto
}

// DTO para crear Tipo de Participación
export interface CreateTipoParticipacionDTO {
  descripcion: string; // Descripción que ingresa el usuario
  idTipoProducto: number; // ID del tipo de producto seleccionado
}

// DTO para Tipo de Producto (Nueva estructura)
export interface TipoProductoDTO {
  id?: number;
  idDescripcion?: string; // Referencia a textos.codigoTexto
  descripcion?: string; // Valor traducido desde textos
}

// DTO para crear Tipo de Producto
export interface CreateTipoProductoDTO {
  descripcion: string; // Descripción que ingresa el usuario
}

// DTO para Estado de Producto
export interface EstadoProductoDTO {
  id?: number;
  codigoDescripcion?: string;
  codigo?: string;
  nombre?: string;
  descripcion?: string;
  color?: string;
  esEstadoFinal?: boolean;
  orden?: number;
  createdAt?: string;
  updatedAt?: string;
}

// DTO para Journal
export interface JournalDTO {
  id?: number;
  idDescripcion?: string;
  descripcion?: string;
  abbreviation?: string;
  issn?: string;
}

// DTO para RRHH (Investigadores)
export interface RRHHDTO {
  id?: number;
  idRecurso?: string; // RUT Chile
  fullname?: string;
  idTipoRRHH?: number;
  tipoRRHH?: TipoRRHHDTO;
  numCelular?: string;
  email?: string;
  iniciales?: string;
  orcid?: string;
  codigoGenero?: string; // M o F
  createdAt?: string;
  updatedAt?: string;
}

// DTO para Tipo de RRHH
export interface TipoRRHHDTO {
  id?: number;
  idDescripcion?: string;
  descripcion?: string;
  createdAt?: string;
  updatedAt?: string;
}

// DTO para Proyecto (Nueva estructura con vista)
export interface ProyectoDTO {
  codigo: string;
  idDescripcion?: string; // Referencia a textos.codigoTexto
  descripcion?: string; // Valor traducido desde textos
  fechaInicio?: string; // LocalDate como string
  fechaTermino?: string; // LocalDate como string
  codigoExterno?: string; // Código ANID
  tipoFinanciamiento?: string; // Lista separada por comas
  realizaCon?: string; // Lista separada por comas
  totalProductos?: number; // Conteo de productos asociados
  createdAt?: string; // LocalDateTime como string
  updatedAt?: string; // LocalDateTime como string
}

// DTO para Participación en Producto
export interface ParticipacionProductoDTO {
  id?: ParticipacionProductoIdDTO;
  producto?: ProductoCientificoDTO;
  participante?: RRHHDTO;
  tipoParticipacion?: TipoParticipacionDTO;
  esCorresponding?: boolean;
  orden?: number;
}

// DTO para ID compuesto de Participación
export interface ParticipacionProductoIdDTO {
  productoId: number;
  participanteId: number;
}

// DTO para Producto-Proyecto
export interface ProductoProyectoDTO {
  id?: ProductoProyectoIdDTO;
  producto?: ProductoCientificoDTO;
  proyecto?: ProyectoDTO;
  porcentaje?: number;
}

// DTO para ID compuesto de Producto-Proyecto
export interface ProductoProyectoIdDTO {
  productoId: number;
  proyectoId: number;
}

// DTO para Usuario
export interface UserDTO {
  id?: number;
  username: string;
  email: string;
  password?: string; // Solo para creación/actualización
  roles?: RoleDTO[];
  activo?: boolean;
}

// DTO para Rol
export interface RoleDTO {
  id?: number;
  name: string;
  description?: string;
}

// DTOs para Autenticación
export interface LoginRequestDTO {
  username: string;
  password: string;
}

export interface LoginResponseDTO {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

// DTOs para Respuestas de la API
export interface ApiResponseDTO<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginatedResponseDTO<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// DTOs para Filtros y Búsquedas
export interface SearchFiltersDTO {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  search?: string;
  filters?: { [key: string]: any };
}

// DTOs para Estadísticas
// ========================================
// DTOs para Preview de Publicaciones desde DOI
// ========================================

export interface PublicationPreviewDataDTO {
  title?: string;
  displayName?: string;
  publicationDate?: string;
  publicationYear?: number;
  doi?: string;
  volume?: string;
  firstPage?: string;
  lastPage?: string;
  openAlexUrl?: string;
}

export interface JournalPreviewDTO {
  name?: string;
  issn?: string;
  status?: 'matched' | 'new' | 'review';
  matchId?: number;
  candidates?: string[];
}

export interface AffiliationPreviewDTO {
  name?: string;
  rawAffiliationString?: string;
  status?: 'matched' | 'new' | 'review';
  matchId?: number;
  candidates?: string[];
}

export interface AuthorPreviewDTO {
  openAlexId?: string;
  name?: string;
  orcid?: string; // ORCID de OpenAlex
  order?: number;
  isCorresponding?: boolean;
  authorPosition?: string;
  tipoParticipacionId?: number; // ID del tipo de participación calculado desde authorPosition (1=Author principal, 2=Co-author)
  status?: 'matched' | 'new' | 'review';
  matchId?: number;
  matchedName?: string; // Nombre de BD cuando hay match
  matchedOrcid?: string; // ORCID de BD cuando hay match
  candidates?: string[];
  affiliations?: AffiliationPreviewDTO[];
  // Estado de sincronización de ORCID
  orcidSyncStatus?: 'ok' | 'missing_local' | 'conflict';
  orcidChangeAction?: 'none' | 'add' | 'replace' | 'unlink';
  matchMethod?: 'orcid' | 'name'; // Cómo se hizo el match
}

export interface PublicationPreviewDTO {
  publication?: PublicationPreviewDataDTO;
  journal?: JournalPreviewDTO;
  authors?: AuthorPreviewDTO[];
}

// ========================================
// DTOs para Importación de Publicaciones desde DOI
// ========================================

export interface JournalImportDecisionDTO {
  action?: 'link' | 'create' | 'matched';
  journalId?: number;
  name?: string;
  issn?: string;
}

export interface AffiliationImportDecisionDTO {
  name?: string;
  rawAffiliationString?: string;
  action?: 'link' | 'create' | 'matched';
  institutionId?: number;
  texto?: string;
}

export interface AuthorImportDecisionDTO {
  openAlexId?: string;
  name?: string;
  orcid?: string;
  order?: number;
  isCorresponding?: boolean;
  authorPosition?: string;
  action?: 'link' | 'create' | 'matched';
  rrhhId?: number;
  tipoParticipacionId?: number;
  affiliations?: AffiliationImportDecisionDTO[];
}

export interface PublicationImportRequestDTO {
  publication?: PublicationPreviewDataDTO;
  journal?: JournalImportDecisionDTO;
  authors?: AuthorImportDecisionDTO[];
}

export interface EstadisticasDTO {
  totalPublicaciones: number;
  totalProyectos: number;
  totalInvestigadores: number;
  publicacionesPorAno: { [key: string]: number };
  investigadoresPorTipo: { [key: string]: number };
}

