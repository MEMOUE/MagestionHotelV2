import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environment/environment';

export interface LoginRequest {
  email: string;
  password: string;
  accountType: 'HOTEL' | 'USER' | 'ADMIN';
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
  taxNumber?: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  name: string;
  firstName?: string;
  lastName?: string;
  accountType: 'HOTEL' | 'USER' | 'ADMIN';
  hotelId: number;
  hotelName: string;
  roles?: string[];
  permissions?: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface ForgotPasswordRequest {
  email: string;
  accountType: 'HOTEL' | 'USER' | 'ADMIN';
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}`;
  private currentUserSubject: BehaviorSubject<LoginResponse | null>;
  public currentUser: Observable<LoginResponse | null>;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<LoginResponse | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): LoginResponse | null {
    return this.currentUserSubject.value;
  }

  public get isAuthenticated(): boolean {
    return !!this.currentUserValue && !!this.currentUserValue.token;
  }

  public get isAdmin(): boolean {
    return this.currentUserValue?.accountType === 'ADMIN';
  }

  public get token(): string | null {
    return this.currentUserValue?.token || null;
  }

  register(data: RegisterRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/hotels/register`, data);
  }

  login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/auth/login`, credentials)
      .pipe(
        tap(response => {
          if (response.success && response.data) {
            localStorage.setItem('currentUser', JSON.stringify(response.data));
            localStorage.setItem('token', response.data.token);
            this.currentUserSubject.next(response.data);
          }
        })
      );
  }

  logout(redirectTo: string = '/login'): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
    this.router.navigate([redirectTo]);
  }

  // ── Mot de passe oublié ─────────────────────────────────────────

  /** Étape 1 : Demande d'envoi d'email de réinitialisation */
  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/forgot-password`, request);
  }

  /** Étape 2 : Validation du token (quand l'utilisateur ouvre le lien) */
  validateResetToken(token: string): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('token', token);
    return this.http.get<ApiResponse<void>>(`${this.apiUrl}/auth/validate-token`, { params });
  }

  /** Étape 3 : Réinitialisation du mot de passe */
  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/reset-password`, request);
  }

  getAuthHeaders(): HttpHeaders {
    const token = this.token;
    if (token) {
      return new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      });
    }
    return new HttpHeaders({
      'Content-Type': 'application/json'
    });
  }
}
