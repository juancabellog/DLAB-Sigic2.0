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
    MatDatepickerModule,
    MatNativeDateModule,
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

  // Control del checkbox Basal
  isBasal: boolean = false;

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
    tipoProducto: { id: 4 } // ID 4 para Thesis según DataInitializer
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
      fechaInicioPrograma: undefined,
      codigoANID: '',
      progressReport: undefined,
      tipoSector: '',
      tipoProducto: { id: 4 },
      fechaInicio: undefined,
      fechaTermino: undefined,
      basal: 'N'
    };
    this.isBasal = false;
    this.participants = [];
    this.selectedSectorTypes = [];
    this.originalThesis = null;
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
          tipoProducto: thesis.tipoProducto || { id: 4 }
        };
        
        this.originalThesis = null; // No hay original porque es una nueva tesis
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
        const thesisData: TesisDTO = {
          ...this.thesis,
          linkPDF: this.thesis.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          tipoSector: this.selectedSectorTypes.map(st => st.id).join(',')
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
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.thesis.progressReport = this.progressReportService.calculateProgressReport(this.thesis.fechaInicio);
  }
}
