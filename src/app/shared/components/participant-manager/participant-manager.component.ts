import { Component, Input, Output, EventEmitter, OnInit, OnChanges, OnDestroy, SimpleChanges, ViewChild, ViewChildren, ElementRef, QueryList } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatRadioModule } from '@angular/material/radio';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

import { CatalogService } from '../../../core/services/catalog.service';
import { TipoParticipacionDTO } from '../../../core/models/catalog-types';
import { ResearcherSearchComponent } from '../researcher-search/researcher-search.component';
import { RRHHDTO } from '../../../core/models/backend-dtos';
import { AffiliationManagerComponent } from '../affiliation-manager/affiliation-manager.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../confirm-dialog/confirm-dialog.component';

export interface ParticipantDTO {
  rrhhId: number;
  fullName: string;
  idRecurso?: string;
  orcid?: string;
  participationTypeId: number;
  participationTypeName?: string;
  corresponding: boolean;
  order: number;
  idRRHHProducto?: number; // ID de la participación en rrhh_producto
  // Campos para preview desde DOI
  previewStatus?: 'matched' | 'new' | 'review';
  previewMatchId?: number;
  previewMatchedName?: string;
  previewCandidates?: string[];
  previewOpenAlexId?: string;
  previewAction?: 'link' | 'create' | 'matched';
  previewOriginalName?: string; // Nombre original de OpenAlex (antes del match)
  // Estado de resolución para autores externos
  resolved?: boolean; // Indica si el autor externo ha sido resuelto
  resolutionType?: 'unresolved' | 'createNew' | 'linkExisting'; // Tipo de resolución
  linkedRrhhId?: number; // ID del RRHH cuando resolutionType === 'linkExisting'
  newRrhhData?: { // Datos para crear nuevo RRHH cuando resolutionType === 'createNew'
    name: string;
    orcid?: string;
  };
  // Mantener resolutionStatus para compatibilidad (deprecated, usar resolutionType)
  resolutionStatus?: 'unresolved' | 'linkExisting' | 'createNew' | 'matched';
  // Estado de afiliaciones para autores externos
  affiliationsStatus?: 'complete' | 'incomplete'; // Estado de resolución de afiliaciones
  // Lista de afiliaciones del participante (se actualiza cuando affiliation-manager carga las afiliaciones)
  affiliations?: import('../../../core/models/backend-dtos').AfiliacionDTO[];
  // Estado de sincronización de ORCID (desde preview)
  orcidSyncStatus?: 'ok' | 'missing_local' | 'conflict';
  orcidChangeAction?: 'none' | 'add' | 'replace' | 'unlink';
  matchMethod?: 'orcid' | 'name';
  // ORCIDs separados (OpenAlex vs BD)
  orcidFromOpenAlex?: string; // ORCID que viene de OpenAlex
  orcidFromBD?: string; // ORCID que está en BD (matchedOrcid)
}

export interface ResearcherDTO {
  id: number;
  fullName: string;
  idRecurso?: string;
  orcid?: string;
  tipoRRHHDescripcion?: string;
}

export interface InstitutionSummaryItem {
  id: number;              // institutionId
  index: number;           // 1, 2, 3...
  name: string;            // institutionName / descripcion
  countAuthors: number;    // cuántos autores tienen esta institution
}

@Component({
  selector: 'app-participant-manager',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatExpansionModule,
    MatRadioModule,
    DragDropModule,
    ResearcherSearchComponent,
    AffiliationManagerComponent
  ],
  templateUrl: './participant-manager.component.html',
  styleUrls: ['./participant-manager.component.scss'],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ])
  ]
})
export class ParticipantManagerComponent implements OnInit, OnChanges, OnDestroy {
  @Input() participants: ParticipantDTO[] = [];
  @Input() productType: string = '';
  @Input() publicationId?: number; // Solo para publicaciones (afiliaciones)
  @Input() previewAuthors?: import('../../../core/models/backend-dtos').AuthorPreviewDTO[]; // Datos del preview desde DOI
  @Input() doiLoading: boolean = false; // Indica si se está cargando desde DOI
  @Output() participantsChange = new EventEmitter<ParticipantDTO[]>();
  @Output() hasIncompleteChange = new EventEmitter<boolean>();
  
  @ViewChild('searchSection', { static: false }) searchSection!: ElementRef;
  @ViewChildren('participantPanel') participantPanels!: QueryList<ElementRef>;
  
  participationTypes: TipoParticipacionDTO[] = [];
  loadingParticipationTypes = false;
  participationTypesError = false;
  
  // Control de expand/collapse
  expandedParticipants: Set<number> = new Set();
  allExpanded: boolean = true;
  
