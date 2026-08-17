import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    if (currentUser && this.authService.isAuthenticated) {
      // Vérifier les rôles si spécifiés dans la route
      if (route.data['roles']) {
        const requiredRoles = route.data['roles'] as string[];
        const userRoles = currentUser.roles || [];

        const hasRole = requiredRoles.some(role => userRoles.includes(role));

        if (!hasRole) {
          // L'utilisateur n'a pas les permissions nécessaires
          this.router.navigate(['/unauthorized']);
          return false;
        }
      }

      // Vérifier le type de compte si spécifié
      if (route.data['accountType']) {
        const requiredAccountType = route.data['accountType'] as string;

        if (currentUser.accountType !== requiredAccountType) {
          this.router.navigate(['/unauthorized']);
          return false;
        }
      }

      return true;
    }

    // L'utilisateur n'est pas connecté - rediriger vers la page de connexion appropriée
    const loginPath = route.data['accountType'] === 'ADMIN' ? '/admin/login' : '/login';
    this.router.navigate([loginPath], { queryParams: { returnUrl: state.url } });
    return false;
  }
}
