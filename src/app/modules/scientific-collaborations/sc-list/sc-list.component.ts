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
import { ScientificCollaborationsService } from '../../../core/services/scientific-collaborations.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { ColaboracionDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-sc-list',
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
  templateUrl: './sc-list.component.html',
  styleUrls: ['./sc-list.component.scss']
})
export class SCListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredCollaborations: ColaboracionDTO[] = [];
  collaborations: ColaboracionDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number = 0;
  private searchResults: ColaboracionDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'description' | 'type' | 'institution' | 'origin' | 'destination' | 'period' | 'date' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private scientificCollaborationsService: ScientificCollaborationsService,
    private utilsService: UtilsService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('scientific-collaborations');
    this.basalOnly = state.basalOnly;
    
    this.loadCollaborations();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadCollaborations(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todas las colaboraciones
    this.scientificCollaborationsService.getScientificCollaborations({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading scientific collaborations:', error);
        this.messageService.error('Error loading scientific collaborations. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.collaborations = response.content || [];
      this.searchResults = [...this.collaborations];
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

  onSearchResults(results: ColaboracionDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('scientific-collaborations', { searchTerm: term });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('scientific-collaborations', { basalOnly });
    this.applyFilters();
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(collab => {
        const basal = collab.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareCollaborations(a, b));
    }

    this.filteredCollaborations = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  getCollaborationTitle(collaboration: ColaboracionDTO): string {
    return collaboration.descripcion || 'Untitled Scientific Collaboration';
  }

  getCollaborationType(collaboration: ColaboracionDTO): string {
    return collaboration.tipoColaboracion?.idDescripcion || 'N/A';
  }

  getOrigin(collaboration: ColaboracionDTO): string {
    const parts: string[] = [];
    if (collaboration.ciudadOrigen) parts.push(collaboration.ciudadOrigen);
    if (collaboration.paisOrigen?.idDescripcion) parts.push(collaboration.paisOrigen.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  getDestination(collaboration: ColaboracionDTO): string {
    const parts: string[] = [];
    if (collaboration.ciudadDestino) parts.push(collaboration.ciudadDestino);
    if (collaboration.codigoPaisDestino) parts.push(collaboration.codigoPaisDestino);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  getProgressReport(collaboration: ColaboracionDTO): string {
    if (collaboration.progressReport != null && collaboration.progressReport !== undefined) {
      return `Period ${collaboration.progressReport}`;
    }
    return 'N/A';
  }

  getPeriod(collaboration: ColaboracionDTO): string {
    if (!collaboration.fechaInicio && !collaboration.fechaTermino) {
      return 'N/A';
    }
    
    const startDate = collaboration.fechaInicio 
      ? new Date(collaboration.fechaInicio).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
      : null;
    const endDate = collaboration.fechaTermino 
      ? new Date(collaboration.fechaTermino).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
      : null;
    
    if (startDate && endDate) {
      return `${startDate} - ${endDate}`;
    } else if (startDate) {
      return startDate;
    } else if (endDate) {
      return endDate;
    }
    
    return 'N/A';
  }

  onSort(column: 'description' | 'type' | 'institution' | 'origin' | 'destination' | 'period' | 'date'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareCollaborations(a: ColaboracionDTO, b: ColaboracionDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'description':
        valueA = (this.getCollaborationTitle(a) || '').toLowerCase();
        valueB = (this.getCollaborationTitle(b) || '').toLowerCase();
        break;
      case 'type':
        valueA = (this.getCollaborationType(a) || '').toLowerCase();
        valueB = (this.getCollaborationType(b) || '').toLowerCase();
        break;
      case 'institution':
        valueA = (a.institucion?.descripcion || a.institucion?.idDescripcion || 'N/A').toLowerCase();
        valueB = (b.institucion?.descripcion || b.institucion?.idDescripcion || 'N/A').toLowerCase();
        break;
      case 'origin':
        valueA = (this.getOrigin(a) || '').toLowerCase();
        valueB = (this.getOrigin(b) || '').toLowerCase();
        break;
      case 'destination':
        valueA = (this.getDestination(a) || '').toLowerCase();
        valueB = (this.getDestination(b) || '').toLowerCase();
        break;
      case 'period':
        valueA = a.progressReport || '';
        valueB = b.progressReport || '';
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

    let compareResult: number;
    if (typeof valueA === 'number' && typeof valueB === 'number') {
      compareResult = valueA - valueB;
    } else {
      compareResult = String(valueA).localeCompare(String(valueB));
    }

    return this.sortDirection === 'asc' ? compareResult : -compareResult;
  }

  viewCollaboration(collaboration: ColaboracionDTO): void {
    if (collaboration.id) {
      this.router.navigate(['/scientific-collaborations', collaboration.id]);
    }
  }

  editCollaboration(collaboration: ColaboracionDTO): void {
    if (collaboration.id) {
      this.router.navigate(['/scientific-collaborations', collaboration.id, 'edit']);
    }
  }

  deleteCollaboration(collaboration: ColaboracionDTO): void {
    if (!collaboration.id) return;
    
    const title = this.getCollaborationTitle(collaboration);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.scientificCollaborationsService.deleteScientificCollaboration(collaboration.id!).subscribe({
            next: () => {
              this.messageService.success('Scientific Collaboration Deleted', `${title} has been successfully removed.`);
              this.loadCollaborations();
            },
            error: (error) => {
              console.error('Error deleting scientific collaboration:', error);
              this.messageService.error('Error deleting scientific collaboration. Please try again.');
            }
          });
        }
      }
    );
  }

  getPdfUrl(collaboration: ColaboracionDTO): string | null {
    return this.utilsService.getPdfUrl(collaboration.linkPDF);
  }

  downloadPdf(event: Event, collaboration: ColaboracionDTO): void {
    event.stopPropagation();
    const pdfUrl = this.getPdfUrl(collaboration);
    if (pdfUrl) {
      window.open(pdfUrl, '_blank');
    }
  }

  getPdfFileName(collaboration: ColaboracionDTO): string {
    if (!collaboration.linkPDF) {
      return '';
    }
    if (collaboration.linkPDF.startsWith('PDF:')) {
      const path = collaboration.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = collaboration.linkPDF.split('/');
    return parts[parts.length - 1];
  }
}


