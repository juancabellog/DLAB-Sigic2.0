import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { trigger, transition, style, animate } from '@angular/animations';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatRadioModule } from '@angular/material/radio';

import { AfiliacionDTO, InstitucionDTO } from '../../../core/models/backend-dtos';
import { GenericCatalogService } from '../../../core/services/generic-catalog.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { MessageService } from '../../../core/services/message.service';
import { InstitutionSearchComponent } from '../institution-search/institution-search.component';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-affiliation-manager',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DragDropModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
    MatRadioModule,
    InstitutionSearchComponent
  ],
  templateUrl: './affiliation-manager.component.html',
  styleUrls: ['./affiliation-manager.component.scss'],
  animations: [
    trigger('slideFadeIn', [
      transition(':enter', [
        style({ 
          opacity: 0, 
          transform: 'translateY(-10px)',
          height: 0,
          marginTop: 0,
          marginBottom: 0
        }),
        animate('300ms cubic-bezier(0.4, 0, 0.2, 1)', style({ 
          opacity: 1, 
          transform: 'translateY(0)',
          height: '*',
          marginTop: '*',
          marginBottom: '*'
        }))
      ]),
      transition(':leave', [
        animate('200ms cubic-bezier(0.4, 0, 0.2, 1)', style({ 
          opacity: 0, 
          transform: 'translateY(-10px)',
          height: 0,
          marginTop: 0,
          marginBottom: 0
        }))
      ])
    ])
  ]
})
export class AffiliationManagerComponent implements OnInit, OnChanges, AfterViewChecked {
  @Input() publicationId!: number;
  @Input() rrhhId!: number;
  @Input() rrhhProductoId!: number;
  @Input() researcherName: string = '';
  @Input() previewAffiliations?: import('../../../core/models/backend-dtos').AffiliationPreviewDTO[]; // Datos del preview desde DOI
  
  @Output() affiliationsChange = new EventEmitter<AfiliacionDTO[]>();

  @ViewChild('formCard', { read: ElementRef }) formCard!: ElementRef;

  affiliations: AfiliacionDTO[] = [];
  institutions: InstitucionDTO[] = [];
  loading: boolean = false;
  loadingInstitutions: boolean = false;
  expanded: boolean = false;
  
  // Control de expansión de texto por click
  expandedAffiliationTexts: Set<number> = new Set();

  // Formulario para nueva/edición de afiliación
  editingAffiliation: AfiliacionDTO | null = null;
  showForm: boolean = false; // Controla si el formulario está visible
  showInstitutionError: boolean = false; // Controla si se muestra error de validación
  formAffiliation: Partial<AfiliacionDTO> = {
    idInstitucion: undefined,
    texto: ''
  };
  // Variable para almacenar el objeto de institución completo para el componente institution-search
  selectedInstitutionForForm: InstitucionDTO | null = null;

  // Variables para undo de eliminación
  private removedAffiliation: AfiliacionDTO | null = null;
  private removedIndex: number = -1;
  
  // Variables para preview
  currentPreviewAffiliation: import('../../../core/models/backend-dtos').AffiliationPreviewDTO | null = null;
  currentPreviewAction: 'link' | 'create' | 'matched' = 'link';

  constructor(
    private baseHttp: BaseHttpService,
    private catalogService: GenericCatalogService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadInstitutions();
    // Si hay previewAffiliations, inicializar las afiliaciones desde el preview
    if (this.previewAffiliations && this.previewAffiliations.length > 0) {
      this.initializeAffiliationsFromPreview();
    } else if (this.publicationId && this.publicationId > 0 && 
               this.rrhhId && this.rrhhId > 0 && 
               this.rrhhProductoId && this.rrhhProductoId > 0) {
      console.log('Affiliation manager: OnInit - Loading affiliations', {
        publicationId: this.publicationId,
        rrhhId: this.rrhhId,
        rrhhProductoId: this.rrhhProductoId
      });
      this.loadAffiliations();
    } else {
      console.log('Affiliation manager: OnInit - Missing required values', {
        publicationId: this.publicationId,
        rrhhId: this.rrhhId,
        rrhhProductoId: this.rrhhProductoId
      });
    }
  }
  
