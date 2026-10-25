/* ============================================================
   SmartGlass — glass-preview.js
   Dibuja la "obra": una pieza de vidrio en SVG que refleja las
   decisiones del cliente (forma, tipo, tratamiento, bordes,
   perforaciones, espesor y proporciones reales).

   Uso:
     SGGlass.render(elemento, { ancho, alto, espesor, tipo, forma,
                                tratamiento, bordes, perforaciones,
                                reciclado, mini,
                                puntos })   // puntos [{x,y}]: solo con forma 'Personalizada' (F45)
     SGGlass.tilt(elementoInclinable)        // inclinacion 3D con el puntero
     SGGlass.contar(elemento, 12.5, {...})   // contador animado
     SGGlass.describir(opts)                 // frase tipo "cartela de museo"

   Los colores salen de variables CSS (theme.css), asi que la pieza
   cambia sola entre modo claro y oscuro.
============================================================ */
(function () {
  'use strict';

  var uid = 0;

  /* Textos traducibles (T2): window.SG_TXT lo llena fragments/textos-vidrio.html segun el idioma activo.
     Sin diccionario se usa el respaldo en espanol. Solo textos: ninguna cifra ni constante pasa por aqui. */
  function t(k, r) {
    var s = (window.SG_TXT && window.SG_TXT[k]) || r || '', a = [].slice.call(arguments, 2);
    return s.replace(/\{(\d+)\}/g, function (m, i) { return a[i] != null ? a[i] : m; });
  }
  function esc(v) { return String(v).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;'); }

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
    if (POLIGONOS[forma]) {
      return 'M' + POLIGONOS[forma].map(function (p) { return f(x + p[0] * w) + ',' + f(y + p[1] * h); }).join(' L') + ' Z';
    }
    if (forma === 'EsquinasRedondeadas') r = Math.min(w, h) * 0.18;
    if (forma === 'Capsula') r = Math.min(w, h) / 2;
    if (forma === 'Arco') {                       /* arco de medio punto (eliptico si la pieza es baja) */
      var ry = Math.min(w / 2, h * 0.6);
      return 'M' + f(x) + ',' + f(y + h) + ' V' + f(y + ry) + ' A' + f(w / 2) + ',' + f(ry) + ' 0 0,1 ' + f(x + w) + ',' + f(y + ry) +
        ' V' + f(y + h) + ' Z';
    }
    if (r > 3) {
      return 'M' + f(x + r) + ',' + f(y) + ' H' + f(x + w - r) + ' A' + f(r) + ',' + f(r) + ' 0 0,1 ' + f(x + w) + ',' + f(y + r) +
        ' V' + f(y + h - r) + ' A' + f(r) + ',' + f(r) + ' 0 0,1 ' + f(x + w - r) + ',' + f(y + h) +
        ' H' + f(x + r) + ' A' + f(r) + ',' + f(r) + ' 0 0,1 ' + f(x) + ',' + f(y + h - r) +
        ' V' + f(y + r) + ' A' + f(r) + ',' + f(r) + ' 0 0,1 ' + f(x + r) + ',' + f(y) + ' Z';
    }
    return 'M' + f(x + r) + ',' + f(y) + ' H' + f(x + w - r) + ' Q' + f(x + w) + ',' + f(y) + ' ' + f(x + w) + ',' + f(y + r) +
      ' V' + f(y + h - r) + ' Q' + f(x + w) + ',' + f(y + h) + ' ' + f(x + w - r) + ',' + f(y + h) +
      ' H' + f(x + r) + ' Q' + f(x) + ',' + f(y + h) + ' ' + f(x) + ',' + f(y + h - r) +
      ' V' + f(y + r) + ' Q' + f(x) + ',' + f(y) + ' ' + f(x + r) + ',' + f(y) + ' Z';
  }

  /* T2: formas predefinidas como poligonos normalizados a su caja (0..1). Solo presentacion:
     todas (menos "Personalizada") se cotizan por su caja envolvente, igual que en fabrica se corta de la lamina. */
  var POLIGONOS = {
    Irregular:           [[0.08, 0.14], [0.46, 0.0], [0.93, 0.1], [1.0, 0.56], [0.78, 1.0], [0.3, 0.92], [0.0, 0.62]],
    Hexagonal:           [[0.25, 0], [0.75, 0], [1, 0.5], [0.75, 1], [0.25, 1], [0, 0.5]],
    Octogonal:           [[0.29, 0], [0.71, 0], [1, 0.29], [1, 0.71], [0.71, 1], [0.29, 1], [0, 0.71], [0, 0.29]],
    Rombo:               [[0.5, 0], [1, 0.5], [0.5, 1], [0, 0.5]],
    TrianguloRectangulo: [[0, 0], [1, 1], [0, 1]],
    TrapecioRectangulo:  [[0, 0], [1, 0.38], [1, 1], [0, 1]],
    Fronton:             [[0.5, 0], [1, 0.34], [1, 1], [0, 1], [0, 0.34]]
  };
  /* Donde va la insignia de reciclado (fraccion de la caja), para que caiga sobre el vidrio */
  var INSIGNIA = {
    Circular: [0.85, 0.15], Capsula: [0.86, 0.2], Arco: [0.93, 0.56], Hexagonal: [0.76, 0.06], Octogonal: [0.8, 0.1],
    Rombo: [0.68, 0.3], TrianguloRectangulo: [0.14, 0.32], TrapecioRectangulo: [0.96, 0.44], Fronton: [0.9, 0.42],
    Triangular: [0.78, 0.04], EsquinasRedondeadas: [0.95, 0.06]
  };
  var FORMAS = ['Rectangular', 'EsquinasRedondeadas', 'Circular', 'Capsula', 'Arco', 'Triangular', 'TrianguloRectangulo',
                'TrapecioRectangulo', 'Fronton', 'Rombo', 'Hexagonal', 'Octogonal', 'Irregular', 'Personalizada'];

  /* Contorno de una forma predefinida como poligono en una caja w x h (cm). Las curvas se aproximan con
     segmentos. Lo usa el editor F45 para empezar desde la forma que ya elegiste. */
  function poligonoForma(forma, w, h, n) {
    n = Math.max(8, n || 32);
    var P = [], i, a;
    function arco(cx, cy, rx, ry, a0, a1, pasos) {
      for (var k = 0; k <= pasos; k++) { var b = a0 + (a1 - a0) * k / pasos; P.push([cx + rx * Math.cos(b), cy + ry * Math.sin(b)]); }
    }
    if (POLIGONOS[forma]) P = POLIGONOS[forma].map(function (p) { return [p[0] * w, p[1] * h]; });
    else if (forma === 'Triangular') P = [[0.22 * w, 0], [0.78 * w, 0], [w, h], [0, h]];
    else if (forma === 'Circular') {
      for (i = 0; i < n; i++) { a = -Math.PI / 2 + i * 2 * Math.PI / n; P.push([w / 2 + w / 2 * Math.cos(a), h / 2 + h / 2 * Math.sin(a)]); }
    } else if (forma === 'Arco') {
      var ry = Math.min(w / 2, h * 0.6);
      P.push([0, h]); arco(w / 2, ry, w / 2, ry, Math.PI, 2 * Math.PI, 10); P.push([w, h]);
    } else if (forma === 'EsquinasRedondeadas' || forma === 'Capsula') {
      var r = forma === 'Capsula' ? Math.min(w, h) / 2 : Math.min(w, h) * 0.18, s = forma === 'Capsula' ? 6 : 4;
      arco(w - r, r, r, r, -Math.PI / 2, 0, s); arco(w - r, h - r, r, r, 0, Math.PI / 2, s);
      arco(r, h - r, r, r, Math.PI / 2, Math.PI, s); arco(r, r, r, r, Math.PI, 1.5 * Math.PI, s);
    } else P = [[0, 0], [w, 0], [w, h], [0, h]];
    return P.map(function (p) { return { x: p[0], y: p[1] }; });
  }

  /* Silueta de referencia de 1,70 m (escala humana): pies en (px, base), alto total ph */
  function silueta(px, base, ph) {
    var u = ph / 170, cab = 11 * u;
    return '<g class="sg-humano" style="fill:var(--glass-dim)" opacity="0.55">' +
      '<circle cx="' + f(px) + '" cy="' + f(base - ph + cab) + '" r="' + f(cab) + '"/>' +
      '<path d="M' + f(px - 21 * u) + ',' + f(base - ph + 2 * cab + 6 * u) + ' Q' + f(px) + ',' + f(base - ph + 2 * cab) + ' ' + f(px + 21 * u) + ',' + f(base - ph + 2 * cab + 6 * u) +
      ' L' + f(px + 24 * u) + ',' + f(base - 80 * u) + ' L' + f(px + 15 * u) + ',' + f(base - 80 * u) + ' L' + f(px + 12 * u) + ',' + f(base) +
      ' L' + f(px + 2 * u) + ',' + f(base) + ' L' + f(px) + ',' + f(base - 70 * u) + ' L' + f(px - 2 * u) + ',' + f(base) +
      ' L' + f(px - 12 * u) + ',' + f(base) + ' L' + f(px - 15 * u) + ',' + f(base - 80 * u) + ' L' + f(px - 24 * u) + ',' + f(base - 80 * u) + ' Z"/></g>';
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
    /* T2: la pieza ocupa mas lienzo (antes 272 x 196) sin cambiar la proporcion del SVG */
    var VW = mini ? 220 : 440, VH = mini ? 170 : 340;
    var maxW = mini ? 150 : 292, maxH = mini ? 104 : 208;
    var cx = mini ? 106 : 207, cy = mini ? 78 : 147;
    var conCotas = !mini && o.cotas !== false;
    var humano = !mini && !!o.referencia, escalaH = 0;
    var ratio = ancho / alto, w, h, x, y;
    if (humano) {
      /* Escala humana: pieza y silueta de 170 cm comparten la misma escala, apoyadas en el mismo piso */
      var anchoTotal = 46 + 24 + ancho, altoTotal = Math.max(alto, 170);
      escalaH = Math.min(maxW / anchoTotal, maxH / altoTotal);
      w = Math.max(ancho * escalaH, 6); h = Math.max(alto * escalaH, 6);
      x = cx - anchoTotal * escalaH / 2 + 70 * escalaH;
      y = cy + altoTotal * escalaH / 2 - h;
    } else {
      if (ratio >= maxW / maxH) { w = maxW; h = maxW / ratio; } else { h = maxH; w = maxH * ratio; }
      w = Math.max(w, 36); h = Math.max(h, 36);
      x = cx - w / 2; y = cy - h / 2;
    }

    /* Profundidad del canto (espesor real, comprimido) */
    var depth = Math.max(3, Math.min(mini ? 9 : 17, esp * 0.46));
    var pasos = Math.max(3, Math.round(depth));
    var dx = depth, dy = depth * 0.55;

    var d;
    if (forma === 'Personalizada' && o.puntos && o.puntos.length >= 3) {
      /* F45: dibuja TU poligono, escalado a la misma caja (w x h) que usan las formas fijas. */
      var pb = { minX: Infinity, minY: Infinity, maxX: -Infinity, maxY: -Infinity };
      o.puntos.forEach(function (p) {
        if (p.x < pb.minX) pb.minX = p.x; if (p.y < pb.minY) pb.minY = p.y;
        if (p.x > pb.maxX) pb.maxX = p.x; if (p.y > pb.maxY) pb.maxY = p.y;
      });
      var pw = Math.max(pb.maxX - pb.minX, 1), ph = Math.max(pb.maxY - pb.minY, 1);
      var sx = w / pw, sy = h / ph;
      d = 'M' + f(x + (o.puntos[0].x - pb.minX) * sx) + ',' + f(y + (o.puntos[0].y - pb.minY) * sy);
      for (var pi = 1; pi < o.puntos.length; pi++) {
        d += ' L' + f(x + (o.puntos[pi].x - pb.minX) * sx) + ',' + f(y + (o.puntos[pi].y - pb.minY) * sy);
      }
      d += ' Z';
    } else {
      d = contorno(forma, x, y, w, h);
    }
    var s = '';

    s += '<svg class="sg-glass" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + VW + ' ' + VH + '" role="img" aria-label="' +
      esc(t('gv_aria', 'Vista previa: vidrio {0}, {1} por {2} centímetros, {3} milímetros de espesor',
            nombreTipo(tipoKey).toLowerCase(), Math.round(ancho), Math.round(alto), esp)) + '">';

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

    /* Escala humana: silueta de 1,70 m y linea de piso comunes */
    if (humano) {
      var piso = y + h, pxH = cx - (46 + 24 + ancho) * escalaH / 2 + 23 * escalaH;
      s += '<path d="M' + f(pxH - 34 * escalaH) + ',' + f(piso) + ' H' + f(x + w + 24 * escalaH) + '" style="stroke:var(--glass-dim)" stroke-width="1" stroke-dasharray="3 4" opacity="0.6"/>' +
        silueta(pxH, piso, 170 * escalaH) +
        '<text x="' + f(pxH) + '" y="' + f(piso - 170 * escalaH - 6) + '" text-anchor="middle" font-size="11" font-weight="700" style="fill:var(--glass-dim)">' +
        esc(t('gv_persona', '1,70 m')) + '</text>';
    }

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
      var ins = INSIGNIA[forma], bx = ins ? x + w * ins[0] : x + w - 2, by = ins ? y + h * ins[1] : y + 2;
      s += '<g transform="translate(' + f(bx) + ' ' + f(by) + ')">' +
        '<circle r="' + (mini ? 8 : 12) + '" fill="rgba(52,211,153,0.2)" stroke="#34D399" stroke-width="1.4"/>' +
        '<path d="M-4.5 3.2C-4.5-3.6 1.4-6 5-5.2C5.2 0.8 1.4 5-4.5 3.2Z M-4.5 3.2L2.6-2.6" fill="none" stroke="#34D399" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" transform="scale(' + (mini ? 0.7 : 1) + ')"/></g>';
    }
    s += '</g>'; // sg-float

    /* Cotas (se pueden ocultar desde los controles de vista) */
    if (conCotas) {
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
    var k = FRASES[o.tipo] ? o.tipo : 'Templado', frase = t('gv_frase_' + k, FRASES[k]);
    var med = Math.round(num(o.ancho, 0)) + ' × ' + Math.round(num(o.alto, 0)) + ' cm · ' + num(o.espesor, 0) + ' mm';
    return { titulo: nombreTipo(k) + ' · ' + med, frase: frase };
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
    if (v <= UMBRAL.favorable) return { nivel: 'favorable', etiqueta: t('gv_etq_favorable', 'Favorable'), emoji: '\uD83D\uDFE2', frase: t('gv_frase_favorable', 'Perfil ambiental favorable'), valor: v };
    if (v <= UMBRAL.moderado) return { nivel: 'moderado', etiqueta: t('gv_etq_moderado', 'Moderado'), emoji: '\uD83D\uDFE1', frase: t('gv_frase_moderado', 'Perfil aceptable, mejorable con m\u00e1s calc\u00edn'), valor: v };
    return { nivel: 'alto', etiqueta: t('gv_etq_alto', 'Alto'), emoji: '\uD83D\uDD34', frase: t('gv_frase_alto', 'Configuraci\u00f3n de alto impacto'), valor: v };
  }
  function nombreTipo(v) { return TIPOS[v] ? t('gv_tipo_' + v, TIPOS[v].nombre) : (v || ''); }
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

  /* F45: area de un poligono (shoelace/Gauss). pts = [{x,y}] en cm. Devuelve cm2. Una sola fuente en JS. */
  function areaPoligono(pts) {
    if (!pts || pts.length < 3) return 0;
    var s = 0, n = pts.length;
    for (var i = 0; i < n; i++) {
      var a = pts[i], b = pts[(i + 1) % n];
      s += a.x * b.y - b.x * a.y;
    }
    return Math.abs(s) / 2;
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

    if (!c.eco) { c.eco = true; cambios.push({ campo: 'eco', texto: t('gv_rec_eco', 'Integrar material reciclado (calc\u00edn)') }); }
    if (!c.renov) { c.renov = true; cambios.push({ campo: 'renov', texto: t('gv_rec_renov', 'Procesar con energ\u00eda solar y e\u00f3lica') }); }

    var r = RANGOS_USO[c.uso];
    var esp = parseFloat(c.espesor) || 0;
    if (r && esp > r.min && (esp > r.max || int(c) > objetivo)) {
      var e = Math.min(esp, r.max);                       /* primero se entra al rango del uso */
      while (e > r.min && int(conEsp(e)) > objetivo) e = Math.max(r.min, e - 1);
      if (e < esp) {
        cambios.push({ campo: 'espesor', texto: t('gv_rec_espesor', 'Espesor de {0} a {1} mm (rango recomendado para {2}: {3}\u2013{4} mm)',
                                                    fmt(esp), fmt(e), t('gv_uso_' + c.uso, r.nombre), r.min, r.max) });
        c.espesor = e;
      }
    }
    if (c.tipo === 'TempladoLaminado' && int(c) > objetivo) {
      c.tipo = 'Laminado';
      cambios.push({ campo: 'tipo', texto: t('gv_rec_tipo', 'Pasar de Templado + Laminado a Laminado (mantiene la seguridad antivandalismo)') });
    }
    var despues = int(c);
    return { cfg: c, cambios: cambios, antes: antes, despues: despues, nivelDespues: impacto(despues), logra: despues <= objetivo };
  }

  /* Equivalencias: 0.25 kg CO2 por km de auto y 21 kg CO2 por arbol al anio */
  function equivalencias(co2) {
    var v = parseFloat(co2) || 0;
    var anios = v / 21, meses = anios * 12, arbol;
    if (v <= 0) arbol = t('gv_meses', '{0} meses', '0');
    else if (anios >= 1) arbol = t('gv_anios', '{0} a\u00f1os', fmtNum(anios, 1));
    else if (meses < 1) arbol = t('gv_menos_mes', 'menos de 1 mes');
    else arbol = (Math.round(meses) === 1 ? t('gv_un_mes', '1 mes') : t('gv_meses', '{0} meses', fmtNum(meses, 0)));
    return { km: fmtNum(Math.round(v / 0.25), 0) + ' km', arbol: arbol };
  }

  /* Nombre legible de la geometria de corte (el servidor guarda el codigo del formulario) */
  var NOMBRES_FORMA = {
    Rectangular: 'Rectangular', EsquinasRedondeadas: 'Esquinas redondeadas', Circular: 'Circular / Elíptica',
    Capsula: 'Cápsula', Arco: 'Arco de medio punto', Triangular: 'Trapezoidal', TrianguloRectangulo: 'Triángulo rectángulo',
    TrapecioRectangulo: 'Inclinada', Fronton: 'Frontón', Rombo: 'Rombo', Hexagonal: 'Hexagonal', Octogonal: 'Octogonal',
    Irregular: 'Irregular (plano CAD)', Personalizada: 'Corte personalizado'
  };
  function nombreForma(v) { return NOMBRES_FORMA[v] ? t('gv_forma_' + v, NOMBRES_FORMA[v]) : (v || ''); }

  /* Controles de la vista previa (T2): cotas, escala humana y ampliar. Preferencia por visitante en localStorage.
     host = contenedor de la barra; escena = .sg-stage que se amplia; alCambiar(estado) vuelve a dibujar. */
  var KEY_VISTA = 'sg-vista';
  function leerVista() {
    try { var v = JSON.parse(localStorage.getItem(KEY_VISTA) || '{}') || {}; return { cotas: v.cotas !== false, referencia: !!v.referencia }; }
    catch (e) { return { cotas: true, referencia: false }; }
  }
  function controlesVista(host, escena, alCambiar) {
    var st = leerVista();
    if (!host) return st;
    host.classList.add('sg-vista-ctl');
    host.setAttribute('role', 'toolbar');
    host.setAttribute('aria-label', t('gv_ctl_titulo', 'Opciones de la vista previa'));
    function boton(icono, texto) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'sg-vista-btn';
      b.innerHTML = '<i class="ph ' + icono + '" aria-hidden="true"></i><span></span>';
      b.lastChild.textContent = texto;
      host.appendChild(b);
      return b;
    }
    [['cotas', 'ph-ruler', t('gv_ctl_cotas', 'Cotas')], ['referencia', 'ph-person-simple', t('gv_ctl_persona', 'Escala humana')]].forEach(function (d) {
      var b = boton(d[1], d[2]);
      b.setAttribute('aria-pressed', String(st[d[0]]));
      b.addEventListener('click', function () {
        st[d[0]] = !st[d[0]];
        b.setAttribute('aria-pressed', String(st[d[0]]));
        try { localStorage.setItem(KEY_VISTA, JSON.stringify(st)); } catch (e) { /* sin almacenamiento: solo esta visita */ }
        if (alCambiar) alCambiar(st);
      });
    });
    if (escena) {
      var amp = boton('ph-arrows-out', t('gv_ctl_ampliar', 'Ampliar'));
      amp.setAttribute('aria-pressed', 'false');
      var cerrarAmp = function () {
        escena.classList.remove('sg-ampliado'); document.body.classList.remove('sg-sin-scroll');
        amp.setAttribute('aria-pressed', 'false'); amp.querySelector('i').className = 'ph ph-arrows-out';
        amp.lastChild.textContent = t('gv_ctl_ampliar', 'Ampliar');
      };
      amp.addEventListener('click', function () {
        if (escena.classList.contains('sg-ampliado')) { cerrarAmp(); return; }
        escena.classList.add('sg-ampliado'); document.body.classList.add('sg-sin-scroll');
        amp.setAttribute('aria-pressed', 'true'); amp.querySelector('i').className = 'ph ph-arrows-in';
        amp.lastChild.textContent = t('gv_ctl_reducir', 'Cerrar vista ampliada');
        amp.focus();
      });
      document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && escena.classList.contains('sg-ampliado')) cerrarAmp(); });
    }
    return st;
  }

  window.SGGlass = {
    render: render, describir: describir, tilt: tilt, contar: contar, impacto: impacto, umbral: UMBRAL,
    nombreTipo: nombreTipo, nombreForma: nombreForma, formas: FORMAS, fmt: fmt, fmtNum: fmtNum, fmtAuto: fmtAuto,
    calcular: calcular, recomendar: recomendar, equivalencias: equivalencias, rangosUso: RANGOS_USO, areaPoligono: areaPoligono,
    controlesVista: controlesVista, poligonoForma: poligonoForma, t: t
  };
})();