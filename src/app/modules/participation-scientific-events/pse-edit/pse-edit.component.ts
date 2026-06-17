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
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import { ParticipationScientificEventsService } from '../../../core/services/participation-scientific-events.service';
import { ModalidadPresentacionDTO, ParticipacionEventoCientificoDTO, RRHHDTO, PaisDTO } from '../../../core/models/backend-dtos';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { of, firstValueFrom } from 'rxjs';

interface TipoParticipacionEventoOption {
  id?: number;
  idDescripcion?: string;
}

@Component({
  selector: 'app-pse-edit',
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
  templateUrl: './pse-edit.component.html',
  styleUrls: ['./pse-edit.component.scss']
})
export class PseEditComponent implements OnInit {
  isEditMode = false;
  itemId: number | null = null;
  loading = false;

  participationEventTypes: TipoParticipacionEventoOption[] = [];
  loadingParticipationEventTypes = false;

  presentationModalities: ModalidadPresentacionDTO[] = [];
  loadingPresentationModalities = false;

  countries: PaisDTO[] = [];
  loadingCountries = false;

  participants: ParticipantDTO[] = [];

  clusterOptions: { id: number; label: string }[] = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];
  selectedClusters: number[] = [];

  isBasal = true;
  selectedPdfFile: File | null = null;
  uploadingPdf = false;
  originalItem: any = null;

  item: ParticipacionEventoCientificoDTO = {
    descripcion: '',
    eventName: '',
    ciudad: '',
    pais: undefined,
    tipoParticipacionEvento: undefined,
    idModalidadPresentacion: undefined,
    codigoANID: '',
    progressReport: undefined,
    tipoProducto: { id: 16 }
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private pseService: ParticipationScientificEventsService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService,
    private progressReportService: ProgressReportService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.itemId = parseInt(id, 10);
        this.loading = true;
        this.loadCatalogs(() => {
          if (this.itemId !== null) {
            this.loadItemForEdit(this.itemId);
          }
        });
      } else {
        this.isEditMode = false;
        this.itemId = null;
        this.loading = false;
        this.loadCatalogs(() => {
          this.initializeNewItem();
        });
      }
    });
  }

  private loadCatalogs(callback?: () => void): void {
    let pending = 3;
    const done = () => {
      pending -= 1;
      if (pending === 0 && callback) callback();
    };
    this.loadCountries(done);
    this.loadParticipationEventTypes(done);
    this.loadPresentationModalities(done);
  }

  loadCountries(callback?: () => void): void {
    this.loadingCountries = true;
    this.baseHttp.get<PaisDTO[]>('/catalogs/countries').pipe(
      catchError(error => {
        console.error('Error loading countries:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingCountries = false;
        if (callback) callback();
      })
    ).subscribe(items => this.countries = items);
  }

  loadParticipationEventTypes(callback?: () => void): void {
    this.loadingParticipationEventTypes = true;
    this.baseHttp.get<TipoParticipacionEventoOption[]>('/catalogs/event-participation-types').pipe(
      catchError(error => {
        console.error('Error loading event participation types:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingParticipationEventTypes = false;
        if (callback) callback();
      })
    ).subscribe(items => this.participationEventTypes = items);
  }

  loadPresentationModalities(callback?: () => void): void {
    this.loadingPresentationModalities = true;
    this.baseHttp.get<ModalidadPresentacionDTO[]>('/catalogs/presentation-modalities').pipe(
      catchError(error => {
        console.error('Error loading presentation modalities:', error);
        return of([]);
      }),
      finalize(() => {
        this.loadingPresentationModalities = false;
        if (callback) callback();
      })
    ).subscribe(items => this.presentationModalities = items);
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Participation in Scientific Events' : 'New Participation in Scientific Events';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Participation in Scientific Events' : 'Add Participation in Scientific Events';
  }

  get backButtonText(): string {
    return 'Back to List';
  }

  initializeNewItem(): void {
    this.item = {
      descripcion: '',
      eventName: '',
      ciudad: '',
      pais: undefined,
      tipoParticipacionEvento: undefined,
      idModalidadPresentacion: undefined,
      codigoANID: '',
      progressReport: undefined,
      tipoProducto: { id: 16 },
      fechaInicio: null,
      fechaTermino: null,
      basal: 'N',
      cluster: ''
    };
    this.isBasal = false;
    this.participants = [];
    this.originalItem = null;
    this.selectedClusters = [];
  }

  loadItemForEdit(id: number): void {
    this.loading = true;
    this.pseService.getParticipationScientificEvent(id).pipe(
      catchError(error => {
        console.error('Error loading participation in scientific events:', error);
        this.messageService.error('Error loading participation in scientific events. Please try again later.');
        this.router.navigate(['/participation-scientific-events']);
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async item => {
      if (!item) {
        this.messageService.error('Participation in scientific events not found');
        this.router.navigate(['/participation-scientific-events']);
        return;
      }

      if (item.tipoParticipacionEvento?.id && this.participationEventTypes.length > 0) {
        const match = this.participationEventTypes.find(et => et.id === item.tipoParticipacionEvento!.id);
        if (match) {
          item.tipoParticipacionEvento = match;
        }
      }

      if (item.pais?.codigo && this.countries.length > 0) {
        const match = this.countries.find(c => c.codigo === item.pais!.codigo);
        if (match) {
          item.pais = match;
        }
      }

      if (!item.tipoProducto) {
        item.tipoProducto = { id: 16 };
      } else if (item.tipoProducto.id !== 16) {
        item.tipoProducto.id = 16;
      }

      this.item = item;
      this.originalItem = JSON.parse(JSON.stringify(item));
      this.isBasal = item.basal === 'S' || item.basal === 's' || item.basal === '1';

      if (item.participantes && item.participantes.length > 0) {
        const participantPromises = item.participantes.map(async (p, index) => {
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

        this.participants = await Promise.all(participantPromises);
      } else {
        this.participants = [];
      }

      this.selectedClusters = [];
      if (this.item.cluster) {
        this.selectedClusters = this.item.cluster
          .split(',')
          .map(v => parseInt(v.trim(), 10))
          .filter(v => !isNaN(v));
      }
    });
  }

  compareParticipationTypes(type1: TipoParticipacionEventoOption | null, type2: TipoParticipacionEventoOption | null): boolean {
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

  isModalityRequired(): boolean {
    const eventTypeId = this.item.tipoParticipacionEvento?.id;
    return eventTypeId === 1 || eventTypeId === 2;
  }

  onEventTypeChange(): void {
    if (!this.isModalityRequired()) {
      this.item.idModalidadPresentacion = undefined;
    }
  }

  onBasalChange(checked: boolean): void {
    this.item.basal = checked ? 'S' : 'N';
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
    this.item.cluster = this.selectedClusters.join(',');
  }

  onFechaInicioChange(): void {
    this.item.progressReport = this.progressReportService.calculateProgressReport(this.item.fechaInicio);
  }

  onStartDateChange(value: string | null): void {
    this.item.fechaInicio = value || null;
    this.onFechaInicioChange();
  }

  goBack(): void {
    this.router.navigate(['/participation-scientific-events']);
  }

  cancelEdit(): void {
    if (this.isEditMode && this.originalItem) {
      const hasChanges = JSON.stringify(this.item) !== JSON.stringify(this.originalItem);
      if (hasChanges) {
        this.messageService.confirm(
          'You have unsaved changes. Are you sure you want to cancel?',
          (accepted: boolean) => {
            if (accepted) this.goBack();
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

  saveItem(): void {
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

    const uploadPdfObservable = this.selectedPdfFile
      ? this.baseHttp.uploadFile<{ linkPDF: string; filename: string; message: string }>('/files/upload-pdf', this.selectedPdfFile).pipe(
          catchError(error => {
            console.error('Error uploading PDF:', error);
            this.messageService.error('Error uploading PDF. Please try again.');
            throw error;
          }),
          tap(response => {
            if (response && response.linkPDF) {
              this.item.linkPDF = response.linkPDF;
              this.clearSelectedFile();
            }
          })
        )
      : of(null as any);

    uploadPdfObservable.pipe(
      switchMap(() => {
        const itemToSave: ParticipacionEventoCientificoDTO = {
          ...this.item,
          tipoProducto: { id: 16 },
          fechaInicio: this.item.fechaInicio,
          fechaTermino: this.item.fechaTermino || undefined,
          linkPDF: this.item.linkPDF || undefined,
          basal: this.isBasal ? 'S' : 'N',
          participantes: participantesBackend.length > 0 ? participantesBackend : undefined,
          cluster: this.selectedClusters.join(',')
        };

        const saveOperation = this.isEditMode && this.itemId
          ? this.pseService.updateParticipationScientificEvent(this.itemId, itemToSave)
          : this.pseService.createParticipationScientificEvent(itemToSave);

        return saveOperation;
      }),
      catchError(error => {
        console.error('Error saving participation in scientific events:', error);
        this.messageService.error('Error saving participation in scientific events. Please try again.');
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(savedItem => {
      if (savedItem) {
        this.messageService.success(
          `Participation in scientific events ${this.isEditMode ? 'updated' : 'created'} successfully!`
        );
        this.goBack();
      }
    });
  }

  validateForm(): boolean {
    if (!this.item.descripcion || this.item.descripcion.trim() === '') {
      this.messageService.error('Title is required');
      return false;
    }
    if (!this.item.eventName || this.item.eventName.trim() === '') {
      this.messageService.error('Event name is required');
      return false;
    }
    if (!this.item.tipoParticipacionEvento?.id) {
      this.messageService.error('Event type is required');
      return false;
    }
    if (!this.item.ciudad || this.item.ciudad.trim() === '') {
      this.messageService.error('City is required');
      return false;
    }
    if (!this.item.fechaInicio) {
      this.messageService.error('Start Date is required');
      return false;
    }
    if (this.isModalityRequired() && !this.item.idModalidadPresentacion) {
      this.messageService.error('Modality is required for this event type');
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

  getPdfFileName(): string {
    if (!this.item.linkPDF) {
      return '';
    }
    if (this.item.linkPDF.startsWith('PDF:')) {
      const path = this.item.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = this.item.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.item?.linkPDF);
  }

  getItemName(): string {
    return this.item.descripcion || 'Participation in Scientific Events';
  }
}
