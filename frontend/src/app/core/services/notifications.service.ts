import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../api-config';
import { NotificationEntry } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);

  list(): Observable<NotificationEntry[]> {
    return this.http.get<NotificationEntry[]>(API.notifications);
  }
}
