import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

// Services et modèles
import { ChambreService, Chambre } from '../../../services/chambre.service';
import {
  TypeChambre,
  StatutChambre,
  TYPE_CHAMBRE_LABELS,
  STATUT_CHAMBRE_LABELS
} from '../../../models/hotel.model';

@Component({
  selector: 'app-detail-chambre',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DialogModule,
    SelectModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './detail-chambre.html'
})
export class DetailChambre implements OnInit, OnDestroy {
  chambre: Chambre | null = null;
  loading = false;
  errorMessage = '';
  chambreId: number | null = null;

  // Dialogs
  showStatutDialog = false;
  showDeleteDialog = false;
  deleting = false;

  // Changement de statut
  nouveauStatut: StatutChambre | null = null;
  statuts: any[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private chambreService: ChambreService,
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initializeStatuts();
    this.loadChambreFromRoute();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeStatuts(): void {
    this.statuts = Object.entries(STATUT_CHAMBRE_LABELS).map(([value, label]) => ({
      label,
      value
    }));
  }

  private loadChambreFromRoute(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params['id'];
      if (id) {
        this.chambreId = +id;
        this.loadChambre(this.chambreId);
      }
    });
  }

  private loadChambre(id: number): void {
    this.loading = true;
    this.errorMessage = '';

    this.chambreService.getChambre(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (chambre) => {
          this.chambre = chambre;
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.errorMessage = 'Impossible de charger la chambre';
          this.showError('Erreur', this.errorMessage);
          this.loading = false;
        }
      });
  }

  getTypeLabel(type: string): string {
    return TYPE_CHAMBRE_LABELS[type as TypeChambre] || type;
  }

  getStatutLabel(statut: string): string {
    return STATUT_CHAMBRE_LABELS[statut as StatutChambre] || statut;
  }

  editChambre(): void {
    if (this.chambreId) {
      this.router.navigate(['/chambres', this.chambreId, 'edit']);
    }
  }

  confirmDelete(): void {
    this.showDeleteDialog = true;
  }

  deleteChambre(): void {
    if (!this.chambreId) return;

    this.deleting = true;

    this.chambreService.deleteChambre(this.chambreId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccess('Succès', `Chambre #${this.chambre?.numero} supprimée`);
          this.deleting = false;
          this.showDeleteDialog = false;
          setTimeout(() => this.router.navigate(['/chambres']), 1500);
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.showError('Erreur', 'Impossible de supprimer');
          this.deleting = false;
        }
      });
  }

  showChangeStatutDialog(): void {
    this.nouveauStatut = this.chambre?.statut as StatutChambre || null;
    this.showStatutDialog = true;
  }

  /**
   * ✅ CORRECTION : Utilise le endpoint dédié PUT /api/chambres/{id}/statut
   *    au lieu de PUT /api/chambres/{id} (updateChambre complet).
   *
   *    PUT /{id}/statut exige PERMISSION_VOIR_RESERVATIONS (RECEPTION l'a).
   *    PUT /{id}        exige PERMISSION_MODIFIER_CONFIGURATION (RECEPTION ne l'a PAS).
   */
  changeStatut(): void {
    if (!this.chambreId || !this.nouveauStatut) return;

    this.chambreService.updateStatutChambre(this.chambreId, this.nouveauStatut)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // Recharger la chambre pour avoir les données à jour
          this.loadChambre(this.chambreId!);
          this.showSuccess('Succès', 'Statut mis à jour');
          this.showStatutDialog = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.showError('Erreur', error.error?.message || 'Impossible de changer le statut');
        }
      });
  }

  goBack(): void {
    this.location.back();
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
