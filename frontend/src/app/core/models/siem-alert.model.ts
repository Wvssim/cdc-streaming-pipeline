export interface SiemAlert {
  id: number;
  docId: number;
  rule: string;
  severity: string;
  detail: string;
  raisedAt: string;
}
