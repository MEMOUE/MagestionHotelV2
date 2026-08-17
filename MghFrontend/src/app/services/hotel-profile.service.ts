import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

export interface HotelProfile {
  id?: number;
  name: string;
  email: string;
  phone?: string;
  address?: string;
  logoUrl?: string;
  taxNumber?: string;
  /** Email optionnel du propriétaire — reçoit le rapport financier mensuel automatique. */
  ownerEmail?: string;
  active?: boolean;
  subscriptionEnd?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdatePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class HotelProfileService {
  private apiUrl = `${environment.apiUrl}/hotels`;

  constructor(private http: HttpClient) {}

  // Obtenir le profil de l'hôtel connecté
  getProfile(): Observable<ApiResponse<HotelProfile>> {
    return this.http.get<ApiResponse<HotelProfile>>(`${this.apiUrl}/profile`);
  }

  // Obtenir un hôtel par ID
  getHotelById(id: number): Observable<ApiResponse<HotelProfile>> {
    return this.http.get<ApiResponse<HotelProfile>>(`${this.apiUrl}/${id}`);
  }

  // Mettre à jour le profil
  updateProfile(id: number, profile: Partial<HotelProfile>): Observable<ApiResponse<HotelProfile>> {
    return this.http.put<ApiResponse<HotelProfile>>(`${this.apiUrl}/${id}`, profile);
  }

  // Changer le mot de passe
  changePassword(id: number, request: UpdatePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/password`, request);
  }
}