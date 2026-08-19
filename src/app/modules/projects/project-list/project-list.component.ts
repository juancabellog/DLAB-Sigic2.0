import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
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
import { ProjectService } from '../../../core/services/project.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { FundingTypeDTO, ProjectProductDTO, TipoProyectoDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [
    CommonModule,
    LocalDatePipe,
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
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss']
})
export class ProjectListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  projects: ProjectProductDTO[] = [];
  filteredProjects: ProjectProductDTO[] = [];
  searchResults: ProjectProductDTO[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  fundingTypes: FundingTypeDTO[] = [];
  projectTypes: TipoProyectoDTO[] = [];

  filterFundingId: number | null = null;
  filterProjectTypeId: number | null = null;
  filterCluster: number | null = null;
  filterProgressReport: string | null = null;
  filterFromDate = '';
  filterToDate = '';

  clusterOptions = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];

  private viewModeSubscription?: Subscription;

  constructor(
    private projectService: ProjectService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService,
    private baseHttp: BaseHttpService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    this.loadCatalogs();
    this.loadProjects();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadCatalogs(): void {
    this.baseHttp.get<FundingTypeDTO[]>('/catalogs/funding-types').subscribe({
      next: data => this.fundingTypes = data || [],
      error: () => this.fundingTypes = []
    });
    this.baseHttp.get<TipoProyectoDTO[]>('/catalogs/project-types').subscribe({
      next: data => this.projectTypes = data || [],
      error: () => this.projectTypes = []
    });
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getProjects({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading projects', error);
        this.messageService.error('Error loading projects. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => this.loading = false)
    ).subscribe(response => {
      this.projects = response.content || [];
      this.searchResults = [...this.projects];
      this.applyFilters();
    });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: ProjectProductDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('projects', { searchTerm: term });
  }

  applyFilters(): void {
    let list = [...this.searchResults];

    if (this.filterFundingId != null) {
      list = list.filter(p => p.idFundingtype === this.filterFundingId
        || p.fundingType?.id === this.filterFundingId);
    }
    if (this.filterProjectTypeId != null) {
      const token = String(this.filterProjectTypeId);
      list = list.filter(p => (p.projectTypes || '')
        .split(',')
        .map(s => s.trim().toUpperCase())
        .includes(token.toUpperCase()));
    }
    if (this.filterCluster != null) {
      list = list.filter(p => (p.cluster || '')
        .split(',')
        .map(s => s.trim())
        .includes(String(this.filterCluster)));
    }
    if (this.filterProgressReport) {
      list = list.filter(p => p.progressReport === this.filterProgressReport);
    }
    if (this.filterFromDate) {
      list = list.filter(p => {
        const d = p.fechaInicio || p.awardDate || '';
        return d >= this.filterFromDate;
      });
    }
    if (this.filterToDate) {
      list = list.filter(p => {
        const d = p.fechaInicio || p.awardDate || '';
        return d && d <= this.filterToDate;
      });
    }

    this.filteredProjects = list;
    this.finalFilteredCount = list.length;
  }

  clearFilters(): void {
    this.filterFundingId = null;
    this.filterProjectTypeId = null;
    this.filterCluster = null;
    this.filterProgressReport = null;
    this.filterFromDate = '';
    this.filterToDate = '';
    this.applyFilters();
  }

  viewProject(project: ProjectProductDTO): void {
    if (project.id != null) {
      this.router.navigate(['/projects', project.id, 'view']);
    }
  }

  editProject(project: ProjectProductDTO): void {
    if (project.id != null) {
      this.router.navigate(['/projects', project.id, 'edit']);
    }
  }

  deleteProject(project: ProjectProductDTO): void {
    if (project.id == null) {
      return;
    }
    this.messageService.confirm(
      `Delete project "${project.descripcion || project.projectCode}"?`,
      (accepted: boolean) => {
        if (!accepted || project.id == null) {
          return;
        }
        this.projectService.deleteProject(project.id).subscribe({
          next: () => {
            this.messageService.success('Project deleted successfully.');
            this.loadProjects();
          },
          error: () => this.messageService.error('Could not delete the project.')
        });
      }
    );
  }

  getTitle(project: ProjectProductDTO): string {
    return project.descripcion || 'Untitled project';
  }

  getFundingLabel(project: ProjectProductDTO): string {
    if (project.otherFundingType) {
      return project.otherFundingType;
    }
    return project.fundingTypeLabel
      || project.fundingType?.idDescripcion
      || '—';
  }

  getTypesLabel(project: ProjectProductDTO): string {
    return project.projectTypesLabels || project.projectTypes || '—';
  }
}
