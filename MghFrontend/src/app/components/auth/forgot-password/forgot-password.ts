import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  form!: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';
  selectedAccountType: 'HOTEL' | 'USER' = 'HOTEL';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  setAccountType(type: 'HOTEL' | 'USER'): void {
    this.selectedAccountType = type;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.get('email')?.markAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.forgotPassword({
      email: this.form.value.email.trim().toLowerCase(),
      accountType: this.selectedAccountType
    }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = response.message || 'Un email de réinitialisation a été envoyé. Vérifiez votre boîte de réception.';
        } else {
          this.errorMessage = response.message || 'Une erreur est survenue';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Impossible d\'envoyer l\'email. Vérifiez votre adresse.';
      }
    });
  }
}
