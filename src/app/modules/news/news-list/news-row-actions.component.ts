import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NewsItem, PUBLICATION_STATUS } from '../models/news.models';

@Component({
  selector: 'app-news-row-actions',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './news-row-actions.component.html',
  styleUrls: ['./news-row-actions.component.scss']
})
export class NewsRowActionsComponent {
  @Input({ required: true }) item!: NewsItem;

  @Output() view = new EventEmitter<void>();
  @Output() edit = new EventEmitter<void>();
  @Output() preview = new EventEmitter<void>();
  @Output() duplicate = new EventEmitter<void>();
  @Output() publish = new EventEmitter<void>();
  @Output() unpublish = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();

  get isPublished(): boolean {
    return this.item.publicationStatus === PUBLICATION_STATUS.PUBLISHED;
  }
}
