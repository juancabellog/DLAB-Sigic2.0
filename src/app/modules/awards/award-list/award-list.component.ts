import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewModeService, ViewMode } from '../../../core/services/view-mode.service';
import { MessageService } from '../../../core/services/message.service';
import { AwardService } from '../../../core/services/award.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { AwardProductDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-award-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ListControlsComponent
  ],
  templateUrl: './award-list.component.html',
  styleUrls: ['./award-list.component.scss']
})
export class AwardListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  awards: AwardProductDTO[] = [];
  filteredAwards: AwardProductDTO[] = [];
  searchResults: AwardProductDTO[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  filterYear: number | null = null;
  filterInstitution = '';
  filterCluster: number | null = null;
  filterProgressReport: string | null = null;

  clusterOptions = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];

  private viewModeSubscription?: Subscription;

  constructor(
    private awardService: AwardService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    this.loadAwards();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadAwards(): void {
    this.loading = true;
    this.awardService.getAwards({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading awards', error);
        this.messageService.error('Error loading awards. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => this.loading = false)
    ).subscribe(response => {
      this.awards = response.content || [];
      this.searchResults = [...this.awards];
      this.applyFilters();
    });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: AwardProductDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('awards', { searchTerm: term });
  }

  applyFilters(): void {
    let list = [...this.searchResults];

    if (this.filterYear != null) {
      list = list.filter(a => a.year === this.filterYear);
    }
    if (this.filterInstitution.trim()) {
      const term = this.filterInstitution.trim().toLowerCase();
      list = list.filter(a => (this.getInstitutionLabel(a) || '').toLowerCase().includes(term));
    }
    if (this.filterCluster != null) {
      list = list.filter(a => (a.cluster || '')
        .split(',')
        .map(s => s.trim())
        .includes(String(this.filterCluster)));
    }
    if (this.filterProgressReport) {
      list = list.filter(a => a.progressReport === this.filterProgressReport);
    }

    this.filteredAwards = list;
    this.finalFilteredCount = this.isSearching || this.hasActiveFilters()
      ? list.length
      : null;
  }

  hasActiveFilters(): boolean {
    return this.filterYear != null
      || !!this.filterInstitution.trim()
      || this.filterCluster != null
      || !!this.filterProgressReport;
  }

  clearFilters(): void {
    this.filterYear = null;
    this.filterInstitution = '';
    this.filterCluster = null;
    this.filterProgressReport = null;
    this.applyFilters();
  }

  getTitle(award: AwardProductDTO): string {
    return award.descripcion?.trim() || 'Untitled award';
  }

  getInstitutionLabel(award: AwardProductDTO): string {
    return award.institutionLabel
      || award.institucion?.descripcion
      || award.institucion?.idDescripcion
      || '—';
  }

  getCountryLabel(award: AwardProductDTO): string {
    return award.countryLabel
      || award.institucion?.countryLabel
      || '—';
  }

  /**
   * Awardees (participants) as a comma-separated list of researcher names.
   */
  getAwardees(award: AwardProductDTO): string {
    const raw = (award.participantesNombres || '').trim();
    if (!raw) {
      return '—';
    }
    // Normalize common separators from MySQL f_getRRHHProducto to ", "
    return raw
      .split(/[,;|]/)
      .map(s => s.trim())
      .filter(Boolean)
      .join(', ');
  }

  viewAward(award: AwardProductDTO): void {
    if (award.id != null) {
      this.router.navigate(['/awards', award.id, 'view']);
    }
  }

  editAward(award: AwardProductDTO): void {
    if (award.id != null) {
      this.router.navigate(['/awards', award.id, 'edit']);
    }
  }

  deleteAward(award: AwardProductDTO): void {
    if (award.id == null) {
      return;
    }
    this.messageService.confirm(
      `Are you sure you want to delete "${this.getTitle(award)}"?`,
      (accepted: boolean) => {
        if (!accepted) {
          return;
        }
        this.awardService.deleteAward(award.id!).subscribe({
          next: () => {
            this.messageService.success('Award deleted.');
            this.loadAwards();
          },
          error: () => this.messageService.error('Could not delete award.')
        });
      },
      'Delete award'
    );
  }
}
