import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MessageService } from '../../../../core/services/message.service';
import { NewsTag } from '../../models/news.models';
import { NewsService } from '../../services/news.service';
import {
  canCreateTagFromInput,
  createPendingTaxonomyId,
  filterTagsLocally,
  hasDuplicateTerm,
  isPendingTaxonomyItem,
  termsMatch
} from '../../utils/news-taxonomy.util';

@Component({
  selector: 'app-news-tags-input',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './news-tags-input.component.html',
  styleUrls: ['./news-tags-input.component.scss']
})
export class NewsTagsInputComponent implements OnInit {
  @Input() tags: NewsTag[] = [];
  @Output() tagsChange = new EventEmitter<NewsTag[]>();
  @Input() disabled = false;
  /** Preload catalog when the form renders; if false, loads on first input focus. */
  @Input() preloadCatalog = true;

  @ViewChild('tagInput') tagInputRef?: ElementRef<HTMLInputElement>;

  allTags: NewsTag[] = [];
  inputValue = '';
  loading = false;
  catalogLoadFailed = false;

  private catalogRequested = false;

  constructor(
    private newsService: NewsService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    if (this.preloadCatalog) {
      this.ensureCatalogLoaded();
    }
  }

  /** Local filter — swap for remote searchTags(query) when the catalog outgrows in-memory filtering. */
  get filteredSuggestions(): NewsTag[] {
    return filterTagsLocally(this.allTags, this.inputValue, this.tags);
  }

  get canCreateTag(): boolean {
    return canCreateTagFromInput(this.inputValue, this.allTags, this.tags);
  }

  get createTagLabel(): string {
    return `Create “${this.inputValue.trim()}”`;
  }

  onInputFocus(): void {
    this.ensureCatalogLoaded();
  }

  ensureCatalogLoaded(): void {
    if (this.catalogRequested || this.disabled) return;
    this.catalogRequested = true;
    this.loading = true;
    this.catalogLoadFailed = false;

    this.newsService.getTags().subscribe({
      next: tags => {
        this.allTags = tags;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.catalogLoadFailed = true;
        this.catalogRequested = false;
        this.messageService.error('Could not load tags. Please try again.');
      }
    });
  }

  removeTag(tag: NewsTag): void {
    if (this.disabled) return;
    this.tags = this.tags.filter(t => t.id !== tag.id && !termsMatch(t.label, tag.label));
    this.tagsChange.emit(this.tags);
  }

  addTag(tag: NewsTag): void {
    if (this.disabled || !tag.id) return;
    if (hasDuplicateTerm(this.tags.map(t => t.label), tag.label)) {
      this.clearInput();
      return;
    }
    this.tags = [...this.tags, tag];
    this.tagsChange.emit(this.tags);
    this.clearInput();
  }

  createTagFromInput(): void {
    const value = this.inputValue.trim();
    if (!value) return;

    const existing = this.allTags.find(t => termsMatch(t.label, value));
    if (existing) {
      this.addTag(existing);
      return;
    }

    const pending: NewsTag = {
      id: createPendingTaxonomyId(value),
      label: value,
      isPending: true
    };
    this.addTag(pending);
  }

  isPendingTag(tag: NewsTag): boolean {
    return isPendingTaxonomyItem(tag);
  }

  onChipInputTokenEnd(): void {
    if (this.canCreateTag) {
      this.createTagFromInput();
    } else if (this.filteredSuggestions.length === 1) {
      this.addTag(this.filteredSuggestions[0]);
    }
  }

  onSuggestionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value as string;
    if (value.startsWith('__create__:')) {
      this.createTagFromInput();
      return;
    }
    const tag = this.allTags.find(t => t.id === value);
    if (tag) {
      this.addTag(tag);
    }
  }

  onInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.onChipInputTokenEnd();
    }
  }

  private clearInput(): void {
    this.inputValue = '';
    if (this.tagInputRef?.nativeElement) {
      this.tagInputRef.nativeElement.value = '';
    }
  }

  displayTagLabel = (): string => '';
}
