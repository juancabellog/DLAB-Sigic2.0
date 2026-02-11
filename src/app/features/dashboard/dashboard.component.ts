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
  template: `
    <div class="dashboard-container">
      <h1>Scientific Products Dashboard</h1>
      
      <div class="dashboard-grid">
        <mat-card class="kpi-card" (click)="navigateToViewAll('/publications')">
          <mat-card-header>
            <mat-icon mat-card-avatar>article</mat-icon>
            <mat-card-title>Publications</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ publicationCount }}</span>
            </div>
            <div class="kpi-subtitle">Scientific publications</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/publications" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/publications/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/scientific-events')">
          <mat-card-header>
            <mat-icon mat-card-avatar>event_note</mat-icon>
            <mat-card-title>Scientific Events</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ scientificEventsCount }}</span>
            </div>
            <div class="kpi-subtitle">Event organizations</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/scientific-events" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/scientific-events/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/thesis-students')">
          <mat-card-header>
            <mat-icon mat-card-avatar>school</mat-icon>
            <mat-card-title>Thesis Students</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ thesisStudentCount }}</span>
            </div>
            <div class="kpi-subtitle">Student researchers</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/thesis-students" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/thesis-students/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/postdoctoral-fellows')">
          <mat-card-header>
            <mat-icon mat-card-avatar>work</mat-icon>
            <mat-card-title>Postdoctoral Fellows</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ postdoctoralFellowsCount }}</span>
            </div>
            <div class="kpi-subtitle">Postdoctoral researchers</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/postdoctoral-fellows" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/postdoctoral-fellows/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/outreach-activities')">
          <mat-card-header>
            <mat-icon mat-card-avatar>campaign</mat-icon>
            <mat-card-title>Outreach Activities</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ outreachActivitiesCount }}</span>
            </div>
            <div class="kpi-subtitle">Diffusion activities</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/outreach-activities" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/outreach-activities/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/scientific-collaborations')">
          <mat-card-header>
            <mat-icon mat-card-avatar>handshake</mat-icon>
            <mat-card-title>Scientific Collaborations</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ scientificCollaborationsCount }}</span>
            </div>
            <div class="kpi-subtitle">Research collaborations</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/scientific-collaborations" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/scientific-collaborations/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/technology-transfer')">
          <mat-card-header>
            <mat-icon mat-card-avatar>transfer_within_a_station</mat-icon>
            <mat-card-title>Technology Transfer</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ technologyTransferCount }}</span>
            </div>
            <div class="kpi-subtitle">Transfer agreements</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/technology-transfer" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/technology-transfer/new" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="kpi-card" (click)="navigateToViewAll('/researchers')">
          <mat-card-header>
            <mat-icon mat-card-avatar>people</mat-icon>
            <mat-card-title>Researchers</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="kpi-value">
              <span *ngIf="loadingStats">-</span>
              <span *ngIf="!loadingStats">{{ researcherCount }}</span>
            </div>
            <div class="kpi-subtitle">Active researchers</div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/researchers" (click)="$event.stopPropagation()">View All</button>
            <button mat-button routerLink="/researchers" (click)="$event.stopPropagation()">Add New</button>
          </mat-card-actions>
        </mat-card>
      </div>

      <div class="recent-activities">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Recent Activities</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>No recent activities to display.</p>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 30px;
      color: #333;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;
      margin-bottom: 30px;
    }

    .kpi-card {
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0,0,0,0.15);
    }

    .kpi-value {
      font-size: 32px;
      font-weight: 500;
      color: #1976d2;
      margin: 10px 0;
    }

    .kpi-subtitle {
      color: #666;
      font-size: 14px;
    }

    .recent-activities {
      margin-top: 30px;
    }

    @media (max-width: 768px) {
      .dashboard-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
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

  constructor(
    private publicationService: PublicationService,
    private scientificEventsService: ScientificEventsService,
    private researcherService: ResearcherService,
    private thesisService: ThesisService,
    private postdoctoralFellowsService: PostdoctoralFellowsService,
    private outreachActivitiesService: OutreachActivitiesService,
    private scientificCollaborationsService: ScientificCollaborationsService,
    private technologyTransferService: TechnologyTransferService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStatistics();
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