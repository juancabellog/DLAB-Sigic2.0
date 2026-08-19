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
import { AgendaRowActionsComponent } from './agenda-row-actions.component';
import { MessageService } from '../../../core/services/message.service';
import { ViewMode, ViewModeService } from '../../../core/services/view-mode.service';
import { ListStateService } from '../../../core/services/list-state.service';
import { NewsCategory } from '../../news/models/news.models';
import { AgendaService } from '../services/agenda.service';
import {
  AgendaEvent,
  EVENT_MODE,
  EVENT_MODE_LABELS,
  EventMode,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  PublicationStatus,
  formatEventTimeRange,
  getPrimaryCategoryLabel,
  isPastEvent
} from '../models/agenda.models';

export type AgendaSortColumn = 'event' | 'category' | 'date' | 'time' | 'location' | 'status' | 'featured';

@Component({
  selector: 'app-agenda-list',
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
    AgendaRowActionsComponent
  ],
  templateUrl: './agenda-list.component.html',
  styleUrls: ['./agenda-list.component.scss']
})
export class AgendaListComponent implements OnInit, OnDestroy {
  viewMode: ViewMode = 'list';
  agendaItems: AgendaEvent[] = [];
  filteredAgenda: AgendaEvent[] = [];
  searchResults: AgendaEvent[] = [];
  availableCategories: NewsCategory[] = [];
  loading = false;
  isSearching = false;
  finalFilteredCount: number | null = null;

  filterPublicationStatus: PublicationStatus | '' = '';
  filterCategoryId = '';
  filterEventMode: EventMode | '' = '';
  filterFromDate = '';
  filterToDate = '';

  sortColumn: AgendaSortColumn = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  readonly publicationStatuses = Object.values(PUBLICATION_STATUS);
  readonly publicationStatusLabels = PUBLICATION_STATUS_LABELS;
  readonly eventModes = Object.values(EVENT_MODE);
  readonly eventModeLabels = EVENT_MODE_LABELS;

  private viewModeSubscription?: Subscription;

