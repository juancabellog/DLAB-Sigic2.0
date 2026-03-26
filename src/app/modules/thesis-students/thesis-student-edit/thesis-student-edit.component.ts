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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { ParticipantManagerComponent, ParticipantDTO } from '../../../shared/components/participant-manager/participant-manager.component';
import { InstitutionSearchComponent } from '../../../shared/components/institution-search/institution-search.component';
import { MessageService } from '../../../core/services/message.service';
import { ThesisService } from '../../../core/services/thesis.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { TesisDTO, InstitucionDTO, GradoAcademicoDTO, EstadoTesisDTO, TipoSectorDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-thesis-student-edit',
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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    ParticipantManagerComponent,
    InstitutionSearchComponent
  ],
  templateUrl: './thesis-student-edit.component.html',
  styleUrls: ['./thesis-student-edit.component.scss']
})
export class ThesisStudentEditComponent implements OnInit {
  isEditMode: boolean = false;
  thesisId: number | null = null;
  loading: boolean = false;

  // Lista de instituciones disponibles
  institutions: InstitucionDTO[] = [];
  loadingInstitutions: boolean = false;

  // Lista de grados académicos disponibles
  academicDegrees: GradoAcademicoDTO[] = [];
  loadingAcademicDegrees: boolean = false;

  // Lista de estados de tesis disponibles
  thesisStatuses: EstadoTesisDTO[] = [];
  loadingThesisStatuses: boolean = false;

  // Lista de tipos de sector disponibles
  sectorTypes: TipoSectorDTO[] = [];
  loadingSectorTypes: boolean = false;

  // Lista de participantes (estudiantes y tutores)
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

  // Opciones de períodos (1 a 5) para progressReport múltiple
  periodOptions: { id: string; label: string }[] = [
    { id: '1', label: 'Period 1' },
    { id: '2', label: 'Period 2' },
    { id: '3', label: 'Period 3' },
    { id: '4', label: 'Period 4' },
    { id: '5', label: 'Period 5' }
  ];
  selectedPeriods: string[] = [];

  // Control del checkbox Basal
  isBasal: boolean = true;

  // Control de ayuda expandible para formato de cita
  showCitationFormat: boolean = false;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalThesis: TesisDTO | null = null;

  thesis: TesisDTO = {
    descripcion: '',
    nombreCompletoTitulo: '',
    fechaInicioPrograma: undefined,
    codigoANID: '',
    progressReport: undefined,
    tipoSector: '',
    tipoProducto: { id: 11 } // ID 11 para Thesis (backend default)
  };

