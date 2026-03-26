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
import { PostdoctoralFellowsService } from '../../../core/services/postdoctoral-fellows.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { BecariosPostdoctoralesDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-pf-list',
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
  templateUrl: './pf-list.component.html',
  styleUrls: ['./pf-list.component.scss']
})
export class PFListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredFellows: BecariosPostdoctoralesDTO[] = [];
  fellows: BecariosPostdoctoralesDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  exportLoading: boolean = false;
  private searchResults: BecariosPostdoctoralesDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'topic' | 'institution' | 'sector' | 'name' | 'period' | 'date' | 'endDate' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private postdoctoralFellowsService: PostdoctoralFellowsService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('postdoctoral-fellows');
    this.basalOnly = state.basalOnly;
    
    this.loadFellows();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadFellows(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todos los becarios
    this.postdoctoralFellowsService.getPostdoctoralFellows({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading postdoctoral fellows:', error);
        this.messageService.error('Error loading postdoctoral fellows. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.fellows = response.content || [];
      this.searchResults = [...this.fellows];
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

  onSearchResults(results: BecariosPostdoctoralesDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('postdoctoral-fellows', { searchTerm: term });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('postdoctoral-fellows', { basalOnly });
    this.applyFilters();
  }

  onExportRequested(): void {
    if (this.filteredFellows.length === 0) {
      this.messageService.info('There are no results to export.');
      return;
    }
    this.exportLoading = true;
    this.postdoctoralFellowsService.exportPostdoctoralFellowsToExcel({
      sort: this.sortColumn || 'id',
      direction: this.sortDirection === 'asc' ? 'ASC' : 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error exporting postdoctoral fellows to Excel:', error);
        this.messageService.error('Error exporting postdoctoral fellows. Please try again later.');
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
      a.download = 'postdoctoral-fellows.xlsx';
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
      filtered = filtered.filter(fellow => {
        const basal = fellow.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareFellows(a, b));
    }

    this.filteredFellows = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  getFellowTitle(fellow: BecariosPostdoctoralesDTO): string {
    return fellow.descripcion || 'Untitled Postdoctoral Fellow';
  }

  getInstitution(fellow: BecariosPostdoctoralesDTO): string {
    return fellow.institucion?.descripcion || fellow.institucion?.idDescripcion || 'N/A';
  }

  getSector(fellow: BecariosPostdoctoralesDTO): string {
    return fellow.tipoSector?.idDescripcion || 'N/A';
  }

  onSort(column: 'topic' | 'institution' | 'sector' | 'name' | 'period' | 'date' | 'endDate'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareFellows(a: BecariosPostdoctoralesDTO, b: BecariosPostdoctoralesDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'topic':
        valueA = (this.getFellowTitle(a) || '').toLowerCase();
        valueB = (this.getFellowTitle(b) || '').toLowerCase();
        break;
      case 'institution':
        valueA = (this.getInstitution(a) || '').toLowerCase();
        valueB = (this.getInstitution(b) || '').toLowerCase();
        break;
      case 'sector':
        valueA = (this.getSector(a) || '').toLowerCase();
        valueB = (this.getSector(b) || '').toLowerCase();
        break;
      case 'name':
        valueA = (a.postdoctoralFellowName || 'N/A').toLowerCase();
        valueB = (b.postdoctoralFellowName || 'N/A').toLowerCase();
        break;
      case 'period':
        valueA = a.progressReport || '';
        valueB = b.progressReport || '';
        break;
      case 'date':
        valueA = a.fechaInicio || '';
        valueB = b.fechaInicio || '';
        break;
      case 'endDate':
        valueA = a.fechaTermino || '';
        valueB = b.fechaTermino || '';
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

  viewFellow(fellow: BecariosPostdoctoralesDTO): void {
    if (fellow.id) {
      this.router.navigate(['/postdoctoral-fellows', fellow.id]);
    }
  }

  editFellow(fellow: BecariosPostdoctoralesDTO): void {
    if (fellow.id) {
      this.router.navigate(['/postdoctoral-fellows', fellow.id, 'edit']);
    }
  }

  deleteFellow(fellow: BecariosPostdoctoralesDTO): void {
    if (!fellow.id) return;
    
    const title = this.getFellowTitle(fellow);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.postdoctoralFellowsService.deletePostdoctoralFellow(fellow.id!).subscribe({
            next: () => {
              this.messageService.success('Postdoctoral Fellow Deleted', `${title} has been successfully removed.`);
              this.loadFellows();
            },
            error: (error) => {
              console.error('Error deleting postdoctoral fellow:', error);
              this.messageService.error('Error deleting postdoctoral fellow. Please try again.');
            }
          });
        }
      }
    );
  }
}


