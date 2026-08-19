import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of, firstValueFrom } from 'rxjs';

import {
  ParticipantManagerComponent,
  ParticipantDTO
} from '../../../shared/components/participant-manager/participant-manager.component';
import { MessageService } from '../../../core/services/message.service';
import { BookService } from '../../../core/services/book.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { ProgressReportService } from '../../../core/services/progress-report.service';
import {
  BookProductDTO,
  BookTypeDTO,
  RRHHDTO
} from '../../../core/models/backend-dtos';

const BOOK_TYPE_CHAPTER_ID = 2;

@Component({
  selector: 'app-book-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ParticipantManagerComponent
  ],
  templateUrl: './book-edit.component.html',
  styleUrls: ['./book-edit.component.scss']
})
export class BookEditComponent implements OnInit {
  readonly BOOK_TYPE_CHAPTER_ID = BOOK_TYPE_CHAPTER_ID;

  isEditMode = false;
  bookId: number | null = null;
  loading = false;
  saving = false;

  book: BookProductDTO = {
    descripcion: '',
    chapterTitle: null,
    firstPage: undefined,
    lastPage: undefined,
    editorialCityCountry: null,
    year: undefined,
    isbn: null,
    basal: 'S',
    progressReport: '',
    codigoANID: ''
  };

  bookTypes: BookTypeDTO[] = [];
  selectedBookTypeId: number | null = null;
  participants: ParticipantDTO[] = [];
  isBasal = true;

