import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MessageService } from '../../../core/services/message.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-researcher-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './researcher-view.component.html',
  styleUrls: ['./researcher-view.component.scss']
})
export class ResearcherViewComponent implements OnInit {
  researcher: RRHHDTO | null = null;
  loading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private researcherService: ResearcherService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadResearcher(parseInt(id));
    }
  }

  loadResearcher(id: number): void {
    this.loading = true;
    this.researcherService.getResearcher(id).subscribe({
      next: (researcher) => {
        this.researcher = researcher;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading researcher:', error);
        this.messageService.error('Researcher not found');
        this.loading = false;
        this.goBack();
      }
    });
  }

  getGenderLabel(codigoGenero: string | undefined): string {
    if (!codigoGenero) return 'N/A';
    return codigoGenero === 'M' ? 'Masculino' : codigoGenero === 'F' ? 'Femenino' : codigoGenero;
  }

  goBack(): void {
    this.router.navigate(['/researchers']);
  }

  editResearcher(): void {
    if (this.researcher) {
      this.router.navigate(['/researchers', this.researcher.id, 'edit']);
    }
  }

  deleteResearcher(): void {
    if (this.researcher) {
      this.messageService.confirm(
        `Are you sure you want to delete "${this.researcher.fullname}"?`,
        (accepted: boolean) => {
          if (accepted) {
            this.researcherService.deleteResearcher(this.researcher!.id!).subscribe({
              next: () => {
                this.messageService.success('Researcher deleted successfully');
                this.goBack();
              },
              error: (error) => {
                console.error('Error deleting researcher:', error);
                this.messageService.error('Error deleting researcher');
              }
            });
          }
        },
        'Delete Researcher'
      );
    }
  }
}