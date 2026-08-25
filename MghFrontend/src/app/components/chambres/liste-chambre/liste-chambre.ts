import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

// Services et modèles
import { ChambreService, Chambre } from '../../../services/chambre.service';
import { AuthService } from '../../../services/auth.service';
import { ReservationService } from '../../../services/reservation.service';
import { Reservation, StatutReservation } from '../../../models/reservation.model';
import {
  TypeChambre,
  StatutChambre,
  TYPE_CHAMBRE_LABELS,
  STATUT_CHAMBRE_LABELS
} from '../../../models/hotel.model';

// Statuts de réservation considérés comme "chambre occupée" pour le calcul du taux
const STATUTS_OCCUPATION = [
  StatutReservation.CONFIRMEE,
  StatutReservation.EN_COURS,
  StatutReservation.TERMINEE
];

@Component({
  selector: 'app-liste-chambres',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    DialogModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './liste-chambre.html'
})
export class ListeChambres implements OnInit, OnDestroy {

  chambres: Chambre[] = [];
  reservations: Reservation[] = [];

  // Période pour le taux d'occupation (par défaut : aujourd'hui)
  dateDebut: string = this.toInputDate(new Date());
  dateFin: string = this.toInputDate(new Date());
  filteredChambres: Chambre[] = [];
  loading = false;
  errorMessage = '';

  // Filtres
  searchTerm = '';
  selectedType: string | null = null;
  selectedStatut: string | null = null;
  selectedEtage: number | null = null;
  selectedDisponibilite: boolean | null = null;
  prixMin: number | null = null;
  prixMax: number | null = null;

  // Options pour les dropdowns
  typesChambres: any[] = [];
  statuts: any[] = [];
  etages: any[] = [];
  disponibiliteOptions = [
    { label: 'Disponible', value: true },
    { label: 'Non disponible', value: false }
  ];

  // Dialog de suppression
  showDeleteDialog = false;
  chambreToDelete: Chambre | null = null;
  deleting = false;

  private destroy$ = new Subject<void>();
  private hotelId: number | null = null;

  constructor(
    private chambreService: ChambreService,
    private reservationService: ReservationService,
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initializeOptions();
    this.loadHotelId();
    this.loadChambres();
    this.loadReservations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadHotelId(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser?.hotelId) {
      this.hotelId = currentUser.hotelId;
    }
  }

  private initializeOptions(): void {
    // Types de chambres
    this.typesChambres = Object.entries(TYPE_CHAMBRE_LABELS).map(([value, label]) => ({
      label,
      value
    }));

    // Statuts
    this.statuts = Object.entries(STATUT_CHAMBRE_LABELS).map(([value, label]) => ({
      label,
      value
    }));

    // Étages (de 0 à 10)
    this.etages = Array.from({ length: 11 }, (_, i) => ({
      label: i === 0 ? 'Rez-de-chaussée' : `Étage ${i}`,
      value: i
    }));
  }