  clusterOptions = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];
  selectedClusters: number[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private messageService: MessageService,
    private baseHttp: BaseHttpService,
    private progressReportService: ProgressReportService,
    private researcherService: ResearcherService
  ) {}

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Book' : 'New Book';
  }

  get saveButtonText(): string {
    return this.isEditMode ? 'Update Book' : 'Create Book';
  }

  get isChapterSelected(): boolean {
    return this.selectedBookTypeId === BOOK_TYPE_CHAPTER_ID;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.bookId = Number(idParam);
    }
    this.loadCatalogs();
    if (this.isEditMode && this.bookId) {
      this.loadBook(this.bookId);
    }
  }

  loadCatalogs(): void {
    this.baseHttp.get<BookTypeDTO[]>('/catalogs/book-types').subscribe({
      next: data => this.bookTypes = data || [],
      error: () => this.bookTypes = []
    });
  }

  loadBook(id: number): void {
    this.loading = true;
    this.bookService.getBook(id).pipe(
      catchError(err => {
        console.error(err);
        this.messageService.error('Could not load book.');
        this.router.navigate(['/books']);
        return of(null);
      }),
      finalize(() => this.loading = false)
    ).subscribe(data => {
      if (!data) {
        return;
      }
      this.book = { ...data };
      this.isBasal = (data.basal || 'N').toUpperCase() === 'S';
      this.selectedBookTypeId = data.idBookType ?? data.bookType?.id ?? null;
      this.selectedClusters = this.parseClusters(data.cluster);
      void this.loadParticipants(data.participantes || []);
    });
  }

  private async loadParticipants(participantes: any[]): Promise<void> {
    this.participants = [];
    for (const p of participantes) {
      if (!p.rrhhId) {
        continue;
      }
      try {
        const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
        if (researcher.id) {
          this.participants.push({
            rrhhId: researcher.id,
            fullName: researcher.fullname || '',
            idRecurso: researcher.idRecurso,
            orcid: researcher.orcid,
            participationTypeId: p.tipoParticipacionId,
            corresponding: p.corresponding || false,
            order: p.orden || 0
          });
        }
      } catch (error) {
        console.error('Error loading researcher', error);
      }
    }
  }

  private parseClusters(raw?: string): number[] {
    if (!raw) {
      return [];
    }
    return raw.split(',')
      .map(s => Number(s.trim()))
      .filter(n => Number.isFinite(n));
  }

  onBookTypeChange(): void {
    if (!this.isChapterSelected) {
      this.book.chapterTitle = null;
    }
  }

  isClusterSelected(id: number): boolean {
    return this.selectedClusters.includes(id);
  }

  onClusterChange(id: number, checked: boolean): void {
    if (checked) {
      if (!this.selectedClusters.includes(id)) {
        this.selectedClusters = [...this.selectedClusters, id].sort((a, b) => a - b);
      }
    } else {
      this.selectedClusters = this.selectedClusters.filter(c => c !== id);
    }
  }

  onBasalChange(checked: boolean): void {
    this.isBasal = checked;
    this.book.basal = checked ? 'S' : 'N';
  }

  onYearChange(): void {
    if (this.book.year != null && Number.isFinite(Number(this.book.year))) {
      const y = Number(this.book.year);
      this.book.fechaInicio = `${y}-01-01`;
      this.book.progressReport = this.progressReportService.calculateProgressReport(this.book.fechaInicio);
    }
  }

  onParticipantsChange(list: ParticipantDTO[]): void {
    this.participants = list;
  }

  validateForm(): string | null {
    if (this.selectedBookTypeId == null) {
      return 'Work Type is required.';
    }
    if (!this.book.descripcion?.trim()) {
      return 'Book Title is required.';
    }
    if (this.isChapterSelected && !this.book.chapterTitle?.trim()) {
      return 'Book Chapter is required when Work Type is Chapter.';
    }
    if (this.book.firstPage == null || !Number.isInteger(Number(this.book.firstPage)) || Number(this.book.firstPage) < 1) {
      return 'First Page must be a positive integer.';
    }
    if (this.book.lastPage == null || !Number.isInteger(Number(this.book.lastPage)) || Number(this.book.lastPage) < 1) {
      return 'Last Page must be a positive integer.';
    }
    if (Number(this.book.lastPage) < Number(this.book.firstPage)) {
      return 'Last Page must be greater than or equal to First Page.';
    }
    const maxYear = new Date().getFullYear() + 1;
    if (this.book.year == null || !Number.isInteger(Number(this.book.year))
      || Number(this.book.year) < 1900 || Number(this.book.year) > maxYear) {
      return `Year must be a valid year between 1900 and ${maxYear}.`;
    }
    return null;
  }

  buildPayload(): BookProductDTO {
    const participantes = this.participants.map((p, index) => ({
      rrhhId: p.rrhhId,
      tipoParticipacionId: p.participationTypeId,
      orden: p.order || index + 1,
      corresponding: p.corresponding || false
    }));

    const year = Number(this.book.year);
    return {
      ...this.book,
      tipoProducto: { id: 20 },
      idBookType: this.selectedBookTypeId!,
      descripcion: this.book.descripcion?.trim(),
      chapterTitle: this.isChapterSelected ? (this.book.chapterTitle?.trim() || null) : null,
      firstPage: Number(this.book.firstPage),
      lastPage: Number(this.book.lastPage),
      editorialCityCountry: this.book.editorialCityCountry?.trim() || null,
      year,
      isbn: this.book.isbn?.trim() || null,
      fechaInicio: `${year}-01-01`,
      cluster: this.selectedClusters.join(','),
      basal: this.isBasal ? 'S' : 'N',
      participantes
    };
  }

  saveBook(): void {
    const error = this.validateForm();
    if (error) {
      this.messageService.error(error);
      return;
    }

    const payload = this.buildPayload();
    this.saving = true;
    const request = this.isEditMode && this.bookId
      ? this.bookService.updateBook(this.bookId, payload)
      : this.bookService.createBook(payload);

    request.pipe(
      catchError(err => {
        const msg = err?.error?.message || 'Could not save book.';
        this.messageService.error(msg);
        return of(null);
      }),
      finalize(() => this.saving = false)
    ).subscribe(result => {
      if (result) {
        this.messageService.success(this.isEditMode ? 'Book updated successfully.' : 'Book created successfully.');
        this.goBack();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/books']);
  }
}
