import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG Modules
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CheckboxModule } from 'primeng/checkbox';

import { MessageService, ConfirmationService } from 'primeng/api';
import { ClientService } from '../../../services/client.service';
import { Client } from '../../../models/client.model';

@Component({
  selector: 'app-listeclient',
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
    CheckboxModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './listeclient.html',
  styleUrl: './listeclient.css'
})
export class Listeclient implements OnInit {
  clients: Client[] = [];
  loading: boolean = true;
  searchKeyword: string = '';
  showFidelesOnly: boolean = false;

  // Seuil à partir duquel un client est considéré "fidèle" (au moins 3 visites)
  readonly SEUIL_FIDELE = 3;

  // Ids des clients fidèles (calculés côté backend via le nombre de réservations)
  fideleClientIds = new Set<number>();

  // Statistiques
  stats = {
    total: 0,
    avecEmail: 0,
    fideles: 0
  };

  constructor(
    private clientService: ClientService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadFideleClientIds();
    this.loadClients();
  }

  loadFideleClientIds(): void {
    this.clientService.getClientsFideles(this.SEUIL_FIDELE).subscribe({
      next: (response) => {
        if (response.success) {
          this.fideleClientIds = new Set(response.data.map(c => c.id!));
          this.calculateStats();
        }
      }
    });
  }

  isFidele(client: Client): boolean {
    return client.id != null && this.fideleClientIds.has(client.id);
  }

  loadClients(): void {
    this.loading = true;

    if (this.showFidelesOnly) {
      this.loadClientsFideles();
    } else if (this.searchKeyword.trim()) {
      this.searchClients();
    } else {
      this.loadAllClients();
    }
  }

  loadAllClients(): void {
    this.clientService.getClients().subscribe({
      next: (response) => {
        if (response.success) {
          this.clients = response.data;
          this.calculateStats();
          this.loading = false;
        }
      },
      error: (error) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des clients'
        });
        this.loading = false;
      }
    });
  }

  searchClients(): void {
    this.clientService.searchClients(this.searchKeyword).subscribe({
      next: (response) => {
        if (response.success) {
          this.clients = response.data;
          this.calculateStats();
          this.loading = false;
        }
      },
      error: (error) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la recherche'
        });
        this.loading = false;
      }
    });
  }

  loadClientsFideles(): void {
    this.clientService.getClientsFideles(3).subscribe({
      next: (response) => {
        if (response.success) {
          this.clients = response.data;
          this.calculateStats();
          this.loading = false;
        }
      },
      error: (error) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des clients fidèles'
        });
        this.loading = false;
      }
    });
  }

  calculateStats(): void {
    this.stats.total = this.clients.length;
    this.stats.avecEmail = this.clients.filter(c => c.email && c.email.trim() !== '').length;
    this.stats.fideles = this.showFidelesOnly
      ? this.clients.length
      : this.clients.filter(c => this.isFidele(c)).length;
  }

  onSearch(): void {
    this.loadClients();
  }

  onToggleFideles(): void {
    this.loadClients();
  }

  viewClient(client: Client): void {
    this.router.navigate(['/clients', client.id]);
  }

  editClient(client: Client): void {
    this.router.navigate(['/clients', client.id, 'edit']);
  }

  createClient(): void {
    this.router.navigate(['/clients/create']);
  }

  deleteClient(client: Client): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le client ${client.prenom} ${client.nom} ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        this.clientService.deleteClient(client.id!).subscribe({
          next: (response) => {
            if (response.success) {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Client supprimé avec succès'
              });
              this.loadClients();
            }
          },
          error: (error) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error?.message || 'Erreur lors de la suppression'
            });
          }
        });
      }
    });
  }

  getFullName(client: Client): string {
    return `${client.prenom} ${client.nom}`;
  }
}
