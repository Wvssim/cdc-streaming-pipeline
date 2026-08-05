export interface DocumentDto {
  id: number;
  filename: string;
  contentType: string;
  size: number;
  storageKey: string;
  uploadedBy: string;
  uploadedAt: string;
}

export interface IntegrityEntry {
  seq: number;
  docId: number;
  docHash: string;
  prevHash: string;
  chainHash: string;
  createdAt: string;
}

export interface OcrEntry {
  docId: number;
  text: string;
  engine: string;
  extractedAt: string;
}

export type DocRowStatus = 'verifie' | 'en_cours' | 'en_attente';

export interface DocumentRow extends DocumentDto {
  integrityStatus: DocRowStatus;
  ocrStatus: DocRowStatus;
}
