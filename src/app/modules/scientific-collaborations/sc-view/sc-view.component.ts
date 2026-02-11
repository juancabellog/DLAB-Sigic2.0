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
import { ScientificCollaborationsService } from '../../../core/services/scientific-collaborations.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ColaboracionDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-sc-view',
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
  templateUrl: './sc-view.component.html',
  styleUrls: ['./sc-view.component.scss']
})
export class ScViewComponent implements OnInit {
  collaboration: ColaboracionDTO | null = null;
  loading: boolean = false;
  participants: Array<{ name: string; role: string; corresponding: boolean }> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private scientificCollaborationsService: ScientificCollaborationsService,
    private researcherService: ResearcherService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCollaboration(parseInt(id));
    }
  }

  loadCollaboration(id: number): void {
    this.loading = true;
    this.scientificCollaborationsService.getScientificCollaboration(id).pipe(
      catchError(error => {
        console.error('Error loading scientific collaboration:', error);
        this.messageService.error('Error loading scientific collaboration. Please try again later.');
        this.goBack();
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async collaboration => {
      this.collaboration = collaboration;
      
      if (!collaboration) {
        this.messageService.error('Scientific collaboration not found');
        this.goBack();
        return;
      }

      // Cargar participantes
      if (collaboration.participantes && collaboration.participantes.length > 0) {
        await this.loadParticipants(collaboration.participantes);
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

  getCollaborationTitle(): string {
    return this.collaboration?.descripcion || 'Untitled Scientific Collaboration';
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.collaboration?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.collaboration?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getOrigin(): string {
    if (!this.collaboration) return 'N/A';
    const parts: string[] = [];
    if (this.collaboration.ciudadOrigen) parts.push(this.collaboration.ciudadOrigen);
    if (this.collaboration.paisOrigen?.idDescripcion) parts.push(this.collaboration.paisOrigen.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  getDestination(): string {
    if (!this.collaboration) return 'N/A';
    const parts: string[] = [];
    if (this.collaboration.ciudadDestino) parts.push(this.collaboration.ciudadDestino);
    if (this.collaboration.codigoPaisDestino) parts.push(this.collaboration.codigoPaisDestino);
    return parts.length > 0 ? parts.join(', ') : 'N/A';
  }

  goBack(): void {
    this.router.navigate(['/scientific-collaborations']);
  }

  editCollaboration(): void {
    if (this.collaboration?.id) {
      this.router.navigate(['/scientific-collaborations', this.collaboration.id, 'edit']);
    }
  }

  deleteCollaboration(): void {
    if (!this.collaboration?.id) return;
    
    const title = this.getCollaborationTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.scientificCollaborationsService.deleteScientificCollaboration(this.collaboration!.id!).subscribe({
            next: () => {
              this.messageService.success('Scientific Collaboration Deleted', `${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting scientific collaboration:', error);
              this.messageService.error('Error deleting scientific collaboration. Please try again.');
            }
          });
        }
      }
    );
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.collaboration?.linkPDF);
  }
}


