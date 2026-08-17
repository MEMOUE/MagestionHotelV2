import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { TooltipModule } from 'primeng/tooltip';
import { DividerModule } from 'primeng/divider';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService, ConfirmationService } from 'primeng/api';

import { RestaurantService } from '../../../services/restaurant.service';
import { HotelProfileService, HotelProfile } from '../../../services/hotel-profile.service';
import {
  CommandeRestaurant, StatutCommandeRestaurant,
  STATUT_COMMANDE_LABELS, STATUT_COMMANDE_COLORS
} from '../../../models/restaurant.model';

type Severity = "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined;

@Component({
  selector: 'app-listeresto',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    TableModule, ButtonModule, InputTextModule,
    TagModule, CardModule, ToolbarModule, ToastModule,
    ConfirmDialogModule, SelectModule, DatePickerModule,
    TooltipModule, DividerModule, DialogModule, InputNumberModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './listeresto.html',
  styleUrl: './listeresto.css'
})
export class Listeresto implements OnInit {

  commandes: CommandeRestaurant[] = [];
  commandesFiltrees: CommandeRestaurant[] = [];
  loading = false;

  searchValue    = '';
  selectedStatut = '';
  dateDebut: Date | null = null;
  dateFin:   Date | null = null;

  totalCommandes     = 0;
  commandesEnAttente = 0;
  commandesServies   = 0;
  chiffreAffaires    = 0;

  hotelProfile: HotelProfile | null = null;

  // ✅ Remise dialog
  showRemiseDialog = false;
  remiseMontant = 0;
  remiseCommandeId: number | null = null;
  remiseCommandeNumero = '';
  remiseMontantBrut = 0;

  statutOptions = [
    { label: 'Tous les statuts',  value: '' },
    { label: 'En attente',        value: StatutCommandeRestaurant.EN_ATTENTE },
    { label: 'Servie',            value: StatutCommandeRestaurant.SERVIE },
    { label: 'Payée',             value: StatutCommandeRestaurant.PAYEE },
    { label: 'Annulée',           value: StatutCommandeRestaurant.ANNULEE }
  ];

  constructor(
    private restaurantService: RestaurantService,
    private hotelProfileService: HotelProfileService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.hotelProfileService.getProfile().subscribe({
      next: r => { if (r.success) this.hotelProfile = r.data; },
      error: () => {}
    });
    this.loadCommandes();
  }

