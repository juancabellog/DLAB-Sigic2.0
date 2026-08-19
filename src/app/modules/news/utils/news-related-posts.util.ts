import {
  NewsItem,
  NewsRelatedPost,
  PUBLICATION_STATUS,
  PublicationStatus,
  TRANSLATION_STATUS
} from '../models/news.models';
import { formatLocalDate } from '../../../core/utils/date.util';
import { termContains } from './news-taxonomy.util';

export type RelatedPostStatusFilter =
  | 'all'
  | PublicationStatus
  | 'ready_to_publish';

export const RELATED_POST_FILTER_LABELS: Record<RelatedPostStatusFilter, string> = {
  all: 'All Posts',
  [PUBLICATION_STATUS.PUBLISHED]: 'Published',
  [PUBLICATION_STATUS.DRAFT]: 'Draft',
  ready_to_publish: 'Ready to publish',
  [PUBLICATION_STATUS.UNPUBLISHED]: 'Unpublished'
};

export const RELATED_POST_FILTER_OPTIONS: RelatedPostStatusFilter[] = [
  'all',
  PUBLICATION_STATUS.PUBLISHED,
  PUBLICATION_STATUS.DRAFT,
  'ready_to_publish',
  PUBLICATION_STATUS.UNPUBLISHED
];

export function isReadyToPublishNews(item: NewsItem): boolean {
  return item.publicationStatus === PUBLICATION_STATUS.DRAFT && (
    item.translationStatus === TRANSLATION_STATUS.VALIDATED ||
    item.translationStatus === TRANSLATION_STATUS.MANUALLY_EDITED
  );
}

export function getRelatedPostDisplayStatus(item: NewsItem): string {
  if (isReadyToPublishNews(item)) {
    return 'Ready to publish';
  }
  switch (item.publicationStatus) {
    case PUBLICATION_STATUS.PUBLISHED:
      return 'Published';
    case PUBLICATION_STATUS.UNPUBLISHED:
      return 'Unpublished';
    default:
      return 'Draft';
  }
}

export function matchesRelatedPostStatusFilter(
  item: NewsItem,
  filter: RelatedPostStatusFilter
): boolean {
  if (filter === 'all') return true;
  if (filter === 'ready_to_publish') return isReadyToPublishNews(item);
  return item.publicationStatus === filter;
}

export function searchNewsPostsLocally(
  posts: NewsItem[],
  query: string,
  filter: RelatedPostStatusFilter,
  excludeId?: number | null
): NewsItem[] {
  return posts.filter(post => {
    if (excludeId && post.id === excludeId) return false;
    if (!matchesRelatedPostStatusFilter(post, filter)) return false;
    const title = `${post.titleEs || ''} ${post.titleEn || ''}`;
    return termContains(title, query);
  });
}

export function mapNewsItemToRelatedPost(item: NewsItem): NewsRelatedPost {
  return {
    id: item.id!,
    title: item.titleEs?.trim() || item.titleEn?.trim() || `News #${item.id}`,
    titleEn: item.titleEn,
    thumbnailUrl: item.mainImageUrl || undefined,
    publicationStatus: item.publicationStatus,
    publicationDate: item.publicationDate || item.publishedAt || undefined,
    author: item.author || undefined
  };
}

export function formatRelatedPostDate(date?: string | null): string {
  return formatLocalDate(date, 'MMM dd, yyyy', 'en-US');
}

export function buildRelatedPostMetadataLine(
  item: Pick<NewsRelatedPost, 'publicationDate' | 'author'>,
  statusLabel: string
): string {
  const date = formatRelatedPostDate(item.publicationDate);
  const author = item.author?.trim();
  const parts = [statusLabel, date, author].filter(Boolean);
  return parts.join(' · ');
}