  // Control del panel de Author Details (colapsado por defecto)
  authorDetailsExpanded: boolean = false;
  
  private removedParticipant: ParticipantDTO | null = null;
  private removedIndex: number = -1;
  
  // Institutions Summary
  institutionSummary: InstitutionSummaryItem[] = [];
  institutionIndexById: Map<number, number> = new Map();
  private calculateInstitutionSummaryTimeout: any = null;

  constructor(
    private catalogService: CatalogService,
    private snackBar: MatSnackBar
  ) {}
  
  ngOnInit(): void {
    console.log('ParticipantManagerComponent initialized with participants:', this.participants);
    console.log('ProductType on init:', this.productType);
    // Solo cargar si tenemos productType, de lo contrario esperar a ngOnChanges
    if (this.productType) {
      this.loadParticipationTypes();
    }
    this.emitIncompleteStatus();
    // Inicializar todos los participantes como colapsados
    // No expandir automáticamente al cargar
    this.expandedParticipants.clear();
    this.allExpanded = false;
    // Calcular el resumen de instituciones al inicializar
    this.calculateInstitutionSummary();
  }
  
  loadParticipationTypes(): void {
    if (!this.productType) {
      console.warn('Cannot load participation types: productType is not set');
      return;
    }

    this.loadingParticipationTypes = true;
    this.participationTypesError = false;
    
    console.log('Loading participation types for productType:', this.productType);
    this.catalogService.getParticipationTypes(this.productType).subscribe({
      next: (types) => {
        console.log('Participation types loaded:', types);
        this.participationTypes = types;
        this.loadingParticipationTypes = false;
        
        // Actualizar participationTypeName para participantes que ya tienen participationTypeId
        // pero no tienen participationTypeName (por ejemplo, cuando vienen del preview)
        this.participants.forEach(participant => {
          if (participant.participationTypeId && participant.participationTypeId > 0 && !participant.participationTypeName) {
            const type = this.participationTypes.find(t => t.id === participant.participationTypeId);
            if (type) {
              participant.participationTypeName = type.descripcion || type.nombre;
            }
          }
        });
        
        // Emitir cambios si se actualizaron nombres
        if (this.participants.some(p => p.participationTypeId && p.participationTypeId > 0 && !p.participationTypeName)) {
          this.participantsChange.emit([...this.participants]);
        }
      },
      error: (error) => {
        console.error('Error loading participation types:', error);
        console.error('Error details:', error);
        this.participationTypesError = true;
        this.loadingParticipationTypes = false;
      }
    });
  }
  
  refreshTypes(): void {
    this.catalogService.refreshParticipationTypes();
    this.loadParticipationTypes();
  }
  
  onResearcherSelected(researcher: RRHHDTO): void {
    // Validar duplicados
    if (this.isDuplicate(researcher)) {
      this.snackBar.open(
        `${researcher.fullname || 'This researcher'} is already in the team`,
        'Close',
        {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['warn-snackbar']
        }
      );
      return;
    }

    const defaultType = this.participationTypes[0];
    
    const newParticipant: ParticipantDTO = {
      rrhhId: researcher.id || 0,
      fullName: researcher.fullname || '',
      idRecurso: researcher.idRecurso,
      orcid: researcher.orcid,
      participationTypeId: defaultType?.id || 0,
      participationTypeName: defaultType?.descripcion || defaultType?.nombre,
      corresponding: false,
      order: this.participants.length + 1
    };
    
    this.participants = [...this.participants, newParticipant];
    // Reordenar para actualizar los números de orden
    this.participants.forEach((p, index) => {
      p.order = index + 1;
    });
    // NO expandir automáticamente el nuevo participante
    // El usuario debe hacer clic para expandirlo
    this.updateAllExpandedState();
    this.participantsChange.emit(this.participants);
    this.emitIncompleteStatus();
  }

  isDuplicate(researcher: RRHHDTO): boolean {
    const normalizedName = this.normalizeName(researcher.fullname || '');
    const normalizedOrcid = this.normalizeOrcid(researcher.orcid);
    const researcherId = researcher.id;

    return this.participants.some(participant => {
      // Comparar por ID (más confiable)
      if (researcherId && participant.rrhhId === researcherId) {
        return true;
      }

      // Comparar por ORCID (si ambos tienen)
      if (normalizedOrcid && participant.orcid) {
        const participantOrcid = this.normalizeOrcid(participant.orcid);
        if (normalizedOrcid === participantOrcid) {
          return true;
        }
      }

      // Comparar por nombre normalizado (último recurso)
      const participantName = this.normalizeName(participant.fullName);
      if (normalizedName && participantName && normalizedName === participantName) {
        return true;
      }

      return false;
    });
  }

