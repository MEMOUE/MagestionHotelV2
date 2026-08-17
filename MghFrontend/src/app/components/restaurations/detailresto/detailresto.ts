import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService, ConfirmationService } from 'primeng/api';

import { RestaurantService } from '../../../services/restaurant.service';
import { HotelProfileService, HotelProfile } from '../../../services/hotel-profile.service';
import {
  CommandeRestaurant, StatutCommandeRestaurant,
  STATUT_COMMANDE_LABELS, STATUT_COMMANDE_COLORS
} from '../../../models/restaurant.model';

type Severity = "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined;

@Component({
  selector: 'app-detailresto',
  standalone: true,
  imports: [
    CommonModule, CardModule, ButtonModule, TagModule,
    TableModule, ToastModule, ConfirmDialogModule, TooltipModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './detailresto.html',
  styleUrl: './detailresto.css'
})
export class Detailresto implements OnInit {
  commande?: CommandeRestaurant;
  loading = false;
  commandeId!: number;

  // ✅ Profil hôtel
  hotelProfile: HotelProfile | null = null;

  constructor(
    private restaurantService: RestaurantService,
    private hotelProfileService: HotelProfileService,
    private router: Router,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    // ✅ Charger le profil hôtel dès le démarrage
    this.hotelProfileService.getProfile().subscribe({
      next: r => { if (r.success) this.hotelProfile = r.data; },
      error: () => {}
    });

    this.route.params.subscribe(params => {
      this.commandeId = +params['id'];
      if (this.commandeId) this.loadCommande();
    });
  }

  loadCommande(): void {
    this.loading = true;
    this.restaurantService.getCommandeById(this.commandeId).subscribe({
      next: (response) => {
        if (response.success) { this.commande = response.data; }
        else {
          this.messageService.add({ severity: 'warn', summary: 'Attention', detail: response.message || 'Commande introuvable' });
        }
        this.loading = false;
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Impossible de charger la commande' });
        this.loading = false;
      }
    });
  }

  changerStatut(nouveauStatut: StatutCommandeRestaurant): void {
    this.confirmationService.confirm({
      message: `Voulez-vous vraiment passer cette commande à "${STATUT_COMMANDE_LABELS[nouveauStatut]}" ?`,
      header: 'Confirmation', icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      accept: () => {
        this.restaurantService.updateStatut(this.commandeId, nouveauStatut).subscribe({
          next: (r) => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour avec succès' });
              this.loadCommande();
            }
          },
          error: (e) => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.error?.message || 'Impossible de mettre à jour le statut' })
        });
      }
    });
  }

  ajouterPaiement(): void {
    if (!this.commande) return;
    const montantRestant = this.getMontantRestant();
    this.confirmationService.confirm({
      message: `Enregistrer un paiement de ${montantRestant.toLocaleString('fr-FR')} FCFA ?`,
      header: 'Paiement', icon: 'pi pi-money-bill',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      accept: () => {
        this.restaurantService.addPaiement(this.commandeId, montantRestant).subscribe({
          next: (r) => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Paiement enregistré avec succès' });
              this.loadCommande();
            }
          },
          error: (e) => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: e.error?.message || "Impossible d'enregistrer le paiement" })
        });
      }
    });
  }

  // ✅ imprimer() avec vraies infos hôtel
  imprimer(): void {
    if (!this.commande) return;

    const hotel     = this.hotelProfile;
    const hotelNom  = hotel?.name    || 'Hôtel';
    const hotelTel  = hotel?.phone   ? `Tél : ${hotel.phone}`   : '';
    const hotelAdr  = hotel?.address ? hotel.address              : '';
    const hotelMail = hotel?.email   ? `Email : ${hotel.email}`   : '';

    const client         = this.getClientDisplay();
    const dateCommande   = this.commande.dateCommande
      ? new Date(this.commande.dateCommande).toLocaleString('fr-FR')
      : new Date().toLocaleString('fr-FR');
    const montantPaye    = this.commande.montantPaye || 0;
    const montantRestant = this.getMontantRestant();
    const statut         = this.getStatutLabel(this.commande.statut);

    const lignesHTML = (this.commande.lignes || []).map(ligne => `
      <tr>
        <td>${ligne.produitNom || 'Produit'}</td>
        <td style="text-align:center">${ligne.quantite}</td>
        <td style="text-align:right">${ligne.prixUnitaire.toLocaleString('fr-FR')} FCFA</td>
        <td style="text-align:right">${(ligne.sousTotal ?? ligne.quantite * ligne.prixUnitaire).toLocaleString('fr-FR')} FCFA</td>
      </tr>
      ${ligne.notes ? `<tr><td colspan="4" style="font-size:11px;color:#666;padding-left:8px;padding-top:0">↳ ${ligne.notes}</td></tr>` : ''}
    `).join('');

    const html = `<!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8">
<title>Reçu ${this.commande.numeroCommande || this.commande.id}</title>
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
  <div class="sub">
    ${hotelAdr  ? hotelAdr + '<br>'  : ''}
    ${hotelTel  ? hotelTel + '<br>'  : ''}
    ${hotelMail ? hotelMail           : ''}
  </div>
  <div style="margin-top:8px;font-weight:700;font-size:13px">
    REÇU DE COMMANDE – ${this.commande.numeroCommande || '#' + this.commande.id}
  </div>
</div>
<div class="section-title">Informations</div>
<div class="info-grid">
  <span>Client</span><span>${client}</span>
  ${this.commande.numeroTable ? `<span>Table</span><span>${this.commande.numeroTable}</span>` : ''}
  ${this.commande.serveurNom  ? `<span>Serveur</span><span>${this.commande.serveurNom}</span>`  : ''}
  <span>Date</span><span>${dateCommande}</span>
  <span>Statut</span><span>${statut}</span>
</div>
<div class="divider"></div>
<div class="section-title">Détail de la commande</div>
<table>
  <thead><tr>
    <th style="text-align:left">Article</th>
    <th style="text-align:center">Qté</th>
    <th style="text-align:right">P.U.</th>
    <th style="text-align:right">Total</th>
  </tr></thead>
  <tbody>${lignesHTML}</tbody>
</table>
<div class="totals">
  <div class="totals-row total-final">
    <span>TOTAL</span>
    <span>${this.commande.montantTotal.toLocaleString('fr-FR')} FCFA</span>
  </div>
  <div class="totals-row paye">
    <span>Montant payé</span>
    <span>${montantPaye.toLocaleString('fr-FR')} FCFA</span>
  </div>
  ${montantRestant > 0
      ? `<div class="totals-row restant"><span>Reste à payer</span><span>${montantRestant.toLocaleString('fr-FR')} FCFA</span></div>`
      : `<div class="totals-row paye"><span>✓ Entièrement payé</span><span></span></div>`}
</div>
<div class="footer">
  <strong>Merci de votre commande !</strong>
  <p>${hotelNom}</p>
  <p>Imprimé le ${new Date().toLocaleString('fr-FR')}</p>
</div>
<script>window.onload=function(){window.print();setTimeout(function(){window.close()},1000)}</script>
</body></html>`;

    const fenetre = window.open('', '_blank', 'width=500,height=700');
    if (fenetre) { fenetre.document.write(html); fenetre.document.close(); }
  }

  retourListe(): void { this.router.navigate(['/restauration']); }

  getStatutLabel(statut: StatutCommandeRestaurant): string { return STATUT_COMMANDE_LABELS[statut] || statut; }
  getStatutSeverity(statut: StatutCommandeRestaurant): Severity { return (STATUT_COMMANDE_COLORS[statut] || 'info') as Severity; }
  getClientDisplay(): string {
    if (!this.commande) return '';
    if (this.commande.clientNom)         return this.commande.clientNom;
    if (this.commande.nomClientExterne)  return this.commande.nomClientExterne;
    if (this.commande.reservationNumero) return `Réservation ${this.commande.reservationNumero}`;
    return 'Client non spécifié';
  }
  getMontantRestant(): number {
    if (!this.commande) return 0;
    return this.commande.montantTotal - (this.commande.montantPaye || 0);
  }
  canMarquerServie():    boolean { return this.commande?.statut === StatutCommandeRestaurant.EN_ATTENTE; }
  canMarquerPayee():     boolean { return this.commande?.statut === StatutCommandeRestaurant.SERVIE;     }
  canAjouterPaiement():  boolean {
    if (!this.commande) return false;
    return this.getMontantRestant() > 0 && this.commande.statut !== StatutCommandeRestaurant.ANNULEE;
  }
  marquerServie(): void { this.changerStatut(StatutCommandeRestaurant.SERVIE); }
  marquerPayee():  void { this.changerStatut(StatutCommandeRestaurant.PAYEE);  }
}
