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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { ParticipantManagerComponent, ParticipantDTO } from '../../../shared/components/participant-manager/participant-manager.component';
import { MessageService } from '../../../core/services/message.service';
import { OutreachActivitiesService } from '../../../core/services/outreach-activities.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { DifusionDTO, TipoDifusionDTO, PaisDTO, PublicoObjetivoDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-oa-edit',
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
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ParticipantManagerComponent
  ],
  templateUrl: './oa-edit.component.html',
  styleUrls: ['./oa-edit.component.scss']
})
export class OaEditComponent implements OnInit {
  isEditMode: boolean = false;
  activityId: number | null = null;
  loading: boolean = false;

  // Lista de tipos de difusión disponibles
  diffusionTypes: TipoDifusionDTO[] = [];
  loadingDiffusionTypes: boolean = false;

  // Lista de países disponibles
  countries: PaisDTO[] = [];
  loadingCountries: boolean = false;

  // Lista de públicos objetivo disponibles
  targetAudiences: PublicoObjetivoDTO[] = [];
  loadingTargetAudiences: boolean = false;

  // Lista de participantes
  participants: ParticipantDTO[] = [];

  // Control del checkbox Basal
  isBasal: boolean = false;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalActivity: DifusionDTO | null = null;

  activity: DifusionDTO = {
    descripcion: '',
    codigoANID: '',
    progressReport: undefined,
    lugar: '',
    ciudad: '',
    link: '',
    tipoProducto: { id: 1 } // ID 1 para DIFUSION
  };

