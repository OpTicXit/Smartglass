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
   ============================================================================= */
(function () {
    'use strict';

    var G = window.SGGlass;

    var LIENZO_W = 300, LIENZO_H = 200;           // cm reales (1 unidad SVG = 1 cm)
    var MIN_PUNTOS = 3, MAX_PUNTOS = 64;
    var MIN_DIM = 10;                             // cm; igual que min="10" de ancho/alto en el formulario
    var SNAP = 1;                                 // rejilla en cm (la caja del corte queda en cm enteros)
    var UNDO_MAX = 50;
    var KEY_BORRADOR = 'sg-borrador-corte';
    var KEY_PREVIO = 'sg-borrador-corte-previo';
    var KEY_LIMPIAR = 'sg-borrador-corte-limpiar';   // lo pone el formulario al enviar; lo consume el detalle

    var estado = {
        abierto: false, puntos: [], formaBase: 'Rectangular / Cuadrado', sel: -1,
        undo: [], redo: [], arrastrando: -1, movio: false, zoom: 1, nCirculo: 32, previo: null, opener: null
    };

    /* ------------------------------------------------------------------ geometria pura */
    function snap(v) { return Math.round(v / SNAP) * SNAP; }
    function cloneP(pts) { return pts.map(function (p) { return { x: p.x, y: p.y }; }); }
    function snapAll(pts) { return pts.map(function (p) { return { x: snap(p.x), y: snap(p.y) }; }); }

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
        if (!pts || !pts.length) return { w: 0, h: 0, minX: 0, minY: 0 };
        var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        pts.forEach(function (p) {
            if (p.x < minX) minX = p.x; if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x; if (p.y > maxY) maxY = p.y;
        });
        return { w: maxX - minX, h: maxY - minY, minX: minX, minY: minY };
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
            if (!isFinite(x) || !isFinite(y)) return [];
            out.push({ x: x, y: y });
        }
        return out;
    }

    /* ------------------------------------------------------------------ formas base */
    function formasBase() {
        var cx = LIENZO_W / 2, cy = LIENZO_H / 2;
        function rect(w, h) { var x = cx - w / 2, y = cy - h / 2; return [{ x: x, y: y }, { x: x + w, y: y }, { x: x + w, y: y + h }, { x: x, y: y + h }]; }
        function pol(n, r) {
            var a = [];
            for (var i = 0; i < n; i++) { var t = i * 2 * Math.PI / n - Math.PI / 2; a.push({ x: cx + r * Math.cos(t), y: cy + r * Math.sin(t) }); }
            return a;
        }
        return {
            'Rectangular / Cuadrado': snapAll(rect(120, 120)),
            'Triangular': snapAll([{ x: cx, y: cy - 70 }, { x: cx + 61, y: cy + 35 }, { x: cx - 61, y: cy + 35 }]),
            'Trapezoidal': snapAll([{ x: cx - 40, y: cy - 55 }, { x: cx + 40, y: cy - 55 }, { x: cx + 70, y: cy + 55 }, { x: cx - 70, y: cy + 55 }]),
            'Rombo / Losango': snapAll([{ x: cx, y: cy - 70 }, { x: cx + 55, y: cy }, { x: cx, y: cy + 70 }, { x: cx - 55, y: cy }]),
            'Hexagonal': snapAll(pol(6, 70)),
            'Octogonal': snapAll(pol(8, 70)),
            'Polígono regular': snapAll(pol(9, 70)),
            'Circular / Elíptica': snapAll(pol(estado.nCirculo, 70)),   // polígono inscrito de N lados (8-64)
            'Corte libre': []
        };
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
        if (n < MIN_PUNTOS) return { ok: false, msg: 'Necesitas al menos ' + MIN_PUNTOS + ' puntos (tienes ' + n + ').' };
        if (n > MAX_PUNTOS) return { ok: false, msg: 'El máximo es ' + MAX_PUNTOS + ' puntos.' };
        var bb = bbox(pts);
        if (bb.w < MIN_DIM || bb.h < MIN_DIM) return { ok: false, msg: 'El corte debe medir al menos ' + MIN_DIM + ' × ' + MIN_DIM + ' cm.' };
        if (autointersecta(pts)) return { ok: false, msg: 'Las líneas del corte se cruzan o se tocan: mueve algún punto.' };
        if (!(areaPoligono(pts) > 0)) return { ok: false, msg: 'El corte no encierra área.' };
        return { ok: true, msg: '' };
    }

    /* ------------------------------------------------------------------ pintado */
    function svgPath(pts) {
        if (!pts.length) return '';
        var d = 'M' + pts[0].x + ' ' + pts[0].y;
        for (var i = 1; i < pts.length; i++) d += ' L' + pts[i].x + ' ' + pts[i].y;
        return d + ' Z';
    }

    function pintarLienzo() {
        var svg = $('sgc-lienzo'); if (!svg) return;
        var pts = estado.puntos, m = metricas(), v = validez();
        var rejilla = '', gx, gy;
        for (gx = 0; gx <= LIENZO_W; gx += 10) rejilla += '<line x1="' + gx + '" y1="0" x2="' + gx + '" y2="' + LIENZO_H + '" class="sgc-grid"/>';
        for (gy = 0; gy <= LIENZO_H; gy += 10) rejilla += '<line x1="0" y1="' + gy + '" x2="' + LIENZO_W + '" y2="' + gy + '" class="sgc-grid"/>';
        var asas = pts.map(function (p, i) {
            return '<circle cx="' + p.x + '" cy="' + p.y + '" r="4.5" class="sgc-asas' + (i === 0 ? ' sgc-asas-inicio' : '') +
                (i === estado.sel ? ' sgc-asas-sel' : '') + '" data-i="' + i + '"/>';
        }).join('');
        var poly = pts.length >= 2 ? '<path d="' + svgPath(pts) + '" class="sgc-forma' + (v.ok || pts.length < MIN_PUNTOS ? '' : ' sgc-forma-mal') + '"/>' : '';
        var bb = m.bbox, cotas = '';
        if (pts.length >= 2 && bb.w > 0) {
            cotas =
                '<line x1="' + bb.minX + '" y1="' + (LIENZO_H - 6) + '" x2="' + (bb.minX + bb.w) + '" y2="' + (LIENZO_H - 6) + '" class="sgc-cota"/>' +
                '<text x="' + (bb.minX + bb.w / 2) + '" y="' + (LIENZO_H - 8) + '" class="sgc-cota-txt">' + Math.round(bb.w) + ' cm</text>' +
                '<line x1="' + (LIENZO_W - 6) + '" y1="' + bb.minY + '" x2="' + (LIENZO_W - 6) + '" y2="' + (bb.minY + bb.h) + '" class="sgc-cota"/>' +
                '<text x="' + (LIENZO_W - 9) + '" y="' + (bb.minY + bb.h / 2) + '" class="sgc-cota-txt" transform="rotate(-90 ' + (LIENZO_W - 9) + ' ' + (bb.minY + bb.h / 2) + ')">' + Math.round(bb.h) + ' cm</text>';
        }
        var z = estado.zoom, tx = (LIENZO_W / 2) * (1 - z), ty = (LIENZO_H / 2) * (1 - z);
        svg.innerHTML =
            '<g class="sgc-view" transform="translate(' + tx + ' ' + ty + ') scale(' + z + ')">' +
            rejilla + '<rect x="0" y="0" width="' + LIENZO_W + '" height="' + LIENZO_H + '" class="sgc-marco"/>' +
            poly + cotas + asas + '</g>';
        pintarPanel(m, v);
        actualizarBotones(v);
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
            if (abs) abs.textContent = fmt(m.co2, 1) + ' kg CO₂e · tu corte';
        } else {
            if (chip) { chip.textContent = '—'; chip.className = 'sgc-chip'; }
            if (inten) inten.textContent = '—';
            if (abs) abs.textContent = '—';
        }
        var msg = $('sgc-msg');
        if (msg) { msg.textContent = v.ok ? '' : v.msg; msg.className = 'sgc-msg' + (v.ok ? '' : ' sgc-msg-mal'); }
    }

    function actualizarBotones(v) {
        function dis(id, f) { var e = $(id); if (e) e.disabled = f; }
        dis('sgc-confirmar', !(v && v.ok));
        dis('sgc-eliminar', estado.puntos.length <= MIN_PUNTOS);
        dis('sgc-undo', !estado.undo.length);
        dis('sgc-redo', !estado.redo.length);
    }

    /* ------------------------------------------------------------------ historial y autoguardado */
    function pushUndo() { estado.undo.push(cloneP(estado.puntos)); if (estado.undo.length > UNDO_MAX) estado.undo.shift(); estado.redo.length = 0; }
    function undo() { if (!estado.undo.length) return; estado.redo.push(cloneP(estado.puntos)); estado.puntos = estado.undo.pop(); estado.sel = -1; pintarLienzo(); autosave(); }
    function redo() { if (!estado.redo.length) return; estado.undo.push(cloneP(estado.puntos)); estado.puntos = estado.redo.pop(); estado.sel = -1; pintarLienzo(); autosave(); }

    var timerAutosave = null;
    function guardarYa() {
        clearTimeout(timerAutosave);
        if (estado.puntos.length < MIN_PUNTOS) return;
        try { localStorage.setItem(KEY_BORRADOR, JSON.stringify({ formaBase: estado.formaBase, puntos: estado.puntos, t: Date.now() })); } catch (e) { /* sin almacenamiento: no pasa nada */ }
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
    function aplicarFormaBase(nombre) {
        var fb = formasBase()[nombre]; if (!fb) return;
        /* No se pierde lo que habia: va al historial (Ctrl+Z) y queda como "corte anterior" restaurable */
        if (estado.puntos.length >= MIN_PUNTOS) {
            estado.previo = cloneP(estado.puntos);
            guardarPrevio(estado.previo);
            pushUndo();
            mostrarBanner('Cambiaste de forma. Tu corte anterior tenía <b>' + estado.previo.length + ' puntos</b>.', [
                { id: 'sgc-restaurar-previo', texto: 'Restaurar mi corte anterior (' + estado.previo.length + ' puntos)', fn: function () {
                    pushUndo(); estado.puntos = cloneP(estado.previo); estado.formaBase = 'Corte libre'; estado.sel = -1; pintarLienzo(); autosave(); ocultarBanner();
                } },
                { id: 'sgc-descartar-previo', texto: 'Descartar', ghost: true, fn: function () { estado.previo = null; borrarLS(KEY_PREVIO); ocultarBanner(); } }
            ]);
        } else {
            pushUndo();
        }
        estado.formaBase = nombre; estado.puntos = cloneP(fb); estado.sel = -1;
        pintarLienzo(); autosave();
    }

    function anadirPunto(p) {
        if (estado.puntos.length >= MAX_PUNTOS) return;
        var q = { x: snap(p.x), y: snap(p.y) };
        pushUndo();
        if (estado.puntos.length >= MIN_PUNTOS) {
            var i = aristaMasCercana(q, estado.puntos);          // se inserta en la arista mas cercana
            estado.puntos.splice(i + 1, 0, q); estado.sel = i + 1;
        } else {
            estado.puntos.push(q); estado.sel = estado.puntos.length - 1;
        }
        pintarLienzo(); autosave();
    }

    function eliminarVertice(i) {
        if (estado.puntos.length <= MIN_PUNTOS || i < 0 || i >= estado.puntos.length) return;
        pushUndo(); estado.puntos.splice(i, 1); estado.sel = -1; pintarLienzo(); autosave();
    }
    function eliminarSeleccion() { eliminarVertice(estado.sel >= 0 ? estado.sel : estado.puntos.length - 1); }

    /* R1: cambia material/proceso SIN mover ningun vertice */
    function aplicarRecomendado() {
        var m = metricas();
        var box = $('sgc-r1-cambios'); if (!box) return;
        if (!m.hayArea) { box.textContent = ''; var li0 = document.createElement('li'); li0.textContent = 'Dibuja tu corte primero.'; box.appendChild(li0); box.style.display = 'block'; return; }
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
            var li = document.createElement('li'); li.textContent = 'Tu corte ya está en perfil favorable o no hay ajustes seguros que aplicar.'; box.appendChild(li);
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
        if (sel) { sel.value = 'Personalizada'; dispararEventos(sel); }
        cerrar(true);
    }

    /* ------------------------------------------------------------------ banner */
    function ocultarBanner() { var b = $('sgc-banner'); if (b) { b.style.display = 'none'; b.textContent = ''; } }
    function mostrarBanner(htmlSeguro, acciones) {
        var b = $('sgc-banner'); if (!b) return;
        b.textContent = '';
        var t = document.createElement('span'); t.innerHTML = htmlSeguro; b.appendChild(t);    // solo texto fijo y numeros
        var grupo = document.createElement('span'); grupo.className = 'sgc-banner-acc';
        acciones.forEach(function (a) {
            var bt = document.createElement('button'); bt.type = 'button'; bt.id = a.id; bt.textContent = a.texto;
            bt.className = 'sgc-btn-mini' + (a.ghost ? ' sgc-btn-mini-ghost' : '');
            bt.addEventListener('click', a.fn); grupo.appendChild(bt);
        });
        b.appendChild(grupo); b.style.display = 'flex';
    }

    /* ------------------------------------------------------------------ abrir / cerrar */
    function abrir(opener) {
        if (!G || !G.areaPoligono) { window.alert('El editor de corte necesita glass-preview.js actualizado.'); return; }
        inyectarModal();
        estado.opener = opener || document.activeElement;
        ocultarBanner();
        var conf = puntosConfirmados(), bor = leerLS(KEY_BORRADOR);
        if (conf.length) {
            estado.puntos = cloneP(conf); estado.formaBase = 'Corte libre';
        } else {
            estado.puntos = cloneP(formasBase()['Rectangular / Cuadrado']); estado.formaBase = 'Rectangular / Cuadrado';
            if (puntosValidosLS(bor)) {
                var n = bor.puntos.length;
                mostrarBanner('Tenías un corte de <b>' + n + ' puntos</b> sin guardar.', [
                    { id: 'sgc-restaurar', texto: 'Restaurar mi corte anterior (' + n + ' puntos)', fn: function () {
                        pushUndo(); estado.puntos = cloneP(bor.puntos); estado.formaBase = bor.formaBase || 'Corte libre'; estado.sel = -1; pintarLienzo(); ocultarBanner();
                    } },
                    { id: 'sgc-descartar', texto: 'Empezar de cero', ghost: true, fn: function () { borrarLS(KEY_BORRADOR); ocultarBanner(); } }
                ]);
            }
        }
        estado.undo.length = 0; estado.redo.length = 0; estado.zoom = 1; estado.sel = -1; estado.abierto = true;
        var r1 = $('sgc-r1-cambios'); if (r1) { r1.style.display = 'none'; r1.textContent = ''; }
        $('sgc-modal').style.display = 'flex';
        document.body.classList.add('sgc-bloquear-scroll');
        pintarLienzo();
        var x = $('sgc-cerrar'); if (x) x.focus();
    }

    function cerrar(confirmado) {
        if (!estado.abierto) return;
        estado.abierto = false;
        $('sgc-modal').style.display = 'none';
        document.body.classList.remove('sgc-bloquear-scroll');
        document.dispatchEvent(new CustomEvent('sgcorte:cerrado', { detail: { confirmado: confirmado === true } }));
        if (estado.opener && estado.opener.focus) { try { estado.opener.focus(); } catch (e) { /* idem */ } }
    }

    /* ------------------------------------------------------------------ modal */
    function inyectarModal() {
        if ($('sgc-modal')) return;
        var nombres = Object.keys(formasBase());
        var botones = nombres.map(function (k) { return '<button type="button" class="sgc-forma-btn" data-forma="' + k + '">' + k + '</button>'; }).join('');
        var html =
            '<div id="sgc-modal" class="sgc-modal" role="dialog" aria-modal="true" aria-label="Editor de corte personalizado">' +
            '<div class="sgc-modal-inner">' +
            '<header class="sgc-modal-head"><div><span class="sgc-kicker">CORTE PERSONALIZADO</span><h2>Dibuja tu pieza</h2></div><button type="button" id="sgc-cerrar" class="sgc-x" aria-label="Cerrar">✕</button></header>' +
            '<div id="sgc-banner" class="sgc-banner" role="status"></div>' +
            '<div class="sgc-body">' +
            '<aside class="sgc-tools"><p class="sgc-tools-t">Forma base</p><div class="sgc-formas">' + botones + '</div>' +
            '<label class="sgc-n"><span>Lados del círculo: <b id="sgc-n-val">32</b></span><input type="range" id="sgc-n" min="8" max="64" step="1" value="32" aria-label="Número de lados del círculo"></label>' +
            '<p class="sgc-tools-t">Edición</p>' +
            '<div class="sgc-row"><button type="button" id="sgc-undo" class="sgc-btn" title="Deshacer (Ctrl+Z)">↶ Deshacer</button><button type="button" id="sgc-redo" class="sgc-btn" title="Rehacer (Ctrl+Y)">↷ Rehacer</button></div>' +
            '<div class="sgc-row"><button type="button" id="sgc-eliminar" class="sgc-btn sgc-btn-warn">Quitar punto</button><button type="button" id="sgc-reset" class="sgc-btn sgc-btn-ghost">Reiniciar</button></div>' +
            '<p class="sgc-hint">Clic en el lienzo para añadir un punto (se inserta en la línea más cercana). Arrastra un círculo para moverlo; doble clic sobre un círculo lo elimina. Mínimo ' + MIN_PUNTOS + ', máximo ' + MAX_PUNTOS + '.</p>' +
            '<button type="button" id="sgc-r1" class="sgc-btn sgc-btn-eco">✦ Versión balanceada (respeta tu forma)</button><ul id="sgc-r1-cambios" class="sgc-r1-cambios"></ul></aside>' +
            '<main class="sgc-canvas-wrap"><svg id="sgc-lienzo" viewBox="0 0 ' + LIENZO_W + ' ' + LIENZO_H + '" preserveAspectRatio="xMidYMid meet" role="img" aria-label="Lienzo de dibujo del corte"></svg>' +
            '<div class="sgc-zoom"><button type="button" id="sgc-zout" class="sgc-btn-mini" aria-label="Alejar">−</button><span>zoom</span><button type="button" id="sgc-zin" class="sgc-btn-mini" aria-label="Acercar">+</button></div></main>' +
            '<aside class="sgc-metrics"><p class="sgc-tools-t">Tu corte, en vivo</p>' +
            '<div class="sgc-metric"><span>Área del corte</span><b id="sgc-m-area">0 m²</b></div>' +
            '<div class="sgc-metric"><span>Perímetro</span><b id="sgc-m-perim">0 cm</b></div>' +
            '<div class="sgc-metric"><span>Vértices</span><b id="sgc-m-vert">0</b></div>' +
            '<div class="sgc-metric"><span>Peso</span><b id="sgc-m-peso">—</b></div>' +
            '<div class="sgc-metric"><span>CO₂e de tu pieza</span><b id="sgc-m-co2">—</b></div>' +
            '<div class="sgc-proceso"><span class="sgc-tools-t">Proceso · intensidad</span><span class="sgc-chip" id="sgc-m-nivel">—</span><small id="sgc-m-int">—</small></div>' +
            '<div class="sgc-absoluto"><small id="sgc-m-absoluto">—</small></div>' +
            '<p class="sgc-note">El color es del <b>proceso</b> (intensidad). Tu total es un dato neutro. El valor que se guarda lo calcula el servidor con tu corte real.</p></aside>' +
            '</div>' +
            '<footer class="sgc-modal-foot"><div><span class="sgc-foot-hint">Tu progreso se guarda solo; si cambias de forma o recargas, podrás restaurarlo.</span><div id="sgc-msg" class="sgc-msg" role="alert" aria-live="polite"></div></div>' +
            '<div class="sgc-foot-actions"><button type="button" id="sgc-cancelar" class="sgc-btn sgc-btn-ghost">Cancelar</button><button type="button" id="sgc-confirmar" class="sgc-btn sgc-btn-principal">Confirmar corte</button></div></footer>' +
            '</div></div>';
        var d = document.createElement('div'); d.innerHTML = html; document.body.appendChild(d.firstChild);

        var st = document.createElement('style'); st.id = 'sgc-css';
        st.textContent =
            '.sgc-modal{display:none;position:fixed;inset:0;z-index:9999;background:rgba(8,12,20,.72);backdrop-filter:blur(4px);align-items:center;justify-content:center;padding:18px}' +
            '.sgc-modal-inner{width:min(1180px,100%);max-height:94vh;display:flex;flex-direction:column;background:var(--bg-secondary);border:1px solid var(--border-color);border-radius:16px;overflow:hidden;color:var(--text-main)}' +
            '.sgc-modal-head{display:flex;align-items:flex-start;justify-content:space-between;padding:18px 22px;border-bottom:1px solid var(--border-color)}' +
            '.sgc-kicker{font-size:.7rem;font-weight:800;letter-spacing:.14em;color:var(--color-accent-text)}' +
            '.sgc-modal-head h2{margin:4px 0 0;font-size:1.4rem;font-weight:800;letter-spacing:-.02em}' +
            '.sgc-x{background:none;border:0;color:var(--text-muted);font-size:1.2rem;cursor:pointer}' +
            '.sgc-banner{display:none;gap:10px;align-items:center;justify-content:space-between;flex-wrap:wrap;padding:10px 22px;background:var(--color-warning-dim);border-bottom:1px solid var(--color-warning);font-size:.85rem}' +
            '.sgc-banner-acc{display:flex;gap:8px;flex-wrap:wrap}' +
            '.sgc-body{display:grid;grid-template-columns:230px 1fr 250px;gap:0;min-height:0;overflow:hidden}' +
            '.sgc-tools,.sgc-metrics{padding:16px;overflow:auto;border-right:1px solid var(--border-color)}' +
            '.sgc-metrics{border-right:0;border-left:1px solid var(--border-color)}' +
            '.sgc-tools-t{margin:0 0 8px;font-size:.7rem;font-weight:800;letter-spacing:.1em;text-transform:uppercase;color:var(--text-muted)}' +
            '.sgc-formas{display:flex;flex-direction:column;gap:6px;margin-bottom:10px}' +
            '.sgc-forma-btn{text-align:left;padding:7px 9px;border:1px solid var(--border-color);border-radius:8px;background:var(--bg-tertiary);color:var(--text-main);font-size:.8rem;cursor:pointer}' +
            '.sgc-forma-btn:hover{border-color:var(--color-accent)}' +
            '.sgc-n{display:block;margin:0 0 14px;font-size:.76rem;color:var(--text-muted)}.sgc-n input{width:100%}' +
            '.sgc-row{display:flex;gap:6px;margin-bottom:8px}.sgc-row .sgc-btn{flex:1}' +
            '.sgc-btn{padding:8px 10px;border:1px solid var(--border-color);border-radius:8px;background:var(--bg-tertiary);color:var(--text-main);font-size:.8rem;font-weight:700;cursor:pointer}' +
            '.sgc-btn:disabled{opacity:.4;cursor:not-allowed}' +
            '.sgc-btn-warn{border-color:var(--color-warning);color:var(--color-warning)}' +
            '.sgc-btn-eco{width:100%;margin-top:6px;border-color:var(--color-success);color:var(--color-success);background:var(--color-success-dim)}' +
            '.sgc-btn-ghost{background:none}' +
            '.sgc-btn-principal{background:var(--color-accent);color:var(--text-on-accent);border-color:var(--color-accent)}' +
            '.sgc-btn-mini{padding:4px 9px;border:1px solid var(--border-color);border-radius:6px;background:var(--bg-tertiary);color:var(--text-main);font-size:.78rem;cursor:pointer}' +
            '.sgc-btn-mini-ghost{background:none}' +
            '.sgc-hint{font-size:.74rem;color:var(--text-muted);line-height:1.5;margin:10px 0}' +
            '.sgc-r1-cambios{display:none;margin:8px 0 0;padding:8px 8px 8px 22px;font-size:.76rem;color:var(--text-muted);line-height:1.5;border:1px dashed var(--border-color);border-radius:8px}' +
            '.sgc-canvas-wrap{position:relative;display:flex;align-items:center;justify-content:center;background:var(--bg-primary);min-height:0;padding:10px}' +
            '#sgc-lienzo{width:100%;height:100%;max-height:62vh;touch-action:none;cursor:crosshair}' +
            '.sgc-grid{stroke:var(--border-color);stroke-width:.4;opacity:.5}' +
            '.sgc-marco{fill:none;stroke:var(--border-color);stroke-width:1}' +
            '.sgc-forma{fill:var(--color-accent);fill-opacity:.12;stroke:var(--color-accent);stroke-width:1.4}' +
            '.sgc-forma-mal{fill:var(--color-error);fill-opacity:.1;stroke:var(--color-error)}' +
            '.sgc-asas{fill:var(--bg-secondary);stroke:var(--color-accent);stroke-width:1.6;cursor:grab}' +
            '.sgc-asas-inicio{fill:var(--color-success);stroke:var(--color-success)}' +
            '.sgc-asas-sel{stroke:var(--color-warning);stroke-width:2.4}' +
            '.sgc-cota{stroke:var(--text-muted);stroke-width:.8;opacity:.8}' +
            '.sgc-cota-txt{fill:var(--text-muted);font-size:7px;font-weight:700;text-anchor:middle}' +
            '.sgc-zoom{position:absolute;right:14px;bottom:14px;display:flex;gap:6px;align-items:center;font-size:.72rem;color:var(--text-muted)}' +
            '.sgc-metric{display:flex;justify-content:space-between;align-items:baseline;padding:7px 0;border-bottom:1px solid var(--border-color);font-size:.82rem}' +
            '.sgc-metric span{color:var(--text-muted)}.sgc-metric b{font-variant-numeric:tabular-nums}' +
            '.sgc-proceso{margin-top:12px;display:flex;flex-direction:column;gap:4px;align-items:flex-start}' +
            '.sgc-chip{padding:3px 9px;border-radius:999px;font-size:.72rem;font-weight:800;letter-spacing:.04em;text-transform:uppercase;border:1px solid var(--border-color)}' +
            '.sgc-chip-favorable{background:var(--color-success-dim);color:var(--color-success);border-color:var(--color-success)}' +
            '.sgc-chip-moderado{background:var(--color-warning-dim);color:var(--color-warning);border-color:var(--color-warning)}' +
            '.sgc-chip-alto{background:var(--color-error-dim);color:var(--color-error);border-color:var(--color-error)}' +
            '.sgc-absoluto{margin-top:8px}.sgc-absoluto small{color:var(--text-main);font-weight:700;font-size:.92rem}' +
            '.sgc-note{margin-top:12px;font-size:.72rem;color:var(--text-muted);line-height:1.5}' +
            '.sgc-modal-foot{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:14px 22px;border-top:1px solid var(--border-color)}' +
            '.sgc-foot-hint{font-size:.74rem;color:var(--text-muted)}.sgc-foot-actions{display:flex;gap:8px}' +
            '.sgc-msg{min-height:1.1em;margin-top:4px;font-size:.8rem}.sgc-msg-mal{color:var(--color-error);font-weight:700}' +
            '.sgc-bloquear-scroll{overflow:hidden}' +
            '@media(max-width:980px){.sgc-body{grid-template-columns:1fr;overflow:auto}.sgc-tools,.sgc-metrics{border:0;border-bottom:1px solid var(--border-color)}#sgc-lienzo{max-height:46vh}}';
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
        $('sgc-reset').onclick = function () { if (estado.puntos.length) pushUndo(); estado.puntos = []; estado.formaBase = 'Corte libre'; estado.sel = -1; pintarLienzo(); };
        $('sgc-r1').onclick = aplicarRecomendado;
        $('sgc-zin').onclick = function () { estado.zoom = Math.min(2.5, estado.zoom * 1.2); pintarLienzo(); };
        $('sgc-zout').onclick = function () { estado.zoom = Math.max(0.5, estado.zoom / 1.2); pintarLienzo(); };
        Array.prototype.forEach.call(document.querySelectorAll('.sgc-forma-btn'), function (b) { b.onclick = function () { aplicarFormaBase(b.getAttribute('data-forma')); }; });

        var nSlider = $('sgc-n');
        nSlider.addEventListener('input', function () {
            estado.nCirculo = Math.max(8, Math.min(64, parseInt(nSlider.value, 10) || 32));
            $('sgc-n-val').textContent = String(estado.nCirculo);
            if (estado.formaBase === 'Circular / Elíptica') {            // regenera el circulo en vivo
                estado.puntos = cloneP(formasBase()['Circular / Elíptica']); estado.sel = -1; pintarLienzo(); autosave();
            }
        });

        var svg = $('sgc-lienzo');
        /* Coordenadas del puntero en cm del lienzo (la matriz del grupo incluye letterbox y zoom) */
        function aCoords(e) {
            var g = svg.querySelector('.sgc-view'); if (!g || !g.getScreenCTM) return null;
            var pt = svg.createSVGPoint(); pt.x = e.clientX; pt.y = e.clientY;
            var m = g.getScreenCTM(); if (!m) return null;
            var p = pt.matrixTransform(m.inverse());
            return { x: p.x, y: p.y };
        }
        function recortar(p) { return { x: snap(Math.max(0, Math.min(LIENZO_W, p.x))), y: snap(Math.max(0, Math.min(LIENZO_H, p.y))) }; }

        svg.addEventListener('pointerdown', function (e) {
            var asa = e.target.closest ? e.target.closest('.sgc-asas') : null;
            if (asa) {
                estado.arrastrando = parseInt(asa.getAttribute('data-i'), 10); estado.sel = estado.arrastrando; estado.movio = false;
                pushUndo();                                              // un arrastre = un paso de deshacer
                try { svg.setPointerCapture(e.pointerId); } catch (x) { /* idem */ }
                e.preventDefault(); return;
            }
            var p = aCoords(e); if (!p || p.x < 0 || p.y < 0 || p.x > LIENZO_W || p.y > LIENZO_H) return;
            anadirPunto(p);
        });
        svg.addEventListener('pointermove', function (e) {
            if (estado.arrastrando < 0) return;
            var p = aCoords(e); if (!p) return;
            var q = recortar(p), actual = estado.puntos[estado.arrastrando];
            if (!actual || (actual.x === q.x && actual.y === q.y)) return;
            estado.puntos[estado.arrastrando] = q; estado.movio = true; pintarLienzo();
        });
        function soltar() {
            if (estado.arrastrando < 0) return;
            if (!estado.movio) estado.undo.pop();                       // clic sin mover: no deja paso vacio en el historial
            estado.arrastrando = -1; estado.movio = false; pintarLienzo(); autosave();
        }
        svg.addEventListener('pointerup', soltar);
        svg.addEventListener('pointercancel', soltar);
        svg.addEventListener('dblclick', function (e) {
            var asa = e.target.closest ? e.target.closest('.sgc-asas') : null;
            if (asa) eliminarVertice(parseInt(asa.getAttribute('data-i'), 10));
        });

        document.addEventListener('keydown', function (e) {
            if (!estado.abierto) return;
            var k = (e.key || '').toLowerCase();
            if ((e.ctrlKey || e.metaKey) && k === 'z' && !e.shiftKey) { e.preventDefault(); undo(); }
            else if ((e.ctrlKey || e.metaKey) && (k === 'y' || (k === 'z' && e.shiftKey))) { e.preventDefault(); redo(); }
            else if (e.key === 'Escape') { cerrar(false); }
        });
    }

    /* ------------------------------------------------------------------ API publica */
    window.SGCorte = {
        abrir: function (opener) { abrir(opener); },
        cerrar: function () { cerrar(false); },
        estado: function () { return { puntos: cloneP(estado.puntos), formaBase: estado.formaBase }; },
        puntosConfirmados: puntosConfirmados,
        /* El formulario lo llama al enviar: el detalle limpia el borrador solo si el POST tuvo exito */
        marcarEnviado: function () { try { localStorage.setItem(KEY_LIMPIAR, String(Date.now())); } catch (e) { /* idem */ } },
        aTexto: aTexto, desdeTexto: desdeTexto,
        /* ganchos de prueba (funciones puras; no los usa la aplicacion) */
        _puro: { autointersecta: autointersecta, formasBase: formasBase, bbox: bbox, perimetro: perimetro, snap: snap, aristaMasCercana: aristaMasCercana, metricas: metricas }
    };

    document.addEventListener('click', function (e) {
        var t = e.target.closest ? e.target.closest('[data-sg-corte]') : null;
        if (t) { e.preventDefault(); abrir(t); }
    });
})();
