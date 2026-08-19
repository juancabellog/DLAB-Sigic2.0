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
import { InstitutionSearchComponent } from '../../../shared/components/institution-search/institution-search.component';
import { MessageService } from '../../../core/services/message.service';
import { AwardService } from '../../../core/services/award.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import {
  AwardProductDTO,
  InstitucionDTO,
  PaisDTO,
  RRHHDTO
} from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-award-edit',
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
    ParticipantManagerComponent,
    InstitutionSearchComponent
  ],
  templateUrl: './award-edit.component.html',
  styleUrls: ['./award-edit.component.scss']
})
export class AwardEditComponent implements OnInit {
  isEditMode = false;
  awardId: number | null = null;
  loading = false;
  saving = false;

  award: AwardProductDTO = {
    descripcion: '',
    comentario: '',
    year: undefined,
    basal: 'S',
    progressReport: '',
    codigoANID: ''
  };

  selectedInstitution: InstitucionDTO | null = null;
  countryLabel = '—';
  countries: PaisDTO[] = [];
  participants: ParticipantDTO[] = [];
  isBasal = true;

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
    private awardService: AwardService,
    private messageService: MessageService,
    private baseHttp: BaseHttpService,
    private progressReportService: ProgressReportService,
    private researcherService: ResearcherService
  ) {}

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Award' : 'New Award';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Award' : 'Create Award';
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.awardId = Number(idParam);
    }
    this.loadCountries();
    if (this.isEditMode && this.awardId) {
      this.loadAward(this.awardId);
    }
  }

  loadCountries(): void {
    this.baseHttp.get<PaisDTO[]>('/catalogs/countries').subscribe({
      next: data => {
        this.countries = data || [];
        if (this.selectedInstitution) {
          this.resolveCountry(this.selectedInstitution);
        }
      },
      error: () => this.countries = []
    });
  }

  loadAward(id: number): void {
    this.loading = true;
    this.awardService.getAward(id).pipe(
      catchError(err => {
        console.error(err);
        this.messageService.error('Could not load award.');
        this.router.navigate(['/awards']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      if (!data) {
        return;
      }
      this.award = { ...data };
      this.isBasal = (data.basal || 'N').toUpperCase() === 'S';
      this.selectedClusters = this.parseClusters(data.cluster);
      this.selectedInstitution = data.institucion || (
        data.idInstitucion != null
          ? {
              id: data.idInstitucion,
              idDescripcion: data.institutionLabel,
              descripcion: data.institutionLabel,
              codigoPais: data.codigoPais,
              countryLabel: data.countryLabel
            }
          : null
      );
      if (data.countryLabel) {
        this.countryLabel = data.countryLabel;
      } else if (this.selectedInstitution) {
        this.resolveCountry(this.selectedInstitution);
      } else {
        this.countryLabel = '—';
      }
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

  private parseClusters(raw?: string): number[] {
    if (!raw) {
      return [];
    }
    return raw.split(',')
      .map(s => Number(s.trim()))
      .filter(n => Number.isFinite(n));
  }

  onInstitutionSelected(institution: InstitucionDTO): void {
    this.selectedInstitution = institution;
    this.award.idInstitucion = institution?.id ?? null;
    this.award.institucion = institution;
    this.resolveCountry(institution);
  }

  private resolveCountry(institution: InstitucionDTO | null): void {
    if (!institution) {
      this.countryLabel = '—';
      return;
    }
    if (institution.countryLabel) {
      this.countryLabel = institution.countryLabel;
      return;
    }
    const code = institution.codigoPais?.trim();
    if (!code) {
      this.countryLabel = '—';
      return;
    }
    const country = this.countries.find(c =>
      (c.codigo || '').toUpperCase() === code.toUpperCase()
    );
    this.countryLabel = country?.idDescripcion || code;
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
    this.award.basal = checked ? 'S' : 'N';
  }

  onYearChange(): void {
    if (this.award.year != null && Number.isFinite(Number(this.award.year))) {
      const y = Number(this.award.year);
      this.award.fechaInicio = `${y}-01-01`;
      this.award.progressReport = this.progressReportService.calculateProgressReport(this.award.fechaInicio);
    }
  }

  onParticipantsChange(list: ParticipantDTO[]): void {
    this.participants = list;
  }

  validateForm(): string | null {
    if (!this.award.descripcion?.trim()) {
      return 'Name is required.';
    }
    const maxYear = new Date().getFullYear() + 1;
    if (this.award.year == null || !Number.isInteger(Number(this.award.year))
      || Number(this.award.year) < 1900 || Number(this.award.year) > maxYear) {
      return `Year must be a valid year between 1900 and ${maxYear}.`;
    }
    if (!this.selectedInstitution?.id && !this.award.idInstitucion) {
      return 'Institution is required.';
    }
    return null;
  }

  buildPayload(): AwardProductDTO {
    const participantes = this.participants.map((p, index) => ({
      rrhhId: p.rrhhId,
      tipoParticipacionId: p.participationTypeId,
      orden: p.order || index + 1,
      corresponding: p.corresponding || false
    }));

    const year = Number(this.award.year);
    const institutionId = this.selectedInstitution?.id ?? this.award.idInstitucion ?? null;
    return {
      ...this.award,
      tipoProducto: { id: 21 },
      descripcion: this.award.descripcion?.trim(),
      comentario: this.award.comentario?.trim() || null,
      year,
      idInstitucion: institutionId,
      institucion: institutionId != null ? { id: institutionId } : null,
      fechaInicio: `${year}-01-01`,
      cluster: this.selectedClusters.join(','),
      basal: this.isBasal ? 'S' : 'N',
      participantes
    };
  }

  saveAward(): void {
    const error = this.validateForm();
    if (error) {
      this.messageService.error(error);
      return;
    }

    const payload = this.buildPayload();
    this.saving = true;
    const request = this.isEditMode && this.awardId
      ? this.awardService.updateAward(this.awardId, payload)
      : this.awardService.createAward(payload);

    request.pipe(
      catchError(err => {
        const msg = err?.error?.message || 'Could not save award.';
        this.messageService.error(msg);
        return of(null);
      }),
      finalize(() => this.saving = false)
    ).subscribe(result => {
      if (result) {
        this.messageService.success(this.isEditMode ? 'Award updated successfully.' : 'Award created successfully.');
        this.goBack();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/awards']);
  }
}
