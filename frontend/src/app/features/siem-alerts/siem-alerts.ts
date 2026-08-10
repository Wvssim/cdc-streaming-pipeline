import { Component, computed, inject, signal } from '@angular/core';
import { SiemAlertsService } from '../../core/services/siem-alerts.service';
import { SiemAlert } from '../../core/models/siem-alert.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { StatusPill, StatusTone } from '../../shared/components/status-pill/status-pill';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-siem-alerts',
  imports: [KpiCard, StatusPill, Icon],
  templateUrl: './siem-alerts.html',
  styleUrl: './siem-alerts.scss',
})
export class SiemAlerts {
  private readonly siemAlertsService = inject(SiemAlertsService);

  readonly rows = signal<SiemAlert[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly highSeverityCount = computed(() => this.rows().filter((r) => r.severity === 'ELEVEE').length);
  readonly lastAlertAt = computed(() => this.rows()[0]?.raisedAt ?? null);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.siemAlertsService.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les alertes SIEM (siem-service injoignable).');
      },
    });
  }

  severityTone(severity: string): StatusTone {
    switch (severity) {
      case 'ELEVEE':
        return 'danger';
      case 'MOYENNE':
        return 'warning';
      case 'FAIBLE':
        return 'neutral';
      default:
        return 'neutral';
    }
  }

  severityLabel(severity: string): string {
    switch (severity) {
      case 'ELEVEE':
        return 'Élevée';
      case 'MOYENNE':
        return 'Moyenne';
      case 'FAIBLE':
        return 'Faible';
      default:
        return severity;
    }
  }

  ruleLabel(rule: string): string {
    switch (rule) {
      case 'FREQUENCE_ANORMALE':
        return 'Fréquence anormale';
      case 'HORAIRE_INHABITUEL':
        return 'Horaire inhabituel';
      case 'EXTENSION_SUSPECTE':
        return 'Extension suspecte';
      default:
        return rule;
    }
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
