import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  Reservation,
  CreateReservationRequest,
  AppliquerRemiseReservationRequest,
  StatutReservation
} from '../models/reservation.model';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface ModifierReservationRequest {
  dateArrivee: string;
  dateDepart: string;
  nombreAdultes: number;
  nombreEnfants: number;
  notes?: string;
  demandesSpeciales?: string;
  referenceExterne?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  private apiUrl = `${environment.apiUrl}/reservations`;

  constructor(private http: HttpClient) {}

  createReservation(request: CreateReservationRequest): Observable<ApiResponse<Reservation>> {
    return this.http.post<ApiResponse<Reservation>>(this.apiUrl, request);
  }

  getAllReservations(): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(this.apiUrl);
  }

  getReservationById(id: number): Observable<ApiResponse<Reservation>> {
    return this.http.get<ApiResponse<Reservation>>(`${this.apiUrl}/${id}`);
  }

  getReservationByNumero(numero: string): Observable<ApiResponse<Reservation>> {
    return this.http.get<ApiResponse<Reservation>>(`${this.apiUrl}/numero/${numero}`);
  }

  getReservationsByStatut(statut: StatutReservation): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/statut/${statut}`);
  }

  getReservationsByClient(clientId: number): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/client/${clientId}`);
  }

  getArriveesAujourdhui(): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/arrivees-aujourdhui`);
  }

  getDepartsAujourdhui(): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/departs-aujourdhui`);
  }

  getReservationsEnCours(): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/en-cours`);
  }

  getReservationsAVenir(): Observable<ApiResponse<Reservation[]>> {
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/a-venir`);
  }

  searchReservations(keyword: string): Observable<ApiResponse<Reservation[]>> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<ApiResponse<Reservation[]>>(`${this.apiUrl}/search`, { params });
  }

  modifierReservation(
    id: number,
    request: ModifierReservationRequest
  ): Observable<ApiResponse<Reservation>> {
    return this.http.put<ApiResponse<Reservation>>(`${this.apiUrl}/${id}/modifier`, request);
  }

  // ✅ NOUVEAU : Appliquer une remise sur une réservation
  appliquerRemise(
    id: number,
    request: AppliquerRemiseReservationRequest
  ): Observable<ApiResponse<Reservation>> {
    return this.http.put<ApiResponse<Reservation>>(`${this.apiUrl}/${id}/remise`, request);
  }

  doCheckin(id: number): Observable<ApiResponse<Reservation>> {
    return this.http.post<ApiResponse<Reservation>>(`${this.apiUrl}/${id}/checkin`, {});
  }

  doCheckout(id: number): Observable<ApiResponse<Reservation>> {
    return this.http.post<ApiResponse<Reservation>>(`${this.apiUrl}/${id}/checkout`, {});
  }

  cancelReservation(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  addPaiement(reservationId: number, paiementData: { montant: number; modePaiement: string }): Observable<any> {
    const params = new HttpParams()
      .set('montant', paiementData.montant.toString())
      .set('modePaiement', paiementData.modePaiement);
    return this.http.post<any>(`${this.apiUrl}/${reservationId}/paiement`, {}, { params });
  }
}
