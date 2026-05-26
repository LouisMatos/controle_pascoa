/* Service Worker — Páscoa Gestão */
const CACHE_NAME = 'pascoa-v1';

/* Recursos estáticos que vale pré-cachear (CDN) */
const STATIC_ASSETS = [
  'https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css',
  'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js',
];

/* ── Install: pré-cacheia assets CDN ───────────────────────────────────── */
self.addEventListener('install', event => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache =>
      cache.addAll(STATIC_ASSETS).catch(() => { /* ignora falhas CDN offline */ })
    )
  );
});

/* ── Activate: limpa caches antigos ────────────────────────────────────── */
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

/* ── Fetch: estratégia por tipo de recurso ──────────────────────────────── */
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);

  /* Ignora requisições não-GET e WebSocket */
  if (request.method !== 'GET') return;

  /* CDN: cache-first (Bootstrap, BI, Chart.js) */
  if (url.hostname.includes('jsdelivr.net') || url.hostname.includes('cdn.')) {
    event.respondWith(cacheFirst(request));
    return;
  }

  /* Ícones e manifest: cache-first */
  if (url.pathname.startsWith('/icons/') || url.pathname === '/manifest.json') {
    event.respondWith(cacheFirst(request));
    return;
  }

  /* Páginas da app (HTML) e recursos do servidor: network-first
     → garante que rotas autenticadas sempre busquem no servidor */
  if (url.origin === self.location.origin) {
    event.respondWith(networkFirst(request));
    return;
  }
});

/* ── Helpers ────────────────────────────────────────────────────────────── */
async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, response.clone());
    }
    return response;
  } catch {
    return new Response('Offline', { status: 503 });
  }
}

async function networkFirst(request) {
  try {
    const response = await fetch(request);
    if (response.ok && request.method === 'GET') {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, response.clone());
    }
    return response;
  } catch {
    const cached = await caches.match(request);
    if (cached) return cached;
    /* Fallback offline simples para navegação */
    if (request.headers.get('accept')?.includes('text/html')) {
      return new Response(
        `<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8">
         <meta name="viewport" content="width=device-width,initial-scale=1">
         <title>Sem Conexão</title>
         <style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;
                min-height:100vh;margin:0;background:#f8f9fa;}
                .box{text-align:center;padding:2rem;}</style></head>
         <body><div class="box">
           <div style="font-size:3rem;">🥚</div>
           <h2 style="color:#198754;">Sem conexão</h2>
           <p style="color:#6c757d;">Verifique sua internet e tente novamente.</p>
           <button onclick="location.reload()"
                   style="background:#198754;color:#fff;border:none;padding:.6rem 1.5rem;
                          border-radius:.4rem;cursor:pointer;font-size:1rem;">
             Tentar novamente
           </button>
         </div></body></html>`,
        { status: 200, headers: { 'Content-Type': 'text/html; charset=utf-8' } }
      );
    }
    return new Response('Offline', { status: 503 });
  }
}
