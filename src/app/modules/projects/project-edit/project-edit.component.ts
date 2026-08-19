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
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of, firstValueFrom } from 'rxjs';

import {
  ParticipantManagerComponent,
  ParticipantDTO
} from '../../../shared/components/participant-manager/participant-manager.component';
import { MessageService } from '../../../core/services/message.service';
import { ProjectService } from '../../../core/services/project.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import {
  FundingTypeDTO,
  ProjectProductDTO,
  RRHHDTO,
  TipoProyectoDTO
} from '../../../core/models/backend-dtos';

const FUNDING_OTHER_ID = 7;
const PROJECT_TYPE_OTHER_ID = 4;

@Component({
  selector: 'app-project-edit',
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
    MatTooltipModule,
    ParticipantManagerComponent
  ],
  templateUrl: './project-edit.component.html',
  styleUrls: ['./project-edit.component.scss']
})
export class ProjectEditComponent implements OnInit {
  readonly FUNDING_OTHER_ID = FUNDING_OTHER_ID;
  readonly PROJECT_TYPE_OTHER_ID = PROJECT_TYPE_OTHER_ID;

  isEditMode = false;
  projectId: number | null = null;
  loading = false;
  saving = false;

  project: ProjectProductDTO = {
    descripcion: '',
    projectCode: '',
    awardDate: '',
    duration: undefined,
    totalAmount: undefined,
    totalAmountCenter: undefined,
    fechaInicio: '',
    fechaTermino: '',
    comentario: '',
    projectTypes: '',
    basal: 'S',
    progressReport: '',
    codigoANID: ''
  };

  fundingTypes: FundingTypeDTO[] = [];
  projectTypeOptions: TipoProyectoDTO[] = [];
  selectedFundingId: number | null = null;
  selectedProjectTypeIds: number[] = [];
  otherFundingSelected = false;
  otherProjectTypeSelected = false;

  participants: ParticipantDTO[] = [];
  isBasal = true;
  amountWarning = '';

