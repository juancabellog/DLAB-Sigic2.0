import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of, firstValueFrom } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { OutreachActivitiesService } from '../../../core/services/outreach-activities.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { DifusionDTO, RRHHDTO, PublicoObjetivoDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-oa-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './oa-view.component.html',
  styleUrls: ['./oa-view.component.scss']
})
export class OaViewComponent implements OnInit {
  activity: DifusionDTO | null = null;
  loading: boolean = false;
  participants: Array<{ name: string; role: string; corresponding: boolean }> = [];
  targetAudiences: PublicoObjetivoDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private outreachActivitiesService: OutreachActivitiesService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    this.loadTargetAudiences();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadActivity(parseInt(id));
    }
  }

  loadTargetAudiences(): void {
    this.baseHttp.get<PublicoObjetivoDTO[]>('/catalogs/target-audiences').pipe(
      catchError(error => {
        console.error('Error loading target audiences:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.targetAudiences = items;
    });
  }

  loadActivity(id: number): void {
    this.loading = true;
    this.outreachActivitiesService.getOutreachActivity(id).pipe(
      catchError(error => {
        console.error('Error loading outreach activity:', error);
        this.messageService.error('Error loading outreach activity. Please try again later.');
        this.goBack();
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async activity => {
      this.activity = activity;
      
      if (!activity) {
        this.messageService.error('Outreach activity not found');
        this.goBack();
        return;
      }

      // Cargar participantes
      if (activity.participantes && activity.participantes.length > 0) {
        await this.loadParticipants(activity.participantes);
      }
    });
  }

  async loadParticipants(participantes: any[]): Promise<void> {
    this.participants = [];
    
    for (const p of participantes) {
      if (p.rrhhId) {
        try {
          const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
          if (researcher.id) {
            this.participants.push({
              name: researcher.fullname || 'Unknown',
              role: 'Participant',
              corresponding: p.corresponding || false
            });
          }
        } catch (error) {
          console.error('Error loading researcher:', error);
        }
      }
    }
  }

  getActivityTitle(): string {
    return this.activity?.descripcion || 'Untitled Outreach Activity';
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.activity?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.activity?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getLocation(): string {
    if (!this.activity) return 'N/A';
    const parts: string[] = [];
    if (this.activity.ciudad) parts.push(this.activity.ciudad);
    if (this.activity.pais?.idDescripcion) parts.push(this.activity.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  getTargetAudiences(): string[] {
    if (!this.activity?.publicoObjetivo) return [];
    try {
      const audienceIds = this.activity.publicoObjetivo.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
      return audienceIds.map(id => {
        const audience = this.targetAudiences.find(ta => ta.id === id);
        return audience?.idDescripcion || `Audience ${id}`;
      });
    } catch (e) {
      return [];
    }
  }

  goBack(): void {
    this.router.navigate(['/outreach-activities']);
  }

  editActivity(): void {
    if (this.activity?.id) {
      this.router.navigate(['/outreach-activities', this.activity.id, 'edit']);
    }
  }

  deleteActivity(): void {
    if (!this.activity?.id) return;
    
    const title = this.getActivityTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.outreachActivitiesService.deleteOutreachActivity(this.activity!.id!).subscribe({
            next: () => {
              this.messageService.success('Outreach Activity Deleted', `${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting outreach activity:', error);
              this.messageService.error('Error deleting outreach activity. Please try again.');
            }
          });
        }
      }
    );
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.activity?.linkPDF);
  }
}


