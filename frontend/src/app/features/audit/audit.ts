import { Component, computed, inject, signal } from '@angular/core';
import { AuditService } from '../../core/services/audit.service';
import { AuditEntry } from '../../core/models/audit.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { StatusPill, StatusTone } from '../../shared/components/status-pill/status-pill';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-audit',
  imports: [KpiCard, StatusPill, Icon],
  templateUrl: './audit.html',
  styleUrl: './audit.scss',
})
export class Audit {
  private readonly auditService = inject(AuditService);

  readonly rows = signal<AuditEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly documentsCount = computed(() => new Set(this.rows().map((r) => r.docId)).size);
  readonly lastEventAt = computed(() => this.rows()[0]?.occurredAt ?? null);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.auditService.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set("Impossible de charger la piste d'audit (audit-service injoignable).");
      },
    });
  }

  statusFor(action: string): StatusTone {
    switch (action) {
      case 'CREATION':
        return 'success';
      case 'MISE_A_JOUR':
        return 'warning';
      case 'SUPPRESSION':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  actionLabel(action: string): string {
    switch (action) {
      case 'CREATION':
        return 'Création';
      case 'MISE_A_JOUR':
        return 'Mise à jour';
      case 'SUPPRESSION':
        return 'Suppression';
      case 'SNAPSHOT':
        return 'Snapshot';
      default:
        return action;
    }
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