  // Públicos objetivo seleccionados (para el formulario)
  selectedTargetAudiences: PublicoObjetivoDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private outreachActivitiesService: OutreachActivitiesService,
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
        this.activityId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego la actividad
        this.loadDiffusionTypes();
        this.loadCountries();
        this.loadTargetAudiences();
        // Usar setTimeout para asegurar que los catálogos se carguen antes de cargar la actividad
        setTimeout(() => {
          if (this.activityId !== null) {
            this.loadActivityForEdit(this.activityId);
          }
        }, 100);
      } else {
        this.isEditMode = false;
        this.activityId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nueva actividad
        this.loadDiffusionTypes();
        this.loadCountries();
        this.loadTargetAudiences();
        this.initializeNewActivity();
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Outreach Activity' : 'New Outreach Activity';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Outreach Activity' : 'Create Outreach Activity';
  }

  get backButtonText(): string {
    return 'Back to List';
  }

  loadDiffusionTypes(): void {
    this.loadingDiffusionTypes = true;
    this.baseHttp.get<TipoDifusionDTO[]>('/catalogs/diffusion-types').pipe(
      catchError(error => {
        console.error('Error loading diffusion types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingDiffusionTypes = false;
      })
    ).subscribe(items => {
      this.diffusionTypes = items;
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

  loadTargetAudiences(): void {
    this.loadingTargetAudiences = true;
    this.baseHttp.get<PublicoObjetivoDTO[]>('/catalogs/target-audiences').pipe(
      catchError(error => {
        console.error('Error loading target audiences:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingTargetAudiences = false;
      })
    ).subscribe(items => {
      this.targetAudiences = items;
    });
  }

  initializeNewActivity(): void {
    this.activity = {
      descripcion: '',
      codigoANID: '',
      progressReport: undefined,
      lugar: '',
      ciudad: '',
      link: '',
      tipoProducto: { id: 1 },
      fechaInicio: undefined,
      fechaTermino: undefined,
      basal: 'N'
    };
    this.isBasal = false;
    this.participants = [];
    this.selectedTargetAudiences = [];
    this.originalActivity = null;
  }

  loadActivityForEdit(id: number): void {
    this.loading = true;

    this.outreachActivitiesService.getOutreachActivity(id).pipe(
      catchError(error => {
        console.error('Error loading outreach activity:', error);
        this.messageService.error('Error loading outreach activity. Please try again later.');
        this.router.navigate(['/outreach-activities']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(activity => {
      if (activity) {
        // Cargar participantes
        if (activity.participantes && activity.participantes.length > 0) {
          this.loadParticipants(activity.participantes);
        } else {
          this.participants = [];
        }

        // Cargar públicos objetivo seleccionados
        if (activity.publicoObjetivo) {
          try {
            const audienceIds = activity.publicoObjetivo.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
            this.selectedTargetAudiences = this.targetAudiences.filter(ta => audienceIds.includes(ta.id!));
          } catch (e) {
            this.selectedTargetAudiences = [];
          }
        } else {
          this.selectedTargetAudiences = [];
        }

        // Configurar checkbox Basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = activity.basal === 'S' || activity.basal === 's' || activity.basal === '1';

        // Establecer tipo de producto si no está definido
        if (!activity.tipoProducto) {
          activity.tipoProducto = { id: 1 };
        }

        // Match diffusion type if available
        if (activity.tipoDifusion?.id && this.diffusionTypes.length > 0) {
          const matchingType = this.diffusionTypes.find(dt => dt.id === activity.tipoDifusion!.id);
          if (matchingType) {
            activity.tipoDifusion = matchingType;
          }
        }

        // Match country if available
        if (activity.pais?.codigo && this.countries.length > 0) {
          const matchingCountry = this.countries.find(c => c.codigo === activity.pais!.codigo);
          if (matchingCountry) {
            activity.pais = matchingCountry;
          }
        }

        this.activity = activity;
        this.originalActivity = JSON.parse(JSON.stringify(activity));
      } else {
        this.messageService.error('Outreach activity not found');
        this.router.navigate(['/outreach-activities']);
      }
    });
  }

  async loadParticipants(participantes: any[]): Promise<void> {
    this.participants = [];
    
    for (const p of participantes) {
      if (p.rrhhId) {
        try {
          const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
          if (researcher.id) {
            const participant: ParticipantDTO = {
              rrhhId: researcher.id,
              fullName: researcher.fullname || '',
              idRecurso: researcher.idRecurso,
              orcid: researcher.orcid,
              participationTypeId: p.tipoParticipacionId,
              corresponding: p.corresponding || false,
              order: p.orden || 0
            };
            this.participants.push(participant);
          }
        } catch (error) {
          console.error('Error loading researcher:', error);
        }
      }
    }
  }

  onParticipantsChange(participants: ParticipantDTO[]): void {
    this.participants = participants;
  }

  onBasalChange(checked: boolean): void {
    this.isBasal = checked;
    this.activity.basal = checked ? 'S' : 'N';
  }

  onTargetAudienceChange(audience: PublicoObjetivoDTO, checked: boolean): void {
    if (checked) {
      if (!this.selectedTargetAudiences.find(ta => ta.id === audience.id)) {
        this.selectedTargetAudiences.push(audience);
      }
    } else {
      this.selectedTargetAudiences = this.selectedTargetAudiences.filter(ta => ta.id !== audience.id);
    }
    // Actualizar el string de publicoObjetivo
    this.activity.publicoObjetivo = this.selectedTargetAudiences.map(ta => ta.id).join(',');
  }

  isTargetAudienceSelected(audience: PublicoObjetivoDTO): boolean {
    return this.selectedTargetAudiences.some(ta => ta.id === audience.id);
  }

  compareDiffusionTypes(type1: TipoDifusionDTO | null, type2: TipoDifusionDTO | null): boolean {
    if (!type1 || !type2) return type1 === type2;
    return type1.id === type2.id;
  }

  compareCountries(country1: PaisDTO | null, country2: PaisDTO | null): boolean {
    if (!country1 || !country2) return country1 === country2;
    return country1.codigo === country2.codigo;
  }

  goBack(): void {
    this.router.navigate(['/outreach-activities']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalActivity) {
      const hasChanges = JSON.stringify(this.activity) !== JSON.stringify(this.originalActivity);
      
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

  validateForm(): boolean {
    if (!this.activity.descripcion || this.activity.descripcion.trim() === '') {
      this.messageService.error('Name is required');
      return false;
    }
    if (!this.activity.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  saveActivity(): void {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    // Preparar participantes
    const participantes = this.participants.map((p, index) => ({
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
              this.activity.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const activityData: DifusionDTO = {
          ...this.activity,
          linkPDF: this.activity.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          publicoObjetivo: this.selectedTargetAudiences.map(ta => ta.id).join(',')
        };

        const saveOperation = this.isEditMode && this.activityId
          ? this.outreachActivitiesService.updateOutreachActivity(this.activityId, activityData)
          : this.outreachActivitiesService.createOutreachActivity(activityData);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving outreach activity:', error);
        this.messageService.error('Error saving outreach activity. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(result => {
      if (result) {
        this.messageService.success(
          `Outreach activity ${this.isEditMode ? 'updated' : 'created'} successfully!`
        );
        this.goBack();
      }
    });
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
        this.activity.linkPDF = response.linkPDF;
        this.messageService.success('PDF uploaded successfully');
        this.clearSelectedFile();
      }
    });
  }

  getPdfFileName(): string {
    if (!this.activity.linkPDF) {
      return '';
    }
    if (this.activity.linkPDF.startsWith('PDF:')) {
      const path = this.activity.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.activity.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.activity?.linkPDF);
  }

  getActivityName(): string {
    return this.activity.descripcion || 'Outreach Activity';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.activity.progressReport = this.progressReportService.calculateProgressReport(this.activity.fechaInicio);
  }
}

