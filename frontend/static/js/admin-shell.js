/* Armazon del panel de administracion (admin/fragments/nav.html).
   - Buscador del panel: salta a cualquier seccion del menu (Ctrl+K / Cmd+K, flechas, Enter, Escape).
   - Menu lateral en movil: lo abre main.js (.nav-toggle); aqui se cierra con el fondo y al elegir.
   - Saludo segun la hora, entrada escalonada de los bloques y graficas de barras y dona (SVG propio).
   - Exportar CSV y filtros de la tabla de Finanzas.
   Respeta prefers-reduced-motion y el modo fotosensible (las animaciones son solo CSS). */
(function () {
  'use strict';

  var nav = document.querySelector('.adm-shell');
  if (!nav) return;
  var LANG = window.SG_LANG || 'es-CO';
  var NUM = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
  var SVGNS = 'http://www.w3.org/2000/svg';

  function quitarTildes(s) { return (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase(); }
  function el(tag, attrs, padre, texto) {
    var n = document.createElementNS(SVGNS, tag);
    for (var k in attrs) n.setAttribute(k, attrs[k]);
    if (texto != null) n.textContent = texto;
    if (padre) padre.appendChild(n);
    return n;
  }
  function compacto(v) {
    if (v >= 1e9) return '$' + (v / 1e9).toLocaleString('es-CO', { maximumFractionDigits: 1 }) + ' mil M';
    if (v >= 1e6) return '$' + (v / 1e6).toLocaleString('es-CO', { maximumFractionDigits: 1 }) + ' M';
    if (v >= 1e3) return '$' + (v / 1e3).toLocaleString('es-CO', { maximumFractionDigits: 0 }) + ' mil';
    return '$' + NUM.format(v);
  }

  /* ---------- Menu movil ---------- */
  function cerrarMovil() {
    nav.classList.remove('mobile-open');
    var t = nav.querySelector('.nav-toggle');
    if (t) t.setAttribute('aria-expanded', 'false');
  }
  nav.querySelectorAll('[data-adm-cerrar], .adm-links a').forEach(function (a) { a.addEventListener('click', cerrarMovil); });

  /* ---------- Buscador del panel ---------- */
  var input = document.getElementById('adm-buscar');
  var lista = document.getElementById('adm-buscar-lista');
  if (input && lista) {
    var destinos = Array.prototype.map.call(nav.querySelectorAll('.adm-links .nav-link'), function (a) {
      var grupo = a.closest('ul').previousElementSibling;
      var icono = a.querySelector('i');
      return { texto: a.textContent.replace(/\s+/g, ' ').trim().replace(/\s\d+$/, ''), href: a.href,
               grupo: grupo ? grupo.textContent.trim() : '', icono: icono ? icono.className : 'ph ph-arrow-right' };
    });
    var activo = -1, actuales = [];

    function pintar() {
      var q = quitarTildes(input.value.trim());
      actuales = destinos.filter(function (d) { return !q || quitarTildes(d.texto + ' ' + d.grupo).indexOf(q) !== -1; });
      lista.textContent = '';
      activo = actuales.length ? 0 : -1;
      if (!actuales.length) {
        var v = document.createElement('li');
        v.className = 'es-vacio';
        v.textContent = lista.getAttribute('data-vacio') || 'Sin resultados';
        lista.appendChild(v);
      }
      actuales.forEach(function (d, i) {
        var li = document.createElement('li');
        li.id = 'adm-op-' + i;
        li.setAttribute('role', 'option');
        li.setAttribute('aria-selected', String(i === activo));
        var ic = document.createElement('i'); ic.className = d.icono; ic.setAttribute('aria-hidden', 'true');
        var tx = document.createElement('span'); tx.textContent = d.texto;
        var gr = document.createElement('small'); gr.textContent = d.grupo;
        li.append(ic, tx, gr);
        li.addEventListener('mousedown', function (e) { e.preventDefault(); location.href = d.href; });
        lista.appendChild(li);
      });
      abrir(true);
      marcar();
    }
    function marcar() {
      Array.prototype.forEach.call(lista.querySelectorAll('[role="option"]'), function (li, i) {
        li.setAttribute('aria-selected', String(i === activo));
      });
      if (activo >= 0) input.setAttribute('aria-activedescendant', 'adm-op-' + activo);
      else input.removeAttribute('aria-activedescendant');
    }
    function abrir(si) {
      lista.hidden = !si;
      input.setAttribute('aria-expanded', String(si));
    }
    input.addEventListener('focus', pintar);
    input.addEventListener('input', pintar);
    input.addEventListener('blur', function () { setTimeout(function () { abrir(false); }, 120); });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        if (!actuales.length) return;
        e.preventDefault();
        activo = (activo + (e.key === 'ArrowDown' ? 1 : -1) + actuales.length) % actuales.length;
        marcar();
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (actuales[activo]) location.href = actuales[activo].href;
      } else if (e.key === 'Escape') {
        input.value = '';
        abrir(false);
        input.blur();
      }
    });
    document.addEventListener('keydown', function (e) {
      if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        input.focus();
        input.select();
      }
    });
    var kbd = nav.querySelector('.adm-kbd');
    if (kbd && /Mac|iPhone|iPad/.test(navigator.platform || '')) kbd.textContent = '⌘ K';
  }

  document.addEventListener('DOMContentLoaded', function () {
    var main = document.getElementById('contenido');
    if (!main) return;

    /* ---------- Saludo segun la hora (Bogota en el servidor; aqui la del navegador) ---------- */
    var saludo = main.querySelector('[data-adm-saludo]');
    if (saludo) {
      var h = new Date().getHours();
      var clave = h < 12 ? 'data-manana' : (h < 19 ? 'data-tarde' : 'data-noche');
      var plantilla = saludo.getAttribute(clave);
      if (plantilla) saludo.textContent = plantilla.replace('{0}', saludo.getAttribute('data-nombre') || '');
    }

    /* ---------- Entrada escalonada ---------- */
    var i = 0;
    Array.prototype.forEach.call(main.children, function (hijo) {
      if (/^(SCRIPT|STYLE|TEMPLATE)$/.test(hijo.tagName)) return;
      hijo.classList.add('adm-entra');
      hijo.style.setProperty('--adm-i', Math.min(i++, 8));
    });

    /* ---------- Graficas ---------- */
    var datos = window.SG_ADM || {};
    main.querySelectorAll('[data-adm-barras]').forEach(function (caja) { barras(caja, datos[caja.getAttribute('data-adm-barras')] || []); });
    main.querySelectorAll('[data-adm-dona]').forEach(function (caja) { dona(caja, datos[caja.getAttribute('data-adm-dona')] || []); });

    /* ---------- Finanzas: filtros y CSV ---------- */
    filtros(main);
    var csv = main.querySelector('[data-adm-csv]');
    if (csv) csv.addEventListener('click', function () { exportar(main.querySelector(csv.getAttribute('data-adm-csv')), csv.getAttribute('data-archivo')); });
  });

  /* Barras por mes con pestanas de 3, 6, 9 y 12 meses. serie = [{clave:'2026-11', ingresos, pedidos}] */
  function barras(caja, serie) {
    var lienzo = caja.querySelector('.adm-graf');
    var tabs = caja.querySelectorAll('[data-meses]');
    var elegido = caja.querySelector('[data-meses][aria-pressed="true"]');
    var meses = elegido ? parseInt(elegido.getAttribute('data-meses'), 10) : 6;
    var fmtMes = new Intl.DateTimeFormat(LANG, { month: 'short' });
    var unidad = caja.getAttribute('data-unidad') || '';

    function dibujar() {
      var datos = serie.slice(-meses);
      lienzo.textContent = '';
      var ancho = Math.max(260, lienzo.clientWidth), alto = Math.max(180, lienzo.clientHeight || 220);
      var m = { a: 30, b: 28, i: 52, d: 8 };
      var max = Math.max.apply(null, datos.map(function (d) { return d.ingresos; }).concat([1]));
      var paso = Math.pow(10, Math.floor(Math.log10(max)));
      var tope = Math.ceil(max / paso) * paso;
      var svg = el('svg', { viewBox: '0 0 ' + ancho + ' ' + alto, width: ancho, height: alto, role: 'img',
                            'aria-label': caja.getAttribute('data-aria') || '' }, lienzo);
      var util = alto - m.a - m.b, y = function (v) { return m.a + util * (1 - v / tope); };
      [0, 0.5, 1].forEach(function (k) {
        el('line', { x1: m.i, x2: ancho - m.d, y1: y(tope * k), y2: y(tope * k), 'class': 'adm-eje' }, svg);
        el('text', { x: m.i - 8, y: y(tope * k), 'text-anchor': 'end', 'dominant-baseline': 'central', 'class': 'adm-eje-t' }, svg, compacto(tope * k));
      });
      var col = (ancho - m.i - m.d) / datos.length, w = Math.min(46, col * 0.56);
      datos.forEach(function (d, i) {
        var x = m.i + col * i + (col - w) / 2, hBar = Math.max(d.ingresos > 0 ? 4 : 0, util * d.ingresos / tope);
        var ultimo = i === datos.length - 1;
        var r = Math.min(8, w / 2, hBar);
        var g = el('g', { 'class': 'adm-barra' + (ultimo ? ' es-actual' : ''), style: '--adm-b:' + i }, svg);
        if (hBar > 0) {
          el('path', { d: 'M' + x + ' ' + (m.a + util) + 'v' + (r - hBar) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + (-r) +
                          'h' + (w - 2 * r) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + r + 'v' + (hBar - r) + 'z' }, g);
        }
        var fecha = new Date(d.clave + '-15T12:00:00');
        el('text', { x: x + w / 2, y: alto - 8, 'text-anchor': 'middle', 'class': 'adm-eje-t' }, svg, fmtMes.format(fecha).replace('.', ''));
        el('title', {}, g, fmtMes.format(fecha) + ': ' + unidad + NUM.format(d.ingresos) + ' · ' + d.pedidos);
        if (ultimo && d.ingresos > 0) {
          var etiqueta = compacto(d.ingresos), ew = etiqueta.length * 7 + 16, ex = Math.min(ancho - m.d - ew, Math.max(m.i, x + w / 2 - ew / 2));
          var ey = Math.max(2, y(d.ingresos) - 28);
          el('rect', { x: ex, y: ey, width: ew, height: 22, rx: 11, 'class': 'adm-burbuja' }, svg);
          el('text', { x: ex + ew / 2, y: ey + 11, 'text-anchor': 'middle', 'dominant-baseline': 'central', 'class': 'adm-burbuja-t' }, svg, etiqueta);
        }
      });
      // Tabla equivalente para lectores de pantalla
      var tabla = document.createElement('table');
      tabla.className = 'visually-hidden';
      datos.forEach(function (d) {
        var tr = tabla.insertRow();
        tr.insertCell().textContent = d.clave;
        tr.insertCell().textContent = unidad + NUM.format(d.ingresos);
      });
      lienzo.appendChild(tabla);
    }
    tabs.forEach(function (b) {
      b.addEventListener('click', function () {
        meses = parseInt(b.getAttribute('data-meses'), 10);
        tabs.forEach(function (o) { o.setAttribute('aria-pressed', String(o === b)); });
        dibujar();
      });
    });
    dibujar();
    if ('ResizeObserver' in window) {
      var ultimoAncho = lienzo.clientWidth;
      new ResizeObserver(function () {
        if (Math.abs(lienzo.clientWidth - ultimoAncho) < 8) return;
        ultimoAncho = lienzo.clientWidth;
        dibujar();
      }).observe(lienzo);
    }
  }

  /* Dona: partes = [{etiqueta, valor, color}] con el total en el centro (HTML ya presente en la caja) */
  function dona(caja, partes) {
    var lienzo = caja.querySelector('.adm-dona-svg');
    if (!lienzo) return;
    var total = partes.reduce(function (s, p) { return s + p.valor; }, 0);
    var r = 54, c = 2 * Math.PI * r, inicio = 0;
    var svg = el('svg', { viewBox: '0 0 140 140', 'aria-hidden': 'true' }, lienzo);
    el('circle', { cx: 70, cy: 70, r: r, 'class': 'adm-dona-fondo' }, svg);
    partes.forEach(function (p, i) {
      if (!total || !p.valor) return;
      var largo = c * p.valor / total, hueco = partes.length > 1 ? Math.min(4, largo / 3) : 0;
      el('circle', { cx: 70, cy: 70, r: r, 'class': 'adm-dona-parte', style: 'stroke:' + p.color + ';--adm-b:' + i,
                     'stroke-dasharray': Math.max(0, largo - hueco) + ' ' + c, 'stroke-dashoffset': -inicio,
                     transform: 'rotate(-90 70 70)' }, svg);
      inicio += largo;
    });
  }

  function filtros(main) {
    var botones = main.querySelectorAll('[data-filtro]');
    if (!botones.length) return;
    var filas = main.querySelectorAll('[data-estado]');
    var vacio = main.querySelector('[data-filtro-vacio]');
    botones.forEach(function (b) {
      b.addEventListener('click', function () {
        var f = b.getAttribute('data-filtro').split(' ');
        botones.forEach(function (o) { o.setAttribute('aria-pressed', String(o === b)); });
        var visibles = 0;
        filas.forEach(function (tr) {
          var ver = f[0] === 'todos' || f.indexOf(tr.getAttribute('data-estado')) !== -1;
          tr.hidden = !ver;
          if (ver) visibles++;
        });
        if (vacio) vacio.hidden = visibles > 0;
      });
    });
  }

  /* CSV de las filas visibles; se genera en el navegador (no sale nada del equipo) */
  function exportar(tabla, nombre) {
    if (!tabla) return;
    var celda = function (t) { t = (t || '').replace(/\s+/g, ' ').trim(); return /[";\n]/.test(t) ? '"' + t.replace(/"/g, '""') + '"' : t; };
    var lineas = [];
    tabla.querySelectorAll('tr').forEach(function (tr) {
      if (tr.hidden) return;
      var cols = Array.prototype.filter.call(tr.children, function (c) { return !c.hasAttribute('data-sin-csv'); });
      lineas.push(cols.map(function (c) { return celda(c.getAttribute('data-csv') || c.textContent); }).join(';'));
    });
    var blob = new Blob(['﻿' + lineas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = (nombre || 'smartglass') + '-' + new Date().toISOString().slice(0, 10) + '.csv';
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 500);
  }
})();
