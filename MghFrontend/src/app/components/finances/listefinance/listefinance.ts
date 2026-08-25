import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { DividerModule } from 'primeng/divider';

// Services
import { FinanceService } from '../../../services/finance.service';
import { MessageService, ConfirmationService } from 'primeng/api';

// Models
import {
  Transaction,
  TypeTransaction,
  StatutTransaction,
  ModePaiementTransaction,
  TYPE_TRANSACTION_LABELS,
  TYPE_TRANSACTION_COLORS,
  STATUT_TRANSACTION_LABELS,
  STATUT_TRANSACTION_COLORS,
  MODE_PAIEMENT_LABELS,
  CATEGORIES_REVENUS,
  CATEGORIES_DEPENSES
} from '../../../models/finance.model';
import {Toolbar} from 'primeng/toolbar';

@Component({
  selector: 'app-listefinance',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    TableModule, ButtonModule, InputTextModule,
    TagModule, CardModule, ToastModule, ConfirmDialogModule,
    SelectModule, DatePickerModule,
    IconFieldModule, InputIconModule, DividerModule, Toolbar
  ],
  templateUrl: './listefinance.html',
  styleUrl: './listefinance.css',
  providers: [MessageService, ConfirmationService]
})
export class Listefinance implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('evolutionChartCanvas') evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoriesChartCanvas') categoriesChartCanvas?: ElementRef<HTMLCanvasElement>;

  private evolutionChart?: Chart;
  private categoriesChart?: Chart;

  // ── Données ───────────────────────────────────────────────────────────────────
  transactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  loading = false;

  // ── Stats dynamiques (calculées sur filteredTransactions) ─────────────────────
  soldeActuel     = 0;
  revenusMois     = 0;
  revenusAujourdhui = 0;
  depensesMois    = 0;
  depensesAujourdhui = 0;
  resultatMois    = 0;

  // ── Filtres ───────────────────────────────────────────────────────────────────
  searchTerm     = '';
  selectedType:      TypeTransaction | null = null;
  selectedStatut:    StatutTransaction | null = null;
  selectedCategorie: string | null = null;
  dateDebut: Date | null = null;
  dateFin:   Date | null = null;

  // ── Options ───────────────────────────────────────────────────────────────────
  typesTransaction = [
    { label: 'Tous', value: null },
    { label: TYPE_TRANSACTION_LABELS[TypeTransaction.REVENU],  value: TypeTransaction.REVENU  },
    { label: TYPE_TRANSACTION_LABELS[TypeTransaction.DEPENSE], value: TypeTransaction.DEPENSE }
  ];

  statutsTransaction = [
    { label: 'Tous',   value: null },
    { label: STATUT_TRANSACTION_LABELS[StatutTransaction.EN_ATTENTE], value: StatutTransaction.EN_ATTENTE },
    { label: STATUT_TRANSACTION_LABELS[StatutTransaction.VALIDEE],    value: StatutTransaction.VALIDEE    },
    { label: STATUT_TRANSACTION_LABELS[StatutTransaction.ANNULEE],    value: StatutTransaction.ANNULEE    },
    { label: STATUT_TRANSACTION_LABELS[StatutTransaction.REMBOURSEE], value: StatutTransaction.REMBOURSEE }
  ];

  /** Catégories disponibles, déduites des transactions chargées. */
  categoriesTransaction: { label: string; value: string | null }[] = [
    { label: 'Toutes', value: null }
  ];

  // Expose enums au template
  TypeTransaction    = TypeTransaction;
  StatutTransaction  = StatutTransaction;
  MODE_PAIEMENT_LABELS = MODE_PAIEMENT_LABELS;

  constructor(
    private financeService: FinanceService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.tryRenderCharts();
  }

  ngOnDestroy(): void {
    this.evolutionChart?.destroy();
    this.categoriesChart?.destroy();
  }

  // ── Chargement ────────────────────────────────────────────────────────────────

  loadTransactions(): void {
    this.loading = true;
    this.financeService.getTransactions().subscribe({
      next: (response) => {
        if (response.success) {
          this.transactions = response.data;
          this.updateCategoriesDisponibles();
          this.applyFilters();
        }
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les transactions' });
        this.loading = false;
      }
    });
  }

  private updateCategoriesDisponibles(): void {
    const noms = (
      this.selectedType === TypeTransaction.REVENU ? CATEGORIES_REVENUS :
      this.selectedType === TypeTransaction.DEPENSE ? CATEGORIES_DEPENSES :
      [...CATEGORIES_REVENUS, ...CATEGORIES_DEPENSES]
    ).map(c => c.nom);

    this.categoriesTransaction = [
      { label: 'Toutes', value: null },
      ...noms.map(c => ({ label: c, value: c }))
    ];

    if (this.selectedCategorie && !noms.includes(this.selectedCategorie)) {
      this.selectedCategorie = null;
    }
  }

  // ── Graphiques ────────────────────────────────────────────────────────────────

  private viewReady = false;

  private tryRenderCharts(): void {
    this.renderEvolutionChart();
    this.renderCategoriesChart();
  }

  /**
   * Graphe de gauche : histogramme Revenus/Dépenses des 6 derniers mois (sans ligne de liaison),
   * calculé depuis filteredTransactions : respecte donc le type, la catégorie et les dates choisis.
   */
  private renderEvolutionChart(): void {
    if (!this.viewReady) return;

    const evolutionCtx = this.evolutionChartCanvas?.nativeElement.getContext('2d');
    if (!evolutionCtx) return;

    const validees = this.filteredTransactions.filter(t => t.statut === StatutTransaction.VALIDEE);

    const now = new Date();
    const mois = Array.from({ length: 6 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth() - (5 - i), 1);
      return {
        label: d.toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' }),
        debut: new Date(d.getFullYear(), d.getMonth(), 1),
        fin: new Date(d.getFullYear(), d.getMonth() + 1, 1)
      };
    });

    const transactionsDuMois = (m: { debut: Date; fin: Date }) =>
      validees.filter(t =>
        t.dateTransaction &&
        new Date(t.dateTransaction) >= m.debut &&
        new Date(t.dateTransaction) < m.fin
      );

    this.evolutionChart?.destroy();
    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: mois.map(m => m.label),
        datasets: [
          {
            label: 'Revenus',
            data: mois.map(m => this.sumMontant(transactionsDuMois(m), TypeTransaction.REVENU)),
            backgroundColor: '#4caf50'
          },
          {
            label: 'Dépenses',
            data: mois.map(m => this.sumMontant(transactionsDuMois(m), TypeTransaction.DEPENSE)),
            backgroundColor: '#f44336'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } },
        scales: { y: { ticks: { callback: (v) => this.formatCurrency(Number(v)) } } }
      }
    };
    this.evolutionChart = new Chart(evolutionCtx, config);
  }

  /** Graphe de droite : dépenses vs revenus par catégorie, sur la période sélectionnée (filteredTransactions). */
  private renderCategoriesChart(): void {
    if (!this.viewReady) return;

    const categoriesCtx = this.categoriesChartCanvas?.nativeElement.getContext('2d');
    if (!categoriesCtx) return;

    const validees = this.filteredTransactions.filter(t => t.statut === StatutTransaction.VALIDEE);

    const categories = Array.from(new Set(validees.map(t => t.categorie || 'Autre'))).sort();
    const revenusParCategorie = categories.map(cat =>
      this.sumMontant(validees.filter(t => (t.categorie || 'Autre') === cat), TypeTransaction.REVENU)
    );
    const depensesParCategorie = categories.map(cat =>
      this.sumMontant(validees.filter(t => (t.categorie || 'Autre') === cat), TypeTransaction.DEPENSE)
    );

    this.categoriesChart?.destroy();
    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: categories,
        datasets: [
          { label: 'Revenus', data: revenusParCategorie, backgroundColor: '#4caf50' },
          { label: 'Dépenses', data: depensesParCategorie, backgroundColor: '#f44336' }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } },
        scales: { y: { ticks: { callback: (v) => this.formatCurrency(Number(v)) } } }
      }
    };
    this.categoriesChart = new Chart(categoriesCtx, config);
  }

  // ── Filtres + stats dynamiques ────────────────────────────────────────────────

  applyFilters(): void {
    let result = [...this.transactions];

    if (this.searchTerm?.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(t =>
        t.reference?.toLowerCase().includes(term) ||
        t.description?.toLowerCase().includes(term) ||
        t.categorie?.toLowerCase().includes(term)
      );
    }

    if (this.selectedType)      result = result.filter(t => t.type      === this.selectedType);
    if (this.selectedStatut)    result = result.filter(t => t.statut    === this.selectedStatut);
    if (this.selectedCategorie) result = result.filter(t => t.categorie === this.selectedCategorie);

    if (this.dateDebut) {
      const debut = new Date(this.dateDebut); debut.setHours(0, 0, 0, 0);
      result = result.filter(t => t.dateTransaction && new Date(t.dateTransaction) >= debut);
    }
    if (this.dateFin) {
      const fin = new Date(this.dateFin); fin.setHours(23, 59, 59, 999);
      result = result.filter(t => t.dateTransaction && new Date(t.dateTransaction) <= fin);
    }

    this.filteredTransactions = result;
    this.recalculerStats();
    this.tryRenderCharts();
  }

  onFilterChange(): void {
    this.updateCategoriesDisponibles();
    this.applyFilters();
  }
  applyFilter():    void { this.applyFilters(); }

  clearFilters(): void {
    this.searchTerm        = '';
    this.selectedType      = null;
    this.selectedStatut    = null;
    this.selectedCategorie = null;
    this.dateDebut         = null;
    this.dateFin           = null;
    this.updateCategoriesDisponibles();
    this.applyFilters();
  }

  get hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.selectedType || this.selectedStatut || this.selectedCategorie || this.dateDebut || this.dateFin);
  }

  // ── Recalcul des 4 cartes depuis filteredTransactions ─────────────────────────

  private recalculerStats(): void {
    const validees = this.filteredTransactions.filter(t => t.statut === StatutTransaction.VALIDEE);

    const now     = new Date();
    const debutMois = new Date(now.getFullYear(), now.getMonth(), 1);
    const debutJour = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    const totalRev  = this.sumMontant(validees, TypeTransaction.REVENU);
    const totalDep  = this.sumMontant(validees, TypeTransaction.DEPENSE);
    this.soldeActuel = totalRev - totalDep;

    const valideesMois = validees.filter(t => t.dateTransaction && new Date(t.dateTransaction) >= debutMois);
    this.revenusMois  = this.sumMontant(valideesMois, TypeTransaction.REVENU);
    this.depensesMois = this.sumMontant(valideesMois, TypeTransaction.DEPENSE);
    this.resultatMois = this.revenusMois - this.depensesMois;

    const valideesJour = validees.filter(t => t.dateTransaction && new Date(t.dateTransaction) >= debutJour);
    this.revenusAujourdhui  = this.sumMontant(valideesJour, TypeTransaction.REVENU);
    this.depensesAujourdhui = this.sumMontant(valideesJour, TypeTransaction.DEPENSE);
  }

  private sumMontant(list: Transaction[], type: TypeTransaction): number {
    return list
      .filter(t => t.type === type)
      .reduce((sum, t) => sum + (Number(t.montant) || 0), 0);
  }

  // ── Navigation ────────────────────────────────────────────────────────────────

  viewDetails(transaction: Transaction): void {
    this.router.navigate(['/finances', transaction.id]);
  }

  editTransaction(transaction: Transaction): void {
    this.router.navigate(['/finances/edit', transaction.id]);
  }

  // ── Actions ───────────────────────────────────────────────────────────────────

  validerTransaction(transaction: Transaction): void {
    if (!transaction.id) return;
    this.confirmationService.confirm({
      message: `Valider cette transaction de ${this.formatCurrency(transaction.montant)} ?`,
      header: 'Confirmation', icon: 'pi pi-check-circle',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      accept: () => {
        this.financeService.validerTransaction(transaction.id!).subscribe({
          next: r => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Transaction validée' });
              this.loadTransactions();
            }
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de valider' })
        });
      }
    });
  }

  annulerTransaction(transaction: Transaction): void {
    if (!transaction.id) return;
    this.confirmationService.confirm({
      message: 'Voulez-vous annuler cette transaction ?',
      header: 'Confirmation', icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.financeService.annulerTransaction(transaction.id!).subscribe({
          next: r => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Transaction annulée' });
              this.loadTransactions();
            }
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Impossible d'annuler" })
        });
      }
    });
  }

  deleteTransaction(transaction: Transaction): void {
    if (!transaction.id) return;
    this.confirmationService.confirm({
      message: 'Supprimer définitivement cette transaction ?',
      header: 'Suppression', icon: 'pi pi-trash',
      acceptLabel: 'Oui', rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.financeService.deleteTransaction(transaction.id!).subscribe({
          next: r => {
            if (r.success) {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Transaction supprimée' });
              this.loadTransactions();
            }
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer' })
        });
      }
    });
  }

  // ── Export PDF/Excel — génération client depuis filteredTransactions ──────────

  exportCSV(): void {
    const rows = this.filteredTransactions;
    if (rows.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Vide', detail: 'Aucune transaction à exporter' });
      return;
    }

    const header = ['Référence', 'Date', 'Type', 'Catégorie', 'Montant (FCFA)', 'Mode paiement', 'Statut', 'Description'].join(';');
    const lines  = rows.map(t => [
      t.reference ?? '',
      t.dateTransaction ? new Date(t.dateTransaction).toLocaleString('fr-FR') : '',
      this.getTypeLabel(t.type),
      t.categorie ?? '',
      t.montant ?? 0,
      t.modePaiement ? this.getModePaiementLabel(t.modePaiement) : '',
      this.getStatutLabel(t.statut),
      (t.description ?? '').replace(/;/g, ',')
    ].join(';'));

    const csv     = '\uFEFF' + [header, ...lines].join('\n');
    const blob    = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    this.downloadBlob(blob, `transactions_${this.today()}.csv`);
    this.messageService.add({ severity: 'success', summary: 'Export CSV', detail: `${rows.length} transaction(s) exportée(s)` });
  }

  exportPDF(): void {
    const rows = this.filteredTransactions;
    if (rows.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Vide', detail: 'Aucune transaction à exporter' });
      return;
    }

    const lignesHTML = rows.map(t => `
      <tr>
        <td>${t.reference ?? '-'}</td>
        <td>${t.dateTransaction ? new Date(t.dateTransaction).toLocaleDateString('fr-FR') : '-'}</td>
        <td><span class="${t.type === TypeTransaction.REVENU ? 'rev' : 'dep'}">${this.getTypeLabel(t.type)}</span></td>
        <td>${t.categorie ?? '-'}</td>
        <td class="num ${t.type === TypeTransaction.REVENU ? 'rev' : 'dep'}">${Number(t.montant ?? 0).toLocaleString('fr-FR')} FCFA</td>
        <td>${this.getStatutLabel(t.statut)}</td>
        <td>${(t.description ?? '-').substring(0, 60)}</td>
      </tr>`).join('');

    const totalRev = this.sumMontant(rows.filter(t => t.statut === StatutTransaction.VALIDEE), TypeTransaction.REVENU);
    const totalDep = this.sumMontant(rows.filter(t => t.statut === StatutTransaction.VALIDEE), TypeTransaction.DEPENSE);
    const solde    = totalRev - totalDep;

    const filterLabel = this.buildFilterLabel();

    const html = `<!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8">
<title>Transactions – ${this.today()}</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:Arial,sans-serif;font-size:12px;color:#1a1a2e;padding:30px}
h1{font-size:20px;font-weight:800;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px}
.subtitle{font-size:11px;color:#6b7280;margin-bottom:6px}
.filters{font-size:11px;color:#4f46e5;margin-bottom:16px;font-style:italic}
table{width:100%;border-collapse:collapse;margin-bottom:20px}
thead tr{background:#1a1a2e;color:#fff}
thead th{padding:8px 10px;text-align:left;font-size:11px;text-transform:uppercase;letter-spacing:.5px}
tbody tr:nth-child(even){background:#f9fafb}
tbody td{padding:7px 10px;border-bottom:1px solid #e5e7eb;font-size:11px}
.num{text-align:right;font-weight:700}
.rev{color:#059669}.dep{color:#dc2626}
.summary{display:flex;gap:24px;margin-top:16px;padding-top:16px;border-top:2px solid #e5e7eb}
.sum-box{flex:1;background:#f9fafb;border-radius:6px;padding:12px 16px;text-align:center}
.sum-box .label{font-size:10px;color:#6b7280;text-transform:uppercase;letter-spacing:.5px;margin-bottom:4px}
.sum-box .val{font-size:18px;font-weight:800}
.sum-box.rev .val{color:#059669}.sum-box.dep .val{color:#dc2626}
.sum-box.sol .val{color:${solde >= 0 ? '#059669' : '#dc2626'}}
.footer{margin-top:24px;font-size:10px;color:#9ca3af;text-align:right}
@media print{body{padding:10px}@page{margin:10mm}}
</style></head><body>
<h1>Rapport de Transactions</h1>
<div class="subtitle">Généré le ${new Date().toLocaleString('fr-FR')}</div>
${filterLabel ? `<div class="filters">Filtres : ${filterLabel}</div>` : ''}
<table>
<thead><tr>
  <th>Référence</th><th>Date</th><th>Type</th><th>Catégorie</th>
  <th>Montant</th><th>Statut</th><th>Description</th>
</tr></thead>
<tbody>${lignesHTML}</tbody>
</table>
<div class="summary">
  <div class="sum-box rev"><div class="label">Revenus validés</div><div class="val">${totalRev.toLocaleString('fr-FR')} FCFA</div></div>
  <div class="sum-box dep"><div class="label">Dépenses validées</div><div class="val">${totalDep.toLocaleString('fr-FR')} FCFA</div></div>
  <div class="sum-box sol"><div class="label">Solde</div><div class="val">${solde.toLocaleString('fr-FR')} FCFA</div></div>
</div>
<div class="footer">MaGestionHotel – ${rows.length} transaction(s) affichée(s)</div>
<script>window.onload=function(){window.print()}</script>
</body></html>`;

    const w = window.open('', '_blank');
    if (w) { w.document.write(html); w.document.close(); }
    this.messageService.add({ severity: 'success', summary: 'PDF', detail: `${rows.length} transaction(s) prêtes à imprimer` });
  }

  exportExcel(): void {
    const rows = this.filteredTransactions;
    if (rows.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Vide', detail: 'Aucune transaction à exporter' });
      return;
    }

    // Génération d'un fichier CSV compatible Excel (avec BOM UTF-8)
    const sep    = '\t'; // tabulation → Excel l'interprète correctement
    const header = ['Référence', 'Date', 'Type', 'Catégorie', 'Montant', 'Mode paiement', 'Statut', 'Description'].join(sep);
    const lines  = rows.map(t => [
      t.reference ?? '',
      t.dateTransaction ? new Date(t.dateTransaction).toLocaleString('fr-FR') : '',
      this.getTypeLabel(t.type),
      t.categorie ?? '',
      t.montant ?? 0,
      t.modePaiement ? this.getModePaiementLabel(t.modePaiement) : '',
      this.getStatutLabel(t.statut),
      (t.description ?? '').replace(/\t/g, ' ')
    ].join(sep));

    const content = '\uFEFF' + [header, ...lines].join('\n');
    const blob    = new Blob([content], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    this.downloadBlob(blob, `transactions_${this.today()}.xls`);
    this.messageService.add({ severity: 'success', summary: 'Export Excel', detail: `${rows.length} transaction(s) exportée(s)` });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href     = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  private today(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  }

  private buildFilterLabel(): string {
    const parts: string[] = [];
    if (this.searchTerm)     parts.push(`Recherche: "${this.searchTerm}"`);
    if (this.selectedType)      parts.push(`Type: ${this.getTypeLabel(this.selectedType)}`);
    if (this.selectedStatut)    parts.push(`Statut: ${this.getStatutLabel(this.selectedStatut)}`);
    if (this.selectedCategorie) parts.push(`Catégorie: ${this.selectedCategorie}`);
    if (this.dateDebut)         parts.push(`Du: ${new Date(this.dateDebut).toLocaleDateString('fr-FR')}`);
    if (this.dateFin)        parts.push(`Au: ${new Date(this.dateFin).toLocaleDateString('fr-FR')}`);
    return parts.join(' | ');
  }

  // ── Formatage ─────────────────────────────────────────────────────────────────

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
      .format(amount) + ' FCFA';
  }

  formatDateTime(date: string | Date | undefined): string {
    if (!date) return '-';
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(new Date(date));
  }

  getTypeLabel(type: TypeTransaction): string {
    return TYPE_TRANSACTION_LABELS[type] ?? type;
  }

  getStatutLabel(statut: StatutTransaction): string {
    return STATUT_TRANSACTION_LABELS[statut] ?? statut;
  }

  getModePaiementLabel(mode: string): string {
    return MODE_PAIEMENT_LABELS[mode as ModePaiementTransaction] ?? mode;
  }

  getSeverity(type: TypeTransaction): 'success' | 'danger' {
    return TYPE_TRANSACTION_COLORS[type] as 'success' | 'danger';
  }

  getStatutSeverity(statut: StatutTransaction): 'success' | 'warn' | 'danger' | 'info' {
    const s = STATUT_TRANSACTION_COLORS[statut];
    return s === 'warning' ? 'warn' : s as any;
  }
}
