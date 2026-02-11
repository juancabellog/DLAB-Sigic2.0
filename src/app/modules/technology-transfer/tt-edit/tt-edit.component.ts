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
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { ParticipantManagerComponent, ParticipantDTO } from '../../../shared/components/participant-manager/participant-manager.component';
import { InstitutionSearchComponent } from '../../../shared/components/institution-search/institution-search.component';
import { MessageService } from '../../../core/services/message.service';
import { TechnologyTransferService } from '../../../core/services/technology-transfer.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { TransferenciaTecnologicaDTO, InstitucionDTO, TipoTransferenciaDTO, CategoriaTransferenciaDTO, PaisDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-tt-edit',
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
    MatTooltipModule,
    ParticipantManagerComponent,
    InstitutionSearchComponent
  ],
  templateUrl: './tt-edit.component.html',
  styleUrls: ['./tt-edit.component.scss']
})
export class TtEditComponent implements OnInit {
  isEditMode: boolean = false;
  transferId: number | null = null;
  loading: boolean = false;

  // Lista de instituciones disponibles
  institutions: InstitucionDTO[] = [];
  loadingInstitutions: boolean = false;

  // Lista de tipos de transferencia disponibles
  transferTypes: TipoTransferenciaDTO[] = [];
  loadingTransferTypes: boolean = false;

  // Lista de categorías de transferencia disponibles
  transferCategories: CategoriaTransferenciaDTO[] = [];
  loadingTransferCategories: boolean = false;

  // Lista de países disponibles
  countries: PaisDTO[] = [];
  loadingCountries: boolean = false;

  // Lista de participantes
  participants: ParticipantDTO[] = [];

  // Control del checkbox Basal
  isBasal: boolean = false;

  // Control de carga de PDF
  selectedPdfFile: File | null = null;
  uploadingPdf: boolean = false;

  // Datos originales para detectar cambios
  originalTransfer: TransferenciaTecnologicaDTO | null = null;

    transfer: TransferenciaTecnologicaDTO = {
      descripcion: '',
      ciudad: '',
      region: '',
      agno: undefined,
      codigoANID: '',
      progressReport: undefined,
      categoriaTransferencia: '',
      tipoProducto: { id: 6 } // ID 6 para Technology Transfer (según orden en DataInitializer)
    };

