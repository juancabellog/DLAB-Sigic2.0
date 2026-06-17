import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-gender-observatory',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    RouterModule
  ],
  template: `
    <div class="under-construction">
      <mat-card>
        <mat-card-content>
          <div class="content">
            <mat-icon>wc</mat-icon>
            <h2>Gender Observatory</h2>
            <p class="lead">
              Tracking of gender-disaggregated data and key indicators of equity.
            </p>
            <p class="status">This section is under construction.</p>
            <button mat-raised-button color="primary" routerLink="/analysis">
              Back to Analysis Center
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .under-construction {
      padding: 48px;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 60vh;

      mat-card {
        max-width: 560px;
        text-align: center;

        .content {
          padding: 24px;

          mat-icon {
            font-size: 64px;
            width: 64px;
            height: 64px;
            color: #1b5e20;
            margin-bottom: 16px;
          }

          h2 {
            margin: 0 0 16px 0;
          }

          .lead {
            color: #424242;
            margin: 0 0 12px 0;
            line-height: 1.5;
          }

          .status {
            color: #757575;
            margin-bottom: 24px;
          }
        }
      }
    }
  `]
})
export class GenderObservatoryComponent {}
