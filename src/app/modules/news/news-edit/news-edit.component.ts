import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';

import { RichTextEditorComponent } from '../components/rich-text-editor/rich-text-editor.component';
import { NewsCategorySelectComponent } from '../components/news-category-select/news-category-select.component';
import { NewsTagsInputComponent } from '../components/news-tags-input/news-tags-input.component';
import { NewsRelatedPostsFieldComponent } from '../components/news-related-posts-field/news-related-posts-field.component';
import { MessageService } from '../../../core/services/message.service';
import { AuthService } from '../../../core/services/auth.service';
import { NewsService } from '../services/news.service';
import { NewsSaveService } from '../services/news-save.service';
import {
  NewsItem,
  MAX_RELATED_POSTS,
  NEWS_PUBLISH_CONFIG,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  PublicationStatus,
  TRANSLATION_STATUS,
  buildSpanishSnapshot,
  createEmptyNewsItem,
  hasRequiredSpanishContent,
  hasSpanishContent,
  hasRichTextContent,
  slugify
} from '../models/news.models';

@Component({
  selector: 'app-news-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    RichTextEditorComponent,
    NewsCategorySelectComponent,
    NewsTagsInputComponent,
    NewsRelatedPostsFieldComponent
  ],
  templateUrl: './news-edit.component.html',
  styleUrls: ['./news-edit.component.scss']
})
export class NewsEditComponent implements OnInit {
  isEditMode = false;
  itemId: number | null = null;
  loading = false;
  saving = false;
  translating = false;
  uploadingImage = false;
  selectedTabIndex = 0;

  item: NewsItem = createEmptyNewsItem();

  translationReviewWarning = false;
  private validatedSpanishSnapshot = '';

  readonly publicationStatusOptions: { value: PublicationStatus; label: string }[] = [
    { value: PUBLICATION_STATUS.DRAFT, label: PUBLICATION_STATUS_LABELS.draft },
    { value: PUBLICATION_STATUS.PUBLISHED, label: PUBLICATION_STATUS_LABELS.published },
    { value: PUBLICATION_STATUS.UNPUBLISHED, label: PUBLICATION_STATUS_LABELS.unpublished }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private newsService: NewsService,
    private newsSaveService: NewsSaveService,
    private messageService: MessageService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id && id !== 'new') {
        this.isEditMode = true;
        this.itemId = parseInt(id, 10);
        this.loadItem(this.itemId);
      } else {
        this.isEditMode = false;
        this.item.author = this.authService.getCurrentUser()?.username || '';
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit News' : 'New News';
  }

  get isPublished(): boolean {
    return this.item.publicationStatus === PUBLICATION_STATUS.PUBLISHED;
  }

  get canSave(): boolean {
    return hasRequiredSpanishContent(this.item);
  }

