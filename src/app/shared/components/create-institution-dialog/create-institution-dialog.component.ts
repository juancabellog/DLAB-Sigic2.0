import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { InstitucionDTO, PaisDTO } from '../../../core/models/backend-dtos';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface CreateInstitutionDialogData {
  /** Prefills Name from the combo search text */
  initialName?: string;
  /** Client-side list for case-insensitive duplicate detection */
  existingInstitutions?: InstitucionDTO[];
}

@Component({
  selector: 'app-create-institution-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './create-institution-dialog.component.html',
  styleUrls: ['./create-institution-dialog.component.scss']
})
export class CreateInstitutionDialogComponent implements OnInit {
  createForm: FormGroup;
  loading = false;
  loadingCountries = false;
  countries: PaisDTO[] = [];
  duplicateError: string | null = null;
  existingInstitution: InstitucionDTO | null = null;

  constructor(
    public dialogRef: MatDialogRef<CreateInstitutionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateInstitutionDialogData,
    private formBuilder: FormBuilder,
    private baseHttp: BaseHttpService,
    private snackBar: MatSnackBar
  ) {
    this.createForm = this.formBuilder.group({
      name: [data?.initialName?.trim() || '', [Validators.required, Validators.maxLength(100)]],
      codigoPais: [null as string | null, [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadCountries();
    this.createForm.get('name')?.valueChanges.subscribe(() => this.checkDuplicateName());
    this.checkDuplicateName();
  }

  loadCountries(): void {
    this.loadingCountries = true;
    this.baseHttp.get<PaisDTO[]>('/catalogs/countries').pipe(
      catchError(() => of([]))
    ).subscribe(countries => {
      this.countries = (countries || []).slice().sort((a, b) =>
        (a.idDescripcion || a.codigo || '').localeCompare(b.idDescripcion || b.codigo || '')
      );
      this.loadingCountries = false;
    });
  }

  private normalize(text: string): string {
    return (text || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  checkDuplicateName(): void {
    const name = (this.createForm.get('name')?.value || '').trim();
    if (!name) {
      this.duplicateError = null;
      this.existingInstitution = null;
      return;
    }

    const normalized = this.normalize(name);
    const list = this.data?.existingInstitutions || [];
    const found = list.find(inst => {
      const label = this.normalize(inst.descripcion || inst.idDescripcion || '');
      return label === normalized;
    }) || null;

    if (found) {
      this.duplicateError = 'This institution already exists.';
      this.existingInstitution = found;
    } else {
      this.duplicateError = null;
      this.existingInstitution = null;
    }
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  useExistingInstitution(): void {
    if (this.existingInstitution) {
      this.dialogRef.close(this.existingInstitution);
    }
  }

  getCountryLabel(country: PaisDTO): string {
    return country.idDescripcion || country.codigo || '';
  }

  onSubmit(): void {
    this.checkDuplicateName();
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(key => {
        this.createForm.get(key)?.markAsTouched();
      });
      return;
    }
    if (this.duplicateError) {
      return;
    }

    const name = (this.createForm.get('name')?.value || '').trim();
    const codigoPais = this.createForm.get('codigoPais')?.value as string;

    this.loading = true;
    const payload: InstitucionDTO = {
      descripcion: name,
      codigoPais
    };

    this.baseHttp.post<InstitucionDTO>('/catalogs/institutions', payload).subscribe({
      next: (created) => {
        this.loading = false;
        if (created && !created.descripcion) {
          created.descripcion = name;
        }
        if (created && !created.countryLabel) {
          const c = this.countries.find(x => x.codigo === codigoPais);
          created.countryLabel = c?.idDescripcion;
          created.codigoPais = codigoPais;
        }
        this.snackBar.open('Institution created.', 'Close', { duration: 3000 });
        this.dialogRef.close(created);
      },
      error: (error) => {
        this.loading = false;
        const message = error?.error?.message || 'Failed to create institution. Please try again.';
        const existing = error?.error?.existing as InstitucionDTO | undefined;
        if (error?.status === 409 || (typeof message === 'string' && message.toLowerCase().includes('already exists'))) {
          this.duplicateError = typeof message === 'string' ? message : 'This institution already exists.';
          this.existingInstitution = existing || this.existingInstitution;
        } else {
          this.snackBar.open(message, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }
}
