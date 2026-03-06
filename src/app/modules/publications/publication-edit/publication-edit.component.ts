import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ParticipantManagerComponent, ParticipantDTO } from '../../../shared/components/participant-manager/participant-manager.component';
import { MessageService } from '../../../core/services/message.service';
import { PublicationService } from '../../../core/services/publication.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { GenericCatalogService } from '../../../core/services/generic-catalog.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { CatalogType, TipoParticipacionDTO } from '../../../core/models/catalog-types';
import { PublicacionDTO, JournalDTO, PublicationPreviewDTO, PublicationImportRequestDTO, AuthorPreviewDTO, JournalPreviewDTO } from '../../../core/models/backend-dtos';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-publication-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatTooltipModule,
    ParticipantManagerComponent
  ],
  templateUrl: './publication-edit.component.html',
  styleUrls: ['./publication-edit.component.scss']
})
export class PublicationEditComponent implements OnInit, OnDestroy {
  // Variables para controlar el modo
  isEditMode: boolean = false;
  publicationId: number | null = null;
  loading: boolean = false;
  doiLoading: boolean = false;
  doiAutoFilled: boolean = false;
  loadingImpactFactors: boolean = false;
  
  // Variables para el preview desde DOI
  previewData: PublicationPreviewDTO | null = null;
  isPreviewMode: boolean = false;
  importRequest: PublicationImportRequestDTO | null = null;
  doiError: string | null = null; // Mensaje de error al cargar DOI
  
  // Variables para publicación existente
  existingPublication: {
    exists: boolean;
    visible?: boolean;
    publicationId?: number;
    title?: string;
    journal?: string;
    year?: number;
    message?: string;
  } | null = null;
  
  // Control de redirección automática
  redirectCancelled: boolean = false;
  redirectTimer: any = null;
  spinnerTimer: any = null;
  showRedirectSpinner: boolean = false;
  
  // Getters con nombres más claros para el template
  get isFetchingFromDoi(): boolean {
    return this.doiLoading;
  }
  
  get isLoadingDoi(): boolean {
    return this.doiLoading;
  }
  
  get isInPreviewMode(): boolean {
    return this.isPreviewMode;
  }
  
  get hasExistingPublication(): boolean {
    return this.existingPublication !== null && this.existingPublication.exists === true;
  }
  
  get isFormDisabled(): boolean {
    return this.hasExistingPublication || this.doiLoading;
  }
  
  /**
   * Determina si debe mostrarse el botón "Start a new DOI import"
   * 
   * Reglas:
   * 1. ✅ Mostrar cuando está editando una publicación existente (isEditMode === true)
   * 2. ✅ Mostrar cuando hay una publicación existente detectada (hasExistingPublication === true)
   * 
   * ❌ NO mostrar cuando:
   * 1. Estás en modo preview de un DOI válido que no existe en el sistema (isInPreviewMode === true)
   * 2. Estás creando una nueva publicación y el formulario está limpio (!isEditMode && !isPreviewMode && DOI vacío)
   */
  shouldShowResetDoiButton(): boolean {
    // NO mostrar cuando estás en modo preview de OpenAlex (datos cargados desde DOI que no existe en el sistema)
    if (this.isInPreviewMode) {
      return false;
    }
    
    // Caso 1: Está editando una publicación existente
    if (this.isEditMode) {
      return true;
    }
    
    // Caso 2: Hay una publicación existente detectada (banner visible)
    if (this.hasExistingPublication) {
      return true;
    }
    
    // NO mostrar en otros casos:
    // - Formulario limpio sin datos (!isEditMode && !isPreviewMode)
    return false;
  }
  
  openExistingPublication(): void {
    if (this.existingPublication?.publicationId) {
      const publicationId = this.existingPublication.publicationId;
      
      // Solo cancelar los timers, NO limpiar el formulario
      // porque vamos a navegar a la publicación existente
      if (this.redirectTimer) {
        clearTimeout(this.redirectTimer);
        this.redirectTimer = null;
      }
      if (this.spinnerTimer) {
        clearTimeout(this.spinnerTimer);
        this.spinnerTimer = null;
      }
      this.showRedirectSpinner = false;
      this.redirectCancelled = true; // Marcar como cancelado para evitar redirecciones automáticas
      
      console.log('Navigating to publication:', publicationId);
      
      // Verificar si ya estamos en la misma ruta pero con diferente ID
      const currentRoute = this.route.snapshot;
      const currentId = currentRoute.params['id'];
      
      if (currentId && parseInt(currentId) === publicationId) {
        // Ya estamos en la misma publicación, solo recargar los datos
        console.log('Already on this publication, reloading data...');
        // Limpiar el banner de publicación existente primero
        this.existingPublication = null;
        // Usar setTimeout para asegurar que el cambio de estado se procese antes de cargar
        setTimeout(() => {
          this.loadPublicationForEdit(publicationId);
        }, 0);
        return;
      }
      
      // Navegar a la publicación existente usando navigateByUrl para forzar recarga
      this.router.navigateByUrl(`/publications/${publicationId}/edit`, { 
        skipLocationChange: false 
      }).then(
        (success) => {
          console.log('Navigation successful:', success);
          // Si la navegación fue exitosa pero retornó null (misma ruta),
          // forzar recarga del componente
          if (success === null || success === undefined) {
            console.log('Navigation returned null, forcing component reload...');
            // Recargar la página para asegurar que el componente se recargue
            window.location.href = `/publications/${publicationId}/edit`;
          }
        },
        (error) => {
          console.error('Navigation error:', error);
          this.messageService.error('Error opening publication. Please try again.');
        }
      );
    } else {
      console.error('No publication ID available');
      this.messageService.error('Publication ID not found.');
    }
  }
  
  /**
   * Inicia la redirección automática suave
   */
  startAutoRedirect(): void {
    if (!this.existingPublication?.publicationId || this.redirectCancelled) {
      return;
    }
    
    // Limpiar timers previos si existen
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
    if (this.spinnerTimer) {
      clearTimeout(this.spinnerTimer);
      this.spinnerTimer = null;
    }
    
    // Fase 1: Mensaje inicial (inmediato)
    // Ya está mostrado en el template
    
    // Fase 2: Añadir spinner después de 800ms
    this.spinnerTimer = setTimeout(() => {
      if (!this.redirectCancelled) {
        this.showRedirectSpinner = true;
      }
    }, 800);
    
    // Fase 3: Redirigir después de 5s
    this.redirectTimer = setTimeout(() => {
      if (!this.redirectCancelled && this.existingPublication?.publicationId) {
        const publicationId = this.existingPublication.publicationId;
        // Verificar si ya estamos en la misma ruta
        const currentRoute = this.route.snapshot;
        const currentId = currentRoute.params['id'];
        
        if (currentId && parseInt(currentId) === publicationId) {
          // Ya estamos en la misma publicación, solo recargar los datos
          console.log('Already on this publication, reloading data...');
          this.loadPublicationForEdit(publicationId);
          this.existingPublication = null;
        } else {
          // Navegar a la publicación existente
          this.router.navigate(['/publications', publicationId, 'edit']).catch(error => {
            console.error('Navigation error:', error);
            this.messageService.error('Error opening publication. Please try again.');
          });
        }
      }
    }, 5000);
  }
  
  /**
   * Reinicia el formulario para comenzar una nueva importación desde DOI
   */
  resetForNewDoi(): void {
    // Preguntar confirmación si hay cambios no guardados
    this.messageService.confirm(
      'This will clear the current publication. Continue?',
      (confirmed: boolean) => {
        if (confirmed) {
          // Cancelar cualquier redirección en curso
          this.cancelRedirect();
          
          // Limpiar todos los estados
          this.isEditMode = false;
          this.isPreviewMode = false;
          this.previewData = null;
          this.importRequest = null;
          this.pendingJournalCreation = false;
          this.existingPublication = null;
          
          // Limpiar DOI y errores
          this.publication.doi = '';
          this.doiError = null;
          this.doiAutoFilled = false;
          this.doiLoading = false;
          
          // Limpiar participantes
          this.participants = [];
          this.updateMissingAffiliationsStatus();
          
          // Limpiar journal
          this.publication.journal = undefined;
          this.journalInput = '';
          
          // Reinicializar publicación
          this.initializeNewPublication();
          
          // Limpiar otros campos del formulario
          this.publication.descripcion = '';
          this.publication.volume = '';
          this.publication.yearPublished = new Date().getFullYear();
          this.publication.firstpage = '';
          this.publication.lastpage = '';
          this.publication.fechaInicio = `${this.publication.yearPublished}-01-01`;
          this.publication.codigoANID = '';
          this.publication.progressReport = undefined;
          this.isBasal = true;
          
          // Limpiar archivo PDF seleccionado
          this.selectedPdfFile = null;
          
          // Scroll al top
          window.scrollTo({ top: 0, behavior: 'smooth' });
          
          console.log('Form reset for new DOI import');
        }
      }
    );
  }
  
