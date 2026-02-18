import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-temporal-evolution',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    RouterModule
  ],
  template: `
    <div class="coming-soon">
      <mat-card>
        <mat-card-content>
          <div class="content">
            <mat-icon>timeline</mat-icon>
            <h2>Evolución Temporal</h2>
            <p>Esta funcionalidad estará disponible próximamente</p>
            <button mat-raised-button color="primary" routerLink="/analysis">
              Volver al Centro de Análisis
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .coming-soon {
      padding: 48px;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 60vh;
      
      mat-card {
        max-width: 500px;
        text-align: center;
        
        .content {
          padding: 24px;
          
          mat-icon {
            font-size: 64px;
            width: 64px;
            height: 64px;
            color: #f57c00;
            margin-bottom: 16px;
          }
          
          h2 {
            margin: 0 0 16px 0;
          }
          
          p {
            color: #757575;
            margin-bottom: 24px;
          }
        }
      }
    }
  `]
})
export class TemporalEvolutionComponent {
}
