import { Component, computed, inject, signal } from '@angular/core';
import { DocumentsService } from '../../core/services/documents.service';
import { DocumentRow } from '../../core/models/document.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { StatusPill } from '../../shared/components/status-pill/status-pill';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-dashboard',
  imports: [KpiCard, StatusPill, Icon],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly documentsService = inject(DocumentsService);

  readonly rows = signal<DocumentRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly verifiedCount = computed(() => this.rows().filter((r) => r.integrityStatus === 'verifie').length);
  readonly recent = computed(() => this.rows().slice(0, 5));

  constructor() {
    this.documentsService.listWithStatus().subscribe({
      next: (rows) => {
        this.rows.set(rows.slice().reverse());
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set("Impossible de charger les données (documents-api ou blockchain-service injoignable).");
      },
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
