import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  @ViewChild('identifierInput') identifierInput?: ElementRef<HTMLInputElement>;

  form: FormGroup;
  loading = false;
  submitted = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.form = this.formBuilder.group({
      identifier: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const identifier = (this.form.value.identifier || '').trim();

    this.authService.forgotPassword(identifier).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open(
          'We could not process your request. Please try again later.',
          'Close',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }

  backToSignIn(): void {
    this.router.navigate(['/login']);
  }

  tryAnotherIdentifier(): void {
    this.submitted = false;
    this.loading = false;
    this.form.reset({ identifier: '' });
    this.form.markAsUntouched();
    this.form.markAsPristine();

    setTimeout(() => {
      this.identifierInput?.nativeElement?.focus();
    });
  }
}
