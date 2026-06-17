import { FormControl, FormGroup } from '@angular/forms';

export interface ProductFormModel {
  id: FormControl<string | null>;
  name: FormControl<string>;
  description: FormControl<string>;
  segments: FormControl<string[]>;
}

export type ProductFormGroup = FormGroup<ProductFormModel>;
