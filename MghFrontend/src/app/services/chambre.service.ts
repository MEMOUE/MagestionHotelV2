import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environment/environment';

// Interface compatible avec le backend Spring Boot
export interface Chambre {
  id?: number;
  numero: string;
  type: string;  // TypeChambre enum côté backend
  prixParNuit: number;
  capacite: number;  // Capacité totale (adultes + enfants)
  superficie: number;
  description?: string;
  statut: string;  // StatutChambre enum côté backend
  etage: number;

  // Équipements (noms exacts du backend)
  wifi?: boolean;
  climatisation?: boolean;
  television?: boolean;
  minibar?: boolean;
  coffre?: boolean;  // PAS coffre_fort
  balcon?: boolean;
  vueMer?: boolean;  // PAS vue_mer

  // Champs en lecture seule
  hotelId?: number;
  hotelName?: string;
  images?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ChambreFilter {
  hotelId?: number;
  type?: string;
  etage?: number;
  capacite?: number;
  prixMin?: number;
  prixMax?: number;
  statut?: string;
}

// Interface de réponse API
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class ChambreService {
  private apiUrl = `${environment.apiUrl}/chambres`;
  private chambresSubject = new BehaviorSubject<Chambre[]>([]);
  public chambres$ = this.chambresSubject.asObservable();

  constructor(private http: HttpClient) {
    console.log('🏗️ ChambreService initialisé avec apiUrl:', this.apiUrl);
  }

  /**
   * Récupère toutes les chambres avec filtres optionnels
   */
  getChambres(filters?: ChambreFilter): Observable<Chambre[]> {
    console.log('📥 Récupération des chambres avec filtres:', filters);

    let params = new HttpParams();
    if (filters) {
      if (filters.hotelId) params = params.set('hotelId', filters.hotelId.toString());
      if (filters.type) params = params.set('type', filters.type);
      if (filters.etage !== undefined) params = params.set('etage', filters.etage.toString());
      if (filters.capacite) params = params.set('capacite', filters.capacite.toString());
      if (filters.prixMin) params = params.set('prixMin', filters.prixMin.toString());
      if (filters.prixMax) params = params.set('prixMax', filters.prixMax.toString());
      if (filters.statut) params = params.set('statut', filters.statut);
    }

    return this.http.get<ApiResponse<Chambre[]>>(this.apiUrl, { params }).pipe(
      map(response => {
        console.log('✅ Chambres récupérées:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la récupération');
      }),
      tap(chambres => this.chambresSubject.next(chambres)),
      catchError(this.handleError)
    );
  }

  /**
   * Récupère une chambre par son ID
   */
  getChambre(id: number): Observable<Chambre> {
    console.log('📥 Récupération de la chambre ID:', id);

    return this.http.get<ApiResponse<Chambre>>(`${this.apiUrl}/${id}`).pipe(
      map(response => {
        console.log('✅ Chambre récupérée:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la récupération');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Crée une nouvelle chambre
   */
  createChambre(chambre: Chambre): Observable<Chambre> {
    console.log('📤 Création de chambre:', chambre);

    return this.http.post<ApiResponse<Chambre>>(`${this.apiUrl}/create`, chambre).pipe(
      map(response => {
        console.log('✅ Chambre créée:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la création');
      }),
      tap(newChambre => {
        const currentChambres = this.chambresSubject.value;
        this.chambresSubject.next([...currentChambres, newChambre]);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Met à jour une chambre existante
   */
  updateChambre(id: number, chambre: Partial<Chambre>): Observable<Chambre> {
    console.log('📤 Mise à jour de la chambre ID:', id, chambre);

    return this.http.put<ApiResponse<Chambre>>(`${this.apiUrl}/${id}`, chambre).pipe(
      map(response => {
        console.log('✅ Chambre mise à jour:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la mise à jour');
      }),
      tap(updatedChambre => {
        const currentChambres = this.chambresSubject.value;
        const index = currentChambres.findIndex(c => c.id === id);
        if (index !== -1) {
          currentChambres[index] = updatedChambre;
          this.chambresSubject.next([...currentChambres]);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Supprime une chambre
   */
  deleteChambre(id: number): Observable<void> {
    console.log('🗑️ Suppression de la chambre ID:', id);

    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).pipe(
      map(response => {
        console.log('✅ Chambre supprimée:', response);
        if (!response.success) {
          throw new Error(response.message || 'Erreur lors de la suppression');
        }
      }),
      tap(() => {
        const currentChambres = this.chambresSubject.value;
        this.chambresSubject.next(currentChambres.filter(c => c.id !== id));
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Met à jour le statut d'une chambre
   */
  updateStatut(id: number, statut: string): Observable<void> {
    console.log('📤 Mise à jour du statut chambre ID:', id, 'vers', statut);

    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/statut?statut=${statut}`, {}).pipe(
      map(response => {
        console.log('✅ Statut mis à jour:', response);
        if (!response.success) {
          throw new Error(response.message || 'Erreur lors de la mise à jour du statut');
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Recherche de chambres par mot-clé
   */
  searchChambres(keyword: string): Observable<Chambre[]> {
    console.log('🔍 Recherche de chambres avec:', keyword);

    return this.http.get<ApiResponse<Chambre[]>>(
      `${this.apiUrl}/search?keyword=${encodeURIComponent(keyword)}`
    ).pipe(
      map(response => {
        console.log('✅ Résultats de recherche:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la recherche');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Récupère les chambres par statut
   */
  getChambresByStatut(statut: string): Observable<Chambre[]> {
    console.log('📥 Récupération des chambres par statut:', statut);

    return this.http.get<ApiResponse<Chambre[]>>(`${this.apiUrl}/statut/${statut}`).pipe(
      map(response => {
        console.log('✅ Chambres récupérées par statut:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la récupération');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Récupère les chambres par type
   */
  getChambresByType(type: string): Observable<Chambre[]> {
    console.log('📥 Récupération des chambres par type:', type);

    return this.http.get<ApiResponse<Chambre[]>>(`${this.apiUrl}/type/${type}`).pipe(
      map(response => {
        console.log('✅ Chambres récupérées par type:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la récupération');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Vérifie la disponibilité des chambres
   */
  getChambresDisponibles(
    dateArrivee: string,
    dateDepart: string,
    typeChambre?: string,
    nombrePersonnes?: number
  ): Observable<Chambre[]> {
    console.log('📥 Vérification disponibilité:', { dateArrivee, dateDepart, typeChambre, nombrePersonnes });

    const params: any = {
      dateArrivee,
      dateDepart
    };

    if (typeChambre) params.typeChambre = typeChambre;
    if (nombrePersonnes) params.nombrePersonnes = nombrePersonnes;

    return this.http.post<ApiResponse<Chambre[]>>(`${this.apiUrl}/disponibilite`, params).pipe(
      map(response => {
        console.log('✅ Chambres disponibles:', response);
        if (response.success) {
          return response.data;
        }
        throw new Error(response.message || 'Erreur lors de la vérification');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Gestion des erreurs HTTP
   */
  private handleError(error: any): Observable<never> {
    console.error('❌ Erreur HTTP:', error);

    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      if (error.error) {
        if (error.error.message) {
          errorMessage = error.error.message;
        } else if (error.error.data) {
          // Gérer les erreurs de validation
          const validationErrors = Object.values(error.error.data).join(', ');
          errorMessage = validationErrors;
        } else if (typeof error.error === 'string') {
          errorMessage = error.error;
        }
      } else {
        errorMessage = `Code d'erreur: ${error.status}\nMessage: ${error.message}`;
      }
    }

    return throwError(() => new Error(errorMessage));
  }


  updateStatutChambre(id: number, statut: string): Observable<any> {
    return this.http.put<ApiResponse<void>>(
      `${this.apiUrl}/${id}/statut`,
      null,
      { params: { statut } }
    ).pipe(
      map(response => {
        if (!response.success) {
          throw new Error(response.message);
        }
        return response;
      }),
      catchError(this.handleError)
    );
  }
}
