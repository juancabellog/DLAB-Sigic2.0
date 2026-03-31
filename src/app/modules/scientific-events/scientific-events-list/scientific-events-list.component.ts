import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { ViewModeService, ViewMode } from '../../../core/services/view-mode.service';
import { ScientificEventsService } from '../../../core/services/scientific-events.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { UtilsService } from '../../../core/services/utils.service';
import { OrganizacionEventosCientificosDTO } from '../../../core/models/backend-dtos';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';

@Component({
  selector: 'app-scientific-events-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    ListControlsComponent
  ],
  templateUrl: './scientific-events-list.component.html',
  styleUrls: ['./scientific-events-list.component.scss']
})
export class ScientificEventsListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredEvents: OrganizacionEventosCientificosDTO[] = [];
  events: OrganizacionEventosCientificosDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  exportLoading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  private searchResults: OrganizacionEventosCientificosDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'title' | 'commentTitle' | 'type' | 'location' | 'organizer' | 'period' | 'date' | 'status' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private scientificEventsService: ScientificEventsService,
    private listStateService: ListStateService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('scientific-events');
    this.basalOnly = state.basalOnly;
    
    this.loadEvents();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadEvents(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todos los eventos
    this.scientificEventsService.getScientificEvents({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading organization of scientific events:', error);
        this.messageService.error('Error loading organization of scientific events. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.events = response.content || [];
      this.searchResults = [...this.events];
      this.applyFilters();
    });
  }

  ngOnDestroy(): void {
    if (this.viewModeSubscription) {
      this.viewModeSubscription.unsubscribe();
    }
  }

  getStatusColor(status: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!status) return 'primary';
    switch (status.toUpperCase()) {
      case 'PUBLISHED': return 'primary';
      case 'IN_REVIEW': return 'accent';
      case 'DRAFT': return 'warn';
      default: return 'primary';
    }
  }

  getEventTitle(event: OrganizacionEventosCientificosDTO): string {
    return event.descripcion || 'Untitled Event';
  }

  getEventLocation(event: OrganizacionEventosCientificosDTO): string {
    const parts = [];
    if (event.ciudad) parts.push(event.ciudad);
    if (event.pais?.idDescripcion) parts.push(event.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  getEventType(event: OrganizacionEventosCientificosDTO): string {
    return event.tipoEvento?.descripcion || 'Type not specified';
  }

  getPdfUrl(event: OrganizacionEventosCientificosDTO): string | null {
    return this.utilsService.getPdfUrl(event.linkPDF);
  }

  downloadPdf(ev: Event, event: OrganizacionEventosCientificosDTO): void {
    ev.stopPropagation();
    const pdfUrl = this.getPdfUrl(event);
    if (pdfUrl) {
      window.open(pdfUrl, '_blank');
    }
  }

  viewEvent(event: OrganizacionEventosCientificosDTO): void {
    if (event.id) {
      this.router.navigate(['/scientific-events', event.id]);
    }
  }

  editEvent(event: OrganizacionEventosCientificosDTO): void {
    if (event.id) {
      this.router.navigate(['/scientific-events', event.id, 'edit']);
    }
  }

  copyEvent(event: OrganizacionEventosCientificosDTO): void {
    if (event.id) {
      this.router.navigate(['/scientific-events/new'], {
        queryParams: { copyFrom: event.id }
      });
    }
  }

  deleteEvent(event: OrganizacionEventosCientificosDTO): void {
    if (!event.id) return;
    
    const title = this.getEventTitle(event);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.scientificEventsService.deleteScientificEvent(event.id!).subscribe({
            next: () => {
              this.messageService.success('Scientific Event Deleted', `${title} has been successfully removed.`);
              this.loadEvents();
            },
            error: (error) => {
              console.error('Error deleting organization of scientific events:', error);
              this.messageService.error('Error deleting organization of scientific events. Please try again.');
            }
          });
        }
      }
    );
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    // Guardar el modo en el servicio para persistencia
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: OrganizacionEventosCientificosDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
    this.listStateService.saveState('scientific-events', { searchTerm });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('scientific-events', { basalOnly });
    this.applyFilters();
  }

  onExportRequested(): void {
    if (this.filteredEvents.length === 0) {
      this.messageService.info('There are no results to export.');
      return;
    }
    this.exportLoading = true;
    this.scientificEventsService.exportScientificEventsToExcel({
      sort: this.sortColumn || 'id',
      direction: this.sortDirection === 'asc' ? 'ASC' : 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error exporting organization of scientific events to Excel:', error);
        this.messageService.error('Error exporting organization of scientific events. Please try again later.');
        return of(null as any);
      }),
      finalize(() => {
        this.exportLoading = false;
      })
    ).subscribe(blob => {
      if (!blob) {
        return;
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'scientific-events.xlsx';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      this.messageService.success('Export started. Your download should begin shortly.');
    });
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(event => {
        const basal = event.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareEvents(a, b));
    }

    this.filteredEvents = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  onSort(column: 'title' | 'commentTitle' | 'type' | 'location' | 'organizer' | 'period' | 'date' | 'status'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareEvents(a: OrganizacionEventosCientificosDTO, b: OrganizacionEventosCientificosDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'title':
        valueA = (this.getEventTitle(a) || '').toLowerCase();
        valueB = (this.getEventTitle(b) || '').toLowerCase();
        break;
      case 'commentTitle':
        valueA = (a.comentario || '').toLowerCase();
        valueB = (b.comentario || '').toLowerCase();
        break;
      case 'type':
        valueA = (this.getEventType(a) || '').toLowerCase();
        valueB = (this.getEventType(b) || '').toLowerCase();
        break;
      case 'location':
        valueA = (this.getEventLocation(a) || '').toLowerCase();
        valueB = (this.getEventLocation(b) || '').toLowerCase();
        break;
      case 'organizer':
        valueA = (a.organizer || 'N/A').toLowerCase();
        valueB = (b.organizer || 'N/A').toLowerCase();
        break;
      case 'period':
        valueA = a.progressReport ?? 0;
        valueB = b.progressReport ?? 0;
        break;
      case 'date':
        valueA = a.fechaInicio || '';
        valueB = b.fechaInicio || '';
        break;
      case 'status':
        valueA = (a.estadoProducto?.nombre || a.estadoProducto?.codigoDescripcion || a.estadoProducto?.codigo || 'N/A').toLowerCase();
        valueB = (b.estadoProducto?.nombre || b.estadoProducto?.codigoDescripcion || b.estadoProducto?.codigo || 'N/A').toLowerCase();
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
}

