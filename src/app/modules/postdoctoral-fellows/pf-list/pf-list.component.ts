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
  private searchResults: BecariosPostdoctoralesDTO[] = [];
  private viewModeSubscription?: Subscription;

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

  private applyFilters(): void {
    let filtered = [...this.searchResults];
    
    if (this.basalOnly) {
      filtered = filtered.filter(fellow => {
        const basal = fellow.basal;
        return basal === 'S' || basal === '1';
      });
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


