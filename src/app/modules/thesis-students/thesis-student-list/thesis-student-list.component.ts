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
import { ThesisService } from '../../../core/services/thesis.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { TesisDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-thesis-student-list',
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
  templateUrl: './thesis-student-list.component.html',
  styleUrls: ['./thesis-student-list.component.scss']
})
export class ThesisStudentListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'card';
  filteredThesis: TesisDTO[] = [];
  thesis: TesisDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;
  exportLoading: boolean = false;
  basalOnly: boolean = false;
  finalFilteredCount: number | null = null;
  private searchResults: TesisDTO[] = [];
  private viewModeSubscription?: Subscription;

  // Sorting state for list view
  sortColumn: 'title' | 'degree' | 'institution' | 'student' | 'status' | 'period' | 'date' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private thesisService: ThesisService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    // Obtener el modo de vista actual inmediatamente
    this.viewMode = this.viewModeService.getCurrentViewMode();
    
    // Restaurar estado de filtros
    const state = this.listStateService.getState('thesis-students');
    this.basalOnly = state.basalOnly;
    
    this.loadThesis();
    
    // Suscribirse al modo de vista para cambios futuros
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadThesis(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todas las tesis
    this.thesisService.getThesis({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading thesis:', error);
        this.messageService.error('Error loading thesis. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(response => {
      this.thesis = response.content || [];
      this.searchResults = [...this.thesis];
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

  onSearchResults(results: TesisDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('thesis-students', { searchTerm: term });
  }

  onBasalFilterChange(basalOnly: boolean): void {
    this.basalOnly = basalOnly;
    this.listStateService.saveState('thesis-students', { basalOnly });
    this.applyFilters();
  }

  onExportRequested(): void {
    if (this.filteredThesis.length === 0) {
      this.messageService.info('There are no results to export.');
      return;
    }
    this.exportLoading = true;
    // Exportar siempre el conjunto actual de resultados (con filtros y búsqueda aplicados)
    this.thesisService.exportThesisToExcel({
      page: 0,
      size: 10000,
      sort: this.sortColumn || 'id',
      direction: this.sortDirection === 'asc' ? 'ASC' : 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error exporting thesis to Excel:', error);
        this.messageService.error('Error exporting thesis. Please try again later.');
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
      a.download = 'thesis-students.xlsx';
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
      filtered = filtered.filter(thesis => {
        const basal = thesis.basal;
        return basal === 'S' || basal === '1';
      });
    }
    
    // Aplicar orden si hay columna seleccionada
    if (this.sortColumn) {
      filtered.sort((a, b) => this.compareThesis(a, b));
    }

    this.filteredThesis = filtered;
    // Actualizar el contador final de resultados filtrados - SIEMPRE mostrar
    this.finalFilteredCount = filtered.length;
  }

  onSort(column: 'title' | 'degree' | 'institution' | 'student' | 'status' | 'period' | 'date'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  private compareThesis(a: TesisDTO, b: TesisDTO): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'title':
        valueA = (this.getThesisTitle(a) || '').toLowerCase();
        valueB = (this.getThesisTitle(b) || '').toLowerCase();
        break;
      case 'degree':
        valueA = (a.gradoAcademico?.descripcion || a.gradoAcademico?.idDescripcion || 'N/A').toLowerCase();
        valueB = (b.gradoAcademico?.descripcion || b.gradoAcademico?.idDescripcion || 'N/A').toLowerCase();
        break;
      case 'institution':
        valueA = (a.institucionOG?.descripcion || a.institucionOG?.idDescripcion || 'N/A').toLowerCase();
        valueB = (b.institucionOG?.descripcion || b.institucionOG?.idDescripcion || 'N/A').toLowerCase();
        break;
      case 'student':
        valueA = (a.estudiante || 'N/A').toLowerCase();
        valueB = (b.estudiante || 'N/A').toLowerCase();
        break;
      case 'status':
        valueA = (this.getThesisStatus(a) || 'N/A').toLowerCase();
        valueB = (this.getThesisStatus(b) || 'N/A').toLowerCase();
        break;
      case 'period':
        // progressReport es numérico; usar 0 para null
        valueA = a.progressReport ?? 0;
        valueB = b.progressReport ?? 0;
        break;
      case 'date':
        // fechaInicioPrograma es string ISO (YYYY-MM-DD), se puede comparar como string
        valueA = a.fechaInicioPrograma || '';
        valueB = b.fechaInicioPrograma || '';
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

  getThesisTitle(thesis: TesisDTO): string {
    return thesis.nombreCompletoTitulo || thesis.descripcion || 'Untitled Thesis';
  }

  getThesisStatus(thesis: TesisDTO): string {
    return thesis.estadoTesis?.descripcion || thesis.estadoProducto?.descripcion || 'N/A';
  }

  getProgressReport(thesis: TesisDTO): string {
    if (thesis.progressReport != null && thesis.progressReport !== undefined && thesis.progressReport !== '') {
      return thesis.progressReport;
    }
    return 'N/A';
  }

  getStatusColor(status: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!status) return 'primary';
    switch (status.toUpperCase()) {
      case 'IN_PROGRESS': return 'primary';
      case 'DEFENDED': return 'accent';
      case 'GRADUATED': return 'primary';
      case 'SUSPENDED': return 'warn';
      default: return 'primary';
    }
  }

  viewThesis(thesis: TesisDTO): void {
    if (thesis.id) {
      this.router.navigate(['/thesis-students', thesis.id]);
    }
  }

  editThesis(thesis: TesisDTO): void {
    if (thesis.id) {
      this.router.navigate(['/thesis-students', thesis.id, 'edit']);
    }
  }

  deleteThesis(thesis: TesisDTO): void {
    if (!thesis.id) return;
    
    const title = this.getThesisTitle(thesis);
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.thesisService.deleteThesis(thesis.id!).subscribe({
            next: () => {
              this.messageService.success('Thesis Deleted', `${title} has been successfully removed.`);
              this.loadThesis();
            },
            error: (error) => {
              console.error('Error deleting thesis:', error);
              this.messageService.error('Error deleting thesis. Please try again.');
            }
          });
        }
      }
    );
  }

  copyThesis(thesis: TesisDTO): void {
    if (thesis.id) {
      this.router.navigate(['/thesis-students/new'], {
        queryParams: { copyFrom: thesis.id }
      });
    }
  }
}
