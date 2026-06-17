import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  InjectFlags,
  Injector,
  Input,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, NgControl } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';

import {
  SEGMENT_NONE_ID,
  SEGMENT_OPTIONS,
  SegmentOption
} from '../models/transfer-products.models';

@Component({
  selector: 'app-segment-selector',
  standalone: true,
  imports: [CommonModule, MatCheckboxModule],
  templateUrl: './segment-selector.component.html',
  styleUrl: './segment-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SegmentSelectorComponent),
      multi: true
    }
  ]
})
export class SegmentSelectorComponent implements ControlValueAccessor, AfterViewInit, OnDestroy {
  /** When true, show validation errors as if the control were touched (e.g. submit attempt). */
  @Input() forceShowErrors = false;

  /** Resolved after view init — avoids circular DI with `NG_VALUE_ACCESSOR` on the same host. */
  private ngControl: NgControl | null = null;
  private readonly destroy$ = new Subject<void>();

  readonly groupedRoots: readonly SegmentOption[] = SEGMENT_OPTIONS.filter(
    o => (o.children?.length ?? 0) > 0
  );
  readonly standaloneLeaves: readonly SegmentOption[] = SEGMENT_OPTIONS.filter(
    o => (o.children?.length ?? 0) === 0
  );

  private value: string[] = [];
  private disabled = false;
  private onChange: (v: string[]) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    private readonly injector: Injector,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    this.ngControl = this.injector.get(NgControl, null, InjectFlags.Optional | InjectFlags.Self);
    const c = this.ngControl?.control;
    if (c) {
      c.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.cdr.markForCheck());
    }
    this.cdr.markForCheck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(obj: string[] | null | undefined): void {
    this.value = Array.isArray(obj) ? [...obj] : [];
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (v: string[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cdr.markForCheck();
  }

  isChecked(id: string): boolean {
    return this.value.includes(id);
  }

  isLeafDisabled(id: string): boolean {
    if (this.disabled) return true;
    const noneOn = this.value.includes(SEGMENT_NONE_ID);
    if (noneOn) {
      return id !== SEGMENT_NONE_ID;
    }
    return false;
  }

  onLeafChange(id: string, event: MatCheckboxChange): void {
    if (this.disabled) return;
    this.touchIfNeeded();
    const checked = event.checked;
    let next: string[];

    if (id === SEGMENT_NONE_ID) {
      next = checked ? [SEGMENT_NONE_ID] : [];
    } else if (checked) {
      next = [...this.value.filter(s => s !== SEGMENT_NONE_ID), id];
    } else {
      next = this.value.filter(s => s !== id);
    }

    this.setValue(next);
  }

  showError(): boolean {
    const c = this.ngControl?.control;
    if (!c) return false;
    return c.invalid && (c.touched || this.forceShowErrors);
  }

  hasRequiredError(): boolean {
    return !!this.ngControl?.control?.hasError('required');
  }

  private touchIfNeeded(): void {
    this.onTouched();
    this.ngControl?.control?.markAsTouched();
  }

  private setValue(next: string[]): void {
    this.value = next;
    this.onChange([...this.value]);
    this.cdr.markForCheck();
  }
}
