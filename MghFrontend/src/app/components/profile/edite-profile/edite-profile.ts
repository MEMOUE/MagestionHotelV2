import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { HotelProfileService, HotelProfile } from '../../../services/hotel-profile.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-edite-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './edite-profile.html',
  styleUrl: './edite-profile.css'
})
export class EditeProfile implements OnInit {
  profileForm!: FormGroup;
  loading = false;
  submitting = false;
  currentUserId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private hotelService: HotelProfileService,
    private authService: AuthService,
    private messageService: MessageService,
    private router: Router
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    // Récupérer l'ID de l'utilisateur connecté
    const currentUser = this.authService.currentUserValue;
    if (currentUser && currentUser.id) {
      this.currentUserId = currentUser.id;
      this.loadProfile();
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Utilisateur non connecté'
      });
      this.router.navigate(['/login']);
    }
  }

  initForm(): void {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      address: [''],
      taxNumber: [''],
      logoUrl: [''],
      ownerEmail: ['', [Validators.email]]
    });
  }

  loadProfile(): void {
    this.loading = true;
    this.hotelService.getProfile().subscribe({
      next: (response) => {
        if (response.success) {
          this.profileForm.patchValue({
            name: response.data.name,
            email: response.data.email,
            phone: response.data.phone || '',
            address: response.data.address || '',
            taxNumber: response.data.taxNumber || '',
            logoUrl: response.data.logoUrl || '',
            ownerEmail: response.data.ownerEmail || ''
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: response.message || 'Erreur lors du chargement du profil'
          });
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du profil:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger le profil'
        });
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.markFormGroupTouched(this.profileForm);
      this.messageService.add({
        severity: 'warn',
        summary: 'Formulaire invalide',
        detail: 'Veuillez corriger les erreurs dans le formulaire'
      });
      return;
    }

    if (!this.currentUserId) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'ID utilisateur non trouvé'
      });
      return;
    }

    this.submitting = true;
    const profileData: Partial<HotelProfile> = this.profileForm.value;

    this.hotelService.updateProfile(this.currentUserId, profileData).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Profil mis à jour avec succès'
          });
          
          // Mettre à jour les données utilisateur dans AuthService si nécessaire
          const currentUser = this.authService.currentUserValue;
          if (currentUser) {
            currentUser.name = response.data.name;
            currentUser.email = response.data.email;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
          }

          // Rediriger vers la page de détails après 1.5 secondes
          setTimeout(() => {
            this.router.navigate(['/profile']);
          }, 1500);
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: response.message || 'Erreur lors de la mise à jour du profil'
          });
        }
        this.submitting = false;
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du profil:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.message || 'Impossible de mettre à jour le profil'
        });
        this.submitting = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/profile']);
  }

  // Méthode utilitaire pour marquer tous les champs comme touchés
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // Helpers pour la validation
  isFieldInvalid(fieldName: string): boolean {
    const field = this.profileForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.profileForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['email']) return 'Email invalide';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
    }
    return '';
  }
}