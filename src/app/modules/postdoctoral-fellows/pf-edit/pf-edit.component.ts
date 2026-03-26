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
import { PostdoctoralFellowsService } from '../../../core/services/postdoctoral-fellows.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { BecariosPostdoctoralesDTO, InstitucionDTO, TipoSectorDTO, ResourceDTO, FundingTypeDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-pf-edit',
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
  templateUrl: './pf-edit.component.html',
  styleUrls: ['./pf-edit.component.scss']
})
export class PfEditComponent implements OnInit {
  isEditMode: boolean = false;
  fellowId: number | null = null;
  loading: boolean = false;

  // Lista de instituciones disponibles
  institutions: InstitucionDTO[] = [];
  loadingInstitutions: boolean = false;

  // Lista de tipos de sector disponibles
  sectorTypes: TipoSectorDTO[] = [];
  loadingSectorTypes: boolean = false;

  // Lista de recursos disponibles
  resources: ResourceDTO[] = [];
  loadingResources: boolean = false;

  // Lista de tipos de financiamiento disponibles
  fundingTypes: FundingTypeDTO[] = [];
  loadingFundingTypes: boolean = false;

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
  originalFellow: BecariosPostdoctoralesDTO | null = null;

  fellow: BecariosPostdoctoralesDTO = {
    descripcion: '',
    codigoANID: '',
    progressReport: undefined,
    fundingSource: '',
    resources: '',
    tipoProducto: { id: 14 } // ID 14 para Postdoctoral Fellows según DataInitializer (BECAPOSTDOCTO)
  };

  // Recursos seleccionados (para el formulario)
  selectedResources: ResourceDTO[] = [];

  // Tipos de financiamiento seleccionados (para el formulario)
  selectedFundingTypes: FundingTypeDTO[] = [];

  // Texto libre para "Other" funding source (id = 7)
  fundingOtherText: string = '';

  // Opciones de períodos (1 a 5) para progressReport múltiple
  periodOptions: { id: string; label: string }[] = [
    { id: '1', label: 'Period 1' },
    { id: '2', label: 'Period 2' },
    { id: '3', label: 'Period 3' },
    { id: '4', label: 'Period 4' },
    { id: '5', label: 'Period 5' }
  ];
  selectedPeriods: string[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private postdoctoralFellowsService: PostdoctoralFellowsService,
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
        this.fellowId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego el becario
        this.loadInstitutions();
        this.loadSectorTypes();
        this.loadResources();
        this.loadFundingTypes();
        // Usar setTimeout para asegurar que los catálogos se carguen antes de cargar el becario
        setTimeout(() => {
          if (this.fellowId !== null) {
            this.loadFellowForEdit(this.fellowId);
          }
        }, 100);
      } else {
        this.isEditMode = false;
        this.fellowId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nuevo becario
        this.loadInstitutions();
        this.loadSectorTypes();
        this.loadResources();
        this.loadFundingTypes();
        this.initializeNewFellow();
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Postdoctoral Fellow' : 'New Postdoctoral Fellow';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Postdoctoral Fellow' : 'Create Postdoctoral Fellow';
  }

