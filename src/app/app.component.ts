import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { ToastModule } from 'primeng/toast';

import { AuthService } from './core/services/auth.service';
import { ConnectionIndicatorComponent } from './shared/components/connection-indicator/connection-indicator.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    ToastModule,
    ConnectionIndicatorComponent
  ],
  template: `
    <p-toast></p-toast>
    <app-connection-indicator></app-connection-indicator>
    <div class="app-container">
      <mat-toolbar color="primary" class="app-header">
        <button mat-icon-button (click)="sidenav.toggle()">
          <mat-icon>menu</mat-icon>
        </button>
        <span>Scientific Products Platform</span>
        <span class="spacer"></span>
        
        <div *ngIf="authService.isAuthenticated()">
          <button mat-button [matMenuTriggerFor]="userMenu">
            <mat-icon>account_circle</mat-icon>
            {{ authService.getCurrentUser()?.username }}
            <mat-icon>arrow_drop_down</mat-icon>
          </button>
          <mat-menu #userMenu="matMenu">
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon>
              Logout
            </button>
          </mat-menu>
        </div>
      </mat-toolbar>

      <mat-sidenav-container class="app-content">
        <mat-sidenav #sidenav mode="side" opened class="sidebar" disableClose="true">
          <div class="sidebar-content">
            <mat-nav-list>
              <a mat-list-item routerLink="/dashboard" routerLinkActive="active" (click)="navigateTo('/dashboard')">
                <mat-icon matListItemIcon>dashboard</mat-icon>
                <span matListItemTitle>Dashboard</span>
              </a>
              
              <mat-divider></mat-divider>
              
              <a mat-list-item routerLink="/publications" routerLinkActive="active" (click)="navigateTo('/publications')">
                <mat-icon matListItemIcon>article</mat-icon>
                <span matListItemTitle>Publications</span>
              </a>
              
              <a mat-list-item routerLink="/scientific-events" routerLinkActive="active" (click)="navigateTo('/scientific-events')">
                <mat-icon matListItemIcon>event_note</mat-icon>
                <span matListItemTitle>Scientific Events</span>
              </a>
              
              <a mat-list-item routerLink="/thesis-students" routerLinkActive="active" (click)="navigateTo('/thesis-students')">
                <mat-icon matListItemIcon>school</mat-icon>
                <span matListItemTitle>Thesis Students</span>
              </a>
              
              <a mat-list-item routerLink="/postdoctoral-fellows" routerLinkActive="active" (click)="navigateTo('/postdoctoral-fellows')">
                <mat-icon matListItemIcon>work</mat-icon>
                <span matListItemTitle>Postdoctoral Fellows</span>
              </a>
              
              <a mat-list-item routerLink="/outreach-activities" routerLinkActive="active" (click)="navigateTo('/outreach-activities')">
                <mat-icon matListItemIcon>campaign</mat-icon>
                <span matListItemTitle>Outreach Activities</span>
              </a>
              
              <a mat-list-item routerLink="/scientific-collaborations" routerLinkActive="active" (click)="navigateTo('/scientific-collaborations')">
                <mat-icon matListItemIcon>handshake</mat-icon>
                <span matListItemTitle>Scientific Collaborations</span>
              </a>
              
              <a mat-list-item routerLink="/technology-transfer" routerLinkActive="active" (click)="navigateTo('/technology-transfer')">
                <mat-icon matListItemIcon>transfer_within_a_station</mat-icon>
                <span matListItemTitle>Technology Transfer</span>
              </a>
              
              <mat-divider></mat-divider>
              
              <a mat-list-item routerLink="/researchers" routerLinkActive="active" (click)="navigateTo('/researchers')">
                <mat-icon matListItemIcon>people</mat-icon>
                <span matListItemTitle>Researchers</span>
              </a>
              
              <a mat-list-item routerLink="/projects" routerLinkActive="active" (click)="navigateTo('/projects')">
                <mat-icon matListItemIcon>folder</mat-icon>
                <span matListItemTitle>Projects</span>
              </a>
              
              <a mat-list-item routerLink="/catalogs" routerLinkActive="active" (click)="navigateTo('/catalogs')">
                <mat-icon matListItemIcon>settings</mat-icon>
                <span matListItemTitle>Catalogs</span>
              </a>
            </mat-nav-list>
          </div>
        </mat-sidenav>

        <mat-sidenav-content>
          <router-outlet></router-outlet>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
    
    .sidebar {
      width: 250px;
    }
    
    .sidebar-content {
      padding: 20px 0;
    }
    
    .mat-mdc-list-item.active {
      background-color: #e3f2fd;
      color: #1976d2;
    }
    
    .mat-mdc-list-item.active .mat-icon {
      color: #1976d2;
    }
  `]
})
export class AppComponent implements OnInit {
  title = 'Scientific Products Platform';

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Validar token al cargar la aplicación
    this.validateTokenOnStartup();
  }

  private validateTokenOnStartup(): void {
    // Si hay un token guardado, validarlo
    if (this.authService.getToken()) {
      this.authService.validateToken().subscribe({
        next: (isValid) => {
          if (!isValid) {
            // Token inválido, redirigir al login
            console.warn('Token inválido, redirigiendo al login');
            this.router.navigate(['/login']);
          }
        },
        error: (error) => {
          // Error al validar token, redirigir al login
          console.error('Error validando token:', error);
          this.router.navigate(['/login']);
        }
      });
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  navigateTo(route: string) {
    this.router.navigate([route]);
  }
}