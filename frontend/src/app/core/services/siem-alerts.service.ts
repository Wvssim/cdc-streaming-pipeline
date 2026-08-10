import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../api-config';
import { SiemAlert } from '../models/siem-alert.model';

@Injectable({ providedIn: 'root' })
export class SiemAlertsService {
  private readonly http = inject(HttpClient);

  list(): Observable<SiemAlert[]> {
    return this.http.get<SiemAlert[]>(API.alerts);
  }
}
