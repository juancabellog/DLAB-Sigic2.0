import { Component, OnInit } from '@angular/core';
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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ParticipantManagerComponent, ParticipantDTO } from '../../../shared/components/participant-manager/participant-manager.component';
import { MessageService } from '../../../core/services/message.service';
import { ScientificEventsService } from '../../../core/services/scientific-events.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { OrganizacionEventosCientificosDTO, TipoEventoDTO, RRHHDTO, PaisDTO } from '../../../core/models/backend-dtos';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-scientific-events-edit',
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
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatTooltipModule,
    ParticipantManagerComponent
  ],
  templateUrl: './scientific-events-edit.component.html',
  styleUrls: ['./scientific-events-edit.component.scss']
})
export class ScientificEventsEditComponent implements OnInit {
  isEditMode: boolean = false;
  eventId: number | null = null;
  loading: boolean = false;

  // Lista de tipos de eventos disponibles
  eventTypes: TipoEventoDTO[] = [];
  loadingEventTypes: boolean = false;

  // Lista de países disponibles
  countries: PaisDTO[] = [];
  loadingCountries: boolean = false;

  // Lista de participantes
  participants: ParticipantDTO[] = [];

  // Control del checkbox Basal
  isBasal: boolean = true;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalEvent: any = null;

  event: OrganizacionEventosCientificosDTO = {
    descripcion: '',
    ciudad: '',
    pais: undefined,
    numParticipantes: undefined,
    codigoANID: '',
    progressReport: undefined,
    tipoProducto: { id: 5 } // ID 5 para organizaciones de eventos científicos (ajustar según necesidad)
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private scientificEventsService: ScientificEventsService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService,
    private progressReportService: ProgressReportService
  ) {}