  loadItem(id: number): void {
    this.loading = true;
    this.newsService.getNewsById(id).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: item => {
        this.item = {
          ...item,
          categories: item.categories || [],
          tags: item.tags || [],
          relatedPosts: item.relatedPosts || []
        };
        this.syncPublicationDateFromItem();
        if (item.translationStatus === TRANSLATION_STATUS.VALIDATED) {
          this.validatedSpanishSnapshot = buildSpanishSnapshot(item);
        }
        this.checkTranslationReviewWarning();
      },
      error: () => {
        this.messageService.error('Could not load news item.');
        this.router.navigate(['/news']);
      }
    });
  }

  onSpanishFieldChange(): void {
    this.autoFillSeoFromSpanish();
    if (
      this.item.translationStatus === TRANSLATION_STATUS.VALIDATED &&
      this.validatedSpanishSnapshot &&
      buildSpanishSnapshot(this.item) !== this.validatedSpanishSnapshot
    ) {
      this.item.translationStatus = TRANSLATION_STATUS.REQUIRES_REVIEW;
      this.translationReviewWarning = true;
    }
  }

  onEnglishFieldChange(): void {
    if (hasSpanishContent(this.item)) {
      if (this.item.translationStatus === TRANSLATION_STATUS.VALIDATED) {
        // manual edit after validation
      }
      if (
        this.item.translationStatus === TRANSLATION_STATUS.VALIDATED ||
        this.item.translationStatus === TRANSLATION_STATUS.AUTO_GENERATED ||
        this.item.translationStatus === TRANSLATION_STATUS.NO_TRANSLATION
      ) {
        if (this.item.titleEn || this.item.summaryEn || this.item.bodyEn) {
          this.item.translationStatus = TRANSLATION_STATUS.MANUALLY_EDITED;
        }
      }
    }
  }

  generateAutomaticTranslation(): void {
    this.translateFromSpanish();
  }

  translateFromSpanish(): void {
    if (!this.item.titleEs?.trim() && !this.item.summaryEs?.trim() && !hasRichTextContent(this.item.bodyEs)) {
      this.messageService.warn('Add Spanish content before generating a translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      !!this.item.titleEn?.trim() || !!this.item.summaryEn?.trim() || hasRichTextContent(this.item.bodyEn),
      'Existing English title, summary and/or body will be overwritten. Do you want to continue?',
      () => this.runTranslation('es_to_en')
    );
  }

  translateFromEnglish(): void {
    if (!this.item.titleEn?.trim() && !this.item.summaryEn?.trim() && !hasRichTextContent(this.item.bodyEn)) {
      this.messageService.warn('Add English content before generating a translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      !!this.item.titleEs?.trim() || !!this.item.summaryEs?.trim() || hasRichTextContent(this.item.bodyEs),
      'Existing Spanish title, summary and/or body will be overwritten. Do you want to continue?',
      () => this.runTranslation('en_to_es')
    );
  }

  private confirmOverwriteIfNeeded(hasContent: boolean, message: string, onConfirm: () => void): void {
    if (!hasContent) {
      onConfirm();
      return;
    }
    this.messageService.confirm(message, (accepted: boolean) => {
      if (accepted) {
        onConfirm();
      }
    }, 'Overwrite content?');
  }

  private runTranslation(direction: 'es_to_en' | 'en_to_es'): void {
    this.translating = true;
    this.messageService.show(
      'This process may take several minutes. Please keep this page open until it completes.',
      'Translation in progress',
      'info',
      10_000
    );
    this.newsService.generateTranslation(this.item, direction).pipe(
      finalize(() => { this.translating = false; })
    ).subscribe({
      next: updated => {
        if (direction === 'en_to_es') {
          this.item.titleEs = updated.titleEs;
          this.item.summaryEs = updated.summaryEs;
          this.item.bodyEs = updated.bodyEs;
          this.messageService.success('Spanish translation generated.');
        } else {
          this.item.titleEn = updated.titleEn;
          this.item.summaryEn = updated.summaryEn;
          this.item.bodyEn = updated.bodyEn;
          this.messageService.success('English translation generated.');
        }
        this.item.translationStatus = TRANSLATION_STATUS.AUTO_GENERATED;
        this.translationReviewWarning = false;
      },
      error: () => this.messageService.error('Translation could not be generated.')
    });
  }

  validateTranslation(): void {
    if (!this.item.titleEn && !this.item.summaryEn && !this.item.bodyEn) {
      this.messageService.warn('No English content to validate.');
      return;
    }
    const validated = this.newsService.validateTranslation(this.item);
    this.item = validated;
    this.validatedSpanishSnapshot = buildSpanishSnapshot(this.item);
    this.translationReviewWarning = false;
    this.messageService.success('Translation validated', 'English version marked as validated.');
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadingImage = true;
    this.newsService.uploadImage(file).pipe(
      finalize(() => { this.uploadingImage = false; input.value = ''; })
    ).subscribe({
      next: path => {
        this.item.mainImageUrl = path;
        this.autoFillSeoFromSpanish();
        this.messageService.success('Image uploaded');
      },
      error: () => this.messageService.error('Image upload failed.')
    });
  }

  getImagePreviewUrl(): string | null {
    return this.newsService.resolveMediaUrl(this.item.mainImageUrl);
  }

  autoFillSeoFromSpanish(): void {
    if (this.item.titleEs && !this.item.slug) {
      this.item.slug = slugify(this.item.titleEs);
    }
    if (this.item.titleEs && !this.item.metaTitle) {
      this.item.metaTitle = this.item.titleEs;
    }
    if (this.item.summaryEs && !this.item.metaDescription) {
      this.item.metaDescription = this.item.summaryEs.replace(/<[^>]*>/g, '').slice(0, 160);
    }
    if (this.item.titleEs && !this.item.ogTitle) {
      this.item.ogTitle = this.item.titleEs;
    }
    if (this.item.summaryEs && !this.item.ogDescription) {
      this.item.ogDescription = this.item.summaryEs.replace(/<[^>]*>/g, '').slice(0, 200);
    }
    if (this.item.mainImageUrl && !this.item.ogImageUrl) {
      this.item.ogImageUrl = this.item.mainImageUrl;
    }
    if (this.item.slug && !this.item.publicUrl) {
      this.item.publicUrl = `https://example.org/news/${this.item.slug}`;
    }
  }

  backToList(): void {
    this.router.navigate(['/news']);
  }

  saveDraft(): void {
    this.item.publicationStatus = PUBLICATION_STATUS.DRAFT;
    this.persist(false);
  }

  saveChanges(): void {
    this.persist(false);
  }

  publish(): void {
    if (!this.validatePublishRequirements()) return;
    if (this.shouldWarnUnvalidatedEnglish()) {
      this.messageService.confirm(
        'English translation is not validated. Publish anyway?',
        ok => { if (ok) this.doPublish(); }
      );
      return;
    }
    this.doPublish();
  }

  unpublish(): void {
    this.item.publicationStatus = PUBLICATION_STATUS.UNPUBLISHED;
    this.persist(false, 'News unpublished.');
  }

  preview(): void {
    const url = this.item.mediaLink?.trim() || this.item.publicUrl || (this.item.slug ? `https://example.org/news/${this.item.slug}` : null);
    if (url) window.open(url, '_blank');
    else this.messageService.info('Set a Media Link to preview.');
  }

  private doPublish(): void {
    this.item.publicationStatus = PUBLICATION_STATUS.PUBLISHED;
    if (!this.item.publicationDate) {
      this.item.publicationDate = new Date().toISOString().slice(0, 10);
    }
    this.item.publishedAt = this.item.publicationDate;
    this.persist(false, 'News published.');
  }

  private syncPublicationDateFromItem(): void {
    if (!this.item.publicationDate && this.item.publishedAt) {
      this.item.publicationDate = this.item.publishedAt;
    }
  }

  private validatePublishRequirements(): boolean {
    if (!this.canSave) {
      this.messageService.warn('Title, summary and body (Spanish) are required.');
      this.selectedTabIndex = 0;
      return false;
    }
    if (!this.item.mainImageUrl?.trim()) {
      this.messageService.warn('Main image is required to publish.');
      return false;
    }
    return true;
  }

  private shouldWarnUnvalidatedEnglish(): boolean {
    const hasEn = hasSpanishContent(this.item);
    if (NEWS_PUBLISH_CONFIG.requireValidatedEnglish) {
      return hasEn && this.item.translationStatus !== TRANSLATION_STATUS.VALIDATED;
    }
    return hasEn && this.item.translationStatus !== TRANSLATION_STATUS.VALIDATED;
  }

  private persist(navigateToList: boolean, successMsg?: string): void {
    if (!this.item.publicationDate) {
      this.item.publicationDate = new Date().toISOString().slice(0, 10);
    }
    if ((this.item.relatedPosts || []).length > MAX_RELATED_POSTS) {
      this.messageService.error(`You can select at most ${MAX_RELATED_POSTS} related posts.`);
      return;
    }
    this.saving = true;
    this.newsSaveService.saveNewsItem(this.item, this.itemId).pipe(
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: saved => {
        this.item = {
          ...saved,
          categories: saved.categories || [],
          tags: saved.tags || [],
          relatedPosts: saved.relatedPosts || []
        };
        this.itemId = saved.id ?? this.itemId;
        this.isEditMode = true;
        this.messageService.success(successMsg || 'News saved.');
        if (navigateToList) this.router.navigate(['/news']);
      },
      error: (err: Error) => {
        const message = err?.message || 'Could not save news.';
        this.messageService.error(message);
      }
    });
  }

  private checkTranslationReviewWarning(): void {
    this.translationReviewWarning =
      this.item.translationStatus === TRANSLATION_STATUS.REQUIRES_REVIEW;
  }
}
