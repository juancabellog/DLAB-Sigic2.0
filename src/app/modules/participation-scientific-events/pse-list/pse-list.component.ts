import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { ParticipacionEventoCientificoDTO } from '../../../core/models/backend-dtos';
import { MessageService } from '../../../core/services/message.service';
import { ViewMode, ViewModeService } from '../../../core/services/view-mode.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { ParticipationScientificEventsService } from '../../../core/services/participation-scientific-events.service';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { UtilsService } from '../../../core/services/utils.service';

@Component({
  selector: 'app-pse-list',
  standalone: true,
  imports: [
    CommonModule,
    LocalDatePipe,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    ListControlsComponent
  ],
  templateUrl: './pse-list.component.html',
  styleUrls: ['./pse-list.component.scss']
})
export class PseListComponent implements OnInit, OnDestroy {
  private readonly PAGE_SIZE = 200;
  viewMode: ViewMode = 'card';
  participations: ParticipacionEventoCientificoDTO[] = [];
  filteredParticipations: ParticipacionEventoCientificoDTO[] = [];
  isSearching = false;
  loading = false;
  exportLoading = false;
  basalOnly = false;
  finalFilteredCount: number | null = null;
  private searchResults: ParticipacionEventoCientificoDTO[] = [];
  private viewModeSubscription?: Subscription;

  sortColumn: 'title' | 'eventName' | 'type' | 'location' | 'period' | 'date' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private router: Router,
    private messageService: MessageService,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService,
    private pseService: ParticipationScientificEventsService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    const state = this.listStateService.getState('participation-scientific-events');
    this.basalOnly = state.basalOnly;
    this.loadParticipations();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadParticipations(): void {
    this.loading = true;
    this.fetchParticipationsPage(0, []);
  }

