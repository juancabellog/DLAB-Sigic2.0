import { ChangeDetectorRef, Component, OnInit, QueryList, ViewChildren } from '@angular/core';
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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';

import { RichTextEditorComponent } from '../../news/components/rich-text-editor/rich-text-editor.component';
import { AgendaCategorySelectComponent } from '../components/agenda-category-select/agenda-category-select.component';
import { MessageService } from '../../../core/services/message.service';
import { AuthService } from '../../../core/services/auth.service';
import { AgendaService } from '../services/agenda.service';
import { AgendaSaveService } from '../services/agenda-save.service';
import {
  AGENDA_PUBLISH_CONFIG,
  AgendaEvent,
  EVENT_MODE,
  EVENT_MODE_LABELS,
  EventMode,
  PUBLICATION_STATUS,
  PUBLICATION_STATUS_LABELS,
  PublicationStatus,
  TRANSLATION_STATUS,
  buildSpanishSnapshot,
  createEmptyAgendaEvent,
  hasEnglishContent,
  hasRichTextContent,
  isEndTimeAfterStart,
  plainTextFromHtml,
  slugify
} from '../models/agenda.models';

@Component({
  selector: 'app-agenda-edit',
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
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    RichTextEditorComponent,
    AgendaCategorySelectComponent
  ],
  templateUrl: './agenda-edit.component.html',
  styleUrls: ['./agenda-edit.component.scss']
})
export class AgendaEditComponent implements OnInit {
  @ViewChildren(RichTextEditorComponent) richTextEditors!: QueryList<RichTextEditorComponent>;

  isEditMode = false;
  itemId: number | null = null;
  loading = false;
  saving = false;
  translating = false;
  uploadingImage = false;
  selectedTabIndex = 0;

  item: AgendaEvent = createEmptyAgendaEvent();
  endTimeError = '';

  translationReviewWarning = false;
  private validatedSpanishSnapshot = '';

