import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-login.html'
})
export class AdminLogin implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isAuthenticated && this.authService.isAdmin) {
      this.router.navigate(['/admin/hotels']);
    }

    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;

    const loginData: LoginRequest = {
      email: this.loginForm.value.email.trim().toLowerCase(),
      password: this.loginForm.value.password,
      accountType: 'ADMIN'
    };

    this.authService.login(loginData).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.router.navigate(['/admin/hotels']);
        } else {
          this.errorMessage = response.message || 'Email ou mot de passe incorrect';
        }
      },
      error: (error) => {
        this.loading = false;
        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.status === 401 || error.status === 400) {
          this.errorMessage = 'Email ou mot de passe incorrect';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de se connecter au serveur.';
        } else {
          this.errorMessage = 'Une erreur s\'est produite. Veuillez réessayer.';
        }
      }
    });
  }
}
