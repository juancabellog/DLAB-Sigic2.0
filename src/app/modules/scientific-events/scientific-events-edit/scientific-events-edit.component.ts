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
  originalEvent: any = null;

  // Texto para importar información del evento (paste)
  eventImportText: string = '';

  // Control de ayuda para formato de descripción (paste)
  showEventFormat: boolean = false;

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
          const copyFromId = this.route.snapshot.queryParams['copyFrom'];
          if (copyFromId) {
            this.loading = true;
            // Limpiar el query param de la URL
            this.router.navigate([], {
              relativeTo: this.route,
              queryParams: {},
              replaceUrl: true
            });
            // Cargar el evento a copiar después de que los catálogos se hayan cargado
            setTimeout(() => {
              this.loadEventForCopy(parseInt(copyFromId, 10));
            }, 100);
          } else {
            this.initializeNewEvent();
          }
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
      fechaInicio: null,
      fechaTermino: null,
      basal: 'N',
      cluster: ''
    };    
    this.isBasal = true;
    this.participants = [];
    this.originalEvent = null;
    this.selectedClusters = [];
    console.log('initializeNewEvent', this.event);
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

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.event.cluster) {
          try {
            this.selectedClusters = this.event.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch (e) {
            this.selectedClusters = [];
          }
        }
      } else {
        this.messageService.error('Scientific event not found');
        this.router.navigate(['/scientific-events']);
      }
    });
  }

  loadEventForCopy(id: number): void {
    this.loading = true;

    this.scientificEventsService.getScientificEvent(id).pipe(
      catchError(error => {
        console.error('Error loading scientific event for copy:', error);
        this.messageService.error('Error loading scientific event to copy. Please try again later.');
        this.router.navigate(['/scientific-events']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(event => {
      if (event) {
        // Normalizar tipo de evento contra el catálogo cargado
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

        // Limpiar identificadores para tratarlo como nuevo
        event.id = undefined;
        if (event.participantes) {
          event.participantes = event.participantes.map(p => ({
            ...p,
            id: undefined
          }));
        }

        this.event = event;
        this.originalEvent = null;

        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = event.basal === 'S' || event.basal === 's' || event.basal === '1';

        // Participantes: recargar con nombres
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

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.event.cluster) {
          try {
            this.selectedClusters = this.event.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch (e) {
            this.selectedClusters = [];
          }
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
    this.event.cluster = this.selectedClusters.join(',');
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
          participantes: participantesBackend.length > 0 ? participantesBackend : undefined,
          cluster: this.selectedClusters.join(',')
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
   * Parsea la descripción pegada y completa automáticamente los campos básicos del evento.
   */
  async onParseEventDescription(): Promise<void> {
    const text = (this.eventImportText || '').trim();
    if (!text) {
      this.messageService.error('Event description is empty. Please paste an event description first.');
      return;
    }

    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length === 0) {
      this.messageService.error('Event description format is not valid.');
      return;
    }

    try {
      // Name (line 1)
      const nameLine = lines[0];
      if (nameLine) {
        this.event.descripcion = nameLine;
      }

      // Event Type (line 2, if present)
      const typeLine = lines.length > 1 ? lines[1] : '';
      if (typeLine) {
        const matchedType = this.findEventTypeByText(typeLine);
        if (matchedType) {
          this.event.tipoEvento = matchedType;
        }
      }

      // Participants: N
      const participantsLine = lines.find(l => /^Participants\s*:/i.test(l));
      if (participantsLine) {
        const match = participantsLine.match(/Participants\s*:\s*(\d+)/i);
        if (match) {
          this.event.numParticipantes = parseInt(match[1], 10);
        }
      }

      // City, Country
      const locationLine = lines.find(l =>
        /,/.test(l) &&
        !/^Participants\s*:/i.test(l) &&
        !/^BASAL\s+/i.test(l)
      );
      if (locationLine) {
        const parts = locationLine.split(',');
        const city = parts[0]?.trim() || '';
        const countryText = parts.slice(1).join(',').trim();
        if (city) {
          this.event.ciudad = city;
        }
        if (countryText) {
          const matchedCountry = this.findCountryByText(countryText);
          if (matchedCountry) {
            this.event.pais = matchedCountry;
          }
        }
      }

      // Dates line: dd/MM/yyyy - dd/MM/yyyy (con o sin espacios alrededor del "-")
      const dateLine = lines.find(l => /\d{1,2}\/\d{1,2}\/\d{4}\s*-\s*\d{1,2}\/\d{1,2}\/\d{4}/.test(l));
      if (dateLine) {
        const dateTokens = dateLine.split(/\s*-\s*/).map(t => t.trim()).filter(t => t.length > 0);
        const startStr = dateTokens[0];
        const endStr = dateTokens[1];
        this.event.fechaInicio = this.normalizeDateDMY(startStr) || this.event.fechaInicio;
        this.event.fechaTermino = this.normalizeDateDMY(endStr) || this.event.fechaTermino;

        // Actualizar progressReport basado en fechaInicio
        if (this.event.fechaInicio) {
          this.event.progressReport = this.progressReportService.calculateProgressReport(this.event.fechaInicio);
        }
      }

      // BASAL Year periods (solo usamos los períodos, NO el estado "Finished / in progress")
      const basalLine = lines.find(l => /BASAL\s+(?:Año|Year)/i.test(l));
      if (basalLine) {
        this.isBasal = true;
        this.event.basal = 'S';

        const basalRegex = /BASAL\s+(?:Año|Year)\s+([0-9,\s]+)/gi;
        let match: RegExpExecArray | null;
        const periods: number[] = [];
        while ((match = basalRegex.exec(basalLine)) !== null) {
          const list = match[1]
            .split(',')
            .map(p => p.trim())
            .filter(p => p.length > 0);
          list.forEach(p => {
            const n = parseInt(p, 10);
            if (!isNaN(n)) {
              periods.push(n);
            }
          });
        }
        if (periods.length > 0) {
          // Para eventos solo hay un progressReport: usar el mayor período
          const maxPeriod = Math.max(...periods);
          this.event.progressReport = String(maxPeriod);
        }
      }

      // Organizer: <Name1>,<Name2>,...
      const organizers: string[] = [];
      const organizerLine = lines.find(l => /^Organizer\s*:/i.test(l));
      if (organizerLine) {
        const orgMatch = organizerLine.match(/^Organizer\s*:\s*(.+)$/i);
        const rawOrganizers = orgMatch?.[1] || '';
        rawOrganizers.split(',').forEach(o => {
          const name = o.trim();
          if (name && !/^N\/A$/i.test(name)) {
            organizers.push(name);
          }
        });
      }

      // Speaker: Name1, Name2, ...
      const speakers: string[] = [];
      const speakerLine = lines.find(l => /^Speaker\s*:/i.test(l));
      if (speakerLine) {
        const spMatch = speakerLine.match(/^Speaker\s*:\s*(.+)$/i);
        const rawSpeakers = spMatch?.[1] || '';
        rawSpeakers.split(',').forEach(s => {
          const name = s.trim();
          if (name && !/^N\/A$/i.test(name)) {
            speakers.push(name);
          }
        });
      }

      const participantPromises: Promise<void>[] = [];

      // Organizer usa el rol 14 (mismo que el campo calculado organizer en backend)
      for (const organizerName of organizers) {
        participantPromises.push(this.addParticipantFromName(organizerName, 14));
      }

      // Speakers: rol específico 24
      for (const speakerName of speakers) {
        participantPromises.push(this.addParticipantFromName(speakerName, 24));
      }

      if (participantPromises.length > 0) {
        await Promise.all(participantPromises);
      }

      this.messageService.success('Event description parsed successfully. Fields and participants have been populated.');
    } catch (error) {
      console.error('Error parsing event description:', error);
      this.messageService.error('Could not parse the event description. Please verify the format.');
    }
  }

  private findEventTypeByText(text: string): TipoEventoDTO | undefined {
    if (!text || !this.eventTypes || this.eventTypes.length === 0) {
      return undefined;
    }
    const normalized = this.normalizeText(text);
    return this.eventTypes.find(et => this.normalizeText(et.descripcion || et.idDescripcion || '') === normalized);
  }

  private findCountryByText(text: string): PaisDTO | undefined {
    if (!text || !this.countries || this.countries.length === 0) {
      return undefined;
    }
    const normalized = this.normalizeText(text);
    // Intentar por descripción y por código
    return this.countries.find(c =>
      this.normalizeText(c.idDescripcion || '') === normalized ||
      this.normalizeText(c.codigo || '') === normalized
    );
  }

  private normalizeText(text: string | undefined): string {
    if (!text) return '';
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  private normalizeDateDMY(value?: string): string | undefined {
    if (!value) return undefined;
    const trimmed = value.trim();
    if (!trimmed || /^N\/A$/i.test(trimmed)) return undefined;
    const dmyMatch = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(trimmed);
    if (!dmyMatch) return undefined;
    const day = dmyMatch[1].padStart(2, '0');
    const month = dmyMatch[2].padStart(2, '0');
    const year = dmyMatch[3];
    return `${year}-${month}-${day}`;
  }

  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.event.progressReport = this.progressReportService.calculateProgressReport(this.event.fechaInicio);
  }

  onEventStartDateChange(value: string | null): void {
    this.event.fechaInicio = value || null;
    this.onFechaInicioChange();
  }

  /**
   * Agrega un participante a partir de su nombre, usando el servicio de matching avanzado.
   */
  private async addParticipantFromName(name: string, participationTypeId: number): Promise<void> {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }

    try {
      const searchName = this.cleanTitlePrefixes(trimmedName);

      const candidates = await firstValueFrom(this.researcherService.matchResearcherByName(searchName));
      if (!candidates || candidates.length === 0) {
        console.warn(`No researcher match found for "${searchName}" (original: "${trimmedName}")`);
        return;
      }

      const rrhh = candidates[0];
      if (!rrhh || !rrhh.id) {
        return;
      }

      // Evitar duplicados de mismo investigador y mismo rol
      const alreadyExists = this.participants.some(
        p => p.rrhhId === rrhh.id && p.participationTypeId === participationTypeId
      );

      if (alreadyExists) {
        return;
      }

      const newParticipant: ParticipantDTO = {
        rrhhId: rrhh.id,
        fullName: rrhh.fullname || trimmedName,
        idRecurso: rrhh.idRecurso,
        orcid: rrhh.orcid,
        participationTypeId: participationTypeId,
        corresponding: false,
        order: this.participants.length + 1
      };

      this.participants = [...this.participants, newParticipant];
      this.onParticipantsChange(this.participants);
    } catch (error) {
      console.error(`Error searching researcher for "${name}":`, error);
    }
  }

  /**
   * Elimina prefijos de título como "Dr.", "Dra." al inicio del nombre.
   */
  private cleanTitlePrefixes(name: string): string {
    if (!name) {
      return '';
    }
    return name.replace(/^\s*(dr\.?\s+|dra\.?\s+)/i, '').trim();
  }
}