  ngOnInit(): void {
    // Primero verificar si estamos en modo edición para establecer loading
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.eventId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego el evento
        this.loadCountries();
        this.loadEventTypes(() => {
          if (this.eventId !== null) {
            this.loadEventForEdit(this.eventId);
          }
        });
      } else {
        this.isEditMode = false;
        this.eventId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nuevo evento
        this.loadCountries();
        this.loadEventTypes(() => {
          this.initializeNewEvent();
        });
      }
    });
  }

  loadCountries(): void {
    this.loadingCountries = true;
    this.baseHttp.get<PaisDTO[]>('/catalogs/countries').pipe(
      catchError(error => {
        console.error('Error loading countries:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingCountries = false;
      })
    ).subscribe(items => {
      this.countries = items;
    });
  }

  loadEventTypes(callback?: () => void): void {
    this.loadingEventTypes = true;
    this.baseHttp.get<TipoEventoDTO[]>('/catalogs/event-types').pipe(
      catchError(error => {
        console.error('Error loading event types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingEventTypes = false;
        if (callback) callback();
      })
    ).subscribe(items => {
      this.eventTypes = items;
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Scientific Event' : 'New Scientific Event';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Scientific Event' : 'Add Scientific Event';
  }

  get backButtonText(): string {
    return 'Back to List';
  }

  initializeNewEvent(): void {
    this.event = {
      descripcion: '',
      ciudad: '',
      pais: undefined,
      numParticipantes: undefined,
      codigoANID: '',
      progressReport: undefined,
      tipoProducto: { id: 5 },
      fechaInicio: undefined, // Usuario debe ingresar la fecha
      fechaTermino: undefined,
      basal: 'N'
    };
    this.isBasal = true;
    this.participants = [];
    this.originalEvent = null;
  }

  loadEventForEdit(id: number): void {
    this.loading = true;
    
    this.scientificEventsService.getScientificEvent(id).pipe(
      catchError(error => {
        console.error('Error loading scientific event:', error);
        this.messageService.error('Error loading scientific event. Please try again later.');
        this.router.navigate(['/scientific-events']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(event => {
      if (event) {
        if (event.tipoEvento?.id && this.eventTypes.length > 0) {
          const matchingEventType = this.eventTypes.find(et => et.id === event.tipoEvento!.id);
          if (matchingEventType) {
            event.tipoEvento = matchingEventType;
          }
        }
        
        if (!event.tipoProducto) {
          event.tipoProducto = { id: 5 };
        } else if (event.tipoProducto.id !== 5) {
          event.tipoProducto.id = 5;
        }
        
        this.event = event;
        this.originalEvent = JSON.parse(JSON.stringify(event));
        
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = event.basal === 'S' || event.basal === 's' || event.basal === '1';
        
        if (event.participantes && event.participantes.length > 0) {
          const participantPromises = event.participantes.map(async (p, index) => {
            const participant: ParticipantDTO = {
              rrhhId: p.rrhhId || 0,
              fullName: '',
              participationTypeId: p.tipoParticipacionId || 0,
              corresponding: p.corresponding || false,
              order: p.orden || index + 1
            };
            
            if (p.rrhhId) {
              try {
                const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
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
          
          Promise.all(participantPromises).then(participants => {
            this.participants = participants;
          });
        } else {
          this.participants = [];
        }
      } else {
        this.messageService.error('Scientific event not found');
        this.router.navigate(['/scientific-events']);
      }
    });
  }

  compareEventTypes(type1: TipoEventoDTO | null, type2: TipoEventoDTO | null): boolean {
    if (!type1 || !type2) return type1 === type2;
    return type1.id === type2.id;
  }

  compareCountries(country1: PaisDTO | null, country2: PaisDTO | null): boolean {
    if (!country1 || !country2) return country1 === country2;
    return country1.codigo === country2.codigo;
  }

  getParticipants(): ParticipantDTO[] {
    return this.participants;
  }

  onParticipantsChange(participants: ParticipantDTO[]): void {
    this.participants = participants;
  }

  onBasalChange(checked: boolean): void {
    this.event.basal = checked ? 'S' : 'N';
  }

  goBack(): void {
    this.router.navigate(['/scientific-events']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalEvent) {
      const hasChanges = JSON.stringify(this.event) !== JSON.stringify(this.originalEvent);
      
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

  saveEvent(): void {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    const participantesBackend = this.participants.map((p, index) => ({
      rrhhId: p.rrhhId,
      tipoParticipacionId: p.participationTypeId,
      orden: p.order || index + 1,
      corresponding: p.corresponding || false
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
              this.event.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const eventToSave: OrganizacionEventosCientificosDTO = {
          ...this.event,
          tipoProducto: { id: 5 },
          fechaInicio: this.event.fechaInicio, // Obligatorio - validado en validateForm
          fechaTermino: this.event.fechaTermino || undefined,
          linkPDF: this.event.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          basal: this.isBasal ? 'S' : 'N',
          participantes: participantesBackend.length > 0 ? participantesBackend : undefined
        };

        const saveOperation = this.isEditMode && this.eventId
          ? this.scientificEventsService.updateScientificEvent(this.eventId, eventToSave)
          : this.scientificEventsService.createScientificEvent(eventToSave);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving scientific event:', error);
        this.messageService.error('Error saving scientific event. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(savedEvent => {
      if (savedEvent) {
        this.messageService.success(
          `Scientific event ${this.isEditMode ? 'updated' : 'created'} successfully!`
        );
        this.goBack();
      }
    });
  }

  validateForm(): boolean {
    if (!this.event.descripcion || this.event.descripcion.trim() === '') {
      this.messageService.error('Description is required');
      return false;
    }
    if (!this.event.ciudad || this.event.ciudad.trim() === '') {
      this.messageService.error('City is required');
      return false;
    }
    if (!this.event.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.type !== 'application/pdf') {
        this.messageService.error('Please select a PDF file');
        return;
      }
      this.selectedPdfFile = file;
    }
  }

  clearSelectedFile(): void {
    this.selectedPdfFile = null;
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
        this.event.linkPDF = response.linkPDF;
        this.messageService.success('PDF uploaded successfully');
        this.clearSelectedFile();
      }
    });
  }

  getPdfFileName(): string {
    if (!this.event.linkPDF) {
      return '';
    }
    if (this.event.linkPDF.startsWith('PDF:')) {
      const path = this.event.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.event.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.event?.linkPDF);
  }

  getEventName(): string {
    return this.event.descripcion || 'Scientific Event';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.event.progressReport = this.progressReportService.calculateProgressReport(this.event.fechaInicio);
  }
}