  // Categorías de transferencia seleccionadas (para el formulario)
  selectedCategories: CategoriaTransferenciaDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private technologyTransferService: TechnologyTransferService,
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
        this.transferId = parseInt(id);
        // Establecer loading inmediatamente cuando estamos editando
        this.loading = true;
        // Cargar catálogos y luego la transferencia
        this.loadInstitutions();
        this.loadTransferTypes();
        this.loadTransferCategories();
        this.loadCountries();
        // Usar setTimeout para asegurar que los catálogos se carguen antes de cargar la transferencia
        setTimeout(() => {
          if (this.transferId !== null) {
            this.loadTransferForEdit(this.transferId);
          }
        }, 100);
      } else {
        this.isEditMode = false;
        this.transferId = null;
        this.loading = false;
        // Cargar catálogos y luego inicializar nueva transferencia
        this.loadInstitutions();
        this.loadTransferTypes();
        this.loadTransferCategories();
        this.loadCountries();
        this.initializeNewTransfer();
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Technology Transfer' : 'New Technology Transfer';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Technology Transfer' : 'Create Technology Transfer';
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

  loadTransferTypes(): void {
    this.loadingTransferTypes = true;
    this.baseHttp.get<TipoTransferenciaDTO[]>('/catalogs/transfer-types').pipe(
      catchError(error => {
        console.error('Error loading transfer types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingTransferTypes = false;
      })
    ).subscribe(items => {
      this.transferTypes = items;
    });
  }

  loadTransferCategories(): void {
    this.loadingTransferCategories = true;
    this.baseHttp.get<CategoriaTransferenciaDTO[]>('/catalogs/transfer-categories').pipe(
      catchError(error => {
        console.error('Error loading transfer categories:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingTransferCategories = false;
      })
    ).subscribe(items => {
      this.transferCategories = items;
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

  initializeNewTransfer(): void {
    this.transfer = {
      descripcion: '',
      ciudad: '',
      region: '',
      agno: undefined,
      codigoANID: '',
      progressReport: undefined,
      categoriaTransferencia: '',
      tipoProducto: { id: 6 },
      fechaInicio: undefined,
      fechaTermino: undefined,
      basal: 'N'
    };
    this.isBasal = false;
    this.participants = [];
    this.selectedCategories = [];
    this.originalTransfer = null;
  }

  loadTransferForEdit(id: number): void {
    this.loading = true;

    this.technologyTransferService.getTechnologyTransfer(id).pipe(
      catchError(error => {
        console.error('Error loading technology transfer:', error);
        this.messageService.error('Error loading technology transfer. Please try again later.');
        this.router.navigate(['/technology-transfer']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(transfer => {
      if (transfer) {
        console.log('transfer', transfer);
        // Cargar participantes
        if (transfer.participantes && transfer.participantes.length > 0) {
          this.loadParticipants(transfer.participantes);
        } else {
          this.participants = [];
        }

        // Cargar categorías seleccionadas
        if (transfer.categoriaTransferencia) {
          try {
            const categoryIds = transfer.categoriaTransferencia.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
            this.selectedCategories = this.transferCategories.filter(cat => categoryIds.includes(cat.id!));
          } catch (e) {
            this.selectedCategories = [];
          }
        } else {
          this.selectedCategories = [];
        }

        // Configurar checkbox Basal
        // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
        this.isBasal = transfer.basal === 'S' || transfer.basal === 's' || transfer.basal === '1';

        // Establecer tipo de producto si no está definido
        if (!transfer.tipoProducto) {
          transfer.tipoProducto = { id: 6 };
        }

        // Match institution if available
        // The institution comes from the backend with all data
        // Create a new object reference to trigger change detection in child component
        if (transfer.institucion) {
          transfer.institucion = {
            id: transfer.institucion.id,
            idDescripcion: transfer.institucion.idDescripcion,
            descripcion: transfer.institucion.descripcion
          };
        }

        // Match transfer type if available
        if (transfer.tipoTransferencia?.id && this.transferTypes.length > 0) {
          const matchingType = this.transferTypes.find(tt => tt.id === transfer.tipoTransferencia!.id);
          if (matchingType) {
            transfer.tipoTransferencia = matchingType;
          }
        }

        // Match country if available
        if (transfer.pais?.codigo && this.countries.length > 0) {
          const matchingCountry = this.countries.find(c => c.codigo === transfer.pais!.codigo);
          if (matchingCountry) {
            transfer.pais = matchingCountry;
          }
        }

        this.transfer = transfer;
        this.originalTransfer = JSON.parse(JSON.stringify(transfer));
      } else {
        this.messageService.error('Technology transfer not found');
        this.router.navigate(['/technology-transfer']);
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
    this.transfer.basal = checked ? 'S' : 'N';
  }

  onCategoryChange(category: CategoriaTransferenciaDTO, checked: boolean): void {
    if (checked) {
      if (!this.selectedCategories.find(c => c.id === category.id)) {
        this.selectedCategories.push(category);
      }
    } else {
      this.selectedCategories = this.selectedCategories.filter(c => c.id !== category.id);
    }
    // Actualizar el string de categoriaTransferencia
    this.transfer.categoriaTransferencia = this.selectedCategories.map(c => c.id).join(',');
  }

  isCategorySelected(category: CategoriaTransferenciaDTO): boolean {
    return this.selectedCategories.some(c => c.id === category.id);
  }

  compareInstitutions(inst1: InstitucionDTO | null, inst2: InstitucionDTO | null): boolean {
    if (!inst1 || !inst2) return inst1 === inst2;
    return inst1.id === inst2.id;
  }

  onInstitutionSelected(institution: InstitucionDTO): void {
    this.transfer.institucion = institution;
  }

  compareTransferTypes(type1: TipoTransferenciaDTO | null, type2: TipoTransferenciaDTO | null): boolean {
    if (!type1 || !type2) return type1 === type2;
    return type1.id === type2.id;
  }

  compareCountries(country1: PaisDTO | null, country2: PaisDTO | null): boolean {
    if (!country1 || !country2) return country1 === country2;
    return country1.codigo === country2.codigo;
  }

  goBack(): void {
    this.router.navigate(['/technology-transfer']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalTransfer) {
      const hasChanges = JSON.stringify(this.transfer) !== JSON.stringify(this.originalTransfer);
      
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
    if (!this.transfer.descripcion || this.transfer.descripcion.trim() === '') {
      this.messageService.error('Description is required');
      return false;
    }
    if (!this.transfer.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    return true;
  }

  saveTransfer(): void {
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
              this.transfer.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    // Después de subir el PDF (si había uno), guardar el registro
    uploadPdfObservable.pipe(
      switchMap((uploadResult) => {
        const transferData: TransferenciaTecnologicaDTO = {
          ...this.transfer,
          linkPDF: this.transfer.linkPDF || undefined, // Asegurar que linkPDF se incluya explícitamente
          participantes: participantes,
          basal: this.isBasal ? 'S' : 'N',
          categoriaTransferencia: this.selectedCategories.map(c => c.id).join(',')
        };

        const saveOperation = this.isEditMode && this.transferId
          ? this.technologyTransferService.updateTechnologyTransfer(this.transferId, transferData)
          : this.technologyTransferService.createTechnologyTransfer(transferData);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving technology transfer:', error);
        this.messageService.error('Error saving technology transfer. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(result => {
      if (result) {
        this.messageService.success(
          `Technology transfer ${this.isEditMode ? 'updated' : 'created'} successfully!`
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
    if (!this.transfer.linkPDF) {
      return '';
    }
    if (this.transfer.linkPDF.startsWith('PDF:')) {
      const path = this.transfer.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.transfer.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.transfer?.linkPDF);
  }

  getTransferName(): string {
    return this.transfer.descripcion || 'Technology Transfer';
  }
  
  /**
   * Maneja el cambio de fecha de inicio y calcula automáticamente el progressReport
   */
  onFechaInicioChange(): void {
    this.transfer.progressReport = this.progressReportService.calculateProgressReport(this.transfer.fechaInicio);
  }
}