  private fetchParticipationsPage(page: number, accumulator: ParticipacionEventoCientificoDTO[]): void {
    this.pseService.getParticipationScientificEvents({
      page,
      size: this.PAGE_SIZE,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading participation in scientific events:', error);
        this.messageService.error('Error loading participation in scientific events. Please try again later.');
        this.loading = false;
        return of(null as any);
      })
    ).subscribe(response => {
      if (!response) {
        return;
      }

      const chunk = response.content || [];
      const allItems = accumulator.concat(chunk);
      const totalPages = response.totalPages ?? 0;
      const isLastPage = totalPages === 0 || page >= totalPages - 1;

      if (isLastPage) {
        this.participations = allItems;
        this.searchResults = [...this.participations];
        this.applyFilters();
        this.loading = false;
        return;
      }

      this.fetchParticipationsPage(page + 1, allItems);
    });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: ParticipacionEventoCientificoDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
    this.listStateService.saveState('participation-scientific-events', { searchTerm });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('participation-scientific-events', { basalOnly });
    this.applyFilters();
  }

  onExportRequested(): void {
    if (this.filteredParticipations.length === 0) {
      this.messageService.info('There are no results to export.');
      return;
    }
    this.exportLoading = true;
    this.pseService
      .exportParticipationScientificEventsToExcel({
        sort: this.mapExportSortColumn(this.sortColumn),
        direction: this.sortDirection === 'asc' ? 'ASC' : 'DESC'
      })
      .pipe(
        catchError(error => {
          console.error('Error exporting participation in scientific events to Excel:', error);
          this.messageService.error(
            'Error exporting participation in scientific events. Please try again later.'
          );
          return of(null as any);
        }),
        finalize(() => {
          this.exportLoading = false;
        })
      )
      .subscribe(blob => {
        if (!blob) {
          return;
        }
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'participation-scientific-events.xlsx';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.messageService.success('Export started. Your download should begin shortly.');
      });
  }

  /**
   * Maps list-view sort keys to JPA property paths for the export endpoint.
   */
  private mapExportSortColumn(
    column: 'title' | 'eventName' | 'type' | 'location' | 'period' | 'date' | null
  ): string {
    const map: Record<string, string> = {
      title: 'descripcion',
      eventName: 'eventName',
      type: 'tipoParticipacionEvento.id',
      location: 'ciudad',
      period: 'progressReport',
      date: 'fechaInicio'
    };
    if (column && map[column]) {
      return map[column];
    }
    return 'id';
  }

  onSort(column: 'title' | 'eventName' | 'type' | 'location' | 'period' | 'date'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  viewItem(item: ParticipacionEventoCientificoDTO): void {
    if (item.id) {
      this.router.navigate(['/participation-scientific-events', item.id]);
    }
  }

  editItem(item: ParticipacionEventoCientificoDTO): void {
    if (item.id) {
      this.router.navigate(['/participation-scientific-events', item.id, 'edit']);
    }
  }

  getPdfUrl(item: ParticipacionEventoCientificoDTO): string | null {
    return this.utilsService.getPdfUrl(item.linkPDF);
  }

  downloadPdf(ev: Event, item: ParticipacionEventoCientificoDTO): void {
    ev.stopPropagation();
    const pdfUrl = this.getPdfUrl(item);
    if (pdfUrl) {
      window.open(pdfUrl, '_blank');
    }
  }

  deleteItem(item: ParticipacionEventoCientificoDTO): void {
    if (!item.id) return;
    const title = this.getItemTitle(item);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.pseService.deleteParticipationScientificEvent(item.id!).subscribe({
            next: () => {
              this.messageService.success('Participation Deleted', `${title} has been successfully removed.`);
              this.loadParticipations();
            },
            error: (error) => {
              console.error('Error deleting participation in scientific events:', error);
              this.messageService.error('Error deleting participation in scientific events. Please try again.');
            }
          });
        }
      }
    );
  }

  getItemTitle(item: ParticipacionEventoCientificoDTO): string {
    return item.descripcion || 'Untitled Participation';
  }

  getEventName(item: ParticipacionEventoCientificoDTO): string {
    return item.eventName || 'Event not specified';
  }

  getParticipationType(item: ParticipacionEventoCientificoDTO): string {
    return item.tipoParticipacionEvento?.idDescripcion || 'Type not specified';
  }

  getLocation(item: ParticipacionEventoCientificoDTO): string {
    const parts = [];
    if (item.ciudad) parts.push(item.ciudad);
    if (item.pais?.idDescripcion) parts.push(item.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];

    if (this.basalOnly) {
      filtered = filtered.filter(item => {
        const basal = item.basal;
        return basal === 'S' || basal === '1';
      });
    }

    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareItems(a, b));
    }

    this.filteredParticipations = filtered;
    this.finalFilteredCount = filtered.length;
  }

  private compareItems(a: ParticipacionEventoCientificoDTO, b: ParticipacionEventoCientificoDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'title':
        valueA = (this.getItemTitle(a) || '').toLowerCase();
        valueB = (this.getItemTitle(b) || '').toLowerCase();
        break;
      case 'eventName':
        valueA = (this.getEventName(a) || '').toLowerCase();
        valueB = (this.getEventName(b) || '').toLowerCase();
        break;
      case 'type':
        valueA = (this.getParticipationType(a) || '').toLowerCase();
        valueB = (this.getParticipationType(b) || '').toLowerCase();
        break;
      case 'location':
        valueA = (this.getLocation(a) || '').toLowerCase();
        valueB = (this.getLocation(b) || '').toLowerCase();
        break;
      case 'period':
        valueA = a.progressReport ?? 0;
        valueB = b.progressReport ?? 0;
        break;
      case 'date':
        valueA = a.fechaInicio || '';
        valueB = b.fechaInicio || '';
        break;
      default:
        return 0;
    }

    if (valueA == null && valueB == null) return 0;
    if (valueA == null) return this.sortDirection === 'asc' ? -1 : 1;
    if (valueB == null) return this.sortDirection === 'asc' ? 1 : -1;

    const compareResult = typeof valueA === 'number' && typeof valueB === 'number'
      ? valueA - valueB
      : String(valueA).localeCompare(String(valueB));

    return this.sortDirection === 'asc' ? compareResult : -compareResult;
  }
}
