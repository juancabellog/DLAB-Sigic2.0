import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { RRHHDTO, TipoRRHHDTO } from '../../../core/models/backend-dtos';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface CreateResearcherDialogData {
  // No data needed for now, but can be extended
}

@Component({
  selector: 'app-create-researcher-dialog',
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
    MatIconModule
  ],
  templateUrl: './create-researcher-dialog.component.html',
  styleUrls: ['./create-researcher-dialog.component.scss']
})
export class CreateResearcherDialogComponent implements OnInit {
  createForm: FormGroup;
  loading = false;
  researcherTypes: TipoRRHHDTO[] = [];
  loadingTypes = false;
  duplicateError: string | null = null;
  existingResearcher: RRHHDTO | null = null;

  constructor(
    public dialogRef: MatDialogRef<CreateResearcherDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateResearcherDialogData,
    private formBuilder: FormBuilder,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private snackBar: MatSnackBar
  ) {
    this.createForm = this.formBuilder.group({
      fullName: ['', [Validators.required]],
      email: ['', [Validators.email]],
      orcid: ['', [this.orcidValidator.bind(this)]],
      rut: [''],
      rrhhTypeId: [null]
    });
  }

  ngOnInit(): void {
    this.loadResearcherTypes();
    
    // Watch ORCID changes to check for duplicates
    this.createForm.get('orcid')?.valueChanges.subscribe(() => {
      this.duplicateError = null;
      this.existingResearcher = null;
    });
  }

  loadResearcherTypes(): void {
    this.loadingTypes = true;
    this.baseHttp.get<TipoRRHHDTO[]>('/researchers/types').pipe(
      catchError(error => {
        console.error('Error loading researcher types:', error);
        return of([]);
      })
    ).subscribe(types => {
      this.researcherTypes = types;
      this.loadingTypes = false;
    });
  }

  orcidValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value || control.value.trim() === '') {
      return null; // ORCID is optional
    }
    
    const orcid = control.value.trim();
    // ORCID format: XXXX-XXXX-XXXX-XXXX (19 characters with hyphens)
    const orcidPattern = /^\d{4}-\d{4}-\d{4}-\d{3}[\dX]$/;
    
    if (!orcidPattern.test(orcid)) {
      return { invalidOrcid: true };
    }
    
    return null;
  }

  checkOrcidDuplicate(): void {
    const orcid = this.createForm.get('orcid')?.value?.trim();
    if (!orcid || this.createForm.get('orcid')?.hasError('invalidOrcid')) {
      this.duplicateError = null;
      this.existingResearcher = null;
      return;
    }

    // Normalize ORCID (remove URL if present)
    let normalizedOrcid = orcid;
    if (normalizedOrcid.includes('/')) {
      normalizedOrcid = normalizedOrcid.substring(normalizedOrcid.lastIndexOf('/') + 1);
    }

    // Search for existing researcher with this ORCID
    this.researcherService.searchResearchers(normalizedOrcid).subscribe({
      next: (researchers) => {
        const found = researchers.find(r => r.orcid === normalizedOrcid);
        if (found) {
          this.duplicateError = `Researcher with ORCID ${normalizedOrcid} already exists.`;
          this.existingResearcher = found;
        } else {
          this.duplicateError = null;
          this.existingResearcher = null;
        }
      },
      error: () => {
        // If search fails, don't block creation
        this.duplicateError = null;
        this.existingResearcher = null;
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSubmit(): void {
    if (this.createForm.invalid) {
      // Mark all fields as touched to show validation errors
      Object.keys(this.createForm.controls).forEach(key => {
        this.createForm.get(key)?.markAsTouched();
      });
      return;
    }

    if (this.duplicateError && !this.existingResearcher) {
      // If there's a duplicate error but we don't have the existing researcher, don't proceed
      return;
    }

    this.loading = true;
    const formValue = this.createForm.value;

    const createRequest = {
      fullName: formValue.fullName.trim(),
      email: formValue.email?.trim() || null,
      orcid: formValue.orcid?.trim() || null,
      rut: formValue.rut?.trim() || null,
      rrhhTypeId: formValue.rrhhTypeId || null
    };

    this.researcherService.createResearcher(createRequest).subscribe({
      next: (createdResearcher) => {
        // Si el fullname no está disponible (campo calculado), recargar el investigador
        if (!createdResearcher.fullname && createdResearcher.id) {
          this.researcherService.getResearcher(createdResearcher.id).subscribe({
            next: (reloadedResearcher) => {
              this.loading = false;
              this.snackBar.open('Researcher created and added.', 'Close', { duration: 3000 });
              this.dialogRef.close(reloadedResearcher);
            },
            error: (error) => {
              // Si falla la recarga, usar el investigador original
              console.warn('Could not reload researcher, using original:', error);
              this.loading = false;
              this.snackBar.open('Researcher created and added.', 'Close', { duration: 3000 });
              this.dialogRef.close(createdResearcher);
            }
          });
        } else {
          this.loading = false;
          this.snackBar.open('Researcher created and added.', 'Close', { duration: 3000 });
          this.dialogRef.close(createdResearcher);
        }
      },
      error: (error) => {
        this.loading = false;
        const errorMessage = error.error?.message || error.error || 'Failed to create researcher. Please try again.';
        
        // Check if it's a duplicate ORCID error
        if (errorMessage.includes('ORCID') && errorMessage.includes('already exists')) {
          this.duplicateError = errorMessage;
          // Try to extract existing researcher info from error message
          const match = errorMessage.match(/already exists: (.+)/);
          if (match) {
            // We can't get the full researcher from the error, but we can show the message
          }
        } else {
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  useExistingResearcher(): void {
    if (this.existingResearcher) {
      this.dialogRef.close(this.existingResearcher);
    }
  }
}