  ngOnChanges(changes: SimpleChanges): void {
    // Detectar cambios en los inputs críticos para cargar afiliaciones
    const publicationIdChanged = changes['publicationId'] && 
      changes['publicationId'].currentValue !== changes['publicationId'].previousValue;
    const rrhhIdChanged = changes['rrhhId'] && 
      changes['rrhhId'].currentValue !== changes['rrhhId'].previousValue;
    const rrhhProductoIdChanged = changes['rrhhProductoId'] && 
      changes['rrhhProductoId'].currentValue !== changes['rrhhProductoId'].previousValue;
    const previewAffiliationsChanged = changes['previewAffiliations'] && 
      changes['previewAffiliations'].currentValue !== changes['previewAffiliations'].previousValue;
    
    // Verificar si tenemos todos los valores necesarios para cargar afiliaciones
    const hasAllRequiredValues = this.publicationId && this.publicationId > 0 && 
                                  this.rrhhId && this.rrhhId > 0 && 
                                  this.rrhhProductoId && this.rrhhProductoId > 0;
    
    // Si cambió alguno de los inputs críticos y tenemos todos los valores necesarios
    if ((publicationIdChanged || rrhhIdChanged || rrhhProductoIdChanged) && hasAllRequiredValues) {
      console.log('Affiliation manager: Inputs changed, loading affiliations', {
        publicationId: this.publicationId,
        rrhhId: this.rrhhId,
        rrhhProductoId: this.rrhhProductoId
      });
      
      // Si hay previewAffiliations, inicializar desde el preview
      if (this.previewAffiliations && this.previewAffiliations.length > 0) {
        this.initializeAffiliationsFromPreview();
      } else {
        // Cargar afiliaciones desde el backend
        this.loadAffiliations();
      }
    }
    
    // Si cambió previewAffiliations y tenemos valores, inicializar desde preview
    if (previewAffiliationsChanged && this.previewAffiliations && this.previewAffiliations.length > 0) {
      this.initializeAffiliationsFromPreview();
    }
  }
  
  /**
   * Inicializa las afiliaciones desde el preview
   */
  private initializeAffiliationsFromPreview(): void {
    if (!this.previewAffiliations) {
      return;
    }
    
    this.affiliations = this.previewAffiliations.map((previewAff, index) => {
      const aff: AfiliacionDTO = {
        idRRHH: this.rrhhId,
        idProducto: this.publicationId,
        idRRHHProducto: this.rrhhProductoId,
        idInstitucion: previewAff.matchId, // Puede ser undefined si status === 'new'
        nombreInstitucion: previewAff.name,
        texto: previewAff.rawAffiliationString
      };
      return aff;
    });
    
    this.affiliationsChange.emit(this.affiliations);
  }
  
  /**
   * Obtiene el estado del preview para la afiliación actual en edición
   */
  getCurrentPreviewAffiliationStatus(): 'matched' | 'new' | 'review' | undefined {
    if (!this.editingAffiliation && !this.currentPreviewAffiliation) {
      return undefined;
    }
    
    if (this.currentPreviewAffiliation) {
      return this.currentPreviewAffiliation.status;
    }
    
    if (this.editingAffiliation) {
      return this.getPreviewStatusForAffiliation(this.editingAffiliation);
    }
    
    return undefined;
  }
  
  /**
   * Obtiene el nombre de la afiliación del preview actual
   */
  getCurrentPreviewAffiliationName(): string {
    if (this.currentPreviewAffiliation) {
      return this.currentPreviewAffiliation.name || '';
    }
    if (this.editingAffiliation) {
      const previewAff = this.getPreviewAffiliationFor(this.editingAffiliation);
      return previewAff?.name || '';
    }
    return '';
  }
  
  /**
   * Maneja el cambio de acción del preview para instituciones
   */
  onPreviewInstitutionActionChange(action: 'link' | 'create' | 'matched'): void {
    this.currentPreviewAction = action;
    if (action === 'create') {
      // Limpiar la selección de institución
      this.formAffiliation.idInstitucion = undefined;
      this.selectedInstitutionForForm = null;
    }
  }
  
  /**
   * Maneja la selección de una institución
   */
  onInstitutionSelected(institution: InstitucionDTO): void {
    if (institution && institution.id) {
      this.formAffiliation.idInstitucion = institution.id;
      this.selectedInstitutionForForm = institution;
      // Si estaba en modo 'create', cambiar a 'link' ya que ahora hay una institución seleccionada
      if (this.currentPreviewAction === 'create') {
        this.currentPreviewAction = 'link';
      }
      this.showInstitutionError = false; // Clear error when institution is selected
    } else {
      // Si se deselecciona la institución y estaba en modo preview 'new', volver a 'create'
      this.formAffiliation.idInstitucion = undefined;
      this.selectedInstitutionForForm = null;
      if (this.currentPreviewAffiliation && this.currentPreviewAffiliation.status === 'new') {
        this.currentPreviewAction = 'create';
      }
    }
  }