  loadCommandes(): void {
    this.loading = true;
    this.restaurantService.getCommandes().subscribe({
      next: (response) => {
        if (response.success) {
          this.commandes = response.data;
          this.applyFilters();
        }
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les commandes' });
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.commandes];

    if (this.searchValue.trim()) {
      const s = this.searchValue.toLowerCase().trim();
      filtered = filtered.filter(c =>
        c.numeroCommande?.toLowerCase().includes(s) ||
        c.clientNom?.toLowerCase().includes(s) ||
        c.nomClientExterne?.toLowerCase().includes(s) ||
        c.numeroTable?.toLowerCase().includes(s) ||
        c.serveurNom?.toLowerCase().includes(s)
      );
    }

    if (this.selectedStatut) {
      filtered = filtered.filter(c => c.statut === this.selectedStatut);
    }

    if (this.dateDebut) {
      const debut = new Date(this.dateDebut);
      debut.setHours(0, 0, 0, 0);
      filtered = filtered.filter(c => c.dateCommande && new Date(c.dateCommande) >= debut);
    }

    if (this.dateFin) {
      const fin = new Date(this.dateFin);
      fin.setHours(23, 59, 59, 999);
      filtered = filtered.filter(c => c.dateCommande && new Date(c.dateCommande) <= fin);
    }

    this.commandesFiltrees = filtered;
    this.recalculerStats();
  }

  private recalculerStats(): void {
    const f = this.commandesFiltrees;
    this.totalCommandes     = f.length;
    this.commandesEnAttente = f.filter(c => c.statut === StatutCommandeRestaurant.EN_ATTENTE).length;
    this.commandesServies   = f.filter(c => c.statut === StatutCommandeRestaurant.SERVIE).length;
    this.chiffreAffaires    = f
      .filter(c => c.statut !== StatutCommandeRestaurant.ANNULEE)
      .reduce((sum, c) => sum + c.montantTotal, 0);
  }

  onSearchChange(): void { this.applyFilters(); }
  onStatutChange(): void { this.applyFilters(); }
  onDateChange():   void { this.applyFilters(); }

  clearFilters(): void {
    this.searchValue    = '';
    this.selectedStatut = '';
    this.dateDebut      = null;
    this.dateFin        = null;
    this.applyFilters();
  }

  get hasActiveFilters(): boolean {
    return !!(this.searchValue || this.selectedStatut || this.dateDebut || this.dateFin);
  }

  nouvelleCommande(): void { this.router.navigate(['/restauration/create']); }

  viewCommande(commande: CommandeRestaurant): void {
    if (!commande.id) return;
    this.router.navigate(['/restauration', commande.id]);
  }

  changerStatut(commande: CommandeRestaurant, statut: StatutCommandeRestaurant): void {
    this.confirmationService.confirm({
      message: `Passer la commande à "${STATUT_COMMANDE_LABELS[statut]}" ?`,
      header: 'Confirmation', icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      accept: () => {
        this.restaurantService.updateStatut(commande.id!, statut).subscribe({
          next: r => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour' });
              this.loadCommandes();
            }
          },
          error: e => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.error?.message || 'Impossible de mettre à jour' })
        });
      }
    });
  }

  marquerCommeServie(c: CommandeRestaurant): void { this.changerStatut(c, StatutCommandeRestaurant.SERVIE); }
  marquerCommePayee(c: CommandeRestaurant):  void { this.changerStatut(c, StatutCommandeRestaurant.PAYEE);  }

  ajouterPaiement(commande: CommandeRestaurant): void {
    const restant = this.getMontantRestant(commande);
    this.confirmationService.confirm({
      message: `Enregistrer un paiement de ${restant.toLocaleString('fr-FR')} FCFA ?`,
      header: 'Paiement', icon: 'pi pi-money-bill',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      accept: () => {
        this.restaurantService.addPaiement(commande.id!, restant).subscribe({
          next: r => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Paiement enregistré' });
              this.loadCommandes();
            }
          },
          error: e => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.error?.message || 'Erreur paiement' })
        });
      }
    });
  }

  // ✅ Ouvrir le dialog de remise
  openRemiseDialog(commande: CommandeRestaurant): void {
    this.remiseCommandeId = commande.id!;
    this.remiseCommandeNumero = commande.numeroCommande || '#' + commande.id;
    this.remiseMontant = commande.remiseTotale || 0;
    // Calculer le montant brut (total + remise existante)
    this.remiseMontantBrut = commande.montantTotal + (commande.remiseTotale || 0);
    this.showRemiseDialog = true;
  }

  // ✅ Appliquer la remise
  saveRemise(): void {
    if (!this.remiseCommandeId) return;
    if (this.remiseMontant < 0) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'La remise ne peut pas être négative' });
      return;
    }
    if (this.remiseMontant > this.remiseMontantBrut) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'La remise ne peut pas dépasser le montant brut' });
      return;
    }

    this.restaurantService.appliquerRemise(this.remiseCommandeId, this.remiseMontant).subscribe({
      next: r => {
        if (r.success) {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Remise appliquée avec succès' });
          this.showRemiseDialog = false;
          this.loadCommandes();
        }
      },
      error: e => this.messageService.add({
        severity: 'error', summary: 'Erreur',
        detail: e.error?.message || "Impossible d'appliquer la remise"
      })
    });
  }

  canApplyRemise(c: CommandeRestaurant): boolean {
    return c.statut !== StatutCommandeRestaurant.ANNULEE &&
      c.statut !== StatutCommandeRestaurant.PAYEE;
  }

  // ── Impression (same as before, with remise display) ──

  imprimerRecu(commande: CommandeRestaurant): void {
    const hotel     = this.hotelProfile;
    const hotelNom  = hotel?.name    || 'Hôtel';
    const hotelTel  = hotel?.phone   ? `Tél : ${hotel.phone}`   : '';
    const hotelAdr  = hotel?.address ? hotel.address              : '';
    const hotelMail = hotel?.email   ? `Email : ${hotel.email}`   : '';

    const client      = this.getClientDisplay(commande);
    const dateCmd     = commande.dateCommande
      ? new Date(commande.dateCommande).toLocaleString('fr-FR')
      : new Date().toLocaleString('fr-FR');
    const montantPaye = commande.montantPaye || 0;
    const restant     = this.getMontantRestant(commande);

    const lignesHTML = (commande.lignes || []).map(l => `
      <tr>
        <td>${l.produitNom || 'Produit'}</td>
        <td style="text-align:center">${l.quantite}</td>
        <td style="text-align:right">${l.prixUnitaire.toLocaleString('fr-FR')} FCFA</td>
        <td style="text-align:right">${(l.sousTotal ?? l.quantite * l.prixUnitaire).toLocaleString('fr-FR')} FCFA</td>
      </tr>
      ${l.notes ? `<tr><td colspan="4" style="font-size:11px;color:#666;padding-left:8px;padding-top:0">↳ ${l.notes}</td></tr>` : ''}
    `).join('');

    const remiseHTML = (commande.remiseTotale || 0) > 0
      ? `<div class="totals-row" style="color:#b8860b"><span>Remise</span><span>-${(commande.remiseTotale || 0).toLocaleString('fr-FR')} FCFA</span></div>`
      : '';

    const html = `<!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8">
<title>Reçu ${commande.numeroCommande || commande.id}</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Courier New',monospace;font-size:13px;color:#111;background:#fff;padding:20px;max-width:380px;margin:0 auto}
.header{text-align:center;border-bottom:2px dashed #333;padding-bottom:12px;margin-bottom:12px}
.header h1{font-size:18px;font-weight:900;letter-spacing:1px;text-transform:uppercase}
.header .sub{font-size:11px;color:#555;margin-top:3px;line-height:1.6}
.section-title{font-weight:700;text-transform:uppercase;font-size:11px;letter-spacing:1px;margin:10px 0 4px;color:#333}
.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:2px 8px;font-size:12px;margin-bottom:10px}
.info-grid span:first-child{color:#555}.info-grid span:last-child{font-weight:600;text-align:right}
.divider{border-top:1px dashed #999;margin:10px 0}
table{width:100%;border-collapse:collapse;font-size:12px}
thead tr th{font-size:11px;text-transform:uppercase;border-bottom:1px solid #333;padding:4px 2px}
tbody tr td{padding:5px 2px;vertical-align:top}
.totals{margin-top:10px;border-top:2px solid #333;padding-top:8px}
.totals-row{display:flex;justify-content:space-between;padding:2px 0;font-size:12px}
.totals-row.total-final{font-size:16px;font-weight:900;border-top:1px dashed #333;margin-top:6px;padding-top:6px}
.totals-row.restant{color:#c0392b;font-weight:700}.totals-row.paye{color:#27ae60}
.footer{text-align:center;margin-top:20px;padding-top:10px;border-top:2px dashed #333;font-size:11px;color:#555}
.footer strong{display:block;font-size:13px;color:#111;margin-bottom:4px}
@media print{body{padding:5px;max-width:100%}@page{margin:10mm;size:80mm auto}}
</style></head><body>
<div class="header">
  <h1>${hotelNom}</h1>
  <div class="sub">${hotelAdr ? hotelAdr+'<br>' : ''}${hotelTel ? hotelTel+'<br>' : ''}${hotelMail}</div>
  <div style="margin-top:8px;font-weight:700;font-size:13px">REÇU – ${commande.numeroCommande || '#' + commande.id}</div>
</div>
<div class="section-title">Informations</div>
<div class="info-grid">
  <span>Client</span><span>${client}</span>
  ${commande.numeroTable ? `<span>Table</span><span>${commande.numeroTable}</span>` : ''}
  ${commande.serveurNom  ? `<span>Serveur</span><span>${commande.serveurNom}</span>`  : ''}
  <span>Date</span><span>${dateCmd}</span>
  <span>Statut</span><span>${this.getStatutLabel(commande.statut)}</span>
</div>
<div class="divider"></div>
<div class="section-title">Détail</div>
<table>
  <thead><tr><th style="text-align:left">Article</th><th style="text-align:center">Qté</th><th style="text-align:right">P.U.</th><th style="text-align:right">Total</th></tr></thead>
  <tbody>${lignesHTML}</tbody>
</table>
<div class="totals">
  ${remiseHTML}
  <div class="totals-row total-final"><span>TOTAL</span><span>${commande.montantTotal.toLocaleString('fr-FR')} FCFA</span></div>
  <div class="totals-row paye"><span>Montant payé</span><span>${montantPaye.toLocaleString('fr-FR')} FCFA</span></div>
  ${restant > 0
      ? `<div class="totals-row restant"><span>Reste à payer</span><span>${restant.toLocaleString('fr-FR')} FCFA</span></div>`
      : `<div class="totals-row paye"><span>✓ Entièrement payé</span><span></span></div>`}
</div>
<div class="footer"><strong>Merci de votre commande !</strong><p>${hotelNom}</p><p>Imprimé le ${new Date().toLocaleString('fr-FR')}</p></div>
<script>window.onload=function(){window.print();setTimeout(function(){window.close()},1000)}</script>
</body></html>`;

    const w = window.open('', '_blank', 'width=500,height=700');
    if (w) { w.document.write(html); w.document.close(); }
  }

  getStatutLabel(s: StatutCommandeRestaurant): string { return STATUT_COMMANDE_LABELS[s] || s; }
  getStatutSeverity(s: StatutCommandeRestaurant): Severity { return (STATUT_COMMANDE_COLORS[s] || 'info') as Severity; }

  getClientDisplay(c: CommandeRestaurant): string {
    if (c.clientNom)         return c.clientNom;
    if (c.nomClientExterne)  return c.nomClientExterne;
    if (c.reservationNumero) return `Réservation ${c.reservationNumero}`;
    return 'Client non spécifié';
  }

  getMontantRestant(c: CommandeRestaurant): number {
    return c.montantTotal - (c.montantPaye || 0);
  }

  canMarquerServie(c: CommandeRestaurant):   boolean { return c.statut === StatutCommandeRestaurant.EN_ATTENTE; }
  canMarquerPayee(c: CommandeRestaurant):    boolean { return c.statut === StatutCommandeRestaurant.SERVIE; }
  canAjouterPaiement(c: CommandeRestaurant): boolean {
    return this.getMontantRestant(c) > 0 && c.statut !== StatutCommandeRestaurant.ANNULEE;
  }
}
