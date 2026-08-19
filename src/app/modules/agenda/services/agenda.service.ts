import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { environment } from '../../../../environments/environment';
import {
  AgendaEvent,
  AgendaListFilters,
  PUBLICATION_STATUS,
  PublicationStatus,
  TRANSLATION_STATUS,
  TranslationStatus,
  createEmptyAgendaEvent
} from '../models/agenda.models';
import { NewsCategory } from '../../news/models/news.models';
import { AgendaEditorialMetadataService } from './agenda-editorial-metadata.service';
import { AgendaTranslationService } from './agenda-translation.service';
import {
  AgendaApiDTO,
  PaginatedAgendaApi,
  extractMetadataFromAgendaEvent,
  mapAgendaEventToApiPayload,
  mapPublicationStatusToEstadoId,
  mergeApiWithMetadata
} from './agenda-api.mapper';
import { MOCK_AGENDA_EVENTS } from './agenda-mock.data';
import {
  normalizeMediaPathsInHtml,
  resolveMediaPathsInHtml
} from '../../news/services/news-media-html.util';

export interface AgendaListResponse {
  content: AgendaEvent[];
  totalElements: number;
}

/**
 * Set to true to use in-memory mock data instead of the REST API.
 */
const USE_MOCK_DATA = false;

@Injectable({
  providedIn: 'root'
})
export class AgendaService {
  private mockStore: AgendaEvent[] = [...MOCK_AGENDA_EVENTS];

  constructor(
    private baseHttp: BaseHttpService,
    private metadataService: AgendaEditorialMetadataService,
    private translationService: AgendaTranslationService
  ) {}

  getAgendaList(filters: AgendaListFilters = {}): Observable<AgendaListResponse> {
    if (USE_MOCK_DATA) {
      return of(this.filterMockList(filters));
    }

    const params: Record<string, string | number> = {
      page: filters.page ?? 0,
      size: filters.size ?? 1000,
      sortBy: filters.sort ?? 'fechaInicio',
      sortDir: (filters.direction ?? 'DESC').toLowerCase()
    };
    if (filters.publicationStatus) {
      params['estadoId'] = mapPublicationStatusToEstadoId(filters.publicationStatus as PublicationStatus);
    }
    if (filters.fromDate) params['fromDate'] = filters.fromDate;
    if (filters.toDate) params['toDate'] = filters.toDate;
    if (filters.title) params['title'] = filters.title;
    if (filters.location) params['location'] = filters.location;

    return this.baseHttp.get<PaginatedAgendaApi>('/agenda', params).pipe(
      map(res => ({
        content: (res.content || []).map(dto =>
          mergeApiWithMetadata(dto, this.metadataService.get(dto.id!))
        ),
        totalElements: res.totalElements ?? 0
      })),
      catchError(err => throwError(() => err))
    );
  }