  loadInstitutions(): void {
    this.loadingInstitutions = true;
    this.catalogService.getInstitutions().pipe(
      catchError(error => {
        console.error('Error loading institutions:', error);
        this.messageService.error('Error loading institutions');
        return of([]);
      }),
      finalize(() => {
        this.loadingInstitutions = false;
      })
    ).subscribe(institutions => {
      this.institutions = institutions;
    });
  }

  loadAffiliations(): void {
    if (!this.publicationId || this.publicationId <= 0 || 
        !this.rrhhId || this.rrhhId <= 0 || 
        !this.rrhhProductoId || this.rrhhProductoId <= 0) {
      console.log('Affiliation manager: Missing required values to load affiliations', {
        publicationId: this.publicationId,
        rrhhId: this.rrhhId,
        rrhhProductoId: this.rrhhProductoId
      });
      return;
    }

    console.log('Affiliation manager: Loading affiliations', {
      publicationId: this.publicationId,
      rrhhId: this.rrhhId,
      rrhhProductoId: this.rrhhProductoId
    });

    this.loading = true;
    const url = `/publications/${this.publicationId}/participants/${this.rrhhId}/affiliations?rrhhProductoId=${this.rrhhProductoId}`;
    
    this.baseHttp.get<AfiliacionDTO[]>(url).pipe(
      catchError(error => {
        console.error('Error loading affiliations:', error);
        this.messageService.error('Error loading affiliations');
        return of([]);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(affiliations => {
      console.log('Affiliation manager: Loaded affiliations', affiliations);
      this.affiliations = affiliations;
      this.affiliationsChange.emit(this.affiliations);
    });
  }

  toggleExpanded(): void {
    this.expanded = !this.expanded;
    if (this.expanded && this.affiliations.length === 0) {
      this.loadAffiliations();
    }
  }

  onPanelOpened(): void {
    this.expanded = true;
    if (this.affiliations.length === 0) {
      this.loadAffiliations();
    }
  }

  onPanelClosed(): void {
    this.expanded = false;
  }

  startAdd(): void {
    console.log('startAdd', this.previewAffiliations );
    this.editingAffiliation = null; // null significa "agregar nueva"
    this.currentPreviewAffiliation = null;
    // Si hay previewAffiliations, usar la primera que no esté ya agregada
    if (this.previewAffiliations && this.previewAffiliations.length > 0) {
      const firstNew = this.previewAffiliations.find(pa => 
        !this.affiliations.some(a => 
          (a.idInstitucion && pa.matchId && a.idInstitucion === pa.matchId) ||
          (a.nombreInstitucion && pa.name && a.nombreInstitucion.toLowerCase() === pa.name.toLowerCase())
        )
      );
      if (firstNew) {
        this.currentPreviewAffiliation = firstNew;
        this.currentPreviewAction = firstNew.status === 'matched' ? 'matched' : (firstNew.status === 'new' ? 'create' : 'link');
        this.formAffiliation = {
          idInstitucion: firstNew.matchId,
          texto: firstNew.rawAffiliationString
        };
        // Convertir el ID de institución a objeto para el componente institution-search
        if (firstNew.matchId) {
          const institution = this.institutions.find(i => i.id === firstNew.matchId);
          this.selectedInstitutionForForm = institution ? {
            id: institution.id,
            idDescripcion: institution.idDescripcion,
            descripcion: institution.descripcion
          } : null;
        } else {
          this.selectedInstitutionForForm = null;
        }
      } else {
        this.formAffiliation = {
          idInstitucion: undefined,
          texto: ''
        };
        this.selectedInstitutionForForm = null;
        this.currentPreviewAction = 'link';
      }
      } else {
        this.formAffiliation = {
          idInstitucion: undefined,
          texto: ''
        };
        this.selectedInstitutionForForm = null;
        this.currentPreviewAction = 'link';
      }
    this.showForm = true; // Mostrar el formulario
    this.cdr.detectChanges();
  }

  ngAfterViewChecked(): void {
    // Scroll to form card when it becomes visible
    if (this.showForm && this.formCard) {
      setTimeout(() => {
        this.formCard.nativeElement.scrollIntoView({ 
          behavior: 'smooth', 
          block: 'nearest' 
        });
      }, 100);
    }
  }

  startEdit(affiliation: AfiliacionDTO): void {
    this.editingAffiliation = affiliation; // Objeto significa "editar existente"
    this.currentPreviewAffiliation = this.getPreviewAffiliationFor(affiliation) || null;
    if (this.currentPreviewAffiliation) {
      this.currentPreviewAction = this.currentPreviewAffiliation.status === 'matched' ? 'matched' : 
                                   (this.currentPreviewAffiliation.status === 'new' ? 'create' : 'link');
    } else {
      this.currentPreviewAction = 'link';
    }
    this.showForm = true; // Mostrar el formulario
    this.formAffiliation = {
      idInstitucion: affiliation.idInstitucion,
      texto: affiliation.texto || ''
    };
    // Convertir el ID de institución a objeto para el componente institution-search
    if (affiliation.idInstitucion) {
      const institution = this.institutions.find(i => i.id === affiliation.idInstitucion);
      if (institution) {
        this.selectedInstitutionForForm = {
          id: institution.id,
          idDescripcion: institution.idDescripcion,
          descripcion: institution.descripcion
        };
      } else {
        this.selectedInstitutionForForm = null;
      }
    } else {
      this.selectedInstitutionForForm = null;
    }
    this.cdr.detectChanges();
  }

  moveAffiliation(event: CdkDragDrop<AfiliacionDTO[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(this.affiliations, event.previousIndex, event.currentIndex);
    this.affiliationsChange.emit(this.affiliations);
  }

  cancelEdit(): void {
    this.editingAffiliation = null;
    this.showForm = false; // Ocultar el formulario
    this.showInstitutionError = false; // Resetear error de validación
    this.formAffiliation = {
      idInstitucion: undefined,
      texto: ''
    };
    this.selectedInstitutionForForm = null;
  }

  saveAffiliation(): void {
    // Validar si se está intentando crear una institución nueva (no implementado)
    // Solo bloquear si currentPreviewAction es 'create' Y no hay institución seleccionada
    if (this.currentPreviewAction === 'create' && !this.formAffiliation.idInstitucion) {
      this.messageService.error('Creating new institutions is not yet implemented. Please link to an existing institution.');
      return;
    }
    
    // Validación contextual: verificar que hay una institución seleccionada
    if (!this.formAffiliation.idInstitucion) {
      this.showInstitutionError = true;
      this.messageService.error('Please select an institution');
      return;
    }
    
    // Si hay una institución seleccionada, actualizar currentPreviewAction a 'link'
    // para permitir el guardado incluso si estaba en modo 'create'
    if (this.formAffiliation.idInstitucion && this.currentPreviewAction === 'create') {
      this.currentPreviewAction = 'link';
    }
    
    this.showInstitutionError = false; // Limpiar error si pasa validación

    // Si la publicación aún no existe (modo preview), actualizar solo localmente
    if (!this.publicationId || this.publicationId === 0 || !this.rrhhId || this.rrhhId === 0 || !this.rrhhProductoId || this.rrhhProductoId === 0) {
      // Modo preview: actualizar solo en el array local
      if (this.editingAffiliation) {
        // Actualizar afiliación existente en el array local
        const index = this.affiliations.findIndex(a => 
          a.id === this.editingAffiliation?.id || 
          (a.idInstitucion === this.editingAffiliation?.idInstitucion && 
           a.nombreInstitucion === this.editingAffiliation?.nombreInstitucion)
        );
        if (index >= 0) {
          this.affiliations[index] = {
            ...this.affiliations[index],
            idInstitucion: this.formAffiliation.idInstitucion,
            nombreInstitucion: this.selectedInstitutionForForm?.descripcion || this.selectedInstitutionForForm?.idDescripcion || '',
            texto: this.formAffiliation.texto || undefined
          };
          this.affiliationsChange.emit(this.affiliations);
          this.messageService.success('Affiliation updated');
          this.cancelEdit();
        }
      } else {
        // Agregar nueva afiliación al array local
        const newAffiliation: AfiliacionDTO = {
          idRRHH: this.rrhhId || 0,
          idProducto: this.publicationId || 0,
          idRRHHProducto: this.rrhhProductoId || 0,
          idInstitucion: this.formAffiliation.idInstitucion,
          nombreInstitucion: this.selectedInstitutionForForm?.descripcion || this.selectedInstitutionForForm?.idDescripcion || '',
          texto: this.formAffiliation.texto || undefined
        };
        this.affiliations.push(newAffiliation);
        this.affiliationsChange.emit(this.affiliations);
        this.messageService.success('Affiliation added');
        this.cancelEdit();
      }
      return;
    }

    const affiliationData: AfiliacionDTO = {
      idRRHH: this.rrhhId,
      idProducto: this.publicationId,
      idRRHHProducto: this.rrhhProductoId,
      idInstitucion: this.formAffiliation.idInstitucion,
      texto: this.formAffiliation.texto || undefined
    };

    this.loading = true;

    if (this.editingAffiliation) {
      // Actualizar afiliación existente
      const url = `/publications/${this.publicationId}/participants/${this.rrhhId}/affiliations/${this.editingAffiliation.id}?rrhhProductoId=${this.rrhhProductoId}`;
      
      this.baseHttp.put<AfiliacionDTO>(url, affiliationData).pipe(
        catchError(error => {
          console.error('Error updating affiliation:', error);
          this.messageService.error('Error updating affiliation');
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      ).subscribe(updated => {
        if (updated) {
          this.messageService.success('Affiliation updated successfully');
          this.loadAffiliations();
          this.cancelEdit();
        }
      });
    } else {
      // Crear nueva afiliación
      const url = `/publications/${this.publicationId}/participants/${this.rrhhId}/affiliations`;
      
      this.baseHttp.post<AfiliacionDTO>(url, affiliationData).pipe(
        catchError(error => {
          console.error('Error creating affiliation:', error);
          this.messageService.error('Error creating affiliation');
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      ).subscribe(created => {
        if (created) {
          this.messageService.success('Affiliation added successfully');
          this.loadAffiliations();
          this.cancelEdit();
        }
      });
    }
  }

  deleteAffiliation(affiliation: AfiliacionDTO): void {
    // Si la afiliación no tiene id (es nueva, del preview), eliminarla directamente
    if (!affiliation.id) {
      const index = this.affiliations.findIndex(a => 
        a.nombreInstitucion === affiliation.nombreInstitucion && 
        a.idInstitucion === affiliation.idInstitucion &&
        !a.id
      );
      if (index >= 0) {
        this.affiliations.splice(index, 1);
        this.affiliationsChange.emit(this.affiliations);
      }
      return;
    }

    // Si tiene id pero faltan otros parámetros, no hacer nada
    if (!this.publicationId || !this.rrhhId || !this.rrhhProductoId) {
      return;
    }

    // Guardar referencia para undo
    this.removedAffiliation = { ...affiliation };
    this.removedIndex = this.affiliations.findIndex(a => a.id === affiliation.id);

    // Remover temporalmente de la lista
    this.affiliations = this.affiliations.filter(a => a.id !== affiliation.id);
    this.affiliationsChange.emit(this.affiliations);

    // Mostrar snackbar con opción de undo
    const institutionName = affiliation.nombreInstitucion || this.getInstitutionName(affiliation.idInstitucion);
    const snackBarRef = this.snackBar.open(
      `Affiliation removed – ${institutionName}`,
      'Undo',
      {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'bottom',
        panelClass: ['affiliation-undo-snackbar']
      }
    );

    // Si el usuario hace clic en Undo
    snackBarRef.onAction().subscribe(() => {
      this.undoRemove();
    });

    // Si se cierra sin acción, eliminar permanentemente
    snackBarRef.afterDismissed().subscribe(() => {
      if (this.removedAffiliation) {
        this.performDelete(this.removedAffiliation);
        this.removedAffiliation = null;
        this.removedIndex = -1;
      }
    });
  }

  undoRemove(): void {
    if (this.removedAffiliation && this.removedIndex >= 0) {
      // Restaurar la afiliación en su posición original
      this.affiliations.splice(this.removedIndex, 0, this.removedAffiliation);
      this.affiliationsChange.emit(this.affiliations);
      this.removedAffiliation = null;
      this.removedIndex = -1;
    }
  }

  private performDelete(affiliation: AfiliacionDTO): void {
    if (!affiliation.id) {
      return;
    }

    const url = `/publications/${this.publicationId}/participants/${this.rrhhId}/affiliations/${affiliation.id}?rrhhProductoId=${this.rrhhProductoId}`;
    
    this.baseHttp.delete(url).pipe(
      catchError(error => {
        console.error('Error deleting affiliation:', error);
        // Si falla, restaurar en la lista
        if (this.removedIndex >= 0) {
          this.affiliations.splice(this.removedIndex, 0, affiliation);
          this.affiliationsChange.emit(this.affiliations);
        }
        this.messageService.error('Error deleting affiliation');
        return of(null);
      })
    ).subscribe(() => {
      // Silencioso, ya se removió visualmente
    });
  }

  /**
   * Obtiene el estado del preview para una afiliación
   */
  getPreviewStatusForAffiliation(affiliation: AfiliacionDTO): 'matched' | 'new' | 'review' | undefined {
    if (!this.previewAffiliations) {
      return undefined;
    }
    // Buscar la afiliación del preview que coincida por nombre o ID
    const previewAff = this.previewAffiliations.find(pa => {
      if (affiliation.idInstitucion && pa.matchId) {
        return affiliation.idInstitucion === pa.matchId;
      }
      if (affiliation.nombreInstitucion && pa.name) {
        return affiliation.nombreInstitucion.toLowerCase() === pa.name.toLowerCase();
      }
      return false;
    });
    return previewAff?.status;
  }
  
  /**
   * Obtiene la afiliación del preview para una afiliación actual
   */
  getPreviewAffiliationFor(affiliation: AfiliacionDTO): import('../../../core/models/backend-dtos').AffiliationPreviewDTO | undefined {
    if (!this.previewAffiliations) {
      return undefined;
    }
    return this.previewAffiliations.find(pa => {
      if (affiliation.idInstitucion && pa.matchId) {
        return affiliation.idInstitucion === pa.matchId;
      }
      if (affiliation.nombreInstitucion && pa.name) {
        return affiliation.nombreInstitucion.toLowerCase() === pa.name.toLowerCase();
      }
      return false;
    });
  }

  getInstitutionName(idInstitucion?: number): string {
    if (!idInstitucion) return 'No institution';
    const institution = this.institutions.find(i => i.id === idInstitucion);
    return institution?.descripcion || institution?.idDescripcion || 'Unknown institution';
  }

  compareInstitutions(i1: InstitucionDTO, i2: InstitucionDTO): boolean {
    return i1?.id === i2?.id;
  }

  quickAddAffiliation(institutionName: string): void {
    if (!institutionName || !institutionName.trim()) {
      return;
    }

    // Buscar institución por nombre
    const institution = this.institutions.find(
      inst => inst.idDescripcion?.toLowerCase().includes(institutionName.toLowerCase().trim())
    );

    if (!institution) {
      // Si no se encuentra, abrir diálogo para seleccionar
      this.startAdd();
      return;
    }

    // Agregar directamente
    const affiliationData: AfiliacionDTO = {
      idRRHH: this.rrhhId,
      idProducto: this.publicationId,
      idRRHHProducto: this.rrhhProductoId,
      idInstitucion: institution.id,
      texto: undefined
    };

    this.loading = true;
    const url = `/publications/${this.publicationId}/participants/${this.rrhhId}/affiliations`;
    
    this.baseHttp.post<AfiliacionDTO>(url, affiliationData).pipe(
      catchError(error => {
        console.error('Error creating affiliation:', error);
        this.messageService.error('Error creating affiliation');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(created => {
      if (created) {
        this.loadAffiliations();
      }
    });
  }

  onInputBlur(input: HTMLInputElement): void {
    // Solo agregar si hay texto y no está vacío
    if (input.value && input.value.trim()) {
      this.quickAddAffiliation(input.value);
      input.value = '';
    }
  }
  
  /**
   * Retorna el texto del tooltip para la afiliación
   */
  getAffiliationTooltip(affiliation: AfiliacionDTO): string {
    if (!affiliation.texto || !affiliation.texto.trim()) {
      return '';
    }
    return affiliation.texto;
  }
  
  /**
   * Controla la expansión del texto de afiliación por click
   */
  isAffiliationTextExpanded(index: number): boolean {
    return this.expandedAffiliationTexts.has(index);
  }
  
  /**
   * Toggle del texto de afiliación al hacer click
   */
  toggleAffiliationText(index: number, event: Event): void {
    event.stopPropagation(); // Evitar que se propague el evento
    
    if (this.expandedAffiliationTexts.has(index)) {
      this.expandedAffiliationTexts.delete(index);
    } else {
      this.expandedAffiliationTexts.add(index);
    }
  }
}

