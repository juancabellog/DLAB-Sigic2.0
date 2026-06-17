import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { MessageService } from '../../../../core/services/message.service';
import { NewsRelatedPostsService } from '../../services/news-related-posts.service';
import {
  MAX_RELATED_POSTS,
  NewsItem,
  NewsRelatedPost
} from '../../models/news.models';
import {
  RELATED_POST_FILTER_LABELS,
  RELATED_POST_FILTER_OPTIONS,
  RelatedPostStatusFilter,
  buildRelatedPostMetadataLine,
  getRelatedPostDisplayStatus,
  mapNewsItemToRelatedPost
} from '../../utils/news-related-posts.util';

export interface NewsRelatedPostsPickerDialogData {
  currentNewsId: number | null;
  selectedPosts: NewsRelatedPost[];
}

@Component({
  selector: 'app-news-related-posts-picker-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './news-related-posts-picker-dialog.component.html',
  styleUrls: ['./news-related-posts-picker-dialog.component.scss']
})
export class NewsRelatedPostsPickerDialogComponent implements OnInit {
  loading = false;
  searchText = '';
  statusFilter: RelatedPostStatusFilter = 'all';
  allPosts: NewsItem[] = [];
  draftSelection: NewsRelatedPost[] = [];

  readonly maxRelatedPosts = MAX_RELATED_POSTS;
  readonly filterOptions = RELATED_POST_FILTER_OPTIONS;
  readonly filterLabels = RELATED_POST_FILTER_LABELS;

  constructor(
    private dialogRef: MatDialogRef<NewsRelatedPostsPickerDialogComponent, NewsRelatedPost[] | null>,
    @Inject(MAT_DIALOG_DATA) public data: NewsRelatedPostsPickerDialogData,
    private relatedPostsService: NewsRelatedPostsService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.draftSelection = [...(this.data.selectedPosts || [])];
    this.loadPosts();
  }

  get filteredPosts(): NewsItem[] {
    return this.relatedPostsService.searchNewsPosts(
      this.allPosts,
      this.searchText,
      this.statusFilter,
      this.data.currentNewsId
    );
  }

  get footerSelectionText(): string {
    return `${this.draftSelection.length} of ${this.maxRelatedPosts} posts selected`;
  }

  isSelected(post: NewsItem): boolean {
    return this.draftSelection.some(selected => selected.id === post.id);
  }

  isSelectionDisabled(post: NewsItem): boolean {
    return !this.isSelected(post) && this.draftSelection.length >= this.maxRelatedPosts;
  }

  getPostTitle(post: NewsItem): string {
    return post.titleEs?.trim() || post.titleEn?.trim() || `News #${post.id}`;
  }

  getPostMetadata(post: NewsItem): string {
    return buildRelatedPostMetadataLine(
      {
        publicationDate: post.publicationDate || post.publishedAt,
        author: post.author
      },
      getRelatedPostDisplayStatus(post)
    );
  }

  getThumbnailUrl(post: NewsItem): string | null {
    return this.relatedPostsService.resolveThumbnailUrl(post.mainImageUrl);
  }

  onRowClick(post: NewsItem): void {
    this.togglePost(post);
  }

  onCheckboxChange(post: NewsItem, event: MatCheckboxChange): void {
    const shouldSelect = event.checked;
    const currentlySelected = this.isSelected(post);
    if (shouldSelect && !currentlySelected) {
      this.togglePost(post);
    } else if (!shouldSelect && currentlySelected) {
      this.togglePost(post);
    }
  }

  togglePost(post: NewsItem): void {
    if (this.isSelected(post)) {
      this.draftSelection = this.draftSelection.filter(selected => selected.id !== post.id);
      return;
    }
    if (this.draftSelection.length >= this.maxRelatedPosts) {
      this.messageService.warn('You can select up to 3 related posts.');
      return;
    }
    this.draftSelection = [...this.draftSelection, mapNewsItemToRelatedPost(post)];
  }

  close(): void {
    this.dialogRef.close(null);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  done(): void {
    this.dialogRef.close(this.draftSelection);
  }

  private loadPosts(): void {
    this.loading = true;
    this.relatedPostsService.getRelatedPostCandidates(this.data.currentNewsId).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: posts => {
        this.allPosts = posts;
      },
      error: () => {
        this.messageService.error('Could not load news posts for related posts picker.');
      }
    });
  }
}
