import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
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
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  loading = false;
  success = false;
  invalidLink = false;
  hidePassword = true;
  hideConfirmPassword = true;
  private token = '';

  constructor(
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.form = this.formBuilder.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required, this.passwordMatchValidator.bind(this)]]
    });

    this.form.get('newPassword')?.valueChanges.subscribe(() => {
      this.form.get('confirmPassword')?.updateValueAndValidity({ emitEvent: false });
    });
  }

  ngOnInit(): void {
    const queryToken = this.route.snapshot.queryParamMap.get('token');
    const pathToken = this.route.snapshot.paramMap.get('token');
    this.token = (queryToken || pathToken || '').trim();
    this.invalidLink = !this.token;
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = this.form?.get('newPassword')?.value;
    const confirmPassword = control.value;
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }

  onSubmit(): void {
    if (this.invalidLink) {
      return;
    }
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.authService.resetPassword(this.token, this.form.value.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (error) => {
        this.loading = false;
        const backendMessage = error?.error?.message || '';
        if (typeof backendMessage === 'string'
          && (backendMessage.toLowerCase().includes('invalid')
            || backendMessage.toLowerCase().includes('expired'))) {
          this.invalidLink = true;
          return;
        }
        if (typeof backendMessage === 'string' && backendMessage.toLowerCase().includes('password')) {
          this.snackBar.open(backendMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        this.snackBar.open(
          'We could not reset your password. Please try again later.',
          'Close',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }

  backToSignIn(): void {
    this.router.navigate(['/login']);
  }
}
