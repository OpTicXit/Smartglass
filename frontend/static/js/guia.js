/* U6 · Guia automatizada (textos: fragments/tour.html; estilos: theme.css "U6 · Guia automatizada").
   Recorre inicio -> catalogo -> detalle -> configurador (y su editor de corte) y vuelve al inicio.
   Pensada para no marear: un solo cambio por paso (texto, desplazamiento y resaltado se funden a la vez),
   sin desplazamientos animados ni cursor, y si lo que se explica ya se ve, la pagina no se mueve.
   Prueba opciones de verdad pero NO guarda ni envia nada:
   - bloquea todo envio de formulario mientras corre (captura en window, antes que main.js);
   - no pulsa "agregar al carrito", "cotizar" ni los interruptores de accesibilidad (esos se guardan);
   - al terminar o salir restaura el borrador del editor (localStorage sg-borrador*) tal como estaba.
   Solo pasa por paginas publicas: nada del panel de administracion.
   Estado entre paginas en sessionStorage "sg-guia" ({ i, pausado, detalle, guardado }).
   Accesible: region con aria-live, pausa (WCAG 2.2.2), siguiente y salir; Esc sale. Con movimiento
   reducido o fotosensible sigue avanzando sola, pero los cambios son inmediatos, sin fundidos. */
