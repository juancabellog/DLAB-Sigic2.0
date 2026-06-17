import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { formatSegmentSelectionSummary } from '../models/transfer-products.models';
import { ProductFormGroup } from '../models/transfer-products.form-types';
import { ProductFormComponent } from '../product-form/product-form.component';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    ProductFormComponent
  ],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCardComponent {
  @Input({ required: true }) form!: ProductFormGroup;
  @Input() index = 0;
  @Input() expanded = false;
  @Input() showErrors = false;

  @Output() toggle = new EventEmitter<void>();
  @Output() duplicate = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();

  get title(): string {
    const value = this.form.controls.name.value?.trim();
    return value && value.length > 0 ? value : 'Unnamed product';
  }

  get segmentSummary(): string {
    return formatSegmentSelectionSummary(this.form.controls.segments.value);
  }

  get isComplete(): boolean {
    return this.form.valid;
  }

  onSummaryToggle(): void {
    this.toggle.emit();
  }

  onSummaryKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.toggle.emit();
    }
  }
}
