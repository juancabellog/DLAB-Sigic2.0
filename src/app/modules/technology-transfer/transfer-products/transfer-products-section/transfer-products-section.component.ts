import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormArray,
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom, Subject, takeUntil } from 'rxjs';

import { ProductFormGroup } from '../models/transfer-products.form-types';
import { TransferProduct } from '../models/transfer-products.models';
import {
  ConfirmDeleteDialogComponent,
  ConfirmDeleteDialogData
} from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { ProductCardComponent } from '../product-card/product-card.component';

@Component({
  selector: 'app-transfer-products-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    ProductCardComponent
  ],
  templateUrl: './transfer-products-section.component.html',
  styleUrl: './transfer-products-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TransferProductsSectionComponent implements OnChanges, OnDestroy {
  @Input() initialProducts: TransferProduct[] = [];
  @Output() productsChange = new EventEmitter<TransferProduct[]>();
  @Output() validChange = new EventEmitter<boolean>();

  readonly productsFormArray: FormArray<ProductFormGroup> = this.fb.array<ProductFormGroup>([]);
  /** Parallel to productsFormArray.controls — new array reference when toggling (OnPush). */
  expandedByIndex: boolean[] = [];
  showErrors = false;

  private readonly destroy$ = new Subject<void>();
  private initialProductsLoaded = false;

  constructor(
    private fb: NonNullableFormBuilder,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {
    this.productsFormArray.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyDuplicateNameErrors();
        this.emitState();
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialProducts'] && !this.initialProductsLoaded) {
      this.initialProductsLoaded = true;
      this.loadInitialProducts(this.initialProducts || []);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByControl(_index: number, control: ProductFormGroup): ProductFormGroup {
    return control;
  }

  addProduct(): void {
    this.productsFormArray.push(this.createProductGroup());
    const n = this.productsFormArray.length;
    this.expandedByIndex = new Array(n).fill(false);
    this.expandedByIndex[n - 1] = true;
    this.cdr.markForCheck();
  }

  duplicateProduct(index: number): void {
    const source = this.productsFormArray.at(index);
    if (!source) return;

    const copy = this.createProductGroup({
      name: source.controls.name.value,
      description: source.controls.description.value,
      segments: [...source.controls.segments.value]
    });
    this.productsFormArray.insert(index + 1, copy);
    const n = this.productsFormArray.length;
    const next = new Array(n).fill(false);
    next[index + 1] = true;
    this.expandedByIndex = next;
    this.cdr.markForCheck();
  }

  async requestDelete(index: number): Promise<void> {
    const dialogData: ConfirmDeleteDialogData = {
      title: 'Delete product',
      message: 'This will remove the associated product. Do you want to continue?'
    };

    const confirmed = await firstValueFrom(
      this.dialog.open(ConfirmDeleteDialogComponent, { data: dialogData, width: '420px' }).afterClosed()
    );

    if (!confirmed) return;

    this.productsFormArray.removeAt(index);
    const next = [...this.expandedByIndex];
    next.splice(index, 1);
    this.expandedByIndex = next;
    this.cdr.markForCheck();
  }

  toggleExpanded(index: number): void {
    if (index < 0 || index >= this.expandedByIndex.length) return;
    const next = [...this.expandedByIndex];
    next[index] = !next[index];
    this.expandedByIndex = next;
    this.cdr.markForCheck();
  }

  markAllAsTouched(): void {
    this.showErrors = true;
    this.productsFormArray.controls.forEach(group => group.markAllAsTouched());
    this.applyDuplicateNameErrors();
    this.emitState();
    this.cdr.markForCheck();
  }

  isValid(): boolean {
    return this.productsFormArray.valid;
  }

  private createProductGroup(product?: Partial<TransferProduct>): ProductFormGroup {
    return this.fb.group({
      id: this.fb.control<string | null>(product?.id ?? this.generateId()),
      name: this.fb.control((product?.name ?? '').trim(), [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(150),
        this.trimmedRequiredValidator
      ]),
      description: this.fb.control(product?.description ?? '', [Validators.maxLength(500)]),
      segments: this.fb.control<string[]>(product?.segments ?? [], [Validators.required])
    });
  }

  private loadInitialProducts(products: TransferProduct[]): void {
    this.productsFormArray.clear();
    this.expandedByIndex = [];
    for (const product of products) {
      this.productsFormArray.push(this.createProductGroup(product));
    }
    const n = this.productsFormArray.length;
    this.expandedByIndex = n === 0 ? [] : new Array(n).fill(false);
    this.applyDuplicateNameErrors();
    this.emitState();
    this.cdr.markForCheck();
  }

  private emitState(): void {
    const products = this.productsFormArray.controls.map(control => ({
      id: control.controls.id.value ?? undefined,
      name: control.controls.name.value.trim(),
      description: control.controls.description.value.trim(),
      segments: [...control.controls.segments.value]
    }));
    this.productsChange.emit(products);
    this.validChange.emit(this.productsFormArray.valid);
  }

  private applyDuplicateNameErrors(): void {
    const normalizedMap = new Map<string, ProductFormGroup[]>();

    this.productsFormArray.controls.forEach(group => {
      const name = this.normalizeName(group.controls.name.value);
      if (!name) return;
      const collection = normalizedMap.get(name) ?? [];
      collection.push(group);
      normalizedMap.set(name, collection);
    });

    const silent = { emitEvent: false } as const;

    this.productsFormArray.controls.forEach(group => {
      const control = group.controls.name;
      const currentErrors = { ...(control.errors || {}) };
      delete currentErrors['duplicateName'];
      if (Object.keys(currentErrors).length === 0) {
        control.setErrors(null, silent);
      } else {
        control.setErrors(currentErrors, silent);
      }
    });

    normalizedMap.forEach(groups => {
      if (groups.length <= 1) return;
      groups.forEach(group => {
        const control = group.controls.name;
        control.setErrors({ ...(control.errors || {}), duplicateName: true }, silent);
      });
    });

    this.productsFormArray.updateValueAndValidity({ emitEvent: false });
  }

  private normalizeName(value: string): string {
    return value.trim().toLowerCase();
  }

  private trimmedRequiredValidator(control: AbstractControl<string | null>) {
    const value = (control.value ?? '').trim();
    return value.length > 0 ? null : { required: true };
  }

  private generateId(): string {
    return `tp-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  }
}
