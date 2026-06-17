import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { environment } from '../../../../environments/environment';
import {
  NewsItem,
  NewsListFilters,
  PUBLICATION_STATUS,
  PublicationStatus,
  TRANSLATION_STATUS,
  TranslationStatus,
  createEmptyNewsItem
} from '../models/news.models';
import { NewsEditorialMetadataService } from './news-editorial-metadata.service';
import { NewsTranslationService } from './news-translation.service';
import { NewsTaxonomyService } from './news-taxonomy.service';
import { NewsTagsCatalogService } from './news-tags-catalog.service';
import {
  NoticiaApiDTO,
  PaginatedNewsApi,
  mapNewsItemToApiPayload,
  mapPublicationStatusToEstadoId,
  mergeApiWithMetadata,
  extractMetadataFromNewsItem
} from './news-api.mapper';
import { MOCK_NEWS_ITEMS } from './news-mock.data';
import {
  normalizeMediaPathsInHtml,
  resolveMediaPathsInHtml
} from './news-media-html.util';
import {
  buildUploadedVideoHtml as buildUploadedVideoMarkup,
  buildVideoEmbedHtml,
  normalizeVideoEmbedUrl
} from './news-video.util';

export interface NewsListResponse {
  content: NewsItem[];
  totalElements: number;
}

/**
 * Set to true to use in-memory mock data instead of the REST API.
 */
const USE_MOCK_DATA = false;

@Injectable({
  providedIn: 'root'
})
export class NewsService {
  private mockStore: NewsItem[] = [...MOCK_NEWS_ITEMS];

  constructor(
    private baseHttp: BaseHttpService,
    private metadataService: NewsEditorialMetadataService,
    private translationService: NewsTranslationService,
    private taxonomyService: NewsTaxonomyService,
    private tagsCatalogService: NewsTagsCatalogService
  ) {}

  getNewsList(filters: NewsListFilters = {}): Observable<NewsListResponse> {
    if (USE_MOCK_DATA) {
      return of(this.filterMockList(filters));
    }

    const params: Record<string, string | number> = {
      page: filters.page ?? 0,
      size: filters.size ?? 1000,
      sortBy: filters.sort ?? 'id',
      sortDir: (filters.direction ?? 'DESC').toLowerCase()
    };
    if (filters.publicationStatus) {
      params['estadoId'] = mapPublicationStatusToEstadoId(filters.publicationStatus as PublicationStatus);
    }
    if (filters.tagId) params['tagId'] = filters.tagId;
    if (filters.categoryId) params['categoryId'] = filters.categoryId;
    if (filters.fromDate) params['fromDate'] = filters.fromDate;
    if (filters.toDate) params['toDate'] = filters.toDate;
    if (filters.title) params['title'] = filters.title;

    return this.baseHttp.get<PaginatedNewsApi>('/news', params).pipe(
      map(res => ({
        content: (res.content || []).map(dto =>
          mergeApiWithMetadata(dto, this.metadataService.get(dto.id!))
        ),
        totalElements: res.totalElements ?? 0
      })),
      catchError(() => of(this.filterMockList(filters)))
    );
  }

  getNewsById(id: number): Observable<NewsItem> {
    if (USE_MOCK_DATA) {
      const item = this.mockStore.find(n => n.id === id);
      return item ? of({ ...item }) : throwError(() => new Error('Not found'));
    }

    return this.baseHttp.get<NoticiaApiDTO>(`/news/${id}`).pipe(
      map(dto => mergeApiWithMetadata(dto, this.metadataService.get(id)))
    );
  }

