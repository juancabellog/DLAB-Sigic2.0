import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { RouterModule } from '@angular/router';

import { MessageService } from '../../../core/services/message.service';
import { ProjectService } from '../../../core/services/project.service';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewMode } from '../../../shared/components/view-mode-selector/view-mode-selector.component';
import { ViewModeService } from '../../../core/services/view-mode.service';
import { ProyectoDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    ListControlsComponent
  ],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss']
})
export class ProjectListComponent implements OnInit {
  viewMode: ViewMode = 'card';
  filteredProjects: ProyectoDTO[] = [];
  isSearching: boolean = false;
  loading: boolean = false;

  constructor(
    private messageService: MessageService,
    private projectService: ProjectService,
    private viewModeService: ViewModeService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
    
    // Subscribe to view mode changes
    this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getProjects().subscribe({
      next: (response) => {
        this.filteredProjects = response.content || [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        this.messageService.error('Error loading projects');
        this.loading = false;
      }
    });
  }


  getStatusColor(project: ProyectoDTO): 'primary' | 'accent' | 'warn' {
    // Determinar estado basado en fechas
    if (!project.fechaTermino) return 'primary'; // Sin fecha de término = activo
    const endDate = new Date(project.fechaTermino);
    const today = new Date();
    return endDate > today ? 'primary' : 'accent';
  }

  getProjectTypeColor(tipoFinanciamiento: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!tipoFinanciamiento) return 'primary';
    const types = tipoFinanciamiento.split(',').map(t => t.trim().toLowerCase());
    if (types.includes('national')) return 'primary';
    if (types.includes('government')) return 'accent';
    if (types.includes('commercial')) return 'warn';
    return 'primary';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('es-CL');
  }

  getFinancingTypes(tipoFinanciamiento: string | undefined): string[] {
    if (!tipoFinanciamiento) return [];
    return tipoFinanciamiento.split(',').map(t => t.trim());
  }

  getCollaborationTypes(realizaCon: string | undefined): string[] {
    if (!realizaCon) return [];
    return realizaCon.split(',').map(t => t.trim());
  }

  deleteProject(project: ProyectoDTO): void {
    this.messageService.confirm(
      `Are you sure you want to delete "${project.descripcion}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.projectService.deleteProject(project.codigo).subscribe({
            next: () => {
              this.messageService.success('Project deleted successfully');
              this.loadProjects();
            },
            error: (error) => {
              console.error('Error deleting project:', error);
              this.messageService.error('Error deleting project');
            }
          });
        }
      },
      'Delete Project'
    );
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
  }

  onSearchResults(results: any[]): void {
    this.filteredProjects = results;
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
  }
}