import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { HotelProfile } from './hotel-profile.service';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Gestion des comptes hôtel réservée à l'administrateur système (ROLE_ADMIN). */
@Injectable({
  providedIn: 'root'
})
export class AdminHotelService {
  private apiUrl = `${environment.apiUrl}/hotels`;

  constructor(private http: HttpClient) {}

  getAllHotels(): Observable<ApiResponse<HotelProfile[]>> {
    return this.http.get<ApiResponse<HotelProfile[]>>(this.apiUrl);
  }

  searchHotels(keyword: string): Observable<ApiResponse<HotelProfile[]>> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<ApiResponse<HotelProfile[]>>(`${this.apiUrl}/search`, { params });
  }

  toggleStatus(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  deleteHotel(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  /** Envoie immédiatement le rapport financier du mois écoulé au propriétaire de l'hôtel. */
  sendFinancialReport(id: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${id}/rapport-financier`, {});
  }
}
