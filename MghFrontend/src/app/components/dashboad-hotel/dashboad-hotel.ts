import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NgClass, DatePipe } from '@angular/common';
import { PermissionService, Permission } from '../../services/permission.service';
import { AuthService } from '../../services/auth.service';

interface DashboardModule {
  title: string;
  description: string;
  icon: string;
  route: string;
  gradient: string;
  permissions: Permission[];
  hotelOnly?: boolean;
}

@Component({
  selector: 'app-dashboad-hotel',
  standalone: true,
  imports: [RouterModule, NgClass, DatePipe],
  templateUrl: './dashboad-hotel.html',
  styleUrl: './dashboad-hotel.css'
})
export class DashboadHotel implements OnInit {

  visibleModules: DashboardModule[] = [];

  private allModules: DashboardModule[] = [
    {
      title: 'Réservations',
      description: 'Réservations, check-in, check-out.',
      icon: 'pi-calendar-plus',
      route: '/reservation',
      gradient: 'from-blue-500 to-indigo-600',
      permissions: [Permission.VOIR_RESERVATIONS, Permission.CREER_RESERVATION]
    },
    {
      title: 'Chambres',
      description: 'Gestion des chambres et housekeeping.',
      icon: 'pi-building',
      route: '/chambres',
      gradient: 'from-emerald-500 to-green-600',
      permissions: [Permission.VOIR_RESERVATIONS, Permission.VOIR_CONFIGURATION]
    },
    {
      title: 'Restauration',
      description: 'Restaurant, bar et room service.',
      icon: 'pi-shop',
      route: '/restauration',
      gradient: 'from-orange-500 to-amber-600',
      permissions: [Permission.VOIR_COMMANDES, Permission.CREER_COMMANDE]
    },
    {
      title: 'Stocks',
      description: 'Produits, inventaires et fournisseurs.',
      icon: 'pi-box',
      route: '/stocks',
      gradient: 'from-purple-500 to-fuchsia-600',
      permissions: [Permission.VOIR_STOCK, Permission.MODIFIER_STOCK]
    },
    {
      title: 'Finance',
      description: 'Facturation et finances.',
      icon: 'pi-wallet',
      route: '/finances',
      gradient: 'from-rose-500 to-red-600',
      permissions: [Permission.VOIR_COMPTABILITE, Permission.MODIFIER_COMPTABILITE]
    },
    {
      title: 'Personnel',
      description: 'Employés et plannings.',
      icon: 'pi-users',
      route: '/employes',
      gradient: 'from-teal-500 to-emerald-600',
      permissions: [Permission.VOIR_EMPLOYES, Permission.GERER_EMPLOYES]
    },
    {
      title: 'Clients',
      description: 'Gestion des Clients.',
      icon: 'pi-id-card',
      route: '/clients',
      gradient: 'from-sky-500 to-cyan-600',
      permissions: [Permission.VOIR_RESERVATIONS, Permission.CREER_RESERVATION]
    },
    {
      title: 'Planning',
      description: 'Planning des Réservations.',
      icon: 'pi-table',
      route: '/planning',
      gradient: 'from-cyan-500 to-sky-600',
      permissions: [Permission.VOIR_RESERVATIONS]
    },
    {
      title: 'Paramètres',
      description: "Paramètres de l'hôtel.",
      icon: 'pi-cog',
      route: '/profile',
      gradient: 'from-slate-500 to-slate-700',
      permissions: [Permission.MODIFIER_CONFIGURATION],
      hotelOnly: true
    }
  ];

  hotelName = '';
  greeting = '';
  today = new Date();

  constructor(
    private permissionService: PermissionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.visibleModules = this.allModules.filter(mod => {
      if (this.permissionService.isHotelAccount) return true;
      return this.permissionService.hasAnyPermission(...mod.permissions);
    });

    const currentUser = this.authService.currentUserValue;
    this.hotelName = currentUser?.hotelName || currentUser?.name || '';
    this.greeting = this.buildGreeting();
  }

  private buildGreeting(): string {
    const hour = this.today.getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }
}