  constructor(
    private agendaService: AgendaService,
    private messageService: MessageService,
    private router: Router,
    private viewModeService: ViewModeService,
    private listStateService: ListStateService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewModeService.getCurrentViewMode();
    const state = this.listStateService.getState('agenda');
    if (state.searchTerm) {
      this.isSearching = true;
    }
    this.loadCategories();
    this.loadAgenda();
    this.viewModeSubscription = this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  ngOnDestroy(): void {
    this.viewModeSubscription?.unsubscribe();
  }

  loadCategories(): void {
    this.agendaService.getCategories().subscribe(categories => {
      this.availableCategories = categories;
    });
  }

  loadAgenda(): void {
    this.loading = true;
    this.agendaService.getAgendaList({ page: 0, size: 10000, sort: 'fechaInicio', direction: 'DESC' }).pipe(
      catchError(() => {
        this.messageService.error('Error loading agenda. Please try again later.');
        return of({ content: [], totalElements: 0 });
      }),
      finalize(() => { this.loading = false; })
    ).subscribe(res => {
      this.agendaItems = res.content || [];
      this.searchResults = [...this.agendaItems];
      this.applyFilters();
    });
  }

  onSearchResults(results: AgendaEvent[]): void {
    this.searchResults = results;
    this.applyFilters();
  }

  onSearchTermChange(term: string): void {
    this.isSearching = term.length > 0;
    this.listStateService.saveState('agenda', { searchTerm: term });
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
    this.filterCategoryId = '';
    this.filterEventMode = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.applyFilters();
  }

  onSort(column: AgendaSortColumn): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  getTitle(item: AgendaEvent): string {
    return item.titleEs || item.titleEn || 'Untitled';
  }

  getImageUrl(item: AgendaEvent): string | null {
    return this.agendaService.resolveMediaUrl(item.mainImageUrl);
  }

  getEventMetaLine(item: AgendaEvent): string {
    const author = item.author?.trim();
    if (author) return author;
    const mode = item.eventMode ? this.eventModeLabels[item.eventMode] : '';
    return mode || '—';
  }

  getCategoryLabel(item: AgendaEvent): string {
    return getPrimaryCategoryLabel(item.categories);
  }

  formatDisplayDate(date: string | null | undefined): string {
    return formatLocalDate(date, 'dd MMM yyyy', 'en-GB');
  }

  formatTime(item: AgendaEvent): string {
    return formatEventTimeRange(item.startTime, item.endTime);
  }

  getLocationDisplay(item: AgendaEvent): string {
    if (item.eventMode === EVENT_MODE.ONLINE) {
      return item.onlineUrl ? 'Online' : '—';
    }
    return item.location?.trim() || '—';
  }

  isPast(item: AgendaEvent): boolean {
    return isPastEvent(item.eventDate);
  }

  isPublished(item: AgendaEvent): boolean {
    return item.publicationStatus === PUBLICATION_STATUS.PUBLISHED;
  }

  getPublicationStatusColor(status: PublicationStatus): 'primary' | 'accent' | 'warn' {
    switch (status) {
      case PUBLICATION_STATUS.PUBLISHED: return 'primary';
      case PUBLICATION_STATUS.READY_TO_PUBLISH: return 'accent';
      case PUBLICATION_STATUS.UNPUBLISHED: return 'accent';
      default: return 'warn';
    }
  }

  viewEvent(item: AgendaEvent): void {
    if (item.id) this.router.navigate(['/agenda', item.id, 'view']);
  }

  editEvent(item: AgendaEvent): void {
    if (item.id) this.router.navigate(['/agenda', item.id, 'edit']);
  }

  duplicateEvent(item: AgendaEvent): void {
    if (!item.id) return;
    this.agendaService.duplicateAgenda(item.id).subscribe({
      next: () => {
        this.messageService.success('Event duplicated', 'A draft copy was created.');
        this.loadAgenda();
      },
      error: () => this.messageService.error('Error duplicating event.')
    });
  }

  previewOnWebsite(item: AgendaEvent): void {
    const url = item.publicUrl || (item.slug ? `https://example.org/agenda/${item.slug}` : null);
    if (url) {
      window.open(url, '_blank');
    } else {
      this.messageService.info('No public URL configured for this event.');
    }
  }

  publishEvent(item: AgendaEvent): void {
    if (!item.id) return;
    this.agendaService.publishAgenda(item.id, item).subscribe({
      next: () => {
        this.messageService.success('Published', 'Event is now published.');
        this.loadAgenda();
      },
      error: () => this.messageService.error('Error publishing event.')
    });
  }

  unpublishEvent(item: AgendaEvent): void {
    if (!item.id) return;
    this.agendaService.unpublishAgenda(item.id, item).subscribe({
      next: () => {
        this.messageService.success('Unpublished', 'Event was unpublished.');
        this.loadAgenda();
      },
      error: () => this.messageService.error('Error unpublishing event.')
    });
  }

  deleteEvent(item: AgendaEvent): void {
    if (!item.id) return;
    this.messageService.confirm(
      `Delete "${this.getTitle(item)}"?`,
      accepted => {
        if (!accepted) return;
        this.agendaService.deleteAgenda(item.id!).subscribe({
          next: () => {
            this.messageService.success('Deleted', 'Event removed.');
            this.loadAgenda();
          },
          error: () => this.messageService.error('Error deleting event.')
        });
      }
    );
  }

  private applyFilters(): void {
    let filtered = [...this.searchResults];

    if (this.filterPublicationStatus) {
      filtered = filtered.filter(e => e.publicationStatus === this.filterPublicationStatus);
    }
    if (this.filterCategoryId) {
      filtered = filtered.filter(e => e.categories.some(c => c.id === this.filterCategoryId));
    }
    if (this.filterEventMode) {
      filtered = filtered.filter(e => e.eventMode === this.filterEventMode);
    }
    if (this.filterFromDate) {
      filtered = filtered.filter(e => e.eventDate && e.eventDate >= this.filterFromDate);
    }
    if (this.filterToDate) {
      filtered = filtered.filter(e => e.eventDate && e.eventDate <= this.filterToDate);
    }

    filtered.sort((a, b) => this.compareEvents(a, b));

    this.filteredAgenda = filtered;
    this.finalFilteredCount = filtered.length;
  }

  private compareEvents(a: AgendaEvent, b: AgendaEvent): number {
    let valueA: string | number | boolean | null = null;
    let valueB: string | number | boolean | null = null;

    switch (this.sortColumn) {
      case 'event':
        valueA = this.getTitle(a).toLowerCase();
        valueB = this.getTitle(b).toLowerCase();
        break;
      case 'category':
        valueA = this.getCategoryLabel(a).toLowerCase();
        valueB = this.getCategoryLabel(b).toLowerCase();
        break;
      case 'date':
        valueA = a.eventDate ? new Date(a.eventDate).getTime() : 0;
        valueB = b.eventDate ? new Date(b.eventDate).getTime() : 0;
        break;
      case 'time':
        valueA = a.startTime || '';
        valueB = b.startTime || '';
        break;
      case 'location':
        valueA = this.getLocationDisplay(a).toLowerCase();
        valueB = this.getLocationDisplay(b).toLowerCase();
        break;
      case 'status':
        valueA = this.publicationStatusLabels[a.publicationStatus].toLowerCase();
        valueB = this.publicationStatusLabels[b.publicationStatus].toLowerCase();
        break;
      case 'featured':
        valueA = a.featured ? 1 : 0;
        valueB = b.featured ? 1 : 0;
        break;
    }

    if (valueA == null && valueB == null) return 0;
    if (valueA == null) return this.sortDirection === 'asc' ? -1 : 1;
    if (valueB == null) return this.sortDirection === 'asc' ? 1 : -1;

    let compareResult: number;
    if (typeof valueA === 'number' && typeof valueB === 'number') {
      compareResult = valueA - valueB;
    } else if (typeof valueA === 'boolean' && typeof valueB === 'boolean') {
      compareResult = Number(valueA) - Number(valueB);
    } else {
      compareResult = String(valueA).localeCompare(String(valueB));
    }

    return this.sortDirection === 'asc' ? compareResult : -compareResult;
  }
}
