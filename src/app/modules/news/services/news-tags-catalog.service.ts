import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { NewsTag } from '../models/news.models';
import { NewsTaxonomyService } from './news-taxonomy.service';

/**
 * In-memory tag catalog for the News form (~218 tags).
 * Fetches once, filters locally in the UI. Replace with remote search when the catalog grows.
 */
@Injectable({
  providedIn: 'root'
})
export class NewsTagsCatalogService {
  private catalog: NewsTag[] | null = null;
  private loadRequest: Observable<NewsTag[]> | null = null;

  constructor(private taxonomyService: NewsTaxonomyService) {}

  /** Loads the full catalog once; subsequent calls return the cached list. */
  loadCatalog(useMockApi: boolean): Observable<NewsTag[]> {
    if (this.catalog) {
      return of([...this.catalog]);
    }
    if (!this.loadRequest) {
      this.loadRequest = this.taxonomyService.getTags(useMockApi).pipe(
        tap(tags => { this.catalog = tags; }),
        shareReplay(1)
      );
    }
    return this.loadRequest;
  }

  getCachedCatalog(): NewsTag[] {
    return this.catalog ? [...this.catalog] : [];
  }

  appendToCache(tag: NewsTag): void {
    if (!tag?.id) return;
    const base = this.catalog || [];
    if (!base.some(t => t.id === tag.id)) {
      this.catalog = this.taxonomyService.mergeTagLists(base, [tag]);
    }
  }

  isLoaded(): boolean {
    return this.catalog !== null;
  }

  createTag(name: string): Observable<NewsTag> {
    const label = name.trim();
    if (!label) {
      return throwError(() => new Error('Tag name is required'));
    }
    return this.taxonomyService.createTagApi(label).pipe(
      tap(created => {
        if (created.id) {
          this.catalog = this.taxonomyService.mergeTagLists(this.catalog || [], [created]);
        }
      })
    );
  }

  /**
   * Future replacement hook — not used yet.
   * When the catalog grows, swap filterTagsLocally() calls for this method.
   */
  searchTagsRemote(_query: string): Observable<NewsTag[]> {
    return of([]);
  }
}
