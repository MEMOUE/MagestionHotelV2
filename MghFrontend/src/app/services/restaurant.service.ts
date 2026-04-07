import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { CommandeRestaurant, StatutCommandeRestaurant, ProduitMenu } from '../models/restaurant.model';
import { TypeProduit } from '../models/produit.model';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class RestaurantService {
  private apiUrl = `${environment.apiUrl}/commandes-restaurant`;
  private produitsUrl = `${environment.apiUrl}/produits`;

  constructor(private http: HttpClient) {}

  createCommande(commande: CommandeRestaurant): Observable<ApiResponse<CommandeRestaurant>> {
    return this.http.post<ApiResponse<CommandeRestaurant>>(this.apiUrl, commande);
  }

  getCommandes(): Observable<ApiResponse<CommandeRestaurant[]>> {
    return this.http.get<ApiResponse<CommandeRestaurant[]>>(this.apiUrl);
  }

  getCommandeById(id: number): Observable<ApiResponse<CommandeRestaurant>> {
    return this.http.get<ApiResponse<CommandeRestaurant>>(`${this.apiUrl}/${id}`);
  }

  getCommandesByStatut(statut: StatutCommandeRestaurant): Observable<ApiResponse<CommandeRestaurant[]>> {
    return this.http.get<ApiResponse<CommandeRestaurant[]>>(`${this.apiUrl}/statut/${statut}`);
  }

  updateStatut(id: number, statut: StatutCommandeRestaurant): Observable<ApiResponse<CommandeRestaurant>> {
    const params = new HttpParams().set('statut', statut);
    return this.http.put<ApiResponse<CommandeRestaurant>>(`${this.apiUrl}/${id}/statut`, {}, { params });
  }

  addPaiement(id: number, montant: number): Observable<ApiResponse<CommandeRestaurant>> {
    const params = new HttpParams().set('montant', montant.toString());
    return this.http.post<ApiResponse<CommandeRestaurant>>(`${this.apiUrl}/${id}/paiement`, {}, { params });
  }

  // ✅ NOUVEAU : Appliquer une remise totale sur une commande restaurant
  appliquerRemise(id: number, montant: number): Observable<ApiResponse<CommandeRestaurant>> {
    const params = new HttpParams().set('montant', montant.toString());
    return this.http.put<ApiResponse<CommandeRestaurant>>(`${this.apiUrl}/${id}/remise`, {}, { params });
  }

  getProduitsDisponibles(): Observable<ApiResponse<ProduitMenu[]>> {
    return this.http.get<ApiResponse<ProduitMenu[]>>(`${this.produitsUrl}/menu`);
  }

  getMenuParType(type: TypeProduit): Observable<ApiResponse<ProduitMenu[]>> {
    return this.http.get<ApiResponse<ProduitMenu[]>>(`${this.produitsUrl}/menu/${type}`);
  }

  getCommandesByReservation(reservationId: number): Observable<ApiResponse<CommandeRestaurant[]>> {
    return this.http.get<ApiResponse<CommandeRestaurant[]>>(
      `${this.apiUrl}/reservation/${reservationId}`
    );
  }
}
