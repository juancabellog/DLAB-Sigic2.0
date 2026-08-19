import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { Subscription } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { formatLocalDate } from '../../../core/utils/date.util';
import { NewsRowActionsComponent } from './news-row-actions.component';
import { MessageService } from '../../../core/services/message.service';
import { ViewMode, ViewModeService } from '../../../core/services/view-mode.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { NewsService } from '../services/news.service';
import {
  NewsItem,
  NewsTag,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  PublicationStatus,
} from '../models/news.models';

export type NewsSortColumn = 'title' | 'tags' | 'date' | 'status';

@Component({
  selector: 'app-news-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ListControlsComponent,
    NewsRowActionsComponent
  ],
  templateUrl: './news-list.component.html',
  styleUrls: ['./news-list.component.scss']
})
export class NewsListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  newsItems: NewsItem[] = [];
  filteredNews: NewsItem[] = [];
  searchResults: NewsItem[] = [];
  availableTags: NewsTag[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  filterPublicationStatus: PublicationStatus | '' = '';
  filterTagId = '';
  filterFromDate = '';
  filterToDate = '';

  sortColumn: NewsSortColumn = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  readonly publicationStatuses = Object.values(PUBLICATION_STATUS);
  readonly publicationStatusLabels = PUBLICATION_STATUS_LABELS;
  /** Máximo de chips de tag visibles en lista; el resto va en +N con tooltip */
  readonly maxVisibleTags = 2;

  private viewModeSubscription?: Subscription;

  constructor(
    private newsService: NewsService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    const state = this.listStateService.getState('news');
    if (state.searchTerm) {
      this.isSearching = true;
    }
    this.loadTags();
    this.loadNews();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadTags(): void {
    this.newsService.getTags().subscribe(tags => {
      this.availableTags = tags;
    });
  }

  loadNews(): void {
    this.loading = true;
    this.newsService.getNewsList({ page: 0, size: 10000, sort: 'fechaTermino', direction: 'DESC' }).pipe(
      catchError(() => {
        this.messageService.error('Error loading news. Please try again later.');
        return of({ content: [], totalElements: 0 });
      }),
      finalize(() => { this.loading = false; })
    ).subscribe(res => {
      this.newsItems = res.content || [];
      this.searchResults = [...this.newsItems];
      this.applyFilters();
    });
  }

  onSearchResults(results: NewsItem[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('news', { searchTerm: term });
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  onFiltersChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.filterPublicationStatus = '';
    this.filterTagId = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.applyFilters();
  }

  onSort(column: NewsSortColumn): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  getTitle(item: NewsItem): string {
    return item.titleEs || item.titleEn || 'Untitled';
  }

  getImageUrl(item: NewsItem): string | null {
    return this.newsService.resolveMediaUrl(item.mainImageUrl);
  }

  getNewsMetaLine(item: NewsItem): string {
    const author = item.author?.trim() || '—';
    const date = this.formatDisplayDate(item.publicationDate);
    return date ? `${author} · ${date}` : author;
  }

  formatDisplayDate(date: string | null | undefined): string {
    return formatLocalDate(date, 'dd MMM yyyy', 'en-GB');
  }

  hasTags(item: NewsItem): boolean {
    return (item.tags?.length ?? 0) > 0;
  }

  getVisibleTags(item: NewsItem): NewsTag[] {
    return (item.tags ?? []).slice(0, this.maxVisibleTags);
  }

  getHiddenTagsCount(item: NewsItem): number {
    const total = item.tags?.length ?? 0;
    return Math.max(0, total - this.maxVisibleTags);
  }

  getAllTagsTooltip(item: NewsItem): string {
    if (!this.hasTags(item)) return '';
    return item.tags.map(t => t.label).join(', ');
  }

  isPublished(item: NewsItem): boolean {
    return item.publicationStatus === PUBLICATION_STATUS.PUBLISHED;
  }

  getPublicationStatusColor(status: PublicationStatus): 'primary' | 'accent' | 'warn' {
    switch (status) {
      case PUBLICATION_STATUS.PUBLISHED: return 'primary';
      case PUBLICATION_STATUS.UNPUBLISHED: return 'accent';
      default: return 'warn';
    }
  }

  viewNews(item: NewsItem): void {
    if (item.id) this.router.navigate(['/news', item.id, 'view']);
  }

  editNews(item: NewsItem): void {
    if (item.id) this.router.navigate(['/news', item.id, 'edit']);
  }

  duplicateNews(item: NewsItem): void {
    if (!item.id) return;
    this.newsService.duplicateNews(item.id).subscribe({
      next: () => {
        this.messageService.success('News duplicated', 'A draft copy was created.');
        this.loadNews();
      },
      error: () => this.messageService.error('Error duplicating news.')
    });
  }

  previewOnWebsite(item: NewsItem): void {
    const url = item.mediaLink?.trim() || item.publicUrl || (item.slug ? `https://example.org/news/${item.slug}` : null);
    if (url) {
      window.open(url, '_blank');
    } else {
      this.messageService.info('No public URL configured for this news item.');
    }
  }

  publishNews(item: NewsItem): void {
    if (!item.id) return;
    this.newsService.publishNews(item.id, item).subscribe({
      next: () => {
        this.messageService.success('Published', 'News item is now published.');
        this.loadNews();
      },
      error: () => this.messageService.error('Error publishing news.')
    });
  }

  unpublishNews(item: NewsItem): void {
    if (!item.id) return;
    this.newsService.unpublishNews(item.id, item).subscribe({
      next: () => {
        this.messageService.success('Unpublished', 'News item was unpublished.');
        this.loadNews();
      },
      error: () => this.messageService.error('Error unpublishing news.')
    });
  }

  deleteNews(item: NewsItem): void {
    if (!item.id) return;
    this.messageService.confirm(
      `Delete "${this.getTitle(item)}"?`,
      accepted => {
        if (!accepted) return;
        this.newsService.deleteNews(item.id!).subscribe({
          next: () => {
            this.messageService.success('Deleted', 'News item removed.');
            this.loadNews();
          },
          error: () => this.messageService.error('Error deleting news.')
        });
      }
    );
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];

    if (this.filterPublicationStatus) {
      filtered = filtered.filter(n => n.publicationStatus === this.filterPublicationStatus);
    }
    if (this.filterTagId) {
      filtered = filtered.filter(n => n.tags.some(t => t.id === this.filterTagId));
    }
    if (this.filterFromDate) {
      filtered = filtered.filter(n => n.publicationDate && n.publicationDate >= this.filterFromDate);
    }
    if (this.filterToDate) {
      filtered = filtered.filter(n => n.publicationDate && n.publicationDate <= this.filterToDate);
    }

    filtered.sort((a, b) => this.compareNews(a, b));

    this.filteredNews = filtered;
    this.finalFilteredCount = filtered.length;
  }

  private compareNews(a: NewsItem, b: NewsItem): number {
    let valueA: string | number | null = null;
    let valueB: string | number | null = null;

    switch (this.sortColumn) {
      case 'title':
        valueA = this.getTitle(a).toLowerCase();
        valueB = this.getTitle(b).toLowerCase();
        break;
      case 'tags':
        valueA = (a.tags ?? []).map(t => t.label).join(', ').toLowerCase();
        valueB = (b.tags ?? []).map(t => t.label).join(', ').toLowerCase();
        break;
      case 'date':
        valueA = a.publicationDate ? new Date(a.publicationDate).getTime() : 0;
        valueB = b.publicationDate ? new Date(b.publicationDate).getTime() : 0;
        break;
      case 'status':
        valueA = this.publicationStatusLabels[a.publicationStatus].toLowerCase();
        valueB = this.publicationStatusLabels[b.publicationStatus].toLowerCase();
        break;
    }

    if (valueA == null && valueB == null) return 0;
    if (valueA == null) return this.sortDirection === 'asc' ? -1 : 1;
    if (valueB == null) return this.sortDirection === 'asc' ? 1 : -1;

    let compareResult: number;
    if (typeof valueA === 'number' && typeof valueB === 'number') {
      compareResult = valueA - valueB;
    } else {
      compareResult = String(valueA).localeCompare(String(valueB));
    }

    return this.sortDirection === 'asc' ? compareResult : -compareResult;
  }
}
