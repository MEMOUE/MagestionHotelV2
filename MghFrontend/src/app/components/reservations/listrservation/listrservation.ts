import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';

import { MessageService, ConfirmationService } from 'primeng/api';

import {
  Reservation,
  StatutReservation,
  StatutPaiement,
  STATUT_RESERVATION_LABELS,
  STATUT_PAIEMENT_LABELS
} from '../../../models/reservation.model';
import { ReservationService } from '../../../services/reservation.service';

interface StatutOption { label: string; value: StatutReservation | ''; }
type Severity = "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | null | undefined;

@Component({
  selector: 'app-listrservation',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    TableModule, ButtonModule, InputTextModule,
    SelectModule, DatePickerModule, TagModule,
    CardModule, ToolbarModule, ToastModule,
    ConfirmDialogModule, DialogModule, InputNumberModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './listrservation.html',
  styleUrl: './listrservation.css'
})
export class Listrservation implements OnInit {
  reservations: Reservation[] = [];
  filteredReservations: Reservation[] = [];
  loading = false;

  searchText = '';
  selectedStatut: StatutReservation | '' = '';
  dateDebutFilter: Date | null = null;
  dateFinFilter: Date | null = null;

  statutOptions: StatutOption[] = [
    { label: 'Tous les statuts', value: '' },
    { label: 'En attente', value: StatutReservation.EN_ATTENTE },
    { label: 'Confirmée', value: StatutReservation.CONFIRMEE },
    { label: 'En cours', value: StatutReservation.EN_COURS },
    { label: 'Terminée', value: StatutReservation.TERMINEE },
    { label: 'Annulée', value: StatutReservation.ANNULEE },
    { label: 'No-show', value: StatutReservation.NO_SHOW }
  ];

  stats = {
    total: 0, enAttente: 0, confirmees: 0, enCours: 0,
    aujourdhuiArrivees: 0, aujourdhuiDeparts: 0
  };

  // ✅ Remise dialog
  showRemiseDialog = false;
  remiseReservationId: number | null = null;
  remiseReservationNumero = '';
  remiseJournaliere = 0;
  remiseTotale = 0;
  remisePrixParNuit = 0;

