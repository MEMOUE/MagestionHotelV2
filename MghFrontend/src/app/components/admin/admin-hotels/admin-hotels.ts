import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService, ConfirmationService } from 'primeng/api';

import { AdminHotelService } from '../../../services/admin-hotel.service';
import { HotelProfile } from '../../../services/hotel-profile.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-hotels',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    TagModule,
    CardModule,
    ToolbarModule,
    ToastModule,
    ConfirmDialogModule,
    TooltipModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './admin-hotels.html',
  styleUrl: './admin-hotels.css'
})
export class AdminHotels implements OnInit {
  hotels: HotelProfile[] = [];
  loading = false;
  searchKeyword = '';
  sendingReportId: number | null = null;

  constructor(
    private adminHotelService: AdminHotelService,
    private authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadHotels();
  }

  loadHotels(): void {
    this.loading = true;
    this.adminHotelService.getAllHotels().subscribe({
      next: (response) => {
        if (response.success) {
          this.hotels = response.data;
        }
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les hôtels' });
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    const keyword = this.searchKeyword.trim();
    if (!keyword) {
      this.loadHotels();
      return;
    }
    this.loading = true;
    this.adminHotelService.searchHotels(keyword).subscribe({
      next: (response) => {
        if (response.success) {
          this.hotels = response.data;
        }
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors de la recherche' });
        this.loading = false;
      }
    });
  }

  sendReport(hotel: HotelProfile): void {
    if (!hotel.ownerEmail) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Email manquant',
        detail: `Aucun email de propriétaire n'est configuré pour "${hotel.name}".`
      });
      return;
    }

    this.confirmationService.confirm({
      message: `Envoyer immédiatement le rapport financier du mois écoulé à ${hotel.ownerEmail} ?`,
      header: 'Envoyer le rapport financier',
      icon: 'pi pi-send',
      acceptLabel: 'Oui, envoyer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.sendingReportId = hotel.id!;
        this.adminHotelService.sendFinancialReport(hotel.id!).subscribe({
          next: (response) => {
            this.sendingReportId = null;
            if (response.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Rapport envoyé à ${hotel.ownerEmail}` });
            }
          },
          error: (error) => {
            this.sendingReportId = null;
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || "Impossible d'envoyer le rapport" });
          }
        });
      }
    });
  }

  toggleStatus(hotel: HotelProfile): void {
    const action = hotel.active ? 'désactiver' : 'activer';
    this.confirmationService.confirm({
      message: `Voulez-vous vraiment ${action} le compte de "${hotel.name}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        this.adminHotelService.toggleStatus(hotel.id!).subscribe({
          next: (response) => {
            if (response.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Compte ${action === 'activer' ? 'activé' : 'désactivé'} avec succès` });
              this.loadHotels();
            }
          },
          error: (error) => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Impossible de modifier le statut' });
          }
        });
      }
    });
  }

  deleteHotel(hotel: HotelProfile): void {
    this.confirmationService.confirm({
      message: `Cette action est IRRÉVERSIBLE. Toutes les données de "${hotel.name}" (réservations, chambres, clients, finances, employés...) seront supprimées définitivement. Continuer ?`,
      header: 'Suppression définitive',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui, supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.adminHotelService.deleteHotel(hotel.id!).subscribe({
          next: (response) => {
            if (response.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Compte hôtel supprimé définitivement' });
              this.loadHotels();
            }
          },
          error: (error) => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Impossible de supprimer ce compte' });
          }
        });
      }
    });
  }

  logout(): void {
    this.authService.logout('/admin/login');
  }
}