  getAgendaById(id: number): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      const item = this.mockStore.find(e => e.id === id);
      return item ? of({ ...item }) : throwError(() => new Error('Not found'));
    }

    return this.baseHttp.get<AgendaApiDTO>(`/agenda/${id}`).pipe(
      map(dto => mergeApiWithMetadata(dto, this.metadataService.get(id)))
    );
  }

  createAgenda(payload: AgendaEvent): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      const created = { ...payload, id: Date.now(), createdAt: new Date().toISOString() };
      this.mockStore.unshift(created);
      return of(created);
    }

    const apiPayload = mapAgendaEventToApiPayload(payload);
    return this.baseHttp.post<AgendaApiDTO>('/agenda', apiPayload).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, payload))
    );
  }

  updateAgenda(id: number, payload: AgendaEvent): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      const idx = this.mockStore.findIndex(e => e.id === id);
      if (idx < 0) return throwError(() => new Error('Not found'));
      this.mockStore[idx] = { ...payload, id, updatedAt: new Date().toISOString() };
      return of(this.mockStore[idx]);
    }

    const apiPayload = mapAgendaEventToApiPayload(payload);
    return this.baseHttp.put<AgendaApiDTO>(`/agenda/${id}`, apiPayload).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, payload))
    );
  }

  deleteAgenda(id: number): Observable<void> {
    if (USE_MOCK_DATA) {
      this.mockStore = this.mockStore.filter(e => e.id !== id);
      return of(undefined);
    }
    return this.baseHttp.delete<void>(`/agenda/${id}`).pipe(
      tap(() => this.metadataService.remove(id))
    );
  }

  publishAgenda(id: number, item: AgendaEvent): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      return this.updateAgenda(id, { ...item, publicationStatus: PUBLICATION_STATUS.PUBLISHED });
    }
    return this.baseHttp.post<AgendaApiDTO>(`/agenda/${id}/publish`, {}).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, { ...item, publicationStatus: PUBLICATION_STATUS.PUBLISHED }))
    );
  }

  unpublishAgenda(id: number, item: AgendaEvent): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      return this.updateAgenda(id, { ...item, publicationStatus: PUBLICATION_STATUS.UNPUBLISHED });
    }
    return this.baseHttp.post<AgendaApiDTO>(`/agenda/${id}/unpublish`, {}).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, { ...item, publicationStatus: PUBLICATION_STATUS.UNPUBLISHED }))
    );
  }

  duplicateAgenda(id: number): Observable<AgendaEvent> {
    if (USE_MOCK_DATA) {
      return this.getAgendaById(id).pipe(
        switchMap(source => {
          const copy = createEmptyAgendaEvent();
          Object.assign(copy, {
            ...source,
            id: undefined,
            titleEs: `${source.titleEs} (copy)`,
            publicationStatus: PUBLICATION_STATUS.DRAFT,
            translationStatus: source.translationStatus,
            publishedAt: null,
            slug: source.slug ? `${source.slug}-copy` : ''
          });
          return this.createAgenda(copy);
        })
      );
    }
    return this.baseHttp.post<AgendaApiDTO>(`/agenda/${id}/duplicate`, {}).pipe(
      map(dto => mergeApiWithMetadata(dto, this.metadataService.get(dto.id!)))
    );
  }

  generateTranslation(
    item: AgendaEvent,
    direction: 'es_to_en' | 'en_to_es' = 'es_to_en'
  ): Observable<AgendaEvent> {
    if (direction === 'en_to_es') {
      return this.translationService.translateAgendaContent(direction, {
        titleEn: item.titleEn,
        descriptionEn: item.descriptionEn
      }).pipe(
        map(result => ({
          ...item,
          titleEs: result.titleEs ?? item.titleEs,
          descriptionEs: result.descriptionEs ?? item.descriptionEs,
          translationStatus: TRANSLATION_STATUS.AUTO_GENERATED as TranslationStatus
        }))
      );
    }
    return this.translationService.translateAgendaContent(direction, {
      titleEs: item.titleEs,
      descriptionEs: item.descriptionEs
    }).pipe(
      map(result => ({
        ...item,
        titleEn: result.titleEn ?? item.titleEn,
        descriptionEn: result.descriptionEn ?? item.descriptionEn,
        translationStatus: TRANSLATION_STATUS.AUTO_GENERATED as TranslationStatus
      }))
    );
  }

  validateTranslation(item: AgendaEvent): AgendaEvent {
    return {
      ...item,
      translationStatus: TRANSLATION_STATUS.VALIDATED,
      translationValidatedAt: new Date().toISOString()
    };
  }

  getCategories(): Observable<NewsCategory[]> {
    if (USE_MOCK_DATA) {
      return of([
        { id: 'cat-research', label: 'Research', slug: 'research' },
        { id: 'cat-events', label: 'Events', slug: 'events' }
      ]);
    }
    return this.baseHttp.get<NewsCategory[]>('/agenda/categories');
  }

  createCategory(name: string): Observable<NewsCategory> {
    const label = name.trim();
    if (!label) {
      return throwError(() => new Error('Category name is required'));
    }
    if (USE_MOCK_DATA) {
      return of({ id: `cat-${Date.now()}`, label, slug: label.toLowerCase().replace(/\s+/g, '-') });
    }
    return this.baseHttp.post<NewsCategory>('/agenda/categories', { label });
  }

  uploadImage(file: File): Observable<string> {
    return this.baseHttp.uploadFile<{ image: string }>('/files/upload-media', file).pipe(
      map(res => res.image)
    );
  }

  uploadBodyMedia(file: File): Observable<string> {
    return this.baseHttp.uploadFile<{ image: string }>(
      '/files/upload-media',
      file,
      'file',
      { subdir: 'IMAGEN' }
    ).pipe(
      map(res => res.image)
    );
  }

  resolveMediaUrl(path: string | null | undefined): string | null {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    const base = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${base}/${path.replace(/^\//, '')}`;
  }

  normalizeBodyHtmlForStorage(html: string): string {
    return normalizeMediaPathsInHtml(html, environment.apiUrl);
  }

  resolveBodyHtmlForDisplay(html: string): string {
    return resolveMediaPathsInHtml(html, p => this.resolveMediaUrl(p) || p);
  }

  private persistMetadataAndMerge(dto: AgendaApiDTO, payload: AgendaEvent): Observable<AgendaEvent> {
    if (!dto.id) {
      return throwError(() => new Error('Agenda id missing from API response'));
    }
    const meta = extractMetadataFromAgendaEvent(payload);
    this.metadataService.save(dto.id, meta);
    return of(mergeApiWithMetadata(dto, meta));
  }

  private filterMockList(filters: AgendaListFilters): AgendaListResponse {
    let items = [...this.mockStore];

    if (filters.publicationStatus) {
      items = items.filter(i => i.publicationStatus === filters.publicationStatus);
    }
    if (filters.categoryId) {
      items = items.filter(i => i.categories.some(c => c.id === filters.categoryId));
    }
    if (filters.eventMode) {
      items = items.filter(i => i.eventMode === filters.eventMode);
    }
    if (filters.fromDate) {
      items = items.filter(i => i.eventDate && i.eventDate >= filters.fromDate!);
    }
    if (filters.toDate) {
      items = items.filter(i => i.eventDate && i.eventDate <= filters.toDate!);
    }
    if (filters.title) {
      const q = filters.title.toLowerCase();
      items = items.filter(i =>
        i.titleEs.toLowerCase().includes(q) ||
        i.titleEn.toLowerCase().includes(q)
      );
    }
    if (filters.location) {
      const q = filters.location.toLowerCase();
      items = items.filter(i => (i.location || '').toLowerCase().includes(q));
    }

    return { content: items, totalElements: items.length };
  }
}
