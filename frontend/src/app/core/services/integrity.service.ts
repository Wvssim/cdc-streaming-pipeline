import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../api-config';
import { IntegrityEntry, VerifyResponse } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class IntegrityService {
  private readonly http = inject(HttpClient);

  list(): Observable<IntegrityEntry[]> {
    return this.http.get<IntegrityEntry[]>(API.integrity);
  }

  verify(): Observable<VerifyResponse> {
    return this.http.get<VerifyResponse>(`${API.integrity}/verify`);
  }
}
