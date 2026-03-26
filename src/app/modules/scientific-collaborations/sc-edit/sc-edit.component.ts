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
import { InstitutionSearchComponent } from '../../../shared/components/institution-search/institution-search.component';
import { MessageService } from '../../../core/services/message.service';
import { ScientificCollaborationsService } from '../../../core/services/scientific-collaborations.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { ColaboracionDTO, TipoColaboracionDTO, PaisDTO, InstitucionDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-sc-edit',
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
    ParticipantManagerComponent,
    InstitutionSearchComponent
  ],
  templateUrl: './sc-edit.component.html',
  styleUrls: ['./sc-edit.component.scss']
})
export class ScEditComponent implements OnInit {
  isEditMode: boolean = false;
  collaborationId: number | null = null;
  loading: boolean = false;

  // Lista de tipos de colaboración disponibles
  collaborationTypes: TipoColaboracionDTO[] = [];
  loadingCollaborationTypes: boolean = false;

  // Lista de instituciones disponibles
  institutions: InstitucionDTO[] = [];
  loadingInstitutions: boolean = false;

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

  // País de destino seleccionado (para el formulario)
  paisDestino: PaisDTO | null = null;

  // Control del checkbox Basal
  isBasal: boolean = true;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalCollaboration: ColaboracionDTO | null = null;

  // Opciones de períodos (1 a 5) para progressReport múltiple
  periodOptions: { id: string; label: string }[] = [
    { id: '1', label: 'Period 1' },
    { id: '2', label: 'Period 2' },
    { id: '3', label: 'Period 3' },
    { id: '4', label: 'Period 4' },
    { id: '5', label: 'Period 5' }
  ];
  selectedPeriods: string[] = [];

  collaboration: ColaboracionDTO = {
    descripcion: '',
    codigoANID: '',
    progressReport: undefined,
    ciudadOrigen: '',
    ciudadDestino: '',
    tipoProducto: { id: 12 } // ID 12 para COLLABORATIONS
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private scientificCollaborationsService: ScientificCollaborationsService,
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
        this.collaborationId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego la colaboración
        this.loadCollaborationTypes();
        this.loadInstitutions();
        this.loadCountries();
        // Usar setTimeout para asegurar que los catálogos se carguen antes de cargar la colaboración
        setTimeout(() => {
          if (this.collaborationId !== null) {
            this.loadCollaborationForEdit(this.collaborationId);
          }
        }, 100);
      } else {
        this.isEditMode = false;
        this.collaborationId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nueva colaboración
        this.loadCollaborationTypes();
        this.loadInstitutions();
        this.loadCountries();
        this.initializeNewCollaboration();
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Scientific Collaboration' : 'New Scientific Collaboration';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Scientific Collaboration' : 'Create Scientific Collaboration';
  }

  get backButtonText(): string {
    return 'Back to List';
  }

