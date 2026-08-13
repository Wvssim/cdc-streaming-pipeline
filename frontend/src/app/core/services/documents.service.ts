import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { API } from '../api-config';
import { DocumentDto, DocumentRow, IntegrityEntry, OcrEntry } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);

  upload(file: File, uploadedBy: string): Observable<DocumentDto> {
    const form = new FormData();
    form.append('file', file);
    form.append('uploadedBy', uploadedBy);
    return this.http.post<DocumentDto>(API.documents, form);
  }

  getById(id: number): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`${API.documents}/${id}`);
  }

  getOcr(docId: number): Observable<OcrEntry | null> {
    return this.http.get<OcrEntry>(`${API.ocr}/${docId}`).pipe(catchError(() => of(null)));
  }

  /** Liste les documents et enrichit chaque ligne avec le statut intégrité (blockchain-service)
   *  et OCR (ocr-service), interrogés en parallèle par document. */
  listWithStatus(): Observable<DocumentRow[]> {
    return forkJoin({
      documents: this.http.get<DocumentDto[]>(API.documents),
      integrity: this.http.get<IntegrityEntry[]>(API.integrity).pipe(catchError(() => of([] as IntegrityEntry[]))),
    }).pipe(
      map(({ documents, integrity }) => {
        const verifiedIds = new Set(integrity.map((e) => e.docId));
        return documents.map((doc) => ({
          ...doc,
          integrityStatus: (verifiedIds.has(doc.id) ? 'verifie' : 'en_attente') as DocumentRow['integrityStatus'],
          ocrStatus: 'en_cours' as DocumentRow['ocrStatus'],
        }));
      }),
    );
  }

  ocrStatusFor(docId: number): Observable<'verifie' | 'en_attente'> {
    return this.http.get<OcrEntry>(`${API.ocr}/${docId}`).pipe(
      map(() => 'verifie' as const),
      catchError(() => of('en_attente' as const)),
    );
  }
}
