import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MessageService } from '../../../../core/services/message.service';
import { NewsCategory } from '../../models/news.models';
import { NewsService } from '../../services/news.service';
import {
  createPendingTaxonomyId,
  filterTagsLocally,
  hasDuplicateTerm,
  isPendingTaxonomyItem,
  termsMatch
} from '../../utils/news-taxonomy.util';

@Component({
  selector: 'app-news-category-select',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './news-category-select.component.html',
  styleUrls: ['./news-category-select.component.scss']
})
export class NewsCategorySelectComponent implements OnInit {
  @Input() categories: NewsCategory[] = [];
  @Output() categoriesChange = new EventEmitter<NewsCategory[]>();
  @Input() disabled = false;

  allCategories: NewsCategory[] = [];
  searchText = '';
  loading = false;

  private catalogRequested = false;

  constructor(
    private newsService: NewsService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.ensureCatalogLoaded();
  }

  get filteredCategories(): NewsCategory[] {
    return filterTagsLocally(this.allCategories, this.searchText, this.categories);
  }

  get canCreateCategory(): boolean {
    const value = this.searchText.trim();
    if (!value) return false;
    if (hasDuplicateTerm(this.categories.map(c => c.label), value)) return false;
    return !this.allCategories.some(c => termsMatch(c.label, value));
  }

  get createCategoryLabel(): string {
    return `Create “${this.searchText.trim()}”`;
  }

  onSearchFocus(): void {
    this.ensureCatalogLoaded();
  }

  ensureCatalogLoaded(): void {
    if (this.catalogRequested || this.disabled) return;
    this.catalogRequested = true;
    this.loading = true;
    this.newsService.getCategories().subscribe({
      next: cats => {
        this.allCategories = cats;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.catalogRequested = false;
        this.messageService.error('Could not load categories. Please try again.');
      }
    });
  }

  isSelected(category: NewsCategory): boolean {
    return this.categories.some(c => c.id === category.id || termsMatch(c.label, category.label));
  }

  toggleCategory(category: NewsCategory): void {
    if (this.disabled) return;

    if (this.isSelected(category)) {
      this.categories = this.categories.filter(
        c => c.id !== category.id && !termsMatch(c.label, category.label)
      );
    } else if (!hasDuplicateTerm(this.categories.map(c => c.label), category.label)) {
      this.categories = [...this.categories, category];
    }
    this.categoriesChange.emit(this.categories);
  }

  removeCategory(category: NewsCategory): void {
    if (this.disabled) return;
    this.categories = this.categories.filter(
      c => c.id !== category.id && !termsMatch(c.label, category.label)
    );
    this.categoriesChange.emit(this.categories);
  }

  createCategoryFromSearch(): void {
    const value = this.searchText.trim();
    if (!value) return;

    const existing = this.allCategories.find(c => termsMatch(c.label, value));
    if (existing) {
      this.toggleCategory(existing);
      this.searchText = '';
      return;
    }

    const pending: NewsCategory = {
      id: createPendingTaxonomyId(value),
      label: value,
      isPending: true
    };
    if (!hasDuplicateTerm(this.categories.map(c => c.label), pending.label)) {
      this.categories = [...this.categories, pending];
      this.categoriesChange.emit(this.categories);
    }
    this.searchText = '';
  }

  isPendingCategory(category: NewsCategory): boolean {
    return isPendingTaxonomyItem(category);
  }

  onOptionSelected(value: string): void {
    if (value.startsWith('__create__:')) {
      this.createCategoryFromSearch();
      return;
    }
    const category = this.allCategories.find(c => c.id === value);
    if (category) {
      this.toggleCategory(category);
      this.searchText = '';
    }
  }

  displayCategoryLabel = (): string => '';
}
