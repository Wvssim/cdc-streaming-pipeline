import { chromium } from 'playwright';
import path from 'node:path';
import fs from 'node:fs';

const outputDir = path.resolve('video-demo/output');
fs.mkdirSync(outputDir, { recursive: true });

const browser = await chromium.launch({
  channel: 'msedge',
  headless: true,
  args: ['--disable-web-security']
});
const context = await browser.newContext({
  viewport: { width: 1600, height: 900 },
  recordVideo: { dir: outputDir, size: { width: 1600, height: 900 } },
  acceptDownloads: true,
  locale: 'fr-FR',
  colorScheme: 'light'
});
const page = await context.newPage();
const video = page.video();

const pause = ms => page.waitForTimeout(ms);

async function title(title, subtitle, duration = 3500) {
  await page.setContent(`<!doctype html><html lang="fr"><head><meta charset="utf-8"><style>
    *{box-sizing:border-box}body{margin:0;background:linear-gradient(135deg,#07111f,#12243a 55%,#0d6b57);color:white;font-family:Segoe UI,Arial,sans-serif;height:100vh;display:grid;place-items:center}
    main{width:1200px}.tag{display:inline-block;padding:10px 18px;border:1px solid #72e0c0;border-radius:999px;color:#9ff5dc;font-size:20px;letter-spacing:.08em;text-transform:uppercase}
    h1{font-size:72px;line-height:1.05;margin:28px 0 18px;max-width:1200px}p{font-size:30px;line-height:1.4;color:#d6e2ef;max-width:1050px;margin:0}.line{width:180px;height:7px;background:#47d7ad;margin-top:38px;border-radius:10px}
  </style></head><body><main><span class="tag">Plateforme documentaire événementielle</span><h1>${title}</h1><p>${subtitle}</p><div class="line"></div></main></body></html>`);
  await pause(duration);
}

async function overlay(text, duration = 2600) {
  await page.evaluate(({ text, duration }) => {
    document.getElementById('__demo_caption')?.remove();
    const el = document.createElement('div');
    el.id = '__demo_caption';
    el.textContent = text;
    Object.assign(el.style, {position:'fixed',left:'40px',right:'40px',bottom:'34px',zIndex:'2147483647',padding:'18px 26px',borderRadius:'14px',background:'rgba(4,14,25,.92)',color:'#fff',font:'600 24px Segoe UI,Arial,sans-serif',boxShadow:'0 12px 40px rgba(0,0,0,.35)',borderLeft:'7px solid #47d7ad'});
    document.body.appendChild(el);
    setTimeout(() => el.remove(), duration);
  }, { text, duration });
  await pause(duration + 400);
}

async function visit(url, caption, wait = 4500) {
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await pause(1800);
  await overlay(caption, wait - 500);
}

try {
  await title('Démonstration fonctionnelle complète', 'Angular · Spring Boot · PostgreSQL · Debezium · Kafka · MinIO · OCR · SIEM', 5000);

  await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle', timeout: 60000 });
  await overlay('Authentification sécurisée par Spring Security et JWT', 2300);
  await page.locator('input[name="username"]').fill('wassim');
  await page.locator('input[name="password"]').fill('wassim2026');
  await pause(900);
  await page.getByRole('button', { name: /Se connecter/i }).click();
  await page.waitForURL('**/tableau-de-bord', { timeout: 30000 });
  await pause(2000);
  await overlay('Tableau de bord : état consolidé du pipeline CDC', 3500);

  await page.goto('http://localhost:4200/documents', { waitUntil: 'networkidle' });
  await overlay('Dépôt d’un document depuis l’interface Angular', 2200);
  const report = 'C:/Users/PC/Downloads/RAPPORT_CDC_EVENT_DRIVEN.pdf';
  await page.locator('input[type="file"]').setInputFiles(report);
  await page.getByText('RAPPORT_CDC_EVENT_DRIVEN.pdf', { exact: false }).first().waitFor({ timeout: 30000 });
  await pause(3500);
  await overlay('Le fichier est stocké dans MinIO ; ses métadonnées sont écrites dans PostgreSQL', 3400);

  await page.getByText('RAPPORT_CDC_EVENT_DRIVEN.pdf', { exact: false }).first().click();
  await page.waitForURL('**/documents/*');
  await pause(9000);
  await overlay('Cinq traitements indépendants : audit, notification, intégrité, OCR et SIEM', 4200);
  const downloadButton = page.getByRole('button', { name: /Télécharger/i });
  if (await downloadButton.count()) {
    const downloadPromise = page.waitForEvent('download');
    await downloadButton.click();
    await downloadPromise;
    await overlay('Téléchargement sécurisé du contenu original — RF-03', 2800);
  }

  await page.goto('http://localhost:4200/piste-audit', { waitUntil: 'networkidle' });
  await overlay('Piste d’audit alimentée automatiquement par les événements Debezium', 3500);
  await page.goto('http://localhost:4200/notifications', { waitUntil: 'networkidle' });
  await overlay('Historique des notifications produites en parallèle', 3300);
  await page.goto('http://localhost:4200/integrite', { waitUntil: 'networkidle' });
  await overlay('Registre d’intégrité : chaîne de hachage SHA-256 vérifiable', 3600);
  await page.goto('http://localhost:4200/alertes-siem', { waitUntil: 'networkidle' });
  await overlay('SIEM : fréquence anormale, extension suspecte et horaire inhabituel', 4200);

  await visit('http://localhost:8081/swagger-ui.html', 'Swagger/OpenAPI : endpoints de dépôt, consultation, téléchargement, renommage et suppression', 5200);
  await visit('http://localhost:8025', 'MailHog : preuve de réception des notifications e-mail', 4500);
  await visit('http://localhost:8080', 'Kafbat UI : cluster Kafka en mode KRaft et supervision du flux', 4300);
  await visit('http://localhost:8080/ui/clusters/cdc-cluster/all-topics', 'Topic CDC persistant : docs.public.documents', 4300);
  await visit('http://localhost:8080/ui/clusters/cdc-cluster/consumer-groups', 'Cinq consumer groups indépendants : fan-out et lag observable', 4800);

  await page.goto('http://localhost:9001', { waitUntil: 'domcontentloaded' });
  await pause(2500);
  const user = page.locator('input').first();
  const pass = page.locator('input[type="password"]');
  if (await pass.count()) {
    await user.fill('minioadmin'); await pass.fill('minioadmin');
    const login = page.getByRole('button', { name: /login/i });
    if (await login.count()) await login.click();
    await pause(3500);
  }
  const acknowledge = page.getByRole('button', { name: /acknowledge/i });
  if (await acknowledge.count()) { await acknowledge.click(); await pause(1800); }
  const documentsBucket = page.getByText('documents', { exact: true }).first();
  if (await documentsBucket.count()) { await documentsBucket.click(); await pause(2200); }
  await overlay('MinIO : stockage objet des fichiers selon le pattern Claim Check', 4000);

  await title('Résultats vérifiés', '55 tests Java réussis · Tesseract natif validé · 398,3 dépôts/minute · lag final nul', 5500);
  await title('Pipeline CDC opérationnel de bout en bout', 'Une architecture découplée, traçable, résiliente et extensible.', 5000);
} finally {
  await context.close();
  await browser.close();
}

const rawPath = await video.path();
const finalRaw = path.join(outputDir, 'demonstration-cdc-raw.webm');
if (rawPath !== finalRaw) fs.copyFileSync(rawPath, finalRaw);
console.log(finalRaw);
