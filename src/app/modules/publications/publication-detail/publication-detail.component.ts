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
import { PublicationService } from '../../../core/services/publication.service';
import { UtilsService } from '../../../core/services/utils.service';
import { PublicacionDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-publication-detail',
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
  templateUrl: './publication-detail.component.html',
  styleUrls: ['./publication-detail.component.scss']
})
export class PublicationDetailComponent implements OnInit {
  publication: PublicacionDTO | null = null;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private publicationService: PublicationService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPublication(parseInt(id));
    }
  }

  loadPublication(id: number): void {
    this.loading = true;
    this.publicationService.getPublication(id).pipe(
      catchError(error => {
        console.error('Error loading publication:', error);
        this.messageService.error('Error loading publication. Please try again later.');
        this.goBack();
        return of(null);
      })
    ).subscribe(publication => {
      this.publication = publication;
      this.loading = false;
      
      if (!publication) {
        this.messageService.error('Publication not found');
        this.goBack();
      }
    });
  }

  getPublicationTitle(): string {
    return this.publication?.descripcion || 'Untitled Publication';
  }

  getJournalName(): string {
    return this.publication?.journal?.descripcion || this.publication?.journal?.abbreviation || 'Unknown Journal';
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
    this.router.navigate(['/publications']);
  }

  editPublication(): void {
    if (this.publication?.id) {
      this.router.navigate(['/publications', this.publication.id, 'edit']);
    }
  }

  deletePublication(): void {
    if (!this.publication?.id) return;
    
    const title = this.getPublicationTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.publicationService.deletePublication(this.publication!.id!).subscribe({
            next: () => {
              this.messageService.success(`${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting publication:', error);
              this.messageService.error('Error deleting publication. Please try again.');
            }
          });
        }
      },
      'Delete Publication'
    );
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.publication?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.publication?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.publication?.linkPDF);
  }

  /**
   * Formatea el factor de impacto con 4 decimales según formato chileno
   * Separador de miles: punto (.)
   * Separador de decimales: coma (,)
   */
  formatImpactFactor(factor: number | null | undefined): string {
    if (factor == null || factor === undefined) {
      return 'N/A';
    }
    // Formatear con 4 decimales usando formato chileno
    const parts = factor.toFixed(4).split('.');
    const integerPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    const decimalPart = parts[1];
    return `${integerPart},${decimalPart}`;
  }
}