  readonly publicationStatusOptions: { value: PublicationStatus; label: string }[] = [
    { value: PUBLICATION_STATUS.DRAFT, label: PUBLICATION_STATUS_LABELS.draft },
    { value: PUBLICATION_STATUS.READY_TO_PUBLISH, label: PUBLICATION_STATUS_LABELS.ready_to_publish },
    { value: PUBLICATION_STATUS.PUBLISHED, label: PUBLICATION_STATUS_LABELS.published },
    { value: PUBLICATION_STATUS.UNPUBLISHED, label: PUBLICATION_STATUS_LABELS.unpublished }
  ];
  readonly eventModeOptions: { value: EventMode; label: string }[] = [
    { value: EVENT_MODE.IN_PERSON, label: EVENT_MODE_LABELS.in_person },
    { value: EVENT_MODE.ONLINE, label: EVENT_MODE_LABELS.online },
    { value: EVENT_MODE.HYBRID, label: EVENT_MODE_LABELS.hybrid }
  ];
  readonly eventModeLabels = EVENT_MODE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private agendaService: AgendaService,
    private agendaSaveService: AgendaSaveService,
    private messageService: MessageService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
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
    return this.isEditMode ? 'Edit Agenda Event' : 'New Agenda Event';
  }

  get isPublished(): boolean {
    return this.item.publicationStatus === PUBLICATION_STATUS.PUBLISHED;
  }

  get canSave(): boolean {
    console.log('canSave', this.hasMinimumContent(), this.hasRequiredEventFields(), !this.endTimeError);
    return this.hasMinimumContent() && this.hasRequiredEventFields() && !this.endTimeError;
  }

  get saveBlockedReason(): string {
    const missing: string[] = [];
    if (!this.item.titleEs?.trim()) {
      missing.push('Spanish title');
    }
    if (!hasRichTextContent(this.item.descriptionEs) && !hasRichTextContent(this.item.descriptionEn)) {
      missing.push('Spanish or English description');
    }
    if (!this.item.eventDate?.trim()) {
      missing.push('event date');
    }
    if (!this.item.startTime?.trim()) {
      missing.push('start time');
    }
    if (!this.item.eventMode) {
      missing.push('event mode');
    }
    if (this.endTimeError) {
      missing.push('valid end time');
    }
    if (missing.length === 0) {
      return '';
    }
    return `Complete required fields: ${missing.join(', ')}`;
  }

  get showLocationField(): boolean {
    return this.item.eventMode === EVENT_MODE.IN_PERSON || this.item.eventMode === EVENT_MODE.HYBRID;
  }

  get showOnlineUrlField(): boolean {
    return this.item.eventMode === EVENT_MODE.ONLINE || this.item.eventMode === EVENT_MODE.HYBRID;
  }

  loadItem(id: number): void {
    this.loading = true;
    this.agendaService.getAgendaById(id).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: item => {
        this.item = {
          ...item,
          categories: item.categories || []
        };
        if (item.translationStatus === TRANSLATION_STATUS.VALIDATED) {
          this.validatedSpanishSnapshot = buildSpanishSnapshot(item);
        }
        this.validateEndTime();
        this.checkTranslationReviewWarning();
      },
      error: () => {
        this.messageService.error('Could not load agenda event.');
        this.router.navigate(['/agenda']);
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
    if (hasEnglishContent(this.item)) {
      if (
        this.item.translationStatus === TRANSLATION_STATUS.VALIDATED ||
        this.item.translationStatus === TRANSLATION_STATUS.AUTO_GENERATED ||
        this.item.translationStatus === TRANSLATION_STATUS.NO_TRANSLATION
      ) {
        if (this.item.titleEn || this.item.descriptionEn) {
          this.item.translationStatus = TRANSLATION_STATUS.MANUALLY_EDITED;
        }
      }
    }
  }

  onEventFieldChange(): void {
    this.validateEndTime();
  }

  onTabChange(index: number): void {
    this.flushRichTextEditors();
    this.selectedTabIndex = index;
  }

  onEventModeChange(): void {
    this.validateEndTime();
  }

  validateEndTime(): void {
    if (!isEndTimeAfterStart(this.item.startTime, this.item.endTime)) {
      this.endTimeError = 'End time must be after start time.';
    } else {
      this.endTimeError = '';
    }
  }

  hasRequiredEventFields(): boolean {
    return Boolean(
      this.item.eventDate?.trim() &&
      this.item.startTime?.trim() &&
      this.item.eventMode
    );
  }

  private hasMinimumContent(): boolean {
    if (!this.item.titleEs?.trim()) {
      return false;
    }
    return hasRichTextContent(this.item.descriptionEs) || hasRichTextContent(this.item.descriptionEn);
  }

  private flushRichTextEditors(): void {
    this.richTextEditors?.forEach(editor => editor.flushToModel());
  }

  generateAutomaticTranslation(): void {
    this.translateFromSpanish();
  }

  translateFromSpanish(): void {
    this.flushRichTextEditors();
    if (!this.item.titleEs?.trim() && !hasRichTextContent(this.item.descriptionEs)) {
      this.messageService.warn('Add Spanish content before generating a translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      !!this.item.titleEn?.trim() || hasRichTextContent(this.item.descriptionEn),
      'Existing English title and/or description will be overwritten. Do you want to continue?',
      () => this.runTranslation('es_to_en')
    );
  }

  translateFromEnglish(): void {
    this.flushRichTextEditors();
    if (!this.item.titleEn?.trim() && !hasRichTextContent(this.item.descriptionEn)) {
      this.messageService.warn('Add English content before generating a translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      !!this.item.titleEs?.trim() || hasRichTextContent(this.item.descriptionEs),
      'Existing Spanish title and/or description will be overwritten. Do you want to continue?',
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
    this.agendaService.generateTranslation(this.item, direction).pipe(
      finalize(() => { this.translating = false; })
    ).subscribe({
      next: updated => {
        if (direction === 'en_to_es') {
          this.item.titleEs = updated.titleEs;
          this.item.descriptionEs = updated.descriptionEs;
          this.messageService.success('Spanish translation generated.');
        } else {
          this.item.titleEn = updated.titleEn;
          this.item.descriptionEn = updated.descriptionEn;
          this.messageService.success('English translation generated.');
        }
        this.item.translationStatus = TRANSLATION_STATUS.AUTO_GENERATED;
        this.translationReviewWarning = false;
        this.cdr.detectChanges();
      },
      error: () => this.messageService.error('Translation could not be generated.')
    });
  }

  validateTranslation(): void {
    if (!this.item.titleEn && !this.item.descriptionEn) {
      this.messageService.warn('No English content to validate.');
      return;
    }
    const validated = this.agendaService.validateTranslation(this.item);
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
    this.agendaService.uploadImage(file).pipe(
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
    return this.agendaService.resolveMediaUrl(this.item.mainImageUrl);
  }

  autoFillSeoFromSpanish(): void {
    if (this.item.titleEs && !this.item.slug) {
      this.item.slug = slugify(this.item.titleEs);
    }
    if (this.item.titleEs && !this.item.metaTitle) {
      this.item.metaTitle = this.item.titleEs;
    }
    const descriptionText = plainTextFromHtml(this.item.descriptionEs);
    if (descriptionText && !this.item.metaDescription) {
      this.item.metaDescription = descriptionText.slice(0, 160);
    }
    if (this.item.titleEs && !this.item.ogTitle) {
      this.item.ogTitle = this.item.titleEs;
    }
    if (descriptionText && !this.item.ogDescription) {
      this.item.ogDescription = descriptionText.slice(0, 200);
    }
    if (this.item.mainImageUrl && !this.item.ogImageUrl) {
      this.item.ogImageUrl = this.item.mainImageUrl;
    }
    if (this.item.slug && !this.item.publicUrl) {
      this.item.publicUrl = `https://example.org/agenda/${this.item.slug}`;
    }
  }

  backToList(): void {
    this.router.navigate(['/agenda']);
  }

  saveDraft(): void {
    if (!this.validateBeforeSave()) return;
    this.item.publicationStatus = PUBLICATION_STATUS.DRAFT;
    this.persist(false);
  }

  saveChanges(): void {
    if (!this.validateBeforeSave()) return;
    this.persist(false);
  }

  markReadyToPublish(): void {
    if (!this.validateBeforeSave()) return;
    this.item.publicationStatus = PUBLICATION_STATUS.READY_TO_PUBLISH;
    this.persist(false, 'Event marked as ready to publish.');
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
    this.persist(false, 'Event unpublished.');
  }

  preview(): void {
    const url = this.item.publicUrl || (this.item.slug ? `https://example.org/agenda/${this.item.slug}` : null);
    if (url) window.open(url, '_blank');
    else this.messageService.info('Set a public URL or slug to preview.');
  }

  private doPublish(): void {
    this.item.publicationStatus = PUBLICATION_STATUS.PUBLISHED;
    if (!this.item.publishedAt) {
      this.item.publishedAt = new Date().toISOString().slice(0, 10);
    }
    this.persist(false, 'Event published.');
  }

  private validateBeforeSave(): boolean {
    this.flushRichTextEditors();
    if (!this.hasMinimumContent()) {
      this.messageService.warn('Title (Spanish) and a description (Spanish or English) are required.');
      this.selectedTabIndex = hasRichTextContent(this.item.descriptionEs) ? 0 : 1;
      return false;
    }
    if (!this.hasRequiredEventFields()) {
      this.messageService.warn('Event date, start time and event mode are required.');
      return false;
    }
    this.validateEndTime();
    if (this.endTimeError) {
      this.messageService.warn(this.endTimeError);
      return false;
    }
    return true;
  }

  private validatePublishRequirements(): boolean {
    if (!this.validateBeforeSave()) return false;
    if (!this.item.mainImageUrl?.trim()) {
      this.messageService.warn('Main image is required to publish.');
      return false;
    }
    return true;
  }

  private shouldWarnUnvalidatedEnglish(): boolean {
    const hasEn = hasEnglishContent(this.item);
    if (AGENDA_PUBLISH_CONFIG.requireValidatedEnglish) {
      return hasEn && this.item.translationStatus !== TRANSLATION_STATUS.VALIDATED;
    }
    return hasEn && this.item.translationStatus !== TRANSLATION_STATUS.VALIDATED;
  }

  private persist(navigateToList: boolean, successMsg?: string): void {
    this.saving = true;
    this.agendaSaveService.saveAgendaEvent(this.item, this.itemId).pipe(
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: saved => {
        this.item = {
          ...saved,
          categories: saved.categories || []
        };
        this.itemId = saved.id ?? this.itemId;
        this.isEditMode = true;
        this.messageService.success(successMsg || 'Event saved.');
        if (navigateToList) this.router.navigate(['/agenda']);
      },
      error: (err: Error) => {
        const message = err?.message || 'Could not save event.';
        this.messageService.error(message);
      }
    });
  }

  private checkTranslationReviewWarning(): void {
    this.translationReviewWarning =
      this.item.translationStatus === TRANSLATION_STATUS.REQUIRES_REVIEW;
  }
}