  createNews(payload: NewsItem): Observable<NewsItem> {
    if (USE_MOCK_DATA) {
      const created = { ...payload, id: Date.now(), createdAt: new Date().toISOString() };
      this.mockStore.unshift(created);
      return of(created);
    }

    const apiPayload = mapNewsItemToApiPayload(payload);
    return this.baseHttp.post<NoticiaApiDTO>('/news', apiPayload).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, payload))
    );
  }

  updateNews(id: number, payload: NewsItem): Observable<NewsItem> {
    if (USE_MOCK_DATA) {
      const idx = this.mockStore.findIndex(n => n.id === id);
      if (idx < 0) return throwError(() => new Error('Not found'));
      this.mockStore[idx] = { ...payload, id, updatedAt: new Date().toISOString() };
      return of(this.mockStore[idx]);
    }

    const apiPayload = mapNewsItemToApiPayload(payload);
    return this.baseHttp.put<NoticiaApiDTO>(`/news/${id}`, apiPayload).pipe(
      switchMap(dto => this.persistMetadataAndMerge(dto, payload))
    );
  }

  deleteNews(id: number): Observable<void> {
    if (USE_MOCK_DATA) {
      this.mockStore = this.mockStore.filter(n => n.id !== id);
      return of(undefined);
    }
    return this.baseHttp.delete<void>(`/news/${id}`).pipe(
      tap(() => this.metadataService.remove(id))
    );
  }

  publishNews(id: number, item: NewsItem): Observable<NewsItem> {
    return this.updateNews(id, { ...item, publicationStatus: PUBLICATION_STATUS.PUBLISHED });
  }

  unpublishNews(id: number, item: NewsItem): Observable<NewsItem> {
    return this.updateNews(id, { ...item, publicationStatus: PUBLICATION_STATUS.UNPUBLISHED });
  }

  duplicateNews(id: number): Observable<NewsItem> {
    return this.getNewsById(id).pipe(
      switchMap(source => {
        const copy = createEmptyNewsItem();
        Object.assign(copy, {
          ...source,
          id: undefined,
          titleEs: `${source.titleEs} (copy)`,
          publicationStatus: PUBLICATION_STATUS.DRAFT,
          translationStatus: source.translationStatus,
          publicationDate: null,
          publishedAt: null,
          slug: source.slug ? `${source.slug}-copy` : '',
          numVisitas: 0,
          numLikes: 0,
          basal: 'N'
        });
        return this.createNews(copy);
      })
    );
  }

  generateTranslation(item: NewsItem): Observable<NewsItem> {
    return this.translationService.translateNewsContent({
      titleEs: item.titleEs,
      summaryEs: item.summaryEs,
      bodyEs: item.bodyEs
    }).pipe(
      map(result => ({
        ...item,
        titleEn: result.titleEn,
        summaryEn: result.summaryEn,
        bodyEn: result.bodyEn,
        translationStatus: TRANSLATION_STATUS.AUTO_GENERATED as TranslationStatus
      }))
    );
  }

  validateTranslation(item: NewsItem): NewsItem {
    return {
      ...item,
      translationStatus: TRANSLATION_STATUS.VALIDATED,
      translationValidatedAt: new Date().toISOString()
    };
  }

  /** Loads the full tag catalog once (cached). Suitable for ~200 tags with local filtering. */
  getTags(): Observable<{ id: string; label: string; slug?: string }[]> {
    return this.tagsCatalogService.loadCatalog(USE_MOCK_DATA);
  }

  getCategories() {
    return this.taxonomyService.getCategories(USE_MOCK_DATA);
  }

  createCategory(name: string) {
    return this.taxonomyService.createCategory(name, USE_MOCK_DATA);
  }

  createTag(name: string): Observable<{ id: string; label: string; slug?: string }> {
    if (USE_MOCK_DATA) {
      return this.taxonomyService.createTag(name, []);
    }
    return this.taxonomyService.createTagApi(name).pipe(
      tap(created => this.tagsCatalogService.appendToCache(created))
    );
  }

  /** Imagen principal de la noticia → media/{archivo} */
  uploadImage(file: File): Observable<string> {
    return this.baseHttp.uploadFile<{ image: string }>('/files/upload-media', file).pipe(
      map(res => res.image)
    );
  }

  /** Imágenes/videos del cuerpo (rich text) → media/IMAGEN/{archivo} */
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

  /** Uploaded video files for news body rich text. */
  uploadNewsVideo(file: File): Observable<string> {
    return this.uploadBodyMedia(file);
  }

  /** Normalizes a provider URL into a safe embed iframe URL. */
  normalizeVideoEmbedUrl(url: string) {
    return normalizeVideoEmbedUrl(url);
  }

  /** Builds sanitized embed HTML for insertion into the news body. */
  insertVideoEmbed(url: string): string | null {
    const result = normalizeVideoEmbedUrl(url);
    if (!result.embedUrl || !result.originalUrl) {
      return null;
    }
    return buildVideoEmbedHtml(result.embedUrl, result.originalUrl);
  }

  /** Builds sanitized uploaded-video HTML for insertion into the news body. */
  buildUploadedVideoHtml(relativePath: string, mimeType: string): string {
    return buildUploadedVideoMarkup(relativePath, mimeType);
  }

  resolveMediaUrl(path: string | null | undefined): string | null {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    const base = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${base}/${path.replace(/^\//, '')}`;
  }

  /** HTML guardado con rutas relativas media/... */
  normalizeBodyHtmlForStorage(html: string): string {
    return normalizeMediaPathsInHtml(html, environment.apiUrl);
  }

  /** HTML con URLs públicas para mostrar en editor o vista previa */
  resolveBodyHtmlForDisplay(html: string): string {
    return resolveMediaPathsInHtml(html, p => this.resolveMediaUrl(p) || p);
  }

  private persistMetadataAndMerge(dto: NoticiaApiDTO, payload: NewsItem): Observable<NewsItem> {
    if (!dto.id) {
      return throwError(() => new Error('News id missing from API response'));
    }
    const meta = extractMetadataFromNewsItem(payload);
    this.metadataService.save(dto.id, meta);
    return of(mergeApiWithMetadata(dto, meta));
  }

  private filterMockList(filters: NewsListFilters): NewsListResponse {
    let items = [...this.mockStore];
    if (filters.publicationStatus) {
      items = items.filter(i => i.publicationStatus === filters.publicationStatus);
    }
    if (filters.tagId) {
      items = items.filter(i => i.tags.some(t => t.id === filters.tagId));
    }
    if (filters.title) {
      const q = filters.title.toLowerCase();
      items = items.filter(i =>
        i.titleEs.toLowerCase().includes(q) ||
        i.titleEn.toLowerCase().includes(q)
      );
    }
    return { content: items, totalElements: items.length };
  }
}
