import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { AwardService } from '../../../core/services/award.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { AwardProductDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-award-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './award-view.component.html',
  styleUrls: ['./award-view.component.scss']
})
export class AwardViewComponent implements OnInit {
  award: AwardProductDTO | null = null;
  loading = false;
  participants: Array<{ rrhhId?: number; fullName?: string }> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private awardService: AwardService,
    private messageService: MessageService,
    private researcherService: ResearcherService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/awards']);
      return;
    }
    this.loadAward(id);
  }

  loadAward(id: number): void {
    this.loading = true;
    this.awardService.getAward(id).pipe(
      catchError(() => {
        this.messageService.error('Award not found.');
        this.router.navigate(['/awards']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      this.award = data;
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
    this.router.navigate(['/awards']);
  }

  editAward(): void {
    if (this.award?.id != null) {
      this.router.navigate(['/awards', this.award.id, 'edit']);
    }
  }

  getInstitutionLabel(): string {
    if (!this.award) {
      return '—';
    }
    return this.award.institutionLabel
      || this.award.institucion?.descripcion
      || this.award.institucion?.idDescripcion
      || '—';
  }

  getCountryLabel(): string {
    if (!this.award) {
      return '—';
    }
    return this.award.countryLabel
      || this.award.institucion?.countryLabel
      || '—';
  }

  getClusters(): string {
    if (!this.award?.cluster) {
      return '';
    }
    const labels: Record<string, string> = {
      '1': 'Cluster I',
      '2': 'Cluster II',
      '3': 'Cluster III',
      '4': 'Cluster IV',
      '5': 'Cluster V'
    };
    return this.award.cluster.split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .map(id => labels[id] || id)
      .join(', ');
  }

  getBasalLabel(): string {
    const v = (this.award?.basal || 'N').toUpperCase();
    return v === 'S' ? 'Yes' : 'No';
  }
}
