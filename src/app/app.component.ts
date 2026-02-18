import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'Scientific Products Platform';

  constructor(
    public authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Validar token al cargar la aplicación
    this.validateTokenOnStartup();
    
    // Suscribirse a cambios en el usuario para actualizar la vista
    this.authService.currentUser$.subscribe(() => {
      this.cdr.detectChanges();
    });
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
          } else {
            // Token válido, refrescar roles desde el backend
            this.authService.refreshRolesFromToken();
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