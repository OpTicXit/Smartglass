/* U6 · Guia automatizada (textos: fragments/tour.html; estilos: theme.css "U6 · Guia automatizada").
   Recorre inicio -> catalogo -> detalle -> configurador (y su editor de corte) y deja a la persona en /login?guia=fin.
   Si a los dos minutos no ha terminado, ofrece seguir, repetirlo a su ritmo (sin avance automatico), ir al inicio
   de sesion o salir.
   Un recuadro iluminado viaja suavemente hasta cada cosa mientras la pagina se desplaza con el, y el
   mensaje aparece a su lado con una flecha que la senala: nunca encima de lo que se explica. Se pide lo
   minimo a la pagina: si lo que se explica y su mensaje ya caben, no se desplaza.
   Prueba opciones de verdad pero NO guarda ni envia nada:
   - bloquea todo envio de formulario mientras corre (captura en window, antes que main.js);
   - no pulsa "agregar al carrito", "cotizar" ni los interruptores de accesibilidad (esos se guardan);
   - al terminar o salir restaura el borrador del editor (localStorage sg-borrador*) tal como estaba.
   Solo pasa por paginas publicas: nada del panel de administracion.
   Estado entre paginas en sessionStorage "sg-guia" ({ i, pausado, detalle, guardado, inicio, modo, avisado }); a11y-init.js lo
   lee antes de pintar para atenuar la pagina desde el primer cuadro.
   Accesible: region con aria-live, pausa (WCAG 2.2.2), siguiente y salir; Esc sale. Con movimiento
   reducido o fotosensible sigue avanzando sola, pero los cambios son inmediatos. */
