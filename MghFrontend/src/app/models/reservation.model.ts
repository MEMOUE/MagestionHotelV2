export interface Reservation {
  id?: number;
  numeroReservation?: string;
  chambreId: number;
  chambreNumero?: string;
  clientId?: number;
  clientNom?: string;
  clientPrenom?: string;
  clientTelephone?: string;
  hotelId?: number;
  dateArrivee: string | Date;
  dateDepart: string | Date;
  nombreNuits?: number;
  nombreAdultes: number;
  nombreEnfants: number;
  prixParNuit?: number;

  // Remises
  remiseJournaliere?: number;
  remiseTotale?: number;

  montantTotal?: number;
  montantPaye?: number;
  montantRestant?: number;
  statut?: StatutReservation;
  statutPaiement?: StatutPaiement;
  modePaiement?: ModePaiement;
  notes?: string;
  demandesSpeciales?: string;
  dateCheckin?: string;
  dateCheckout?: string;
  createdById?: number;
  createdByName?: string;
  checkinById?: number;
  checkinByName?: string;
  checkoutById?: number;
  checkoutByName?: string;
  referenceExterne?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateReservationRequest {
  chambreId: number;
  clientId?: number;
  newClient?: Client;
  dateArrivee: string | Date;
  dateDepart: string | Date;
  nombreAdultes: number;
  nombreEnfants: number;
  notes?: string;
  demandesSpeciales?: string;
  referenceExterne?: string;
}

export interface AppliquerRemiseReservationRequest {
  remiseJournaliere?: number;
  remiseTotale?: number;
}

export interface Client {
  id?: number;
  prenom: string;
  nom: string;
  email?: string;
  telephone: string;
  pieceIdentite?: string;
  typePiece?: string;
  dateNaissance?: string;
  nationalite?: string;
  adresse?: string;
  ville?: string;
  pays?: string;
  notes?: string;
}

export enum StatutReservation {
  EN_ATTENTE = 'EN_ATTENTE',
  CONFIRMEE = 'CONFIRMEE',
  ANNULEE = 'ANNULEE',
  EN_COURS = 'EN_COURS',
  TERMINEE = 'TERMINEE',
  NO_SHOW = 'NO_SHOW'
}

export enum StatutPaiement {
  NON_PAYE = 'NON_PAYE',
  ACOMPTE = 'ACOMPTE',
  PAYE = 'PAYE',
  REMBOURSE = 'REMBOURSE'
}

export enum ModePaiement {
  ESPECES = 'ESPECES',
  CARTE_BANCAIRE = 'CARTE_BANCAIRE',
  VIREMENT = 'VIREMENT',
  CHEQUE = 'CHEQUE',
  MOBILE_MONEY = 'MOBILE_MONEY',
  ORANGE_MONEY = 'ORANGE_MONEY',
  MTN_MONEY = 'MTN_MONEY',
  WAVE = 'WAVE',
  MOOV_MONEY = 'MOOV_MONEY'
}

export const STATUT_RESERVATION_LABELS: Record<StatutReservation, string> = {
  [StatutReservation.EN_ATTENTE]: 'En attente',
  [StatutReservation.CONFIRMEE]: 'Confirmée',
  [StatutReservation.ANNULEE]: 'Annulée',
  [StatutReservation.EN_COURS]: 'En cours',
  [StatutReservation.TERMINEE]: 'Terminée',
  [StatutReservation.NO_SHOW]: 'No-show'
};

export const STATUT_PAIEMENT_LABELS: Record<StatutPaiement, string> = {
  [StatutPaiement.NON_PAYE]: 'Non payé',
  [StatutPaiement.ACOMPTE]: 'Acompte versé',
  [StatutPaiement.PAYE]: 'Payé',
  [StatutPaiement.REMBOURSE]: 'Remboursé'
};