  // Tipos de sector seleccionados (para el formulario)
  selectedSectorTypes: TipoSectorDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private thesisService: ThesisService,
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
        this.thesisId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego la tesis
        this.loadInstitutions();
        this.loadAcademicDegrees();
        this.loadThesisStatuses();
        this.loadSectorTypes();
        // Usar setTimeout para asegurar que los catálogos se carguen antes de cargar la tesis
        setTimeout(() => {
          if (this.thesisId !== null) {
            this.loadThesisForEdit(this.thesisId);
          }
        }, 100);
      } else {
        this.isEditMode = false;
        this.thesisId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nueva tesis
        this.loadInstitutions();
        this.loadAcademicDegrees();
        this.loadThesisStatuses();
        this.loadSectorTypes();
        
        // Verificar si hay un query param para copiar desde otra tesis
        const copyFromId = this.route.snapshot.queryParams['copyFrom'];
        if (copyFromId) {
          this.loading = true;
          // Limpiar el query param de la URL
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
            replaceUrl: true
          });
          // Cargar la tesis a copiar después de que los catálogos se hayan cargado
          setTimeout(() => {
            this.loadThesisForCopy(parseInt(copyFromId));
          }, 100);
        } else {
          this.initializeNewThesis();
        }
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Thesis' : 'New Thesis';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Thesis' : 'Create Thesis';
  }

  get backButtonText(): string {
    return 'Back to List';
  }

  get isPhDThesis(): boolean {
    return this.thesis.gradoAcademico?.id === 3;
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

  loadAcademicDegrees(): void {
    this.loadingAcademicDegrees = true;
    this.baseHttp.get<GradoAcademicoDTO[]>('/catalogs/academic-degrees').pipe(
      catchError(error => {
        console.error('Error loading academic degrees:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingAcademicDegrees = false;
      })
    ).subscribe(items => {
      this.academicDegrees = items;
    });
  }

  loadThesisStatuses(): void {
    this.loadingThesisStatuses = true;
    this.baseHttp.get<EstadoTesisDTO[]>('/catalogs/thesis-status').pipe(
      catchError(error => {
        console.error('Error loading thesis statuses:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingThesisStatuses = false;
      })
    ).subscribe(items => {
      this.thesisStatuses = items;
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

  initializeNewThesis(): void {
    this.thesis = {
      descripcion: '',
      nombreCompletoTitulo: '',
      fechaInicioPrograma: '',
      codigoANID: '',
      progressReport: undefined,
      tipoSector: '',
      tipoProducto: { id: 11 },
      fechaInicio: '',
      fechaTermino: '',
      basal: 'N',
      cluster: ''
    };
    this.isBasal = true;
    this.participants = [];
    this.selectedSectorTypes = [];
    this.originalThesis = null;
    this.selectedClusters = [];
    this.selectedPeriods = [];
  }

  loadThesisForEdit(id: number): void {
    this.loading = true;

    this.thesisService.getThesisById(id).pipe(
      catchError(error => {
        console.error('Error loading thesis:', error);
        this.messageService.error('Error loading thesis. Please try again later.');
        this.router.navigate(['/thesis-students']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(thesis => {
      if (thesis) {
        // Cargar participantes
        if (thesis.participantes && thesis.participantes.length > 0) {
          this.loadParticipants(thesis.participantes);
        } else {
          this.participants = [];
        }

        // Cargar tipos de sector seleccionados
        if (thesis.tipoSector) {
          try {
            const sectorIds = thesis.tipoSector.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
            this.selectedSectorTypes = this.sectorTypes.filter(st => sectorIds.includes(st.id!));
          } catch (e) {
            this.selectedSectorTypes = [];
          }
        } else {
          this.selectedSectorTypes = [];
        }

        // Configurar checkbox Basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = thesis.basal === 'S' || thesis.basal === 's' || thesis.basal === '1';

        // Establecer tipo de producto si no está definido
        if (!thesis.tipoProducto) {
          thesis.tipoProducto = { id: 4 };
        }

        // Create new object references for institutions to trigger change detection in child components
        if (thesis.institucionOG) {
          thesis.institucionOG = {
            id: thesis.institucionOG.id,
            idDescripcion: thesis.institucionOG.idDescripcion,
            descripcion: thesis.institucionOG.descripcion
          };
        }
        if (thesis.institucion) {
          thesis.institucion = {
            id: thesis.institucion.id,
            idDescripcion: thesis.institucion.idDescripcion,
            descripcion: thesis.institucion.descripcion
          };
        }

        this.thesis = thesis;
        this.originalThesis = JSON.parse(JSON.stringify(thesis));

        // Cargar clusters seleccionados desde el string de backend
        this.selectedClusters = [];
        if (this.thesis.cluster) {
          try {
            this.selectedClusters = this.thesis.cluster
              .split(',')
              .map(id => parseInt(id.trim(), 10))
              .filter(id => !isNaN(id));
          } catch {
            this.selectedClusters = [];
          }
        }

        // Cargar períodos seleccionados desde el string de backend (progressReport, ej: "1,2")
        this.selectedPeriods = [];
        if (this.thesis.progressReport) {
          try {
            this.selectedPeriods = this.thesis.progressReport
              .split(',')
              .map(p => p.trim())
              .filter(p => p !== '');
            this.selectedPeriods = this.sanitizePeriodIds(this.selectedPeriods);
          } catch {
            this.selectedPeriods = [];
          }
        }

      } else {
        this.messageService.error('Thesis not found');
        this.router.navigate(['/thesis-students']);
      }
    });
  }

  loadThesisForCopy(id: number): void {
    this.loading = true;

    this.thesisService.getThesisById(id).pipe(
      catchError(error => {
        console.error('Error loading thesis for copy:', error);
        this.messageService.error('Error loading thesis to copy. Please try again later.');
        this.router.navigate(['/thesis-students']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(thesis => {
      if (thesis) {
        // Cargar participantes
        if (thesis.participantes && thesis.participantes.length > 0) {
          this.loadParticipants(thesis.participantes);
        } else {
          this.participants = [];
        }

        // Cargar tipos de sector seleccionados
        if (thesis.tipoSector) {
          try {
            const sectorIds = thesis.tipoSector.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
            this.selectedSectorTypes = this.sectorTypes.filter(st => sectorIds.includes(st.id!));
          } catch (e) {
            this.selectedSectorTypes = [];
          }
        } else {
          this.selectedSectorTypes = [];
        }

        // Configurar checkbox Basal
        this.isBasal = thesis.basal === 'S' || thesis.basal === 's' || thesis.basal === '1';

        // Copiar la tesis pero sin ID, sin progressReport, y con estado inicial
        this.thesis = {
          ...thesis,
          id: undefined, // No copiar el ID
          progressReport: undefined, // No copiar el progressReport
          estadoProducto: undefined, // No copiar el estado (se establecerá como Draft o Pending)
          estadoTesis: undefined, // No copiar el estado de tesis
          tipoProducto: thesis.tipoProducto || { id: 11 }
        };
        
        this.originalThesis = null; // No hay original porque es una nueva tesis
        this.selectedPeriods = [];
        this.messageService.info('Thesis data loaded. You can now edit and save as a new thesis.');
      } else {
        this.messageService.error('Thesis not found');
        this.router.navigate(['/thesis-students']);
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
    this.thesis.basal = checked ? 'S' : 'N';
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
    this.thesis.cluster = this.selectedClusters.join(',');
  }

  isPeriodSelected(periodId: string): boolean {
    return this.selectedPeriods.includes(periodId);
  }

  private isValidPeriodId(periodId: string): boolean {
    return this.periodOptions.some(opt => opt.id === periodId);
  }

  private sanitizePeriodIds(periods: string[]): string[] {
    const unique = Array.from(new Set(periods.map(p => p.trim()).filter(p => p !== '')));
    return unique.filter(p => this.isValidPeriodId(p));
  }

  onPeriodChange(periodId: string, checked: boolean): void {
    if (checked) {
      if (!this.selectedPeriods.includes(periodId)) {
        this.selectedPeriods.push(periodId);
      }
    } else {
      this.selectedPeriods = this.selectedPeriods.filter(id => id !== periodId);
    }
    this.selectedPeriods = this.sanitizePeriodIds(this.selectedPeriods);
    // Guardar como string separado por comas, o undefined si no hay selección
    this.thesis.progressReport = this.selectedPeriods.length > 0 ? this.selectedPeriods.join(',') : undefined;
  }

  onSectorTypeChange(sector: TipoSectorDTO, checked: boolean): void {
    if (checked) {
      if (!this.selectedSectorTypes.find(st => st.id === sector.id)) {
        this.selectedSectorTypes.push(sector);
      }
    } else {
      this.selectedSectorTypes = this.selectedSectorTypes.filter(st => st.id !== sector.id);
    }
    // Actualizar el string de tipoSector
    this.thesis.tipoSector = this.selectedSectorTypes.map(st => st.id).join(',');
  }

  isSectorTypeSelected(sector: TipoSectorDTO): boolean {
    return this.selectedSectorTypes.some(st => st.id === sector.id);
  }

  compareInstitutions(inst1: InstitucionDTO | null, inst2: InstitucionDTO | null): boolean {
    if (!inst1 || !inst2) return inst1 === inst2;
    return inst1.id === inst2.id;
  }

  onDegreeGrantingInstitutionSelected(institution: InstitucionDTO): void {
    this.thesis.institucionOG = institution;
  }

  onStudentInstitutionSelected(institution: InstitucionDTO): void {
    this.thesis.institucion = institution;
  }

  compareAcademicDegrees(deg1: GradoAcademicoDTO | null, deg2: GradoAcademicoDTO | null): boolean {
    if (!deg1 || !deg2) return deg1 === deg2;
    return deg1.id === deg2.id;
  }

  compareThesisStatuses(status1: EstadoTesisDTO | null, status2: EstadoTesisDTO | null): boolean {
    if (!status1 || !status2) return status1 === status2;
    return status1.id === status2.id;
  }

  goBack(): void {
    this.router.navigate(['/thesis-students']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalThesis) {
      const hasChanges = JSON.stringify(this.thesis) !== JSON.stringify(this.originalThesis);
      
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
    if (!this.thesis.nombreCompletoTitulo || this.thesis.nombreCompletoTitulo.trim() === '') {
      this.messageService.error('Thesis title is required');
      return false;
    }
    if (!this.thesis.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    // Cross-validation between end date and "Finished" status (id = 1)
    const finishedId = 1;
    const isFinishedStatus = this.thesis.estadoTesis?.id === finishedId;
    const hasEndDate = !!this.thesis.fechaTermino;

    if (hasEndDate && !isFinishedStatus) {
      this.messageService.error('If an end date is set, the thesis status must be "Finished".');
      return false;
    }

    if (isFinishedStatus && !hasEndDate) {
      this.messageService.error('If the thesis status is "Finished", an end date is required.');
      return false;
    }

    return true;
  }

  saveThesis(): void {
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
              this.thesis.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const sanitizedPeriods = this.sanitizePeriodIds(this.selectedPeriods);
        const thesisData: TesisDTO = {
          ...this.thesis,
          linkPDF: this.thesis.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          tipoSector: this.selectedSectorTypes.map(st => st.id).join(','),
          cluster: this.selectedClusters.join(','),
          progressReport: sanitizedPeriods.length > 0
            ? sanitizedPeriods.join(',')
            : undefined
        };

        const saveOperation = this.isEditMode && this.thesisId
          ? this.thesisService.updateThesis(this.thesisId, thesisData)
          : this.thesisService.createThesis(thesisData);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving thesis:', error);
        this.messageService.error('Error saving thesis. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(result => {
      if (result) {
        this.messageService.success(
          `Thesis ${this.isEditMode ? 'updated' : 'created'} successfully!`
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

  getPdfFileName(): string {
    if (!this.thesis.linkPDF) {
      return '';
    }
    if (this.thesis.linkPDF.startsWith('PDF:')) {
      const path = this.thesis.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.thesis.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.thesis?.linkPDF);
  }

  getThesisName(): string {
    return this.thesis.nombreCompletoTitulo || 'Thesis';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport (un solo período).
   * Siempre actualiza la selección automática; el usuario luego puede ajustar manualmente los períodos.
   */
  onFechaInicioChange(): void {
    const pr = this.progressReportService.calculateProgressReport(this.thesis.fechaInicio);
    this.selectedPeriods = this.sanitizePeriodIds(pr ? [pr] : []);
    this.thesis.progressReport = this.selectedPeriods.length > 0 ? this.selectedPeriods.join(',') : undefined;
  }

  onExamDateChange(value: string | null): void {
    this.thesis.fechaInicio = value || undefined;
    this.onFechaInicioChange();
  }

  /**
   * Parsea la cita pegada en el campo descripción y rellena automáticamente
   * varios campos de la tesis (título, fechas, programa, institución, estado,
   * períodos y participantes Estudiante/Director/Codirector).
   */
  async onParseDescription(): Promise<void> {
    const description = (this.thesis.descripcion || '').trim();

    if (!description) {
      this.messageService.error('Citation is empty. Please paste the thesis citation first.');
      return;
    }

    try {
      const lines = description.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
      if (lines.length === 0) {
        this.messageService.error('Citation format is not valid.');
        return;
      }

      const firstLine = lines[0];
      const basalLine = lines.find(l => /BASAL\s+(?:Año|Year)/i.test(l)) || lines[lines.length - 1];

      // Línea 1: <Estudiante>. <Titulo>. [opcional: <Programa>, <Institucion>. <Fechas>]
      const firstParts = firstLine.split('.').map(p => p.trim()).filter(p => p.length > 0);
      const studentName = firstParts[0] || '';
      const titlePart = firstParts.length > 1 ? firstParts[1] : '';
      let restAfterTitle = firstParts.length > 2 ? firstParts.slice(2).join('.').trim() : '';

      if (!studentName || !titlePart) {
        console.warn('Unexpected citation format in first line:', firstLine);
      }

      // Si después del título no viene programa+institución en la misma línea,
      // intentar tomarlos de la siguiente línea no vacía (caso multi-línea).
      if (!restAfterTitle && lines.length > 1) {
        // Buscar la primera línea siguiente que contenga una coma (Programa, Institución)
        const programLine = lines.slice(1).find(l => l.includes(','));
        if (programLine) {
          restAfterTitle = programLine.trim();
        }
      }

      // <Programa>, <Institucion>. [<Fechas> si están en la misma línea]
      const programInstitutionSplit = restAfterTitle ? restAfterTitle.split(',') : [];
      const programText = programInstitutionSplit[0]?.trim() || '';
      const institutionAndRest = programInstitutionSplit.slice(1).join(',').trim();

      let institutionText = '';
      let datesPart = '';

      const lastPeriodIdx = institutionAndRest.lastIndexOf('.');
      if (lastPeriodIdx >= 0) {
        institutionText = institutionAndRest.substring(0, lastPeriodIdx).trim();
        datesPart = institutionAndRest.substring(lastPeriodIdx + 1).trim();
      } else {
        institutionText = institutionAndRest;
      }

      // Si las fechas no están al final de la línea 1, buscar una línea con formato "x - y - z"
      if (!datesPart) {
        const dateLine = lines.find(l => /^\S.*\s-\s.*\s-\s/.test(l));
        if (dateLine) {
          datesPart = dateLine.trim();
        }
      }

      // Separar por "-" con o sin espacios para soportar "dd/MM/yyyy-..." y "dd/MM/yyyy - ..."
      const dateTokens = datesPart.split(/\s*-\s*/).map(t => t.trim()).filter(t => t.length > 0);
      const fechaInicioProgramaStr = dateTokens[0];
      const fechaInicioStr = dateTokens[1];
      const fechaTerminoStr = dateTokens[2];
      // Asignar título y fechas
      if (titlePart) {
        this.thesis.nombreCompletoTitulo = titlePart;
      }
      this.thesis.fechaInicioPrograma = this.normalizeDate(fechaInicioProgramaStr);
      this.thesis.fechaInicio = this.normalizeDate(fechaInicioStr);
      this.thesis.fechaTermino = this.normalizeDate(fechaTerminoStr);

      // Detectar grado académico a partir del texto del programa
      const detectedDegree = this.detectAcademicDegree(programText);
      if (detectedDegree) {
        this.thesis.gradoAcademico = detectedDegree;
      }

      // Intentar asociar institución otorgante usando el texto parseado
      const matchedInstitution = this.findInstitutionByName(institutionText);
      if (matchedInstitution) {
        this.thesis.institucionOG = matchedInstitution;
      }

      // Línea de Director / Co-supervisor: buscar la línea que contenga "Supervisor" o "Director"
      let directorName: string | undefined;
      let coDirectorName: string | undefined;
      const directorLine = lines.find(l => /Thesis Supervisor|Director de Tesis|Co-supervisor|Codirector/i.test(l));
      if (directorLine) {
        const directorMatch = directorLine.match(/(?:Thesis Supervisor|Director de Tesis):\s*([^,]+?)(?=,|$)/i);
        const coDirectorMatch = directorLine.match(/Co-?supervisor:\s*([^,]+?)(?=,|$)/i);
        const d = directorMatch?.[1]?.trim();
        const c = coDirectorMatch?.[1]?.trim();
        directorName = d && !/^N\/A$/i.test(d) ? d : undefined;
        coDirectorName = c && !/^N\/A$/i.test(c) ? c : undefined;
      }

      // Línea BASAL: períodos y estado de tesis
      if (basalLine) {
        this.isBasal = /BASAL/i.test(basalLine);
        this.thesis.basal = this.isBasal ? 'S' : 'N';

        const periodSet = new Set<string>();
        // Aceptar "Año" o "Year" (ej. "BASAL Year 5" o "BASAL Año 1,2,5")
        const basalRegex = /BASAL\s+(?:Año|Year)\s+([0-9,\s]+)/gi;
        let match: RegExpExecArray | null;
        while ((match = basalRegex.exec(basalLine)) !== null) {
          const list = match[1]
            .split(',')
            .map(p => p.trim())
            .filter(p => p.length > 0);
          list.forEach(p => periodSet.add(p));
        }

        // Asegurar período si aparece "BASAL Año/Year 5" sin captura en la lista
        if (/BASAL\s+(?:Año|Year)\s*5\b/i.test(basalLine)) {
          periodSet.add('5');
        }

        this.selectedPeriods = this.sanitizePeriodIds(Array.from(periodSet).sort());
        this.thesis.progressReport = this.selectedPeriods.length > 0 ? this.selectedPeriods.join(',') : undefined;

        // Estado de tesis: (in progress) / (In Progress) => id=2, (Finished) => id=1
        const hasFinished = /\(Finished\)/i.test(basalLine);
        const hasInProgress = /\(in progress\)/i.test(basalLine) || /\(In Progress\)/i.test(basalLine);

        let targetStatusId: number | undefined;
        if (hasFinished) {
          targetStatusId = 1;
        } else if (hasInProgress) {
          targetStatusId = 2;
        }

        if (targetStatusId && this.thesisStatuses && this.thesisStatuses.length > 0) {
          const status = this.thesisStatuses.find(s => s.id === targetStatusId);
          if (status) {
            this.thesis.estadoTesis = status;
          }
        }
      }

      // Crear participantes a partir de los nombres detectados
      const participantPromises: Promise<void>[] = [];

      if (studentName) {
        participantPromises.push(this.addParticipantFromName(studentName, 7)); // Estudiante
      }
      if (directorName) {
        participantPromises.push(this.addParticipantFromName(directorName, 12)); // Director de tesis
      }
      if (coDirectorName) {
        participantPromises.push(this.addParticipantFromName(coDirectorName, 13)); // Codirector de tesis
      }

      if (participantPromises.length > 0) {
        await Promise.all(participantPromises);
      }

      // Recalcular orden de participantes
      this.participants.forEach((p, index) => {
        p.order = index + 1;
      });

      this.messageService.success('Citation parsed successfully. Fields have been populated.');
    } catch (error) {
      console.error('Error parsing thesis citation:', error);
      this.messageService.error('Could not parse the citation. Please verify the format.');
    }
  }

  private normalizeDate(value?: string): string | undefined {
    if (!value) return undefined;
    const trimmed = value.trim();
    if (!trimmed || /^N\/A$/i.test(trimmed)) return undefined;

    // Formato explícito dd/MM/yyyy (ej: 01/03/2017)
    const dmyMatch = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(trimmed);
    if (dmyMatch) {
      const day = dmyMatch[1];
      const month = dmyMatch[2];
      const year = dmyMatch[3];
      return `${year}-${month}-${day}`;
    }

    // Si ya viene en formato ISO yyyy-MM-dd, retornarlo tal cual
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      return trimmed;
    }

    // Reemplazar '/' por '-' y dejar que Date intente parsear
    const normalized = trimmed.replace(/\//g, '-');
    const parsed = new Date(normalized);
    if (isNaN(parsed.getTime())) {
      return undefined;
    }

    const year = parsed.getFullYear();
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const day = String(parsed.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private detectAcademicDegree(programText?: string): GradoAcademicoDTO | undefined {
    if (!programText) {
      return undefined;
    }

    const text = programText.toLowerCase();
    let targetId: number | undefined;

    if (text.includes('doctorado')) {
      targetId = 3;
    } else if (text.includes('magister')) {
      targetId = 2;
    } else {
      targetId = 1;
    }

    if (!this.academicDegrees || this.academicDegrees.length === 0) {
      return undefined;
    }

    return this.academicDegrees.find(d => d.id === targetId);
  }

  private findInstitutionByName(name?: string): InstitucionDTO | undefined {
    if (!name || !this.institutions || this.institutions.length === 0) {
      return undefined;
    }

    const normalizedName = this.normalizeText(name);

    // Buscar coincidencia que contenga el texto (ignorando acentos y mayúsculas/minúsculas)
    const exactMatch = this.institutions.find(inst => {
      const instName = this.normalizeText(inst.descripcion || inst.idDescripcion || '');
      return instName.includes(normalizedName);
    });

    return exactMatch || undefined;
  }

  private normalizeText(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  /**
   * Elimina prefijos de título como "Dr.", "Dra.", "Dr ", "Dra " (case-insensitive)
   * al inicio del nombre, para facilitar la búsqueda de RRHH.
   */
  private cleanTitlePrefixes(name: string): string {
    if (!name) {
      return '';
    }
    return name.replace(/^\s*(dr\.?\s+|dra\.?\s+)/i, '').trim();
  }

  private async addParticipantFromName(name: string, participationTypeId: number): Promise<void> {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }

    try {
      // Eliminar prefijos de título (Dr., Dra., etc.) para mejorar el match
      const searchName = this.cleanTitlePrefixes(trimmedName);

      // Usar el servicio avanzado de matching en backend (ResearcherMatchingService)
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
        participationTypeId,
        corresponding: false,
        order: this.participants.length + 1
      };

      this.participants = [...this.participants, newParticipant];
      this.onParticipantsChange(this.participants);
    } catch (error) {
      console.error(`Error searching researcher for "${trimmedName}":`, error);
    }
  }
}
