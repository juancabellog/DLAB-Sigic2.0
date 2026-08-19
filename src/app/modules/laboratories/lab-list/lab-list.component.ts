import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { MessageService } from '../../../core/services/message.service';
import { ViewMode, ViewModeService } from '../../../core/services/view-mode.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { RRHHDTO } from '../../../core/models/backend-dtos';
import { LaboratoryService } from '../services/laboratory.service';
import { LabRowActionsComponent } from './lab-row-actions.component';
import {
  CLUSTER_OPTIONS,
  LAB_STATUS,
  LAB_STATUS_LABELS,
  LabStatus,
  Laboratory,
  plainTextFromHtml
} from '../models/laboratory.models';

@Component({
  selector: 'app-lab-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ListControlsComponent,
    LabRowActionsComponent
  ],
  templateUrl: './lab-list.component.html',
  styleUrls: ['./lab-list.component.scss']
})
export class LabListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  laboratories: Laboratory[] = [];
  filteredLabs: Laboratory[] = [];
  searchResults: Laboratory[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  filterClusterId: number | '' = '';
  filterStatus: LabStatus | 'all' | '' = '';
  filterDirectorId: number | '' = '';
  filterHasActiveMembers: 'all' | 'yes' | 'no' = 'all';

  researchers: RRHHDTO[] = [];
  readonly clusterOptions = CLUSTER_OPTIONS;
  readonly labStatusLabels = LAB_STATUS_LABELS;
  readonly labStatuses = [LAB_STATUS.ACTIVE, LAB_STATUS.INACTIVE];

  private viewModeSubscription?: Subscription;

  constructor(
    private laboratoryService: LaboratoryService,
    private researcherService: ResearcherService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    const state = this.listStateService.getState('laboratories');
    if (state.searchTerm) {
      this.isSearching = true;
    }
    this.loadResearchers();
    this.loadLaboratories();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadResearchers(): void {
    this.researcherService.getResearchers({ page: 0, size: 10000 }).subscribe(res => {
      this.researchers = res.content || [];
    });
  }

  loadLaboratories(): void {
    this.loading = true;
    this.laboratoryService.getLaboratories({ page: 0, size: 10000, status: 'all' }).pipe(
      catchError(() => {
        this.messageService.error('Error loading laboratories.');
        return of({ content: [], totalElements: 0 });
      }),
      finalize(() => { this.loading = false; })
    ).subscribe(res => {
      this.laboratories = res.content || [];
      this.searchResults = [...this.laboratories];
      this.applyFilters();
    });
  }

  onSearchResults(results: Laboratory[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('laboratories', { searchTerm: term });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onFiltersChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.filterClusterId = '';
    this.filterStatus = '';
    this.filterDirectorId = '';
    this.filterHasActiveMembers = 'all';
    this.applyFilters();
  }

  applyFilters(): void {
    let results = [...this.searchResults];

    if (this.filterClusterId !== '') {
      results = results.filter(l => l.clusterId === this.filterClusterId);
    }
    if (this.filterStatus && this.filterStatus !== 'all') {
      results = results.filter(l => l.status === this.filterStatus);
    } else if (!this.filterStatus) {
      results = results.filter(l => l.status === LAB_STATUS.ACTIVE);
    }
    if (this.filterDirectorId !== '') {
      results = results.filter(l => l.directorId === this.filterDirectorId);
    }
    if (this.filterHasActiveMembers === 'yes') {
      results = results.filter(l => (l.activeMemberCount ?? 0) > 0);
    } else if (this.filterHasActiveMembers === 'no') {
      results = results.filter(l => (l.activeMemberCount ?? 0) === 0);
    }

    this.filteredLabs = results;
    this.finalFilteredCount = results.length;
  }

  getName(item: Laboratory): string {
    return item.nameEs || item.nameEn || 'Untitled';
  }

  getShortDescription(item: Laboratory): string {
    const text = plainTextFromHtml(item.descriptionEs || item.descriptionEn || '');
    return text.length > 140 ? `${text.slice(0, 140)}…` : text;
  }

  getImageUrl(item: Laboratory): string | null {
    return this.laboratoryService.resolveMediaUrl(item.imageUrl);
  }

  getClusterLabel(item: Laboratory): string {
    if (item.clusterLabel) return item.clusterLabel;
    const opt = this.clusterOptions.find(c => c.id === item.clusterId);
    return opt?.label || '—';
  }

  getStatusColor(status: LabStatus): 'primary' | 'warn' | undefined {
    return status === LAB_STATUS.ACTIVE ? 'primary' : 'warn';
  }

  viewLab(item: Laboratory): void {
    this.router.navigate(['/laboratories', item.id, 'view'], {
      queryParams: { clusterId: item.clusterId }
    });
  }

  editLab(item: Laboratory): void {
    this.router.navigate(['/laboratories', item.id, 'edit'], {
      queryParams: { clusterId: item.clusterId }
    });
  }

  duplicateLab(item: Laboratory): void {
    if (!item.id) return;
    this.laboratoryService.getLaboratoryById(item.id, item.clusterId).subscribe({
      next: lab => {
        const copy = {
          ...lab,
          id: undefined,
          nameEs: `${lab.nameEs} (copy)`,
          nameEn: lab.nameEn ? `${lab.nameEn} (copy)` : '',
          status: LAB_STATUS.INACTIVE
        };
        this.laboratoryService.createLaboratory(copy).subscribe({
          next: created => {
            this.messageService.success('Laboratory duplicated.');
            this.router.navigate(['/laboratories', created.id, 'edit'], {
              queryParams: { clusterId: created.clusterId }
            });
          },
          error: () => this.messageService.error('Could not duplicate laboratory.')
        });
      }
    });
  }

  activateLab(item: Laboratory): void {
    if (!item.id) return;
    this.laboratoryService.activateLaboratory(item.id, item.clusterId).subscribe({
      next: () => {
        this.messageService.success('Laboratory activated.');
        this.loadLaboratories();
      },
      error: () => this.messageService.error('Could not activate laboratory.')
    });
  }

  deactivateLab(item: Laboratory): void {
    if (!item.id) return;
    this.laboratoryService.deactivateLaboratory(item.id, item.clusterId).subscribe({
      next: () => {
        this.messageService.success('Laboratory deactivated.');
        this.loadLaboratories();
      },
      error: () => this.messageService.error('Could not deactivate laboratory.')
    });
  }

  deleteLab(item: Laboratory): void {
    if (!item.id) return;
    this.messageService.confirm(
      `Delete laboratory "${item.nameEs || item.nameEn || item.id}"? This will also remove all memberships (director, managers and members) and cannot be undone.`,
      (accepted: boolean) => {
        if (!accepted) {
          return;
        }
        this.laboratoryService.deleteLaboratory(item.id!, item.clusterId).subscribe({
          next: () => {
            this.messageService.success('Laboratory deleted.');
            this.loadLaboratories();
          },
          error: (err) => {
            const msg = err?.error?.message
              || err?.error
              || 'Could not delete laboratory.';
            this.messageService.error(typeof msg === 'string' ? msg : 'Could not delete laboratory.');
          }
        });
      },
      'Delete laboratory'
    );
  }

  canDelete(item: Laboratory): boolean {
    return !!item.id;
  }
}
