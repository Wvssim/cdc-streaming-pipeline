import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../api-config';
import { AuditEntry } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);

  list(): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(API.audit);
  }
}