(function () {
  'use strict';

  var datos = document.getElementById('sg-tour-datos');
  // En una pagina de error (templates/error.html) la invitacion taparia los botones para salir de ella
  if (!datos || !datos.content || document.querySelector('.pagina-error')) return;
  var CLAVE = 'sg-guia', VISTO = 'sg-tour-visto';
  var TXT = {};
  Array.prototype.forEach.call(datos.content.querySelectorAll('section[data-id]'), function (s) {
    TXT[s.getAttribute('data-id')] = { t: s.querySelector('h2').textContent, p: s.querySelector('p').textContent };
  });
  function d(k, r) { return datos.getAttribute('data-' + k) || r; }

  function quieto() {
    var a = document.documentElement.getAttribute('data-a11y') || '';
    return a.indexOf('fotosensible') !== -1 || (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  }
  function leer() { try { return JSON.parse(sessionStorage.getItem(CLAVE) || 'null'); } catch (e) { return null; } }
  function guardar(e) { try { sessionStorage.setItem(CLAVE, JSON.stringify(e)); } catch (x) { /* sin almacenamiento */ } }
  function $(s) { return document.querySelector(s); }
  // Las pausas no son movimiento: tambien con movimiento reducido se deja ver cada cambio antes del siguiente
  function esperar(ms) { return new Promise(function (r) { setTimeout(r, saltando ? 0 : ms); }); }

  /* ---------- Lo que se restaura al terminar ---------- */
  function fotografia() {
    var f = { local: {} };
    try {
      for (var k = 0; k < localStorage.length; k++) {
        var n = localStorage.key(k);
        if (n && n.indexOf('sg-borrador') === 0) f.local[n] = localStorage.getItem(n);
      }
    } catch (e) { /* sin almacenamiento */ }
    return f;
  }
  function restaurar(f) {
    if (!f) return;
    try {
      var borrar = [];
      for (var k = 0; k < localStorage.length; k++) { var n = localStorage.key(k); if (n && n.indexOf('sg-borrador') === 0) borrar.push(n); }
      borrar.forEach(function (n) { localStorage.removeItem(n); });
      Object.keys(f.local || {}).forEach(function (n) { localStorage.setItem(n, f.local[n]); });
    } catch (e) { /* sin almacenamiento */ }
  }

  /* ---------- Resaltado: un marco aparte, para no tocar el estilo ni el recorte del elemento ---------- */
  var foco = null, focoEl = null;
  function pintarFoco() {
    if (!foco) return;
    var r = focoEl ? focoEl.getBoundingClientRect() : null;
    if (!r || !r.width || !r.height) { foco.classList.remove('is-visible'); return; }
    var izq = Math.max(4, r.left - 6), arriba = Math.max(4, r.top - 6);
    foco.style.left = izq + 'px';
    foco.style.top = arriba + 'px';
    foco.style.width = Math.max(0, Math.min(window.innerWidth - 4, r.right + 6) - izq) + 'px';
    foco.style.height = Math.max(0, Math.min(window.innerHeight - 4, r.bottom + 6) - arriba) + 'px';
    foco.classList.add('is-visible');
  }
  function marcar(el) { focoEl = el || null; pintarFoco(); }

  /** Funde en un solo cambio todo lo que pasa entre dos pasos. Sin soporte, o con movimiento reducido, es inmediato. */
  function cruzar(cambio) {
    if (quieto() || saltando || !document.startViewTransition) { cambio(); return Promise.resolve(); }
    var raiz = document.documentElement;
    raiz.classList.add('sg-guia-vt');
    var vt = document.startViewTransition(cambio);
    function nada() { /* pestana oculta: el cambio ya se aplico */ }
    vt.ready.catch(nada);
    return vt.finished.catch(nada).then(function () { raiz.classList.remove('sg-guia-vt'); });
  }

  /** Panel con desplazamiento propio que contiene el elemento (en escritorio el configurador tiene dos). */
  function panelDe(el) {
    for (var p = el.parentElement; p && p !== document.body; p = p.parentElement) {
      if (/(auto|scroll)/.test(getComputedStyle(p).overflowY) && p.scrollHeight > p.clientHeight + 1) return p;
    }
    return null;
  }
  /** Franja de la pantalla que se ve: bajo la barra fija y, si la tapa, sobre la caja del recorrido. */
  function zonaLibre(el) {
    var nav = $('.navbar'), arriba = 0, abajo = window.innerHeight;
    if (nav && /fixed|sticky/.test(getComputedStyle(nav).position)) arriba = Math.max(0, nav.getBoundingClientRect().bottom);
    if (caja && !caja.hidden) {
      var c = caja.getBoundingClientRect(), r = el.getBoundingClientRect();
      if (c.left < r.right && c.right > r.left) abajo = Math.min(abajo, c.top - 12);
    }
    return { arriba: arriba + 12, abajo: Math.max(arriba + 120, abajo) };
  }
  /** Desplazamiento que deja el rectangulo centrado en la franja (o su borde de arriba al inicio si no cabe). */
  function centrar(top, alto, z) {
    return alto > z.abajo - z.arriba ? top - z.arriba : top + alto / 2 - (z.arriba + z.abajo) / 2;
  }
  /** Deja el elemento a la vista sin animar el desplazamiento: quien lo funde es cruzar(). */
  function irA(el) {
    marcar(null);
    if (!el) return;
    if (el.closest('.sgc-modal')) {
      el.scrollIntoView({ block: 'nearest', behavior: 'auto' });
    } else {
      // Se centra en lo que queda libre entre la barra y la caja del recorrido. Dentro de un panel con
      // desplazamiento propio se mueve el panel y la ventana solo lo justo para verlo entero: asi no se
      // baja hasta el pie de pagina buscando algo que esta dentro del panel.
      var z = zonaLibre(el), r = el.getBoundingClientRect(), panel = panelDe(el), dy, pr = panel ? panel.getBoundingClientRect() : null;
      var seVe = r.top >= z.arriba && r.bottom <= z.abajo && (!pr || (r.top >= pr.top && r.bottom <= pr.bottom));
      if (!seVe) {
        if (panel) {
          // si cabe, el panel termina donde termina la pantalla (como lo pinta el configurador); si no, empieza bajo la barra
          var cabe = pr.height <= window.innerHeight - 12 - z.arriba;
          dy = cabe ? pr.bottom - (window.innerHeight - 12) : pr.top - z.arriba;
          var arriba = Math.max(z.arriba, pr.top - dy), abajo = Math.min(z.abajo, pr.bottom - dy);
          var dentro = centrar(r.top - dy, r.height, { arriba: arriba, abajo: Math.max(arriba + 80, abajo) });
          panel.scrollTo({ top: Math.max(0, panel.scrollTop + dentro), behavior: 'auto' });
        } else {
          dy = centrar(r.top, r.height, z);
          // el pie de pagina no entra en la franja libre si el elemento cabe un poco mas abajo
          var pie = $('footer');
          if (pie) dy = Math.min(dy, Math.max(pie.getBoundingClientRect().top - z.abajo, r.bottom - z.abajo + 12));
        }
        if (Math.abs(dy) > 2) window.scrollTo({ top: Math.max(0, window.scrollY + dy), behavior: 'auto' });
      }
    }
    marcar(el);
  }
  function evento(el, tipo) { el.dispatchEvent(new Event(tipo, { bubbles: true })); }
  function deslizar(el, fin) {
    if (!el) return Promise.resolve();
    var ini = parseFloat(el.value) || 0, pasos = quieto() || saltando ? 1 : 24, k = 0;
    return new Promise(function (r) {
      (function paso() {
        k++;
        el.value = String(Math.round(ini + (fin - ini) * k / pasos));
        evento(el, 'input');
        if (k >= pasos || saltando) { el.value = String(fin); evento(el, 'input'); evento(el, 'change'); r(); return; }
        setTimeout(paso, 40);
      })();
    });
  }
  function tildar(el, si) { if (el && el.checked !== si) { el.checked = si; evento(el, 'change'); } }

  /* ---------- Guion: cada paso resalta una sola cosa ---------- */
  var DETALLE = '/detalle/', COTIZAR = '/vidrio-personalizado/cotizar';
  var PASOS = [
    { id: 'bienvenida', pagina: '/home' },
    { id: 'tienda', pagina: '/catalogo', foco: function () { return $('.filtros'); },
      antes: function () {
        var a = $('.producto-card .producto-body h3 a');
        if (a) estado.detalle = a.getAttribute('href');
        guardar(estado);
      } },
    { id: 'ficha', pagina: DETALLE, foco: function () { return $('.info-producto'); } },
    { id: 'medidas', pagina: COTIZAR, cifras: true, foco: function () { return $('#escena'); },
      demo: function () { return deslizar($('#ancho'), 160).then(function () { return deslizar($('#alto'), 90); }); } },
    { id: 'huella', pagina: COTIZAR, cifras: true, foco: function () { var e = $('#opcionEco'); return e ? e.closest('label') : null; },
      demo: function () {
        var eco = $('#opcionEco');
        tildar(eco, false);
        return esperar(1600).then(function () { tildar(eco, true); });
      } },
    { id: 'editor', pagina: COTIZAR, saltarSi: function () { return !window.SGCorte || !$('[data-sg-corte]'); },
      foco: function () { return $('#sgc-3d'); },
      antes: function () { window.SGCorte.abrir($('[data-sg-corte]'), { forma: 'Hexagonal', ancho: 160, alto: 90 }); },
      alSalir: function () { window.SGCorte.cerrar(); } },
    { id: 'enviar', pagina: COTIZAR, foco: function () { return $('#btnForjar'); } },
    { id: 'ajustes', pagina: COTIZAR, foco: function () { return $('.navbar [data-tour="ajustes"]') || $('.navbar [data-a11y-control]'); } },
    { id: 'final', pagina: '/home', foco: function () { return $('.hero-cta'); }, fin: true }
  ];

  /* ---------- Caja ---------- */
  var estado = leer(), caja = null, temporizador = 0, saltando = false, corriendo = false, esperando = false, alSalir = null, vigia = null;

  function enPagina(p) {
    var ruta = location.pathname;
    if (p.pagina === DETALLE) return ruta.indexOf(DETALLE) === 0;
    return ruta === p.pagina || (p.pagina === '/home' && ruta === '/');
  }
  function urlDe(p) { return p.pagina === DETALLE ? (estado.detalle || null) : p.pagina; }

  function crearCaja() {
    if (caja) return;
    caja = document.createElement('div');
    caja.className = 'sg-tour sg-guia';
    caja.setAttribute('role', 'region');
    caja.setAttribute('aria-label', d('region', 'Recorrido guiado'));
    caja.innerHTML = '<p class="sg-tour-n"></p><div class="sg-guia-barra" aria-hidden="true"><i></i></div>' +
      '<h2 class="sg-tour-titulo" tabindex="-1"></h2><p class="sg-tour-texto" aria-live="polite"></p>' +
      '<dl class="sg-guia-cifras" hidden><div><dt></dt><dd data-c="p"></dd></div><div><dt></dt><dd data-c="co"></dd></div></dl>' +
      '<p class="sg-guia-nota" hidden></p>' +
      '<div class="sg-tour-acciones"><button type="button" class="sg-tour-saltar" data-g="salir"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-g="pausa"></button>' +
      '<button type="button" class="btn btn-primary btn-sm" data-g="sig"></button></div>';
    document.body.appendChild(caja);
    var etiquetas = caja.querySelectorAll('.sg-guia-cifras dt');
    etiquetas[0].textContent = d('precio', 'Precio estimado');
    etiquetas[1].textContent = d('co2', 'CO₂ estimado');
    caja.querySelector('[data-g="salir"]').textContent = d('salir', 'Salir');
    caja.querySelector('[data-g="salir"]').addEventListener('click', function () { salir(); });
    caja.querySelector('[data-g="pausa"]').addEventListener('click', function () {
      estado.pausado = !estado.pausado; guardar(estado); pintarControles(); programar();
    });
    caja.querySelector('[data-g="sig"]').addEventListener('click', function () {
      if (esperando) { esperando = false; ejecutar(); return; }   // vuelve a la pagina del paso pendiente
      siguiente(true);
    });
    foco = document.createElement('div');
    foco.className = 'sg-guia-foco';
    foco.setAttribute('aria-hidden', 'true');
    document.body.appendChild(foco);
    window.addEventListener('scroll', pintarFoco, { capture: true, passive: true });
    window.addEventListener('resize', pintarFoco);
    document.addEventListener('keydown', teclas);
  }
  function teclas(e) { if (e.key === 'Escape' && caja) { e.preventDefault(); salir(); } }
  function pintarControles() {
    var p = caja.querySelector('[data-g="pausa"]'), fin = !!(PASOS[estado.i] && PASOS[estado.i].fin);
    p.textContent = estado.pausado ? d('reanudar', 'Reanudar') : d('pausar', 'Pausar');
    p.setAttribute('aria-pressed', String(!!estado.pausado));
    p.hidden = fin;
    caja.querySelector('[data-g="salir"]').hidden = fin;
    caja.querySelector('[data-g="sig"]').textContent = fin ? d('terminar', 'Terminar') : d('siguiente', 'Siguiente');
  }
  /** Copia en la caja el precio y el CO2 de la pagina: se ven cambiar sin mover la pantalla hasta donde estan. */
  function pintarCifras(si) {
    var c = caja.querySelector('.sg-guia-cifras'), p = $('#priceText'), co = $('#co2Text');
    if (vigia) { vigia.disconnect(); vigia = null; }
    c.hidden = !(si && p && co);
    if (c.hidden) return;
    function copiar() {
      c.querySelector('[data-c="p"]').textContent = p.textContent;
      c.querySelector('[data-c="co"]').textContent = co.textContent;
    }
    copiar();
    vigia = new MutationObserver(copiar);
    [p, co].forEach(function (n) { vigia.observe(n, { childList: true, characterData: true, subtree: true }); });
  }
  function pintar(paso) {
    var tx = TXT[paso.id] || { t: '', p: '' };
    caja.querySelector('.sg-tour-n').textContent = d('paso', 'Paso {0} de {1}').replace('{0}', estado.i + 1).replace('{1}', PASOS.length);
    caja.querySelector('.sg-guia-barra i').style.width = ((estado.i + 1) / PASOS.length * 100) + '%';
    var h = caja.querySelector('.sg-tour-titulo');
    h.textContent = tx.t;
    caja.querySelector('.sg-tour-texto').textContent = tx.p;
    caja.querySelector('.sg-guia-nota').hidden = true;
    pintarCifras(paso.cifras);
    pintarControles();
    if (estado.i === 0) h.focus({ preventScroll: true });
  }

  function programar() {
    clearTimeout(temporizador);
    // Avanza solo tambien con movimiento reducido (Android lo activa con "Quitar animaciones" y, en algunos
    // telefonos, con el ahorro de bateria): lo que se quita es el movimiento, no la automatizacion. Pausar siempre esta.
    // El ultimo paso no avanza: la persona decide cuando cerrarlo.
    if (!caja || corriendo || estado.pausado || (PASOS[estado.i] || {}).fin) return;
    var tx = TXT[(PASOS[estado.i] || {}).id] || { p: '' };
    temporizador = setTimeout(function () { siguiente(false); }, Math.min(12000, Math.max(4200, 1800 + tx.p.length * 55)));
  }

  function soltar() {
    if (!alSalir) return;
    try { alSalir(); } catch (e) { /* ya cerrado */ }
    alSalir = null;
  }
  function aplicar(paso) {
    soltar();
    var el = null;
    try {
      if (paso.antes) paso.antes();
      if (paso.foco) el = paso.foco();
    } catch (e) { /* un elemento que no esta: se sigue */ }
    alSalir = paso.alSalir || null;
    pintar(paso);
    irA(el);
  }
  function ejecutar() {
    var paso = PASOS[estado.i];
    if (!paso) return terminar();
    if (paso.saltarSi && paso.saltarSi()) { estado.i++; guardar(estado); return ejecutar(); }
    if (!enPagina(paso)) {
      var url = urlDe(paso);
      if (!url) { estado.i++; guardar(estado); return ejecutar(); }   // sin producto para el detalle
      guardar(estado);
      location.assign(url);
      return;
    }
    // Al llegar a una pagina ya corre su propia transicion: otra la cortaria de golpe, asi que entra con ella
    var llegando = !caja;
    crearCaja();
    corriendo = true;
    saltando = false;
    if (llegando) aplicar(paso);
    (llegando ? Promise.resolve() : cruzar(function () { aplicar(paso); }))
      .then(function () { return paso.demo ? esperar(700).then(paso.demo) : null; })
      .catch(function () { /* un elemento que no esta: se sigue */ })
      .then(function () { corriendo = false; if (saltando) siguiente(true); else programar(); });
  }
  function siguiente(manual) {
    clearTimeout(temporizador);
    if (corriendo) { saltando = true; return; }    // termina rapido la accion en curso y avanza
    if (PASOS[estado.i] && PASOS[estado.i].fin) return terminar();
    estado.i++;
    guardar(estado);
    if (manual) saltando = false;
    ejecutar();
  }
  function limpiar() {
    clearTimeout(temporizador);
    soltar();
    focoEl = null;
    if (vigia) { vigia.disconnect(); vigia = null; }
    if (caja) { caja.remove(); caja = null; }
    if (foco) { foco.remove(); foco = null; }
    window.removeEventListener('scroll', pintarFoco, { capture: true });
    window.removeEventListener('resize', pintarFoco);
    document.removeEventListener('keydown', teclas);
    restaurar(estado && estado.guardado);
    try { sessionStorage.removeItem(CLAVE); } catch (e) { /* nada */ }
  }
  function salir() { limpiar(); estado = null; }
  function terminar() { salir(); }

  function iniciar() {
    try { localStorage.setItem(VISTO, '1'); } catch (e) { /* nada */ }
    estado = { i: 0, pausado: false, detalle: null, guardado: fotografia() };
    guardar(estado);
    ejecutar();
  }

  /* ---------- Mientras corre, nada se envia ---------- */
  window.addEventListener('submit', function (e) {
    if (!leer()) return;
    e.preventDefault();
    e.stopImmediatePropagation();
    if (caja) { var n = caja.querySelector('.sg-guia-nota'); n.textContent = d('no_envia', 'En el recorrido no se envía nada.'); n.hidden = false; }
  }, true);

  /* ---------- Arranque ---------- */
  function invitar() {
    var c = document.createElement('div');
    c.className = 'sg-tour sg-guia-invita';
    c.setAttribute('role', 'region');
    c.setAttribute('aria-label', d('region', 'Recorrido guiado'));
    c.innerHTML = '<h2 class="sg-tour-titulo"></h2><p class="sg-tour-texto"></p><div class="sg-tour-acciones">' +
      '<button type="button" class="sg-tour-saltar"></button><button type="button" class="btn btn-primary btn-sm"></button></div>';
    c.querySelector('h2').textContent = d('invita_t', '¿Te mostramos SmartGlass?');
    c.querySelector('.sg-tour-texto').textContent = d('invita', 'Un recorrido automático de un minuto. No se guarda nada.');
    var no = c.querySelector('.sg-tour-saltar'), si = c.querySelector('.btn');
    no.textContent = d('ahora_no', 'Ahora no');
    si.textContent = d('ver', 'Ver el recorrido');
    function quitar() { c.remove(); try { localStorage.setItem(VISTO, '1'); } catch (e) { /* nada */ } }
    no.addEventListener('click', quitar);
    si.addEventListener('click', function () { quitar(); iniciar(); });
    document.body.appendChild(c);
  }

  function arrancar() {
    document.querySelectorAll('[data-tour-iniciar]').forEach(function (b) { b.addEventListener('click', iniciar); });
    if (/[?&]tour=1(&|$)/.test(location.search)) { iniciar(); return; }
    if (estado) {
      var paso = PASOS[estado.i];
      if (paso && enPagina(paso)) { ejecutar(); return; }
      // La persona se fue a otra pagina: el recorrido espera aqui
      crearCaja();
      pintar(paso || PASOS[0]);
      var n = caja.querySelector('.sg-guia-nota');
      n.textContent = d('pausada', 'El recorrido está en pausa.'); n.hidden = false;
      var sig = caja.querySelector('[data-g="sig"]');
      sig.textContent = d('continuar', 'Continuar');
      esperando = true;
      caja.querySelector('[data-g="pausa"]').hidden = true;
      return;
    }
    var visto;
    try { visto = localStorage.getItem(VISTO); } catch (e) { visto = '1'; }
    if (!visto && document.querySelector('.navbar[data-nav="public"]')) invitar();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', arrancar); else arrancar();
})();
