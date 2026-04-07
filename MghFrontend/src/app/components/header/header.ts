import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PermissionService, Permission } from '../../services/permission.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class Header implements OnInit, OnDestroy {
  protected readonly isDarkMode = signal(false);
  protected readonly isScrolled = signal(false);
  showMobileMenu = false;
  showProfileMenu = false;

  // État d'authentification
  isAuthenticated = false;
  currentUser: any = null;
  private authSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private router: Router,
    public permissionService: PermissionService
  ) {
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = savedTheme === 'dark' || (!savedTheme && prefersDark);
    this.isDarkMode.set(isDark);
    this.applyTheme(isDark);

    if (typeof window !== 'undefined') {
      window.addEventListener('scroll', () => {
        this.isScrolled.set(window.scrollY > 10);
      });
    }
  }

  ngOnInit(): void {
    this.authSubscription = this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      this.isAuthenticated = !!user;
    });
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  // ─── Permissions de visibilité ────────────────────────────────

  get canSeeReservations(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_RESERVATIONS, Permission.CREER_RESERVATION
      );
  }

  get canSeeRestaurant(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_COMMANDES, Permission.CREER_COMMANDE
      );
  }

  get canSeeFinances(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_COMPTABILITE, Permission.MODIFIER_COMPTABILITE
      );
  }

  get canSeeStocks(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_STOCK, Permission.MODIFIER_STOCK
      );
  }

  get canSeeChambres(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_RESERVATIONS, Permission.VOIR_CONFIGURATION
      );
  }

  get canSeePlanning(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasPermission(Permission.VOIR_RESERVATIONS);
  }

  get canSeeEmployes(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_EMPLOYES, Permission.GERER_EMPLOYES
      );
  }

  get canSeeClients(): boolean {
    return this.permissionService.isHotelAccount ||
      this.permissionService.hasAnyPermission(
        Permission.VOIR_RESERVATIONS, Permission.CREER_RESERVATION
      );
  }

  // ─── Rôle affiché ─────────────────────────────────────────────

  get userRoleLabel(): string {
    if (this.permissionService.isHotelAccount) return 'Administrateur';
    const roles = this.currentUser?.roles;
    if (!roles || roles.length === 0) return 'Employé';
    return roles[0].charAt(0).toUpperCase() + roles[0].slice(1).toLowerCase();
  }

  // ─── Navigation mobile fiable ─────────────────────────────────

  /**
   * Ferme le menu PUIS navigue via le Router.
   * Évite le conflit routerLink + (click) + position:fixed sur mobile.
   */
  navigateMobile(path: string): void {
    this.closeMobileMenu();
    // Petit délai pour laisser l'animation de fermeture se terminer
    setTimeout(() => {
      this.router.navigate([path]);
    }, 50);
  }

  /**
   * Vérifie si la route courante correspond au chemin donné.
   * Utilisé pour le style .active sur les liens mobiles.
   */
  isActiveRoute(path: string): boolean {
    return this.router.url === path || this.router.url.startsWith(path + '/');
  }

  // ─── Actions existantes ───────────────────────────────────────

  toggleTheme(): void {
    const newValue = !this.isDarkMode();
    this.isDarkMode.set(newValue);
    this.applyTheme(newValue);
    localStorage.setItem('theme', newValue ? 'dark' : 'light');
  }

  private applyTheme(isDark: boolean): void {
    if (typeof document !== 'undefined') {
      if (isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }
  }

  toggleMobileMenu(): void {
    this.showMobileMenu = !this.showMobileMenu;
    this.showProfileMenu = false;
    if (typeof document !== 'undefined') {
      document.body.style.overflow = this.showMobileMenu ? 'hidden' : '';
    }
  }

  closeMobileMenu(): void {
    this.showMobileMenu = false;
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  closeProfileMenu(): void {
    this.showProfileMenu = false;
  }

  logout(): void {
    this.closeMobileMenu();
    this.closeProfileMenu();
    this.authService.logout();
    this.router.navigate(['/accueil']);
  }

  getUserInitials(): string {
    if (!this.currentUser) return '';

    if (this.currentUser.firstName && this.currentUser.lastName) {
      return `${this.currentUser.firstName[0]}${this.currentUser.lastName[0]}`.toUpperCase();
    }

    if (this.currentUser.name) {
      const parts = this.currentUser.name.split(' ');
      return parts.length > 1
        ? `${parts[0][0]}${parts[1][0]}`.toUpperCase()
        : parts[0].substring(0, 2).toUpperCase();
    }

    return this.currentUser.email ? this.currentUser.email[0].toUpperCase() : 'U';
  }

  getUserDisplayName(): string {
    if (!this.currentUser) return '';

    if (this.currentUser.firstName && this.currentUser.lastName) {
      return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
    }

    return this.currentUser.name || this.currentUser.email || 'Utilisateur';
  }
}
