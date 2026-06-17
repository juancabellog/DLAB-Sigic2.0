import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { NewsItem, NewsRelatedPost } from '../models/news.models';
import {
  RelatedPostStatusFilter,
  mapNewsItemToRelatedPost,
  searchNewsPostsLocally
} from '../utils/news-related-posts.util';
import { NewsService } from './news.service';

/**
 * Isolated service for related-posts picker and preview.
 * Local filtering today; swap searchNewsPosts() for remote search when the catalog grows.
 */
@Injectable({
  providedIn: 'root'
})
export class NewsRelatedPostsService {

  constructor(private newsService: NewsService) {}

  getRelatedPostCandidates(excludeId?: number | null): Observable<NewsItem[]> {
    return this.newsService.getNewsList({ size: 1000, sort: 'id', direction: 'DESC' }).pipe(
      map(response =>
        (response.content || []).filter(post => !excludeId || post.id !== excludeId)
      )
    );
  }

  searchNewsPosts(
    posts: NewsItem[],
    query: string,
    statusFilter: RelatedPostStatusFilter,
    excludeId?: number | null
  ): NewsItem[] {
    return searchNewsPostsLocally(posts, query, statusFilter, excludeId);
  }

  getNewsByIds(ids: number[]): Observable<NewsRelatedPost[]> {
    if (!ids.length) {
      return of([]);
    }
    return this.getRelatedPostCandidates().pipe(
      map(posts => {
        const byId = new Map(posts.filter(p => p.id != null).map(p => [p.id!, p]));
        return ids
          .map(id => byId.get(id))
          .filter((post): post is NewsItem => !!post)
          .map(mapNewsItemToRelatedPost);
      })
    );
  }

  resolveThumbnailUrl(path?: string | null): string | null {
    return this.newsService.resolveMediaUrl(path);
  }
}