  constructor(
    private reservationService: ReservationService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading = true;
    this.reservationService.getAllReservations().subscribe({
      next: (response) => {
        if (response.success) {
          this.reservations = response.data;
          this.filteredReservations = [...this.reservations];
          this.calculateStats();
        }
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors du chargement' });
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredReservations = this.reservations.filter(r => {
      const matchesSearch = !this.searchText ||
        r.numeroReservation?.toLowerCase().includes(this.searchText.toLowerCase()) ||
        r.clientNom?.toLowerCase().includes(this.searchText.toLowerCase()) ||
        r.clientPrenom?.toLowerCase().includes(this.searchText.toLowerCase()) ||
        r.chambreNumero?.toLowerCase().includes(this.searchText.toLowerCase());
      const matchesStatut = !this.selectedStatut || r.statut === this.selectedStatut;
      const matchesDateDebut = !this.dateDebutFilter || new Date(r.dateArrivee) >= this.dateDebutFilter;
      const matchesDateFin = !this.dateFinFilter || new Date(r.dateDepart) <= this.dateFinFilter;
      return matchesSearch && matchesStatut && matchesDateDebut && matchesDateFin;
    });
  }

  clearFilters(): void {
    this.searchText = '';
    this.selectedStatut = '';
    this.dateDebutFilter = null;
    this.dateFinFilter = null;
    this.filteredReservations = [...this.reservations];
  }

  calculateStats(): void {
    this.stats.total = this.reservations.length;
    this.stats.enAttente = this.reservations.filter(r => r.statut === StatutReservation.EN_ATTENTE).length;
    this.stats.confirmees = this.reservations.filter(r => r.statut === StatutReservation.CONFIRMEE).length;
    this.stats.enCours = this.reservations.filter(r => r.statut === StatutReservation.EN_COURS).length;
    const today = new Date().toISOString().split('T')[0];
    this.stats.aujourdhuiArrivees = this.reservations.filter(r => new Date(r.dateArrivee).toISOString().split('T')[0] === today).length;
    this.stats.aujourdhuiDeparts = this.reservations.filter(r => new Date(r.dateDepart).toISOString().split('T')[0] === today).length;
  }

  viewReservation(reservation: Reservation): void {
    this.router.navigate(['/reservation', reservation.id]);
  }

  doCheckin(reservation: Reservation): void {
    if (reservation.statut !== StatutReservation.CONFIRMEE) return;
    this.confirmationService.confirm({
      message: `Confirmer le check-in pour ${reservation.clientPrenom} ${reservation.clientNom} ?`,
      header: 'Confirmation Check-in', icon: 'pi pi-check-circle',
      accept: () => {
        this.reservationService.doCheckin(reservation.id!).subscribe({
          next: (r) => { if (r.success) { this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Check-in effectué' }); this.loadReservations(); }},
          error: (e) => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.message || 'Erreur check-in' })
        });
      }
    });
  }

  doCheckout(reservation: Reservation): void {
    if (reservation.statut !== StatutReservation.EN_COURS) return;
    this.confirmationService.confirm({
      message: `Confirmer le check-out pour ${reservation.clientPrenom} ${reservation.clientNom} ?`,
      header: 'Confirmation Check-out', icon: 'pi pi-sign-out',
      accept: () => {
        this.reservationService.doCheckout(reservation.id!).subscribe({
          next: (r) => { if (r.success) { this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Check-out effectué' }); this.loadReservations(); }},
          error: (e) => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.message || 'Erreur check-out' })
        });
      }
    });
  }

  cancelReservation(reservation: Reservation): void {
    if (reservation.statut === StatutReservation.TERMINEE || reservation.statut === StatutReservation.EN_COURS) return;
    this.confirmationService.confirm({
      message: `Annuler la réservation ${reservation.numeroReservation} ?`,
      header: "Confirmation d'annulation", icon: 'pi pi-exclamation-triangle', acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.reservationService.cancelReservation(reservation.id!).subscribe({
          next: (r) => { if (r.success) { this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Réservation annulée' }); this.loadReservations(); }},
          error: (e) => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.message || "Erreur annulation" })
        });
      }
    });
  }

  createReservation(): void { this.router.navigate(['/reservation/create']); }

  // ✅ Ouvrir le dialog remise
  openRemiseDialog(reservation: Reservation): void {
    this.remiseReservationId = reservation.id!;
    this.remiseReservationNumero = reservation.numeroReservation || '';
    this.remiseJournaliere = reservation.remiseJournaliere || 0;
    this.remiseTotale = reservation.remiseTotale || 0;
    this.remisePrixParNuit = reservation.prixParNuit || 0;
    this.showRemiseDialog = true;
  }

  // ✅ Sauvegarder la remise
  saveRemise(): void {
    if (!this.remiseReservationId) return;
    if (this.remiseJournaliere < 0 || this.remiseTotale < 0) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Les remises ne peuvent pas être négatives' });
      return;
    }
    if (this.remiseJournaliere > this.remisePrixParNuit) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'La remise journalière ne peut pas dépasser le prix par nuit' });
      return;
    }

    this.reservationService.appliquerRemise(this.remiseReservationId, {
      remiseJournaliere: this.remiseJournaliere,
      remiseTotale: this.remiseTotale
    }).subscribe({
      next: (r) => {
        if (r.success) {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Remise appliquée avec succès' });
          this.showRemiseDialog = false;
          this.loadReservations();
        }
      },
      error: (e) => this.messageService.add({
        severity: 'error', summary: 'Erreur',
        detail: e.error?.message || "Impossible d'appliquer la remise"
      })
    });
  }

  canApplyRemise(reservation: Reservation): boolean {
    return reservation.statut !== StatutReservation.ANNULEE &&
      reservation.statut !== StatutReservation.TERMINEE;
  }

  getStatutSeverity(statut: StatutReservation | undefined): Severity {
    switch (statut) {
      case StatutReservation.CONFIRMEE: return 'success';
      case StatutReservation.EN_COURS: return 'info';
      case StatutReservation.EN_ATTENTE: return 'warn';
      case StatutReservation.ANNULEE: case StatutReservation.NO_SHOW: return 'danger';
      case StatutReservation.TERMINEE: return 'secondary';
      default: return 'secondary';
    }
  }

  getStatutPaiementSeverity(statut: StatutPaiement | undefined): Severity {
    switch (statut) {
      case StatutPaiement.PAYE: return 'success';
      case StatutPaiement.ACOMPTE: return 'warn';
      case StatutPaiement.NON_PAYE: return 'danger';
      case StatutPaiement.REMBOURSE: return 'info';
      default: return 'secondary';
    }
  }

  getStatutLabel(statut: StatutReservation | undefined): string {
    return statut ? STATUT_RESERVATION_LABELS[statut] : '';
  }

  getStatutPaiementLabel(statut: StatutPaiement | undefined): string {
    return statut ? STATUT_PAIEMENT_LABELS[statut] : '';
  }

  formatDate(date: string | Date | undefined): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatCurrency(amount: number | undefined): string {
    if (!amount) return '0 FCFA';
    return new Intl.NumberFormat('fr-FR', { style: 'decimal', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(amount) + ' FCFA';
  }
}
