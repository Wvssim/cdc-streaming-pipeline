import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DocumentsService } from '../../core/services/documents.service';
import { AuditService } from '../../core/services/audit.service';
import { NotificationsService } from '../../core/services/notifications.service';
import { SiemAlertsService } from '../../core/services/siem-alerts.service';
import { IntegrityService } from '../../core/services/integrity.service';
import { DocumentDto, IntegrityEntry, OcrEntry } from '../../core/models/document.model';
import { AuditEntry } from '../../core/models/audit.model';
import { NotificationEntry } from '../../core/models/notification.model';
import { SiemAlert } from '../../core/models/siem-alert.model';
import { StatusPill, StatusTone } from '../../shared/components/status-pill/status-pill';
import { Icon, IconName } from '../../shared/components/icon/icon';

interface TimelineItem {
  service: string;
  icon: IconName;
  tone: StatusTone;
  status: string;
  detail: string;
  at: string | null;
}

@Component({
  selector: 'app-document-detail',
  imports: [RouterLink, StatusPill, Icon],
  templateUrl: './document-detail.html',
  styleUrl: './document-detail.scss',
})
export class DocumentDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly documentsService = inject(DocumentsService);
  private readonly auditService = inject(AuditService);
  private readonly notificationsService = inject(NotificationsService);
  private readonly siemAlertsService = inject(SiemAlertsService);
  private readonly integrityService = inject(IntegrityService);

  readonly docId = Number(this.route.snapshot.paramMap.get('id'));

  readonly document = signal<DocumentDto | null>(null);
  readonly integrityEntry = signal<IntegrityEntry | null>(null);
  readonly ocrEntry = signal<OcrEntry | null>(null);
  readonly auditEntries = signal<AuditEntry[]>([]);
  readonly notifications = signal<NotificationEntry[]>([]);
  readonly alerts = signal<SiemAlert[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly timeline = computed<TimelineItem[]>(() => {
    const audit = this.auditEntries()[0] ?? null;
    const notification = this.notifications()[0] ?? null;
    const integrity = this.integrityEntry();
    const ocr = this.ocrEntry();
    const alerts = this.alerts();

    return [
      {
        service: 'Piste d\'audit',
        icon: 'history',
        tone: audit ? 'success' : 'neutral',
        status: audit ? 'Tracé' : 'En attente',
        detail: audit ? `${audit.action} par ${audit.actor}` : 'Aucun événement consommé',
        at: audit?.occurredAt ?? null,
      },
      {
        service: 'Notification',
        icon: 'mail',
        tone: notification ? 'success' : 'neutral',
        status: notification ? 'Envoyée' : 'En attente',
        detail: notification ? `À ${notification.recipient}` : 'Aucun e-mail envoyé',
        at: notification?.sentAt ?? null,
      },
      {
        service: 'Intégrité',
        icon: 'link',
        tone: integrity ? 'success' : 'neutral',
        status: integrity ? 'Vérifié' : 'En attente',
        detail: integrity ? `Maillon #${integrity.seq} de la chaîne` : 'Pas encore de maillon',
        at: integrity?.createdAt ?? null,
      },
      {
        service: 'OCR',
        icon: 'scan-text',
        tone: ocr ? 'success' : 'warning',
        status: ocr ? 'Extrait' : 'En cours',
        detail: ocr ? `Moteur ${ocr.engine}` : 'Texte pas encore extrait',
        at: ocr?.extractedAt ?? null,
      },
      {
        service: 'SIEM',
        icon: alerts.length > 0 ? 'alert-triangle' : 'shield',
        tone: alerts.length > 0 ? 'danger' : 'success',
        status: alerts.length > 0 ? `${alerts.length} alerte(s)` : 'Aucune anomalie',
        detail: alerts.length > 0 ? alerts[0].detail : 'Aucune règle déclenchée',
        at: alerts[0]?.raisedAt ?? null,
      },
    ];
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    forkJoin({
      document: this.documentsService.getById(this.docId),
      integrity: this.integrityService.list().pipe(catchError(() => of([] as IntegrityEntry[]))),
      ocr: this.documentsService.getOcr(this.docId),
      audit: this.auditService.list().pipe(catchError(() => of([] as AuditEntry[]))),
      notifications: this.notificationsService.list().pipe(catchError(() => of([] as NotificationEntry[]))),
      alerts: this.siemAlertsService.list().pipe(catchError(() => of([] as SiemAlert[]))),
    }).subscribe({
      next: ({ document, integrity, ocr, audit, notifications, alerts }) => {
        this.document.set(document);
        this.integrityEntry.set(integrity.find((e) => e.docId === this.docId) ?? null);
        this.ocrEntry.set(ocr);
        this.auditEntries.set(audit.filter((a) => a.docId === this.docId));
        this.notifications.set(notifications.filter((n) => n.docId === this.docId));
        this.alerts.set(alerts.filter((a) => a.docId === this.docId));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger le détail de ce document.');
      },
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
