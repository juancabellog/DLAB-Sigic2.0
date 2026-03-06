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

import { MessageService } from '../../../core/services/message.service';
import { ViewModeService, ViewMode } from '../../../core/services/view-mode.service';
import { TechnologyTransferService } from '../../../core/services/technology-transfer.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { TransferenciaTecnologicaDTO } from '../../../core/models/backend-dtos';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';

@Component({
  selector: 'app-tt-list',
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
  templateUrl: './tt-list.component.html',
  styleUrls: ['./tt-list.component.scss']
})
export class TTListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredTransfers: TransferenciaTecnologicaDTO[] = [];
  transfers: TransferenciaTecnologicaDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  private searchResults: TransferenciaTecnologicaDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'description' | 'type' | 'beneficiary' | 'location' | 'year' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private technologyTransferService: TechnologyTransferService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('technology-transfer');
    this.basalOnly = state.basalOnly;
    
    this.loadTransfers();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadTransfers(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todas las transferencias
    this.technologyTransferService.getTechnologyTransfers({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading technology transfers:', error);
        this.messageService.error('Error loading technology transfers. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.transfers = response.content || [];
      this.searchResults = [...this.transfers];
      this.applyFilters();
    });
  }

  ngOnDestroy(): void {
    if (this.viewModeSubscription) {
      this.viewModeSubscription.unsubscribe();
    }
  }

  getTransferTitle(transfer: TransferenciaTecnologicaDTO): string {
    return transfer.descripcion || 'Untitled Transfer';
  }

  getTypeColor(type: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!type) return 'primary';
    switch (type.toUpperCase()) {
      case 'LICENSE': return 'primary';
      case 'ASSIGNMENT': return 'accent';
      case 'COLLABORATION': return 'warn';
      default: return 'primary';
    }
  }

  getLocation(transfer: TransferenciaTecnologicaDTO): string {
    const parts = [];
    if (transfer.ciudad) parts.push(transfer.ciudad);
    if (transfer.region) parts.push(transfer.region);
    if (transfer.pais?.idDescripcion) parts.push(transfer.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  viewTransfer(transfer: TransferenciaTecnologicaDTO): void {
    if (transfer.id) {
      this.router.navigate(['/technology-transfer', transfer.id]);
    }
  }

  editTransfer(transfer: TransferenciaTecnologicaDTO): void {
    if (transfer.id) {
      this.router.navigate(['/technology-transfer', transfer.id, 'edit']);
    }
  }

  deleteTransfer(transfer: TransferenciaTecnologicaDTO): void {
    if (!transfer.id) return;
    
    const title = this.getTransferTitle(transfer);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.technologyTransferService.deleteTechnologyTransfer(transfer.id!).subscribe({
            next: () => {
              this.messageService.success('Technology Transfer Deleted', `${title} has been successfully removed.`);
              this.loadTransfers();
            },
            error: (error) => {
              console.error('Error deleting technology transfer:', error);
              this.messageService.error('Error deleting technology transfer. Please try again.');
            }
          });
        }
      },
      'Delete Technology Transfer'
    );
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    // Guardar el modo en el servicio para persistencia
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: TransferenciaTecnologicaDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
    this.listStateService.saveState('technology-transfer', { searchTerm });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('technology-transfer', { basalOnly });
    this.applyFilters();
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(transfer => {
        const basal = transfer.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareTransfers(a, b));
    }

    this.filteredTransfers = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  onSort(column: 'description' | 'type' | 'beneficiary' | 'location' | 'year'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareTransfers(a: TransferenciaTecnologicaDTO, b: TransferenciaTecnologicaDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'description':
        valueA = (this.getTransferTitle(a) || '').toLowerCase();
        valueB = (this.getTransferTitle(b) || '').toLowerCase();
        break;
      case 'type':
        valueA = (a.tipoTransferencia?.idDescripcion || 'N/A').toLowerCase();
        valueB = (b.tipoTransferencia?.idDescripcion || 'N/A').toLowerCase();
        break;
      case 'beneficiary':
        valueA = (a.institucion?.descripcion || a.institucion?.idDescripcion || 'N/A').toLowerCase();
        valueB = (b.institucion?.descripcion || b.institucion?.idDescripcion || 'N/A').toLowerCase();
        break;
      case 'location':
        valueA = (this.getLocation(a) || '').toLowerCase();
        valueB = (this.getLocation(b) || '').toLowerCase();
        break;
      case 'year':
        valueA = a.agno ?? 0;
        valueB = b.agno ?? 0;
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
