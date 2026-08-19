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
import { BookService } from '../../../core/services/book.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BookProductDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-book-view',
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
  templateUrl: './book-view.component.html',
  styleUrls: ['./book-view.component.scss']
})
export class BookViewComponent implements OnInit {
  book: BookProductDTO | null = null;
  loading = false;
  participants: Array<{ rrhhId?: number; fullName?: string }> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private messageService: MessageService,
    private researcherService: ResearcherService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/books']);
      return;
    }
    this.loadBook(id);
  }

  loadBook(id: number): void {
    this.loading = true;
    this.bookService.getBook(id).pipe(
      catchError(() => {
        this.messageService.error('Book not found.');
        this.router.navigate(['/books']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      this.book = data;
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
    this.router.navigate(['/books']);
  }

  editBook(): void {
    if (this.book?.id != null) {
      this.router.navigate(['/books', this.book.id, 'edit']);
    }
  }

  getWorkTypeLabel(): string {
    if (!this.book) {
      return '—';
    }
    return this.book.bookTypeLabel
      || this.book.bookType?.idDescripcion
      || '—';
  }

  isChapter(): boolean {
    return this.book?.idBookType === 2 || this.book?.bookType?.id === 2;
  }

  getPages(): string {
    if (!this.book || this.book.firstPage == null || this.book.lastPage == null) {
      return '—';
    }
    return `${this.book.firstPage} - ${this.book.lastPage}`;
  }

  getClusters(): string {
    if (!this.book?.cluster) {
      return '';
    }
    const labels: Record<string, string> = {
      '1': 'Cluster I',
      '2': 'Cluster II',
      '3': 'Cluster III',
      '4': 'Cluster IV',
      '5': 'Cluster V'
    };
    return this.book.cluster.split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .map(id => labels[id] || id)
      .join(', ');
  }

  getBasalLabel(): string {
    const v = (this.book?.basal || 'N').toUpperCase();
    return v === 'S' ? 'Yes' : 'No';
  }
}
