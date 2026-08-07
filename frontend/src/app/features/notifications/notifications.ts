import { Component, computed, inject, signal } from '@angular/core';
import { NotificationsService } from '../../core/services/notifications.service';
import { NotificationEntry } from '../../core/models/notification.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { StatusPill, StatusTone } from '../../shared/components/status-pill/status-pill';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-notifications',
  imports: [KpiCard, StatusPill, Icon],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications {
  private readonly notificationsService = inject(NotificationsService);

  readonly rows = signal<NotificationEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly sentCount = computed(() => this.rows().filter((r) => r.status === 'ENVOYE').length);
  readonly failedCount = computed(() => this.rows().filter((r) => r.status === 'ECHEC').length);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.notificationsService.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les notifications (notification-service injoignable).');
      },
    });
  }

  statusFor(status: string): StatusTone {
    switch (status) {
      case 'ENVOYE':
        return 'success';
      case 'ECHEC':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ENVOYE':
        return 'Envoyé';
      case 'ECHEC':
        return 'Échec';
      default:
        return status;
    }
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
