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
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { ScientificEventsService } from '../../../core/services/scientific-events.service';
import { UtilsService } from '../../../core/services/utils.service';
import { OrganizacionEventosCientificosDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-scientific-events-view',
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
  templateUrl: './scientific-events-view.component.html',
  styleUrls: ['./scientific-events-view.component.scss']
})
export class ScientificEventsViewComponent implements OnInit {
  event: OrganizacionEventosCientificosDTO | null = null;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private scientificEventsService: ScientificEventsService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEvent(parseInt(id));
    }
  }

  loadEvent(id: number): void {
    this.loading = true;
    this.scientificEventsService.getScientificEvent(id).pipe(
      catchError(error => {
        console.error('Error loading scientific event:', error);
        this.messageService.error('Error loading scientific event. Please try again later.');
        this.goBack();
        return of(null);
      })
    ).subscribe(event => {
      this.event = event;
      this.loading = false;
      
      if (!event) {
        this.messageService.error('Scientific event not found');
        this.goBack();
      }
    });
  }

  getEventTitle(): string {
    return this.event?.descripcion || 'Untitled Event';
  }

  getEventType(): string {
    return this.event?.tipoEvento?.descripcion || 'Type not specified';
  }

  getEventLocation(): string {
    const parts = [];
    if (this.event?.ciudad) parts.push(this.event.ciudad);
    if (this.event?.pais?.idDescripcion) parts.push(this.event.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  getStatusColor(status: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!status) return 'primary';
    switch (status.toUpperCase()) {
      case 'PUBLISHED': return 'primary';
      case 'IN_REVIEW': return 'accent';
      case 'DRAFT': return 'warn';
      default: return 'primary';
    }
  }

  goBack(): void {
    this.router.navigate(['/scientific-events']);
  }

  editEvent(): void {
    if (this.event?.id) {
      this.router.navigate(['/scientific-events', this.event.id, 'edit']);
    }
  }

  deleteEvent(): void {
    if (!this.event?.id) return;
    
    const title = this.getEventTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.scientificEventsService.deleteScientificEvent(this.event!.id!).subscribe({
            next: () => {
              this.messageService.success(`${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting scientific event:', error);
              this.messageService.error('Error deleting scientific event. Please try again.');
            }
          });
        }
      },
      'Delete Scientific Event'
    );
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.event?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.event?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.event?.linkPDF);
  }
}

