import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';

import { MessageService } from '../../../core/services/message.service';
import { AgendaService } from '../services/agenda.service';
import {
  AgendaEvent,
  EVENT_MODE,
  EVENT_MODE_LABELS,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  TRANSLATION_STATUS,
  formatEventTimeRange,
  isPastEvent
} from '../models/agenda.models';

export type PreviewLanguage = 'es' | 'en';

@Component({
  selector: 'app-agenda-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './agenda-view.component.html',
  styleUrls: ['./agenda-view.component.scss']
})
export class AgendaViewComponent implements OnInit {
  item: AgendaEvent | null = null;
  loading = false;
  selectedLanguage: PreviewLanguage = 'es';

  readonly eventModeLabels = EVENT_MODE_LABELS;
  readonly formatEventTimeRange = formatEventTimeRange;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private agendaService: AgendaService,
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
    this.agendaService.getAgendaById(id).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: item => {
        this.item = item;
        this.selectedLanguage = 'es';
      },
      error: () => {
        this.messageService.error('Could not load event.');
        this.router.navigate(['/agenda']);
      }
    });
  }

  onLanguageChange(language: PreviewLanguage): void {
    if (language === 'en' && this.isEnglishDisabled()) return;
    this.selectedLanguage = language;
  }

  getDisplayTitle(): string {
    if (!this.item) return 'Event';
    if (this.selectedLanguage === 'en') {
      return this.item.titleEn?.trim() || 'Event';
    }
    return this.item.titleEs?.trim() || 'Event';
  }

  getDisplaySummary(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.summaryEn || '') : (this.item.summaryEs || '');
  }

  getDisplayDescription(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.descriptionEn || '') : (this.item.descriptionEs || '');
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
      this.item.descriptionEn?.trim()
    );
  }

  isEnglishValidated(): boolean {
    return this.item?.translationStatus === TRANSLATION_STATUS.VALIDATED;
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

  hasDisplayDescription(): boolean {
    return Boolean(this.getSanitizedDescriptionHtmlRaw()?.trim());
  }

  getSanitizedDescriptionHtml(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(this.getSanitizedDescriptionHtmlRaw());
  }

  getEditorialMetaLine(): string {
    if (!this.item) return '';

    const date = this.formatEditorialDate(this.item.eventDate);
    const time = formatEventTimeRange(this.item.startTime, this.item.endTime);
    const status = this.getEditorialStatusLabel();
    const parts = [status];
    if (date) parts.push(date);
    if (time && time !== '—') parts.push(time);
    if (this.isPast()) parts.push('Past event');
    return parts.join(' · ');
  }

  getEventModeLabel(): string {
    if (!this.item?.eventMode) return '';
    return this.eventModeLabels[this.item.eventMode];
  }

  getLocationLine(): string {
    if (!this.item) return '';
    if (this.item.eventMode === EVENT_MODE.ONLINE) {
      return this.item.onlineUrl || 'Online event';
    }
    if (this.item.eventMode === EVENT_MODE.HYBRID) {
      const parts = [];
      if (this.item.location?.trim()) parts.push(this.item.location);
      if (this.item.onlineUrl?.trim()) parts.push('Also online');
      return parts.join(' · ') || 'Hybrid event';
    }
    return this.item.location?.trim() || '';
  }

  isPast(): boolean {
    return this.item ? isPastEvent(this.item.eventDate) : false;
  }

  formatEventDate(date: string | null | undefined): string {
    return this.formatEditorialDate(date);
  }

  getDisplayOrganizer(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.organizerEn || '') : (this.item.organizerEs || '');
  }

  getDisplaySpeaker(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.speakerEn || '') : (this.item.speakerEs || '');
  }

  getDisplayAudience(): string {
    if (!this.item) return '';
    return this.selectedLanguage === 'en' ? (this.item.audienceEn || '') : (this.item.audienceEs || '');
  }

  getCtaUrl(): string {
    if (!this.item) return '';
    return this.item.ctaUrl?.trim() || '';
  }

  getCtaLabel(): string {
    if (!this.item) return 'Register';
    const label = this.selectedLanguage === 'en' ? this.item.ctaLabelEn : this.item.ctaLabelEs;
    return label?.trim() || 'Register';
  }

  getImageUrl(): string | null {
    return this.item ? this.agendaService.resolveMediaUrl(this.item.mainImageUrl) : null;
  }

  edit(): void {
    if (this.item?.id) this.router.navigate(['/agenda', this.item.id, 'edit']);
  }

  back(): void {
    this.router.navigate(['/agenda']);
  }

  preview(): void {
    const url = this.item?.publicUrl || this.buildPreviewUrlFromSlug();
    if (url) {
      window.open(url, '_blank');
    } else {
      this.messageService.info('No public URL configured for this event.');
    }
  }

  private getSanitizedDescriptionHtmlRaw(): string {
    const html = this.getDisplayDescription()?.trim();
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
      const resolved = this.agendaService.resolveMediaUrl(src) || src;
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
    return this.agendaService.resolveBodyHtmlForDisplay(sanitized);
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
        return PUBLICATION_STATUS_LABELS.published;
      case PUBLICATION_STATUS.UNPUBLISHED:
        return PUBLICATION_STATUS_LABELS.unpublished;
      case PUBLICATION_STATUS.READY_TO_PUBLISH:
        return PUBLICATION_STATUS_LABELS.ready_to_publish;
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
    return slug ? `https://example.org/agenda/${slug}` : null;
  }
}
