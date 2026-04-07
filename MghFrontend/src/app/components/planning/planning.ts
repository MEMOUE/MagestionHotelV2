import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ReservationService } from '../../services/reservation.service';
import { ChambreService, Chambre } from '../../services/chambre.service';
import { HotelProfileService, HotelProfile } from '../../services/hotel-profile.service';
import { AuthService } from '../../services/auth.service';
import { Reservation, StatutReservation } from '../../models/reservation.model';
import { RestaurantService } from '../../services/restaurant.service';
import { CommandeRestaurant } from '../../models/restaurant.model';

@Component({
  selector: 'app-planning',
  imports: [CommonModule, FormsModule],
  templateUrl: './planning.html',
  styleUrl: './planning.css',
})
export class Planning implements OnInit {
  chambres: Chambre[] = [];
  reservations: Reservation[] = [];
  dates: Date[] = [];

  grid: Map<number, Map<string, Reservation>> = new Map();

  selectedReservation: Reservation | null = null;
  showModal = false;
  loading = true;
  error = '';

  hotelProfile: HotelProfile | null = null;

  startDate: Date = new Date();
  endDate: Date = new Date();
  daysCount = 30;

  // Paiement
  showPaiementForm = false;
  paiementMontant = 0;
  paiementMode = 'ESPECES';

  // ✅ Remise
  showRemiseForm = false;
  remiseJournaliere = 0;
  remiseTotaleRes = 0;

  modesPaiement = [
    { value: 'ESPECES',        label: 'Espèces' },
    { value: 'CARTE_BANCAIRE', label: 'Carte bancaire' },
    { value: 'ORANGE_MONEY',   label: 'Orange Money' },
    { value: 'MTN_MONEY',      label: 'MTN Money' },
    { value: 'WAVE',           label: 'Wave' },
    { value: 'MOBILE_MONEY',   label: 'Mobile Money' },
    { value: 'VIREMENT',       label: 'Virement' },
  ];

  statutConfig: Record<string, { label: string; color: string; bg: string; border: string }> = {
    EN_ATTENTE: { label: 'En attente', color: '#78350f', bg: '#fbbf24', border: '#b45309' },
    CONFIRMEE:  { label: 'Confirmée',  color: '#eff6ff', bg: '#1d4ed8', border: '#1e3a8a' },
    EN_COURS:   { label: 'En cours',   color: '#f0fdf4', bg: '#16a34a', border: '#14532d' },
    TERMINEE:   { label: 'Terminée',   color: '#fdf4ff', bg: '#7c3aed', border: '#4c1d95' },
    ANNULEE:    { label: 'Annulée',    color: '#fff1f2', bg: '#dc2626', border: '#7f1d1d' },
    NO_SHOW:    { label: 'No-show',    color: '#fff7ed', bg: '#ea580c', border: '#7c2d12' },
  };

  constructor(
    private reservationService: ReservationService,
    private chambreService: ChambreService,
    private hotelProfileService: HotelProfileService,
    private authService: AuthService,
    private restaurantService: RestaurantService
  ) {
    this.startDate = new Date();
    this.startDate.setDate(this.startDate.getDate() - 3);
    this.endDate = new Date(this.startDate);
    this.endDate.setDate(this.endDate.getDate() + this.daysCount);
  }

  ngOnInit(): void {
    this.loadHotelProfile();
    this.loadData();
  }

