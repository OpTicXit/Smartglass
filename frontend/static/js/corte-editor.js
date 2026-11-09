/* =============================================================================
   SMARTGLASS · F45 CORTE PERSONALIZADO (editor de lienzo)
   Vanilla JS, sin dependencias. Requiere glass-preview.js cargado ANTES.

   - Dibuja un poligono (3 a 64 vertices) arrastrando, anadiendo y quitando puntos.
   - Una sola fuente de numeros: SGGlass.areaPoligono (shoelace), SGGlass.calcular
     (formulas del contrato), SGGlass.impacto (umbrales 12/20) y SGGlass.recomendar (R1).
     Aqui NO se duplican constantes del contrato. El numero que se guarda lo calcula el
     servidor (AnalisisService) con la misma area.
   - R1 (version balanceada) cambia material/proceso, NUNCA mueve un vertice.
   - Progreso: deshacer/rehacer, autoguardado en localStorage y banner de restauracion.
   - Al confirmar escribe en el formulario: ancho, alto (caja del corte), forma = Personalizada
     y el campo oculto "puntosTexto" ("x,y;x,y;..."). Avisa con el evento "sgcorte:cerrado".
   - Herramientas: puntos, contorno a mano (se simplifica a un poligono), y pincel, relleno y goma para un
     diseno sobre el vidrio. El diseno no entra en precio ni CO2: va al taller en el campo oculto "disenoPng".
     La vista 3D en vivo vive en corte-3d.js.
   - T2: lienzo de 420 x 420 cm (cabe la pieza maxima del formulario, 400 x 400), vista con
     encuadre, zoom con la rueda y arrastre para desplazar; puntos de tamano tactil; edicion
     numerica del punto elegido; medidas exactas; girar, reflejar y centrar; iman de rejilla
     de 1, 5 o 10 cm; plantillas de todas las formas del configurador; textos traducibles.
   ============================================================================= */