  clusterOptions = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];
  selectedClusters: number[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private messageService: MessageService,
    private baseHttp: BaseHttpService,
    private progressReportService: ProgressReportService,
    private researcherService: ResearcherService
  ) {}

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Project' : 'New Project';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Project' : 'Create Project';
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.projectId = Number(idParam);
    }
    this.loadCatalogs();
    if (this.isEditMode && this.projectId) {
      this.loadProject(this.projectId);
    }
  }

  loadCatalogs(): void {
    this.baseHttp.get<FundingTypeDTO[]>('/catalogs/funding-types').subscribe({
      next: data => this.fundingTypes = data || [],
      error: () => this.fundingTypes = []
    });
    this.baseHttp.get<TipoProyectoDTO[]>('/catalogs/project-types').subscribe({
      next: data => this.projectTypeOptions = data || [],
      error: () => this.projectTypeOptions = []
    });
  }

  loadProject(id: number): void {
    this.loading = true;
    this.projectService.getProject(id).pipe(
      catchError(err => {
        console.error(err);
        this.messageService.error('Could not load project.');
        this.router.navigate(['/projects']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      if (!data) {
        return;
      }
      this.project = { ...data };
      this.isBasal = (data.basal || 'N').toUpperCase() === 'S';
      this.selectedFundingId = data.idFundingtype ?? data.fundingType?.id ?? null;
      this.onFundingChange();
      this.selectedProjectTypeIds = this.parseProjectTypes(data.projectTypes);
      this.onProjectTypesChange();
      this.selectedClusters = this.parseClusters(data.cluster);
      void this.loadParticipants(data.participantes || []);
    });
  }

  private async loadParticipants(participantes: any[]): Promise<void> {
    this.participants = [];
    for (const p of participantes) {
      if (!p.rrhhId) {
        continue;
      }
      try {
        const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
        if (researcher.id) {
          this.participants.push({
            rrhhId: researcher.id,
            fullName: researcher.fullname || '',
            idRecurso: researcher.idRecurso,
            orcid: researcher.orcid,
            participationTypeId: p.tipoParticipacionId,
            corresponding: p.corresponding || false,
            order: p.orden || 0
          });
        }
      } catch (error) {
        console.error('Error loading researcher', error);
      }
    }
  }

  private parseProjectTypes(raw?: string): number[] {
    if (!raw) {
      return [];
    }
    return raw.split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .map(token => Number(token))
      .filter(n => Number.isFinite(n));
  }

  private parseClusters(raw?: string): number[] {
    if (!raw) {
      return [];
    }
    return raw.split(',')
      .map(s => Number(s.trim()))
      .filter(n => Number.isFinite(n));
  }

  onFundingChange(): void {
    this.otherFundingSelected = this.selectedFundingId === FUNDING_OTHER_ID;
    if (!this.otherFundingSelected) {
      this.project.otherFundingType = null;
    }
  }

  onProjectTypesChange(): void {
    this.otherProjectTypeSelected = this.selectedProjectTypeIds.includes(PROJECT_TYPE_OTHER_ID);
    if (!this.otherProjectTypeSelected) {
      this.project.otherProjectType = null;
    }
  }

  isClusterSelected(id: number): boolean {
    return this.selectedClusters.includes(id);
  }

  onClusterChange(id: number, checked: boolean): void {
    if (checked) {
      if (!this.selectedClusters.includes(id)) {
        this.selectedClusters = [...this.selectedClusters, id].sort((a, b) => a - b);
      }
    } else {
      this.selectedClusters = this.selectedClusters.filter(c => c !== id);
    }
  }

  onBasalChange(checked: boolean): void {
    this.isBasal = checked;
    this.project.basal = checked ? 'S' : 'N';
  }

  onFechaInicioChange(): void {
    this.project.progressReport = this.progressReportService.calculateProgressReport(this.project.fechaInicio);
  }

  onAmountsChange(): void {
    const total = this.project.totalAmount;
    const center = this.project.totalAmountCenter;
    if (total != null && center != null && center > total) {
      this.amountWarning = 'Center amount should not exceed total project amount.';
    } else {
      this.amountWarning = '';
    }
  }

  onParticipantsChange(list: ParticipantDTO[]): void {
    this.participants = list;
  }

  validateForm(): string | null {
    if (!this.project.descripcion?.trim()) {
      return 'Project Title is required.';
    }
    if (!this.project.projectCode?.trim()) {
      return 'Project Code is required.';
    }
    if (this.project.projectCode.trim().length > 100) {
      return 'Project Code max length is 100.';
    }
    if (this.selectedFundingId == null) {
      return 'Funding Source is required.';
    }
    if (this.otherFundingSelected && !this.project.otherFundingType?.trim()) {
      return 'Other Funding Source is required when Other is selected.';
    }
    if (!this.selectedProjectTypeIds.length) {
      return 'Project Type is required.';
    }
    if (this.otherProjectTypeSelected && !this.project.otherProjectType?.trim()) {
      return 'Other Project Type is required when Other is selected.';
    }
    if (!this.project.awardDate) {
      return 'Award Date is required.';
    }
    if (this.project.duration == null || this.project.duration <= 0 || !Number.isInteger(Number(this.project.duration))) {
      return 'Duration must be a positive integer.';
    }
    if (this.project.totalAmount == null || this.project.totalAmount < 0 || !Number.isInteger(Number(this.project.totalAmount))) {
      return 'Total amount of the project must be a non-negative integer.';
    }
    if (this.project.totalAmountCenter == null || this.project.totalAmountCenter < 0
      || !Number.isInteger(Number(this.project.totalAmountCenter))) {
      return 'Total amount awarded by the center must be a non-negative integer.';
    }
    if (!this.project.fechaInicio) {
      return 'Start Date is required.';
    }
    if (this.project.fechaTermino && this.project.fechaTermino < this.project.fechaInicio) {
      return 'Ending Date must be equal to or after Start Date.';
    }
    return null;
  }

  buildPayload(): ProjectProductDTO {
    const projectTypes = this.selectedProjectTypeIds
      .map(v => String(v))
      .join(',');

    const participantes = this.participants.map((p, index) => ({
      rrhhId: p.rrhhId,
      tipoParticipacionId: p.participationTypeId,
      orden: p.order || index + 1,
      corresponding: p.corresponding || false
    }));

    return {
      ...this.project,
      tipoProducto: { id: 19 },
      projectCode: this.project.projectCode?.trim(),
      descripcion: this.project.descripcion?.trim(),
      comentario: this.project.comentario?.trim() || null,
      idFundingtype: this.selectedFundingId!,
      otherFundingType: this.otherFundingSelected ? (this.project.otherFundingType?.trim() || null) : null,
      projectTypes,
      otherProjectType: this.otherProjectTypeSelected ? (this.project.otherProjectType?.trim() || null) : null,
      nameSocialOrganizations: this.project.nameSocialOrganizations?.trim() || null,
      namePublicSectorEntities: this.project.namePublicSectorEntities?.trim() || null,
      namePrivateSectorEntities: this.project.namePrivateSectorEntities?.trim() || null,
      nameTradeRegionalAssociations: this.project.nameTradeRegionalAssociations?.trim() || null,
      nameSTEntities: this.project.nameSTEntities?.trim() || null,
      cluster: this.selectedClusters.join(','),
      basal: this.isBasal ? 'S' : 'N',
      participantes
    };
  }

  saveProject(): void {
    const error = this.validateForm();
    if (error) {
      this.messageService.error(error);
      return;
    }
    this.onAmountsChange();
    if (this.amountWarning) {
      this.messageService.info(this.amountWarning);
    }

    const payload = this.buildPayload();
    this.saving = true;
    const request = this.isEditMode && this.projectId
      ? this.projectService.updateProject(this.projectId, payload)
      : this.projectService.createProject(payload);

    request.pipe(
      catchError(err => {
        const msg = err?.error?.message || 'Could not save project.';
        this.messageService.error(msg);
        return of(null);
      }),
      finalize(() => this.saving = false)
    ).subscribe(result => {
      if (result) {
        this.messageService.success(this.isEditMode ? 'Project updated successfully.' : 'Project created successfully.');
        this.goBack();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }
}