  loadHotelProfile(): void {
    const user = this.authService.currentUserValue;
    if (!user) return;

    // getProfile() ne marche que pour HOTEL, donc on utilise getHotelById
    this.hotelProfileService.getHotelById(user.hotelId).subscribe({
      next: (r) => { if (r.success) this.hotelProfile = r.data; },
      error: () => {}
    });
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      chambres: this.chambreService.getChambres(),
      reservationsResp: this.reservationService.getAllReservations(),
    }).subscribe({
      next: ({ chambres, reservationsResp }) => {
        this.chambres = (chambres || []).sort((a, b) =>
          (a.numero || '').localeCompare(b.numero || '', undefined, { numeric: true })
        );
        const raw = (reservationsResp as any)?.data ?? reservationsResp;
        this.reservations = Array.isArray(raw) ? raw : [];
        this.buildDates();
        this.buildGrid();
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.message || 'Erreur lors du chargement des données';
        this.loading = false;
      },
    });
  }

  buildDates(): void {
    this.dates = [];
    const cur = new Date(this.startDate);
    cur.setHours(0, 0, 0, 0);
    const end = new Date(this.endDate);
    end.setHours(0, 0, 0, 0);
    while (cur <= end) {
      this.dates.push(new Date(cur));
      cur.setDate(cur.getDate() + 1);
    }
  }

  buildGrid(): void {
    this.grid = new Map();
    for (const c of this.chambres) this.grid.set(c.id!, new Map());
    for (const res of this.reservations) {
      const map = this.grid.get(res.chambreId);
      if (!map) continue;
      const arr = new Date(res.dateArrivee); arr.setHours(0,0,0,0);
      const dep = new Date(res.dateDepart);  dep.setHours(0,0,0,0);
      const cur = new Date(arr);
      while (cur < dep) { map.set(this.dk(cur), res); cur.setDate(cur.getDate() + 1); }
    }
  }

  dk(d: Date): string { return d.toISOString().split('T')[0]; }

  getRes(chambreId: number, date: Date): Reservation | null {
    return this.grid.get(chambreId)?.get(this.dk(date)) ?? null;
  }

  isStartDate(chambreId: number, date: Date): boolean {
    const res = this.getRes(chambreId, date);
    if (!res) return false;
    const arr = new Date(res.dateArrivee); arr.setHours(0,0,0,0);
    const d   = new Date(date);            d.setHours(0,0,0,0);
    return arr.getTime() === d.getTime();
  }

  isEndDate(chambreId: number, date: Date): boolean {
    const res = this.getRes(chambreId, date);
    if (!res) return false;
    const dep  = new Date(res.dateDepart); dep.setHours(0,0,0,0);
    const next = new Date(date); next.setDate(next.getDate() + 1); next.setHours(0,0,0,0);
    return dep.getTime() === next.getTime();
  }

  isToday(date: Date): boolean    { return this.dk(date) === this.dk(new Date()); }
  isSunday(date: Date): boolean   { return date.getDay() === 0; }
  isSaturday(date: Date): boolean { return date.getDay() === 6; }

  getCellStyle(res: Reservation | null): Record<string, string> {
    if (!res) return {};
    const cfg = this.statutConfig[res.statut || ''] || { bg: '#6b7280', border: '#4b5563', color: '#fff' };
    return { 'background-color': cfg.bg, 'border-left': `4px solid ${cfg.border}` };
  }

  getStatutCfg(statut?: string) {
    return this.statutConfig[statut || ''] || {
      label: statut || '', color: '#fff', bg: '#6b7280', border: '#4b5563'
    };
  }

  onCellClick(chambreId: number, date: Date): void {
    const res = this.getRes(chambreId, date);
    if (res) {
      this.selectedReservation = { ...res };
      this.showModal = true;
      this.showPaiementForm = false;
      this.showRemiseForm = false;
      this.paiementMontant = 0;
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedReservation = null;
    this.showPaiementForm = false;
    this.showRemiseForm = false;
  }

  canCheckin():  boolean { return this.selectedReservation?.statut === StatutReservation.CONFIRMEE; }
  canCheckout(): boolean { return this.selectedReservation?.statut === StatutReservation.EN_COURS;  }
  canCancel():   boolean {
    const s = this.selectedReservation?.statut;
    return s === StatutReservation.EN_ATTENTE || s === StatutReservation.CONFIRMEE;
  }
  canPay(): boolean {
    const s = this.selectedReservation?.statut;
    return s !== StatutReservation.ANNULEE &&
      s !== StatutReservation.TERMINEE &&
      s !== StatutReservation.NO_SHOW;
  }
  canApplyRemise(): boolean {
    const s = this.selectedReservation?.statut;
    return s !== StatutReservation.ANNULEE && s !== StatutReservation.TERMINEE;
  }

  // ✅ Ouvrir le formulaire remise avec les valeurs existantes
  openRemiseForm(): void {
    this.showRemiseForm = true;
    this.showPaiementForm = false;
    this.remiseJournaliere = this.selectedReservation?.remiseJournaliere || 0;
    this.remiseTotaleRes = this.selectedReservation?.remiseTotale || 0;
  }

  // ✅ Soumettre la remise
  submitRemise(): void {
    if (!this.selectedReservation?.id) return;
    this.reservationService.appliquerRemise(this.selectedReservation.id, {
      remiseJournaliere: this.remiseJournaliere,
      remiseTotale: this.remiseTotaleRes
    }).subscribe({
      next: (resp) => {
        const data = (resp as any)?.data ?? resp;
        this.selectedReservation = data;
        this.showRemiseForm = false;
        this.loadData();
        alert('Remise appliquée avec succès !');
      },
      error: (e) => alert('Erreur remise : ' + (e?.error?.message || e.message)),
    });
  }

  doCheckin(): void {
    if (!this.selectedReservation?.id) return;
    this.reservationService.doCheckin(this.selectedReservation.id).subscribe({
      next: (resp) => { this.selectedReservation = (resp as any)?.data ?? resp; this.loadData(); },
      error: (e)   => alert('Erreur check-in : ' + (e?.error?.message || e.message)),
    });
  }

  doCheckout(): void {
    if (!this.selectedReservation?.id) return;
    this.reservationService.doCheckout(this.selectedReservation.id).subscribe({
      next: (resp) => { this.selectedReservation = (resp as any)?.data ?? resp; this.loadData(); },
      error: (e)   => alert('Erreur check-out : ' + (e?.error?.message || e.message)),
    });
  }

  doCancel(): void {
    if (!this.selectedReservation?.id) return;
    if (!confirm("Confirmer l'annulation de cette réservation ?")) return;
    this.reservationService.cancelReservation(this.selectedReservation.id).subscribe({
      next: () => { this.closeModal(); this.loadData(); },
      error: (e) => alert('Erreur annulation : ' + (e?.error?.message || e.message)),
    });
  }

  submitPaiement(): void {
    if (!this.selectedReservation?.id || this.paiementMontant <= 0) return;
    this.reservationService.addPaiement(this.selectedReservation.id, {
      montant: this.paiementMontant,
      modePaiement: this.paiementMode,
    }).subscribe({
      next: (resp) => {
        this.selectedReservation = (resp as any)?.data ?? resp;
        this.showPaiementForm = false;
        this.paiementMontant  = 0;
        this.loadData();
      },
      error: (e) => alert('Erreur paiement : ' + (e?.error?.message || e.message)),
    });
  }

  printFacture(): void {
    const res = this.selectedReservation;
    if (!res || !res.id) return;

    this.restaurantService.getCommandesByReservation(res.id).subscribe({
      next: (response) => {
        const commandes = response.success ? response.data : [];
        this.imprimerFactureConsolidee(res, commandes);
      },
      error: () => this.imprimerFactureConsolidee(res, [])
    });
  }

  private imprimerFactureConsolidee(res: Reservation, commandes: CommandeRestaurant[]): void {
    const hotel     = this.hotelProfile;
    const hotelNom  = hotel?.name    || 'Hôtel';
    const hotelTel  = hotel?.phone   ? `Tél : ${hotel.phone}`   : '';
    const hotelAdr  = hotel?.address || '';
    const hotelMail = hotel?.email   ? `Email : ${hotel.email}`  : '';

    const dateArr = res.dateArrivee ? new Date(res.dateArrivee).toLocaleDateString('fr-FR') : '-';
    const dateDep = res.dateDepart  ? new Date(res.dateDepart).toLocaleDateString('fr-FR')  : '-';
    const now     = new Date().toLocaleDateString('fr-FR');
    const cfg     = this.getStatutCfg(res.statut);

    const prixEffectif = (res.prixParNuit || 0) - (res.remiseJournaliere || 0);
    const montantHebergement = prixEffectif * (res.nombreNuits || 0);

    let lignesHTML = `
    <tr class="section-header"><td colspan="4"><strong>HÉBERGEMENT</strong></td></tr>
    <tr>
      <td>Chambre ${res.chambreNumero || res.chambreId}${(res.remiseJournaliere || 0) > 0 ? ' <small style="color:#b8860b">(remise ' + (res.remiseJournaliere || 0).toLocaleString('fr-FR') + ' F CFA/nuit)</small>' : ''}</td>
      <td class="center">${res.nombreNuits || 0}</td>
      <td class="right">${prixEffectif.toLocaleString('fr-FR')} F CFA</td>
      <td class="right">${montantHebergement.toLocaleString('fr-FR')} F CFA</td>
    </tr>`;

    if ((res.remiseTotale || 0) > 0) {
      lignesHTML += `
      <tr>
        <td colspan="3" class="right" style="color:#b8860b"><em>Remise totale</em></td>
        <td class="right" style="color:#b8860b"><em>-${(res.remiseTotale || 0).toLocaleString('fr-FR')} F CFA</em></td>
      </tr>`;
    }

    const sousTotal = montantHebergement - (res.remiseTotale || 0);
    lignesHTML += `
    <tr class="subtotal-row">
      <td colspan="3" class="right"><strong>Sous-total Hébergement</strong></td>
      <td class="right"><strong>${Math.max(0, sousTotal).toLocaleString('fr-FR')} F CFA</strong></td>
    </tr>`;

    let montantRestaurant = 0;
    if (commandes.length > 0) {
      lignesHTML += `<tr class="section-header"><td colspan="4"><strong>CONSOMMATION RESTAURANT</strong></td></tr>`;
      for (const cmd of commandes) {
        for (const ligne of (cmd.lignes || [])) {
          const st = ligne.sousTotal ?? (ligne.quantite * ligne.prixUnitaire);
          montantRestaurant += st;
          lignesHTML += `
          <tr>
            <td>${ligne.produitNom || 'Produit'} <small style="color:#888">(${cmd.numeroCommande})</small></td>
            <td class="center">${ligne.quantite}</td>
            <td class="right">${ligne.prixUnitaire.toLocaleString('fr-FR')} F CFA</td>
            <td class="right">${st.toLocaleString('fr-FR')} F CFA</td>
          </tr>`;
        }
        if ((cmd.remiseTotale || 0) > 0) {
          montantRestaurant -= (cmd.remiseTotale || 0);
          lignesHTML += `
          <tr>
            <td colspan="3" class="right" style="color:#b8860b"><em>Remise commande ${cmd.numeroCommande}</em></td>
            <td class="right" style="color:#b8860b"><em>-${(cmd.remiseTotale || 0).toLocaleString('fr-FR')} F CFA</em></td>
          </tr>`;
        }
      }
      lignesHTML += `
      <tr class="subtotal-row">
        <td colspan="3" class="right"><strong>Sous-total Restaurant (${commandes.length} cmd)</strong></td>
        <td class="right"><strong>${Math.max(0, montantRestaurant).toLocaleString('fr-FR')} F CFA</strong></td>
      </tr>`;
    }

    const totalGeneral   = res.montantTotal || 0;
    const montantPaye    = res.montantPaye    || 0;
    const montantRestant = Math.max(0, totalGeneral - montantPaye);

    const html = `<!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8">
<title>Facture – ${res.numeroReservation}</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Georgia',serif;color:#1a1a2e;background:#fff;padding:40px}
.header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #1a1a2e;padding-bottom:20px;margin-bottom:28px}
.hotel-info .name{font-size:24px;font-weight:900;letter-spacing:1px;text-transform:uppercase}
.hotel-info .details{font-size:12px;color:#555;margin-top:6px;line-height:1.8}
.invoice-meta{text-align:right}
.invoice-meta h2{font-size:16px;text-transform:uppercase;letter-spacing:3px}
.invoice-meta p{color:#555;font-size:12px;margin-top:4px}
.parties{display:flex;gap:40px;margin-bottom:28px}
.party{flex:1}
.party h3{font-size:10px;text-transform:uppercase;letter-spacing:2px;color:#888;border-bottom:1px solid #ddd;padding-bottom:6px;margin-bottom:10px}
.party p{font-size:13px;line-height:1.8}
table{width:100%;border-collapse:collapse;margin-bottom:20px}
th{background:#1a1a2e;color:#fff;padding:8px 12px;text-align:left;font-size:11px;text-transform:uppercase;letter-spacing:1px}
td{padding:8px 12px;border-bottom:1px solid #eee;font-size:12px}
.center{text-align:center}.right{text-align:right}
.section-header td{background:#f0f0f0;font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#1a1a2e;padding:10px 12px;border-top:2px solid #ddd}
.subtotal-row td{background:#f8f8f8;border-top:1px solid #ccc}
.totals{display:flex;justify-content:flex-end;margin-top:8px}
.totals-box{width:320px;border:1px solid #ddd}
.totals-row{display:flex;justify-content:space-between;padding:10px 16px;border-bottom:1px solid #eee;font-size:13px}
.totals-row.grand-total{background:#1a1a2e;color:#fff;font-weight:bold;font-size:15px}
.totals-row.paye{background:#d1fae5;color:#065f46;font-weight:bold}
.totals-row.reste{background:#fee2e2;color:#991b1b;font-weight:bold}
.totals-row.ok{background:#d1fae5;color:#065f46;font-weight:bold}
.statut-badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:bold;background:${cfg.bg};color:${cfg.color};border:1px solid ${cfg.border}}
.footer{margin-top:40px;padding-top:16px;border-top:1px solid #ddd;display:flex;justify-content:space-between;font-size:11px;color:#888}
@media print{body{padding:20px}@page{margin:10mm}}
</style></head><body>
<div class="header">
  <div class="hotel-info">
    <div class="name">${hotelNom}</div>
    <div class="details">${hotelAdr ? hotelAdr+'<br>' : ''}${hotelTel ? hotelTel+'<br>' : ''}${hotelMail}</div>
  </div>
  <div class="invoice-meta">
    <h2>Facture</h2>
    <p>N° ${res.numeroReservation}</p>
    <p>Émise le ${now}</p>
    <p style="margin-top:8px"><span class="statut-badge">${cfg.label}</span></p>
  </div>
</div>
<div class="parties">
  <div class="party">
    <h3>Client</h3>
    <p><strong>${res.clientPrenom || ''} ${res.clientNom || ''}</strong></p>
    ${res.clientTelephone ? `<p>Tél : ${res.clientTelephone}</p>` : ''}
  </div>
  <div class="party">
    <h3>Séjour</h3>
    <p>Chambre <strong>${res.chambreNumero || res.chambreId}</strong></p>
    <p>Du <strong>${dateArr}</strong> au <strong>${dateDep}</strong> — ${res.nombreNuits || 0} nuit(s)</p>
    <p>${res.nombreAdultes || 1} adulte(s), ${res.nombreEnfants || 0} enfant(s)</p>
  </div>
</div>
<table>
  <thead><tr><th>Désignation</th><th class="center">Qté</th><th class="right">P.U.</th><th class="right">Montant</th></tr></thead>
  <tbody>${lignesHTML}</tbody>
</table>
<div class="totals"><div class="totals-box">
  <div class="totals-row grand-total"><span>TOTAL GÉNÉRAL</span><span>${totalGeneral.toLocaleString('fr-FR')} F CFA</span></div>
  <div class="totals-row paye"><span>Montant payé</span><span>${montantPaye.toLocaleString('fr-FR')} F CFA</span></div>
  <div class="totals-row ${montantRestant > 0 ? 'reste' : 'ok'}">
    <span>${montantRestant > 0 ? 'Reste à payer' : 'Entièrement payé'}</span>
    <span>${montantRestant > 0 ? montantRestant.toLocaleString('fr-FR')+' F CFA' : '✓'}</span>
  </div>
</div></div>
${res.notes ? `<div style="margin-top:24px;padding:12px;background:#f9f9f9;border-left:3px solid #1a1a2e;font-size:12px"><strong>Notes :</strong> ${res.notes}</div>` : ''}
<div class="footer"><span>${hotelNom}${hotelAdr ? ' — '+hotelAdr : ''}</span><span>Généré le ${now}</span></div>
<script>window.onload=function(){window.print();setTimeout(function(){window.close()},1000)}</script>
</body></html>`;

    const win = window.open('', '_blank');
    if (win) { win.document.write(html); win.document.close(); win.focus(); }
  }

  navigateDays(delta: number): void {
    this.startDate = new Date(this.startDate);
    this.startDate.setDate(this.startDate.getDate() + delta);
    this.endDate = new Date(this.startDate);
    this.endDate.setDate(this.endDate.getDate() + this.daysCount);
    this.buildDates();
  }

  goToToday(): void {
    this.startDate = new Date();
    this.startDate.setDate(this.startDate.getDate() - 3);
    this.endDate = new Date(this.startDate);
    this.endDate.setDate(this.endDate.getDate() + this.daysCount);
    this.buildDates();
  }

  formatDate(d: Date): string {
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  formatDateLong(d: string | Date | undefined): string {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  formatMontant(m: number | undefined): string {
    return (m || 0).toLocaleString('fr-FR') + ' F CFA';
  }

  getDayName(d: Date): string {
    return d.toLocaleDateString('fr-FR', { weekday: 'short' }).slice(0, 3).toUpperCase();
  }

  getMonthLabel(): string {
    const months = new Set(this.dates.map(d => d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })));
    return Array.from(months).join(' – ');
  }

  trackDate(_i: number, d: Date): string      { return d.toISOString(); }
  trackChambre(_i: number, c: Chambre): number { return c.id!; }
}
