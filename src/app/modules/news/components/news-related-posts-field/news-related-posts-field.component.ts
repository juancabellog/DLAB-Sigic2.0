import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MessageService } from '../../../../core/services/message.service';
import { MAX_RELATED_POSTS, NewsRelatedPost } from '../../models/news.models';
import {
  NewsRelatedPostsPickerDialogComponent,
  NewsRelatedPostsPickerDialogData
} from '../news-related-posts-picker/news-related-posts-picker-dialog.component';

@Component({
  selector: 'app-news-related-posts-field',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './news-related-posts-field.component.html',
  styleUrls: ['./news-related-posts-field.component.scss']
})
export class NewsRelatedPostsFieldComponent {
  @Input() relatedPosts: NewsRelatedPost[] = [];
  @Output() relatedPostsChange = new EventEmitter<NewsRelatedPost[]>();
  @Input() currentNewsId: number | null = null;
  @Input() disabled = false;

  readonly maxRelatedPosts = MAX_RELATED_POSTS;
  readonly fieldTooltip =
    'Select up to 3 related news posts. They appear at the end of the article preview.';

  constructor(
    private dialog: MatDialog,
    private messageService: MessageService
  ) {}

  get selectedCount(): number {
    return this.relatedPosts.length;
  }

  openPicker(): void {
    if (this.disabled) return;

    const data: NewsRelatedPostsPickerDialogData = {
      currentNewsId: this.currentNewsId,
      selectedPosts: [...this.relatedPosts]
    };

    this.dialog
      .open(NewsRelatedPostsPickerDialogComponent, {
        data,
        width: 'min(720px, 96vw)',
        maxWidth: '96vw',
        panelClass: 'news-related-posts-dialog-panel',
        autoFocus: 'input'
      })
      .afterClosed()
      .subscribe(result => {
        if (result == null) return;
        if (result.length > this.maxRelatedPosts) {
          this.messageService.warn('You can select up to 3 related posts.');
          return;
        }
        this.relatedPosts = this.dedupeRelatedPosts(result);
        this.relatedPostsChange.emit(this.relatedPosts);
      });
  }

  removeRelatedPost(post: NewsRelatedPost): void {
    if (this.disabled) return;
    this.relatedPosts = this.relatedPosts.filter(item => item.id !== post.id);
    this.relatedPostsChange.emit(this.relatedPosts);
  }

  private dedupeRelatedPosts(posts: NewsRelatedPost[]): NewsRelatedPost[] {
    const seen = new Set<number>();
    return posts.filter(post => {
      if (!post?.id || seen.has(post.id)) return false;
      seen.add(post.id);
      return true;
    });
  }
}