  get backButtonText(): string {
    return 'Back to List';
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

  loadSectorTypes(): void {
    this.loadingSectorTypes = true;
    this.baseHttp.get<TipoSectorDTO[]>('/catalogs/sector-types').pipe(
      catchError(error => {
        console.error('Error loading sector types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingSectorTypes = false;
      })
    ).subscribe(items => {
      this.sectorTypes = items;
    });
  }

  loadResources(): void {
    this.loadingResources = true;
    this.baseHttp.get<ResourceDTO[]>('/catalogs/resources').pipe(
      catchError(error => {
        console.error('Error loading resources:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingResources = false;
      })
    ).subscribe(items => {
      this.resources = items;
    });
  }

  loadFundingTypes(): void {
    this.loadingFundingTypes = true;
    this.baseHttp.get<FundingTypeDTO[]>('/catalogs/funding-types').pipe(
      catchError(error => {
        console.error('Error loading funding types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingFundingTypes = false;
      })
    ).subscribe(items => {
      this.fundingTypes = items;
    });
  }

  initializeNewFellow(): void {
    this.fellow = {
      descripcion: '',
      codigoANID: '',
      progressReport: undefined,
      fundingSource: '',
      resources: '',
      tipoProducto: { id: 14 },
      fechaInicio: undefined,
      fechaTermino: undefined,
      basal: 'N',
      cluster: ''
    };
    this.isBasal = true;
    this.participants = [];
    this.selectedResources = [];
    this.selectedFundingTypes = [];
    this.selectedPeriods = [];
    this.originalFellow = null;
    this.selectedClusters = [];
  }

  loadFellowForEdit(id: number): void {
    this.loading = true;

    this.postdoctoralFellowsService.getPostdoctoralFellow(id).pipe(
      catchError(error => {
        console.error('Error loading postdoctoral fellow:', error);
        this.messageService.error('Error loading postdoctoral fellow. Please try again later.');
        this.router.navigate(['/postdoctoral-fellows']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(fellow => {
      if (fellow) {
        // Cargar participantes
        if (fellow.participantes && fellow.participantes.length > 0) {
          this.loadParticipants(fellow.participantes);
        } else {
          this.participants = [];
        }

        // Cargar recursos seleccionados
        if (fellow.resources) {
          try {
            const resourceIds = fellow.resources.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
            this.selectedResources = this.resources.filter(r => resourceIds.includes(r.id!));
          } catch (e) {
            this.selectedResources = [];
          }
        } else {
          this.selectedResources = [];
        }

        // Cargar tipos de financiamiento seleccionados
        this.selectedFundingTypes = [];
        this.fundingOtherText = '';
        if (fellow.fundingSource) {
          try {
            const raw = fellow.fundingSource.trim();
            if (raw.startsWith('[')) {
              // Nuevo formato JSON: [{"id":7,"text":"Volkswagen"},{"id":1}]
              const arr: any[] = JSON.parse(raw);
              const ids: number[] = [];
              arr.forEach(item => {
                if (item && typeof item.id === 'number') {
                  ids.push(item.id);
                  if (item.id === 7 && item.text) {
                    this.fundingOtherText = item.text;
                  }
                }
              });
              this.selectedFundingTypes = this.fundingTypes.filter(ft => ids.includes(ft.id!));
            } else {
              // Formato antiguo: "1,2,3"
              const fundingIds = raw.split(',').map(id => parseInt(id.trim(), 10)).filter(id => !isNaN(id));
              this.selectedFundingTypes = this.fundingTypes.filter(ft => fundingIds.includes(ft.id!));
            }
          } catch (e) {
            this.selectedFundingTypes = [];
            this.fundingOtherText = '';
          }
        }

        // Cargar períodos seleccionados desde el string de backend (progressReport, ej: "1,2")
        this.selectedPeriods = [];
        if (fellow.progressReport) {
          try {
            this.selectedPeriods = fellow.progressReport
              .split(',')
              .map(p => p.trim())
              .filter(p => p.length > 0);
          } catch {
            this.selectedPeriods = [];
          }
        }

        // Configurar checkbox Basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = fellow.basal === 'S' || fellow.basal === 's' || fellow.basal === '1';

        // Establecer tipo de producto si no está definido
        if (!fellow.tipoProducto) {
          fellow.tipoProducto = { id: 14 };
        }

        // Match institution if available
        // The institution comes from the backend with all data
        // Create a new object reference to trigger change detection in child component
        if (fellow.institucion) {
          fellow.institucion = {
            id: fellow.institucion.id,
            idDescripcion: fellow.institucion.idDescripcion,
            descripcion: fellow.institucion.descripcion
          };
        }

        // Match sector type if available
        if (fellow.tipoSector?.id && this.sectorTypes.length > 0) {
          const matchingSector = this.sectorTypes.find(st => st.id === fellow.tipoSector!.id);
          if (matchingSector) {
            fellow.tipoSector = matchingSector;
          }
        }

        this.fellow = fellow;
        this.originalFellow = JSON.parse(JSON.stringify(fellow));

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.fellow.cluster) {
          try {
            this.selectedClusters = this.fellow.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch (e) {
            this.selectedClusters = [];
          }
        }
      } else {
        this.messageService.error('Postdoctoral fellow not found');
        this.router.navigate(['/postdoctoral-fellows']);
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
    this.fellow.basal = checked ? 'S' : 'N';
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
    this.fellow.cluster = this.selectedClusters.join(',');
  }

  onResourceChange(resource: ResourceDTO, checked: boolean): void {
    if (checked) {
      if (!this.selectedResources.find(r => r.id === resource.id)) {
        this.selectedResources.push(resource);
      }
    } else {
      this.selectedResources = this.selectedResources.filter(r => r.id !== resource.id);
    }
    // Actualizar el string de resources
    this.fellow.resources = this.selectedResources.map(r => r.id).join(',');
  }

  isResourceSelected(resource: ResourceDTO): boolean {
    return this.selectedResources.some(r => r.id === resource.id);
  }

  onFundingTypeChange(fundingType: FundingTypeDTO, checked: boolean): void {
    if (checked) {
      if (!this.selectedFundingTypes.find(ft => ft.id === fundingType.id)) {
        this.selectedFundingTypes.push(fundingType);
      }
    } else {
      this.selectedFundingTypes = this.selectedFundingTypes.filter(ft => ft.id !== fundingType.id);
      // Si deseleccionan "Other", limpiamos el texto asociado
      if (fundingType.id === 7) {
        this.fundingOtherText = '';
      }
    }
    this.updateFundingSourceString();
  }

  isFundingTypeSelected(fundingType: FundingTypeDTO): boolean {
    return this.selectedFundingTypes.some(ft => ft.id === fundingType.id);
  }

  hasOtherFundingType(): boolean {
    return this.selectedFundingTypes.some(ft => ft.id === 7);
  }

  updateFundingSourceString(): void {
    const payload = this.selectedFundingTypes.map(ft => {
      if (ft.id === 7) {
        return { id: ft.id, text: this.fundingOtherText || '' };
      }
      return { id: ft.id };
    });
    this.fellow.fundingSource = JSON.stringify(payload);
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
    // Guardar como string separado por comas, o undefined si no hay selección
    this.fellow.progressReport = this.selectedPeriods.length > 0 ? this.selectedPeriods.join(',') : undefined;
  }

  compareInstitutions(inst1: InstitucionDTO | null, inst2: InstitucionDTO | null): boolean {
    if (!inst1 || !inst2) return inst1 === inst2;
    return inst1.id === inst2.id;
  }

  onInstitutionSelected(institution: InstitucionDTO): void {
    this.fellow.institucion = institution;
  }

  compareSectorTypes(sector1: TipoSectorDTO | null, sector2: TipoSectorDTO | null): boolean {
    if (!sector1 || !sector2) return sector1 === sector2;
    return sector1.id === sector2.id;
  }

  goBack(): void {
    this.router.navigate(['/postdoctoral-fellows']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalFellow) {
      const hasChanges = JSON.stringify(this.fellow) !== JSON.stringify(this.originalFellow);
      
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
    if (!this.fellow.descripcion || this.fellow.descripcion.trim() === '') {
      this.messageService.error('Research Topic is required');
      return false;
    }
    /*if (!this.fellow.fundingSource || this.fellow.fundingSource.trim() === '') {
      this.messageService.error('Funding Source is required');
      return false;
    }*/
    if (!this.fellow.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  saveFellow(): void {
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
              this.fellow.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const fellowData: BecariosPostdoctoralesDTO = {
          ...this.fellow,
          linkPDF: this.fellow.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          resources: this.selectedResources.map(r => r.id).join(','),
          cluster: this.selectedClusters.join(',')
        };

        const saveOperation = this.isEditMode && this.fellowId
          ? this.postdoctoralFellowsService.updatePostdoctoralFellow(this.fellowId, fellowData)
          : this.postdoctoralFellowsService.createPostdoctoralFellow(fellowData);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving postdoctoral fellow:', error);
        this.messageService.error('Error saving postdoctoral fellow. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(result => {
      if (result) {
        this.messageService.success(
          `Postdoctoral fellow ${this.isEditMode ? 'updated' : 'created'} successfully!`
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
        this.fellow.linkPDF = response.linkPDF;
        this.messageService.success('PDF uploaded successfully');
        this.clearSelectedFile();
      }
    });
  }

  getPdfFileName(): string {
    if (!this.fellow.linkPDF) {
      return '';
    }
    if (this.fellow.linkPDF.startsWith('PDF:')) {
      const path = this.fellow.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.fellow.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.fellow?.linkPDF);
  }

  getFellowName(): string {
    return this.fellow.descripcion || 'Postdoctoral Fellow';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.fellow.progressReport = this.progressReportService.calculateProgressReport(this.fellow.fechaInicio);
  }
}

