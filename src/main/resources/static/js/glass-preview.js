/* ============================================================
   SmartGlass — glass-preview.js
   Dibuja la "obra": una pieza de vidrio en SVG que refleja las
   decisiones del cliente (forma, tipo, tratamiento, bordes,
   perforaciones, espesor y proporciones reales).

   Uso:
     SGGlass.render(elemento, { ancho, alto, espesor, tipo, forma,
                                tratamiento, bordes, perforaciones,
                                reciclado, mini })
     SGGlass.tilt(elementoInclinable)        // inclinacion 3D con el puntero
     SGGlass.contar(elemento, 12.5, {...})   // contador animado
     SGGlass.describir(opts)                 // frase tipo "cartela de museo"

   Los colores salen de variables CSS (theme.css), asi que la pieza
   cambia sola entre modo claro y oscuro.
============================================================ */
(function () {
  'use strict';

  var uid = 0;

  var TIPOS = {
    Crudo:            { nombre: 'Crudo',               marca: '' },
    Templado:         { nombre: 'Templado',            marca: 'TEMPLADO' },
    Laminado:         { nombre: 'Laminado',            marca: 'LAMINADO', pvb: true },
    TempladoLaminado: { nombre: 'Templado + Laminado', marca: 'TEMPLADO · LAMINADO', pvb: true },
    Insulado:         { nombre: 'Insulado',            marca: 'INSULADO', camara: true },
    Inteligente:      { nombre: 'Inteligente (PDLC)',  marca: 'PDLC', pdlc: true },
    Fotovoltaico:     { nombre: 'Fotovoltaico',        marca: 'PV', solar: true }
  };

  var TRATAMIENTOS = {
    Ninguno:       {},
    TintadoGris:   { tinte: 'rgba(60, 72, 90, 0.55)' },
    TintadoBronce: { tinte: 'rgba(170, 105, 45, 0.5)' },
    ControlSolar:  { irid: true },
    Esmerilado:    { frost: true },
    Serigrafiado:  { puntos: true },
    Antireflejo:   { limpio: true }
  };

  var FRASES = {
    Crudo: 'Vidrio crudo: la base más honesta, sin tratamientos térmicos.',
    Templado: 'Templado: hasta cinco veces más resistente, listo para el uso diario.',
    Laminado: 'Laminado: dos láminas unidas por una capa que retiene los fragmentos.',
    TempladoLaminado: 'Templado y laminado: la combinación de máxima seguridad.',
    Insulado: 'Insulado: dos láminas con cámara de aire que aíslan calor y ruido.',
    Inteligente: 'Inteligente: privacidad que cambia con un interruptor.',
    Fotovoltaico: 'Fotovoltaico: una ventana que además genera energía.'
  };

  function num(v, d) {
    var n = parseFloat(v);
    return isFinite(n) && n > 0 ? n : d;
  }
  function f(n) { return Math.round(n * 100) / 100; }

  /* Contorno segun la geometria de corte */
  function contorno(forma, x, y, w, h) {
    var r = 3;
    if (forma === 'Circular') {
      return 'M' + f(x) + ',' + f(y + h / 2) +
        ' A' + f(w / 2) + ',' + f(h / 2) + ' 0 1,0 ' + f(x + w) + ',' + f(y + h / 2) +
        ' A' + f(w / 2) + ',' + f(h / 2) + ' 0 1,0 ' + f(x) + ',' + f(y + h / 2) + ' Z';
    }
    if (forma === 'Triangular') {
      return 'M' + f(x + w * 0.22) + ',' + f(y) + ' L' + f(x + w * 0.78) + ',' + f(y) +
        ' L' + f(x + w) + ',' + f(y + h) + ' L' + f(x) + ',' + f(y + h) + ' Z';
    }
    if (forma === 'Irregular') {
      var P = [[0.08, 0.14], [0.46, 0.0], [0.93, 0.1], [1.0, 0.56], [0.78, 1.0], [0.3, 0.92], [0.0, 0.62]];
      return 'M' + P.map(function (p) { return f(x + p[0] * w) + ',' + f(y + p[1] * h); }).join(' L') + ' Z';
    }
    return 'M' + f(x + r) + ',' + f(y) + ' H' + f(x + w - r) + ' Q' + f(x + w) + ',' + f(y) + ' ' + f(x + w) + ',' + f(y + r) +
      ' V' + f(y + h - r) + ' Q' + f(x + w) + ',' + f(y + h) + ' ' + f(x + w - r) + ',' + f(y + h) +
      ' H' + f(x + r) + ' Q' + f(x) + ',' + f(y + h) + ' ' + f(x) + ',' + f(y + h - r) +
      ' V' + f(y + r) + ' Q' + f(x) + ',' + f(y) + ' ' + f(x + r) + ',' + f(y) + ' Z';
  }

  function agujeros(n, x, y, w, h) {
    var m = Math.min(w, h) * 0.12, pts = [];
    var L = x + m, R = x + w - m, T = y + m, B = y + h - m, cx = x + w / 2;
    if (n >= 2) { pts.push([L, T], [R, T]); }
    if (n >= 4) { pts.push([L, B], [R, B]); }
    if (n >= 6) { pts.push([cx, T], [cx, B]); }
    return pts;
  }

  function render(target, o) {
    if (typeof target === 'string') target = document.querySelector(target);
    if (!target) return null;
    o = o || {};
    var id = 'sgg' + (++uid);
    var mini = !!o.mini;

    var tipoKey = TIPOS[o.tipo] ? o.tipo : 'Templado';
    var tipo = TIPOS[tipoKey];
    var trat = TRATAMIENTOS[o.tratamiento] || {};
    var ancho = num(o.ancho, 100), alto = num(o.alto, 100), esp = num(o.espesor, 6);
    var forma = o.forma || 'Rectangular';
    var bordes = o.bordes || 'CorteLimpio';
    var nHoles = parseInt(o.perforaciones, 10) || 0;

    /* Lienzo */
    var VW = mini ? 220 : 440, VH = mini ? 170 : 340;
    var maxW = mini ? 150 : 272, maxH = mini ? 104 : 196;
    var cx = mini ? 106 : 205, cy = mini ? 78 : 146;
    var ratio = ancho / alto, w, h;
    if (ratio >= maxW / maxH) { w = maxW; h = maxW / ratio; } else { h = maxH; w = maxH * ratio; }
    w = Math.max(w, 36); h = Math.max(h, 36);
    var x = cx - w / 2, y = cy - h / 2;

    /* Profundidad del canto (espesor real, comprimido) */
    var depth = Math.max(3, Math.min(mini ? 9 : 17, esp * 0.46));
    var pasos = Math.max(3, Math.round(depth));
    var dx = depth, dy = depth * 0.55;

    var d = contorno(forma, x, y, w, h);
    var s = '';

    s += '<svg class="sg-glass" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + VW + ' ' + VH + '" role="img" aria-label="' +
      'Vista previa: vidrio ' + tipo.nombre.toLowerCase() + ', ' + Math.round(ancho) + ' por ' + Math.round(alto) + ' centímetros, ' + esp + ' milímetros de espesor">';

    s += '<defs>' +
      '<clipPath id="' + id + 'c"><path d="' + d + '"/></clipPath>' +
      '<linearGradient id="' + id + 'f" x1="0" y1="0" x2="1" y2="1">' +
        '<stop offset="0" style="stop-color:var(--glass-face-a)"/><stop offset="1" style="stop-color:var(--glass-face-b)"/></linearGradient>' +
      '<linearGradient id="' + id + 'e" x1="0" y1="0" x2="1" y2="1">' +
        '<stop offset="0" style="stop-color:var(--glass-edge-a)"/><stop offset="1" style="stop-color:var(--glass-edge-b)"/></linearGradient>' +
      '<linearGradient id="' + id + 'g" x1="0" y1="0" x2="0.9" y2="1">' +
        '<stop offset="0" stop-color="#fff" stop-opacity="0.65"/><stop offset="0.55" stop-color="#fff" stop-opacity="0.06"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>' +
      '<linearGradient id="' + id + 'b" x1="0" y1="0" x2="1" y2="0">' +
        '<stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset="0.5" stop-color="#fff" stop-opacity="0.55"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>' +
      '<filter id="' + id + 'sh" x="-30%" y="-100%" width="160%" height="300%"><feGaussianBlur stdDeviation="' + (mini ? 4 : 7) + '"/></filter>';
    if (trat.frost) {
      s += '<filter id="' + id + 'fr" x="0" y="0" width="100%" height="100%">' +
        '<feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed="4" result="n"/>' +
        '<feColorMatrix in="n" type="matrix" values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 0 0 0.9 0"/></filter>';
    }
    if (trat.puntos) {
      s += '<pattern id="' + id + 'p" width="11" height="11" patternUnits="userSpaceOnUse"><circle cx="5.5" cy="5.5" r="1.9" fill="#fff" fill-opacity="0.75"/></pattern>';
    }
    if (tipo.solar) {
      s += '<pattern id="' + id + 's" width="26" height="26" patternUnits="userSpaceOnUse">' +
        '<rect width="26" height="26" fill="#0F2A5C"/><rect x="1.5" y="1.5" width="23" height="23" rx="2" fill="#173D82"/>' +
        '<path d="M1.5 9h23M1.5 17h23" stroke="#8FB3F0" stroke-opacity="0.45" stroke-width="0.8"/>' +
        '<path d="M8 1.5v23M18 1.5v23" stroke="#C9D8F5" stroke-opacity="0.55" stroke-width="0.9"/></pattern>';
    }
    s += '</defs>';

    /* Sombra de contacto */
    s += '<ellipse cx="' + f(cx + dx / 2) + '" cy="' + f(y + h + depth + (mini ? 8 : 16)) + '" rx="' + f(w * 0.46) + '" ry="' + (mini ? 5 : 8) +
      '" style="fill:var(--glass-shadow)" filter="url(#' + id + 'sh)"/>';

    s += '<g class="sg-float">';

    /* Canto: extrusion paso a paso (de atras hacia adelante) */
    var camaraMedio = Math.round(pasos / 2);
    for (var i = pasos; i >= 1; i--) {
      var ox = dx * i / pasos, oy = dy * i / pasos;
      s += '<path d="' + d + '" transform="translate(' + f(ox) + ' ' + f(oy) + ')" fill="url(#' + id + 'e)"' +
        (tipo.camara ? ' fill-opacity="0.55"' : '') + '/>';
      if (i === camaraMedio && (tipo.pvb || tipo.camara)) {
        s += '<path d="' + d + '" transform="translate(' + f(ox) + ' ' + f(oy) + ')" fill="none" ' +
          (tipo.camara ? 'stroke="#D5DDE8" stroke-width="2.6" stroke-opacity="0.95"' : 'stroke="#fff" stroke-width="1.3" stroke-opacity="0.85"') + '/>';
      }
    }

    /* Cara principal */
    if (tipo.solar) {
      s += '<path d="' + d + '" fill="#0F2A5C"/>';
      s += '<g clip-path="url(#' + id + 'c)"><rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="url(#' + id + 's)"/></g>';
    } else {
      s += '<path d="' + d + '" fill="url(#' + id + 'f)"/>';
    }

    s += '<g clip-path="url(#' + id + 'c)">';
    if (trat.tinte) s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="' + trat.tinte + '"/>';
    if (trat.irid) {
      s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="url(#' + id + 'i)"/>';
    }
    if (trat.frost) {
      s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="#fff" fill-opacity="0.28"/>' +
        '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" filter="url(#' + id + 'fr)" opacity="0.5"/>';
    }
    if (trat.puntos) s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="url(#' + id + 'p)"/>';
    if (tipo.pdlc) {
      s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w * 0.52) + '" height="' + f(h) + '" fill="#fff" fill-opacity="0.34"/>' +
        '<path d="M' + f(x + w * 0.52) + ',' + f(y) + ' V' + f(y + h) + '" stroke="#fff" stroke-opacity="0.8" stroke-width="1.2" stroke-dasharray="3 3"/>';
    }

    /* Brillo diagonal fijo + barrido de luz animado */
    s += '<rect x="' + f(x) + '" y="' + f(y) + '" width="' + f(w) + '" height="' + f(h) + '" fill="url(#' + id + 'g)"' + (trat.limpio ? ' opacity="0.45"' : '') + '/>';
    s += '<g class="sg-sweep"><polygon points="' + f(x - h * 0.4) + ',' + f(y + h) + ' ' + f(x + 18) + ',' + f(y + h) + ' ' + f(x + 18 + h * 0.55) + ',' + f(y) + ' ' + f(x - h * 0.4 + h * 0.55) + ',' + f(y) +
      '" fill="url(#' + id + 'b)" opacity="' + (trat.limpio ? 0.25 : 0.55) + '"/></g>';

    /* Bisel decorativo */
    if (bordes === 'Biselado') {
      s += '<path d="' + d + '" fill="none" stroke="#fff" stroke-opacity="0.42" stroke-width="' + (mini ? 6 : 10) + '"/>' +
        '<path d="' + d + '" fill="none" stroke="#fff" stroke-opacity="0.8" stroke-width="0.8" transform="translate(0 0)"/>';
    }

    /* Marca de templado/laminado (sello discreto) */
    if (!mini && tipo.marca) {
      s += '<text x="' + f(cx) + '" y="' + f(y + h * 0.9) + '" text-anchor="middle" font-size="7.5" letter-spacing="1.6" font-weight="700" style="fill:var(--glass-dim)" opacity="0.8">' + tipo.marca + '</text>';
    }
    s += '</g>';

    /* Perforaciones */
    agujeros(nHoles, x, y, w, h).forEach(function (p) {
      s += '<circle cx="' + f(p[0]) + '" cy="' + f(p[1]) + '" r="' + (mini ? 2.6 : 4.6) + '" style="fill:var(--stage-bottom)" stroke="#fff" stroke-opacity="0.85" stroke-width="1"/>';
    });

    /* Contorno (segun el acabado de borde) */
    s += '<path d="' + d + '" fill="none" style="stroke:var(--glass-outline)" stroke-width="' + (bordes === 'PulidoPlano' ? 1.8 : 1.1) + '"/>';

    /* Insignia de material reciclado */
    if (o.reciclado) {
      var bx = x + w - (forma === 'Circular' ? w * 0.15 : 2), by = y + (forma === 'Circular' ? h * 0.15 : 2);
      s += '<g transform="translate(' + f(bx) + ' ' + f(by) + ')">' +
        '<circle r="' + (mini ? 8 : 12) + '" fill="rgba(52,211,153,0.2)" stroke="#34D399" stroke-width="1.4"/>' +
        '<path d="M-4.5 3.2C-4.5-3.6 1.4-6 5-5.2C5.2 0.8 1.4 5-4.5 3.2Z M-4.5 3.2L2.6-2.6" fill="none" stroke="#34D399" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" transform="scale(' + (mini ? 0.7 : 1) + ')"/></g>';
    }
    s += '</g>'; // sg-float

    /* Cotas */
    if (!mini) {
      var yb = y + h + depth + 30, xr = x + w + depth + 26;
      s += '<g style="stroke:var(--glass-dim)" stroke-width="1" fill="none" opacity="0.9">' +
        '<path d="M' + f(x) + ',' + f(yb) + ' H' + f(x + w) + ' M' + f(x) + ',' + f(yb - 5) + ' V' + f(yb + 5) + ' M' + f(x + w) + ',' + f(yb - 5) + ' V' + f(yb + 5) + '"/>' +
        '<path d="M' + f(xr) + ',' + f(y) + ' V' + f(y + h) + ' M' + f(xr - 5) + ',' + f(y) + ' H' + f(xr + 5) + ' M' + f(xr - 5) + ',' + f(y + h) + ' H' + f(xr + 5) + '"/></g>';
      s += '<text x="' + f(x + w / 2) + '" y="' + f(yb + 17) + '" text-anchor="middle" font-size="12" font-weight="700" style="fill:var(--glass-dim)">' + Math.round(ancho) + ' cm</text>';
      s += '<text transform="translate(' + f(xr + 18) + ' ' + f(y + h / 2) + ') rotate(90)" text-anchor="middle" font-size="12" font-weight="700" style="fill:var(--glass-dim)">' + Math.round(alto) + ' cm</text>';
      var ex = x + w + dx, ey = y + h + dy;   /* esquina inferior derecha del canto */
      s += '<g style="stroke:var(--glass-dim)" stroke-width="1" fill="none" opacity="0.85"><path d="M' + f(ex) + ',' + f(ey) + ' l12,14"/></g>' +
        '<text x="' + f(ex + 15) + '" y="' + f(ey + 26) + '" font-size="11" font-weight="700" style="fill:var(--glass-dim)">' + esp + ' mm</text>';
    }
    s += '</svg>';

    /* Degradado iridiscente (control solar) se define aparte para no ensuciar el flujo */
    if (trat.irid) {
      s = s.replace('</defs>', '<linearGradient id="' + id + 'i" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5EEAD4" stop-opacity="0.32"/><stop offset="1" stop-color="#818CF8" stop-opacity="0.28"/></linearGradient></defs>');
    }

    target.innerHTML = s;
    return target.firstChild;
  }

  function describir(o) {
    o = o || {};
    var frase = FRASES[o.tipo] || FRASES.Templado;
    var med = Math.round(num(o.ancho, 0)) + ' × ' + Math.round(num(o.alto, 0)) + ' cm · ' + num(o.espesor, 0) + ' mm';
    return { titulo: (TIPOS[o.tipo] || TIPOS.Templado).nombre + ' · ' + med, frase: frase };
  }

  function tilt(el, max) {
    if (!el) return;
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    max = max || 7;
    var stage = el.closest('.sg-stage') || el;
    stage.addEventListener('pointermove', function (e) {
      var r = stage.getBoundingClientRect();
      var px = (e.clientX - r.left) / r.width - 0.5, py = (e.clientY - r.top) / r.height - 0.5;
      el.style.transform = 'perspective(900px) rotateY(' + (px * max).toFixed(2) + 'deg) rotateX(' + (-py * max).toFixed(2) + 'deg)';
    });
    stage.addEventListener('pointerleave', function () { el.style.transform = ''; });
  }

  /* Contador animado (no repite si el valor no cambia).
     op.fmt(v) -> texto; op.instantaneo -> sin animacion (p. ej. tras aplicar una recomendacion) */
  function contar(el, hasta, op) {
    if (!el) return;
    op = op || {};
    var dec = op.decimales || 0, pre = op.prefijo || '', suf = op.sufijo || '', dur = op.duracion || 800;
    function texto(v) {
      if (op.fmt) return op.fmt(v);
      return pre + (op.locale ? Math.round(v).toLocaleString(op.locale) : v.toFixed(dec)) + suf;
    }
    var desde = typeof el._sgv === 'number' ? el._sgv : (op.desde || 0);
    if (el._sgRaf) cancelAnimationFrame(el._sgRaf);
    el._sgv = hasta;
    var reducir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (op.instantaneo || desde === hasta || reducir) {
      el._sgRaf = null;
      el.textContent = texto(hasta);
      return;
    }
    var t0 = null;
    function paso(t) {
      if (t0 === null) t0 = t;
      var p = Math.min(1, (t - t0) / dur), e = 1 - Math.pow(1 - p, 3);
      el.textContent = texto(desde + (hasta - desde) * e);
      if (p < 1) el._sgRaf = requestAnimationFrame(paso); else { el._sgRaf = null; el.textContent = texto(hasta); }
    }
    el._sgRaf = requestAnimationFrame(paso);
  }

  /* Clasificacion por intensidad (kg CO2e por m2 de vidrio) */
  var UMBRAL = { favorable: 12, moderado: 20 };
  function impacto(intensidad) {
    var v = parseFloat(intensidad);
    if (!isFinite(v) || v < 0) v = 0;
    if (v <= UMBRAL.favorable) return { nivel: 'favorable', etiqueta: 'Favorable', emoji: '\uD83D\uDFE2', frase: 'Perfil ambiental favorable', valor: v };
    if (v <= UMBRAL.moderado) return { nivel: 'moderado', etiqueta: 'Moderado', emoji: '\uD83D\uDFE1', frase: 'Perfil aceptable, mejorable con m\u00e1s calc\u00edn', valor: v };
    return { nivel: 'alto', etiqueta: 'Alto', emoji: '\uD83D\uDD34', frase: 'Configuraci\u00f3n de alto impacto', valor: v };
  }
  function nombreTipo(v) { return (TIPOS[v] || {}).nombre || v || ''; }
  function fmt(n) {            /* 105.0 -> "105", 7.5 -> "7.5" */
    var x = parseFloat(n);
    if (!isFinite(x)) return '';
    return String(Math.round(x * 10) / 10).replace(/\.0$/, '');
  }


  /* Formato es-CO: miles con punto y decimales con coma (1.028 · 12,2). */
  function fmtNum(v, dec) {
    var n = parseFloat(v);
    if (!isFinite(n)) return '\u2014';
    dec = dec || 0;
    var neg = n < 0;
    var partes = Math.abs(n).toFixed(dec).split('.');
    var ent = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return (neg ? '-' : '') + ent + (dec ? ',' + partes[1] : '');
  }

  /* Medidas: entero sin decimales ("105"), y con una coma si hace falta ("7,5") */
  function fmtAuto(v) {
    var n = parseFloat(v);
    if (!isFinite(n)) return '';
    return Math.abs(n - Math.round(n)) < 0.05 ? fmtNum(Math.round(n), 0) : fmtNum(n, 1);
  }

  /* Contrato de formulas (no tocar constantes):
     area = ancho*alto/10000 ; peso = area*espesor*densidad (2.5 | 2.0 Insulado | 2.6 Laminado y Templado+Laminado)
     CO2 = peso * (0.6 renovable | 1.1) * (1.4 Templado+Laminado) * (1.8 Inteligente|Fotovoltaico) * (0.85 reciclado)
     intensidad = CO2 / area. El "baseline" es la misma pieza sin energia renovable ni reciclado. */
  function calcular(c) {
    var ancho = parseFloat(c.ancho) || 0, alto = parseFloat(c.alto) || 0, esp = parseFloat(c.espesor) || 0;
    var area = (ancho * alto) / 10000;
    var densidad = 2.5;
    if (c.tipo === 'Insulado') densidad = 2.0;
    if (c.tipo === 'Laminado' || c.tipo === 'TempladoLaminado') densidad = 2.6;
    var peso = area * esp * densidad;
    var mult = 1;
    if (c.tipo === 'TempladoLaminado') mult *= 1.4;
    if (c.tipo === 'Inteligente' || c.tipo === 'Fotovoltaico') mult *= 1.8;
    function co2De(renov, eco) { return peso * (renov ? 0.6 : 1.1) * mult * (eco ? 0.85 : 1); }
    var co2 = co2De(!!c.renov, !!c.eco);
    var base = co2De(false, false);
    return {
      area: area, peso: peso, co2: co2, base: base,
      ahorro: Math.min(Math.max(0, base - co2), base),          /* nunca supera el baseline */
      intensidad: area > 0 ? co2 / area : 0
    };
  }

  /* Espesores recomendados por uso (referencias del configurador). En los demas usos no se toca el espesor. */
  var RANGOS_USO = {
    Mobiliario: { min: 6, max: 8, nombre: 'muebles' },
    Ventana:    { min: 6, max: 10, nombre: 'ventanas' },
    Ducha:      { min: 8, max: 10, nombre: 'duchas' },
    Baranda:    { min: 12, max: 25, nombre: 'barandas' },
    Piso:       { min: 12, max: 25, nombre: 'pisos' }
  };

  /* Configuracion recomendada. Orden de intento: reciclado -> renovable -> espesor -> Templado+Laminado a Laminado.
     objetivo = intensidad que se busca no superar (20 si es alto, 12 si es moderado). */
  function recomendar(c0, objetivo) {
    objetivo = objetivo || UMBRAL.moderado;
    var c = {}, k;
    for (k in c0) c[k] = c0[k];
    var cambios = [];
    var antes = calcular(c0).intensidad;
    function int(x) { return calcular(x).intensidad; }
    function conEsp(e) { var d = {}, q; for (q in c) d[q] = c[q]; d.espesor = e; return d; }

    if (!c.eco) { c.eco = true; cambios.push({ campo: 'eco', texto: 'Integrar material reciclado (calc\u00edn)' }); }
    if (!c.renov) { c.renov = true; cambios.push({ campo: 'renov', texto: 'Procesar con energ\u00eda solar y e\u00f3lica' }); }

    var r = RANGOS_USO[c.uso];
    var esp = parseFloat(c.espesor) || 0;
    if (r && esp > r.min && (esp > r.max || int(c) > objetivo)) {
      var e = Math.min(esp, r.max);                       /* primero se entra al rango del uso */
      while (e > r.min && int(conEsp(e)) > objetivo) e = Math.max(r.min, e - 1);
      if (e < esp) {
        cambios.push({ campo: 'espesor', texto: 'Espesor de ' + fmt(esp) + ' a ' + fmt(e) + ' mm (rango recomendado para ' + r.nombre + ': ' + r.min + '\u2013' + r.max + ' mm)' });
        c.espesor = e;
      }
    }
    if (c.tipo === 'TempladoLaminado' && int(c) > objetivo) {
      c.tipo = 'Laminado';
      cambios.push({ campo: 'tipo', texto: 'Pasar de Templado + Laminado a Laminado (mantiene la seguridad antivandalismo)' });
    }
    var despues = int(c);
    return { cfg: c, cambios: cambios, antes: antes, despues: despues, nivelDespues: impacto(despues), logra: despues <= objetivo };
  }

  /* Equivalencias: 0.25 kg CO2 por km de auto y 21 kg CO2 por arbol al anio */
  function equivalencias(co2) {
    var v = parseFloat(co2) || 0;
    var anios = v / 21, meses = anios * 12, arbol;
    if (v <= 0) arbol = '0 meses';
    else if (anios >= 1) arbol = fmtNum(anios, 1) + ' a\u00f1os';
    else if (meses < 1) arbol = 'menos de 1 mes';
    else arbol = (Math.round(meses) === 1 ? '1 mes' : fmtNum(meses, 0) + ' meses');
    return { km: fmtNum(Math.round(v / 0.25), 0) + ' km', arbol: arbol };
  }

  window.SGGlass = {
    render: render, describir: describir, tilt: tilt, contar: contar, impacto: impacto, umbral: UMBRAL,
    nombreTipo: nombreTipo, fmt: fmt, fmtNum: fmtNum, fmtAuto: fmtAuto, calcular: calcular, recomendar: recomendar,
    equivalencias: equivalencias, rangosUso: RANGOS_USO
  };
})();