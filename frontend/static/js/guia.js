/* U6 · Guia automatizada (textos: fragments/tour.html; estilos: theme.css "U6 · Guia automatizada").
   Recorre inicio -> catalogo -> detalle -> configurador (y su editor de corte) -> planes y termina en
   /login?guia=fin. Prueba opciones de verdad pero NO guarda ni envia nada:
   - bloquea todo envio de formulario mientras corre (captura en window, antes que main.js);
   - no pulsa "agregar al carrito", "cotizar" ni los interruptores de accesibilidad (esos se guardan);
   - al terminar o salir restaura el borrador del editor (localStorage sg-borrador*) y la seleccion
     del comparador (sessionStorage sg-comparar) tal como estaban al empezar.
   Estado entre paginas en sessionStorage "sg-guia" ({ i, pausado, detalle, guardado }).
   Accesible: region con aria-live, pausa (WCAG 2.2.2), siguiente y salir; Esc sale. Con movimiento
   reducido o fotosensible sigue avanzando sola, pero sin cursor animado ni desplazamientos suaves. */
(function () {
  'use strict';

  var datos = document.getElementById('sg-tour-datos');
  if (!datos || !datos.content) return;
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
    var f = { local: {}, sesion: null };
    try {
      for (var k = 0; k < localStorage.length; k++) {
        var n = localStorage.key(k);
        if (n && n.indexOf('sg-borrador') === 0) f.local[n] = localStorage.getItem(n);
      }
      f.sesion = sessionStorage.getItem('sg-comparar');
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
      if (f.sesion == null) sessionStorage.removeItem('sg-comparar'); else sessionStorage.setItem('sg-comparar', f.sesion);
    } catch (e) { /* sin almacenamiento */ }
  }

  /* ---------- Cursor y resaltado ---------- */
  var cursor = null, resaltado = null;
  function marcar(el) {
    if (resaltado) resaltado.classList.remove('sg-tour-objetivo');
    resaltado = el || null;
    if (resaltado) resaltado.classList.add('sg-tour-objetivo');
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
  function irA(el) {
    if (!el) return Promise.resolve();
    var comportamiento = quieto() || saltando ? 'auto' : 'smooth';
    if (el.closest('.sgc-modal')) {
      el.scrollIntoView({ block: 'nearest', behavior: comportamiento });
    } else {
      // Se centra en lo que queda libre entre la barra y la caja del recorrido. Dentro de un panel con
      // desplazamiento propio se mueve el panel y la ventana solo lo justo para verlo entero: asi no se
      // baja hasta el pie de pagina buscando algo que esta dentro del panel.
      var z = zonaLibre(el), r = el.getBoundingClientRect(), panel = panelDe(el), dy;
      if (panel) {
        // si cabe, el panel termina donde termina la pantalla (como lo pinta el configurador); si no, empieza bajo la barra
        var pr = panel.getBoundingClientRect(), cabe = pr.height <= window.innerHeight - 12 - z.arriba;
        dy = cabe ? pr.bottom - (window.innerHeight - 12) : pr.top - z.arriba;
        var arriba = Math.max(z.arriba, pr.top - dy), abajo = Math.min(z.abajo, pr.bottom - dy);
        var dentro = centrar(r.top - dy, r.height, { arriba: arriba, abajo: Math.max(arriba + 80, abajo) });
        panel.scrollTo({ top: Math.max(0, panel.scrollTop + dentro), behavior: comportamiento });
      } else {
        dy = centrar(r.top, r.height, z);
        // el pie de pagina no entra en la franja libre si el elemento cabe un poco mas abajo
        var pie = $('footer');
        if (pie) dy = Math.min(dy, Math.max(pie.getBoundingClientRect().top - z.abajo, r.bottom - z.abajo + 12));
      }
      if (Math.abs(dy) > 2) window.scrollTo({ top: Math.max(0, window.scrollY + dy), behavior: comportamiento });
    }
    marcar(el);
    return esperar(520).then(function () {
      if (quieto() || !cursor) return;
      var r = el.getBoundingClientRect();
      cursor.hidden = false;
      cursor.style.transform = 'translate(' + Math.round(r.left + Math.min(r.width / 2, 40)) + 'px,' + Math.round(r.top + Math.min(r.height / 2, 24)) + 'px)';
      return esperar(560);
    });
  }
  function pulsar() {
    if (!cursor || quieto()) return Promise.resolve();
    cursor.classList.remove('is-clic'); void cursor.offsetWidth; cursor.classList.add('is-clic');
    return esperar(260);
  }
  function evento(el, tipo) { el.dispatchEvent(new Event(tipo, { bubbles: true })); }
  function escribir(el, texto) {
    if (!el) return Promise.resolve();
    el.focus({ preventScroll: true });
    if (quieto() || saltando) { el.value = texto; evento(el, 'input'); return Promise.resolve(); }
    el.value = '';
    var i = 0;
    return new Promise(function (r) {
      (function paso() {
        if (saltando || i >= texto.length) { el.value = texto; evento(el, 'input'); r(); return; }
        el.value += texto[i++]; evento(el, 'input'); setTimeout(paso, 85);
      })();
    });
  }
  function deslizar(el, fin) {
    if (!el) return Promise.resolve();
    var ini = parseFloat(el.value) || 0, pasos = quieto() || saltando ? 1 : 14, k = 0;
    return new Promise(function (r) {
      (function paso() {
        k++;
        el.value = String(Math.round(ini + (fin - ini) * k / pasos));
        evento(el, 'input');
        if (k >= pasos || saltando) { el.value = String(fin); evento(el, 'input'); evento(el, 'change'); r(); return; }
        setTimeout(paso, 45);
      })();
    });
  }
  function elegir(el, valor) {
    if (!el || !Array.prototype.some.call(el.options || [], function (o) { return o.value === valor; })) return false;
    el.value = valor; evento(el, 'input'); evento(el, 'change');
    return true;
  }
  function tildar(el, si) { if (el && el.checked !== si) { el.checked = si; evento(el, 'change'); } }

  /* ---------- Guion ---------- */
  var DETALLE = '/detalle/';
  var PASOS = [
    { id: 'bienvenida', pagina: '/home' },
    { id: 'inicio', pagina: '/home', accion: function () { return irA($('.hero-cta')); } },
    { id: 'buscar', pagina: '/catalogo', accion: function () {
        var q = $('#q');
        return irA(q).then(function () { return escribir(q, d('busqueda', 'templado')); })
          .then(function () { return esperar(700); })
          .then(function () { q.value = ''; evento(q, 'input'); return irA($('.filtros')); });
      } },
    { id: 'comparar', pagina: '/catalogo', accion: function () {
        var c = Array.prototype.slice.call(document.querySelectorAll('.chk-comparar')).slice(0, 2);
        if (!c.length) return Promise.resolve();
        return irA(c[0].closest('.producto-card')).then(pulsar).then(function () { tildar(c[0], true); return esperar(500); })
          .then(function () { return c[1] ? irA(c[1].closest('.producto-card')) : null; })
          .then(pulsar).then(function () { if (c[1]) tildar(c[1], true); return irA($('#barra-comparador')); })
          .then(function () { return esperar(1400); })
          .then(function () { c.forEach(function (x) { tildar(x, false); }); });
      } },
    { id: 'ficha', pagina: '/catalogo', accion: function () {
        var a = $('.producto-card .producto-body h3 a');
        if (a) estado.detalle = a.getAttribute('href');
        guardar(estado);
        return irA(a ? a.closest('.producto-card') : null).then(pulsar);
      } },
    { id: 'detalle', pagina: DETALLE, accion: function () {
        return irA($('.precio')).then(function () { return irA($('.stock-linea') || $('.info-producto')); })
          .then(function () { return irA($('form.compra')); });
      } },
    { id: 'medidas', pagina: '/vidrio-personalizado/cotizar', accion: function () {
        return irA($('#escena')).then(function () { return deslizar($('#ancho'), 160); })
          .then(function () { return deslizar($('#alto'), 90); });
      } },
    { id: 'tipo', pagina: '/vidrio-personalizado/cotizar', accion: function () {
        var t = $('#tipoVidrio');
        return irA(t).then(pulsar).then(function () { elegir(t, 'Laminado'); return esperar(500); })
          .then(function () { return irA($('#espesor')); }).then(function () { return deslizar($('#espesor'), 8); })
          .then(function () { return irA($('#escena')); });
      } },
    { id: 'forma', pagina: '/vidrio-personalizado/cotizar', saltarSi: function () { return !Array.prototype.some.call(($('#forma') || {}).options || [], function (o) { return o.value === 'Arco'; }); },
      accion: function () {
        var f = $('#forma');
        return irA(f).then(pulsar).then(function () { elegir(f, 'Arco'); return irA($('#escena')); });
      } },
    { id: 'huella', pagina: '/vidrio-personalizado/cotizar', accion: function () {
        var eco = $('#opcionEco'), et = $('#co2Etapas');
        return irA(eco).then(pulsar).then(function () { tildar(eco, false); return esperar(900); })
          .then(pulsar).then(function () { tildar(eco, true); if (et) et.open = true; return irA(et || $('#termometro')); })
          .then(function () { return irA($('#termometro')); });
      } },
    { id: 'editor', pagina: '/vidrio-personalizado/cotizar', saltarSi: function () { return !window.SGCorte || !$('[data-sg-corte]'); },
      accion: function () {
        var btn = $('[data-sg-corte]');
        try { window.SGCorte.abrir(btn); } catch (e) { return Promise.resolve(); }
        var forma = $('.sgc-forma-btn[data-forma="Hexagonal"]') || $('.sgc-forma-btn');
        return esperar(700).then(function () { return irA(forma); }).then(pulsar)
          .then(function () { if (forma) forma.click(); return esperar(800); })
          .then(function () { return irA($('#sgc-rotar')); }).then(pulsar).then(function () { var b = $('#sgc-rotar'); if (b) b.click(); return esperar(700); })
          .then(function () { return irA($('#sgc-espejo-h')); }).then(pulsar).then(function () { var b = $('#sgc-espejo-h'); if (b) b.click(); return esperar(900); })
          .then(function () { try { window.SGCorte.cerrar(); } catch (e) { /* ya cerrado */ } marcar(null); });
      } },
    { id: 'enviar', pagina: '/vidrio-personalizado/cotizar', accion: function () { return irA($('#btnForjar')); } },
    { id: 'planes', pagina: '/planes', accion: function () { return irA($('.pv')); } },
    { id: 'ajustes', pagina: '/planes', accion: function () {
        return irA($('.navbar [data-tour="ajustes"]') || $('.navbar [data-a11y-control]'))
          .then(function () { return irA($('.navbar [data-a11y-control]') || $('.sg-theme-switch')); });
      } },
    { id: 'final', pagina: '/planes', fin: true }
  ];

  /* ---------- Caja ---------- */
  var estado = leer(), caja = null, temporizador = 0, saltando = false, corriendo = false, esperando = false;

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
      '<p class="sg-guia-nota" hidden></p>' +
      '<div class="sg-tour-acciones"><button type="button" class="sg-tour-saltar" data-g="salir"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-g="pausa"></button>' +
      '<button type="button" class="btn btn-primary btn-sm" data-g="sig"></button></div>';
    document.body.appendChild(caja);
    caja.querySelector('[data-g="salir"]').textContent = d('salir', 'Salir');
    caja.querySelector('[data-g="salir"]').addEventListener('click', function () { salir(); });
    caja.querySelector('[data-g="pausa"]').addEventListener('click', function () {
      estado.pausado = !estado.pausado; guardar(estado); pintarControles(); programar();
    });
    caja.querySelector('[data-g="sig"]').addEventListener('click', function () {
      if (esperando) { esperando = false; ejecutar(); return; }   // vuelve a la pagina del paso pendiente
      siguiente(true);
    });
    cursor = document.createElement('div');
    cursor.className = 'sg-guia-cursor';
    cursor.setAttribute('aria-hidden', 'true');
    cursor.hidden = true;
    document.body.appendChild(cursor);
    document.addEventListener('keydown', teclas);
  }
  function teclas(e) { if (e.key === 'Escape' && caja) { e.preventDefault(); salir(); } }
  function pintarControles() {
    var p = caja.querySelector('[data-g="pausa"]');
    p.textContent = estado.pausado ? d('reanudar', 'Reanudar') : d('pausar', 'Pausar');
    p.setAttribute('aria-pressed', String(!!estado.pausado));
    caja.querySelector('[data-g="sig"]').textContent = PASOS[estado.i] && PASOS[estado.i].fin ? d('terminar', 'Terminar') : d('siguiente', 'Siguiente');
  }
  function pintar(paso) {
    var tx = TXT[paso.id] || { t: '', p: '' };
    caja.querySelector('.sg-tour-n').textContent = d('paso', 'Paso {0} de {1}').replace('{0}', estado.i + 1).replace('{1}', PASOS.length);
    caja.querySelector('.sg-guia-barra i').style.width = ((estado.i + 1) / PASOS.length * 100) + '%';
    var h = caja.querySelector('.sg-tour-titulo');
    h.textContent = tx.t;
    caja.querySelector('.sg-tour-texto').textContent = tx.p;
    caja.querySelector('.sg-guia-nota').hidden = true;
    pintarControles();
    if (estado.i === 0) h.focus();
  }

  function programar() {
    clearTimeout(temporizador);
    // Avanza solo tambien con movimiento reducido (Android lo activa con "Quitar animaciones" y, en algunos
    // telefonos, con el ahorro de bateria): lo que se quita es el movimiento, no la automatizacion. Pausar siempre esta.
    if (!caja || corriendo || estado.pausado) return;
    var tx = TXT[(PASOS[estado.i] || {}).id] || { p: '' };
    temporizador = setTimeout(function () { siguiente(false); }, Math.min(9000, 2600 + tx.p.length * 30));
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
    crearCaja();
    pintar(paso);
    corriendo = true;
    saltando = false;
    Promise.resolve(paso.accion ? paso.accion() : null)
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
    marcar(null);
    if (caja) { caja.remove(); caja = null; }
    if (cursor) { cursor.remove(); cursor = null; }
    document.removeEventListener('keydown', teclas);
    restaurar(estado && estado.guardado);
    try { sessionStorage.removeItem(CLAVE); } catch (e) { /* nada */ }
  }
  function salir() { limpiar(); estado = null; }
  function terminar() { limpiar(); estado = null; location.assign('/login?guia=fin'); }

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
    c.querySelector('.sg-tour-texto').textContent = d('invita', 'Un recorrido automático de dos minutos. No se guarda nada.');
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