(function () {
    'use strict';

    var G = window.SGGlass;

    var LIENZO_W = 420, LIENZO_H = 420;           // cm reales (1 unidad SVG = 1 cm)
    var MIN_PUNTOS = 3, MAX_PUNTOS = 64;
    var MIN_DIM = 10;                             // cm; igual que min="10" de ancho/alto en el formulario
    var MAX_COORD = 1000;                         // cm; igual que MAX_COORD_CORTE_CM de AnalisisService
    var UNDO_MAX = 50;
    var KEY_BORRADOR = 'sg-borrador-corte';
    var KEY_PREVIO = 'sg-borrador-corte-previo';
    var KEY_LIMPIAR = 'sg-borrador-corte-limpiar';   // lo pone el formulario al enviar; lo consume el detalle
    var Z_MIN = 0.5, Z_MAX = 6;
    /* Diseno pintado: mapa de bits que cubre el lienzo entero (2 px por cm) y se recorta con el contorno al mostrarlo */
    var PX_POR_CM = 2, DISENO_PX = LIENZO_W * PX_POR_CM;
    var DISENO_LADO_MAX = 1000, DISENO_BYTES_MAX = 500000;   // lo que se envia; el servidor rechaza mas de 700.000
    var TOLERANCIA_RELLENO = 40;
    var COLORES = ['#00C2FF', '#0072FF', '#00E5A0', '#FBBF24', '#F2542D', '#E0389A', '#7C4DFF', '#FFFFFF', '#111827'];
    var MODOS = [
        { id: 'puntos', icono: 'ph-cursor-click' }, { id: 'contorno', icono: 'ph-pencil-simple-line' },
        { id: 'pincel', icono: 'ph-paint-brush' }, { id: 'relleno', icono: 'ph-paint-bucket' }, { id: 'goma', icono: 'ph-eraser' }
    ];

    var estado = {
        abierto: false, puntos: [], formaBase: 'Rectangular', sel: -1,
        undo: [], redo: [], arrastrando: -1, movio: false, nCirculo: 32, previo: null, opener: null,
        snap: 1, vista: { z: 1, cx: LIENZO_W / 2, cy: LIENZO_H / 2 }, gesto: null, base: { w: 120, h: 120 },
        modo: 'puntos', color: COLORES[0], grosor: 6, diseno: null, disenoUrl: null, trazo: null
    };

    /* ------------------------------------------------------------------ textos (T2) */
    function T(k, r) {
        var s = (window.SG_TXT && window.SG_TXT['sgc_' + k]) || r || '', a = [].slice.call(arguments, 2);
        return s.replace(/\{(\d+)\}/g, function (m, i) { return a[i] != null ? a[i] : m; });
    }
    function esc(v) { return String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }

    /* ------------------------------------------------------------------ geometria pura */
    function snapCon(v, paso) { return Math.round(v / paso) * paso; }
    function snap(v) { return snapCon(v, estado.snap); }
    function cloneP(pts) { return pts.map(function (p) { return { x: p.x, y: p.y }; }); }
    function snapAll(pts, paso) { return pts.map(function (p) { return { x: snapCon(p.x, paso || 1), y: snapCon(p.y, paso || 1) }; }); }
    /* Puntos consecutivos repetidos (p. ej. al ajustar a la rejilla) crean aristas de largo cero: se quitan */
    function sinRepetidos(pts) {
        var out = [];
        pts.forEach(function (p) { var u = out[out.length - 1]; if (!u || u.x !== p.x || u.y !== p.y) out.push(p); });
        while (out.length > 1 && out[0].x === out[out.length - 1].x && out[0].y === out[out.length - 1].y) out.pop();
        return out;
    }
    function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }

    function areaPoligono(pts) { return G && G.areaPoligono ? G.areaPoligono(pts) : 0; }

    function perimetro(pts) {
        if (!pts || pts.length < 2) return 0;
        var s = 0;
        for (var i = 0; i < pts.length; i++) {
            var a = pts[i], b = pts[(i + 1) % pts.length];
            s += Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
        }
        return s;
    }

    function bbox(pts) {
        if (!pts || !pts.length) return { w: 0, h: 0, minX: 0, minY: 0, maxX: 0, maxY: 0 };
        var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        pts.forEach(function (p) {
            if (p.x < minX) minX = p.x; if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x; if (p.y > maxY) maxY = p.y;
        });
        return { w: maxX - minX, h: maxY - minY, minX: minX, minY: minY, maxX: maxX, maxY: maxY };
    }

    /* Misma regla que AnalisisService.autointersecta: aristas NO contiguas que se cruzan o se tocan */
    function orient(a, b, c) { return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x); }
    function enSeg(a, b, c) {
        return Math.min(a.x, b.x) <= c.x && c.x <= Math.max(a.x, b.x) && Math.min(a.y, b.y) <= c.y && c.y <= Math.max(a.y, b.y);
    }
    function seCruzan(a, b, c, d) {
        var o1 = orient(a, b, c), o2 = orient(a, b, d), o3 = orient(c, d, a), o4 = orient(c, d, b);
        if (((o1 > 0 && o2 < 0) || (o1 < 0 && o2 > 0)) && ((o3 > 0 && o4 < 0) || (o3 < 0 && o4 > 0))) return true;
        return (o1 === 0 && enSeg(a, b, c)) || (o2 === 0 && enSeg(a, b, d)) || (o3 === 0 && enSeg(c, d, a)) || (o4 === 0 && enSeg(c, d, b));
    }
    function autointersecta(p) {
        var n = p.length;
        for (var i = 0; i < n; i++) {
            var a = p[i], b = p[(i + 1) % n];
            for (var j = i + 1; j < n; j++) {
                if (j === i + 1 || (i === 0 && j === n - 1)) continue;
                if (seCruzan(a, b, p[j], p[(j + 1) % n])) return true;
            }
        }
        return false;
    }

    /* Distancia de un punto a un segmento (para insertar el vertice en la arista mas cercana) */
    function distSeg(p, a, b) {
        var dx = b.x - a.x, dy = b.y - a.y, l2 = dx * dx + dy * dy;
        var t = l2 === 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / l2));
        var qx = a.x + t * dx, qy = a.y + t * dy;
        return Math.sqrt((p.x - qx) * (p.x - qx) + (p.y - qy) * (p.y - qy));
    }
    function aristaMasCercana(p, pts) {
        var mejor = 0, md = Infinity;
        for (var i = 0; i < pts.length; i++) {
            var d = distSeg(p, pts[i], pts[(i + 1) % pts.length]);
            if (d < md) { md = d; mejor = i; }
        }
        return mejor;
    }

    /* Trazo a mano -> poligono: Ramer-Douglas-Peucker con tolerancia creciente hasta caber en el maximo de puntos */
    function rdp(pts, tol) {
        if (pts.length < 3) return pts.slice();
        var a = pts[0], b = pts[pts.length - 1], peor = 0, ip = 0;
        for (var i = 1; i < pts.length - 1; i++) { var d = distSeg(pts[i], a, b); if (d > peor) { peor = d; ip = i; } }
        if (peor <= tol) return [a, b];
        return rdp(pts.slice(0, ip + 1), tol).slice(0, -1).concat(rdp(pts.slice(ip), tol));
    }
    function simplificarTrazo(pts, maximo) {
        if (pts.length < 3) return [];
        // Se parte por el punto mas lejano al inicio: en un trazo cerrado, inicio y fin casi coinciden
        var lejos = 0, md = -1;
        pts.forEach(function (p, i) { var d = Math.hypot(p.x - pts[0].x, p.y - pts[0].y); if (d > md) { md = d; lejos = i; } });
        for (var tol = 1.2; ; tol *= 1.3) {
            var r = rdp(pts.slice(0, lejos + 1), tol).slice(0, -1).concat(rdp(pts.slice(lejos), tol));
            r = sinRepetidos(snapAll(r, 1));
            if (r.length <= maximo) return r;
        }
    }

    /* Mueve el poligono lo justo para que quede dentro del lienzo */
    function dentroDelLienzo(pts) {
        var bb = bbox(pts), dx = 0, dy = 0;
        if (bb.minX < 0) dx = -bb.minX; else if (bb.maxX > LIENZO_W) dx = LIENZO_W - bb.maxX;
        if (bb.minY < 0) dy = -bb.minY; else if (bb.maxY > LIENZO_H) dy = LIENZO_H - bb.maxY;
        return pts.map(function (p) { return { x: p.x + dx, y: p.y + dy }; });
    }

    /* Formato "x,y;x,y" (identico a PuntoCorte.java) */
    function num2(v) { var s = (Math.round(v * 100) / 100).toFixed(2); return s.replace(/0+$/, '').replace(/\.$/, ''); }
    function aTexto(pts) { return (pts || []).map(function (p) { return num2(p.x) + ',' + num2(p.y); }).join(';'); }
    function desdeTexto(t) {
        if (!t || typeof t !== 'string' || t.length > 4000) return [];
        var pares = t.trim().split(';'), out = [];
        if (pares.length > MAX_PUNTOS) return [];
        for (var i = 0; i < pares.length; i++) {
            var xy = pares[i].trim().split(',');
            if (xy.length !== 2) return [];
            var x = parseFloat(xy[0]), y = parseFloat(xy[1]);
            if (!isFinite(x) || !isFinite(y) || x < 0 || y < 0 || x > MAX_COORD || y > MAX_COORD) return [];
            out.push({ x: x, y: y });
        }
        return out;
    }

    /* ------------------------------------------------------------------ formas base */
    /* Mismas formas que el configurador (SGGlass.poligonoForma) + triangulo, poligono regular y corte libre */
    var ORDEN_FORMAS = ['Rectangular', 'EsquinasRedondeadas', 'Circular', 'Capsula', 'Arco', 'Triangulo', 'Triangular',
                        'TrianguloRectangulo', 'TrapecioRectangulo', 'Fronton', 'Rombo', 'Hexagonal', 'Octogonal', 'Poligono', 'Libre'];
    var NOMBRES = {
        Rectangular: 'Rectangular', EsquinasRedondeadas: 'Esquinas redondeadas', Circular: 'Circular / Elíptica',
        Capsula: 'Cápsula', Arco: 'Arco', Triangulo: 'Triángulo', Triangular: 'Trapecio', TrianguloRectangulo: 'Triángulo rectángulo',
        TrapecioRectangulo: 'Inclinada', Fronton: 'Frontón', Rombo: 'Rombo', Hexagonal: 'Hexagonal', Octogonal: 'Octogonal',
        Poligono: 'Polígono de 9 lados', Libre: 'Corte libre'
    };
    function nombreForma(id) { return T('f_' + id, NOMBRES[id] || id); }

    /* Poligono de la forma en una caja w x h con origen en (0,0) */
    function formaEnCaja(id, w, h) {
        if (id === 'Libre') return [];
        if (id === 'Triangulo') return [{ x: w / 2, y: 0 }, { x: w, y: h }, { x: 0, y: h }];
        if (id === 'Poligono') {
            var a = [];
            for (var i = 0; i < 9; i++) { var t = i * 2 * Math.PI / 9 - Math.PI / 2; a.push({ x: w / 2 + w / 2 * Math.cos(t), y: h / 2 + h / 2 * Math.sin(t) }); }
            return a;
        }
        return G.poligonoForma(id, w, h, estado.nCirculo);
    }
    function formaBase(id) {
        if (!NOMBRES[id]) return null;
        var w = clamp(estado.base.w, MIN_DIM, LIENZO_W - 20), h = clamp(estado.base.h, MIN_DIM, LIENZO_H - 20);
        var ox = (LIENZO_W - w) / 2, oy = (LIENZO_H - h) / 2;
        return sinRepetidos(snapAll(formaEnCaja(id, w, h).map(function (p) { return { x: p.x + ox, y: p.y + oy }; }), 1));
    }
    function formasBase() {
        var o = {};
        ORDEN_FORMAS.forEach(function (id) { o[id] = formaBase(id); });
        return o;
    }
    function iconoForma(id) {
        if (id === 'Libre') return '<i class="ph ph-pencil-simple-line" aria-hidden="true"></i>';
        var pts = formaEnCaja(id, 22, 16);
        return '<svg viewBox="-2 -2 26 20" aria-hidden="true"><path d="M' + pts.map(function (p) { return p.x.toFixed(1) + ' ' + p.y.toFixed(1); }).join(' L') + ' Z"/></svg>';
    }

    /* ------------------------------------------------------------------ lectura/escritura del formulario */
    function $(id) { return document.getElementById(id); }

    function cfgDelForm() {
        function val(id, d) { var e = $(id); return e ? e.value : d; }
        function chk(id) { var e = $(id); return !!(e && e.checked); }
        return {
            tipo: val('tipoVidrio', 'Templado') || 'Templado',
            espesor: parseFloat(val('espesor', '6')) || 0,
            renov: chk('energiaRenovable'),
            eco: chk('opcionEco'),
            uso: val('usoPrincipal', '')
        };
    }

    function puntosConfirmados() {
        var h = $('puntosTexto'), sel = $('forma');
        if (!h || !sel || sel.value !== 'Personalizada') return [];
        var p = desdeTexto(h.value);
        return p.length >= MIN_PUNTOS ? p : [];
    }

    function dispararEventos(el) {
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
    }
    function setValor(id, v) { var e = $(id); if (e) { e.value = v; dispararEventos(e); } }

    /* ------------------------------------------------------------------ metricas en vivo */
    /* Mismas formulas del contrato: SGGlass.calcular con un cuadrado de igual AREA que el poligono
       (calcular() solo usa ancho*alto; la intensidad no depende de la forma). */
    function metricas(ptsOpc) {
        var pts = ptsOpc || estado.puntos;
        var areaCm2 = pts.length >= MIN_PUNTOS ? areaPoligono(pts) : 0;
        var bb = bbox(pts), c = cfgDelForm();
        var s = Math.sqrt(areaCm2);
        var r = G.calcular({ ancho: s, alto: s, espesor: c.espesor, tipo: c.tipo, renov: c.renov, eco: c.eco });
        return {
            areaCm2: areaCm2, areaM2: areaCm2 / 10000, perimetro: perimetro(pts), bbox: bb, vertices: pts.length,
            peso: r.peso, co2: r.co2, intensidad: r.intensidad, imp: G.impacto(r.intensidad), cfg: c, hayArea: areaCm2 > 0
        };
    }

    function validez() {
        var pts = estado.puntos, n = pts.length;
        if (n < MIN_PUNTOS) return { ok: false, msg: T('err_min', 'Necesitas al menos {0} puntos (tienes {1}).', MIN_PUNTOS, n) };
        if (n > MAX_PUNTOS) return { ok: false, msg: T('err_max', 'El máximo es {0} puntos.', MAX_PUNTOS) };
        var bb = bbox(pts);
        if (bb.w < MIN_DIM || bb.h < MIN_DIM) return { ok: false, msg: T('err_dim', 'El corte debe medir al menos {0} × {0} cm.', MIN_DIM) };
        if (autointersecta(pts)) return { ok: false, msg: T('err_cruce', 'Las líneas del corte se cruzan o se tocan: mueve algún punto.') };
        if (!(areaPoligono(pts) > 0)) return { ok: false, msg: T('err_area', 'El corte no encierra área.') };
        return { ok: true, msg: '' };
    }

    /* ------------------------------------------------------------------ diseno pintado */
    var versionDiseno = 0;
    /* Cada cambio crea un lienzo nuevo: el historial de deshacer guarda referencias, sin copiar nada */
    function copiaDiseno(origen) {
        var c = document.createElement('canvas');
        c.width = c.height = DISENO_PX;
        if (origen) c.getContext('2d').drawImage(origen, 0, 0);
        return c;
    }
    function trazarContorno(ctx, pts, escala) {
        ctx.beginPath();
        pts.forEach(function (p, i) { if (i) ctx.lineTo(p.x * escala, p.y * escala); else ctx.moveTo(p.x * escala, p.y * escala); });
        ctx.closePath();
    }
    function hayPintura(ctx, w, h) {
        var d = ctx.getImageData(0, 0, w, h).data;
        for (var i = 3; i < d.length; i += 4) if (d[i]) return true;
        return false;
    }
    /* Solo lo que queda dentro de la pieza, recortado a su caja: asi lo recibe el taller */
    function exportarDiseno() {
        if (!estado.diseno || estado.puntos.length < MIN_PUNTOS) return '';
        var bb = bbox(estado.puntos), escala = Math.min(PX_POR_CM, DISENO_LADO_MAX / Math.max(bb.w, bb.h, 1));
        for (var intento = 0; intento < 5; intento++, escala *= 0.75) {
            var c = document.createElement('canvas');
            c.width = Math.max(1, Math.round(bb.w * escala)); c.height = Math.max(1, Math.round(bb.h * escala));
            var ctx = c.getContext('2d');
            ctx.translate(-bb.minX * escala, -bb.minY * escala);
            trazarContorno(ctx, estado.puntos, escala);
            ctx.clip();
            ctx.drawImage(estado.diseno, 0, 0, LIENZO_W * escala, LIENZO_H * escala);
            if (!hayPintura(ctx, c.width, c.height)) return '';
            var png = c.toDataURL('image/png');
            if (png.length <= DISENO_BYTES_MAX) return png;
        }
        return '';
    }
    /* Inverso de exportarDiseno: lo vuelve a colocar sobre la caja de la pieza */
    function cargarDiseno(png, pts) {
        if (!png || pts.length < MIN_PUNTOS) return;
        var img = new Image();
        img.onload = function () {
            var bb = bbox(pts), c = copiaDiseno(null);
            c.getContext('2d').drawImage(img, bb.minX * PX_POR_CM, bb.minY * PX_POR_CM, bb.w * PX_POR_CM, bb.h * PX_POR_CM);
            estado.diseno = c;
            refrescarDiseno();
        };
        img.src = png;
    }
    function refrescarDiseno() {
        var version = ++versionDiseno, lienzo = estado.diseno;
        if (!lienzo) {
            if (estado.disenoUrl) { URL.revokeObjectURL(estado.disenoUrl); estado.disenoUrl = null; }
            pintarLienzo();
            return;
        }
        lienzo.toBlob(function (b) {
            if (version !== versionDiseno) return;             // toBlob es asincrono: un resultado viejo no pisa al nuevo
            if (estado.disenoUrl) URL.revokeObjectURL(estado.disenoUrl);
            estado.disenoUrl = URL.createObjectURL(b);
            pintarLienzo();
        });
        pintarLienzo();
    }
    function empezarPincel(p) {
        pushUndo();
        estado.diseno = copiaDiseno(estado.diseno);
        var ctx = estado.diseno.getContext('2d');
        ctx.lineCap = ctx.lineJoin = 'round';
        ctx.lineWidth = estado.grosor * PX_POR_CM;
        ctx.strokeStyle = ctx.fillStyle = estado.color;
        ctx.globalCompositeOperation = estado.modo === 'goma' ? 'destination-out' : 'source-over';
        estado.trazo = { tipo: 'pincel', ctx: ctx, ultimo: p, d: 'M' + p.x + ' ' + p.y + ' L' + p.x + ' ' + p.y };
        extenderPincel(p);
    }
    function extenderPincel(p) {
        var t = estado.trazo, ctx = t.ctx;
        ctx.beginPath();
        ctx.moveTo(t.ultimo.x * PX_POR_CM, t.ultimo.y * PX_POR_CM);
        ctx.lineTo(p.x * PX_POR_CM, p.y * PX_POR_CM);
        ctx.stroke();
        t.ultimo = p;
        t.d += ' L' + p.x + ' ' + p.y;
        var vivo = $('sgc-trazo');
        if (vivo) vivo.setAttribute('d', t.d);
        avisar3D();
    }
    /* Relleno por contagio dentro de la pieza: se detiene en lo pintado y en el contorno; sin dibujo, cubre toda la pieza */
    function rellenar(p) {
        var w = DISENO_PX, x0 = Math.floor(p.x * PX_POR_CM), y0 = Math.floor(p.y * PX_POR_CM);
        if (estado.puntos.length < MIN_PUNTOS || x0 < 0 || y0 < 0 || x0 >= w || y0 >= w) return;
        var mascara = copiaDiseno(null), mctx = mascara.getContext('2d');
        trazarContorno(mctx, estado.puntos, PX_POR_CM);
        mctx.fill();
        var m = mctx.getImageData(0, 0, w, w).data;
        if (m[(y0 * w + x0) * 4 + 3] < 128) return;              // fuera de la pieza

        var nuevo = copiaDiseno(estado.diseno), ctx = nuevo.getContext('2d'), img = ctx.getImageData(0, 0, w, w), d = img.data;
        var i0 = (y0 * w + x0) * 4, r0 = d[i0], g0 = d[i0 + 1], b0 = d[i0 + 2], a0 = d[i0 + 3];
        var hex = parseInt(estado.color.slice(1), 16), r = hex >> 16 & 255, g = hex >> 8 & 255, b = hex & 255;
        var visto = new Uint8Array(w * w), pila = [y0 * w + x0];
        function entra(k) {
            if (visto[k] || m[k * 4 + 3] < 128) return false;
            var i = k * 4;
            if (a0 < 16) return d[i + 3] < 110;                   // sobre vacio: el borde suavizado de un trazo tambien detiene
            return Math.abs(d[i] - r0) + Math.abs(d[i + 1] - g0) + Math.abs(d[i + 2] - b0) + Math.abs(d[i + 3] - a0) <= TOLERANCIA_RELLENO;
        }
        while (pila.length) {
            var k = pila.pop();
            if (!entra(k)) continue;
            visto[k] = 1;
            var i = k * 4;
            d[i] = r; d[i + 1] = g; d[i + 2] = b; d[i + 3] = 255;
            var x = k % w;
            if (x > 0) pila.push(k - 1);
            if (x < w - 1) pila.push(k + 1);
            if (k >= w) pila.push(k - w);
            if (k < w * (w - 1)) pila.push(k + w);
        }
        ctx.putImageData(img, 0, 0);
        pushUndo();
        estado.diseno = nuevo;
        refrescarDiseno();
        autosave();
    }
    function borrarDiseno() {
        if (!estado.diseno) return;
        pushUndo();
        estado.diseno = null;
        refrescarDiseno();
        autosave();
    }
    function terminarContorno(t) {
        var r = simplificarTrazo(t.pts, MAX_PUNTOS);
        if (r.length < MIN_PUNTOS) { pintarLienzo(); return; }
        pushUndo();
        estado.puntos = r; estado.formaBase = 'Libre'; estado.sel = -1;
        encajar();
        pintarLienzo();
        autosave();
    }
    function cambiarModo(id) {
        estado.modo = id;
        var pinta = id === 'pincel' || id === 'relleno' || id === 'goma';
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-modo-btn'), function (b) {
            b.setAttribute('aria-pressed', String(b.getAttribute('data-modo') === id));
        });
        $('sgc-pintura').hidden = !pinta;
        $('sgc-grosor-box').hidden = id === 'relleno';
        $('sgc-colores').hidden = id === 'goma';
        $('sgc-modo-ayuda').textContent = T('ayuda_' + id, AYUDAS[id]);
        pintarLienzo();
    }
    var AYUDAS = {
        contorno: 'Dibuja de un solo trazo la silueta de la pieza; se cierra sola y se vuelve un corte con puntos que puedes ajustar. Si las líneas se cruzan, usa Deshacer.',
        pincel: 'Pinta sobre el vidrio. Solo se ve dentro de la pieza. El diseño no cambia el precio: es una referencia para el taller, que confirma el acabado.',
        relleno: 'Toca una zona para llenarla de color; se detiene en lo que hayas pintado. Sin dibujo, cubre toda la pieza.',
        goma: 'Borra lo pintado. No cambia el contorno de la pieza.'
    };
    function avisar3D() {
        if (!window.SGCorte3D || !estado.abierto) return;
        window.SGCorte3D.actualizar({ puntos: estado.puntos, espesorMm: cfgDelForm().espesor, diseno: estado.diseno });
    }

    /* ------------------------------------------------------------------ vista (zoom y desplazamiento) */
    function pxPorCm() {
        var svg = $('sgc-lienzo'); if (!svg) return 1;
        var r = svg.getBoundingClientRect();
        var s = Math.min(r.width / LIENZO_W, r.height / LIENZO_H);
        return (s > 0 ? s : 1.4) * estado.vista.z;
    }
    /* Encuadre en pixeles: deja sitio a las cotas (derecha y abajo) y a la barra de zoom (abajo) */
    function encajar() {
        var v = estado.vista, pts = estado.puntos, svg = $('sgc-lienzo');
        if (pts.length >= 2) {
            var bb = bbox(pts), r = svg ? svg.getBoundingClientRect() : { width: 0, height: 0 };
            var lado = Math.min(r.width, r.height) || 600, s = lado / LIENZO_W;          // px por cm con zoom 1
            var lados = 64, arriba = 28, abajo = 88;
            v.z = clamp(Math.min((lado - 2 * lados) / Math.max(bb.w * s, 1), (lado - arriba - abajo) / Math.max(bb.h * s, 1)), Z_MIN, Z_MAX);
            v.cx = bb.minX + bb.w / 2;
            v.cy = bb.minY + bb.h / 2 + ((abajo - arriba) / 2) / (s * v.z);              // la pieza sube para no quedar bajo la barra
        } else { v.z = 1; v.cx = LIENZO_W / 2; v.cy = LIENZO_H / 2; }
    }
    function zoomEn(factor, p) {
        var v = estado.vista, z2 = clamp(v.z * factor, Z_MIN, Z_MAX);
        if (p) { v.cx = p.x - (p.x - v.cx) * v.z / z2; v.cy = p.y - (p.y - v.cy) * v.z / z2; }
        v.z = z2;
        v.cx = clamp(v.cx, 0, LIENZO_W); v.cy = clamp(v.cy, 0, LIENZO_H);
        pintarLienzo();
    }

    /* ------------------------------------------------------------------ pintado */
    function svgPath(pts) {
        if (!pts.length) return '';
        var d = 'M' + pts[0].x + ' ' + pts[0].y;
        for (var i = 1; i < pts.length; i++) d += ' L' + pts[i].x + ' ' + pts[i].y;
        return d + ' Z';
    }
    var tactil = window.matchMedia && window.matchMedia('(pointer: coarse)').matches;

    function pintarLienzo() {
        var svg = $('sgc-lienzo'); if (!svg) return;
        var pts = estado.puntos, m = metricas(), v = validez();
        var ppu = pxPorCm(), u = 1 / ppu;                         // u = 1 px de pantalla en cm
        var rejilla = '', gx, gy, etiquetas = '';
        for (gx = 0; gx <= LIENZO_W; gx += 10) rejilla += '<line x1="' + gx + '" y1="0" x2="' + gx + '" y2="' + LIENZO_H + '" class="sgc-grid' + (gx % 50 === 0 ? ' sgc-grid-50' : '') + '"/>';
        for (gy = 0; gy <= LIENZO_H; gy += 10) rejilla += '<line x1="0" y1="' + gy + '" x2="' + LIENZO_W + '" y2="' + gy + '" class="sgc-grid' + (gy % 50 === 0 ? ' sgc-grid-50' : '') + '"/>';
        if (ppu * 50 > 34) {                                      // regla cada 50 cm, solo si se lee
            for (gx = 50; gx < LIENZO_W; gx += 50) etiquetas += '<text x="' + gx + '" y="' + (12 * u) + '" class="sgc-regla" font-size="' + (11 * u) + '">' + gx + '</text>';
            for (gy = 50; gy < LIENZO_H; gy += 50) etiquetas += '<text x="' + (4 * u) + '" y="' + (gy + 4 * u) + '" class="sgc-regla sgc-regla-y" font-size="' + (11 * u) + '">' + gy + '</text>';
        }
        var rv = (tactil ? 10 : 6.5) * u, rh = (tactil ? 22 : 12) * u;
        var asas = pts.map(function (p, i) {
            return '<circle cx="' + p.x + '" cy="' + p.y + '" r="' + rh + '" class="sgc-asas sgc-hit" data-i="' + i + '"/>' +
                '<circle cx="' + p.x + '" cy="' + p.y + '" r="' + rv + '" class="sgc-punto' + (i === 0 ? ' sgc-punto-inicio' : '') +
                (i === estado.sel ? ' sgc-punto-sel' : '') + '"/>';
        }).join('');
        var poly = pts.length >= 2 ? '<path d="' + svgPath(pts) + '" class="sgc-forma' + (v.ok || pts.length < MIN_PUNTOS ? '' : ' sgc-forma-mal') + '"/>' : '';
        /* Diseno recortado por la pieza, con el trazo en curso encima; el contorno a mano se ve fuera del recorte */
        var capaDiseno = pts.length >= MIN_PUNTOS ?
            '<clipPath id="sgc-recorte"><path d="' + svgPath(pts) + '"/></clipPath><g clip-path="url(#sgc-recorte)">' +
            (estado.disenoUrl ? '<image href="' + estado.disenoUrl + '" x="0" y="0" width="' + LIENZO_W + '" height="' + LIENZO_H + '" opacity=".9"/>' : '') +
            '<path id="sgc-trazo" class="sgc-trazo' + (estado.modo === 'goma' ? ' sgc-trazo-goma' : '') + '" d="" stroke="' + esc(estado.color) + '" stroke-width="' + estado.grosor + '"/></g>' : '';
        var aMano = '<path id="sgc-a-mano" class="sgc-a-mano" d=""/>';
        var bb = m.bbox, cotas = '';
        if (pts.length >= 2 && bb.w > 0) {
            var yc = bb.maxY + 18 * u, xc = bb.maxX + 18 * u, fs = 12 * u;
            cotas =
                '<line x1="' + bb.minX + '" y1="' + yc + '" x2="' + bb.maxX + '" y2="' + yc + '" class="sgc-cota"/>' +
                '<text x="' + (bb.minX + bb.w / 2) + '" y="' + (yc + 15 * u) + '" class="sgc-cota-txt" font-size="' + fs + '">' + Math.round(bb.w) + ' cm</text>' +
                '<line x1="' + xc + '" y1="' + bb.minY + '" x2="' + xc + '" y2="' + bb.maxY + '" class="sgc-cota"/>' +
                '<text x="' + (xc + 15 * u) + '" y="' + (bb.minY + bb.h / 2) + '" class="sgc-cota-txt" font-size="' + fs + '" transform="rotate(90 ' + (xc + 15 * u) + ' ' + (bb.minY + bb.h / 2) + ')">' + Math.round(bb.h) + ' cm</text>';
        }
        var z = estado.vista.z, tx = LIENZO_W / 2 - estado.vista.cx * z, ty = LIENZO_H / 2 - estado.vista.cy * z;
        svg.innerHTML =
            '<g class="sgc-view" transform="translate(' + tx + ' ' + ty + ') scale(' + z + ')">' +
            '<rect x="0" y="0" width="' + LIENZO_W + '" height="' + LIENZO_H + '" class="sgc-fondo"/>' +
            rejilla + '<rect x="0" y="0" width="' + LIENZO_W + '" height="' + LIENZO_H + '" class="sgc-marco"/>' + etiquetas +
            poly + capaDiseno + cotas + (estado.modo === 'puntos' ? asas : '') + aMano + '</g>';
        svg.style.cursor = estado.modo === 'relleno' ? 'cell' : '';
        var zl = $('sgc-zval'); if (zl) zl.textContent = Math.round(z * 100) + ' %';
        pintarPanel(m, v);
        pintarPunto();
        pintarMedidas(bb);
        actualizarBotones(v);
        avisar3D();
    }

    function fmt(n, d) { return G.fmtNum(n, d || 0); }

    function pintarPanel(m, v) {
        function set(id, t) { var e = $(id); if (e) e.textContent = t; }
        set('sgc-m-area', fmt(m.areaM2, 2) + ' m²');
        set('sgc-m-perim', fmt(m.perimetro, 0) + ' cm');
        set('sgc-m-vert', String(m.vertices));
        set('sgc-m-peso', m.hayArea ? fmt(m.peso, 1) + ' kg' : '—');
        set('sgc-m-co2', m.hayArea ? fmt(m.co2, 1) + ' kg' : '—');
        var chip = $('sgc-m-nivel'), inten = $('sgc-m-int'), abs = $('sgc-m-absoluto');
        if (m.hayArea) {
            /* F24: el color es SOLO del proceso (intensidad); el absoluto de la pieza es neutro */
            if (chip) { chip.textContent = m.imp.etiqueta; chip.className = 'sgc-chip sgc-chip-' + m.imp.nivel; }
            if (inten) inten.textContent = fmt(m.intensidad, 1) + ' kg CO₂e/m²';
            if (abs) abs.textContent = T('tu_corte', '{0} kg CO₂e · tu corte', fmt(m.co2, 1));
        } else {
            if (chip) { chip.textContent = '—'; chip.className = 'sgc-chip'; }
            if (inten) inten.textContent = '—';
            if (abs) abs.textContent = '—';
        }
        var msg = $('sgc-msg');
        if (msg) { msg.textContent = v.ok ? '' : v.msg; msg.className = 'sgc-msg' + (v.ok ? '' : ' sgc-msg-mal'); }
    }

    /* Punto elegido: coordenadas exactas */
    function pintarPunto() {
        var i = estado.sel, p = estado.puntos[i], x = $('sgc-px'), y = $('sgc-py'), t = $('sgc-punto-t');
        if (!x || !y) return;
        var hay = !!p;
        x.disabled = !hay; y.disabled = !hay;
        if (document.activeElement !== x) x.value = hay ? num2(p.x) : '';
        if (document.activeElement !== y) y.value = hay ? num2(p.y) : '';
        if (t) t.textContent = hay ? T('punto_n', 'Punto {0} de {1}', i + 1, estado.puntos.length) : T('punto_ninguno', 'Toca un punto para editarlo con precisión.');
    }
    function pintarMedidas(bb) {
        var w = $('sgc-mw'), h = $('sgc-mh');
        if (!w || !h) return;
        if (document.activeElement !== w) w.value = estado.puntos.length >= 2 ? Math.round(bb.w) : '';
        if (document.activeElement !== h) h.value = estado.puntos.length >= 2 ? Math.round(bb.h) : '';
    }

    function actualizarBotones(v) {
        function dis(id, f) { var e = $(id); if (e) e.disabled = f; }
        var pocos = estado.puntos.length < MIN_PUNTOS;
        dis('sgc-confirmar', !(v && v.ok));
        dis('sgc-eliminar', estado.puntos.length <= MIN_PUNTOS);
        dis('sgc-undo', !estado.undo.length);
        dis('sgc-redo', !estado.redo.length);
        ['sgc-rotar', 'sgc-espejo-h', 'sgc-espejo-v', 'sgc-centrar', 'sgc-aplicar-medidas'].forEach(function (id) { dis(id, pocos); });
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-forma-btn'), function (b) {
            b.setAttribute('aria-pressed', String(b.getAttribute('data-forma') === estado.formaBase));
        });
        var suavidad = $('sgc-n-box'); if (suavidad) suavidad.hidden = estado.formaBase !== 'Circular';
        dis('sgc-borrar-diseno', !estado.diseno);
    }

    /* ------------------------------------------------------------------ historial y autoguardado */
    /* Cada paso guarda los puntos y una referencia al diseno de ese momento */
    function paso() { var e = cloneP(estado.puntos); e.diseno = estado.diseno; return e; }
    function restaurarPaso(e) {
        var cambioDiseno = e.diseno !== estado.diseno;
        estado.puntos = e; estado.diseno = e.diseno; estado.sel = -1;
        if (cambioDiseno) refrescarDiseno(); else pintarLienzo();
        autosave();
    }
    function pushUndo() { estado.undo.push(paso()); if (estado.undo.length > UNDO_MAX) estado.undo.shift(); estado.redo.length = 0; }
    function undo() { if (!estado.undo.length) return; estado.redo.push(paso()); restaurarPaso(estado.undo.pop()); }
    function redo() { if (!estado.redo.length) return; estado.undo.push(paso()); restaurarPaso(estado.redo.pop()); }

    var timerAutosave = null;
    function guardarYa() {
        clearTimeout(timerAutosave);
        if (estado.puntos.length < MIN_PUNTOS) return;
        try { localStorage.setItem(KEY_BORRADOR, JSON.stringify({ formaBase: estado.formaBase, puntos: estado.puntos, diseno: exportarDiseno(), t: Date.now() })); } catch (e) { /* sin almacenamiento: no pasa nada */ }
    }
    function autosave() { clearTimeout(timerAutosave); timerAutosave = setTimeout(guardarYa, 800); }
    function leerLS(k) { try { return JSON.parse(localStorage.getItem(k) || 'null'); } catch (e) { return null; } }
    function borrarLS(k) { try { localStorage.removeItem(k); } catch (e) { /* idem */ } }
    function guardarPrevio(pts) { try { localStorage.setItem(KEY_PREVIO, JSON.stringify({ puntos: pts, t: Date.now() })); } catch (e) { /* idem */ } }
    function puntosValidosLS(o) {
        return o && Array.isArray(o.puntos) && o.puntos.length >= MIN_PUNTOS && o.puntos.length <= MAX_PUNTOS &&
            o.puntos.every(function (p) { return p && isFinite(p.x) && isFinite(p.y); });
    }

    /* ------------------------------------------------------------------ edicion */
    function aplicarFormaBase(id) {
        var fb = formaBase(id); if (!fb) return;
        /* No se pierde lo que habia: va al historial (Ctrl+Z) y queda como "corte anterior" restaurable */
        if (estado.puntos.length >= MIN_PUNTOS) {
            estado.previo = cloneP(estado.puntos);
            guardarPrevio(estado.previo);
            pushUndo();
            var n = estado.previo.length;
            mostrarBanner(T('banner_cambio', 'Cambiaste de forma. Tu corte anterior tenía {0}.'), T('n_puntos', '{0} puntos', n), [
                { id: 'sgc-restaurar-previo', texto: T('restaurar', 'Restaurar mi corte anterior ({0} puntos)', n), fn: function () {
                    pushUndo(); estado.puntos = cloneP(estado.previo); estado.formaBase = 'Libre'; estado.sel = -1; encajar(); pintarLienzo(); autosave(); ocultarBanner();
                } },
                { id: 'sgc-descartar-previo', texto: T('descartar', 'Descartar'), ghost: true, fn: function () { estado.previo = null; borrarLS(KEY_PREVIO); ocultarBanner(); } }
            ]);
        } else {
            pushUndo();
        }
        estado.formaBase = id; estado.puntos = fb; estado.sel = -1;
        encajar(); pintarLienzo(); autosave();
    }

    function anadirPunto(p) {
        if (estado.puntos.length >= MAX_PUNTOS) return;
        var q = { x: clamp(snap(p.x), 0, LIENZO_W), y: clamp(snap(p.y), 0, LIENZO_H) };
        pushUndo();
        if (estado.puntos.length >= MIN_PUNTOS) {
            var i = aristaMasCercana(q, estado.puntos);          // se inserta en la arista mas cercana
            estado.puntos.splice(i + 1, 0, q); estado.sel = i + 1;
        } else {
            estado.puntos.push(q); estado.sel = estado.puntos.length - 1;
        }
        estado.formaBase = 'Libre';
        pintarLienzo(); autosave();
    }

    function eliminarVertice(i) {
        if (estado.puntos.length <= MIN_PUNTOS || i < 0 || i >= estado.puntos.length) return;
        pushUndo(); estado.puntos.splice(i, 1); estado.sel = -1; estado.formaBase = 'Libre'; pintarLienzo(); autosave();
    }
    function eliminarSeleccion() { eliminarVertice(estado.sel >= 0 ? estado.sel : estado.puntos.length - 1); }

    function moverSeleccion(dx, dy) {
        var p = estado.puntos[estado.sel]; if (!p) return;
        pushUndo();
        estado.puntos[estado.sel] = { x: clamp(p.x + dx, 0, LIENZO_W), y: clamp(p.y + dy, 0, LIENZO_H) };
        estado.formaBase = 'Libre'; pintarLienzo(); autosave();
    }

    /* Transformaciones de todo el corte (cada una es un paso de deshacer) */
    function transformar(fn) {
        if (estado.puntos.length < MIN_PUNTOS) return;
        pushUndo();
        var bb = bbox(estado.puntos), c = { x: bb.minX + bb.w / 2, y: bb.minY + bb.h / 2 };
        var pts = estado.puntos.map(function (p) { return fn(p, c, bb); });
        estado.puntos = sinRepetidos(snapAll(dentroDelLienzo(pts), 1));
        estado.sel = -1; estado.formaBase = 'Libre';
        encajar();
        if (estado.diseno) {
            // El diseno sigue a la pieza: fn es lineal, asi que tres puntos dan la matriz; lo demas es el ajuste al lienzo
            var o = fn({ x: 0, y: 0 }, c, bb), ex = fn({ x: 1, y: 0 }, c, bb), ey = fn({ x: 0, y: 1 }, c, bb);
            var crudo = bbox(pts), fin = bbox(estado.puntos), nuevo = copiaDiseno(null);
            nuevo.getContext('2d').setTransform(ex.x - o.x, ex.y - o.y, ey.x - o.x, ey.y - o.y,
                (o.x + fin.minX - crudo.minX) * PX_POR_CM, (o.y + fin.minY - crudo.minY) * PX_POR_CM);
            nuevo.getContext('2d').drawImage(estado.diseno, 0, 0);
            estado.diseno = nuevo;
            refrescarDiseno();
        } else {
            pintarLienzo();
        }
        autosave();
    }
    function rotar90() { transformar(function (p, c) { return { x: c.x - (p.y - c.y), y: c.y + (p.x - c.x) }; }); }
    function espejoH() { transformar(function (p, c) { return { x: 2 * c.x - p.x, y: p.y }; }); }
    function espejoV() { transformar(function (p, c) { return { x: p.x, y: 2 * c.y - p.y }; }); }
    function centrar() { transformar(function (p, c) { return { x: p.x + LIENZO_W / 2 - c.x, y: p.y + LIENZO_H / 2 - c.y }; }); }
    function ajustarMedidas() {
        var w = parseFloat(($('sgc-mw') || {}).value), h = parseFloat(($('sgc-mh') || {}).value);
        if (!isFinite(w) || !isFinite(h)) return;
        w = clamp(w, MIN_DIM, LIENZO_W); h = clamp(h, MIN_DIM, LIENZO_H);
        transformar(function (p, c, bb) {
            return { x: bb.minX + (bb.w ? (p.x - bb.minX) * w / bb.w : 0), y: bb.minY + (bb.h ? (p.y - bb.minY) * h / bb.h : 0) };
        });
    }
    function editarPunto() {
        var p = estado.puntos[estado.sel]; if (!p) return;
        var x = parseFloat($('sgc-px').value), y = parseFloat($('sgc-py').value);
        if (!isFinite(x) || !isFinite(y)) return;
        pushUndo();
        estado.puntos[estado.sel] = { x: clamp(x, 0, LIENZO_W), y: clamp(y, 0, LIENZO_H) };
        estado.formaBase = 'Libre'; pintarLienzo(); autosave();
    }

    /* R1: cambia material/proceso SIN mover ningun vertice */
    function aplicarRecomendado() {
        var m = metricas();
        var box = $('sgc-r1-cambios'); if (!box) return;
        if (!m.hayArea) { box.textContent = ''; var li0 = document.createElement('li'); li0.textContent = T('dibuja_primero', 'Dibuja tu corte primero.'); box.appendChild(li0); box.style.display = 'block'; return; }
        var s = Math.sqrt(m.areaCm2);
        var c = { ancho: s, alto: s, espesor: m.cfg.espesor, tipo: m.cfg.tipo, renov: m.cfg.renov, eco: m.cfg.eco, uso: m.cfg.uso };
        var objetivo = m.imp.nivel === 'alto' ? G.umbral.moderado : G.umbral.favorable;
        var rec = G.recomendar(c, objetivo);
        var cambios = rec.cambios || [];
        if (cambios.length) {
            var eco = $('opcionEco'), ren = $('energiaRenovable');
            if (eco) { eco.checked = !!rec.cfg.eco; dispararEventos(eco); }
            if (ren) { ren.checked = !!rec.cfg.renov; dispararEventos(ren); }
            if (parseFloat($('espesor').value) !== rec.cfg.espesor) setValor('espesor', rec.cfg.espesor);
            if ($('tipoVidrio').value !== rec.cfg.tipo) setValor('tipoVidrio', rec.cfg.tipo);
        }
        box.textContent = '';
        if (cambios.length) {
            cambios.forEach(function (x) { var li = document.createElement('li'); li.textContent = x.texto; box.appendChild(li); });
        } else {
            var li = document.createElement('li'); li.textContent = T('r1_nada', 'Tu corte ya está en perfil favorable o no hay ajustes seguros que aplicar.'); box.appendChild(li);
        }
        box.style.display = 'block';
        pintarLienzo();                       // refresca las metricas con el nuevo material; los puntos siguen igual
    }

    function confirmar() {
        var v = validez(); if (!v.ok) return;
        var bb = bbox(estado.puntos);
        guardarYa();                          // el borrador se conserva hasta que el envio tenga exito (lo limpia el detalle)
        var sel = $('forma');
        setValor('ancho', Math.round(bb.w));
        setValor('alto', Math.round(bb.h));
        setValor('puntosTexto', aTexto(estado.puntos));
        var png = $('disenoPng'); if (png) png.value = exportarDiseno();
        if (sel) { sel.value = 'Personalizada'; dispararEventos(sel); }
        cerrar(true);
    }

    /* ------------------------------------------------------------------ banner */
    function ocultarBanner() { var b = $('sgc-banner'); if (b) { b.style.display = 'none'; b.textContent = ''; } }
    /* plantilla con {0}: ese hueco va en negrita (todo por textContent, sin HTML) */
    function mostrarBanner(plantilla, negrita, acciones) {
        var b = $('sgc-banner'); if (!b) return;
        b.textContent = '';
        var t = document.createElement('span'), partes = plantilla.split('{0}');
        t.appendChild(document.createTextNode(partes[0]));
        if (partes.length > 1) { var bb = document.createElement('b'); bb.textContent = negrita; t.appendChild(bb); t.appendChild(document.createTextNode(partes.slice(1).join(''))); }
        b.appendChild(t);
        var grupo = document.createElement('span'); grupo.className = 'sgc-banner-acc';
        acciones.forEach(function (a) {
            var bt = document.createElement('button'); bt.type = 'button'; bt.id = a.id; bt.textContent = a.texto;
            bt.className = 'sgc-btn sgc-btn-mini' + (a.ghost ? ' sgc-btn-ghost' : '');
            bt.addEventListener('click', a.fn); grupo.appendChild(bt);
        });
        b.appendChild(grupo); b.style.display = 'flex';
    }

    /* ------------------------------------------------------------------ abrir / cerrar */
    /* opciones (opcional): { forma, ancho, alto } de la pieza que ya estaba en el configurador */
    function abrir(opener, opciones) {
        if (!G || !G.areaPoligono || !G.poligonoForma) { window.alert(T('falta_gp', 'El editor de corte necesita glass-preview.js actualizado.')); return; }
        if (!opciones && typeof api.plantilla === 'function') { try { opciones = api.plantilla(); } catch (e) { opciones = null; } }
        inyectarModal();
        estado.opener = opener || document.activeElement;
        ocultarBanner();
        var o = opciones || {};
        estado.base = { w: parseFloat(o.ancho) > 0 ? parseFloat(o.ancho) : 120, h: parseFloat(o.alto) > 0 ? parseFloat(o.alto) : 120 };
        var conf = puntosConfirmados(), bor = leerLS(KEY_BORRADOR);
        estado.diseno = null;
        if (conf.length) {
            estado.puntos = cloneP(conf); estado.formaBase = 'Libre';
            var guardado = $('disenoPng');
            if (guardado && guardado.value) cargarDiseno(guardado.value, conf);
        } else {
            var id = NOMBRES[o.forma] && o.forma !== 'Libre' ? o.forma : 'Rectangular';
            estado.puntos = formaBase(id); estado.formaBase = id;
            if (puntosValidosLS(bor)) {
                var n = bor.puntos.length;
                mostrarBanner(T('borrador', 'Tenías un corte de {0} sin guardar.'), T('n_puntos', '{0} puntos', n), [
                    { id: 'sgc-restaurar', texto: T('restaurar', 'Restaurar mi corte anterior ({0} puntos)', n), fn: function () {
                        pushUndo(); estado.puntos = cloneP(bor.puntos); estado.formaBase = NOMBRES[bor.formaBase] ? bor.formaBase : 'Libre'; estado.sel = -1; encajar(); pintarLienzo(); ocultarBanner();
                        if (typeof bor.diseno === 'string') cargarDiseno(bor.diseno, estado.puntos);
                    } },
                    { id: 'sgc-descartar', texto: T('empezar_cero', 'Empezar de cero'), ghost: true, fn: function () { borrarLS(KEY_BORRADOR); ocultarBanner(); } }
                ]);
            }
        }
        estado.undo.length = 0; estado.redo.length = 0; estado.sel = -1; estado.abierto = true;
        var r1 = $('sgc-r1-cambios'); if (r1) { r1.style.display = 'none'; r1.textContent = ''; }
        $('sgc-modal').style.display = 'flex';
        document.body.classList.add('sgc-bloquear-scroll');
        encajar();
        cambiarModo('puntos');
        refrescarDiseno();
        requestAnimationFrame(function () { pintarLienzo(); });   // con el modal ya medido, los puntos toman su tamano en pantalla
        var x = $('sgc-cerrar'); if (x) x.focus();
        if (window.SGCorte3D) { window.SGCorte3D.abrir($('sgc-3d')); avisar3D(); pintarGiro(); }
    }
    function pintarGiro() {
        var b = $('sgc-3d-giro'); if (!b) return;
        // Sin animaciones no hay giro automatico que pausar; arrastrar sigue funcionando
        b.hidden = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        b.textContent = T(window.SGCorte3D.girando() ? 'giro_pausar' : 'giro_seguir', window.SGCorte3D.girando() ? 'Pausar giro' : 'Girar');
    }

    function cerrar(confirmado) {
        if (!estado.abierto) return;
        estado.abierto = false;
        estado.trazo = null;
        if (window.SGCorte3D) window.SGCorte3D.cerrar();
        $('sgc-modal').style.display = 'none';
        document.body.classList.remove('sgc-bloquear-scroll');
        document.dispatchEvent(new CustomEvent('sgcorte:cerrado', { detail: { confirmado: confirmado === true } }));
        if (estado.opener && estado.opener.focus) { try { estado.opener.focus(); } catch (e) { /* idem */ } }
    }

    /* ------------------------------------------------------------------ modal */
    function inyectarModal() {
        if ($('sgc-modal')) return;
        var botones = ORDEN_FORMAS.map(function (k) {
            return '<button type="button" class="sgc-forma-btn" data-forma="' + k + '" aria-pressed="false">' + iconoForma(k) + '<span>' + esc(nombreForma(k)) + '</span></button>';
        }).join('');
        var NOMBRES_MODO = { puntos: 'Puntos', contorno: 'Dibujar contorno', pincel: 'Pintar', relleno: 'Rellenar', goma: 'Borrar' };
        var botonesModo = MODOS.map(function (m) {
            return '<button type="button" class="sgc-modo-btn" data-modo="' + m.id + '" aria-pressed="false"><i class="ph ' + m.icono + '" aria-hidden="true"></i><span>' + esc(T('modo_' + m.id, NOMBRES_MODO[m.id])) + '</span></button>';
        }).join('');
        var botonesColor = COLORES.map(function (c) {
            return '<button type="button" class="sgc-color" data-color="' + c + '" style="background:' + c + '" aria-label="' + c + '"></button>';
        }).join('');
        var html =
            '<div id="sgc-modal" class="sgc-modal" role="dialog" aria-modal="true" aria-labelledby="sgc-titulo">' +
            '<div class="sgc-modal-inner">' +
            /* Nov. 2026: la cabecera cuenta los tres pasos; las herramientas van en una barra sobre el lienzo */
            '<header class="sgc-modal-head"><div><span class="sgc-kicker">' + esc(T('kicker', 'Corte personalizado')) + '</span><h2 id="sgc-titulo">' + esc(T('titulo', 'Dibuja tu pieza')) + '</h2></div>' +
            '<ol class="sgc-guia"><li><b>1</b>' + esc(T('guia1', 'Elige una forma')) + '</li><li><b>2</b>' + esc(T('guia2', 'Ajusta, dibuja o pinta')) + '</li><li><b>3</b>' + esc(T('guia3', 'Confirma tu corte')) + '</li></ol>' +
            '<button type="button" id="sgc-cerrar" class="sgc-x" aria-label="' + esc(T('cerrar', 'Cerrar')) + '"><i class="ph ph-x" aria-hidden="true"></i></button></header>' +
            '<div id="sgc-banner" class="sgc-banner" role="status"></div>' +
            '<div class="sgc-body">' +
            '<aside class="sgc-tools">' +
            '<div id="sgc-pintura" class="sgc-pintura" hidden>' +
            '<div id="sgc-colores"><div class="sgc-colores">' + botonesColor + '<input type="color" id="sgc-color" value="' + COLORES[0] + '" aria-label="' + esc(T('otro_color', 'Otro color')) + '"></div></div>' +
            '<label class="sgc-n" id="sgc-grosor-box"><span>' + esc(T('grosor', 'Grosor:')) + ' <b id="sgc-grosor-val">6</b> cm</span><input type="range" id="sgc-grosor" min="1" max="40" step="1" value="6"></label>' +
            '<button type="button" id="sgc-borrar-diseno" class="sgc-btn sgc-btn-ghost sgc-btn-full">' + esc(T('borrar_diseno', 'Borrar todo el diseño')) + '</button></div>' +
            '<p class="sgc-tools-t">' + esc(T('forma_base', 'Forma base')) + '</p>' +
            '<div id="sgc-n-box" class="sgc-n" hidden><label for="sgc-n"><span>' + esc(T('lados', 'Suavidad de la curva:')) + ' <b id="sgc-n-val">32</b> ' + esc(T('tramos', 'tramos')) + '</span></label>' +
            '<input type="range" id="sgc-n" min="8" max="64" step="1" value="32" aria-describedby="sgc-n-ayuda" aria-label="' + esc(T('lados_aria', 'Cantidad de tramos rectos que forman el círculo')) + '">' +
            '<div class="sgc-n-escala" aria-hidden="true"><span>' + esc(T('lados_min', 'Facetado')) + '</span><span>' + esc(T('lados_max', 'Redondo')) + '</span></div>' +
            '<p id="sgc-n-ayuda" class="sgc-hint">' + esc(T('lados_ayuda', 'El círculo se corta como muchos tramos rectos muy cortos. Con 8 parece un octágono y con 64 se ve redondo. Menos tramos también dan algo menos de área: con 8 es cerca de un 10 % menos que con 64.')) + '</p></div>' +
            '<div class="sgc-formas">' + botones + '</div>' +
            '<p class="sgc-tools-t">' + esc(T('transformar', 'Transformar')) + '</p>' +
            /* botones de solo icono: el nombre va en aria-label y title */
            '<div class="sgc-grid-btn">' + [['sgc-rotar', 'ph-arrow-clockwise', T('rotar', 'Girar 90°')], ['sgc-centrar', 'ph-crosshair-simple', T('centrar', 'Centrar')],
                ['sgc-espejo-h', 'ph-flip-horizontal', T('espejo_h', 'Reflejar') + ' ↔'], ['sgc-espejo-v', 'ph-flip-vertical', T('espejo_v', 'Reflejar') + ' ↕']].map(function (b) {
                return '<button type="button" id="' + b[0] + '" class="sgc-btn" title="' + esc(b[2]) + '" aria-label="' + esc(b[2]) + '"><i class="ph ' + b[1] + '" aria-hidden="true"></i></button>';
            }).join('') + '</div>' +
            '<div class="sgc-row"><button type="button" id="sgc-eliminar" class="sgc-btn sgc-btn-warn">' + esc(T('quitar', 'Quitar punto')) + '</button><button type="button" id="sgc-reset" class="sgc-btn sgc-btn-ghost">' + esc(T('reiniciar', 'Reiniciar')) + '</button></div>' +
            '<label class="sgc-campo sgc-snap"><span>' + esc(T('rejilla', 'Imán de la rejilla')) + '</span><select id="sgc-snap">' +
            '<option value="1">1 cm</option><option value="5">5 cm</option><option value="10">10 cm</option></select></label>' +
            '<details class="sgc-ayuda"><summary>' + esc(T('como_se_usa', 'Cómo se usa')) + '</summary><p class="sgc-hint">' + esc(T('ayuda', 'Toca o haz clic en el lienzo para añadir un punto (se inserta en la línea más cercana). Arrastra un punto para moverlo y el fondo para desplazar la vista; doble clic sobre un punto lo elimina. Con un punto elegido, las flechas lo mueven (Mayús: 10 cm). Mínimo {0}, máximo {1}.', MIN_PUNTOS, MAX_PUNTOS)) + '</p></details>' +
            '<button type="button" id="sgc-r1" class="sgc-btn sgc-btn-eco">✦ ' + esc(T('balanceada', 'Versión balanceada (respeta tu forma)')) + '</button><ul id="sgc-r1-cambios" class="sgc-r1-cambios"></ul></aside>' +
            '<main class="sgc-canvas-wrap">' +
            '<div class="sgc-barra" role="toolbar" aria-label="' + esc(T('herramienta', 'Herramienta')) + '">' + botonesModo + '<span class="sgc-barra-sep" aria-hidden="true"></span>' +
            '<button type="button" id="sgc-undo" class="sgc-btn" title="' + esc(T('deshacer_t', 'Deshacer (Ctrl+Z)')) + '"><i class="ph ph-arrow-counter-clockwise" aria-hidden="true"></i><span>' + esc(T('deshacer', 'Deshacer')) + '</span></button>' +
            '<button type="button" id="sgc-redo" class="sgc-btn" title="' + esc(T('rehacer_t', 'Rehacer (Ctrl+Y)')) + '"><i class="ph ph-arrow-clockwise" aria-hidden="true"></i><span>' + esc(T('rehacer', 'Rehacer')) + '</span></button></div>' +
            '<p id="sgc-modo-ayuda" class="sgc-hint sgc-modo-ayuda" aria-live="polite"></p>' +
            '<div class="sgc-lienzo-caja"><svg id="sgc-lienzo" viewBox="0 0 ' + LIENZO_W + ' ' + LIENZO_H + '" preserveAspectRatio="xMidYMid meet" role="img" aria-label="' + esc(T('lienzo_aria', 'Lienzo de dibujo del corte')) + '"></svg>' +
            '<div class="sgc-zoom"><button type="button" id="sgc-zout" class="sgc-btn sgc-btn-mini" aria-label="' + esc(T('alejar', 'Alejar')) + '"><i class="ph ph-minus" aria-hidden="true"></i></button>' +
            '<span id="sgc-zval" aria-live="polite">100 %</span>' +
            '<button type="button" id="sgc-zin" class="sgc-btn sgc-btn-mini" aria-label="' + esc(T('acercar', 'Acercar')) + '"><i class="ph ph-plus" aria-hidden="true"></i></button>' +
            '<button type="button" id="sgc-zfit" class="sgc-btn sgc-btn-mini"><i class="ph ph-corners-out" aria-hidden="true"></i><span>' + esc(T('encajar', 'Encajar')) + '</span></button></div></div></main>' +
            '<aside class="sgc-metrics"><p class="sgc-tools-t">' + esc(T('titulo_3d', 'Así va quedando')) + '</p>' +
            '<div id="sgc-3d" class="sgc-3d"><canvas tabindex="0" role="img" aria-label="' + esc(T('aria_3d', 'Vista 3D de la pieza. Arrastra o usa las flechas para girarla.')) + '"></canvas>' +
            '<p class="sgc-3d-aviso" hidden>' + esc(T('sin_3d', 'Este navegador no pudo mostrar la vista 3D. El corte funciona igual.')) + '</p>' +
            '<button type="button" id="sgc-3d-giro" class="sgc-btn sgc-btn-mini sgc-3d-giro"></button></div>' +
            '<p class="sgc-note sgc-3d-nota">' + esc(T('nota_3d', 'Arrastra para girar y usa la rueda para acercar. En piezas grandes el espesor se ve exagerado.')) + '</p>' +
            '<p class="sgc-tools-t sgc-sep">' + esc(T('en_vivo', 'Tu corte, en vivo')) + '</p>' +
            '<div class="sgc-metric"><span>' + esc(T('area', 'Área del corte')) + '</span><b id="sgc-m-area">0 m²</b></div>' +
            '<div class="sgc-metric"><span>' + esc(T('perimetro', 'Perímetro')) + '</span><b id="sgc-m-perim">0 cm</b></div>' +
            '<div class="sgc-metric"><span>' + esc(T('vertices', 'Vértices')) + '</span><b id="sgc-m-vert">0</b></div>' +
            '<div class="sgc-metric"><span>' + esc(T('peso', 'Peso')) + '</span><b id="sgc-m-peso">—</b></div>' +
            '<div class="sgc-metric"><span>' + esc(T('co2', 'CO₂e de tu pieza')) + '</span><b id="sgc-m-co2">—</b></div>' +
            '<div class="sgc-proceso"><span class="sgc-tools-t">' + esc(T('proceso', 'Proceso · intensidad')) + '</span><span class="sgc-chip" id="sgc-m-nivel">—</span><small id="sgc-m-int">—</small></div>' +
            '<div class="sgc-absoluto"><small id="sgc-m-absoluto">—</small></div>' +
            '<p class="sgc-tools-t sgc-sep">' + esc(T('medidas', 'Medidas exactas')) + '</p>' +
            '<div class="sgc-fila2"><label class="sgc-campo"><span>' + esc(T('ancho', 'Ancho (cm)')) + '</span><input type="number" id="sgc-mw" min="' + MIN_DIM + '" max="' + LIENZO_W + '" step="1" inputmode="decimal"></label>' +
            '<label class="sgc-campo"><span>' + esc(T('alto', 'Alto (cm)')) + '</span><input type="number" id="sgc-mh" min="' + MIN_DIM + '" max="' + LIENZO_H + '" step="1" inputmode="decimal"></label></div>' +
            '<button type="button" id="sgc-aplicar-medidas" class="sgc-btn sgc-btn-full">' + esc(T('aplicar_medidas', 'Ajustar a estas medidas')) + '</button>' +
            '<p class="sgc-tools-t sgc-sep">' + esc(T('punto', 'Punto elegido')) + '</p><p class="sgc-punto-t" id="sgc-punto-t"></p>' +
            '<div class="sgc-fila2"><label class="sgc-campo"><span>X (cm)</span><input type="number" id="sgc-px" min="0" max="' + LIENZO_W + '" step="1" inputmode="decimal"></label>' +
            '<label class="sgc-campo"><span>Y (cm)</span><input type="number" id="sgc-py" min="0" max="' + LIENZO_H + '" step="1" inputmode="decimal"></label></div>' +
            '<p class="sgc-note">' + esc(T('nota', 'El color es del proceso (intensidad). Tu total es un dato neutro. El valor que se guarda lo calcula el servidor con tu corte real.')) + '</p></aside>' +
            '</div>' +
            '<footer class="sgc-modal-foot"><div class="sgc-foot-txt"><span class="sgc-foot-hint">' + esc(T('progreso', 'Tu progreso se guarda solo; si cambias de forma o recargas, podrás restaurarlo.')) + '</span><div id="sgc-msg" class="sgc-msg" role="alert" aria-live="polite"></div></div>' +
            '<div class="sgc-foot-actions"><button type="button" id="sgc-cancelar" class="sgc-btn sgc-btn-ghost">' + esc(T('cancelar', 'Cancelar')) + '</button><button type="button" id="sgc-confirmar" class="sgc-btn sgc-btn-principal">' + esc(T('confirmar', 'Confirmar corte')) + '</button></div></footer>' +
            '</div></div>';
        var d = document.createElement('div'); d.innerHTML = html; document.body.appendChild(d.firstChild);

        var st = document.createElement('style'); st.id = 'sgc-css';
        st.textContent =
            '.sgc-modal{display:none;position:fixed;inset:0;z-index:9999;background:rgba(14,19,27,.62);backdrop-filter:blur(4px);align-items:center;justify-content:center;padding:12px}' +
            '.sgc-modal-inner{width:min(1480px,100%);height:min(96vh,1040px);height:min(96dvh,1040px);display:flex;flex-direction:column;background:var(--bg-secondary);border:1px solid var(--border-color);border-radius:18px;overflow:hidden;color:var(--text-main);box-shadow:0 30px 80px rgba(0,0,0,.45)}' +
            '.sgc-modal-head{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 20px;border-bottom:1px solid var(--border-color)}' +
            '.sgc-kicker{font-size:max(12px,.7rem);font-weight:800;letter-spacing:.12em;text-transform:uppercase;color:var(--color-accent-text)}' +
            '.sgc-modal-head h2{margin:2px 0 0;font-size:1.3rem;font-weight:800;letter-spacing:-.02em}' +
            '.sgc-x{display:grid;place-items:center;width:44px;height:44px;flex:none;border:1px solid var(--border-color);border-radius:12px;background:none;color:var(--text-muted);font-size:1.2rem;cursor:pointer}' +
            '.sgc-x:hover{color:var(--text-main);border-color:var(--border-hover)}' +
            '.sgc-banner{display:none;gap:10px;align-items:center;justify-content:space-between;flex-wrap:wrap;padding:10px 20px;background:var(--color-warning-dim);border-bottom:1px solid var(--color-warning);font-size:.88rem}' +
            '.sgc-banner-acc{display:flex;gap:8px;flex-wrap:wrap}' +
            '.sgc-body{flex:1;display:grid;grid-template-columns:276px minmax(0,1fr) 300px;min-height:0;overflow:hidden}' +
            /* Cabecera con los tres pasos */
            '.sgc-guia{display:flex;gap:6px;margin:0 auto 0 24px;padding:0;list-style:none;font-size:max(12px,.78rem);font-weight:700;color:var(--text-muted)}' +
            '.sgc-guia li{display:flex;align-items:center;gap:6px;padding:5px 10px 5px 5px;border:1px solid var(--border-color);border-radius:999px;background:var(--bg-tertiary)}' +
            '.sgc-guia b{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;background:var(--color-accent-dim);color:var(--color-accent-text);font-size:11px}' +
            /* Barra de herramientas sobre el lienzo */
            '.sgc-barra{align-self:center;display:flex;flex-wrap:wrap;justify-content:center;align-items:center;gap:3px;margin:0 0 6px;padding:4px;border:1px solid var(--border-color);border-radius:14px;background:var(--bg-secondary);box-shadow:0 8px 24px -16px rgba(0,0,0,.5)}' +
            '.sgc-modo-btn,.sgc-barra .sgc-btn{display:inline-flex;align-items:center;gap:6px;min-height:40px;padding:6px 11px;border:1px solid transparent;border-radius:10px;background:transparent;color:var(--text-main);font:inherit;font-size:max(12px,.8rem);font-weight:700;line-height:1.1;cursor:pointer}' +
            '.sgc-modo-btn i,.sgc-barra .sgc-btn i{font-size:18px;color:var(--color-accent);flex:none}.sgc-modo-btn:hover,.sgc-barra .sgc-btn:hover:not(:disabled){background:var(--bg-tertiary)}' +
            '.sgc-modo-btn[aria-pressed="true"]{border-color:var(--color-accent);background:var(--color-accent-dim)}' +
            '.sgc-modo-btn:focus-visible,.sgc-color:focus-visible{outline:2px solid var(--color-accent);outline-offset:2px}' +
            '.sgc-barra-sep{width:1px;align-self:stretch;margin:4px 3px;background:var(--border-color)}' +
            '.sgc-modo-ayuda{align-self:center;max-width:62ch;margin:0 0 6px;text-align:center}.sgc-modo-ayuda:empty{display:none}' +
            '.sgc-ayuda{margin:4px 0 10px;font-size:max(12px,.8rem)}.sgc-ayuda summary{cursor:pointer;font-weight:700;color:var(--color-accent-text);min-height:32px;display:flex;align-items:center}.sgc-ayuda .sgc-hint{margin:4px 0 0}' +
            '.sgc-pintura{margin-bottom:14px;padding:10px;border:1px solid var(--border-color);border-radius:10px}.sgc-pintura[hidden]{display:none}' +
            '.sgc-colores{display:grid;grid-template-columns:repeat(5,1fr);gap:6px;margin-bottom:10px}' +
            '.sgc-color{height:30px;border:1px solid var(--border-strong);border-radius:8px;cursor:pointer}.sgc-color[aria-pressed="true"]{outline:2px solid var(--text-main);outline-offset:1px}' +
            '#sgc-color{width:100%;height:30px;padding:0;border:1px solid var(--border-strong);border-radius:8px;background:none;cursor:pointer}' +
            '.sgc-n[hidden],#sgc-colores[hidden]{display:none}.sgc-n-escala{display:flex;justify-content:space-between;font-size:max(12px,.72rem)}.sgc-n .sgc-hint{margin:6px 0 0}' +
            '.sgc-trazo{fill:none;stroke-linecap:round;stroke-linejoin:round;opacity:.9;pointer-events:none}.sgc-trazo-goma{stroke:var(--bg-secondary)}' +
            '.sgc-a-mano{fill:none;stroke:var(--color-warning);stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round;vector-effect:non-scaling-stroke;pointer-events:none}' +
            '.sgc-3d{position:relative;height:230px;overflow:hidden;border:1px solid var(--border-color);border-radius:12px;background:radial-gradient(ellipse at 50% 42%,#2B4464 0%,#1A2432 72%)}' +
            '.sgc-3d canvas{display:block;width:100%;height:100%;cursor:grab;touch-action:none}.sgc-3d canvas:active{cursor:grabbing}.sgc-3d canvas:focus-visible{outline:2px solid var(--color-accent);outline-offset:-2px}' +
            '.sgc-3d-giro{position:absolute;right:8px;bottom:8px;background:rgba(14,20,32,.82);color:#EAF6FF;border-color:rgba(255,255,255,.2)}.sgc-3d-giro[hidden]{display:none}' +
            '.sgc-3d-aviso{position:absolute;inset:0;display:grid;place-items:center;margin:0;padding:16px;text-align:center;font-size:.84rem;color:#C7D6E8}.sgc-3d-aviso[hidden]{display:none}' +
            '.sgc-3d-nota{margin:6px 0 0}' +
            '.sgc-tools,.sgc-metrics{padding:14px 16px;overflow:auto;border-right:1px solid var(--border-color);scrollbar-width:thin}' +
            '.sgc-metrics{border-right:0;border-left:1px solid var(--border-color)}' +
            '.sgc-tools-t{margin:0 0 8px;font-size:max(12px,.7rem);font-weight:800;letter-spacing:.08em;text-transform:uppercase;color:var(--text-muted)}' +
            '.sgc-sep{margin-top:18px}' +
            '.sgc-formas{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px;margin-bottom:12px}.sgc-forma-btn{min-width:0}.sgc-forma-btn span{min-width:0;hyphens:auto}' +
            '.sgc-btn-eco{white-space:normal;text-align:left;line-height:1.3}' +
            '.sgc-forma-btn{display:flex;align-items:center;gap:8px;min-height:44px;padding:6px 8px;text-align:left;border:1px solid var(--border-color);border-radius:10px;background:var(--bg-tertiary);color:var(--text-main);font:inherit;font-size:max(12px,.76rem);font-weight:600;line-height:1.2;cursor:pointer}' +
            '.sgc-forma-btn svg{flex:none;width:20px;height:16px;fill:var(--color-accent-dim);stroke:var(--color-accent);stroke-width:1.4;stroke-linejoin:round}' +
            '.sgc-forma-btn i{font-size:20px;color:var(--color-accent)}' +
            '.sgc-forma-btn:hover{border-color:var(--color-accent)}' +
            '.sgc-forma-btn[aria-pressed="true"]{border-color:var(--color-accent);box-shadow:0 0 0 2px var(--color-accent-dim)}' +
            '.sgc-n{display:block;margin:0 0 14px;font-size:max(12px,.78rem);color:var(--text-muted)}.sgc-n input{width:100%}' +
            '.sgc-row{display:flex;gap:6px;margin-bottom:8px}.sgc-row .sgc-btn{flex:1}' +
            '.sgc-grid-btn{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-bottom:8px}.sgc-grid-btn .sgc-btn{min-height:40px;padding:6px}.sgc-grid-btn .sgc-btn i{font-size:18px}' +
            '.sgc-btn{display:inline-flex;align-items:center;justify-content:center;gap:6px;min-height:38px;padding:7px 10px;border:1px solid var(--border-color);border-radius:10px;background:var(--bg-tertiary);color:var(--text-main);font:inherit;font-size:max(12px,.8rem);font-weight:700;cursor:pointer}' +
            '.sgc-btn:hover:not(:disabled){border-color:var(--border-hover)}' +
            '.sgc-btn:disabled{opacity:.4;cursor:not-allowed}' +
            '.sgc-btn:focus-visible,.sgc-forma-btn:focus-visible,.sgc-x:focus-visible{outline:2px solid var(--color-accent);outline-offset:2px}' +
            '.sgc-btn-warn{border-color:var(--color-warning);color:var(--color-warning)}' +
            '.sgc-btn-eco{width:100%;margin-top:6px;border-color:var(--color-success);color:var(--color-success);background:var(--color-success-dim)}' +
            '.sgc-btn-ghost{background:none}' +
            '.sgc-btn-full{width:100%}' +
            '.sgc-btn-principal{background:var(--color-accent-solid);color:var(--on-accent-solid);border-color:var(--color-accent-solid);padding-inline:18px}' +
            '.sgc-btn-mini{min-height:34px;padding:4px 10px;font-size:max(12px,.78rem)}' +
            '.sgc-campo{display:flex;flex-direction:column;gap:4px;margin-bottom:8px;font-size:max(12px,.76rem);color:var(--text-muted);font-weight:700}' +
            '.sgc-campo input,.sgc-campo select{width:100%;min-height:38px;padding:6px 9px;border:1px solid var(--border-color);border-radius:9px;background:var(--bg-tertiary);color:var(--text-main);font:inherit;font-size:.9rem;font-variant-numeric:tabular-nums}' +
            '.sgc-campo input:disabled{opacity:.45}' +
            '.sgc-fila2{display:grid;grid-template-columns:1fr 1fr;gap:8px}' +
            '.sgc-punto-t{margin:-2px 0 8px;font-size:max(12px,.78rem);color:var(--text-muted)}' +
            '.sgc-hint{font-size:max(12px,.76rem);color:var(--text-muted);line-height:1.5;margin:10px 0}' +
            '.sgc-r1-cambios{display:none;margin:8px 0 0;padding:8px 8px 8px 22px;font-size:max(12px,.78rem);color:var(--text-muted);line-height:1.5;border:1px dashed var(--border-color);border-radius:8px}' +
            '.sgc-canvas-wrap{position:relative;display:flex;flex-direction:column;align-items:stretch;background:var(--bg-primary);min-height:0;min-width:0;padding:10px;overflow:hidden}' +
            '.sgc-lienzo-caja{position:relative;flex:1;min-height:0;display:flex}' +
            '#sgc-lienzo{display:block;width:100%;height:100%;touch-action:none;cursor:crosshair;user-select:none;-webkit-user-select:none}' +
            '.sgc-fondo{fill:var(--bg-secondary)}' +
            '.sgc-grid{stroke:var(--border-color);stroke-width:1;opacity:.45;vector-effect:non-scaling-stroke}' +
            '.sgc-grid-50{opacity:.9}' +
            '.sgc-marco{fill:none;stroke:var(--border-hover);stroke-width:1.5;vector-effect:non-scaling-stroke}' +
            '.sgc-regla{fill:var(--text-muted);font-weight:700;text-anchor:middle;opacity:.8}.sgc-regla-y{text-anchor:start}' +
            '.sgc-forma{fill:var(--color-accent);fill-opacity:.14;stroke:var(--color-accent);stroke-width:2;stroke-linejoin:round;vector-effect:non-scaling-stroke}' +
            '.sgc-forma-mal{fill:var(--color-error);fill-opacity:.1;stroke:var(--color-error)}' +
            '.sgc-hit{fill:transparent;cursor:grab}' +
            '.sgc-punto{fill:var(--bg-secondary);stroke:var(--color-accent);stroke-width:2;pointer-events:none;vector-effect:non-scaling-stroke}' +
            '.sgc-punto-inicio{fill:var(--color-success);stroke:var(--color-success)}' +
            '.sgc-punto-sel{stroke:var(--color-warning);stroke-width:3.5}' +
            '.sgc-cota{stroke:var(--text-muted);stroke-width:1;opacity:.85;vector-effect:non-scaling-stroke}' +
            '.sgc-cota-txt{fill:var(--text-muted);font-weight:700;text-anchor:middle}' +
            '.sgc-zoom{position:absolute;right:14px;bottom:14px;display:flex;gap:6px;align-items:center;padding:4px;border:1px solid var(--border-color);border-radius:12px;background:color-mix(in srgb,var(--bg-secondary) 88%,transparent);font-size:max(12px,.76rem);font-weight:700;color:var(--text-muted);font-variant-numeric:tabular-nums}' +
            '.sgc-zoom span{min-width:46px;text-align:center}' +
            '.sgc-metric{display:flex;justify-content:space-between;align-items:baseline;gap:10px;padding:7px 0;border-bottom:1px solid var(--border-color);font-size:.86rem}' +
            '.sgc-metric span{color:var(--text-muted)}.sgc-metric b{font-variant-numeric:tabular-nums;text-align:right}' +
            '.sgc-proceso{margin-top:12px;display:flex;flex-direction:column;gap:4px;align-items:flex-start}' +
            '.sgc-proceso small{font-size:max(12px,.8rem)}' +
            '.sgc-chip{padding:3px 9px;border-radius:999px;font-size:max(12px,.72rem);font-weight:800;letter-spacing:.04em;text-transform:uppercase;border:1px solid var(--border-color)}' +
            '.sgc-chip-favorable{background:var(--color-success-dim);color:var(--color-success);border-color:var(--color-success)}' +
            '.sgc-chip-moderado{background:var(--color-warning-dim);color:var(--color-warning);border-color:var(--color-warning)}' +
            '.sgc-chip-alto{background:var(--color-error-dim);color:var(--color-error);border-color:var(--color-error)}' +
            '.sgc-absoluto{margin-top:8px}.sgc-absoluto small{color:var(--text-main);font-weight:700;font-size:.92rem}' +
            '.sgc-note{margin-top:14px;font-size:max(12px,.74rem);color:var(--text-muted);line-height:1.5}' +
            '.sgc-modal-foot{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:12px 20px;border-top:1px solid var(--border-color);background:var(--bg-secondary)}' +
            '.sgc-foot-txt{min-width:0}.sgc-foot-hint{font-size:max(12px,.76rem);color:var(--text-muted)}.sgc-foot-actions{display:flex;gap:8px;flex:none}' +
            '.sgc-msg{min-height:1.1em;margin-top:4px;font-size:.84rem}.sgc-msg-mal{color:var(--color-error);font-weight:700}' +
            '.sgc-bloquear-scroll{overflow:hidden}' +
            '@media (pointer:coarse){.sgc-btn,.sgc-campo input,.sgc-campo select{min-height:44px}.sgc-btn-mini{min-height:44px;min-width:44px}.sgc-forma-btn{min-height:62px}}' +
            '@media (max-width:1180px){.sgc-body{grid-template-columns:228px minmax(0,1fr) 256px}.sgc-3d{height:190px}.sgc-guia{display:none}' +
            /* columna mas angosta: el icono va encima del nombre para no partir palabras */
            '.sgc-forma-btn{flex-direction:column;align-items:flex-start;gap:3px}}' +
            /* poco ancho para el lienzo: deshacer y rehacer quedan solo con icono (el nombre sigue para lectores) */
            '@media (max-width:1340px){.sgc-barra .sgc-btn span{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}}' +
            '@media (max-width:980px){.sgc-modal{padding:0}.sgc-modal-inner{height:100vh;height:100dvh;border-radius:0;border:0}' +
            '.sgc-body{position:relative;display:flex;flex-direction:column;overflow:auto}' +
            /* En celular el 3D flota sobre el lienzo: asi se ve como va quedando mientras se dibuja */
            /* abajo a la izquierda del lienzo: arriba esta la barra de herramientas y a la derecha el zoom */
            '.sgc-guia{display:none}' +
            '.sgc-barra{flex-wrap:nowrap;max-width:100%;overflow-x:auto}.sgc-barra .sgc-modo-btn span,.sgc-barra .sgc-btn span{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}' +
            '.sgc-modo-btn,.sgc-barra .sgc-btn{min-width:44px;min-height:44px;justify-content:center;padding:6px}' +
            /* el zoom queda compacto para no chocar con la vista 3D (abajo a la izquierda) */
            '#sgc-zfit span{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}.sgc-zoom{right:6px;bottom:6px}' +
            '.sgc-3d{position:absolute;top:calc(min(100vw,58dvh) + 52px - 150px);left:10px;z-index:2;width:136px;height:136px}.sgc-3d-giro{right:4px;bottom:4px;min-height:30px!important;min-width:0!important;padding:2px 6px;font-size:11px}' +
            '.sgc-metrics>.sgc-tools-t:first-child,.sgc-3d-nota{display:none}.sgc-metrics>.sgc-sep{margin-top:0}' +
            '.sgc-canvas-wrap{order:-1;flex:none;height:calc(min(100vw,58vh) + 52px);height:calc(min(100vw,58dvh) + 52px);padding:6px}' +
            '.sgc-tools,.sgc-metrics{overflow:visible;border:0;border-top:1px solid var(--border-color)}' +
            '.sgc-formas{grid-template-columns:repeat(auto-fill,minmax(112px,1fr))}.sgc-grid-btn{grid-template-columns:repeat(auto-fill,minmax(130px,1fr))}' +
            '.sgc-modal-foot{flex-wrap:wrap;padding:10px 14px}.sgc-foot-hint{display:none}.sgc-foot-actions{width:100%}.sgc-foot-actions .sgc-btn{flex:1}}' +
            '@media (max-width:980px) and (orientation:landscape){.sgc-canvas-wrap{height:78dvh}}';
        document.head.appendChild(st);
        wireEventos();
    }

    function wireEventos() {
        $('sgc-cerrar').onclick = function () { cerrar(false); };
        $('sgc-cancelar').onclick = function () { cerrar(false); };
        $('sgc-confirmar').onclick = confirmar;
        $('sgc-undo').onclick = undo;
        $('sgc-redo').onclick = redo;
        $('sgc-eliminar').onclick = eliminarSeleccion;
        $('sgc-reset').onclick = function () { if (estado.puntos.length || estado.diseno) pushUndo(); estado.puntos = []; estado.diseno = null; estado.formaBase = 'Libre'; estado.sel = -1; encajar(); refrescarDiseno(); };
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-modo-btn'), function (b) { b.onclick = function () { cambiarModo(b.getAttribute('data-modo')); }; });
        function elegirColor(c) {
            estado.color = c; $('sgc-color').value = c;
            Array.prototype.forEach.call(document.querySelectorAll('.sgc-color'), function (b) { b.setAttribute('aria-pressed', String(b.getAttribute('data-color') === c)); });
            pintarLienzo();
        }
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-color'), function (b) { b.onclick = function () { elegirColor(b.getAttribute('data-color')); }; });
        $('sgc-color').addEventListener('input', function () { elegirColor(this.value); });
        elegirColor(estado.color);
        $('sgc-grosor').addEventListener('input', function () { estado.grosor = parseInt(this.value, 10) || 6; $('sgc-grosor-val').textContent = String(estado.grosor); pintarLienzo(); });
        $('sgc-borrar-diseno').onclick = borrarDiseno;
        $('sgc-3d-giro').onclick = function () { if (window.SGCorte3D) { window.SGCorte3D.alternarGiro(); pintarGiro(); } };
        $('sgc-r1').onclick = aplicarRecomendado;
        $('sgc-zin').onclick = function () { zoomEn(1.25); };
        $('sgc-zout').onclick = function () { zoomEn(1 / 1.25); };
        $('sgc-zfit').onclick = function () { encajar(); pintarLienzo(); };
        $('sgc-rotar').onclick = rotar90;
        $('sgc-espejo-h').onclick = espejoH;
        $('sgc-espejo-v').onclick = espejoV;
        $('sgc-centrar').onclick = centrar;
        $('sgc-aplicar-medidas').onclick = ajustarMedidas;
        ['sgc-mw', 'sgc-mh'].forEach(function (id) { $(id).addEventListener('keydown', function (e) { if (e.key === 'Enter') { e.preventDefault(); ajustarMedidas(); } }); });
        ['sgc-px', 'sgc-py'].forEach(function (id) { $(id).addEventListener('change', editarPunto); });
        $('sgc-snap').addEventListener('change', function () { estado.snap = parseInt(this.value, 10) || 1; });
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-forma-btn'), function (b) { b.onclick = function () { aplicarFormaBase(b.getAttribute('data-forma')); }; });

        var nSlider = $('sgc-n');
        nSlider.addEventListener('input', function () {
            estado.nCirculo = Math.max(8, Math.min(64, parseInt(nSlider.value, 10) || 32));
            $('sgc-n-val').textContent = String(estado.nCirculo);
            if (estado.formaBase === 'Circular') {                     // regenera el circulo en vivo
                estado.puntos = formaBase('Circular'); estado.sel = -1; pintarLienzo(); autosave();
            }
        });

        var svg = $('sgc-lienzo');
        /* Coordenadas del puntero en cm del lienzo (la matriz del grupo incluye letterbox, zoom y desplazamiento) */
        function aCoords(e) {
            var g = svg.querySelector('.sgc-view'); if (!g || !g.getScreenCTM) return null;
            var pt = svg.createSVGPoint(); pt.x = e.clientX; pt.y = e.clientY;
            var m = g.getScreenCTM(); if (!m) return null;
            var p = pt.matrixTransform(m.inverse());
            return { x: p.x, y: p.y };
        }
        function recortar(p) { return { x: clamp(snap(p.x), 0, LIENZO_W), y: clamp(snap(p.y), 0, LIENZO_H) }; }

        /* Punto dentro del lienzo, sin iman: un trazo a mano no debe verse escalonado */
        function enLienzo(p) { return { x: clamp(p.x, 0, LIENZO_W), y: clamp(p.y, 0, LIENZO_H) }; }

        svg.addEventListener('pointerdown', function (e) {
            if (e.button !== undefined && e.button > 0 && e.pointerType === 'mouse') return;
            var asa = e.target.closest ? e.target.closest('.sgc-asas') : null;
            try { svg.setPointerCapture(e.pointerId); } catch (x) { /* idem */ }
            if (estado.modo !== 'puntos') {
                var inicio = aCoords(e);
                e.preventDefault();
                if (!inicio) return;
                inicio = enLienzo(inicio);
                if (estado.modo === 'contorno') estado.trazo = { tipo: 'contorno', pts: [inicio], d: 'M' + inicio.x + ' ' + inicio.y };
                else if (estado.modo === 'relleno') rellenar(inicio);
                else if (estado.puntos.length >= MIN_PUNTOS) empezarPincel(inicio);
                return;
            }
            if (asa) {
                estado.arrastrando = parseInt(asa.getAttribute('data-i'), 10); estado.sel = estado.arrastrando; estado.movio = false;
                pushUndo();                                              // un arrastre = un paso de deshacer
                pintarPunto();
                e.preventDefault(); return;
            }
            /* Fondo: si el puntero se mueve es desplazar la vista; si no, al soltar se anade un punto */
            estado.gesto = { x0: e.clientX, y0: e.clientY, cx: estado.vista.cx, cy: estado.vista.cy, pan: false };
            e.preventDefault();
        });
        svg.addEventListener('pointermove', function (e) {
            if (estado.trazo) {
                var ahora = aCoords(e); if (!ahora) return;
                ahora = enLienzo(ahora);
                if (estado.trazo.tipo === 'pincel') { extenderPincel(ahora); return; }
                estado.trazo.pts.push(ahora);
                estado.trazo.d += ' L' + ahora.x + ' ' + ahora.y;
                var mano = $('sgc-a-mano'); if (mano) mano.setAttribute('d', estado.trazo.d);
                return;
            }
            if (estado.modo !== 'puntos') return;
            if (estado.arrastrando >= 0) {
                var p = aCoords(e); if (!p) return;
                var q = recortar(p), actual = estado.puntos[estado.arrastrando];
                if (!actual || (actual.x === q.x && actual.y === q.y)) return;
                estado.puntos[estado.arrastrando] = q; estado.movio = true; estado.formaBase = 'Libre'; pintarLienzo();
                return;
            }
            var gst = estado.gesto; if (!gst) return;
            var dx = e.clientX - gst.x0, dy = e.clientY - gst.y0;
            if (!gst.pan && Math.sqrt(dx * dx + dy * dy) < 6) return;
            gst.pan = true;
            var ppu = pxPorCm();
            estado.vista.cx = clamp(gst.cx - dx / ppu, 0, LIENZO_W);
            estado.vista.cy = clamp(gst.cy - dy / ppu, 0, LIENZO_H);
            svg.style.cursor = 'grabbing';
            pintarLienzo();
        });
        function soltar(e) {
            if (estado.trazo) {
                var hecho = estado.trazo; estado.trazo = null;
                if (hecho.tipo === 'contorno') terminarContorno(hecho);
                else { refrescarDiseno(); autosave(); }
                return;
            }
            svg.style.cursor = '';
            if (estado.arrastrando >= 0) {
                if (!estado.movio) estado.undo.pop();                   // clic sin mover: no deja paso vacio en el historial
                estado.arrastrando = -1; estado.movio = false; pintarLienzo(); autosave();
                return;
            }
            var gst = estado.gesto; estado.gesto = null;
            if (!gst || gst.pan || !e || e.type === 'pointercancel') return;
            var p = aCoords(e); if (!p || p.x < 0 || p.y < 0 || p.x > LIENZO_W || p.y > LIENZO_H) return;
            anadirPunto(p);
        }
        svg.addEventListener('pointerup', soltar);
        svg.addEventListener('pointercancel', soltar);
        /* Doble clic sobre un punto lo elimina. Se busca por cercania: con la captura del puntero el
           destino del evento puede ser el propio SVG y no el circulo. */
        svg.addEventListener('dblclick', function (e) {
            if (estado.modo !== 'puntos') return;
            var p = aCoords(e); if (!p) return;
            var radio = (tactil ? 22 : 12) / pxPorCm(), mejor = -1, md = Infinity;
            estado.puntos.forEach(function (q, i) { var d = Math.sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y)); if (d < md) { md = d; mejor = i; } });
            if (mejor >= 0 && md <= radio) eliminarVertice(mejor);
        });
        svg.addEventListener('wheel', function (e) {
            e.preventDefault();
            zoomEn(e.deltaY < 0 ? 1.15 : 1 / 1.15, aCoords(e));
        }, { passive: false });
        window.addEventListener('resize', function () { if (estado.abierto) pintarLienzo(); });

        document.addEventListener('keydown', function (e) {
            if (!estado.abierto) return;
            var k = (e.key || '').toLowerCase();
            var enCampo = /^(input|select|textarea)$/i.test((document.activeElement || {}).tagName || '');
            if (e.key === 'Escape') { cerrar(false); return; }
            if ((e.ctrlKey || e.metaKey) && k === 'z' && !e.shiftKey) { if (!enCampo) { e.preventDefault(); undo(); } return; }
            if ((e.ctrlKey || e.metaKey) && (k === 'y' || (k === 'z' && e.shiftKey))) { if (!enCampo) { e.preventDefault(); redo(); } return; }
            if (enCampo || estado.sel < 0) return;
            var paso = e.shiftKey ? 10 : Math.max(1, estado.snap);
            var mov = { arrowleft: [-paso, 0], arrowright: [paso, 0], arrowup: [0, -paso], arrowdown: [0, paso] }[k];
            if (mov) { e.preventDefault(); moverSeleccion(mov[0], mov[1]); }
            else if (k === 'delete' || k === 'backspace') { e.preventDefault(); eliminarSeleccion(); }
        });
    }

    /* ------------------------------------------------------------------ API publica */
    var api = window.SGCorte = {
        abrir: function (opener, opciones) { abrir(opener, opciones); },
        cerrar: function () { cerrar(false); },
        estado: function () { return { puntos: cloneP(estado.puntos), formaBase: estado.formaBase }; },
        puntosConfirmados: puntosConfirmados,
        /* El configurador la define: devuelve { forma, ancho, alto } para empezar desde la pieza actual */
        plantilla: null,
        /* El formulario lo llama al enviar: el detalle limpia el borrador solo si el POST tuvo exito */
        marcarEnviado: function () { try { localStorage.setItem(KEY_LIMPIAR, String(Date.now())); } catch (e) { /* idem */ } },
        aTexto: aTexto, desdeTexto: desdeTexto,
        /* ganchos de prueba (funciones puras; no los usa la aplicacion) */
        _puro: { autointersecta: autointersecta, formasBase: formasBase, bbox: bbox, perimetro: perimetro, snap: snap, aristaMasCercana: aristaMasCercana, metricas: metricas, sinRepetidos: sinRepetidos, simplificarTrazo: simplificarTrazo }
    };

    document.addEventListener('click', function (e) {
        var t = e.target.closest ? e.target.closest('[data-sg-corte]') : null;
        if (t) { e.preventDefault(); abrir(t); }
    });
})();
