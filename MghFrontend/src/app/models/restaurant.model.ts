import { Produit, TypeProduit } from './produit.model';

export type ProduitMenu = Produit;

export interface LigneCommande {
  id?: number;
  produitId: number;
  produitNom?: string;
  quantite: number;
  prixUnitaire: number;
  sousTotal?: number;
  notes?: string;
}

export interface CommandeRestaurant {
  id?: number;
  numeroCommande?: string;

  nomClientExterne?: string;
  telephoneClientExterne?: string;

  clientId?: number;
  clientNom?: string;

  reservationId?: number;
  reservationNumero?: string;

  numeroTable?: string;
  statut: StatutCommandeRestaurant;
  montantTotal: number;

  /** Remise globale soustraite du montant brut */
  remiseTotale?: number;

  montantPaye?: number;
  notes?: string;
  dateCommande?: string;
  dateService?: string;

  serveurId?: number;
  serveurNom?: string;

  hotelId?: number;
  lignes: LigneCommande[];

  createdAt?: string;
  updatedAt?: string;
}

export enum StatutCommandeRestaurant {
  EN_ATTENTE = 'EN_ATTENTE',
  EN_PREPARATION = 'EN_PREPARATION',
  PRETE = 'PRETE',
  SERVIE = 'SERVIE',
  PAYEE = 'PAYEE',
  ANNULEE = 'ANNULEE'
}

export const STATUT_COMMANDE_LABELS: Record<StatutCommandeRestaurant, string> = {
  [StatutCommandeRestaurant.EN_ATTENTE]: 'En attente',
  [StatutCommandeRestaurant.EN_PREPARATION]: 'En préparation',
  [StatutCommandeRestaurant.PRETE]: 'Prête',
  [StatutCommandeRestaurant.SERVIE]: 'Servie',
  [StatutCommandeRestaurant.PAYEE]: 'Payée',
  [StatutCommandeRestaurant.ANNULEE]: 'Annulée'
};

export const STATUT_COMMANDE_COLORS: Record<StatutCommandeRestaurant, string> = {
  [StatutCommandeRestaurant.EN_ATTENTE]: 'warning',
  [StatutCommandeRestaurant.EN_PREPARATION]: 'info',
  [StatutCommandeRestaurant.PRETE]: 'success',
  [StatutCommandeRestaurant.SERVIE]: 'primary',
  [StatutCommandeRestaurant.PAYEE]: 'success',
  [StatutCommandeRestaurant.ANNULEE]: 'danger'
};

export const CATEGORIES_MENU = [
  { value: TypeProduit.ENTREE,  label: 'Entrées',   icon: 'pi-star' },
  { value: TypeProduit.PLAT,    label: 'Plats',     icon: 'pi-sun' },
  { value: TypeProduit.DESSERT, label: 'Desserts',  icon: 'pi-heart' },
  { value: TypeProduit.BOISSON, label: 'Boissons',  icon: 'pi-droplet' },
  { value: TypeProduit.AUTRE,   label: 'Autres',    icon: 'pi-box' }
];
