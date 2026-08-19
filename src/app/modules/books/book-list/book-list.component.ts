import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewModeService, ViewMode } from '../../../core/services/view-mode.service';
import { MessageService } from '../../../core/services/message.service';
import { BookService } from '../../../core/services/book.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { BookProductDTO, BookTypeDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ListControlsComponent
  ],
  templateUrl: './book-list.component.html',
  styleUrls: ['./book-list.component.scss']
})
export class BookListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  books: BookProductDTO[] = [];
  filteredBooks: BookProductDTO[] = [];
  searchResults: BookProductDTO[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  bookTypes: BookTypeDTO[] = [];

  filterBookTypeId: number | null = null;
  filterYear: number | null = null;
  filterCluster: number | null = null;
  filterProgressReport: string | null = null;
  filterFromYear: number | null = null;
  filterToYear: number | null = null;

  clusterOptions = [
    { id: 1, label: 'Cluster I' },
    { id: 2, label: 'Cluster II' },
    { id: 3, label: 'Cluster III' },
    { id: 4, label: 'Cluster IV' },
    { id: 5, label: 'Cluster V' }
  ];

  private viewModeSubscription?: Subscription;

  constructor(
    private bookService: BookService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService,
    private baseHttp: BaseHttpService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    this.loadCatalogs();
    this.loadBooks();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadCatalogs(): void {
    this.baseHttp.get<BookTypeDTO[]>('/catalogs/book-types').subscribe({
      next: data => this.bookTypes = data || [],
      error: () => this.bookTypes = []
    });
  }

  loadBooks(): void {
    this.loading = true;
    this.bookService.getBooks({
      page: 0,
      size: 10000,
      sort: 'id',
      direction: 'DESC'
    }).pipe(
      catchError(error => {
        console.error('Error loading books', error);
        this.messageService.error('Error loading books. Please try again later.');
        return of({ content: [], totalElements: 0 } as any);
      }),
      finalize(() => this.loading = false)
    ).subscribe(response => {
      this.books = response.content || [];
      this.searchResults = [...this.books];
      this.applyFilters();
    });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onSearchResults(results: BookProductDTO[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('books', { searchTerm: term });
  }

  applyFilters(): void {
    let list = [...this.searchResults];

    if (this.filterBookTypeId != null) {
      list = list.filter(b => b.idBookType === this.filterBookTypeId
        || b.bookType?.id === this.filterBookTypeId);
    }
    if (this.filterYear != null) {
      list = list.filter(b => b.year === this.filterYear);
    }
    if (this.filterCluster != null) {
      list = list.filter(b => (b.cluster || '')
        .split(',')
        .map(s => s.trim())
        .includes(String(this.filterCluster)));
    }
    if (this.filterProgressReport) {
      list = list.filter(b => b.progressReport === this.filterProgressReport);
    }
    if (this.filterFromYear != null) {
      list = list.filter(b => b.year != null && b.year >= this.filterFromYear!);
    }
    if (this.filterToYear != null) {
      list = list.filter(b => b.year != null && b.year <= this.filterToYear!);
    }

    this.filteredBooks = list;
    this.finalFilteredCount = this.isSearching || this.hasActiveFilters()
      ? list.length
      : null;
  }

  hasActiveFilters(): boolean {
    return this.filterBookTypeId != null
      || this.filterYear != null
      || this.filterCluster != null
      || !!this.filterProgressReport
      || this.filterFromYear != null
      || this.filterToYear != null;
  }

  clearFilters(): void {
    this.filterBookTypeId = null;
    this.filterYear = null;
    this.filterCluster = null;
    this.filterProgressReport = null;
    this.filterFromYear = null;
    this.filterToYear = null;
    this.applyFilters();
  }

  getTitle(book: BookProductDTO): string {
    return book.descripcion?.trim() || 'Untitled book';
  }

  getWorkTypeLabel(book: BookProductDTO): string {
    return book.bookTypeLabel
      || book.bookType?.idDescripcion
      || '—';
  }

  getPages(book: BookProductDTO): string {
    if (book.firstPage == null || book.lastPage == null) {
      return '—';
    }
    return `${book.firstPage} - ${book.lastPage}`;
  }

  isChapter(book: BookProductDTO): boolean {
    return book.idBookType === 2 || book.bookType?.id === 2;
  }

  viewBook(book: BookProductDTO): void {
    if (book.id != null) {
      this.router.navigate(['/books', book.id, 'view']);
    }
  }

  editBook(book: BookProductDTO): void {
    if (book.id != null) {
      this.router.navigate(['/books', book.id, 'edit']);
    }
  }

  deleteBook(book: BookProductDTO): void {
    if (book.id == null) {
      return;
    }
    this.messageService.confirm(
      `Are you sure you want to delete "${this.getTitle(book)}"?`,
      (accepted: boolean) => {
        if (!accepted) {
          return;
        }
        this.bookService.deleteBook(book.id!).subscribe({
          next: () => {
            this.messageService.success('Book deleted.');
            this.loadBooks();
          },
          error: () => this.messageService.error('Could not delete book.')
        });
      },
      'Delete book'
    );
  }
}
