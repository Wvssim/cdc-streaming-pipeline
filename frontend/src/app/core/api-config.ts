// Ports applicatifs : voir docs/plan-6-semaines.md (documents-api 8081, audit-service 8082,
// notification-service 8084, blockchain-service 8085, siem-service 8086, ocr-service 8087).
// Front en dev sur :4200, CORS ouvert côté services pour cette origine.
export const API = {
  auth: 'http://localhost:8081/api/auth',
  documents: 'http://localhost:8081/api/documents',
  audit: 'http://localhost:8082/api/audit',
  notifications: 'http://localhost:8084/api/notifications',
  integrity: 'http://localhost:8085/api/integrity',
  alerts: 'http://localhost:8086/api/alerts',
  ocr: 'http://localhost:8087/api/ocr',
};