  loadCollaborationTypes(): void {
    this.loadingCollaborationTypes = true;
    this.baseHttp.get<TipoColaboracionDTO[]>('/catalogs/collaboration-types').pipe(
      catchError(error => {
        console.error('Error loading collaboration types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingCollaborationTypes = false;
      })
    ).subscribe(items => {
      this.collaborationTypes = items;
    });
  }

  loadInstitutions(): void {
    this.loadingInstitutions = true;
    this.baseHttp.get<InstitucionDTO[]>('/catalogs/institutions').pipe(
      catchError(error => {
        console.error('Error loading institutions:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingInstitutions = false;
      })
    ).subscribe(items => {
      this.institutions = items;
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

  initializeNewCollaboration(): void {
    this.collaboration = {
      descripcion: '',
      codigoANID: '',
      progressReport: undefined,
      ciudadOrigen: '',
      ciudadDestino: '',
      tipoProducto: { id: 12 },
      fechaInicio: undefined,
      fechaTermino: undefined,
      basal: 'N',
      cluster: ''
    };
    this.paisDestino = null;
    this.isBasal = true;
    this.participants = [];
    this.originalCollaboration = null;
    this.selectedClusters = [];
    this.selectedPeriods = [];
  }

  loadCollaborationForEdit(id: number): void {
    this.loading = true;

    this.scientificCollaborationsService.getScientificCollaboration(id).pipe(
      catchError(error => {
        console.error('Error loading scientific collaboration:', error);
        this.messageService.error('Error loading scientific collaboration. Please try again later.');
        this.router.navigate(['/scientific-collaborations']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(collaboration => {
      if (collaboration) {
        // Cargar participantes
        if (collaboration.participantes && collaboration.participantes.length > 0) {
          this.loadParticipants(collaboration.participantes);
        } else {
          this.participants = [];
        }

        // Configurar checkbox Basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = collaboration.basal === 'S' || collaboration.basal === 's' || collaboration.basal === '1';

        // Establecer tipo de producto si no está definido
        if (!collaboration.tipoProducto) {
          collaboration.tipoProducto = { id: 12 };
        }

        // Match collaboration type if available
        if (collaboration.tipoColaboracion?.id && this.collaborationTypes.length > 0) {
          const matchingType = this.collaborationTypes.find(ct => ct.id === collaboration.tipoColaboracion!.id);
          if (matchingType) {
            collaboration.tipoColaboracion = matchingType;
          }
        }

        // Match institution if available
        if (collaboration.institucion?.id && this.institutions.length > 0) {
          const matchingInstitution = this.institutions.find(i => i.id === collaboration.institucion!.id);
          if (matchingInstitution) {
            collaboration.institucion = matchingInstitution;
          }
        }

        // Match origin country if available
        if (collaboration.paisOrigen?.codigo && this.countries.length > 0) {
          const matchingCountry = this.countries.find(c => c.codigo === collaboration.paisOrigen!.codigo);
          if (matchingCountry) {
            collaboration.paisOrigen = matchingCountry;
          }
        }

        // Match destination country if available
        if (collaboration.codigoPaisDestino && this.countries.length > 0) {
          const matchingCountry = this.countries.find(c => c.codigo === collaboration.codigoPaisDestino);
          if (matchingCountry) {
            this.paisDestino = matchingCountry;
          } else {
            this.paisDestino = null;
          }
        } else {
          this.paisDestino = null;
        }

        // Cargar períodos seleccionados desde el string de backend (progressReport, ej: "1,2")
        this.selectedPeriods = [];
        if (collaboration.progressReport) {
          try {
            this.selectedPeriods = collaboration.progressReport
              .split(',')
              .map(p => p.trim())
              .filter(p => p.length > 0);
          } catch (e) {
            this.selectedPeriods = [];
          }
        }

        this.collaboration = collaboration;
        this.originalCollaboration = JSON.parse(JSON.stringify(collaboration));

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.collaboration.cluster) {
          try {
            this.selectedClusters = this.collaboration.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch (e) {
            this.selectedClusters = [];
          }
        }
      } else {
        this.messageService.error('Scientific collaboration not found');
        this.router.navigate(['/scientific-collaborations']);
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
    this.collaboration.basal = checked ? 'S' : 'N';
  }

  isPeriodSelected(periodId: string): boolean {
    return this.selectedPeriods.includes(periodId);
  }

  onPeriodChange(periodId: string, checked: boolean): void {
    if (checked) {
      if (!this.selectedPeriods.includes(periodId)) {
        this.selectedPeriods.push(periodId);
      }
    } else {
      this.selectedPeriods = this.selectedPeriods.filter(id => id !== periodId);
    }
    this.collaboration.progressReport = this.selectedPeriods.length > 0 ? this.selectedPeriods.join(',') : undefined;
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
    this.collaboration.cluster = this.selectedClusters.join(',');
  }

  onDestinationCountryChange(country: PaisDTO | null): void {
    this.paisDestino = country;
    this.collaboration.codigoPaisDestino = country?.codigo || undefined;
  }

  compareCollaborationTypes(type1: TipoColaboracionDTO | null, type2: TipoColaboracionDTO | null): boolean {
    if (!type1 || !type2) return type1 === type2;
    return type1.id === type2.id;
  }

  compareInstitutions(inst1: InstitucionDTO | null, inst2: InstitucionDTO | null): boolean {
    if (!inst1 || !inst2) return inst1 === inst2;
    return inst1.id === inst2.id;
  }

  onInstitutionSelected(institution: InstitucionDTO): void {
    this.collaboration.institucion = institution;
  }

  compareCountries(country1: PaisDTO | null, country2: PaisDTO | null): boolean {
    if (!country1 || !country2) return country1 === country2;
    return country1.codigo === country2.codigo;
  }

  goBack(): void {
    this.router.navigate(['/scientific-collaborations']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalCollaboration) {
      const hasChanges = JSON.stringify(this.collaboration) !== JSON.stringify(this.originalCollaboration);
      
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
    if (!this.collaboration.descripcion || this.collaboration.descripcion.trim() === '') {
      this.messageService.error('Name is required');
      return false;
    }
    if (!this.collaboration.ciudadOrigen || this.collaboration.ciudadOrigen.trim() === '') {
      this.messageService.error('Origin City is required');
      return false;
    }
    if (!this.collaboration.ciudadDestino || this.collaboration.ciudadDestino.trim() === '') {
      this.messageService.error('Destination City is required');
      return false;
    }
    if (!this.collaboration.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  saveCollaboration(): void {
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
              this.collaboration.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const collaborationData: ColaboracionDTO = {
          ...this.collaboration,
          linkPDF: this.collaboration.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          codigoPaisDestino: this.paisDestino?.codigo || undefined,
          cluster: this.selectedClusters.join(',')
        };

        const saveOperation = this.isEditMode && this.collaborationId
          ? this.scientificCollaborationsService.updateScientificCollaboration(this.collaborationId, collaborationData)
          : this.scientificCollaborationsService.createScientificCollaboration(collaborationData);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving scientific collaboration:', error);
        this.messageService.error('Error saving scientific collaboration. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(result => {
      if (result) {
        this.messageService.success(
          `Scientific collaboration ${this.isEditMode ? 'updated' : 'created'} successfully!`
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
        this.collaboration.linkPDF = response.linkPDF;
        this.messageService.success('PDF uploaded successfully');
        this.clearSelectedFile();
      }
    });
  }

  getPdfFileName(): string {
    if (!this.collaboration.linkPDF) {
      return '';
    }
    if (this.collaboration.linkPDF.startsWith('PDF:')) {
      const path = this.collaboration.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.collaboration.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.collaboration?.linkPDF);
  }

  getCollaborationName(): string {
    return this.collaboration.descripcion || 'Scientific Collaboration';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.collaboration.progressReport = this.progressReportService.calculateProgressReport(this.collaboration.fechaInicio);
  }
}

