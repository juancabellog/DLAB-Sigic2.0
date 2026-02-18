import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { PublicationService } from '../../core/services/publication.service';
import { ScientificEventsService } from '../../core/services/scientific-events.service';
import { ResearcherService } from '../../core/services/researcher.service';
import { ThesisService } from '../../core/services/thesis.service';
import { TechnologyTransferService } from '../../core/services/technology-transfer.service';
import { PostdoctoralFellowsService } from '../../core/services/postdoctoral-fellows.service';
import { OutreachActivitiesService } from '../../core/services/outreach-activities.service';
import { ScientificCollaborationsService } from '../../core/services/scientific-collaborations.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  loadingStats: boolean = true;
  publicationCount: number = 0;
  scientificEventsCount: number = 0;
  researcherCount: number = 0;
  thesisStudentCount: number = 0;
  postdoctoralFellowsCount: number = 0;
  outreachActivitiesCount: number = 0;
  scientificCollaborationsCount: number = 0;
  technologyTransferCount: number = 0;
  impactMetricsUpdate: string | null = null;

  constructor(
    private publicationService: PublicationService,
    private scientificEventsService: ScientificEventsService,
    private researcherService: ResearcherService,
    private thesisService: ThesisService,
    private postdoctoralFellowsService: PostdoctoralFellowsService,
    private outreachActivitiesService: OutreachActivitiesService,
    private scientificCollaborationsService: ScientificCollaborationsService,
    private technologyTransferService: TechnologyTransferService,
    private router: Router,
    public authService: AuthService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.loadStatistics();
    this.loadImpactMetricsUpdate();
  }

  loadImpactMetricsUpdate(): void {
    this.dashboardService.getImpactMetricsUpdate().subscribe({
      next: (response) => {
        this.impactMetricsUpdate = response.lastUpdate;
      },
      error: (error) => {
        console.error('Error loading impact metrics update:', error);
        // Silently fail - don't show error if the update date is not available
      }
    });
  }

  loadStatistics(): void {
    this.loadingStats = true;
    
    // Cargar estadísticas de todos los módulos en paralelo
    forkJoin({
      publications: this.publicationService.getPublications({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      scientificEvents: this.scientificEventsService.getScientificEvents({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      researchers: this.researcherService.getResearchers({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      thesis: this.thesisService.getThesis({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      postdoctoralFellows: this.postdoctoralFellowsService.getPostdoctoralFellows({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      outreachActivities: this.outreachActivitiesService.getOutreachActivities({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      scientificCollaborations: this.scientificCollaborationsService.getScientificCollaborations({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      ),
      technologyTransfer: this.technologyTransferService.getTechnologyTransfers({ page: 0, size: 1 }).pipe(
        catchError(() => of({ totalElements: 0 } as any))
      )
    }).subscribe({
      next: (stats) => {
        // Extraer totalElements de la respuesta paginada
        this.publicationCount = (stats.publications as any)?.totalElements || 0;
        this.scientificEventsCount = (stats.scientificEvents as any)?.totalElements || 0;
        this.researcherCount = (stats.researchers as any)?.totalElements || 0;
        this.thesisStudentCount = (stats.thesis as any)?.totalElements || 0;
        this.postdoctoralFellowsCount = (stats.postdoctoralFellows as any)?.totalElements || 0;
        this.outreachActivitiesCount = (stats.outreachActivities as any)?.totalElements || 0;
        this.scientificCollaborationsCount = (stats.scientificCollaborations as any)?.totalElements || 0;
        this.technologyTransferCount = (stats.technologyTransfer as any)?.totalElements || 0;
        
        this.loadingStats = false;
      },
      error: (error) => {
        console.error('Error loading statistics:', error);
        this.loadingStats = false;
      }
    });
  }

  navigateToViewAll(route: string): void {
    this.router.navigate([route]);
  }
}