  /**
   * Cancela la redirección automática y restaura el formulario al estado inicial
   */
  cancelRedirect(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    
    console.log('Cancel redirect called');
    
    // Cancelar timers
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
    
    if (this.spinnerTimer) {
      clearTimeout(this.spinnerTimer);
      this.spinnerTimer = null;
    }
    
    // Limpiar estado de redirección
    this.redirectCancelled = false;
    this.showRedirectSpinner = false;
    
    // Limpiar publicación existente
    this.existingPublication = null;
    
    // Limpiar DOI y volver al estado inicial
    this.publication.doi = '';
    this.doiError = null;
    this.doiAutoFilled = false;
    this.doiLoading = false;
    
    // Limpiar preview data
    this.previewData = null;
    this.isPreviewMode = false;
    this.importRequest = null;
    this.pendingJournalCreation = false;
    
    // Limpiar participantes del preview
    this.participants = [];
    this.updateMissingAffiliationsStatus();
    
    console.log('Form reset to initial state');
  }

  // Lista de journals disponibles
  journals: JournalDTO[] = [];
  filteredJournals: JournalDTO[] = [];
  journalInput: string = '';
  loadingJournals: boolean = false;
  isSelectingJournal: boolean = false;
  pendingJournalCreation: boolean = false; // Indica si el journal será creado al guardar

  // Lista de participantes
  participants: ParticipantDTO[] = [];
  hasIncompleteParticipants: boolean = false;
  hasAuthorsWithMissingAffiliations: boolean = false;
  missingAffiliationsCount: number = 0;
  private updateMissingAffiliationsTimeout: any = null;

  // Marca cuándo el participant-manager ya procesó al menos una vez
  // (usamos esto para saber que las afiliaciones se cargaron/inicializaron)
  participantsInitialized: boolean = false;