  loadChambres(): void {
    this.loading = true;
    this.errorMessage = '';

    const filters = this.hotelId ? { hotelId: this.hotelId } : undefined;

    this.chambreService.getChambres(filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (chambres) => {
          this.chambres = chambres;
          this.applyFilters();
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des chambres:', error);
          this.errorMessage = 'Impossible de charger les chambres. Veuillez réessayer.';
          this.loading = false;
          this.showError('Erreur de chargement', this.errorMessage);
        }
      });
  }

  loadReservations(): void {
    this.reservationService.getAllReservations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => {
          const raw = (resp as any)?.data ?? resp;
          this.reservations = Array.isArray(raw) ? raw : [];
        },
        error: (error) => {
          console.error('Erreur lors du chargement des réservations:', error);
        }
      });
  }

  applyFilters(): void {
    let filtered = [...this.chambres];

    // Filtre de recherche
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(c =>
        c.numero.toLowerCase().includes(term) ||
        c.description?.toLowerCase().includes(term) ||
        this.getTypeLabel(c.type).toLowerCase().includes(term)
      );
    }

    // Filtre type
    if (this.selectedType) {
      filtered = filtered.filter(c => c.type === this.selectedType);
    }

    // Filtre statut
    if (this.selectedStatut) {
      filtered = filtered.filter(c => c.statut === this.selectedStatut);
    }

    // Filtre étage
    if (this.selectedEtage !== null) {
      filtered = filtered.filter(c => c.etage === this.selectedEtage);
    }

    // Filtre prix
    if (this.prixMin !== null) {
      filtered = filtered.filter(c => c.prixParNuit >= this.prixMin!);
    }
    if (this.prixMax !== null) {
      filtered = filtered.filter(c => c.prixParNuit <= this.prixMax!);
    }

    this.filteredChambres = filtered;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedType = null;
    this.selectedStatut = null;
    this.selectedEtage = null;
    this.selectedDisponibilite = null;
    this.prixMin = null;
    this.prixMax = null;
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return !!(
      this.searchTerm ||
      this.selectedType ||
      this.selectedStatut ||
      this.selectedEtage !== null ||
      this.selectedDisponibilite !== null ||
      this.prixMin !== null ||
      this.prixMax !== null
    );
  }

  get totalChambres(): number {
    return this.chambres.length;
  }

  // ── Taux d'occupation ────────────────────────────────────────────────────────

  /** Nombre moyen de chambres occupées sur la période sélectionnée (arrondi). */
  get chambresOccupees(): number {
    const jours = this.joursPeriode();
    if (jours.length === 0 || this.chambres.length === 0) return 0;
    const totalChambreJours = jours.reduce((sum, jour) => sum + this.chambresOccupeesLe(jour), 0);
    return Math.round(totalChambreJours / jours.length);
  }

  /** Taux d'occupation sur la période sélectionnée = chambres occupées / total des chambres (arrondi, en %). */
  get tauxOccupation(): number {
    const jours = this.joursPeriode();
    if (jours.length === 0 || this.chambres.length === 0) return 0;
    const totalPossible = this.chambres.length * jours.length;
    const totalOccupe = jours.reduce((sum, jour) => sum + this.chambresOccupeesLe(jour), 0);
    return Math.round((totalOccupe / totalPossible) * 100);
  }

  onPeriodeChange(): void {
    if (this.dateDebut && this.dateFin && this.dateFin < this.dateDebut) {
      this.dateFin = this.dateDebut;
    }
  }

  private toInputDate(d: Date): string {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  /** Liste des jours (minuit) compris entre dateDebut et dateFin, inclus. */
  private joursPeriode(): Date[] {
    if (!this.dateDebut || !this.dateFin) return [];
    const start = new Date(this.dateDebut + 'T00:00:00');
    const end = new Date(this.dateFin + 'T00:00:00');
    if (isNaN(start.getTime()) || isNaN(end.getTime()) || end < start) return [];

    const jours: Date[] = [];
    const cur = new Date(start);
    while (cur <= end) {
      jours.push(new Date(cur));
      cur.setDate(cur.getDate() + 1);
    }
    return jours;
  }

  /** Nombre de chambres occupées (via réservations) pour un jour donné. */
  private chambresOccupeesLe(jour: Date): number {
    const chambresOccupeesIds = new Set<number>();
    for (const res of this.reservations) {
      if (!res.statut || !STATUTS_OCCUPATION.includes(res.statut)) continue;
      const arr = new Date(res.dateArrivee); arr.setHours(0, 0, 0, 0);
      const dep = new Date(res.dateDepart); dep.setHours(0, 0, 0, 0);
      if (jour >= arr && jour < dep) {
        chambresOccupeesIds.add(res.chambreId);
      }
    }
    return chambresOccupeesIds.size;
  }

  getTypeLabel(type: string): string {
    return TYPE_CHAMBRE_LABELS[type as TypeChambre] || type;
  }

  getStatutLabel(statut: string): string {
    return STATUT_CHAMBRE_LABELS[statut as StatutChambre] || statut;
  }

  navigateToCreate(): void {
    this.router.navigate(['/chambres/create']);
  }

  viewDetails(id: number): void {
    this.router.navigate(['/chambres/', id, 'detail']);
  }

  editChambre(id: number): void {
    this.router.navigate(['/chambres', id, 'edit']);
  }

  confirmDelete(chambre: Chambre): void {
    this.chambreToDelete = chambre;
    this.showDeleteDialog = true;
  }

  deleteChambre(): void {
    if (!this.chambreToDelete?.id) return;

    this.deleting = true;

    this.chambreService.deleteChambre(this.chambreToDelete.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccess('Succès', `Chambre ${this.chambreToDelete!.numero} supprimée avec succès`);
          this.showDeleteDialog = false;
          this.chambreToDelete = null;
          this.deleting = false;
          this.loadChambres();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.showError('Erreur', 'Impossible de supprimer la chambre');
          this.deleting = false;
        }
      });
  }

  private showSuccess(summary: string, detail: string): void {
    this.messageService.add({
      severity: 'success',
      summary,
      detail,
      life: 3000
    });
  }

  private showError(summary: string, detail: string): void {
    this.messageService.add({
      severity: 'error',
      summary,
      detail,
      life: 5000
    });
  }
}
