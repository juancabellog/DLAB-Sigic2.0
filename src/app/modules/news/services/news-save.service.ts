import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { NewsCategory, NewsItem, NewsTag } from '../models/news.models';
import { isPendingTaxonomyItem } from '../utils/news-taxonomy.util';
import { NewsService } from './news.service';

@Injectable({
  providedIn: 'root'
})
export class NewsSaveService {

  constructor(private newsService: NewsService) {}

  /**
   * Creates pending tags/categories in the backend, then saves the News item.
   * Aborts the whole operation if any taxonomy creation fails.
   */
  saveNewsItem(item: NewsItem, newsId?: number | null): Observable<NewsItem> {
    return this.resolvePendingCategories(item.categories || []).pipe(
      switchMap(categories =>
        this.resolvePendingTags(item.tags || []).pipe(
          map(tags => ({ ...item, categories, tags }))
        )
      ),
      switchMap(resolved => {
        const save$ = newsId
          ? this.newsService.updateNews(newsId, resolved)
          : this.newsService.createNews(resolved);
        return save$.pipe(
          catchError(() => throwError(() => new Error('Could not save news.')))
        );
      })
    );
  }

  private resolvePendingCategories(categories: NewsCategory[]): Observable<NewsCategory[]> {
    const pending = categories.filter(isPendingTaxonomyItem);
    if (!pending.length) {
      return of(categories);
    }

    return forkJoin(
      pending.map(cat =>
        this.newsService.createCategory(cat.label).pipe(
          map(created => {
            if (!created?.id) {
              throw new Error(`Could not create category "${cat.label}".`);
            }
            return { pending: cat, created: { ...created, isPending: false } };
          }),
          catchError(() =>
            throwError(() => new Error(`Could not create category "${cat.label}".`))
          )
        )
      )
    ).pipe(
      map(replacements => this.replacePendingItems(categories, replacements))
    );
  }

  private resolvePendingTags(tags: NewsTag[]): Observable<NewsTag[]> {
    const pending = tags.filter(isPendingTaxonomyItem);
    if (!pending.length) {
      return of(tags);
    }

    return forkJoin(
      pending.map(tag =>
        this.newsService.createTag(tag.label).pipe(
          map(created => {
            if (!created?.id) {
              throw new Error(`Could not create tag "${tag.label}".`);
            }
            return { pending: tag, created: { ...created, isPending: false } };
          }),
          catchError(() =>
            throwError(() => new Error(`Could not create tag "${tag.label}".`))
          )
        )
      )
    ).pipe(
      map(replacements => this.replacePendingItems(tags, replacements))
    );
  }

  private replacePendingItems<T extends { id: string; label: string; isPending?: boolean }>(
    items: T[],
    replacements: { pending: T; created: T }[]
  ): T[] {
    const byPendingId = new Map(replacements.map(r => [r.pending.id, r.created]));
    return items.map(item => {
      const replacement = byPendingId.get(item.id);
      if (replacement) {
        return { ...replacement, isPending: false };
      }
      return item;
    });
  }
}
