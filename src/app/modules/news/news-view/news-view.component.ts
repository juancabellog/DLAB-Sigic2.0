import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';

import { MessageService } from '../../../core/services/message.service';
import { NewsService } from '../services/news.service';
import {
  NewsItem,
  NewsRelatedPost,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  TRANSLATION_STATUS
} from '../models/news.models';
import {
  formatRelatedPostDate
} from '../utils/news-related-posts.util';

export type PreviewLanguage = 'es' | 'en';

@Component({
  selector: 'app-news-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './news-view.component.html',
  styleUrls: ['./news-view.component.scss']
})
export class NewsViewComponent implements OnInit {
  item: NewsItem | null = null;
  loading = false;
  selectedLanguage: PreviewLanguage = 'es';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private newsService: NewsService,
    private messageService: MessageService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) this.loadItem(parseInt(id, 10));
    });
  }

  loadItem(id: number): void {
    this.loading = true;
    this.newsService.getNewsById(id).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: item => {
        this.item = item;
        this.selectedLanguage = 'es';
      },
      error: () => {
        this.messageService.error('Could not load news.');
        this.router.navigate(['/news']);
      }
    });
  }

  onLanguageChange(language: PreviewLanguage): void {
    if (language === 'en' && this.isEnglishDisabled()) return;
    this.selectedLanguage = language;
  }

  getDisplayTitle(): string {
    if (!this.item) return 'News';
    if (this.selectedLanguage === 'en') {
      return this.item.titleEn?.trim() || 'News';
    }
    return this.item.titleEs?.trim() || 'News';
  }

  getDisplaySummary(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.summaryEn || '') : (this.item.summaryEs || '');
  }

  getDisplayBody(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.bodyEn || '') : (this.item.bodyEs || '');
  }

  getImageAlt(): string {
    if (!this.item) return this.getDisplayTitle();
    const alt = this.selectedLanguage === 'en' ? this.item.mainImageAltEn : this.item.mainImageAltEs;
    return alt?.trim() || this.getDisplayTitle();
  }

  hasEnglishContent(): boolean {
    if (!this.item) return false;
    return Boolean(
      this.item.titleEn?.trim() ||
      this.item.summaryEn?.trim() ||
      this.item.bodyEn?.trim()
    );
  }

  isEnglishValidated(): boolean {
    return true;//this.item?.translationStatus === TRANSLATION_STATUS.VALIDATED;
  }

  isEnglishDisabled(): boolean {
    return !this.hasEnglishContent() || !this.isEnglishValidated();
  }

  englishDisabledTooltip(): string {
    return 'English version is not available because the translation has not been validated.';
  }

  showEnglishNotValidatedWarning(): boolean {
    return this.hasEnglishContent() && !this.isEnglishValidated();
  }

  hasDisplayBody(): boolean {
    return Boolean(this.getSanitizedBodyHtmlRaw()?.trim());
  }

  getSanitizedBodyHtml(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(this.getSanitizedBodyHtmlRaw());
  }

  getEditorialMetaLine(): string {
    if (!this.item) return '';

    const date = this.formatEditorialDate(this.getMetaDate());
    if (!date) return this.getEditorialStatusLabel();

    if (this.item.publicationStatus === PUBLICATION_STATUS.PUBLISHED) {
      return `Published · ${date}`;
    }

    return `${this.getEditorialStatusLabel()} · Last updated ${date}`;
  }

  getImageUrl(): string | null {
    return this.item ? this.newsService.resolveMediaUrl(this.item.mainImageUrl) : null;
  }

  getRelatedPostTitle(post: NewsRelatedPost): string {
    if (this.selectedLanguage === 'en') {
      return post.titleEn?.trim() || post.title?.trim() || `News #${post.id}`;
    }
    return post.title?.trim() || post.titleEn?.trim() || `News #${post.id}`;
  }

  getRelatedPostThumbnail(post: NewsRelatedPost): string | null {
    return this.newsService.resolveMediaUrl(post.thumbnailUrl);
  }

  getRelatedPostDate(post: NewsRelatedPost): string {
    return formatRelatedPostDate(post.publicationDate);
  }

  getRelatedPostStatusLabel(post: NewsRelatedPost): string {
    if (!post.publicationStatus) return '';
    return PUBLICATION_STATUS_LABELS[post.publicationStatus as keyof typeof PUBLICATION_STATUS_LABELS]
      || String(post.publicationStatus);
  }

  edit(): void {
    if (this.item?.id) this.router.navigate(['/news', this.item.id, 'edit']);
  }

  back(): void {
    this.router.navigate(['/news']);
  }

  preview(): void {
    const url = this.item?.publicUrl || this.buildPreviewUrlFromSlug();
    if (url) {
      window.open(url, '_blank');
    } else {
      this.messageService.info('No public URL configured for this news item.');
    }
  }

  /** Strips hero image, title and lead duplicates from rich text before render. */
  private getSanitizedBodyHtmlRaw(): string {
    const html = this.getDisplayBody()?.trim();
    if (!html) return '';

    if (typeof DOMParser === 'undefined') {
      return html;
    }

    const doc = new DOMParser().parseFromString(html, 'text/html');
    const mainImageKey = this.getImageUrlKey(this.getImageUrl());
    const mainImageRawKey = this.getImageUrlKey(this.item?.mainImageUrl ?? null);
    const title = this.getDisplayTitle().trim().toLowerCase();
    const summary = this.getDisplaySummary().trim();

    doc.querySelectorAll('img').forEach(img => {
      const src = img.getAttribute('src') || '';
      const resolved = this.newsService.resolveMediaUrl(src) || src;
      const srcKey = this.getImageUrlKey(resolved) || this.getImageUrlKey(src);
      const isDuplicate =
        (mainImageKey && srcKey === mainImageKey) ||
        (mainImageRawKey && srcKey === mainImageRawKey);
      if (isDuplicate) {
        const parent = img.parentElement;
        img.remove();
        if (parent?.tagName === 'P' && !parent.textContent?.trim() && parent.children.length === 0) {
          parent.remove();
        }
      }
    });

    doc.querySelectorAll('h1, h2').forEach(heading => {
      const text = heading.textContent?.trim().toLowerCase() || '';
      if (title && text === title) {
        heading.remove();
      }
    });

    const firstBlock = doc.body.firstElementChild;
    if (summary && firstBlock?.tagName === 'P') {
      const pText = firstBlock.textContent?.trim() || '';
      if (pText === summary) {
        firstBlock.remove();
      }
    }

    const sanitized = doc.body.innerHTML.trim();
    return this.newsService.resolveBodyHtmlForDisplay(sanitized);
  }

  private getImageUrlKey(url: string | null): string | null {
    if (!url) return null;
    try {
      const normalized = url.split('?')[0].toLowerCase();
      const segments = normalized.split('/');
      return segments[segments.length - 1] || normalized;
    } catch {
      return url.toLowerCase();
    }
  }

  private getEditorialStatusLabel(): string {
    if (!this.item) return '';

    switch (this.item.publicationStatus) {
      case PUBLICATION_STATUS.PUBLISHED:
        return 'Published';
      case PUBLICATION_STATUS.UNPUBLISHED:
        return 'Unpublished';
      case PUBLICATION_STATUS.DRAFT:
        return this.getDraftEditorialLabel();
      default:
        return 'Draft';
    }
  }

  private getDraftEditorialLabel(): string {
    if (!this.item) return 'Draft';

    switch (this.item.translationStatus) {
      case TRANSLATION_STATUS.VALIDATED:
      case TRANSLATION_STATUS.MANUALLY_EDITED:
        return 'Ready to publish';
      case TRANSLATION_STATUS.NO_TRANSLATION:
      case TRANSLATION_STATUS.REQUIRES_REVIEW:
      case TRANSLATION_STATUS.AUTO_GENERATED:
        return 'Needs translation';
      default:
        return 'Draft';
    }
  }

  private getMetaDate(): string | null | undefined {
    if (!this.item) return null;

    if (this.item.publicationStatus === PUBLICATION_STATUS.PUBLISHED) {
      return this.item.publicationDate || this.item.publishedAt || this.item.updatedAt;
    }

    return this.item.updatedAt || this.item.publicationDate || this.item.createdAt;
  }

  private formatEditorialDate(date: string | null | undefined): string {
    if (!date) return '';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return date;
    return parsed.toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  private buildPreviewUrlFromSlug(): string | null {
    const slug = this.item?.slug?.trim();
    return slug ? `https://example.org/news/${slug}` : null;
  }
}
