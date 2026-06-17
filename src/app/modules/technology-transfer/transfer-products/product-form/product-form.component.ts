import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { ProductFormGroup } from '../models/transfer-products.form-types';
import { SegmentSelectorComponent } from '../segment-selector/segment-selector.component';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    SegmentSelectorComponent
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductFormComponent {
  @Input({ required: true }) form!: ProductFormGroup;
  @Input() showErrors = false;

  readonly maxDescriptionLength = 500;

  get nameControl() {
    return this.form.controls.name;
  }

  get descriptionControl() {
    return this.form.controls.description;
  }

  shouldShow(controlTouched: boolean): boolean {
    return this.showErrors || controlTouched;
  }
}
