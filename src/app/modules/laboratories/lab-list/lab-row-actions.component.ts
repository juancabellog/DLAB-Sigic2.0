import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Laboratory, LAB_STATUS } from '../models/laboratory.models';

@Component({
  selector: 'app-lab-row-actions',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './lab-row-actions.component.html',
  styleUrls: ['./lab-row-actions.component.scss']
})
export class LabRowActionsComponent {
  @Input({ required: true }) item!: Laboratory;
  @Input() canDelete = false;

  @Output() view = new EventEmitter<void>();
  @Output() edit = new EventEmitter<void>();
  @Output() duplicate = new EventEmitter<void>();
  @Output() activate = new EventEmitter<void>();
  @Output() deactivate = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();

  get isActive(): boolean {
    return this.item.status === LAB_STATUS.ACTIVE;
  }
}
