import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MessageService } from '../../../core/services/message.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewMode } from '../../../shared/components/view-mode-selector/view-mode-selector.component';
import { ViewModeService } from '../../../core/services/view-mode.service';
import { RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-researcher-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    ListControlsComponent
  ],
  templateUrl: './researcher-list.component.html',
  styleUrls: ['./researcher-list.component.scss']
})
export class ResearcherListComponent implements OnInit {
  viewMode: ViewMode = 'card';
  filteredResearchers: RRHHDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;

  constructor(
    private messageService: MessageService,
    private researcherService: ResearcherService,
    private viewModeService: ViewModeService
  ) {}

  ngOnInit(): void {
    this.loadResearchers();
    
    // Suscribirse a cambios en el modo de vista
    this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }
  
  loadResearchers(): void {
    this.loading = true;
    // Solicitar un tamaño de página grande para obtener todos los investigadores
    this.researcherService.getResearchers({ page: 0, size: 10000 }).subscribe({
      next: (response) => {
        this.filteredResearchers = response.content || [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading researchers:', error);
        this.messageService.error('Error loading researchers');
        this.loading = false;
      }
    });
  }

  getGenderLabel(codigoGenero: string | undefined): string {
    if (!codigoGenero) return 'N/A';
    return codigoGenero === 'M' ? 'Masculino' : codigoGenero === 'F' ? 'Femenino' : codigoGenero;
  }

  deleteResearcher(researcher: RRHHDTO): void {
    if (researcher.id) {
      this.messageService.confirm(
        `Are you sure you want to delete ${researcher.fullname}?`,
        (accepted: boolean) => {
          if (accepted) {
            this.researcherService.deleteResearcher(researcher.id!).subscribe({
              next: () => {
                this.messageService.success('Researcher deleted successfully');
                this.loadResearchers();
              },
              error: (error) => {
                console.error('Error deleting researcher:', error);
                this.messageService.error('Error deleting researcher');
              }
            });
          }
        },
        'Delete Researcher'
      );
    }
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
  }

  onSearchResults(results: RRHHDTO[]): void {
    this.filteredResearchers = results;
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
  }
}