import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
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
import { PublicationService } from '../../../core/services/publication.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { PublicacionDTO } from '../../../core/models/backend-dtos';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';

@Component({
  selector: 'app-publication-list',
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
  templateUrl: './publication-list.component.html',
  styleUrls: ['./publication-list.component.scss']
})
export class PublicationListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredPublications: PublicacionDTO[] = [];
  publications: PublicacionDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  exportLoading: boolean = false;
  basalOnly: boolean = false;
  pendingOnly: boolean = false;
  finalFilteredCount: number = 0;
  private searchResults: PublicacionDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'title' | 'journal' | 'year' | 'period' | 'status' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private publicationService: PublicationService,
    private utilsService: UtilsService,
    private listStateService: ListStateService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('publications');
    this.basalOnly = state.basalOnly;
    this.pendingOnly = state.pendingOnly;
    
    // Cargar publicaciones del backend
    this.loadPublications();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadPublications(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todas las publicaciones
    this.publicationService.getPublications({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading publications:', error);
        this.messageService.error('Error loading publications. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.publications = response.content || [];
      this.searchResults = [...this.publications];
      // Aplicar filtros después de cargar las publicaciones
      // Los filtros basalOnly y pendingOnly ya están restaurados desde el estado
      // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
      setTimeout(() => {
        this.applyFilters();
        this.cdr.detectChanges();
      }, 0);
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

  getPublicationTitle(publication: PublicacionDTO): string {
    return publication.descripcion || 'Untitled Publication';
  }

  getJournalName(publication: PublicacionDTO): string {
    return publication.journal?.descripcion || publication.journal?.abbreviation || 'Unknown Journal';
  }

  viewPublication(publication: PublicacionDTO): void {
    if (publication.id) {
      this.router.navigate(['/publications', publication.id]);
    }
  }

  editPublication(publication: PublicacionDTO): void {
    if (publication.id) {
      this.router.navigate(['/publications', publication.id, 'edit']);
    }
  }

  deletePublication(publication: PublicacionDTO): void {
    if (!publication.id) return;
    
    const title = this.getPublicationTitle(publication);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.publicationService.deletePublication(publication.id!).subscribe({
            next: () => {
              this.messageService.success('Publication Deleted', `${title} has been successfully removed.`);
              this.loadPublications(); // Recargar la lista
            },
            error: (error) => {
              console.error('Error deleting publication:', error);
              this.messageService.error('Error deleting publication. Please try again.');
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

  onSearchResults(results: PublicacionDTO[]): void {
    this.searchResults = results;
    // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.applyFilters();
    }, 0);
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
    this.listStateService.saveState('publications', { searchTerm });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('publications', { basalOnly });
    // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.applyFilters();
    }, 0);
  }

  onPendingFilterChange(pendingOnly: boolean): void {
    this.pendingOnly = pendingOnly;
    this.listStateService.saveState('publications', { pendingOnly });
    // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.applyFilters();
    }, 0);
  }

  onExportRequested(): void {
    if (this.filteredPublications.length === 0) {
      this.messageService.info('There are no results to export.');
      return;
    }
    this.exportLoading = true;
    this.publicationService.exportPublicationsToExcel({
      sort: this.sortColumn || 'id',
      direction: this.sortDirection === 'asc' ? 'ASC' : 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error exporting publications to Excel:', error);
        this.messageService.error('Error exporting publications. Please try again later.');
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
      a.download = 'publications.xlsx';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      this.messageService.success('Export started. Your download should begin shortly.');
    });
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    // Aplicar filtro Basal Only si está activo
    if (this.basalOnly) {
      filtered = filtered.filter(pub => {
        const basal = pub.basal;
        // El backend normaliza: '1' -> 'S', '0' -> 'N'
        // También puede venir directamente como 'S' o '1'
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar filtro Pending Only si está activo
    if (this.pendingOnly) {
      filtered = filtered.filter(pub => {
        const estadoId = pub.estadoProducto?.id;
        // idEstadoProducto = 4 es "Pending"
        return estadoId === 4;
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.comparePublications(a, b));
    }

    this.filteredPublications = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  onSort(column: 'title' | 'journal' | 'year' | 'period' | 'status'): void {
    if (this.sortColumn === column) {
      // Toggle direction
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private comparePublications(a: PublicacionDTO, b: PublicacionDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'title':
        valueA = (this.getPublicationTitle(a) || '').toLowerCase();
        valueB = (this.getPublicationTitle(b) || '').toLowerCase();
        break;
      case 'journal':
        valueA = (this.getJournalName(a) || '').toLowerCase();
        valueB = (this.getJournalName(b) || '').toLowerCase();
        break;
      case 'year':
        valueA = a.yearPublished ?? 0;
        valueB = b.yearPublished ?? 0;
        break;
      case 'period':
        valueA = a.progressReport ?? 0;
        valueB = b.progressReport ?? 0;
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

  /**
   * Verifica si una publicación está pendiente (idEstadoProducto = 4)
   */
  isPending(publication: PublicacionDTO): boolean {
    return publication.estadoProducto?.id === 4;
  }

  /**
   * Verifica si una publicación está completa (idEstadoProducto = 3)
   */
  isComplete(publication: PublicacionDTO): boolean {
    return publication.estadoProducto?.id === 3;
  }

  getPdfUrl(publication: PublicacionDTO): string | null {
    return this.utilsService.getPdfUrl(publication.linkPDF);
  }

  downloadPdf(event: Event, publication: PublicacionDTO): void {
    event.stopPropagation(); // Evitar que se active el click en la tarjeta/fila
    const pdfUrl = this.getPdfUrl(publication);
    if (pdfUrl) {
      window.open(pdfUrl, '_blank');
    }
  }

  getPdfFileName(publication: PublicacionDTO): string {
    if (!publication.linkPDF) {
      return '';
    }
    if (publication.linkPDF.startsWith('PDF:')) {
      const path = publication.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    const parts = publication.linkPDF.split('/');
    return parts[parts.length - 1];
  }

  /**
   * Formatea el factor de impacto con 4 decimales según formato chileno
   * Separador de miles: punto (.)
   * Separador de decimales: coma (,)
   */
  formatImpactFactor(factor: number | null | undefined): string {
    if (factor == null || factor === undefined) {
      return 'N/A';
    }
    // Formatear con 4 decimales usando formato chileno
    const parts = factor.toFixed(4).split('.');
    const integerPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    const decimalPart = parts[1];
    return `${integerPart},${decimalPart}`;
  }
}
