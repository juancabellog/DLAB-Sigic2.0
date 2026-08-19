import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
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
import { ParticipacionEventoCientificoDTO } from '../../../core/models/backend-dtos';
import { MessageService } from '../../../core/services/message.service';
import { UtilsService } from '../../../core/services/utils.service';
import { ParticipationScientificEventsService } from '../../../core/services/participation-scientific-events.service';

@Component({
  selector: 'app-pse-view',
  standalone: true,
  imports: [
    CommonModule,
    LocalDatePipe,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './pse-view.component.html',
  styleUrls: ['./pse-view.component.scss']
})
export class PseViewComponent implements OnInit {
  item: ParticipacionEventoCientificoDTO | null = null;
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private pseService: ParticipationScientificEventsService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadItem(parseInt(id, 10));
    }
  }

  loadItem(id: number): void {
    this.loading = true;
    this.pseService.getParticipationScientificEvent(id).pipe(
      catchError(error => {
        console.error('Error loading participation in scientific events:', error);
        this.messageService.error('Error loading participation in scientific events. Please try again later.');
        this.goBack();
        return of(null);
      })
    ).subscribe(item => {
      this.item = item;
      this.loading = false;
      if (!item) {
        this.messageService.error('Participation in scientific events not found');
        this.goBack();
      }
    });
  }

  getTitle(): string {
    return this.item?.descripcion || 'Untitled Participation';
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

  getLocation(): string {
    const parts = [];
    if (this.item?.ciudad) parts.push(this.item.ciudad);
    if (this.item?.pais?.idDescripcion) parts.push(this.item.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  getBasalStatus(): string {
    const basal = this.item?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    const basal = this.item?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.item?.linkPDF);
  }

  goBack(): void {
    this.router.navigate(['/participation-scientific-events']);
  }

  editItem(): void {
    if (this.item?.id) {
      this.router.navigate(['/participation-scientific-events', this.item.id, 'edit']);
    }
  }

  deleteItem(): void {
    if (!this.item?.id) return;
    const title = this.getTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.pseService.deleteParticipationScientificEvent(this.item!.id!).subscribe({
            next: () => {
              this.messageService.success(`${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting participation in scientific events:', error);
              this.messageService.error('Error deleting participation in scientific events. Please try again.');
            }
          });
        }
      },
      'Delete Participation in Scientific Events'
    );
  }
}
