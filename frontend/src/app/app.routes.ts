import { Routes } from '@angular/router';
import { Shell } from './core/shell/shell';
import { Dashboard } from './features/dashboard/dashboard';
import { Documents } from './features/documents/documents';
import { Audit } from './features/audit/audit';
import { Notifications } from './features/notifications/notifications';
import { SiemAlerts } from './features/siem-alerts/siem-alerts';
import { Integrity } from './features/integrity/integrity';
import { DocumentDetail } from './features/document-detail/document-detail';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', component: Dashboard },
      { path: 'documents', component: Documents },
      { path: 'documents/:id', component: DocumentDetail },
      { path: 'piste-audit', component: Audit },
      { path: 'notifications', component: Notifications },
      { path: 'alertes-siem', component: SiemAlerts },
      { path: 'integrite', component: Integrity },
    ],
  },
];
