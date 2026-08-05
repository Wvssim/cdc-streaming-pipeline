import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DocumentsService } from '../../core/services/documents.service';
import { DocumentRow } from '../../core/models/document.model';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { StatusPill } from '../../shared/components/status-pill/status-pill';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-documents',
  imports: [KpiCard, StatusPill, Icon],
  templateUrl: './documents.html',
  styleUrl: './documents.scss',
})
export class Documents {
  private readonly documentsService = inject(DocumentsService);

  readonly rows = signal<DocumentRow[]>([]);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly dragOver = signal(false);
  readonly error = signal<string | null>(null);

  readonly verifiedCount = computed(() => this.rows().filter((r) => r.integrityStatus === 'verifie').length);
  readonly extractedCount = computed(() => this.rows().filter((r) => r.ocrStatus === 'verifie').length);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.documentsService.listWithStatus().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
        this.loadOcrStatuses(rows);
      },
      error: () => {
        this.loading.set(false);
        this.error.set("Impossible de charger les documents (documents-api ou blockchain-service injoignable).");
      },
    });
  }

  private loadOcrStatuses(rows: DocumentRow[]): void {
    if (rows.length === 0) return;
    const calls = rows.map((r) => this.documentsService.ocrStatusFor(r.id));
    forkJoin(calls).subscribe((statuses) => {
      this.rows.update((current) =>
        current.map((row, i) => ({ ...row, ocrStatus: statuses[i] })),
      );
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.uploadFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.uploadFile(file);
    input.value = '';
  }

  private uploadFile(file: File): void {
    this.uploading.set(true);
    this.error.set(null);
    this.documentsService.upload(file, 'wassim').subscribe({
      next: () => {
        this.uploading.set(false);
        this.refresh();
      },
      error: () => {
        this.uploading.set(false);
        this.error.set("Échec de l'upload.");
      },
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
