import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { IntegrityService } from '../../core/services/integrity.service';
import { IntegrityEntry, VerifyResponse } from '../../core/models/document.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-integrity',
  imports: [KpiCard, Icon],
  templateUrl: './integrity.html',
  styleUrl: './integrity.scss',
})
export class Integrity {
  private readonly integrityService = inject(IntegrityService);

  readonly rows = signal<IntegrityEntry[]>([]);
  readonly verification = signal<VerifyResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly lastEntryAt = computed(() => this.rows()[this.rows().length - 1]?.createdAt ?? null);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    forkJoin({
      list: this.integrityService.list(),
      verify: this.integrityService.verify(),
    }).subscribe({
      next: ({ list, verify }) => {
        this.rows.set(list);
        this.verification.set(verify);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set("Impossible de charger le registre d'intégrité (blockchain-service injoignable).");
      },
    });
  }

  chainLabel(): string {
    const v = this.verification();
    if (!v) return 'Inconnu';
    return v.valid ? 'Chaîne intègre' : `Rupture au maillon #${v.brokenAtSeq}`;
  }

  truncateHash(hash: string): string {
    return hash.length > 20 ? `${hash.slice(0, 10)}…${hash.slice(-8)}` : hash;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