(function () {
  'use strict';

  var datos = document.getElementById('sg-tour-datos');
  // En una pagina de error (templates/error.html) la invitacion taparia los botones para salir de ella
  if (!datos || !datos.content || document.querySelector('.pagina-error')) return;
  var CLAVE = 'sg-guia', VISTO = 'sg-tour-visto';
  var LIMITE_MS = 120000, MARGEN = 10, SEPARACION = 24, RELLENO = 8;   // separacion: del borde del objetivo al mensaje, con la flecha en medio
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
  function acotar(v, min, max) { return Math.max(min, Math.min(max, v)); }
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

  /* ---------- Objetivo: uno o varios elementos, tratados como un solo rectangulo ---------- */
  var foco = null, objetivos = [], flecha = null, moviendo = false, ultimo = null, cuadro = 0;

  function visibles(lista) {
    return (lista || []).filter(function (el) {
      if (!el) return false;
      var r = el.getBoundingClientRect();
      return r.width > 0 && r.height > 0;
    });
  }
  function rectObjetivo() {
    var v = visibles(objetivos);
    if (!v.length) return null;
    var a = Infinity, i = Infinity, b = -Infinity, de = -Infinity;
    v.forEach(function (el) {
      var r = el.getBoundingClientRect();
      a = Math.min(a, r.top); i = Math.min(i, r.left); b = Math.max(b, r.bottom); de = Math.max(de, r.right);
    });
    return { top: a, left: i, bottom: b, right: de, width: de - i, height: b - a };
  }
  function enModal() { var v = visibles(objetivos)[0]; return !!(v && v.closest('.sgc-modal')); }
  function centroPantalla() {
    var x = window.innerWidth / 2, y = window.innerHeight / 2;
    return { top: y, left: x, bottom: y, right: x, width: 0, height: 0 };
  }
  /** Lo que se puede usar de la pantalla: bajo la barra fija, salvo dentro del editor, que la tapa. */
  function zona() {
    var nav = $('.navbar'), arriba = 0;
    if (!enModal() && nav && /fixed|sticky/.test(getComputedStyle(nav).position)) arriba = Math.max(0, nav.getBoundingClientRect().bottom);
    return { t: arriba + MARGEN, b: window.innerHeight - MARGEN, l: MARGEN, r: window.innerWidth - MARGEN };
  }

  function pintarFoco(r) {
    if (!foco) return;
    foco.classList.toggle('sin-objetivo', !r.width);
    var pad = r.width ? RELLENO : 0;
    foco.style.left = (r.left - pad) + 'px';
    foco.style.top = (r.top - pad) + 'px';
    foco.style.width = (r.width + pad * 2) + 'px';
    foco.style.height = (r.height + pad * 2) + 'px';
  }

  /** Donde va el mensaje para no pisar el objetivo. Null si no cabe en ningun lado. */
  function colocacion(r, W, H) {
    var z = zona();
    if (!r.width) return { lado: '', x: (window.innerWidth - W) / 2, y: (window.innerHeight - H) / 2 };
    var cx = acotar(r.left + r.width / 2 - W / 2, z.l, Math.max(z.l, z.r - W));
    var cy = acotar(r.top + r.height / 2 - H / 2, z.t, Math.max(z.t, z.b - H));
    if (r.bottom + SEPARACION + H <= z.b) return { lado: 'abajo', x: cx, y: r.bottom + SEPARACION };
    if (r.top - SEPARACION - H >= z.t) return { lado: 'arriba', x: cx, y: r.top - SEPARACION - H };
    if (r.right + SEPARACION + W <= z.r) return { lado: 'derecha', x: r.right + SEPARACION, y: cy };
    if (r.left - SEPARACION - W >= z.l) return { lado: 'izquierda', x: r.left - SEPARACION - W, y: cy };
    return null;
  }
  function colocarCaja(r) {
    var W = caja.offsetWidth, H = caja.offsetHeight;
    var c = colocacion(r, W, H);
    // Solo pasa si el objetivo y el mensaje no caben juntos en la pantalla: el mensaje va al lado con mas espacio
    if (!c) {
      var z = zona(), abajo = z.b - r.bottom >= r.top - z.t;
      c = { lado: abajo ? 'abajo' : 'arriba', x: acotar(r.left + r.width / 2 - W / 2, z.l, Math.max(z.l, z.r - W)), y: abajo ? Math.max(z.t, z.b - H) : z.t };
    }
    caja.style.left = Math.round(c.x) + 'px';
    caja.style.top = Math.round(c.y) + 'px';
    caja.setAttribute('data-lado', c.lado);
    flecha.hidden = !c.lado;
    if (c.lado === 'abajo' || c.lado === 'arriba') {
      flecha.style.left = acotar(r.left + r.width / 2 - c.x - 7, 16, W - 30) + 'px';
      flecha.style.top = '';
    } else if (c.lado) {
      flecha.style.top = acotar(r.top + r.height / 2 - c.y - 7, 16, H - 30) + 'px';
      flecha.style.left = '';
    }
  }

  /** Panel con desplazamiento propio que contiene el elemento (en escritorio el configurador tiene dos). */
  function panelDe(el) {
    for (var p = el.parentElement; p && p !== document.body; p = p.parentElement) {
      if (/(auto|scroll)/.test(getComputedStyle(p).overflowY) && p.scrollHeight > p.clientHeight + 1) return p;
    }
    return null;
  }
  /** Lo minimo que hay que desplazar para que el objetivo y su mensaje quepan sin pisarse. */
  function planDesplazamiento() {
    var v = visibles(objetivos), r = rectObjetivo(), plan = { ventana: 0, panel: null, enPanel: 0 };
    if (!r || enModal()) return plan;
    var W = caja.offsetWidth, H = caja.offsetHeight, z = zona(), panel = panelDe(v[0]);
    var pr = panel ? panel.getBoundingClientRect() : null;
    var dentroPanel = !pr || (r.top >= pr.top && r.bottom <= pr.bottom);
    if (r.top >= z.t && r.bottom <= z.b && dentroPanel && colocacion(r, W, H)) return plan;

    if (panel) {
      // Se mueve el panel y la ventana solo lo justo para verlo entero: asi no se baja hasta el pie de pagina
      var cabe = pr.height <= window.innerHeight - MARGEN - z.t;
      plan.ventana = cabe ? pr.bottom - (window.innerHeight - MARGEN) : pr.top - z.t;
      var ventanaTop = Math.max(z.t, pr.top - plan.ventana), ventanaFin = Math.min(z.b, pr.bottom - plan.ventana);
      var top = r.top - plan.ventana;
      plan.panel = panel;
      plan.enPanel = r.height > ventanaFin - ventanaTop ? top - ventanaTop : top + r.height / 2 - (ventanaTop + ventanaFin) / 2;
      plan.enPanel = acotar(plan.enPanel, -panel.scrollTop, panel.scrollHeight - panel.clientHeight - panel.scrollTop);
    } else {
      var aUnLado = r.left - SEPARACION - W >= z.l || r.right + SEPARACION + W <= z.r;
      var alto = aUnLado ? r.height : r.height + SEPARACION + H;
      if (alto > z.b - z.t) plan.ventana = r.top - z.t;
      else if (r.top < z.t) plan.ventana = r.top - z.t;
      else if (r.top + alto > z.b - 4) plan.ventana = r.top + alto - z.b + 4;
    }
    var maxV = document.documentElement.scrollHeight - window.innerHeight;
    plan.ventana = acotar(plan.ventana, -window.scrollY, Math.max(-window.scrollY, maxV - window.scrollY));
    return plan;
  }
  function suavizar(t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
  /** Lleva el recuadro y la pagina juntos, con la misma curva. Con movimiento reducido llega de una vez. */
  function viajar(plan, desde) {
    var y0 = window.scrollY, p0 = plan.panel ? plan.panel.scrollTop : 0;
    function aplicar(e) {
      if (plan.ventana) window.scrollTo({ top: y0 + plan.ventana * e, behavior: 'instant' });
      if (plan.panel && plan.enPanel) plan.panel.scrollTo({ top: p0 + plan.enPanel * e, behavior: 'instant' });
    }
    if (quieto() || saltando || !desde) { aplicar(1); pintarFoco(rectObjetivo() || centroPantalla()); return Promise.resolve(); }
    // Va en linea recta hasta donde quedara el objetivo: seguirlo mientras se desplaza lo haria pasarse y volver
    var r0 = rectObjetivo() || centroPantalla(), corrimiento = plan.ventana + plan.enPanel;
    var fin = { top: r0.top - corrimiento, left: r0.left, width: r0.width, height: r0.height };
    var distancia = Math.abs(corrimiento) + Math.abs(fin.top - desde.top) + Math.abs(fin.left - desde.left);
    var dura = acotar(300 + distancia * 0.3, 340, 680);
    moviendo = true;
    return new Promise(function (hecho) {
      var ini = 0;
      function paso(ahora) {
        if (!ini) ini = ahora;
        var t = saltando ? 1 : Math.min(1, (ahora - ini) / dura), e = suavizar(t);
        aplicar(e);
        pintarFoco({
          top: desde.top + (fin.top - desde.top) * e, left: desde.left + (fin.left - desde.left) * e,
          width: desde.width + (fin.width - desde.width) * e, height: desde.height + (fin.height - desde.height) * e
        });
        if (t < 1) { requestAnimationFrame(paso); return; }
        moviendo = false;
        hecho();
      }
      requestAnimationFrame(paso);
    });
  }
  /** Mantiene recuadro y mensaje sobre su objetivo si la pagina cambia sola (imagenes que cargan, teclado del movil). */
  function seguir() {
    cuadro = requestAnimationFrame(seguir);
    if (!caja || moviendo || caja.classList.contains('is-oculta')) return;
    var r = rectObjetivo() || centroPantalla();
    if (ultimo && Math.abs(r.top - ultimo.top) < 1 && Math.abs(r.left - ultimo.left) < 1 && Math.abs(r.height - ultimo.height) < 1 && Math.abs(r.width - ultimo.width) < 1) return;
    ultimo = r;
    pintarFoco(r);
    colocarCaja(r);
  }

  /** Funde abrir o cerrar el editor, que de otro modo aparece de golpe. */
  function cruzar(cambio) {
    if (quieto() || saltando || !document.startViewTransition) { cambio(); return Promise.resolve(); }
    var raiz = document.documentElement;
    raiz.classList.add('sg-guia-vt');
    var vt = document.startViewTransition(cambio);
    function nada() { /* pestana oculta: el cambio ya se aplico */ }
    vt.ready.catch(nada);
    return vt.finished.catch(nada).then(function () { raiz.classList.remove('sg-guia-vt'); });
  }

  function evento(el, tipo) { el.dispatchEvent(new Event(tipo, { bubbles: true })); }
  function deslizar(el, fin) {
    if (!el) return Promise.resolve();
    var ini = parseFloat(el.value) || 0, pasos = quieto() || saltando ? 1 : 14, k = 0;
    return new Promise(function (r) {
      (function paso() {
        k++;
        el.value = String(Math.round(ini + (fin - ini) * k / pasos));
        evento(el, 'input');
        if (k >= pasos || saltando) { el.value = String(fin); evento(el, 'input'); evento(el, 'change'); r(); return; }
        setTimeout(paso, 30);
      })();
    });
  }
  function tildar(el, si) { if (el && el.checked !== si) { el.checked = si; evento(el, 'change'); } }
  function mostrarPaso(el) { if (el && window.SGAsistente) window.SGAsistente.mostrar(el); }

  /* ---------- Guion: cada paso senala una sola cosa ---------- */
  var DETALLE = '/detalle/', COTIZAR = '/vidrio-personalizado/cotizar';
  var PASOS = [
    { id: 'bienvenida', pagina: '/home' },
    { id: 'tienda', pagina: '/catalogo', foco: function () { return [$('.filtros')]; },
      antes: function () {
        var a = $('.producto-card .producto-body h3 a');
        if (a) estado.detalle = a.getAttribute('href');
        guardar(estado);
      } },
    { id: 'comparar', pagina: '/catalogo', foco: function () { var c = $('.chk-comparar'); return [c ? c.closest('label') || c : null]; } },
    { id: 'ficha', pagina: DETALLE, foco: function () { return [$('.info-producto a[href="#resenas"]'), $('.precio'), $('.stock-linea')]; } },
    // El configurador es un asistente por pasos: SGAsistente.mostrar abre el paso donde vive cada elemento
    { id: 'medidas', pagina: COTIZAR, cifras: true, foco: function () { mostrarPaso($('#ancho')); return [$('#escena')]; },
      demo: function () { return deslizar($('#ancho'), 160).then(function () { return deslizar($('#alto'), 90); }); } },
    { id: 'huella', pagina: COTIZAR, cifras: true, foco: function () { var e = $('#opcionEco'); mostrarPaso(e); return [e ? e.closest('label') : null]; },
      demo: function () {
        var eco = $('#opcionEco');
        tildar(eco, false);
        return esperar(1100).then(function () { tildar(eco, true); });
      } },
    { id: 'editor', pagina: COTIZAR, saltarSi: function () { return !window.SGCorte || !$('[data-sg-corte]'); },
      foco: function () { return [$('#sgc-3d')]; },
      antes: function () { window.SGCorte.abrir($('[data-sg-corte]'), { forma: 'Hexagonal', ancho: 160, alto: 90 }); },
      alSalir: function () { window.SGCorte.cerrar(); } },
    { id: 'enviar', pagina: COTIZAR, foco: function () { mostrarPaso($('#btnForjar')); return [$('#btnForjar')]; } },
    { id: 'ajustes', pagina: COTIZAR, foco: function () {
        // En el celular estos controles viven dentro del menu: se senala el boton que lo abre
        var a = [$('.navbar [data-tour="ajustes"]'), $('.navbar [data-a11y-control]')];
        return visibles(a).length ? a : [$('.nav-toggle')];
      } },
    { id: 'final', pagina: COTIZAR, fin: true }
  ];

  /* ---------- Caja ---------- */
  var estado = leer(), caja = null, temporizador = 0, saltando = false, corriendo = false, esperando = false, alSalir = null, vigia = null, reloj = 0;

  function enPagina(p) {
    var ruta = location.pathname;
    if (p.pagina === DETALLE) return ruta.indexOf(DETALLE) === 0;
    return ruta === p.pagina || (p.pagina === '/home' && ruta === '/');
  }
  function urlDe(p) { return p.pagina === DETALLE ? (estado.detalle || null) : p.pagina; }

  function crearCaja() {
    if (caja) return;
    document.documentElement.classList.add('sg-guia-activa', 'sg-guia-lista');
    // Al recargar, el navegador no debe llevarse la pagina a donde estaba: la guia decide donde se ve
    if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
    foco = document.createElement('div');
    foco.className = 'sg-guia-foco sin-objetivo';
    foco.setAttribute('aria-hidden', 'true');
    document.body.appendChild(foco);
    caja = document.createElement('div');
    caja.className = 'sg-tour sg-guia is-oculta';
    caja.setAttribute('role', 'region');
    caja.setAttribute('aria-label', d('region', 'Recorrido guiado'));
    caja.innerHTML = '<i class="sg-guia-flecha" aria-hidden="true" hidden></i>' +
      '<p class="sg-tour-n"></p><div class="sg-guia-barra" aria-hidden="true">' + PASOS.map(function () { return '<i></i>'; }).join('') + '</div>' +
      '<h2 class="sg-tour-titulo" tabindex="-1"></h2><p class="sg-tour-texto" aria-live="polite"></p>' +
      '<dl class="sg-guia-cifras" hidden><div><dt></dt><dd data-c="p"></dd></div><div><dt></dt><dd data-c="co"></dd></div></dl>' +
      '<p class="sg-guia-nota" hidden></p>' +
      '<div class="sg-guia-opciones" hidden>' +
      '<button type="button" class="btn btn-primary btn-sm" data-o="seguir"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-o="ritmo"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-o="login"></button>' +
      '<button type="button" class="sg-tour-saltar" data-o="salir"></button></div>' +
      '<div class="sg-tour-acciones"><button type="button" class="sg-tour-saltar" data-g="salir"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-g="atras"></button>' +
      '<button type="button" class="btn btn-secondary btn-sm" data-g="pausa"></button>' +
      '<button type="button" class="btn btn-primary btn-sm" data-g="sig"></button></div>';
    document.body.appendChild(caja);
    flecha = caja.querySelector('.sg-guia-flecha');
    var etiquetas = caja.querySelectorAll('.sg-guia-cifras dt');
    etiquetas[0].textContent = d('precio', 'Precio estimado');
    etiquetas[1].textContent = d('co2', 'CO₂ estimado');
    caja.querySelector('[data-g="salir"]').textContent = d('salir', 'Salir');
    caja.querySelector('[data-g="salir"]').addEventListener('click', function () { salir(); });
    caja.querySelector('[data-g="pausa"]').addEventListener('click', function () {
      estado.pausado = !estado.pausado; guardar(estado); pintarControles(); programar();
    });
    caja.querySelector('[data-g="atras"]').textContent = d('atras', 'Atrás');
    caja.querySelector('[data-g="atras"]').addEventListener('click', anterior);
    caja.querySelector('[data-o="seguir"]').textContent = d('op_seguir', 'Seguir donde iba');
    caja.querySelector('[data-o="ritmo"]').textContent = d('op_ritmo', 'Repetirlo a mi ritmo');
    caja.querySelector('[data-o="login"]').textContent = d('op_login', 'Ir al inicio de sesión');
    caja.querySelector('[data-o="salir"]').textContent = d('op_salir', 'Salir de la guía');
    caja.querySelector('[data-o="seguir"]').addEventListener('click', function () {
      estado.pausado = false; quitarOpciones(); ejecutar();
    });
    caja.querySelector('[data-o="ritmo"]').addEventListener('click', function () {
      estado.i = 0; estado.modo = 'ritmo'; estado.pausado = false; quitarOpciones(); ejecutar();
    });
    caja.querySelector('[data-o="login"]').addEventListener('click', function () { terminar(); });
    caja.querySelector('[data-o="salir"]').addEventListener('click', function () { salir(); });
    caja.querySelector('[data-g="sig"]').addEventListener('click', function () {
      if (esperando) { esperando = false; ejecutar(); return; }   // vuelve a la pagina del paso pendiente
      siguiente(true);
    });
    document.addEventListener('keydown', teclas);
    cuadro = requestAnimationFrame(seguir);
    vigilarLimite();
  }
  /* ---------- Dos minutos sin terminar: se ofrecen opciones ---------- */
  function vigilarLimite() {
    clearTimeout(reloj);
    if (!estado || estado.avisado || estado.modo === 'ritmo') return;
    reloj = setTimeout(mostrarOpciones, Math.max(0, (estado.inicio || Date.now()) + LIMITE_MS - Date.now()));
  }
  function mostrarOpciones() {
    if (!caja) return;
    if (corriendo) { reloj = setTimeout(mostrarOpciones, 300); return; }
    clearTimeout(temporizador);
    estado.avisado = true;
    estado.pausado = true;
    esperando = false;
    guardar(estado);
    soltar();
    objetivos = [];
    caja.querySelector('.sg-tour-titulo').textContent = d('opciones_t', '¿Cómo seguimos?');
    caja.querySelector('.sg-tour-texto').textContent = d('opciones', 'Llevas dos minutos en el recorrido.');
    caja.querySelector('.sg-guia-cifras').hidden = true;
    caja.querySelector('.sg-guia-nota').hidden = true;
    caja.querySelector('.sg-guia-opciones').hidden = false;
    caja.classList.add('con-opciones');
    caja.classList.remove('is-oculta');
    var r = centroPantalla();
    ultimo = r;
    pintarFoco(r);
    colocarCaja(r);
    caja.querySelector('[data-o="seguir"]').focus({ preventScroll: true });
  }
  function quitarOpciones() {
    caja.querySelector('.sg-guia-opciones').hidden = true;
    caja.classList.remove('con-opciones');
    guardar(estado);
  }
  function anterior() {
    if (corriendo || estado.i <= 0) return;
    clearTimeout(temporizador);
    estado.i--;
    while (estado.i > 0 && PASOS[estado.i].saltarSi && PASOS[estado.i].saltarSi()) estado.i--;
    guardar(estado);
    ejecutar();
  }
  function teclas(e) { if (e.key === 'Escape' && caja) { e.preventDefault(); salir(); } }
  function pintarControles() {
    var p = caja.querySelector('[data-g="pausa"]'), fin = !!(PASOS[estado.i] && PASOS[estado.i].fin);
    p.textContent = estado.pausado ? d('reanudar', 'Reanudar') : d('pausar', 'Pausar');
    p.setAttribute('aria-pressed', String(!!estado.pausado));
    var aRitmo = estado.modo === 'ritmo';
    p.hidden = aRitmo;
    caja.querySelector('[data-g="atras"]').hidden = !aRitmo || estado.i === 0;
    caja.querySelector('[data-g="sig"]').textContent = fin ? d('ir_login', 'Ir al inicio de sesión') : d('siguiente', 'Siguiente');
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
    Array.prototype.forEach.call(caja.querySelectorAll('.sg-guia-barra i'), function (s, k) { s.classList.toggle('hecho', k <= estado.i); });
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
    // A su ritmo, solo avanza con Siguiente.
    if (!caja || corriendo || estado.pausado || estado.modo === 'ritmo') return;
    var tx = TXT[(PASOS[estado.i] || {}).id] || { p: '' };
    temporizador = setTimeout(function () { siguiente(false); }, Math.min(9000, Math.max(3000, 1200 + tx.p.length * 38)));
  }

  function soltar() {
    if (!alSalir) return;
    try { alSalir(); } catch (e) { /* ya cerrado */ }
    alSalir = null;
  }
  /** Lo que cambia la pagina al entrar al paso; devuelve los elementos a senalar. */
  function prepararPaso(paso) {
    soltar();
    try {
      if (paso.antes) paso.antes();
      objetivos = paso.foco ? paso.foco() : [];
    } catch (e) { objetivos = []; /* un elemento que no esta: se sigue */ }
    alSalir = paso.alSalir || null;
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
    // Al llegar a una pagina el recuadro aparece ya en su sitio; dentro de una pagina viaja desde donde estaba
    var llegando = !caja, desde = llegando ? null : rectObjetivo() || centroPantalla();
    crearCaja();
    corriendo = true;
    saltando = false;
    caja.classList.add('is-oculta');
    (llegando ? Promise.resolve() : esperar(90))
      .then(function () {
        if (paso.antes || alSalir) { desde = null; return cruzar(function () { prepararPaso(paso); }); }
        prepararPaso(paso);
      })
      .then(function () {
        pintar(paso);
        return viajar(planDesplazamiento(), desde);
      })
      .then(function () {
        var r = rectObjetivo() || centroPantalla();
        ultimo = r;
        pintarFoco(r);
        colocarCaja(r);
        caja.classList.remove('is-oculta');
        foco.classList.remove('llega'); void foco.offsetWidth; foco.classList.add('llega');
        return paso.demo ? esperar(350).then(paso.demo) : null;
      })
      .catch(function () { /* un elemento que no esta: se sigue */ })
      .then(function () {
        if (caja) caja.classList.remove('is-oculta');
        corriendo = false;
        if (saltando) siguiente(true); else programar();
      });
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
    clearTimeout(reloj);
    cancelAnimationFrame(cuadro);
    soltar();
    objetivos = [];
    if (vigia) { vigia.disconnect(); vigia = null; }
    if (caja) { caja.remove(); caja = null; }
    if (foco) { foco.remove(); foco = null; }
    document.documentElement.classList.remove('sg-guia-activa', 'sg-guia-lista');
    if ('scrollRestoration' in history) history.scrollRestoration = 'auto';
    document.removeEventListener('keydown', teclas);
    restaurar(estado && estado.guardado);
    try { sessionStorage.removeItem(CLAVE); } catch (e) { /* nada */ }
  }
  function salir() { limpiar(); estado = null; }
  function terminar() {
    var conCuenta = !!document.querySelector('.navbar[data-auth="true"]');
    salir();
    location.assign(conCuenta ? '/home' : '/login?guia=fin');
  }

  function iniciar() {
    try { localStorage.setItem(VISTO, '1'); } catch (e) { /* nada */ }
    estado = { i: 0, pausado: false, detalle: null, guardado: fotografia(), inicio: Date.now(), modo: 'auto', avisado: false };
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
    c.querySelector('.sg-tour-texto').textContent = d('invita', 'Un recorrido automático de cerca de un minuto. No se guarda nada.');
    var no = c.querySelector('.sg-tour-saltar'), si = c.querySelector('.btn');
    no.textContent = d('ahora_no', 'Ahora no');
    si.textContent = d('ver', 'Ver el recorrido');
    function tecla(e) { if (e.key === 'Escape') quitar(); }
    function quitar() {
      c.remove();
      document.removeEventListener('keydown', tecla);
      document.documentElement.classList.remove('sg-guia-activa');
      try { localStorage.setItem(VISTO, '1'); } catch (e) { /* nada */ }
    }
    no.addEventListener('click', quitar);
    si.addEventListener('click', function () { quitar(); iniciar(); });
    document.addEventListener('keydown', tecla);
    document.documentElement.classList.add('sg-guia-activa');
    document.body.appendChild(c);
    si.focus({ preventScroll: true });
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
      objetivos = [];
      var centro = centroPantalla();
      pintarFoco(centro);
      colocarCaja(centro);
      caja.classList.remove('is-oculta');
      return;
    }
    var visto;
    try { visto = localStorage.getItem(VISTO); } catch (e) { visto = '1'; }
    if (!visto && document.querySelector('.navbar[data-nav="public"]')) invitar();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', arrancar); else arrancar();
})();
