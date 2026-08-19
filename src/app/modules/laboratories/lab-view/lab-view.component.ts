import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTableModule } from '@angular/material/table';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';

import { MessageService } from '../../../core/services/message.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { TipoRRHHDTO } from '../../../core/models/backend-dtos';
import { LaboratoryService } from '../services/laboratory.service';
import {
  CLUSTER_OPTIONS,
  isMembershipActive,
  LAB_STATUS_LABELS,
  Laboratory,
  LaboratoryMembership,
  MEMBERSHIP_TYPE,
  MEMBERSHIP_STATUS_LABELS
} from '../models/laboratory.models';
import { formatLocalDate } from '../../../core/utils/date.util';
import { resolveMediaPathsInHtml } from '../../news/services/news-media-html.util';
import { getTipoRrhhLabel, sortTipoRrhhTypes } from '../services/laboratory-tipo-rrhh.util';

type ViewLanguage = 'es' | 'en';

@Component({
  selector: 'app-lab-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatTableModule,
    MatButtonToggleModule
  ],
  templateUrl: './lab-view.component.html',
  styleUrls: ['./lab-view.component.scss']
})
export class LabViewComponent implements OnInit {
  loading = false;
  itemId: number | null = null;
  item: Laboratory | null = null;
  viewLanguage: ViewLanguage = 'es';

  readonly clusterOptions = CLUSTER_OPTIONS;
  readonly labStatusLabels = LAB_STATUS_LABELS;
  readonly membershipStatusLabels = MEMBERSHIP_STATUS_LABELS;
  readonly memberColumns = ['person', 'resourceType', 'startDate', 'endDate'];
  researcherTypes: TipoRRHHDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private laboratoryService: LaboratoryService,
    private researcherService: ResearcherService,
    private messageService: MessageService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.researcherService.getResearcherTypes().subscribe(types => {
      this.researcherTypes = sortTipoRrhhTypes(types);
    });
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.itemId = parseInt(id, 10);
        const clusterParam = this.route.snapshot.queryParamMap.get('clusterId');
        const clusterId = clusterParam != null ? parseInt(clusterParam, 10) : undefined;
        this.loadItem(this.itemId, clusterId);
      }
    });
  }

  loadItem(id: number, clusterId?: number): void {
    this.loading = true;
    this.laboratoryService.getLaboratoryById(id, clusterId).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: lab => { this.item = lab; },
      error: () => {
        this.messageService.error('Could not load laboratory.');
        this.router.navigate(['/laboratories']);
      }
    });
  }

  get name(): string {
    if (!this.item) return '';
    return this.viewLanguage === 'es'
      ? (this.item.nameEs || this.item.nameEn)
      : (this.item.nameEn || this.item.nameEs);
  }

  get descriptionHtml(): SafeHtml {
    if (!this.item) return '';
    const html = this.viewLanguage === 'es'
      ? (this.item.descriptionEs || this.item.descriptionEn)
      : (this.item.descriptionEn || this.item.descriptionEs);
    const resolved = resolveMediaPathsInHtml(html, p => this.laboratoryService.resolveMediaUrl(p) || p);
    return this.sanitizer.bypassSecurityTrustHtml(resolved);
  }

  get imageUrl(): string | null {
    return this.item ? this.laboratoryService.resolveMediaUrl(this.item.imageUrl) : null;
  }

  get clusterLabel(): string {
    if (!this.item) return '';
    if (this.item.clusterLabel) return this.item.clusterLabel;
    const opt = this.clusterOptions.find(c => c.id === this.item!.clusterId);
    return opt?.label || '—';
  }

  get activeManagers(): LaboratoryMembership[] {
    return this.getByType(MEMBERSHIP_TYPE.LAB_MANAGER).filter(isMembershipActive);
  }

  get activeMembers(): LaboratoryMembership[] {
    return this.getByType(MEMBERSHIP_TYPE.MEMBER).filter(isMembershipActive);
  }

  get historicalMembers(): LaboratoryMembership[] {
    return this.getByType(MEMBERSHIP_TYPE.MEMBER).filter(m => !isMembershipActive(m));
  }

  private getByType(type: string): LaboratoryMembership[] {
    return (this.item?.memberships || []).filter(m => m.membershipType === type);
  }

  formatDate(date: string | null | undefined): string {
    return formatLocalDate(date, 'dd MMM yyyy', 'en-GB');
  }

  get directorResourceLabel(): string {
    if (!this.item) return '—';
    return this.item.directorResourceTypeLabel
      || getTipoRrhhLabel(this.researcherTypes, this.item.directorResourceType);
  }

  getResourceTypeLabel(value: string | LaboratoryMembership | null | undefined): string {
    if (value && typeof value === 'object') {
      return getTipoRrhhLabel(this.researcherTypes, value.resourceType, value.resourceTypeLabel);
    }
    return getTipoRrhhLabel(this.researcherTypes, value);
  }

  backToList(): void {
    this.router.navigate(['/laboratories']);
  }

  editLab(): void {
    if (this.itemId) {
      this.router.navigate(['/laboratories', this.itemId, 'edit'], {
        queryParams: { clusterId: this.item?.clusterId }
      });
    }
  }
}
