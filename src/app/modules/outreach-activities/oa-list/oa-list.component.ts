import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewModeService, ViewMode } from '../../../core/services/view-mode.service';
import { MessageService } from '../../../core/services/message.service';
import { OutreachActivitiesService } from '../../../core/services/outreach-activities.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { DifusionDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-oa-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    ListControlsComponent
  ],
  templateUrl: './oa-list.component.html',
  styleUrls: ['./oa-list.component.scss']
})
export class OAListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredActivities: DifusionDTO[] = [];
  activities: DifusionDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  private searchResults: DifusionDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'description' | 'type' | 'location' | 'attendees' | 'period' | 'date' | 'responsible' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private outreachActivitiesService: OutreachActivitiesService,
    private utilsService: UtilsService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('outreach-activities');
    this.basalOnly = state.basalOnly;
    
    this.loadActivities();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadActivities(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todas las actividades
    this.outreachActivitiesService.getOutreachActivities({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading outreach activities:', error);
        this.messageService.error('Error loading outreach activities. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.activities = response.content || [];
      this.searchResults = [...this.activities];
      this.applyFilters();
    });
  }

  ngOnDestroy(): void {
    if (this.viewModeSubscription) {
      this.viewModeSubscription.unsubscribe();
    }
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    // Guardar el modo en el servicio para persistencia
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: DifusionDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('outreach-activities', { searchTerm: term });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('outreach-activities', { basalOnly });
    this.applyFilters();
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(activity => {
        const basal = activity.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareActivities(a, b));
    }

    this.filteredActivities = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  getActivityTitle(activity: DifusionDTO): string {
    return activity.descripcion || 'Untitled Outreach Activity';
  }

  getActivityType(activity: DifusionDTO): string {
    return activity.tipoDifusion?.idDescripcion || 'N/A';
  }

  getLocation(activity: DifusionDTO): string {
    const parts: string[] = [];
    if (activity.ciudad) parts.push(activity.ciudad);
    if (activity.pais?.idDescripcion) parts.push(activity.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  onSort(column: 'description' | 'type' | 'location' | 'attendees' | 'period' | 'date' | 'responsible'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareActivities(a: DifusionDTO, b: DifusionDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'description':
        valueA = (this.getActivityTitle(a) || '').toLowerCase();
        valueB = (this.getActivityTitle(b) || '').toLowerCase();
        break;
      case 'type':
        valueA = (this.getActivityType(a) || '').toLowerCase();
        valueB = (this.getActivityType(b) || '').toLowerCase();
        break;
      case 'location':
        valueA = (this.getLocation(a) || '').toLowerCase();
        valueB = (this.getLocation(b) || '').toLowerCase();
        break;
      case 'attendees':
        valueA = a.numAsistentes ?? 0;
        valueB = b.numAsistentes ?? 0;
        break;
      case 'period':
        valueA = a.progressReport ?? 0;
        valueB = b.progressReport ?? 0;
        break;
      case 'date':
        valueA = a.fechaInicio || '';
        valueB = b.fechaInicio || '';
        break;
      case 'responsible':
        valueA = (a.mainResponsible || 'N/A').toLowerCase();
        valueB = (b.mainResponsible || 'N/A').toLowerCase();
        break;
      default:
        return 0;
    }

    if (valueA == null && valueB == null) return 0;
    if (valueA == null) return this.sortDirection === 'asc' ? -1 : 1;
    if (valueB == null) return this.sortDirection === 'asc' ? 1 : -1;

    let compareResult: number;
    if (typeof valueA === 'number' && typeof valueB === 'number') {
      compareResult = valueA - valueB;
    } else {
      compareResult = String(valueA).localeCompare(String(valueB));
    }

    return this.sortDirection === 'asc' ? compareResult : -compareResult;
  }

  viewActivity(activity: DifusionDTO): void {
    if (activity.id) {
      this.router.navigate(['/outreach-activities', activity.id]);
    }
  }

  editActivity(activity: DifusionDTO): void {
    if (activity.id) {
      this.router.navigate(['/outreach-activities', activity.id, 'edit']);
    }
  }

  deleteActivity(activity: DifusionDTO): void {
    if (!activity.id) return;
    
    const title = this.getActivityTitle(activity);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.outreachActivitiesService.deleteOutreachActivity(activity.id!).subscribe({
            next: () => {
              this.messageService.success('Outreach Activity Deleted', `${title} has been successfully removed.`);
              this.loadActivities();
            },
            error: (error) => {
              console.error('Error deleting outreach activity:', error);
              this.messageService.error('Error deleting outreach activity. Please try again.');
            }
          });
        }
      }
    );
  }

  getPdfUrl(activity: DifusionDTO): string | null {
    return this.utilsService.getPdfUrl(activity.linkPDF);
  }

  downloadPdf(event: Event, activity: DifusionDTO): void {
    event.stopPropagation();
    const pdfUrl = this.getPdfUrl(activity);
    if (pdfUrl) {
      window.open(pdfUrl, '_blank');
    }
  }

  getPdfFileName(activity: DifusionDTO): string {
    if (!activity.linkPDF) {
      return '';
    }
    if (activity.linkPDF.startsWith('PDF:')) {
      const path = activity.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = activity.linkPDF.split('/');
    return parts[parts.length - 1];
  }
}