  normalizeName(name: string): string {
    if (!name) return '';
    return name
      .toLowerCase()
      .trim()
      .replace(/\s+/g, ' ') // Múltiples espacios a uno solo
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, ''); // Remover acentos
  }

  normalizeOrcid(orcid: string | null | undefined): string | null {
    if (!orcid) return null;
    // Remover guiones y espacios, convertir a mayúsculas
    return orcid.replace(/[-\s]/g, '').toUpperCase();
  }
  
  onRoleChange(participantIndex: number, participationTypeId: number): void {
    console.log('onRoleChange', participantIndex, participationTypeId, this.participationTypes);
    const selectedType = this.participationTypes.find(type => type.id === participationTypeId);
    
    this.participants[participantIndex] = {
      ...this.participants[participantIndex],
      participationTypeId: participationTypeId,
      participationTypeName: selectedType?.descripcion || selectedType?.nombre
    };
    
    
    this.participantsChange.emit([...this.participants]);
    this.emitIncompleteStatus();
  }
  
  onCorrespondingChange(participantIndex: number, isCorresponding: boolean): void {
    this.participants[participantIndex].corresponding = isCorresponding;
    this.participantsChange.emit([...this.participants]);
  }
  
  /**
   * Maneja el cambio de acción del preview (link/create)
   */
  onPreviewActionChange(participantIndex: number, action: 'link' | 'create' | 'matched'): void {
    const participant = this.participants[participantIndex];
    participant.previewAction = action;
    
    // Actualizar estado de resolución según la acción
    if (action === 'link') {
      participant.resolutionType = 'linkExisting';
      participant.resolutionStatus = 'linkExisting'; // Mantener para compatibilidad
      participant.resolved = false; // No resuelto hasta que seleccione un RRHH
      participant.rrhhId = 0; // Limpiar para que el usuario seleccione uno nuevo
      participant.linkedRrhhId = undefined;
      participant.newRrhhData = undefined;
    } else if (action === 'create') {
      participant.resolutionType = 'createNew';
      participant.resolutionStatus = 'createNew'; // Mantener para compatibilidad
      participant.resolved = true; // Resuelto inmediatamente al seleccionar create
      // Usar el nombre original de OpenAlex si está disponible, sino usar el nombre actual
      const originalName = participant.previewOriginalName || participant.fullName;
      // Usar ORCID de OpenAlex si está disponible
      const originalOrcid = participant.orcidFromOpenAlex || participant.orcid;
      participant.newRrhhData = {
        name: originalName,
        orcid: originalOrcid
      };
      participant.linkedRrhhId = undefined;
      // Limpiar el rrhhId cuando se cambia a create
      participant.rrhhId = 0;
    } else if (action === 'matched') {
      participant.resolutionType = 'linkExisting';
      participant.resolutionStatus = 'matched'; // Mantener para compatibilidad
      participant.resolved = true; // Ya está resuelto (matched)
    }
    
    this.participantsChange.emit([...this.participants]);
    this.emitIncompleteStatus();
  }
  
  /**
   * Maneja el cambio de acción de ORCID
   */
  onOrcidActionChange(participantIndex: number, action: 'none' | 'add' | 'replace' | 'unlink'): void {
    if (participantIndex >= 0 && participantIndex < this.participants.length) {
      this.participants[participantIndex].orcidChangeAction = action;
      
      // Si elige "unlink", marcar como no resuelto
      if (action === 'unlink') {
        this.participants[participantIndex].resolved = false;
        this.participants[participantIndex].resolutionType = 'unresolved';
        this.participants[participantIndex].resolutionStatus = 'unresolved';
        this.participants[participantIndex].rrhhId = 0;
        this.participants[participantIndex].previewStatus = 'new';
        this.participants[participantIndex].previewAction = 'create';
      }
      
      this.participantsChange.emit([...this.participants]);
      this.emitIncompleteStatus();
    }
  }
  
  /**
   * Maneja la selección de un investigador existente para linkear
   */
  onLinkResearcherSelected(participantIndex: number, researcher: RRHHDTO | null): void {
    const participant = this.participants[participantIndex];
    
    if (researcher && researcher.id) {
      participant.rrhhId = researcher.id;
      participant.fullName = researcher.fullname || '';
      participant.orcid = researcher.orcid;
      participant.previewAction = 'link';
      participant.resolutionType = 'linkExisting';
      participant.resolutionStatus = 'linkExisting'; // Mantener para compatibilidad
      participant.resolved = true; // Resuelto al seleccionar un RRHH
      participant.linkedRrhhId = researcher.id;
      participant.newRrhhData = undefined;
    } else {
      // Si se limpia la selección, volver a unresolved
      participant.rrhhId = 0;
      participant.previewAction = 'link';
      participant.resolutionType = 'unresolved';
      participant.resolutionStatus = 'unresolved'; // Mantener para compatibilidad
      participant.resolved = false;
      participant.linkedRrhhId = undefined;
      participant.newRrhhData = undefined;
    }
    
    this.participantsChange.emit([...this.participants]);
    this.emitIncompleteStatus();
  }
  
  /**
   * Maneja el clic en un autor del summary
   */
  onAuthorSummaryClick(participant: ParticipantDTO, index: number): void {
    // Expandir el panel de Author Details si está colapsado
    if (!this.authorDetailsExpanded) {
      this.authorDetailsExpanded = true;
    }
    
    // Expandir el panel del participante específico
    this.expandedParticipants.add(index);
    
    // Hacer scroll suave hasta la card del autor
    // Usar un timeout más largo para asegurar que el DOM se haya actualizado
    setTimeout(() => {
      const panels = this.participantPanels.toArray();
      const panel = panels[index];
      
      // Verificar que el panel exista y tenga nativeElement
      if (panel && panel.nativeElement) {
        try {
          panel.nativeElement.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center' 
          });
          
          // Agregar clase de highlight
          const panelElement = panel.nativeElement;
          panelElement.classList.add('author-highlight');
          
          // Remover la clase después de 2 segundos
          setTimeout(() => {
            if (panelElement && panelElement.classList) {
              panelElement.classList.remove('author-highlight');
            }
          }, 2000);
        } catch (error) {
          console.warn('Error scrolling to participant panel:', error);
        }
      } else {
        // Si el panel no está disponible, intentar de nuevo después de un delay más largo
        setTimeout(() => {
          const retryPanels = this.participantPanels.toArray();
          const retryPanel = retryPanels[index];
          if (retryPanel && retryPanel.nativeElement) {
            try {
              retryPanel.nativeElement.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center' 
              });
              
              const panelElement = retryPanel.nativeElement;
              panelElement.classList.add('author-highlight');
              
              setTimeout(() => {
                if (panelElement && panelElement.classList) {
                  panelElement.classList.remove('author-highlight');
                }
              }, 2000);
            } catch (error) {
              console.warn('Error scrolling to participant panel on retry:', error);
            }
          }
        }, 300);
      }
    }, 100);
  }
  
  /**
   * Obtiene las afiliaciones del preview para un participante específico
   */
  getPreviewAffiliationsForParticipant(participant: ParticipantDTO): import('../../../core/models/backend-dtos').AffiliationPreviewDTO[] | undefined {
    if (!this.previewAuthors || !participant.previewOpenAlexId) {
      return undefined;
    }
    const previewAuthor = this.previewAuthors.find(a => a.openAlexId === participant.previewOpenAlexId);
    return previewAuthor?.affiliations;
  }
  
  removeParticipant(participantIndex: number): void {
    const participant = this.participants[participantIndex];
    this.removedParticipant = { ...participant };
    this.removedIndex = participantIndex;
    
    // Remover inmediatamente
    this.participants.splice(participantIndex, 1);
    // Remover del estado expandido
    this.expandedParticipants.delete(participantIndex);
    // Actualizar índices de los participantes expandidos después del índice removido
    const newExpandedSet = new Set<number>();
    this.expandedParticipants.forEach(index => {
      if (index < participantIndex) {
        newExpandedSet.add(index);
      } else if (index > participantIndex) {
        newExpandedSet.add(index - 1);
      }
    });
    this.expandedParticipants = newExpandedSet;
    this.updateAllExpandedState();
    // Reordenar los participantes restantes
    this.participants.forEach((p, index) => {
      p.order = index + 1;
    });
    this.participantsChange.emit([...this.participants]);

    // Mostrar snackbar con opción de undo
    const snackBarRef = this.snackBar.open(
      `${participant.fullName} removed`,
      'Undo',
      {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top'
      }
    );

    snackBarRef.onAction().subscribe(() => {
      this.undoRemove();
    });

    // Si se cierra sin acción, limpiar referencias
    snackBarRef.afterDismissed().subscribe(() => {
      if (this.removedParticipant) {
        this.removedParticipant = null;
        this.removedIndex = -1;
      }
    });
  }

  undoRemove(): void {
    if (this.removedParticipant && this.removedIndex >= 0) {
      // Restaurar el participante en su posición original
      this.participants.splice(this.removedIndex, 0, this.removedParticipant);
      // Reordenar todos los participantes
      this.participants.forEach((p, index) => {
        p.order = index + 1;
      });
      // Restaurar el estado expandido
      this.expandedParticipants.add(this.removedIndex);
      this.updateAllExpandedState();
      this.participantsChange.emit([...this.participants]);
      this.emitIncompleteStatus();
      this.removedParticipant = null;
      this.removedIndex = -1;
    }
  }

  moveParticipant(event: CdkDragDrop<ParticipantDTO[]>): void {
    moveItemInArray(this.participants, event.previousIndex, event.currentIndex);
    
    // Actualizar el estado expandido después del reordenamiento
    const wasPreviousExpanded = this.expandedParticipants.has(event.previousIndex);
    const wasCurrentExpanded = this.expandedParticipants.has(event.currentIndex);
    
    // Remover ambos índices
    this.expandedParticipants.delete(event.previousIndex);
    this.expandedParticipants.delete(event.currentIndex);
    
    // Agregar en las nuevas posiciones
    if (wasPreviousExpanded) {
      this.expandedParticipants.add(event.currentIndex);
    }
    if (wasCurrentExpanded) {
      this.expandedParticipants.add(event.previousIndex);
    }
    
    this.updateAllExpandedState();
    
    // Actualizar los números de orden
    this.participants.forEach((p, index) => {
      p.order = index + 1;
    });
    this.participantsChange.emit([...this.participants]);
    this.emitIncompleteStatus();
  }

  moveUp(index: number): void {
    if (index > 0) {
      const temp = this.participants[index];
      this.participants[index] = this.participants[index - 1];
      this.participants[index - 1] = temp;
      // Actualizar los números de orden
      this.participants.forEach((p, i) => {
        p.order = i + 1;
      });
      this.participantsChange.emit([...this.participants]);
      this.emitIncompleteStatus();
    }
  }

  moveDown(index: number): void {
    if (index < this.participants.length - 1) {
      const temp = this.participants[index];
      this.participants[index] = this.participants[index + 1];
      this.participants[index + 1] = temp;
      // Actualizar los números de orden
      this.participants.forEach((p, i) => {
        p.order = i + 1;
      });
      this.participantsChange.emit([...this.participants]);
      this.emitIncompleteStatus();
    }
  }
  
  compareParticipationTypes(type1: any, type2: any): boolean {
    return type1 === type2;
  }
  
  isTypeApplicable(type: TipoParticipacionDTO): boolean {
    // Si aplica a todos, retornar true
    if (type.aplicableProductos === 'ALL') {
      return true;
    }
    
    // Normalizar valores para comparación (a mayúsculas, sin guiones, sin espacios)
    const aplicableNormalized = (type.aplicableProductos || '').toUpperCase().trim().replace(/-/g, '_').replace(/\s+/g, '_');
    const productTypeNormalized = (this.productType || '').toUpperCase().trim().replace(/-/g, '_').replace(/\s+/g, '_');
    
    // Mapeo del productType del frontend (con guiones) a posibles valores del backend
    // Frontend usa: 'publications', 'thesis-students', 'scientific-collaborations', etc.
    // Backend puede enviar: 'PUBLICATION', 'PUBLICATIONS', 'THESIS', etc.
    const productTypeMappings: { [key: string]: string[] } = {
      'PUBLICATIONS': ['PUBLICATIONS', 'PUBLICATION'],
      'THESIS_STUDENTS': ['THESIS_STUDENTS', 'THESIS_STUDENT', 'THESIS'],
      'SCIENTIFIC_COLLABORATIONS': ['SCIENTIFIC_COLLABORATIONS', 'COLLABORATIONS', 'COLLABORATION'],
      'OUTREACH_ACTIVITIES': ['OUTREACH_ACTIVITIES', 'ACTIVITIES', 'ACTIVITY'],
      'POSTDOCTORAL_FELLOWS': ['POSTDOCTORAL_FELLOWS', 'FELLOWS', 'FELLOW'],
      'TECHNOLOGY_TRANSFER': ['TECHNOLOGY_TRANSFER', 'TECHNOLOGY_TRANSFERS', 'TRANSFERS', 'TRANSFER'],
      'SCIENTIFIC_EVENTS': ['SCIENTIFIC_EVENTS', 'EVENTS', 'EVENT']
    };
    
    // Obtener variantes posibles del productType del frontend
    const productTypeVariants = productTypeMappings[productTypeNormalized] || [productTypeNormalized];
    
    // Verificar si aplicableProductos coincide con alguna variante del productType
    // Comparar tanto exacto como con includes para mayor flexibilidad
    return productTypeVariants.some(variant => {
      const variantUpper = variant.toUpperCase();
      const aplicableUpper = aplicableNormalized.toUpperCase();
      return aplicableUpper === variantUpper || 
             aplicableUpper.includes(variantUpper) || 
             variantUpper.includes(aplicableUpper);
    });
  }
  
  getSelectedTypeDescription(participationTypeId: number): string {
    const type = this.participationTypes.find(t => t.id === participationTypeId);
    return type?.descripcion || '';
  }
  
  canBeCorresponding(participationTypeId: number): boolean {
    if (this.productType !== 'publications') return false;
    
    const type = this.participationTypes.find(t => t.id === participationTypeId);
    return type?.puedeSerCorresponding || false;
  }
  
  getCorrespondingTooltip(participationTypeId: number): string {
    return 'Mark as corresponding author';
  }
  
  scrollToSearch(): void {
    if (this.searchSection) {
      this.searchSection.nativeElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'start' 
      });
      // Enfocar el campo de búsqueda después de un pequeño delay
      setTimeout(() => {
        const searchInput = this.searchSection.nativeElement.querySelector('input');
        if (searchInput) {
          searchInput.focus();
        }
      }, 300);
    }
  }

  trackByParticipant(index: number, participant: ParticipantDTO): number {
    return participant.rrhhId || index;
  }

  /**
   * Helper to decide if a participant has affiliations
   */
  hasAffiliations(participant: ParticipantDTO): boolean {
    // Verificar si tiene afiliaciones en la lista
    if (participant.affiliations && Array.isArray(participant.affiliations) && participant.affiliations.length > 0) {
      return true;
    }
    
    // Verificar si el estado marca 'complete'
    if (participant.affiliationsStatus === 'complete') {
      return true;
    }
    
    // Si tiene idRRHHProducto y publicationId, asumimos que puede tener afiliaciones
    // (se cargarán desde el backend en affiliation-manager)
    // Pero no podemos asumir que tiene afiliaciones solo por tener idRRHHProducto
    // porque puede no tener ninguna afiliación asignada
    
    return false;
  }
  
  /**
   * Verifica si un participante tiene afiliaciones con instituciones incompletas
   * (tienen nombreInstitucion pero no idInstitucion)
   */
  hasIncompleteInstitutions(participant: ParticipantDTO): boolean {
    if (!participant.affiliations || !Array.isArray(participant.affiliations)) {
      return false;
    }
    
    return participant.affiliations.some(aff => {
      // Si tiene nombreInstitucion pero no idInstitucion, la institución está incompleta
      return aff.nombreInstitucion && !aff.idInstitucion;
    });
  }
  
  /**
   * Maneja el cambio de afiliaciones desde affiliation-manager
   */
  onAffiliationsChange(participantIndex: number, affiliations: import('../../../core/models/backend-dtos').AfiliacionDTO[]): void {
    if (participantIndex >= 0 && participantIndex < this.participants.length) {
      // Verificar si hay afiliaciones con instituciones incompletas (sin idInstitucion pero con nombreInstitucion)
      // Esto indica que la institución no está vinculada y necesita ser seleccionada
      const hasIncompleteInstitutions = affiliations && affiliations.some(aff => {
        // Si tiene nombreInstitucion pero no idInstitucion, significa que la institución no está vinculada
        return aff.nombreInstitucion && !aff.idInstitucion;
      });
      
      // Actualizar las afiliaciones del participante
      this.participants[participantIndex] = {
        ...this.participants[participantIndex],
        affiliations: affiliations,
        // Marcar como incompleto si no hay afiliaciones, o si hay afiliaciones con instituciones incompletas
        affiliationsStatus: (!affiliations || affiliations.length === 0 || hasIncompleteInstitutions) ? 'incomplete' : 'complete'
      };
      
      // Recalcular el resumen de instituciones
      this.calculateInstitutionSummary();
      
      // Emitir el cambio
      this.participantsChange.emit([...this.participants]);
      this.emitIncompleteStatus();
    }
  }
  
  /**
   * Calcula el resumen de instituciones únicas a partir de todas las afiliaciones de todos los participantes
   * Solo para publicaciones (productType === 'publications')
   * Usa setTimeout para asegurar que la actualización ocurra en el siguiente ciclo del event loop
   */
  calculateInstitutionSummary(): void {
    // Usar setTimeout para asegurar que la actualización ocurra en el siguiente ciclo
    // Esto evita ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this._calculateInstitutionSummaryInternal();
    }, 0);
  }

  /**
   * Implementación interna de calculateInstitutionSummary
   */
  private _calculateInstitutionSummaryInternal(): void {
    // Solo calcular para publicaciones
    if (this.productType !== 'publications') {
      this.institutionSummary = [];
      this.institutionIndexById.clear();
      return;
    }
    
    const institutionMap = new Map<number, {
      id: number;
      name: string;
      authorIds: Set<number>;
    }>();
    
    // Recorrer todas las afiliaciones de todos los participantes
    this.participants.forEach(participant => {
      if (participant.affiliations && Array.isArray(participant.affiliations)) {
        participant.affiliations.forEach(affiliation => {
          if (affiliation.idInstitucion) {
            const institutionId = affiliation.idInstitucion;
            
            if (!institutionMap.has(institutionId)) {
              // Obtener el nombre de la institución
              const institutionName = affiliation.nombreInstitucion || `Institution ${institutionId}`;
              
              institutionMap.set(institutionId, {
                id: institutionId,
                name: institutionName,
                authorIds: new Set<number>()
              });
            }
            
            // Agregar el autor a la lista de autores de esta institución
            const institution = institutionMap.get(institutionId)!;
            institution.authorIds.add(participant.rrhhId);
          }
        });
      }
    });
    
    // Convertir el Map a un array y asignar índices
    this.institutionSummary = Array.from(institutionMap.values())
      .sort((a, b) => a.name.localeCompare(b.name)) // Ordenar alfabéticamente por nombre
      .map((institution, index) => ({
        id: institution.id,
        index: index + 1,
        name: institution.name,
        countAuthors: institution.authorIds.size
      }));
    
    // Crear el mapa de índice por ID para búsqueda rápida
    this.institutionIndexById.clear();
    this.institutionSummary.forEach(item => {
      this.institutionIndexById.set(item.id, item.index);
    });
  }

  /**
   * Debounced version of calculateInstitutionSummary
   * Agrupa múltiples actualizaciones rápidas en una sola
   */
  private debouncedCalculateInstitutionSummary(): void {
    // Limpiar timeout anterior si existe
    if (this.calculateInstitutionSummaryTimeout) {
      clearTimeout(this.calculateInstitutionSummaryTimeout);
    }
    
    // Programar la actualización después de un pequeño delay
    // El delay más largo ayuda a agrupar todas las actualizaciones durante la carga del DOI
    this.calculateInstitutionSummaryTimeout = setTimeout(() => {
      this._calculateInstitutionSummaryInternal();
      this.calculateInstitutionSummaryTimeout = null;
    }, 100);
  }
  
  /**
   * Obtiene los índices de instituciones para un autor específico
   * Solo para publicaciones (productType === 'publications')
   */
  getInstitutionIndicesForAuthor(participant: ParticipantDTO): number[] {
    // Solo retornar índices para publicaciones
    if (this.productType !== 'publications') {
      return [];
    }
    
    if (!participant.affiliations || !Array.isArray(participant.affiliations)) {
      return [];
    }
    
    const indices = new Set<number>();
    
    participant.affiliations.forEach(affiliation => {
      if (affiliation.idInstitucion) {
        const index = this.institutionIndexById.get(affiliation.idInstitucion);
        if (index !== undefined) {
          indices.add(index);
        }
      }
    });
    
    // Retornar los índices ordenados
    return Array.from(indices).sort((a, b) => a - b);
  }

  isParticipantIncomplete(participant: ParticipantDTO): boolean {
    // Verificar que tenga rol asignado
    const hasRole = participant.participationTypeId && participant.participationTypeId > 0;
    
    if (!hasRole) {
      return true;
    }
    
    // Verificar resolución para autores externos (preview)
    if (participant.previewStatus === 'new' || participant.previewStatus === 'review') {
      const isExternal = !participant.rrhhId || participant.rrhhId === 0;
      if (isExternal) {
        // Verificar resolución de RRHH
        let rrhhResolved = false;
        if (participant.resolved !== undefined) {
          rrhhResolved = participant.resolved;
        } else {
          // Lógica de compatibilidad con resolutionStatus/resolutionType
          const resolutionType = participant.resolutionType || participant.resolutionStatus;
          rrhhResolved = !!(resolutionType && resolutionType !== 'unresolved' && 
                        (resolutionType === 'createNew' || 
                         (resolutionType === 'linkExisting' && participant.linkedRrhhId && participant.linkedRrhhId > 0)));
        }
        
        if (!rrhhResolved) {
          return true; // RRHH no resuelto
        }
        
        // Verificar resolución de afiliaciones (solo para publicaciones)
        if (this.productType === 'publications' && participant.affiliationsStatus === 'incomplete') {
          return true; // Afiliaciones incompletas
        }
      }
    }

    // Para publicaciones, también verificar que tenga idRRHHProducto (significa que fue guardado)
    // y si tiene idRRHHProducto, asumimos que puede tener afiliaciones
    // Nota: La verificación real de afiliaciones se hace en el componente hijo (affiliation-manager)
    // Aquí solo verificamos que tenga el rol y que esté guardado (tenga idRRHHProducto)
    if (this.productType === 'publications') {
      // Si no tiene idRRHHProducto, está incompleto porque no ha sido guardado aún
      // Si tiene idRRHHProducto, asumimos que puede tener afiliaciones (se verifica en el backend)
      const missingRRHHProducto = !!(this.publicationId && !participant.idRRHHProducto);
      return !hasRole || missingRRHHProducto;
    }

    // Para otros tipos de productos, solo verificar rol
    return !hasRole;
  }

  hasIncompleteParticipants(): boolean {
    return this.participants.some(p => this.isParticipantIncomplete(p));
  }

  emitIncompleteStatus(): void {
    this.hasIncompleteChange.emit(this.hasIncompleteParticipants());
  }

  capitalizeText(text: string | undefined): string {
    if (!text) return '';
    // Capitalizar primera letra de cada palabra
    return text.split(' ').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
  }

  // Métodos para expand/collapse
  toggleParticipant(index: number): void {
    // Toggle del estado de expansión del participante
    if (this.expandedParticipants.has(index)) {
      this.expandedParticipants.delete(index);
    } else {
      this.expandedParticipants.add(index);
    }
    this.updateAllExpandedState();
  }

  onParticipantPanelOpened(index: number): void {
    // Sincronizar el estado cuando el panel se abre
    this.expandedParticipants.add(index);
    this.updateAllExpandedState();
  }

  onParticipantPanelClosed(index: number): void {
    // Sincronizar el estado cuando el panel se cierra
    this.expandedParticipants.delete(index);
    this.updateAllExpandedState();
  }

  isParticipantExpanded(index: number): boolean {
    // Los participantes NO se expanden automáticamente
    // Solo se expanden si están explícitamente en el Set
    return this.expandedParticipants.has(index);
  }

  expandAll(): void {
    this.participants.forEach((_, index) => {
      this.expandedParticipants.add(index);
    });
    this.allExpanded = true;
  }

  collapseAll(): void {
    this.expandedParticipants.clear();
    this.allExpanded = false;
  }

  toggleExpandAll(): void {
    if (this.allExpanded) {
      this.collapseAll();
    } else {
      this.expandAll();
    }
  }

  private updateAllExpandedState(): void {
    this.allExpanded = this.participants.length > 0 && 
                       this.expandedParticipants.size === this.participants.length;
  }

  getParticipantRoleName(participant: ParticipantDTO): string {
    if (participant.participationTypeName) {
      return this.capitalizeText(participant.participationTypeName);
    }
    const type = this.participationTypes.find(t => t.id === participant.participationTypeId);
    return type ? this.capitalizeText(type.descripcion || type.nombre) : 'No role';
  }

  onAuthorDetailsOpened(): void {
    this.authorDetailsExpanded = true;
  }

  onAuthorDetailsClosed(): void {
    this.authorDetailsExpanded = false;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['participants']) {
      console.log('Participants changed in ParticipantManagerComponent:', changes['participants'].currentValue);
      this.emitIncompleteStatus();
      // Actualizar estado de expand cuando cambian los participantes
      this.updateAllExpandedState();
      // Recalcular el resumen de instituciones cuando cambien los participantes
      // Si estamos cargando desde DOI, usar debounce para evitar múltiples actualizaciones
      if (this.doiLoading) {
        this.debouncedCalculateInstitutionSummary();
      } else {
        this.calculateInstitutionSummary();
      }
    }
    if (changes['productType']) {
      console.log('ProductType changed:', changes['productType'].currentValue, 'Previous:', changes['productType'].previousValue);
      if (changes['productType'].currentValue) {
        this.loadParticipationTypes();
      }
    }
    // Si doiLoading cambió de true a false, actualizar el resumen una vez más
    if (changes['doiLoading'] && changes['doiLoading'].previousValue === true && changes['doiLoading'].currentValue === false) {
      this.calculateInstitutionSummary();
    }
  }

  ngOnDestroy(): void {
    // Limpiar timeout de debounce si existe
    if (this.calculateInstitutionSummaryTimeout) {
      clearTimeout(this.calculateInstitutionSummaryTimeout);
      this.calculateInstitutionSummaryTimeout = null;
    }
  }
}
