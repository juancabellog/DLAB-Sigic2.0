import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MessageService } from '../../../core/services/message.service';
import { ProjectService } from '../../../core/services/project.service';
import { ProyectoDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-project-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.scss']
})
export class ProjectViewComponent implements OnInit {
  project: ProyectoDTO | null = null;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    const codigo = this.route.snapshot.paramMap.get('id');
    if (codigo) {
      this.loadProject(codigo);
    }
  }

  loadProject(codigo: string): void {
    this.loading = true;
    this.projectService.getProject(codigo).subscribe({
      next: (project) => {
        this.project = project;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading project:', error);
        this.messageService.error('Project not found');
        this.loading = false;
        this.goBack();
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

  goBack(): void {
    this.router.navigate(['/projects']);
  }

  editProject(): void {
    if (this.project) {
      this.router.navigate(['/projects', this.project.codigo, 'edit']);
    }
  }

  deleteProject(): void {
    if (this.project) {
      this.messageService.confirm(
        `Are you sure you want to delete "${this.project.descripcion}"?`,
        (accepted: boolean) => {
          if (accepted) {
            this.projectService.deleteProject(this.project!.codigo).subscribe({
              next: () => {
                this.messageService.success('Project deleted successfully');
                this.goBack();
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
  }
}
