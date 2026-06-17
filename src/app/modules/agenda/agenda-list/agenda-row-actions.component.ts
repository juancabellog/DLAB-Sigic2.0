import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AgendaEvent, PUBLICATION_STATUS } from '../models/agenda.models';

@Component({
  selector: 'app-agenda-row-actions',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './agenda-row-actions.component.html',
  styleUrls: ['./agenda-row-actions.component.scss']
})
export class AgendaRowActionsComponent {
  @Input({ required: true }) item!: AgendaEvent;

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
