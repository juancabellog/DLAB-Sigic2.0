import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { ProjectService } from '../../../core/services/project.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { ProjectProductDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-project-view',
  standalone: true,
  imports: [
    CommonModule,
    LocalDatePipe,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.scss']
})
export class ProjectViewComponent implements OnInit {
  project: ProjectProductDTO | null = null;
  loading = false;
  participants: Array<{ rrhhId?: number; fullName?: string }> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private messageService: MessageService,
    private researcherService: ResearcherService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/projects']);
      return;
    }
    this.loadProject(id);
  }

  loadProject(id: number): void {
    this.loading = true;
    this.projectService.getProject(id).pipe(
      catchError(() => {
        this.messageService.error('Project not found.');
        this.router.navigate(['/projects']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      this.project = data;
      if (data?.participantes?.length) {
        this.participants = data.participantes.map(p => ({ rrhhId: p.rrhhId, fullName: '' }));
        this.participants.forEach(p => {
          if (p.rrhhId) {
            this.researcherService.getResearcher(p.rrhhId).subscribe({
              next: r => {
                p.fullName = r.fullname || `RRHH #${p.rrhhId}`;
              },
              error: () => {
                p.fullName = `RRHH #${p.rrhhId}`;
              }
            });
          }
        });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }

  editProject(): void {
    if (this.project?.id != null) {
      this.router.navigate(['/projects', this.project.id, 'edit']);
    }
  }

  getClusters(): string {
    if (!this.project?.cluster) {
      return '';
    }
    const labels: Record<string, string> = {
      '1': 'Cluster I',
      '2': 'Cluster II',
      '3': 'Cluster III',
      '4': 'Cluster IV',
      '5': 'Cluster V'
    };
    return this.project.cluster.split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .map(id => labels[id] || id)
      .join(', ');
  }

  getBasalLabel(): string {
    const v = (this.project?.basal || 'N').toUpperCase();
    return v === 'S' ? 'Yes' : 'No';
  }

  getFundingLabel(): string {
    if (!this.project) {
      return '';
    }
    if (this.project.otherFundingType) {
      return this.project.otherFundingType;
    }
    return this.project.fundingTypeLabel
      || this.project.fundingType?.idDescripcion
      || '—';
  }
}