  // Opciones de clusters (1 a 5)
  clusterOptions: { id: number; label: string }[] = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];
  selectedClusters: number[] = [];

  // Control del checkbox Basal
  isBasal: boolean = true;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalPublication: any = null;

  publication: PublicacionDTO = {
    descripcion: '',
    doi: '',
    journal: undefined,
    volume: '',
    yearPublished: new Date().getFullYear(),
    firstpage: '',
    lastpage: '',
    codigoANID: '',
    progressReport: undefined,
    tipoProducto: { id: 3 }, // ID 3 para publicaciones
    cluster: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private publicationService: PublicationService,
    private researcherService: ResearcherService,
    private catalogService: GenericCatalogService,
    private participationTypesService: CatalogService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Primero verificar si estamos en modo edición para establecer loading
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.publicationId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar journals y luego la publicación
        this.loadJournals(() => {
          if (this.publicationId !== null) {
            this.loadPublicationForEdit(this.publicationId);
          }
        });
      } else {
        this.isEditMode = false;
        this.publicationId = null;
        this.loading = false;
        // Cargar journals y luego inicializar nueva publicación
        this.loadJournals(() => {
          this.initializeNewPublication();
        });
      }
    });
  }

  loadJournals(callback?: () => void): void {
    this.loadingJournals = true;
    this.catalogService.getItems(CatalogType.JOURNALS).pipe(
      catchError(error => {
        console.error('Error loading journals:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingJournals = false;
        if (callback) callback();
      })
    ).subscribe(items => {
      // Mapear CatalogItem a JournalDTO
      this.journals = items.map(item => ({
        id: item.id,
        idDescripcion: (item as any).idDescripcion,
        descripcion: item.nombre || item.descripcion,
        abbreviation: (item as any).abbreviation,
        issn: (item as any).issn
      } as JournalDTO));
      
      // Ordenar alfabéticamente por descripción
      this.journals.sort((a, b) => {
        const descA = (a.descripcion || '').toLowerCase();
        const descB = (b.descripcion || '').toLowerCase();
        return descA.localeCompare(descB);
      });
      
      // Inicializar la lista filtrada con todos los journals
      this.filteredJournals = [...this.journals];
      // Si hay un journal seleccionado, actualizar el input
      if (this.publication.journal) {
        this.journalInput = this.getJournalDisplayText(this.publication.journal);
      }
    });
  }

  // Getters para el template
  get pageTitle(): string {
    return this.isEditMode ? 'Edit Publication' : 'New Publication';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Publication' : 'Add Publication';
  }

  get backButtonText(): string {
    return this.isEditMode ? 'Back to List' : 'Back to List';
  }

  initializeNewPublication(): void {
    const currentYear = new Date().getFullYear();
    this.publication = {
      descripcion: '',
      doi: '',
      journal: undefined,
      volume: '',
      yearPublished: currentYear,
      firstpage: '',
      lastpage: '',
      codigoANID: '',
      progressReport: undefined,
      tipoProducto: { id: 3 }, // ID 3 para publicaciones
      // fechaInicio se establecerá automáticamente cuando cambie yearPublished
      fechaInicio: undefined,
      basal: 'S', // Por defecto "S" (Basal asociado)
      cluster: ''
    };
    // Establecer fechaInicio basada en yearPublished inicial
    if (this.publication.yearPublished) {
      this.publication.fechaInicio = `${this.publication.yearPublished}-01-01`;
    }
    // Checkbox "Product associated with Basal funding" marcado por defecto
    this.isBasal = true;
    this.participants = [];
    this.updateMissingAffiliationsStatus();
    this.originalPublication = null;
    this.selectedClusters = [];
  }

  loadPublicationForEdit(id: number): void {
    console.log('loadPublicationForEdit', id)
    this.loading = true;
    
    // Asegurar que estamos en modo edición y NO en modo preview
    this.isEditMode = true;
    this.isPreviewMode = false;
    this.previewData = null;
    this.existingPublication = null; // Limpiar banner si existe
    
    this.publicationService.getPublication(id).pipe(
      catchError(error => {
        console.error('Error loading publication:', error);
        this.messageService.error('Error loading publication. Please try again later.');
        this.router.navigate(['/publications']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(publication => {
      if (publication) {
        console.log('publication:', publication)
        // Si la publicación tiene un journal, asegurarnos de que coincida con uno de los journals cargados
        if (publication.journal?.id && this.journals.length > 0) {
          const matchingJournal = this.journals.find(j => j.id === publication.journal!.id);
          if (matchingJournal) {
            publication.journal = matchingJournal;
            this.journalInput = this.getJournalDisplayText(matchingJournal);
          }
        }
        
        // Asegurar que tipoProducto.id sea 3 para publicaciones
        if (!publication.tipoProducto) {
          publication.tipoProducto = { id: 3 };
        } else if (publication.tipoProducto.id !== 3) {
          publication.tipoProducto.id = 3;
        }
        
        // Si hay año publicado pero no fechaInicio, establecerla automáticamente
        // Formato ISO: YYYY-MM-DD
        if (publication.yearPublished && !publication.fechaInicio) {
          publication.fechaInicio = `${publication.yearPublished}-01-01`;
        }
        this.publication = publication;
        this.originalPublication = JSON.parse(JSON.stringify(publication)); // Deep copy

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.publication.cluster) {
          try {
            this.selectedClusters = this.publication.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch {
            this.selectedClusters = [];
          }
        }
        
        // Los factores de impacto ya vienen del backend desde impactFactor y avgImpactFactor
        // No necesitamos recargarlos si ya están en la publicación
        // Solo cargar si no están presentes y hay journal y año
        if (!this.publication.factorImpacto && !this.publication.factorImpactoPromedio 
            && this.publication.journal?.id && this.publication.yearPublished) {
              console.log('carga factores de impacto')
          this.loadImpactFactors();
        }
        
        // Inicializar isBasal basado en publication.basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = publication.basal === 'S' || publication.basal === 's' || publication.basal === '1';
        
        // Cargar participantes desde la publicación si existen
        // Los participantes ya vienen del backend, solo necesitamos cargar los nombres de los investigadores
        console.log('Loading participants from publication:', publication.participantes);
        if (publication.participantes && publication.participantes.length > 0) {
          console.log(`Found ${publication.participantes.length} participants`);
          // Convertir participantes del backend al formato del frontend
          // Cargar datos completos de los investigadores usando los rrhhId
          const participantPromises = publication.participantes.map(async (p, index) => {
            const participant: ParticipantDTO = {
              rrhhId: p.rrhhId || 0,
              fullName: '', // Se cargará desde el servicio
              participationTypeId: p.tipoParticipacionId || 0,
              corresponding: p.corresponding || false,
              order: p.orden || index + 1,
              idRRHHProducto: p.idRRHHProducto // ID de la participación en rrhh_producto - IMPORTANTE para cargar afiliaciones
            };
            
            // Cargar datos completos del investigador si tenemos el ID
            if (p.rrhhId) {
              try {
                const researcher = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
                if (researcher) {
                  participant.fullName = researcher.fullname || '';
                  participant.idRecurso = researcher.idRecurso;
                  participant.orcid = researcher.orcid;
                }
              } catch (error) {
                console.error(`Error loading researcher ${p.rrhhId}:`, error);
              }
            }
            
            return participant;
          });
          
          // Esperar a que se carguen todos los participantes
          Promise.all(participantPromises).then(participants => {
            console.log('All participants loaded:', participants);
            this.participants = participants;
            // Actualizar el estado de afiliaciones faltantes después de cargar todos los participantes
            this.updateMissingAffiliationsStatus();
            // Las afiliaciones se cargarán automáticamente por el componente affiliation-manager
            // cuando detecte que publicationId, rrhhId y rrhhProductoId están disponibles
          }).catch(error => {
            console.error('Error loading participants:', error);
            this.participants = [];
            // Actualizar el estado de afiliaciones faltantes incluso en caso de error
            this.updateMissingAffiliationsStatus();
          });
        } else {
          console.log('No participants found in publication');
          this.participants = [];
          this.updateMissingAffiliationsStatus();
        }
        
      } else {
        this.messageService.error('Publication not found');
        this.router.navigate(['/publications']);
      }
    });
  }

  compareJournals(journal1: JournalDTO | null, journal2: JournalDTO | null): boolean {
    if (!journal1 || !journal2) return journal1 === journal2;
    return journal1.id === journal2.id;
  }

  /**
   * Obtiene el texto de visualización de un journal
   */
  getJournalDisplayText(journal: JournalDTO | null): string {
    if (!journal) return '';
    let text = journal.descripcion || '';
    if (journal.abbreviation) {
      text += ` (${journal.abbreviation})`;
    }
    return text;
  }

  /**
   * Filtra los journals basado en el texto de búsqueda
   */
  filterJournals(): void {
    if (!this.journalInput || this.journalInput.trim() === '') {
      this.filteredJournals = [...this.journals];
      return;
    }

    const searchTerm = this.journalInput.toLowerCase().trim();
    this.filteredJournals = this.journals.filter(journal => {
      const descripcion = (journal.descripcion || '').toLowerCase();
      const abbreviation = (journal.abbreviation || '').toLowerCase();
      const issn = (journal.issn || '').toLowerCase();
      
      return descripcion.includes(searchTerm) || 
             abbreviation.includes(searchTerm) || 
             issn.includes(searchTerm);
    });
  }

  /**
   * Maneja la selección de un journal desde el autocomplete
   */
  onJournalSelected(journal: JournalDTO): void {
    this.isSelectingJournal = true;
    this.publication.journal = journal;
    const displayText = this.getJournalDisplayText(journal);
    this.journalInput = displayText;
    
    // Si se selecciona un journal existente, limpiar el flag de creación pendiente
    if (journal.id !== 0 && journal.id !== undefined) {
      this.pendingJournalCreation = false;
    }
    
    // Si estamos en modo preview y hay previewData, actualizar el estado del journal
    if (this.isInPreviewMode && this.previewData?.journal) {
      this.previewData.journal.status = 'matched';
      this.previewData.journal.matchId = journal.id;
      this.previewData.journal.name = journal.descripcion;
    }
    
    this.onJournalChange(journal);
    // Forzar actualización del input después de un pequeño delay
    setTimeout(() => {
      this.journalInput = displayText;
      this.isSelectingJournal = false;
    }, 100);
  }

  /**
   * Maneja cuando el usuario escribe en el campo de journal
   */
  onJournalInputChange(): void {
    this.filterJournals();
    // Si el input no coincide con el journal seleccionado, limpiar la selección
    if (this.publication.journal) {
      const currentDisplayText = this.getJournalDisplayText(this.publication.journal);
      if (this.journalInput !== currentDisplayText) {
        console.log('limpiar selección de journal')
        this.publication.journal = undefined;
        this.onJournalChange(null);
      }
    }
  }

  /**
   * Maneja cuando el autocomplete se cierra
   */
  onAutocompleteClosed(): void {
    // No hacer nada si estamos en proceso de selección
    if (this.isSelectingJournal) {
      console.log('onAutocompleteClosed: skipping because isSelectingJournal is true');
      return;
    }
    
    // Si hay un journal seleccionado, restaurar el texto de visualización
    if (this.publication.journal) {
      const displayText = this.getJournalDisplayText(this.publication.journal);
      this.journalInput = displayText;
    } else {
      // Solo limpiar si el input no coincide con ningún journal
      const matchingJournal = this.journals.find(j => 
        this.getJournalDisplayText(j).toLowerCase() === this.journalInput.toLowerCase()
      );
      if (!matchingJournal) {
        this.journalInput = '';
      }
    }
    // Restaurar la lista completa
    this.filteredJournals = [...this.journals];
  }

  getParticipants(): ParticipantDTO[] {
    return this.participants;
  }

  onParticipantsChange(participants: ParticipantDTO[]): void {
    // Actualizar la lista de participantes cuando cambie
    this.participants = participants;
    console.log('Participants updated:', participants);
    
    // Si estamos cargando desde DOI, no actualizar el estado hasta que termine la carga
    // Esto evita múltiples actualizaciones durante la carga asíncrona de participantes
    if (this.doiLoading) {
      return;
    }
    
    // Si no está cargando, usar debounce para agrupar actualizaciones rápidas
    this.debouncedUpdateMissingAffiliationsStatus();
    
    // Forzar actualización de hasIncompleteParticipants basado en afiliaciones
    // Esto asegura que el botón de guardar se habilite cuando todas las afiliaciones estén completas
    this.updateHasIncompleteParticipants();
  }
  
  /**
   * Actualiza hasIncompleteParticipants basado en el estado actual de los participantes
   * Se llama cuando cambian los participantes o sus afiliaciones
   */
  private updateHasIncompleteParticipants(): void {
    if (!this.participants || this.participants.length === 0) {
      this.hasIncompleteParticipants = false;
      return;
    }
    
    // Verificar si hay participantes incompletos:
    // 1. Sin afiliaciones (verificado por hasAffiliations)
    // 2. O con estado de incompletitud explícito
    const hasIncomplete = this.participants.some(p => {
      // Verificar si tiene afiliaciones
      const hasAffs = this.hasAffiliations(p);
      
      // Verificar si tiene estado de incompletitud
      const isIncomplete = (p as any).affiliationsStatus === 'incomplete' || 
                          (p as any).isIncomplete === true;
      
      // Un participante está incompleto si no tiene afiliaciones O tiene estado incompleto
      return !hasAffs || isIncomplete;
    });
    
    this.hasIncompleteParticipants = hasIncomplete;
    
    // También actualizar el estado de afiliaciones faltantes
    this.debouncedUpdateMissingAffiliationsStatus();
  }

  onHasIncompleteChange(hasIncomplete: boolean): void {
    this.hasIncompleteParticipants = hasIncomplete;
    // También actualizar el estado de afiliaciones faltantes cuando cambia el estado de incompletitud
    this.debouncedUpdateMissingAffiliationsStatus();
    // Marcar que el participant-manager ya terminó su primer ciclo de cálculo
    this.participantsInitialized = true;
  }

  onJournalChange(selectedJournal: JournalDTO | null): void {
    if (selectedJournal && selectedJournal.id) {
      // Si hay un año publicado, cargar los factores de impacto
      if (this.publication.yearPublished) {
        this.loadImpactFactors();
      }
    } else {
      // Limpiar factores si no hay journal seleccionado
      this.publication.factorImpacto = undefined;
      this.publication.factorImpactoPromedio = undefined;
    }
  }

  onYearPublishedChange(): void {
    // Actualizar fechaInicio automáticamente cuando cambia el año
    // Formato ISO: YYYY-MM-DD
    if (this.publication.yearPublished) {
      this.publication.fechaInicio = `${this.publication.yearPublished}-01-01`;
    }
    
    // Cuando cambie el año, recargar factores de impacto si hay un journal seleccionado
    if (this.publication.journal?.id && this.publication.yearPublished) {
      this.loadImpactFactors();
    }
  }

  onBasalChange(checked: boolean): void {
    // Actualizar publication.basal cuando cambia el checkbox
    // El backend espera "S" o "N" como string
    this.publication.basal = checked ? 'S' : 'N';
  }

  isClusterSelected(clusterId: number): boolean {
    return this.selectedClusters.includes(clusterId);
  }

  onClusterChange(clusterId: number, checked: boolean): void {
    if (checked) {
      if (!this.selectedClusters.includes(clusterId)) {
        this.selectedClusters.push(clusterId);
      }
    } else {
      this.selectedClusters = this.selectedClusters.filter(id => id !== clusterId);
    }
    this.publication.cluster = this.selectedClusters.join(',');
  }

  loadImpactFactors(): void {
    if (!this.publication.journal?.id || !this.publication.yearPublished) {
      return;
    }

    this.loadingImpactFactors = true;
    this.publicationService.getImpactFactors(
      this.publication.journal.id,
      this.publication.yearPublished
    ).pipe(
      catchError(error => {
        console.error('Error loading impact factors:', error);
        this.messageService.error('Error loading impact factors');
        return of({ factorImpacto: null, factorImpactoPromedio: null });
      }),
      finalize(() => {
        this.loadingImpactFactors = false;
      })
    ).subscribe(factors => {
      console.log('entra a cargar factores de impacto de forma incorrecta', factors)
      this.publication.factorImpacto = factors.factorImpacto ?? undefined;
      this.publication.factorImpactoPromedio = factors.factorImpactoPromedio ?? undefined;
    });
  }

  fetchByDOI(): void {
    if (!this.publication.doi || !this.publication.doi.trim()) {
      return;
    }

    this.doiLoading = true;
    this.doiAutoFilled = false;
    this.doiError = null; // Limpiar errores previos
    this.previewData = null;
    this.isPreviewMode = false;
    this.importRequest = null;
    this.pendingJournalCreation = false;
    this.existingPublication = null; // Limpiar publicación existente
    
    console.log('Fetching publication preview by DOI:', this.publication.doi);
    
    this.publicationService.getPublicationPreview(this.publication.doi).pipe(
      catchError(error => {
        console.error('Error fetching publication preview:', error);
        
        // Establecer mensaje de error para mostrar en el banner
        if (error.status === 404) {
          this.doiError = 'We could not fetch metadata for this DOI. Please check the value or try again.';
        } else {
          this.doiError = 'We could not fetch metadata for this DOI. Please check the value or try again.';
        }
        
        // También mostrar toast
        this.messageService.error(this.doiError);
        
        // Quitar skeletons y overlays, rehabilitar DOI
        this.doiLoading = false;
        this.existingPublication = null;
        
        return of(null);
      }),
      finalize(() => {
        // Asegurar que doiLoading se limpie siempre (catchError ya lo hace en caso de error)
        this.doiLoading = false;
        // Actualizar el estado de afiliaciones faltantes cuando termine la carga del DOI
        // para asegurar que el estado esté actualizado después de que todos los participantes se hayan cargado
        this.updateMissingAffiliationsStatus();
      })
    ).subscribe(response => {
      if (response) {
        // Verificar si la publicación ya existe
        if (response.exists === true) {
          // Verificar si el usuario puede ver la publicación
          if (response.visible === false || !response.publicationId) {
            // La publicación existe pero el usuario no puede verla
            const errorMessage = response.message || 'This publication already exists but is not available for your account.';
            this.doiError = errorMessage;
            this.messageService.error(errorMessage);
            this.existingPublication = null;
            this.previewData = null;
            this.isPreviewMode = false;
            return;
          }
          
          // La publicación ya existe y el usuario puede verla
          this.existingPublication = {
            exists: true,
            visible: true,
            publicationId: response.publicationId,
            title: response.title,
            journal: response.journal,
            year: response.year
          };
          
          // Limpiar preview data y deshabilitar modo preview
          this.previewData = null;
          this.isPreviewMode = false;
          this.doiError = null;
          
          // Reiniciar control de redirección
          this.redirectCancelled = false;
          this.showRedirectSpinner = false;
          
          // Iniciar redirección automática
          this.startAutoRedirect();
          
          // Deshabilitar el formulario (se hará en el template)
          return;
        }
        
        // Si no existe, procesar el preview normalmente
        const preview = response.preview;
        if (preview) {
          // Limpiar información de publicación existente
          this.existingPublication = null;
          
          // Éxito: limpiar cualquier error previo
          this.doiError = null;
          
          this.previewData = preview;
          this.isPreviewMode = true;
          
          // Inicializar el importRequest con los datos del preview
          this.initializeImportRequest(preview);
          
          // Autocompletar campos básicos con datos del preview
          if (preview.publication) {
            const pub = preview.publication;
            // Siempre rellenar campos con datos del preview
            this.publication.descripcion = pub.title || this.publication.descripcion || '';
            this.publication.volume = pub.volume || this.publication.volume || '';
            this.publication.yearPublished = pub.publicationYear || this.publication.yearPublished;
            this.publication.fechaInicio = pub.publicationDate || this.publication.fechaInicio || '';
            this.publication.firstpage = pub.firstPage || this.publication.firstpage || '';
            this.publication.lastpage = pub.lastPage || this.publication.lastpage || '';

            // Si OpenAlex ya entregó y descargamos un PDF, propagarlo al formulario
            if (pub.linkPDF) {
              this.publication.linkPDF = pub.linkPDF;
            }
            
            // Calcular progressReport basado en fechaInicio
            if (pub.publicationDate) {
              this.publication.progressReport = this.calculateProgressReport(pub.publicationDate);
            }
            
            this.doiAutoFilled = true;
          }
          
          // Procesar journal
          if (preview.journal) {
            this.processJournalPreview(preview.journal);
          }
          
          // Procesar autores (se mostrarán en el participant-manager)
          console.log('preview.authors', preview.authors);
          if (preview.authors && preview.authors.length > 0) {
            this.processAuthorsPreview(preview.authors);
          }
          
          // Mostrar mensaje de éxito
          this.messageService.success('Publication metadata loaded from DOI.');
        }
      }
    });
  }
  
  /**
   * Inicializa el importRequest con los datos del preview
   */
  private initializeImportRequest(preview: PublicationPreviewDTO): void {
    this.importRequest = {
      publication: preview.publication ? { ...preview.publication } : undefined,
      journal: preview.journal ? {
        action: preview.journal.status === 'matched' ? 'matched' : (preview.journal.status === 'new' ? 'create' : 'link'),
        journalId: preview.journal.matchId,
        name: preview.journal.name,
        issn: preview.journal.issn
      } : undefined,
      authors: preview.authors ? preview.authors.map(author => ({
        openAlexId: author.openAlexId,
        name: author.name,
        orcid: author.orcid,
        order: author.order,
        isCorresponding: author.isCorresponding,
        authorPosition: author.authorPosition,
        action: author.status === 'matched' ? 'matched' : (author.status === 'new' ? 'create' : 'link'),
        rrhhId: author.matchId,
        tipoParticipacionId: undefined, // El usuario debe seleccionar
        affiliations: author.affiliations ? author.affiliations.map(aff => ({
          name: aff.name,
          rawAffiliationString: aff.rawAffiliationString,
          action: aff.status === 'matched' ? 'matched' : (aff.status === 'new' ? 'create' : 'link'),
          institutionId: aff.matchId,
          texto: aff.rawAffiliationString
        })) : []
      })) : []
    };
  }
  
  /**
   * Procesa el preview del journal
   */
  private processJournalPreview(journalPreview: JournalPreviewDTO): void {
    if (journalPreview.status === 'matched' && journalPreview.matchId) {
      // Buscar el journal en la lista cargada
      const journal = this.journals.find(j => j.id === journalPreview.matchId);
      if (journal) {
        this.publication.journal = journal;
        this.journalInput = this.getJournalDisplayText(journal);
      }
    }
    // Si es "new" o "review", el usuario deberá decidir (se mostrará en la UI)
  }
  
  /**
   * Procesa los autores del preview y los convierte a participantes
   */
  private processAuthorsPreview(authors: AuthorPreviewDTO[]): void {
    this.participationTypesService.getParticipationTypes('publications').subscribe(types => {
      this.participants = authors.map(author => {
        let participationTypeName: string | undefined;
        if (author.tipoParticipacionId && types.length > 0) {
          const type = types.find(t => t.id === author.tipoParticipacionId);
          participationTypeName = type?.descripcion || type?.nombre;
        }
  
        const isMatched = author.status === 'matched';
        const isNew     = author.status === 'new';
        const isReview  = author.status === 'review';
  
        // 🔴 NUEVA LÓGICA DE RESOLUCIÓN INICIAL
        let resolutionType: any;
        let resolved = false;
  
        if (isMatched) {
          resolutionType = 'linkExisting';
          resolved = true;
        } else if (isNew) {
          // Para autores "new" desde OpenAlex:
          // asumimos por defecto "Create new RRHH from this data"
          resolutionType = 'createNew';
          resolved = true; // ✅ ya está resuelto
        } else if (isReview) {
          // Caso dudoso, requiere decisión explícita del usuario
          resolutionType = 'unresolved';
          resolved = false;
        }
  
        // Usar datos de BD cuando hay match o cuando hay review con match preseleccionado
        // Para review: mostrar el nombre del match preseleccionado (de BD), no el de OpenAlex
        // El nombre original de OpenAlex se guarda en previewOriginalName para cuando el usuario elija "create"
        const displayName = (isMatched || (isReview && author.matchedName)) && author.matchedName 
          ? author.matchedName 
          : (author.name || '');
        const displayOrcid = (isMatched || (isReview && author.matchedOrcid)) && author.matchedOrcid 
          ? author.matchedOrcid 
          : author.orcid;
        
        const participant: ParticipantDTO = {
          rrhhId: author.matchId || 0,
          fullName: displayName,
          orcid: displayOrcid, // ORCID a mostrar (de BD si hay match, sino de OpenAlex)
          participationTypeId: author.tipoParticipacionId || 0,
          participationTypeName,
          corresponding: author.isCorresponding || false,
          order: author.order || 0,
  
          // Preview
          previewStatus: author.status,
          previewMatchId: author.matchId,
          previewMatchedName: author.matchedName,
          previewCandidates: author.candidates,
          previewOpenAlexId: author.openAlexId,
          previewAction: isMatched ? 'matched' : (isNew ? 'create' : 'link'),
          previewOriginalName: author.name, // Guardar el nombre original de OpenAlex
  
          // Estado de resolución
          resolved,
          resolutionType,
          resolutionStatus: resolutionType, // para compatibilidad
          linkedRrhhId: isMatched ? author.matchId || undefined : undefined,
          newRrhhData: isNew ? { name: author.name, orcid: author.orcid } as any : undefined,
  
          // Afiliaciones: si tiene, las marcamos "incomplete"; si no, no bloquean nada
          affiliationsStatus:
            author.affiliations && author.affiliations.length > 0 ? 'incomplete' : undefined,
          
          // Estado de sincronización de ORCID
          orcidSyncStatus: author.orcidSyncStatus,
          orcidChangeAction: author.orcidChangeAction || (author.orcidSyncStatus === 'missing_local' ? 'add' : 'none'),
          matchMethod: author.matchMethod,
          orcidFromOpenAlex: author.orcid, // ORCID de OpenAlex
          orcidFromBD: author.matchedOrcid // ORCID de BD (si hay match)
        };
  
        return participant;
      });
      
      // Actualizar el estado de afiliaciones faltantes una sola vez al final
      // cuando todos los participantes han sido procesados
      this.updateMissingAffiliationsStatus();
    });
  }
  
  
  /**
   * Maneja el clic en "Select existing" para journal
   */
  selectExistingJournal(): void {
    // El usuario puede seleccionar del dropdown que ya está visible
    // Solo necesitamos enfocar el campo
    // Por ahora, solo mostramos un mensaje
    this.messageService.info('Please select a journal from the dropdown above.');
  }
  
  /**
   * Maneja el clic en "Create new" para journal
   */
  createNewJournal(): void {
    if (this.previewData?.journal) {
      const journalName = this.previewData.journal.name || '';
      const journalIssn = this.previewData.journal.issn || '';
      
      // Crear un journal temporal para mostrar en el campo
      const pendingJournal: JournalDTO = {
        id: 0, // ID temporal, será creado en el backend
        idDescripcion: '',
        descripcion: journalName,
        abbreviation: '',
        issn: journalIssn
      };
      
      // Asignar el journal temporal a la publicación
      this.publication.journal = pendingJournal;
      this.journalInput = journalName;
      this.pendingJournalCreation = true;
      
      // Actualizar el estado del preview para ocultar el mensaje de error
      if (this.previewData.journal) {
        this.previewData.journal.status = 'matched'; // Cambiar a 'matched' para ocultar el panel amarillo
        this.previewData.journal.matchId = 0; // ID temporal
        this.previewData.journal.name = journalName;
      }
      
      // Actualizar el importRequest para indicar que se creará un nuevo journal
      if (this.importRequest?.journal) {
        this.importRequest.journal.action = 'create';
        this.importRequest.journal.name = journalName;
        this.importRequest.journal.issn = journalIssn;
      }
      
      // Mostrar toast como retroalimentación complementaria
      this.messageService.info(`A new journal "${journalName}" will be created when you save the publication.`);
    }
  }
  
  /**
   * Permite cambiar el DOI cuando ya hay un preview cargado
   * Limpia el preview y permite ingresar un nuevo DOI
   */
  changeDOI(): void {
    this.messageService.confirm(
      'Changing the DOI will clear all preview data. Are you sure you want to continue?',
      (confirmed: boolean) => {
        if (confirmed) {
          // Limpiar el preview y volver a estado "en blanco"
          this.isPreviewMode = false;
          this.previewData = null;
          this.importRequest = null;
          this.pendingJournalCreation = false;
          this.doiAutoFilled = false;
          this.cancelRedirect(); // Cancelar redirección si está activa
          this.existingPublication = null; // Limpiar publicación existente
          this.doiError = null; // Limpiar cualquier error previo
          this.doiLoading = false; // Asegurar que no esté en loading
          
          // Limpiar participantes del preview (Authors Summary & Authors Detail)
          this.participants = [];
          this.updateMissingAffiliationsStatus();
          
          // Vaciar todos los campos del formulario que fueron autocompletados
          this.publication.descripcion = '';
          this.publication.volume = '';
          this.publication.yearPublished = undefined;
          this.publication.fechaInicio = '';
          this.publication.firstpage = '';
          this.publication.lastpage = '';
          this.publication.journal = undefined;
          this.publication.factorImpacto = undefined;
          this.publication.factorImpactoPromedio = undefined;
          this.publication.progressReport = undefined;
          this.publication.codigoANID = '';
          this.isBasal = true;
          
          // Limpiar el DOI para que pueda ingresar uno nuevo
          // El DOI se habilita automáticamente porque isPreviewMode = false
          this.publication.doi = '';
          
          // Limpiar PDF seleccionado si fue cargado durante el preview
          this.selectedPdfFile = null;
          
          this.messageService.info('You can now enter a new DOI to load a different publication.');
        }
      },
      'Change DOI'
    );
  }

  /**
   * Determina el estado actual del flujo de importación desde DOI
   */
  getImportState(): 'doi' | 'preview' | 'review' | 'confirm' {
    if (!this.isPreviewMode) {
      return 'doi';
    }
    
    // Si estamos en preview mode, estamos en "preview"
    // "review" sería cuando el usuario está revisando decisiones
    // "confirm" sería cuando está a punto de confirmar
    // Por ahora, siempre mostramos "preview" cuando isPreviewMode es true
    // Podríamos expandir esto más adelante para detectar si hay decisiones pendientes
    return 'preview';
  }

  goBack(): void {
    this.router.navigate(['/publications']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalPublication) {
      // Verificar si hay cambios no guardados
      const hasChanges = JSON.stringify(this.publication) !== JSON.stringify(this.originalPublication);
      
      if (hasChanges) {
        this.messageService.confirm(
          'You have unsaved changes. Are you sure you want to cancel?',
          (accepted: boolean) => {
            if (accepted) {
              this.goBack();
            }
          },
          'Unsaved Changes'
        );
      } else {
        this.goBack();
      }
    } else {
      this.goBack();
    }
  }

  savePublication(): void {
    // Si estamos en modo preview, usar el endpoint de importación
    if (this.isPreviewMode && this.importRequest) {
      this.importFromDoi();
      return;
    }

    // Flujo normal de guardado
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    // Convertir participantes del frontend al formato del backend
    const participantesBackend = this.participants.map((p, index) => ({
      rrhhId: p.rrhhId,
      tipoParticipacionId: p.participationTypeId,
      orden: p.order || index + 1,
      corresponding: p.corresponding || false,
      idRRHHProducto: p.idRRHHProducto,
      // Pasar las afiliaciones para que el backend pueda recrearlas si borra/crea participantes
      afiliaciones: (p as any).affiliations || []
    }));

    // Si hay un archivo seleccionado, primero subirlo
    const uploadPdfObservable = this.selectedPdfFile
      ? this.baseHttp.uploadFile<{ linkPDF: string; filename: string; message: string }>('/files/upload-pdf', this.selectedPdfFile).pipe(
          catchError(error => {
            console.error('Error uploading PDF:', error);
            this.messageService.error('Error uploading PDF. Please try again.');
            throw error; // Re-lanzar para que el flujo se detenga
          }),
          tap(response => {
            if (response && response.linkPDF) {
              this.publication.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const publicationToSave: PublicacionDTO = {
          ...this.publication,
          // Asegurar que tipoProducto.id sea 3 para publicaciones
          tipoProducto: { id: 3 },
          // Asegurar que fechaInicio esté establecida basada en yearPublished (formato ISO: YYYY-MM-DD)
          fechaInicio: this.publication.fechaInicio || (this.publication.yearPublished ? `${this.publication.yearPublished}-01-01` : undefined),
          // Asegurar que las fechas estén en formato string ISO
          fechaTermino: this.publication.fechaTermino || undefined,
          // Asegurar que linkPDF se incluya explícitamente
          linkPDF: this.publication.linkPDF || undefined,
          // Asegurar que basal esté establecido correctamente (S o N)
          basal: this.isBasal ? 'S' : 'N',
          // Asegurar que cluster se base en los seleccionados
          cluster: this.selectedClusters.join(','),
          // Incluir participantes en formato backend
          participantes: participantesBackend.length > 0 ? participantesBackend : undefined
        };

        const saveOperation = this.isEditMode && this.publicationId
          ? this.publicationService.updatePublication(this.publicationId, publicationToSave)
          : this.publicationService.createPublication(publicationToSave);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving publication:', error);
        this.messageService.error('Error saving publication. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(savedPublication => {
      if (savedPublication) {
        this.messageService.success(
          `Publication ${this.isEditMode ? 'updated' : 'created'} successfully!`
        );
        this.goBack();
      }
    });
  }

  /**
   * Importa la publicación desde DOI usando las decisiones del usuario
   */
  private importFromDoi(): void {
    if (!this.importRequest) {
      this.messageService.error('Import request not initialized. Please reload the preview.');
      return;
    }

    // Actualizar el importRequest con las decisiones actuales del formulario
    this.updateImportRequestFromForm();

    // Validar que todas las decisiones estén tomadas
    if (!this.validateImportRequest()) {
      return;
    }

    this.loading = true;

    // Si hay un archivo seleccionado, primero subirlo
    const uploadPdfObservable = this.selectedPdfFile
      ? this.baseHttp.uploadFile<{ linkPDF: string; filename: string; message: string }>('/files/upload-pdf', this.selectedPdfFile).pipe(
          catchError(error => {
            console.error('Error uploading PDF:', error);
            this.messageService.error('Error uploading PDF. Please try again.');
            throw error;
          }),
          tap(response => {
            if (response && response.linkPDF) {
              // Guardar el linkPDF para agregarlo después si es necesario
              // Por ahora el import-from-doi no maneja PDFs, así que lo guardamos en el publication
              this.publication.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    uploadPdfObservable.pipe(
      switchMap(() => {
        return this.publicationService.importPublicationFromDoi(this.importRequest!);
      }),
      catchError(error => {
        console.error('Error importing publication from DOI:', error);
        const errorMessage = error.error?.message || error.message || 'Error importing publication. Please try again.';
        this.messageService.error(errorMessage);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(savedPublication => {
      if (savedPublication) {
        this.messageService.success('Publication imported successfully!');
        // Limpiar el modo preview
        this.isPreviewMode = false;
        this.previewData = null;
        this.importRequest = null;
        this.pendingJournalCreation = false;
        this.goBack();
      }
    });
  }

  /**
   * Actualiza el importRequest con las decisiones actuales del formulario
   */
  private updateImportRequestFromForm(): void {
    if (!this.importRequest) {
      return;
    }

    // Actualizar journal decision
    if (this.importRequest.journal && this.previewData?.journal) {
      // Si pendingJournalCreation es true, forzar action='create'
      if (this.pendingJournalCreation) {
        this.importRequest.journal.action = 'create';
        this.importRequest.journal.name = this.previewData.journal.name || this.publication.journal?.descripcion || '';
        this.importRequest.journal.issn = this.previewData.journal.issn || this.publication.journal?.issn || '';
        this.importRequest.journal.journalId = undefined; // Limpiar journalId cuando se crea nuevo
      } else if (this.publication.journal?.id && this.publication.journal.id > 0) {
        // Si hay un journal seleccionado (y no es el temporal con id=0), actualizar la decisión
        if (this.previewData.journal.matchId === this.publication.journal.id) {
          this.importRequest.journal.action = 'matched';
          this.importRequest.journal.journalId = this.publication.journal.id;
        } else {
          this.importRequest.journal.action = 'link';
          this.importRequest.journal.journalId = this.publication.journal.id;
        }
      } else if (this.previewData.journal.status === 'new') {
        // Si no hay journal seleccionado y el status es 'new', mantener 'create'
        this.importRequest.journal.action = 'create';
        this.importRequest.journal.name = this.previewData.journal.name || '';
        this.importRequest.journal.issn = this.previewData.journal.issn || '';
        this.importRequest.journal.journalId = undefined;
      }
    }

    // Actualizar autores con las decisiones del participant-manager
    if (this.importRequest.authors && this.participants.length > 0) {
      this.importRequest.authors = this.importRequest.authors.map((author, index) => {
        const participant = this.participants[index];
        if (participant) {
          // Actualizar la acción del autor basada en resolutionType (preferido) o previewAction
          let authorAction = author.action;
          
          // Priorizar resolutionType sobre previewAction para mayor robustez
          if (participant.resolutionType) {
            if (participant.resolutionType === 'createNew') {
              authorAction = 'create';
            } else if (participant.resolutionType === 'linkExisting') {
              // Si tiene linkedRrhhId, usar 'link', sino 'matched' si tiene rrhhId
              if (participant.linkedRrhhId && participant.linkedRrhhId > 0) {
                authorAction = 'link';
              } else if (participant.rrhhId && participant.rrhhId > 0) {
                authorAction = participant.previewStatus === 'matched' ? 'matched' : 'link';
              } else {
                authorAction = 'create'; // Fallback si no hay ID
              }
            } else if (participant.resolutionType === 'unresolved') {
              // Si está unresolved, verificar si tiene rrhhId asignado
              if (participant.rrhhId && participant.rrhhId > 0) {
                authorAction = 'link';
              } else {
                authorAction = 'create';
              }
            }
          } else if (participant.previewAction) {
            // Fallback a previewAction si no hay resolutionType
            authorAction = participant.previewAction;
          } else if (participant.previewStatus === 'matched') {
            authorAction = 'matched';
          } else if (participant.previewStatus === 'new') {
            authorAction = 'create';
          } else if (participant.previewStatus === 'review') {
            authorAction = participant.rrhhId ? 'link' : 'create';
          }

          // Si la acción es 'link' o 'matched', asegurar que hay rrhhId
          if ((authorAction === 'link' || authorAction === 'matched') && !participant.rrhhId) {
            authorAction = 'create';
          }

          // Determinar el rrhhId final
          let finalRrhhId: number | undefined = participant.rrhhId || participant.linkedRrhhId || author.rrhhId;
          // Si es 'create', no debe tener rrhhId (será creado)
          if (authorAction === 'create') {
            finalRrhhId = undefined;
          }

          // Actualizar afiliaciones desde el participant (sincronizar con lo que el usuario tiene)
          let updatedAffiliations: any[] = [];
          if (participant.affiliations && participant.affiliations.length > 0) {
            // Usar las afiliaciones actuales del participant
            updatedAffiliations = participant.affiliations.map(aff => {
              // Determinar la acción basada en si tiene idInstitucion
              let affAction: string;
              if (aff.idInstitucion && aff.idInstitucion > 0) {
                // Si tiene idInstitucion, verificar si es del preview original (matched) o linkeado
                const originalAff = author.affiliations?.find(origAff => 
                  origAff.institutionId === aff.idInstitucion || 
                  origAff.name === aff.nombreInstitucion
                );
                if (originalAff && originalAff.action === 'matched') {
                  affAction = 'matched';
                } else {
                  affAction = 'link';
                }
              } else if (aff.nombreInstitucion && aff.nombreInstitucion.trim()) {
                // Si tiene nombre pero no id, es nueva (create)
                affAction = 'create';
              } else {
                // Si no tiene ni nombre ni id, omitir esta afiliación
                return null;
              }

              return {
                name: aff.nombreInstitucion || '',
                rawAffiliationString: aff.texto || aff.nombreInstitucion || '',
                action: affAction,
                institutionId: aff.idInstitucion && aff.idInstitucion > 0 ? aff.idInstitucion : undefined,
                texto: aff.texto || undefined
              };
            }).filter(aff => aff !== null); // Filtrar afiliaciones nulas
          } else {
            // Si no hay afiliaciones en el participant, usar las originales del preview
            updatedAffiliations = author.affiliations || [];
          }

          return {
            ...author,
            action: authorAction,
            rrhhId: finalRrhhId,
            tipoParticipacionId: participant.participationTypeId || author.tipoParticipacionId,
            order: participant.order || author.order,
            isCorresponding: participant.corresponding || author.isCorresponding,
            affiliations: updatedAffiliations
          };
        }
        return author;
      });
    }
  }

  /**
   * Verifica si el importRequest es válido (todas las decisiones tomadas)
   * Usado para deshabilitar el botón de confirmación
   */
  /**
   * Verifica si hay autores sin resolver
   */
  get hasUnresolvedAuthors(): boolean {
    return this.participants.some(p => {
      const isExternal = (p.previewStatus === 'new' || p.previewStatus === 'review') && (!p.rrhhId || p.rrhhId === 0);
      if (isExternal) {
        // Usar el nuevo campo 'resolved' si está disponible
        if (p.resolved !== undefined) {
          return !p.resolved;
        }
        // Lógica de compatibilidad
        const resolutionType = p.resolutionType || p.resolutionStatus;
        return !resolutionType || resolutionType === 'unresolved' || 
               (resolutionType === 'linkExisting' && (!p.linkedRrhhId || p.linkedRrhhId === 0));
      }
      return false;
    });
  }
  
  /**
   * Verifica si todos los autores están resueltos:
   * - RRHH decidido (link o create)
   * - Rol (tipoParticipacionId) seleccionado
   * Las afiliaciones NO se consideran obligatorias para habilitar el botón.
   */
  get allAuthorsResolved(): boolean {
    return this.participants.every(p => {
      // Autor externo = viene de OpenAlex sin rrhhId todavía
      const isExternal =
        (p.previewStatus === 'new' || p.previewStatus === 'review') &&
        (!p.rrhhId || p.rrhhId === 0);

      // 1) Resolver RRHH
      let rrhhResolved = false;

      if (p.resolved !== undefined) {
        // Campo nuevo explícito
        rrhhResolved = p.resolved === true;
      } else {
        // Compatibilidad con resolutionType / resolutionStatus
        const resolutionType = p.resolutionType || p.resolutionStatus;
        rrhhResolved = !!(
          resolutionType &&
          resolutionType !== 'unresolved' &&
          (
            resolutionType === 'createNew' ||
            (resolutionType === 'linkExisting' && p.linkedRrhhId && p.linkedRrhhId > 0)
          )
        );
      }

      // 2) Rol obligatorio para TODOS (externos e internos)
      const hasRole = !!p.participationTypeId;

      if (!isExternal) {
        // Autores ya internos: con rol basta
        return hasRole;
      }

      // Autores externos: RRHH resuelto + rol
      return rrhhResolved && hasRole;
    });
  }

  
  /**
   * Indica si se debe mostrar el mensaje de error de participantes
   */
  get showParticipantsError(): boolean {
    if (!this.isInPreviewMode) {
      return this.hasIncompleteParticipants;
    }
    // En modo preview, mostrar si hay autores sin resolver (RRHH o afiliaciones)
    return !this.allAuthorsResolved || this.hasIncompleteParticipants;
  }

  /**
   * Verifica si se puede confirmar la publicación
   * En modo edición normal:
   *  - Esperar a que el participant-manager haya inicializado (participantsInitialized)
   *  - No estar en loading
   * En preview, mantenemos la lógica relajada (se valida a fondo en validateImportRequest).
   */
  get canConfirmPublication(): boolean {
    // En modo edición normal (no preview), requerimos que participantes estén inicializados
    if (!this.isInPreviewMode) {
      return !this.loading && this.participantsInitialized;
    }

    // En modo preview, usamos sólo el estado de loading (la validación detallada se hace al confirmar)
    return !this.loading;
  }

  /**
   * Updates the missing affiliations count and flag
   * Call this method whenever participants change
   */
  private updateMissingAffiliationsStatus(): void {
    if (!this.participants || this.participants.length === 0) {
      this.missingAffiliationsCount = 0;
      this.hasAuthorsWithMissingAffiliations = false;
      // También actualizar hasIncompleteParticipants cuando no hay participantes
      this.hasIncompleteParticipants = false;
      return;
    }
    this.missingAffiliationsCount = this.participants.filter(p => !this.hasAffiliations(p)).length;
    this.hasAuthorsWithMissingAffiliations = this.missingAffiliationsCount > 0;
    
    // Actualizar hasIncompleteParticipants basado en el estado actual de los participantes
    // Esto asegura que el botón de guardar se habilite cuando todas las afiliaciones estén completas
    this.updateHasIncompleteParticipants();
  }

  /**
   * Debounced version of updateMissingAffiliationsStatus
   * Agrupa múltiples actualizaciones rápidas en una sola
   */
  private debouncedUpdateMissingAffiliationsStatus(): void {
    // Limpiar timeout anterior si existe
    if (this.updateMissingAffiliationsTimeout) {
      clearTimeout(this.updateMissingAffiliationsTimeout);
    }
    
    // Programar la actualización después de un pequeño delay
    this.updateMissingAffiliationsTimeout = setTimeout(() => {
      this.updateMissingAffiliationsStatus();
      this.updateMissingAffiliationsTimeout = null;
    }, 50);
  }

  /**
   * Helper to decide if a participant has affiliations
   */
  private hasAffiliations(participant: ParticipantDTO): boolean {
    // Ajustar según tu modelo real:
    // - Si tienes participant.affiliations[]
    // - O si usas participant.affiliationsStatus === 'complete', etc.
    const anyList =
      (participant as any).affiliations &&
      Array.isArray((participant as any).affiliations) &&
      (participant as any).affiliations.length > 0;
    const statusComplete =
      (participant as any).affiliationsStatus === 'complete';
    
    // Consideramos que tiene afiliaciones si:
    // - hay una lista explícita, o
    // - el estado marca 'complete'
    return !!anyList || !!statusComplete;
  }


  /**
   * Valida que todas las decisiones del importRequest estén tomadas
   * Muestra mensajes de error específicos
   */
  private validateImportRequest(): boolean {
    if (!this.importRequest) {
      this.messageService.error('Import request not initialized.');
      return false;
    }

    // Validar journal
    if (!this.importRequest.journal || !this.importRequest.journal.action) {
      this.messageService.error('Please make a decision for the journal (match, link, or create).');
      return false;
    }
    if (this.importRequest.journal.action === 'link' && !this.importRequest.journal.journalId) {
      this.messageService.error('Please select a journal or create a new one.');
      return false;
    }
    if (this.importRequest.journal.action === 'create' && !this.importRequest.journal.name) {
      this.messageService.error('Journal name is required when creating a new journal.');
      return false;
    }

    // Validar autores
    if (!this.importRequest.authors || this.importRequest.authors.length === 0) {
      this.messageService.error('At least one author is required.');
      return false;
    }

    for (const author of this.importRequest.authors) {
      if (!author.action) {
        this.messageService.error(`Please make a decision for author: ${author.name || 'Unknown'}`);
        return false;
      }
      if (author.action === 'link' && !author.rrhhId) {
        this.messageService.error(`Please select an existing researcher for: ${author.name || 'Unknown'}`);
        return false;
      }
      if (author.action === 'create' && !author.name) {
        this.messageService.error(`Author name is required for: ${author.name || 'Unknown'}`);
        return false;
      }
      if (!author.tipoParticipacionId) {
        this.messageService.error(`Participation type is required for: ${author.name || 'Unknown'}`);
        return false;
      }

      // Validar afiliaciones del autor
      if (author.affiliations && author.affiliations.length > 0) {
        for (const affiliation of author.affiliations) {
          if (!affiliation.action) {
            this.messageService.error(`Please make a decision for affiliation of author: ${author.name || 'Unknown'}`);
            return false;
          }
          if (affiliation.action === 'link' && !affiliation.institutionId) {
            this.messageService.error(`Please select an existing institution for affiliation of author: ${author.name || 'Unknown'}`);
            return false;
          }
          if (affiliation.action === 'create' && !affiliation.name) {
            this.messageService.error(`Institution name is required when creating new institution for author: ${author.name || 'Unknown'}`);
            return false;
          }
        }
      }
    }

    return true;
  }

  validateForm(): boolean {
    if (!this.publication.descripcion || this.publication.descripcion.trim() === '') {
      this.messageService.error('Title is required');
      return false;
    }
    if (!this.publication.journal) {
      this.messageService.error('Journal is required');
      return false;
    }
    if (!this.publication.journal.id) {
      this.messageService.error('Please select a valid journal');
      return false;
    }
    if (!this.publication.yearPublished) {
      this.messageService.error('Year published is required');
      return false;
    }
    // fechaInicio se establece automáticamente desde yearPublished, pero validamos que exista
    if (!this.publication.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      // Validar que sea un PDF
      if (file.type !== 'application/pdf') {
        this.messageService.error('Please select a PDF file');
        return;
      }
      this.selectedPdfFile = file;
    }
  }

  clearSelectedFile(): void {
    this.selectedPdfFile = null;
    // Resetear el input file
    const fileInput = document.getElementById('pdf-upload') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  uploadPdf(): void {
    if (!this.selectedPdfFile) {
      this.messageService.error('Please select a PDF file first');
      return;
    }

    this.uploadingPdf = true;
    this.baseHttp.uploadFile<{ linkPDF: string; filename: string; message: string }>('/files/upload-pdf', this.selectedPdfFile).pipe(
      catchError(error => {
        console.error('Error uploading PDF:', error);
        this.messageService.error('Error uploading PDF. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.uploadingPdf = false;
      })
    ).subscribe(response => {
      if (response && response.linkPDF) {
        this.publication.linkPDF = response.linkPDF;
        this.messageService.success('PDF uploaded successfully');
        this.clearSelectedFile();
      }
    });
  }

  getPdfFileName(): string {
    if (!this.publication.linkPDF) {
      return '';
    }
    // Extraer el nombre del archivo del linkPDF (formato: "PDF:pdfs/nombre_archivo.pdf")
    if (this.publication.linkPDF.startsWith('PDF:')) {
      const path = this.publication.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    // Si es una URL completa, extraer el nombre del archivo
    const parts = this.publication.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.publication?.linkPDF);
  }

  getPublicationTitle(): string {
    return this.publication.descripcion || 'Publication';
  }

  /**
   * Formatea el factor de impacto con 4 decimales según formato chileno
   * Separador de miles: punto (.)
   * Separador de decimales: coma (,)
   */
  formatImpactFactor(factor: number | null | undefined): string {
    if (factor == null || factor === undefined) {
      return 'N/A';
    }
    // Formatear con 4 decimales usando formato chileno
    const parts = factor.toFixed(4).split('.');
    const integerPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    const decimalPart = parts[1];
    return `${integerPart},${decimalPart}`;
  }
  
  /**
   * Calcula el progressReport basado en la fecha de inicio (publicationDate)
   * Lógica:
   * - Si fechaInicio <= "2022-07-31" → progressReport = 1
   * - Si fechaInicio <= "2023-07-31" → progressReport = 2
   * - Si fechaInicio <= "2024-07-31" → progressReport = 3
   * - Si fechaInicio <= "2025-07-31" → progressReport = 4
   * - Si fechaInicio > "2025-07-31" → progressReport = 5
   */
  private calculateProgressReport(publicationDate: string): number | undefined {
    if (!publicationDate || publicationDate.trim() === '') {
      return undefined;
    }
    
    try {
      const date = new Date(publicationDate);
      const cutoff1 = new Date('2022-07-31');
      const cutoff2 = new Date('2023-07-31');
      const cutoff3 = new Date('2024-07-31');
      const cutoff4 = new Date('2025-07-31');
      
      // Comparar solo la fecha (sin hora)
      const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
      const cutoff1Only = new Date(cutoff1.getFullYear(), cutoff1.getMonth(), cutoff1.getDate());
      const cutoff2Only = new Date(cutoff2.getFullYear(), cutoff2.getMonth(), cutoff2.getDate());
      const cutoff3Only = new Date(cutoff3.getFullYear(), cutoff3.getMonth(), cutoff3.getDate());
      const cutoff4Only = new Date(cutoff4.getFullYear(), cutoff4.getMonth(), cutoff4.getDate());
      
      if (dateOnly <= cutoff1Only) {
        return 1;
      } else if (dateOnly <= cutoff2Only) {
        return 2;
      } else if (dateOnly <= cutoff3Only) {
        return 3;
      } else if (dateOnly <= cutoff4Only) {
        return 4;
      } else {
        return 5;
      }
    } catch (error) {
      console.error('Error calculating progressReport:', error);
      return undefined;
    }
  }
  
  ngOnDestroy(): void {
    // Limpiar timers de redirección al destruir el componente
    this.redirectCancelled = true;
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
    if (this.spinnerTimer) {
      clearTimeout(this.spinnerTimer);
      this.spinnerTimer = null;
    }
    // Limpiar timeout de debounce si existe
    if (this.updateMissingAffiliationsTimeout) {
      clearTimeout(this.updateMissingAffiliationsTimeout);
      this.updateMissingAffiliationsTimeout = null;
    }
    this.showRedirectSpinner = false;
  }
}