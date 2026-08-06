export interface AuditEntry {
  eventId: string;
  docId: number;
  action: string;
  actor: string;
  occurredAt: string;
}
