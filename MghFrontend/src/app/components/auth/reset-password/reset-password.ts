import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPassword implements OnInit {
  form!: FormGroup;
  loading = false;
  validating = true;
  errorMessage = '';
  successMessage = '';
  tokenValid = false;
  token = '';
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      if (!this.token) {
        this.validating = false;
        this.errorMessage = 'Lien invalide. Aucun token de réinitialisation trouvé.';
        return;
      }
      this.validateToken();
    });
  }

  private validateToken(): void {
    this.validating = true;
    this.authService.validateResetToken(this.token).subscribe({
      next: (response) => {
        this.validating = false;
        if (response.success) {
          this.tokenValid = true;
        } else {
          this.errorMessage = response.message || 'Token invalide';
        }
      },
      error: (error) => {
        this.validating = false;
        this.errorMessage = error.error?.message || 'Ce lien est invalide ou a expiré. Veuillez refaire une demande.';
      }
    });
  }

  get passwordsMatch(): boolean {
    return this.form.value.newPassword === this.form.value.confirmPassword;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(k => this.form.get(k)?.markAsTouched());
      return;
    }

    if (!this.passwordsMatch) {
      this.errorMessage = 'Les mots de passe ne correspondent pas';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.resetPassword({
      token: this.token,
      newPassword: this.form.value.newPassword
    }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = response.message || 'Mot de passe réinitialisé avec succès !';
          this.tokenValid = false; // Cache le formulaire
          // Redirection vers login après 3 secondes
          setTimeout(() => {
            this.router.navigate(['/login'], {
              queryParams: { resetSuccess: 'true' }
            });
          }, 3000);
        } else {
          this.errorMessage = response.message || 'Erreur lors de la réinitialisation';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Erreur lors de la réinitialisation du mot de passe';
      }
    });
  }
}
