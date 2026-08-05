// Ports applicatifs : voir docs/plan-6-semaines.md (documents-api 8081, ocr-service 8087,
// blockchain-service 8085). Front en dev sur :4200, CORS ouvert côté services pour cette origine.
export const API = {
  documents: 'http://localhost:8081/api/documents',
  ocr: 'http://localhost:8087/api/ocr',
  integrity: 'http://localhost:8085/api/integrity',
};
