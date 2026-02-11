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
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  private searchResults: OrganizacionEventosCientificosDTO[] = [];
  private viewModeSubscription?: Subscription;

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private scientificEventsService: ScientificEventsService,
    private listStateService: ListStateService
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
        console.error('Error loading scientific events:', error);
        this.messageService.error('Error loading scientific events. Please try again later.');
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
              console.error('Error deleting scientific event:', error);
              this.messageService.error('Error deleting scientific event. Please try again.');
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

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(event => {
        const basal = event.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    this.filteredEvents = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }
}

