import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Icon } from '../../shared/components/icon/icon';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems = [
    { path: 'tableau-de-bord', label: 'Tableau de bord', icon: 'grid' as const },
    { path: 'documents', label: 'Documents', icon: 'file-text' as const },
    { path: 'piste-audit', label: "Piste d'audit", icon: 'history' as const },
    { path: 'notifications', label: 'Notifications', icon: 'mail' as const },
    { path: 'alertes-siem', label: 'Alertes SIEM', icon: 'shield' as const },
    { path: 'integrite', label: 'Intégrité', icon: 'link' as const },
  ];

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
