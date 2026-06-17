import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { NewsCategory, NewsTag, slugify } from '../models/news.models';
import { hasDuplicateTerm, normalizeTerm, termsMatch } from '../utils/news-taxonomy.util';

const LOCAL_TAGS_STORAGE_KEY = 'sisgic_news_local_tags';

const MOCK_CATEGORIES: NewsCategory[] = [
  { id: 'cat-research', label: 'Research', slug: 'research' },
  { id: 'cat-events', label: 'Events', slug: 'events' },
  { id: 'cat-outreach', label: 'Outreach', slug: 'outreach' },
  { id: 'cat-awards', label: 'Awards', slug: 'awards' }
];

@Injectable({
  providedIn: 'root'
})
export class NewsTaxonomyService {
  constructor(private baseHttp: BaseHttpService) {}

  getCategories(useMockApi: boolean): Observable<NewsCategory[]> {
    if (useMockApi) {
      return of([...MOCK_CATEGORIES].sort((a, b) => a.label.localeCompare(b.label)));
    }
    return this.baseHttp.get<NewsCategory[]>('/news/categories');
  }

  createCategory(name: string, useMockApi: boolean): Observable<NewsCategory> {
    const label = name.trim();
    if (!label) {
      return of({ id: '', label: '', slug: '' });
    }

    if (useMockApi) {
      const existing = MOCK_CATEGORIES.find(c => termsMatch(c.label, label));
      if (existing) {
        return of(existing);
      }
      const category: NewsCategory = {
        id: `cat-${slugify(label)}-${Date.now()}`,
        label,
        slug: slugify(label)
      };
      MOCK_CATEGORIES.push(category);
      return of(category);
    }

    return this.baseHttp.post<NewsCategory>('/news/categories', { label });
  }

  getTags(useMockApi: boolean): Observable<NewsTag[]> {
    const local = this.loadLocalTags();
    if (useMockApi) {
      const mock: NewsTag[] = [
        { id: 'tag-1', label: 'Research', slug: 'research' },
        { id: 'tag-2', label: 'Events', slug: 'events' },
        { id: 'tag-3', label: 'Neurobiología del Envejecimiento', slug: 'neurobiologia-del-envejecimiento' }
      ];
      return of(this.mergeTags(mock, local));
    }

    return this.baseHttp.get<NewsTag[]>('/news/tags').pipe(
      map(apiTags => this.mergeTags(apiTags || [], local))
    );
  }

  createTagApi(label: string): Observable<NewsTag> {
    return this.baseHttp.post<NewsTag>('/news/tags', { label });
  }

  mergeTagLists(primary: NewsTag[], secondary: NewsTag[]): NewsTag[] {
    return this.mergeTags(primary, secondary);
  }

  createTag(name: string, existingCatalog: NewsTag[] = []): Observable<NewsTag> {
    const label = name.trim();
    if (!label) {
      return of({ id: '', label: '', slug: '' });
    }

    const merged = this.mergeTags(existingCatalog, this.loadLocalTags());
    const found = merged.find(t => termsMatch(t.label, label));
    if (found) {
      return of(found);
    }

    const tag: NewsTag = {
      id: `tag-${slugify(label)}-${Date.now()}`,
      label,
      slug: slugify(label)
    };
    const local = this.loadLocalTags();
    if (!hasDuplicateTerm(local.map(t => t.label), label)) {
      this.saveLocalTags([...local, tag]);
    }
    return of(tag);
  }

  private mergeTags(primary: NewsTag[], secondary: NewsTag[]): NewsTag[] {
    const byNorm = new Map<string, NewsTag>();
    [...primary, ...secondary].forEach(tag => {
      const key = normalizeTerm(tag.label || tag.id);
      if (!byNorm.has(key)) {
        byNorm.set(key, tag);
      }
    });
    return Array.from(byNorm.values()).sort((a, b) => a.label.localeCompare(b.label));
  }

  private loadLocalTags(): NewsTag[] {
    try {
      const raw = localStorage.getItem(LOCAL_TAGS_STORAGE_KEY);
      return raw ? JSON.parse(raw) as NewsTag[] : [];
    } catch {
      return [];
    }
  }

  private saveLocalTags(tags: NewsTag[]): void {
    localStorage.setItem(LOCAL_TAGS_STORAGE_KEY, JSON.stringify(tags));
  }
}
