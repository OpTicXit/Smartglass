/* SmartGlass - informe tecnico: metricas y graficas del modelo, dibujadas en SVG desde el JSON que
   exporta "mineria de datos/exportar_informe.py". Los colores salen de CSS, asi siguen el tema.
   window.sgInformeDatos lo comparte con informe-globo.js. */
(function () {
    var main = document.getElementById('contenido');
    var NUM = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 });
    var ENTERO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
    var R2 = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
    var SVG = 'http://www.w3.org/2000/svg';
    var tip = document.getElementById('grafica-tip');

    window.sgInformeDatos = fetch(main.dataset.datos, { headers: { Accept: 'application/json' } })
        .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); });

    var nombreTipo = function (t) { return sgT('inf_t_' + t, t); };
    /* "tipo_vidrio_float" -> "Tipo: Float"; "proceso_recocido" -> "Proceso: recocido" */
    function nombreVariable(v) {
        var directo = window.SG_TXT['inf_v_' + v];
        if (directo) return directo;
        if (v.indexOf('tipo_vidrio_') === 0) return sgT('inf_v_tipo_vidrio') + ': ' + nombreTipo(v.slice(12));
        if (v.indexOf('proceso_') === 0) {
            var p = v.slice(8);
            return sgT('inf_v_proceso') + ': ' + (window.SG_TXT['inf_v_' + p] || nombreTipo(p).toLowerCase());
        }
        return v;
    }

    function nodo(tag, attrs, padre, texto) {
        var n = document.createElementNS(SVG, tag);
        for (var k in attrs) n.setAttribute(k, attrs[k]);
        if (texto != null) n.textContent = texto;
        if (padre) padre.appendChild(n);
        return n;
    }
    /* La tabla de datos es "details.g-tabla"; el otro details de la figura es el "¿Que estoy viendo?" */
    function lienzo(fig, ancho, alto) {
        var viejo = fig.querySelector('svg');
        if (viejo) viejo.remove();
        var titulo = fig.querySelector('figcaption [data-titulo]') || fig.querySelector('figcaption');
        var s = nodo('svg', { viewBox: '0 0 ' + ancho + ' ' + alto, role: 'img', 'aria-label': titulo.textContent });
        fig.insertBefore(s, fig.querySelector('details.g-tabla'));
        return s;
    }
    function escalaBonita(max) {
        var paso = Math.pow(10, Math.floor(Math.log10(max || 1)));
        var r = max / paso, n = r <= 1.5 ? 0.25 : r <= 3 ? 0.5 : r <= 6 ? 1 : 2;
        var marca = n * paso, ticks = [];
        for (var v = 0; v < max + marca * 0.999; v += marca) ticks.push(+v.toFixed(6));
        return ticks;
    }
    function compacto(v) {
        return Math.abs(v) >= 1000 ? NUM.format(v / 1000) + ' K' : NUM.format(v);
    }

    /* Hover: una sola caja que sigue al puntero */
    function mostrarTip(e, titulo, detalle) {
        tip.hidden = false;
        tip.textContent = titulo;
        if (detalle) { var s = document.createElement('small'); s.textContent = detalle; tip.appendChild(s); }
        var x = Math.min(e.clientX + 14, innerWidth - tip.offsetWidth - 8);
        var y = Math.min(e.clientY + 14, innerHeight - tip.offsetHeight - 8);
        tip.style.left = x + 'px'; tip.style.top = y + 'px';
    }
    function conTip(el, titulo, detalle) {
        el.addEventListener('pointermove', function (e) { mostrarTip(e, titulo, detalle); });
        el.addEventListener('pointerleave', function () { tip.hidden = true; });
    }

    /* Vista en tabla de cada grafica: nunca dependen solo del dibujo */
    function tabla(fig, cabeceras, filas) {
        var d = document.createElement('details');
        d.className = 'g-tabla';
        var s = document.createElement('summary'); s.textContent = sgT('inf_ver_tabla'); d.appendChild(s);
        var w = document.createElement('div'); w.className = 'table-wrap';
        var t = document.createElement('table'); t.className = 'table-admin';
        var cab = t.createTHead().insertRow();
        cabeceras.forEach(function (c) { var th = document.createElement('th'); th.textContent = c; cab.appendChild(th); });
        var cuerpo = t.createTBody();
        filas.forEach(function (f) { var r = cuerpo.insertRow(); f.forEach(function (c) { r.insertCell().textContent = c; }); });
        w.appendChild(t); d.appendChild(w); fig.appendChild(d);
    }
    function leyenda(fig, items) {
        var u = document.createElement('ul'); u.className = 'grafica-leyenda';
        items.forEach(function (it) {
            var li = document.createElement('li'), i = document.createElement('i');
            i.className = it[0]; li.appendChild(i); li.appendChild(document.createTextNode(it[1])); u.appendChild(li);
        });
        fig.insertBefore(u, fig.querySelector('svg') || fig.querySelector('details.g-tabla'));
    }

    /* Barras horizontales, una o dos series. Ancho real del contenedor: el texto no se escala */
    function barras(fig, etiquetas, series, opts) {
        var ancho = Math.max(280, fig.clientWidth - 36);
        var doble = series.length > 1, grueso = doble ? 8 : 14, hueco = doble ? 2 : 0, fila = doble ? 26 : 22;
        var izq = Math.min(opts.izquierda || 110, ancho * 0.42), der = 14, arriba = 4, abajo = 24;
        var alto = arriba + etiquetas.length * fila + abajo, util = ancho - izq - der;
        var max = Math.max.apply(null, series.map(function (s) { return Math.max.apply(null, s.valores); }));
        var ticks = escalaBonita(max), tope = ticks[ticks.length - 1];
        var s = lienzo(fig, ancho, alto);
        ticks.forEach(function (t) {
            var x = izq + util * t / tope;
            nodo('line', { x1: x, x2: x, y1: arriba, y2: alto - abajo, 'class': 'g-eje' }, s);
            nodo('text', { x: x, y: alto - 8, 'text-anchor': 'middle' }, s, opts.formato ? opts.formato(t) : compacto(t));
        });
        etiquetas.forEach(function (et, i) {
            var y = arriba + i * fila;
            var franja = nodo('rect', { x: 0, y: y, width: ancho, height: fila, 'class': 'g-fila' }, s);
            nodo('text', { x: izq - 8, y: y + fila / 2, 'text-anchor': 'end', 'dominant-baseline': 'central' }, s, et);
            series.forEach(function (ser, j) {
                var w = Math.max(1, util * ser.valores[i] / tope);
                var by = y + (fila - (grueso * series.length + hueco * (series.length - 1))) / 2 + j * (grueso + hueco);
                var r = Math.min(4, grueso / 2, w);
                /* Extremo del dato redondeado, base recta */
                nodo('path', {
                    d: 'M' + izq + ' ' + by + 'h' + (w - r) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + r + 'v' + (grueso - 2 * r) +
                       'a' + r + ' ' + r + ' 0 0 1 ' + (-r) + ' ' + r + 'h' + (r - w) + 'z',
                    'class': ser.clase, 'pointer-events': 'none'
                }, s);
            });
            conTip(franja, et, series.map(function (ser) {
                return (doble ? ser.nombre + ': ' : '') + (opts.detalle ? opts.detalle(ser.valores[i], i) : NUM.format(ser.valores[i]));
            }).join(' · '));
        });
    }

    function grafPaises(fig, p) {
        var filas = p.filas, en = /^en/.test(window.SG_LANG);
        var nombres = filas.map(function (f) { return en ? f.en : f.es; });
        var dibujar = function () {
            barras(fig, nombres, [{ clase: 'g-a', valores: filas.map(function (f) { return f.co2_mt; }) }], {
                izquierda: 150,
                detalle: function (v, i) { return sgT('inf_mt', '', ENTERO.format(v), NUM.format(filas[i].pct)); }
            });
        };
        tabla(fig, [sgT('inf_pais'), sgT('inf_mt_corto'), sgT('inf_pct')],
            filas.map(function (f, i) { return [nombres[i], ENTERO.format(f.co2_mt), NUM.format(f.pct)]; }));
        return dibujar;
    }

    function grafSimple(fig, d, titulo, unidad) {
        var nombres = d.claves.map(nombreTipo);
        tabla(fig, [sgT('inf_tipo'), titulo], nombres.map(function (n, i) { return [n, NUM.format(d.valores[i])]; }));
        return function () {
            barras(fig, nombres, [{ clase: 'g-a', valores: d.valores }], {
                detalle: function (v) { return NUM.format(v) + (unidad ? ' ' + unidad : ''); }
            });
        };
    }

    /* T7: los tipos con pocas piezas en el dataset llevan su n en la etiqueta: ahi la brecha es falta de ejemplos */
    var POCAS_PIEZAS = 150;
    function grafRealPredTipo(fig, d, tipos) {
        var cuantas = {};
        tipos.claves.forEach(function (k, i) { cuantas[k] = tipos.valores[i]; });
        var nombres = d.claves.map(function (k) {
            var n = cuantas[k];
            return nombreTipo(k) + (n != null && n < POCAS_PIEZAS ? ' (n=' + ENTERO.format(n) + ')' : '');
        });
        tabla(fig, [sgT('inf_tipo'), sgT('inf_real'), sgT('inf_predicho'), sgT('inf_n')],
            d.claves.map(function (k, i) { return [nombreTipo(k), NUM.format(d.real[i]), NUM.format(d.pred[i]), cuantas[k] != null ? ENTERO.format(cuantas[k]) : '—']; }));
        return function () {
            barras(fig, nombres, [
                { clase: 'g-b', nombre: sgT('inf_real'), valores: d.real },
                { clase: 'g-a', nombre: sgT('inf_predicho'), valores: d.pred }
            ], { izquierda: 150, detalle: function (v) { return NUM.format(v) + ' kg'; } });
            if (!fig.querySelector('.grafica-leyenda')) leyenda(fig, [['b', sgT('inf_real')], ['a', sgT('inf_predicho')], ['n', sgT('inf_n_nota')]]);
        };
    }

    function grafImportancia(fig, d) {
        var nombres = d.map(function (x) { return nombreVariable(x.variable); });
        var valores = d.map(function (x) { return x.valor; });
        tabla(fig, [sgT('inf_variable'), sgT('inf_importancia')], nombres.map(function (n, i) { return [n, NUM.format(valores[i])]; }));
        return function () { barras(fig, nombres, [{ clase: 'g-a', valores: valores }], { izquierda: 170 }); };
    }

    /* Matriz divergente: dos polos y gris neutro al centro, mezclados por CSS para seguir el tema */
    function grafCorrelacion(fig, d) {
        var nombres = d.variables.map(nombreVariable);
        tabla(fig, [sgT('inf_variable')].concat(nombres), d.valores.map(function (f, i) {
            return [nombres[i]].concat(f.map(function (v) { return NUM.format(v); }));
        }));
        return function () {
            var ancho = Math.max(280, fig.clientWidth - 36), n = nombres.length;
            var izq = Math.min(112, ancho * 0.3), abajo = 58, lado = (ancho - izq) / n;
            var alto = lado * n + abajo, s = lienzo(fig, ancho, alto);
            d.valores.forEach(function (fila, i) {
                fila.forEach(function (v, j) {
                    var pct = Math.round(Math.abs(v) * 100), polo = v >= 0 ? '--graf-b' : '--graf-a';
                    var c = nodo('rect', {
                        x: izq + j * lado + 1, y: i * lado + 1, width: lado - 2, height: lado - 2, rx: 4,
                        style: 'fill:color-mix(in oklab, var(' + polo + ') ' + pct + '%, var(--graf-medio))'
                    }, s);
                    conTip(c, nombres[i] + ' × ' + nombres[j], sgT('inf_correlacion') + ': ' + NUM.format(v));
                    if (lado >= 34) {
                        nodo('text', { x: izq + j * lado + lado / 2, y: i * lado + lado / 2, 'class': 'g-celda-texto ' + (pct >= 60 ? 'fuerte' + (v >= 0 ? ' polo-b' : '') : 'suave') }, s, NUM.format(Math.round(v * 100) / 100 || 0));
                    }
                });
                nodo('text', { x: izq - 8, y: i * lado + lado / 2, 'text-anchor': 'end', 'dominant-baseline': 'central' }, s, nombres[i]);
                var x = izq + i * lado + lado / 2, y = lado * n + 10;
                nodo('text', { x: x, y: y, 'text-anchor': 'end', transform: 'rotate(-32 ' + x + ' ' + y + ')' }, s, nombres[i]);
            });
        };
    }

    function ejesXY(s, ancho, alto, m, xt, yt, xs, ys, tituloX, tituloY) {
        yt.forEach(function (t) {
            nodo('line', { x1: m.i, x2: ancho - m.d, y1: ys(t), y2: ys(t), 'class': 'g-eje' }, s);
            nodo('text', { x: m.i - 6, y: ys(t), 'text-anchor': 'end', 'dominant-baseline': 'central' }, s, compacto(t));
        });
        xt.forEach(function (t) {
            nodo('text', { x: xs(t), y: alto - m.b + 16, 'text-anchor': 'middle' }, s, compacto(t));
        });
        nodo('text', { x: (m.i + ancho - m.d) / 2, y: alto - 4, 'text-anchor': 'middle', 'class': 'g-titulo-eje' }, s, tituloX);
        nodo('text', { x: 12, y: (m.a + alto - m.b) / 2, 'text-anchor': 'middle', 'class': 'g-titulo-eje', transform: 'rotate(-90 12 ' + (m.a + alto - m.b) / 2 + ')' }, s, tituloY);
    }

    function grafRealPred(fig, d) {
        var n = d.real.length;
        leyenda(fig, [['l', sgT('inf_perfecta')]]);
        return function () {
            var ancho = Math.max(280, fig.clientWidth - 36), alto = Math.round(ancho * 0.72);
            var m = { i: 52, d: 12, a: 8, b: 44 }, s = lienzo(fig, ancho, alto);
            var max = Math.max(Math.max.apply(null, d.real), Math.max.apply(null, d.pred));
            var ticks = escalaBonita(max), tope = ticks[ticks.length - 1];
            var xs = function (v) { return m.i + (ancho - m.i - m.d) * v / tope; };
            var ys = function (v) { return alto - m.b - (alto - m.a - m.b) * v / tope; };
            ejesXY(s, ancho, alto, m, ticks, ticks, xs, ys, sgT('inf_real') + ' (kg)', sgT('inf_predicho') + ' (kg)');
            var g = nodo('g', { 'pointer-events': 'none' }, s);
            for (var i = 0; i < n; i++) nodo('circle', { cx: xs(d.real[i]).toFixed(1), cy: ys(d.pred[i]).toFixed(1), r: 2.1, 'class': 'g-punto' }, g);
            nodo('line', { x1: xs(0), y1: ys(0), x2: xs(tope), y2: ys(tope), 'class': 'g-linea' }, s);
            /* Hover por cercania: un objetivo por punto seria inmanejable con miles */
            var resalte = nodo('circle', { r: 5, 'class': 'g-resalte', visibility: 'hidden' }, s);
            s.addEventListener('pointermove', function (e) {
                var caja = s.getBoundingClientRect(), k = ancho / caja.width;
                var px = (e.clientX - caja.left) * k, py = (e.clientY - caja.top) * k, mejor = -1, dist = 400;
                for (var j = 0; j < n; j++) {
                    var dx = xs(d.real[j]) - px, dy = ys(d.pred[j]) - py, q = dx * dx + dy * dy;
                    if (q < dist) { dist = q; mejor = j; }
                }
                if (mejor < 0) { resalte.setAttribute('visibility', 'hidden'); tip.hidden = true; return; }
                resalte.setAttribute('cx', xs(d.real[mejor])); resalte.setAttribute('cy', ys(d.pred[mejor]));
                resalte.setAttribute('visibility', 'visible');
                mostrarTip(e, sgT('inf_real') + ': ' + NUM.format(d.real[mejor]) + ' kg', sgT('inf_predicho') + ': ' + NUM.format(d.pred[mejor]) + ' kg');
            });
            s.addEventListener('pointerleave', function () { resalte.setAttribute('visibility', 'hidden'); tip.hidden = true; });
        };
    }

    function grafErrores(fig, d) {
        var rango = function (i) { return NUM.format(d.bordes[i]) + ' a ' + NUM.format(d.bordes[i + 1]); };
        tabla(fig, [sgT('inf_rango'), sgT('inf_frecuencia')], d.conteo.map(function (c, i) { return [rango(i), ENTERO.format(c)]; }));
        return function () {
            var ancho = Math.max(280, fig.clientWidth - 36), alto = Math.round(ancho * 0.6);
            var m = { i: 52, d: 12, a: 8, b: 44 }, s = lienzo(fig, ancho, alto);
            var ini = d.bordes[0], fin = d.bordes[d.bordes.length - 1];
            var yt = escalaBonita(Math.max.apply(null, d.conteo)), tope = yt[yt.length - 1];
            var xs = function (v) { return m.i + (ancho - m.i - m.d) * (v - ini) / (fin - ini); };
            var ys = function (v) { return alto - m.b - (alto - m.a - m.b) * v / tope; };
            var xt = escalaBonita(fin - ini).map(function (t) { return Math.round((ini + t) / 50) * 50; })
                .filter(function (t, i, a) { return t >= ini && t <= fin && a.indexOf(t) === i; });
            ejesXY(s, ancho, alto, m, xt, yt, xs, ys, sgT('inf_error'), sgT('inf_frecuencia'));
            d.conteo.forEach(function (c, i) {
                var x = xs(d.bordes[i]) + 1, w = Math.max(1, xs(d.bordes[i + 1]) - xs(d.bordes[i]) - 2), y = ys(c), h = alto - m.b - y, r = Math.min(4, w / 2, h);
                nodo('path', {
                    d: 'M' + x + ' ' + (y + h) + 'v' + (r - h) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + (-r) + 'h' + (w - 2 * r) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + r + 'v' + (h - r) + 'z',
                    'class': 'g-a', 'pointer-events': 'none'
                }, s);
                var hit = nodo('rect', { x: x - 1, y: m.a, width: w + 2, height: alto - m.a - m.b, 'class': 'g-fila' }, s);
                conTip(hit, rango(i) + ' kg', ENTERO.format(c) + ' ' + sgT('inf_frecuencia').toLowerCase());
            });
            nodo('line', { x1: xs(0), x2: xs(0), y1: m.a, y2: alto - m.b, 'class': 'g-linea' }, s);
        };
    }

    function pintar(datos) {
        var m = datos.modelo, met = m.metricas;
        document.getElementById('informe-anio').textContent = ' (' + datos.paises.anio + ')';
        document.getElementById('informe-meta').textContent = sgT('inf_generado', '', datos.generado, ENTERO.format(m.registros), m.arboles);

        var caja = document.getElementById('informe-metricas');
        [[R2.format(met.r2), 'R²'], [R2.format(met.r2_cv) + ' ± ' + R2.format(met.r2_cv_std), 'R² CV (5)'],
         [NUM.format(met.mae) + ' kg', 'MAE'], [NUM.format(met.rmse) + ' kg', 'RMSE']].forEach(function (x) {
            var d = document.createElement('div'), b = document.createElement('b'), s = document.createElement('small');
            b.textContent = x[0]; s.textContent = x[1]; d.appendChild(b); d.appendChild(s); caja.appendChild(d);
        });

        var ref = document.getElementById('informe-referencia');
        [[ref.dataset.media, m.referencia.media], [ref.dataset.lineal, m.referencia.lineal], ['Random Forest', met]].forEach(function (f) {
            var r = ref.insertRow();
            [f[0], NUM.format(f[1].mae), NUM.format(f[1].rmse), R2.format(f[1].r2)].forEach(function (c) { r.insertCell().textContent = c; });
        });

        var figuras = {};
        document.querySelectorAll('[data-grafica]').forEach(function (f) { figuras[f.dataset.grafica] = f; });
        var dibujos = [
            grafPaises(figuras.paises, datos.paises),
            grafSimple(figuras.tipos, m.tipos, sgT('inf_registros')),
            grafSimple(figuras.co2_tipo, m.co2_tipo, sgT('inf_kg'), 'kg'),
            grafCorrelacion(figuras.correlacion, m.correlacion),
            grafRealPred(figuras.real_pred, m.real_pred),
            grafErrores(figuras.errores, m.errores),
            grafImportancia(figuras.importancia, m.importancia),
            grafRealPredTipo(figuras.real_pred_tipo, m.real_pred_tipo, m.tipos)
        ];
        /* "¿Que estoy viendo?" del globo: Colombia como escala local, con el dato del mismo JSON */
        var col = datos.paises.filas.filter(function (f) { return f.iso === 'COL'; })[0];
        var extra = document.querySelector('[data-qv="paises"] [data-qv-extra]');
        if (col && extra) extra.textContent = sgT('inf_qv_colombia', '', NUM.format(col.co2_mt), NUM.format(col.pct));
        var redibujar = function () { dibujos.forEach(function (d) { d(); }); };
        redibujar();
        var espera, anchoPrevio = innerWidth;
        addEventListener('resize', function () {
            if (innerWidth === anchoPrevio) return;
            anchoPrevio = innerWidth;
            clearTimeout(espera); espera = setTimeout(redibujar, 150);
        });
    }

    /* Actividad en vivo: consulta la plataforma cada 30 s mientras la pestana esta a la vista */
    var CADA_MS = 30000, vivoFig = document.querySelector('[data-vivo-grafica]'), vivoEstado = document.getElementById('vivo-estado');
    var vivoMetricas = document.getElementById('vivo-metricas'), vivoHora = '';

    function grafVivo(dias) {
        var abierta = vivoFig.querySelector('details.g-tabla');
        var estabaAbierta = abierta && abierta.open;
        var ancho = Math.max(280, vivoFig.clientWidth - 36), alto = 190, m = { a: 8, b: 26, i: 44, d: 6 };
        var max = Math.max.apply(null, dias.map(function (d) { return d.co2Kg; }));
        var ticks = escalaBonita(max || 1), tope = ticks[ticks.length - 1];
        var s = lienzo(vivoFig, ancho, alto), util = alto - m.a - m.b, paso = (ancho - m.i - m.d) / dias.length;
        var y = function (v) { return m.a + util * (1 - v / tope); };
        ticks.forEach(function (t) {
            nodo('line', { x1: m.i, x2: ancho - m.d, y1: y(t), y2: y(t), 'class': 'g-eje' }, s);
            nodo('text', { x: m.i - 6, y: y(t), 'text-anchor': 'end', 'dominant-baseline': 'central' }, s, compacto(t));
        });
        dias.forEach(function (d, i) {
            var x = m.i + i * paso, fecha = d.fecha.slice(8) + '/' + d.fecha.slice(5, 7);
            var franja = nodo('rect', { x: x, y: m.a, width: paso, height: util, 'class': 'g-fila' }, s);
            if (d.co2Kg > 0) {
                var w = Math.max(2, paso - 2), h = Math.max(2, util * d.co2Kg / tope), r = Math.min(3, w / 2, h);
                nodo('path', {
                    d: 'M' + (x + 1) + ' ' + (m.a + util) + 'v' + (r - h) + 'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + (-r) + 'h' + (w - 2 * r) +
                       'a' + r + ' ' + r + ' 0 0 1 ' + r + ' ' + r + 'v' + (h - r) + 'z',
                    'class': 'g-a', 'pointer-events': 'none'
                }, s);
            }
            if ((dias.length - 1 - i) % 7 === 0) nodo('text', { x: x + paso / 2, y: alto - 8, 'text-anchor': 'middle' }, s, fecha);
            conTip(franja, fecha, NUM.format(d.co2Kg) + ' kg · ' + d.cotizaciones + ' ' + sgT('inf_vivo_cotizaciones').toLowerCase());
        });
        if (abierta) abierta.remove();
        tabla(vivoFig, [sgT('inf_vivo_fecha'), sgT('inf_kg'), sgT('inf_vivo_cotizaciones')], dias.map(function (d) {
            return [d.fecha, NUM.format(d.co2Kg), d.cotizaciones];
        }));
        vivoFig.querySelector('details.g-tabla').open = !!estabaAbierta;
    }

    function pintarVivo(a) {
        vivoHora = new Date().toLocaleTimeString(window.SG_LANG || 'es-CO');
        vivoEstado.classList.remove('con-error');
        vivoEstado.textContent = sgT('inf_vivo_estado', '', vivoHora);
        vivoMetricas.textContent = '';
        [[ENTERO.format(a.cotizaciones), sgT('inf_vivo_cotizaciones')], [ENTERO.format(a.pendientes), sgT('inf_vivo_pendientes')],
         [ENTERO.format(a.pedidos), sgT('inf_vivo_pedidos')], [NUM.format(a.co2Kg) + ' kg', sgT('inf_vivo_co2')],
         [ENTERO.format(a.cotizacionesSemana), sgT('inf_vivo_semana')], [NUM.format(a.co2KgSemana) + ' kg', sgT('inf_vivo_co2_semana')],
         [ENTERO.format(a.pedidosSemana), sgT('inf_vivo_pedidos') + ' · 7 d'],
         /* F3: un porcentaje sin base parece una cifra de toda la industria */
         [a.pctReciclado + ' %', sgT('inf_vivo_reciclado'), sgT('inf_vivo_base', '', ENTERO.format(a.cotizaciones))]].forEach(function (x) {
            var d = document.createElement('div'), b = document.createElement('b'), t = document.createElement('small');
            b.textContent = x[0]; t.textContent = x[1]; d.appendChild(b); d.appendChild(t);
            if (x[2]) { var base = document.createElement('small'); base.className = 'base'; base.textContent = x[2]; d.appendChild(base); }
            vivoMetricas.appendChild(d);
        });
        grafVivo(a.dias);
        vivoFig.sgDias = a.dias;
    }

    /* ---------- Proyecciones del CO2 estimado ----------
       "Simulacion" (JSON exportado, fin = null: la historia se ancla a hoy) y "Actividad de la plataforma"
       (en vivo). Forma: { fin, historia[], futuro[{y,lo,hi}], horizontes[{clave,dias,total,lo,hi}], metricas? }.
       La grafica muestra CO2 por periodo; la frase, el acumulado desde hoy hasta el horizonte. */
    var FECHA = new Intl.DateTimeFormat(window.SG_LANG || 'es-CO', { day: 'numeric', month: 'short', year: '2-digit' });
    function masDias(fecha, n) { var d = new Date(fecha.getTime()); d.setDate(d.getDate() + n); return d; }
    /* Solo bloques completos: uno incompleto (p. ej. el dia 365 en semanas) sumaria menos y la curva caeria sin motivo */
    function bloques(valores, tam, desdeElFinal) {
        var out = [], n = valores.length;
        if (desdeElFinal) {
            for (var fin = n; fin - tam >= 0; fin -= tam) out.unshift({ ini: fin - tam, fin: fin - 1 });
        } else {
            for (var ini = 0; ini + tam <= n; ini += tam) out.push({ ini: ini, fin: ini + tam - 1 });
        }
        return out;
    }
    function suma(lista, a, b, campo) {
        var s = 0;
        for (var k = a; k <= b; k++) s += campo ? lista[k][campo] : lista[k];
        return s;
    }

    function grafProyeccion(bloque, p, clave) {
        var fig = bloque.querySelector('[data-proy-grafica]');
        var h = p.horizontes.filter(function (x) { return x.clave === clave; })[0];
        if (!fig || !h) return;
        var abierta = fig.querySelector('details.g-tabla'), estabaAbierta = abierta && abierta.open;
        var tam = h.dias <= 31 ? 1 : (h.dias <= 366 ? 7 : 30);
        var hoy = p.fin ? new Date(p.fin + 'T12:00:00') : new Date();
        var historia = p.historia.slice(-Math.max(h.dias, 30));
        var futuro = p.futuro.slice(0, h.dias);
        var pasados = bloques(historia, tam, true).map(function (b) {
            return { v: suma(historia, b.ini, b.fin), desde: masDias(hoy, b.ini - historia.length + 1), hasta: masDias(hoy, b.fin - historia.length + 1) };
        });
        var proximos = bloques(futuro, tam, false).map(function (b) {
            return { v: suma(futuro, b.ini, b.fin, 'y'), lo: suma(futuro, b.ini, b.fin, 'lo'), hi: suma(futuro, b.ini, b.fin, 'hi'),
                     desde: masDias(hoy, b.ini + 1), hasta: masDias(hoy, b.fin + 1) };
        });
        var todos = pasados.concat(proximos);
        var max = Math.max.apply(null, todos.map(function (b) { return b.hi != null ? b.hi : b.v; }).concat([1]));
        var ticks = escalaBonita(max), tope = ticks[ticks.length - 1];
        var ancho = Math.max(280, fig.clientWidth - 36), alto = 230, m = { a: 10, b: 28, i: 52, d: 10 };
        var s = lienzo(fig, ancho, alto), util = alto - m.a - m.b;
        var paso = (ancho - m.i - m.d) / Math.max(1, todos.length - 1);
        var x = function (i) { return m.i + i * paso; };
        var y = function (v) { return m.a + util * (1 - v / tope); };
        ticks.forEach(function (t) {
            nodo('line', { x1: m.i, x2: ancho - m.d, y1: y(t), y2: y(t), 'class': 'g-eje' }, s);
            nodo('text', { x: m.i - 6, y: y(t), 'text-anchor': 'end', 'dominant-baseline': 'central' }, s, compacto(t));
        });
        var corte = pasados.length - 1;
        // Banda del rango probable (desde el ultimo punto conocido)
        if (proximos.length) {
            var arriba = ['M' + x(corte) + ' ' + y(pasados[corte] ? pasados[corte].v : 0)];
            proximos.forEach(function (b, i) { arriba.push('L' + x(corte + 1 + i) + ' ' + y(Math.min(tope, b.hi))); });
            for (var i = proximos.length - 1; i >= 0; i--) arriba.push('L' + x(corte + 1 + i) + ' ' + y(Math.max(0, proximos[i].lo)));
            arriba.push('Z');
            nodo('path', { d: arriba.join(' '), 'class': 'g-banda' }, s);
        }
        var linea = function (desde, lista, clase) {
            var d = lista.map(function (b, i) { return (i ? 'L' : 'M') + x(desde + i) + ' ' + y(b.v); }).join(' ');
            nodo('path', { d: d, 'class': clase }, s);
        };
        linea(0, pasados, 'g-historia');
        if (proximos.length) linea(corte, [pasados[corte]].concat(proximos), 'g-proyeccion');
        nodo('line', { x1: x(corte), x2: x(corte), y1: m.a, y2: m.a + util, 'class': 'g-hoy' }, s);
        nodo('circle', { cx: x(corte), cy: y(pasados[corte].v), r: 4, 'class': 'g-resalte' }, s);
        // Etiquetas: inicio, hoy y fin
        [[0, pasados[0].desde, 'start'], [corte, hoy, 'middle'], [todos.length - 1, todos[todos.length - 1].hasta, 'end']].forEach(function (e) {
            nodo('text', { x: x(e[0]), y: alto - 8, 'text-anchor': e[2] }, s, FECHA.format(e[1]));
        });
        // Zonas de lectura para el tooltip
        todos.forEach(function (b, i) {
            var zona = nodo('rect', { x: x(i) - paso / 2, y: m.a, width: Math.max(4, paso), height: util, 'class': 'g-fila' }, s);
            var rango = tam === 1 ? FECHA.format(b.desde) : FECHA.format(b.desde) + ' – ' + FECHA.format(b.hasta);
            conTip(zona, rango, NUM.format(b.v) + ' kg' + (b.hi != null ? ' (' + NUM.format(b.lo) + ' – ' + NUM.format(b.hi) + ')' : ''));
        });
        if (abierta) abierta.remove();
        tabla(fig, [sgT('inf_vivo_fecha'), sgT('inf_proy_futuro'), sgT('inf_proy_rango')], proximos.map(function (b) {
            return [FECHA.format(b.desde) + (tam > 1 ? ' – ' + FECHA.format(b.hasta) : ''), NUM.format(b.v), NUM.format(b.lo) + ' – ' + NUM.format(b.hi)];
        }));
        fig.querySelector('details.g-tabla').open = !!estabaAbierta;
    }

    function pintarProyeccion(bloque, p) {
        var ctl = bloque.querySelector('[data-proy-horizontes]');
        var resumen = bloque.querySelector('[data-proy-resumen]');
        var fig = bloque.querySelector('[data-proy-grafica]');
        var validacion = bloque.querySelector('[data-proy-validacion]');
        var claves = (p.horizontes || []).map(function (h) { return h.clave; });
        if (!claves.length) {
            ctl.hidden = true; fig.hidden = true;
            resumen.textContent = sgT('inf_proy_sin_datos');
            bloque.sgProy = null;
            return;
        }
        ctl.hidden = false; fig.hidden = false;
        var previa = bloque.sgProy && claves.indexOf(bloque.sgProy.clave) !== -1 ? bloque.sgProy.clave : null;
        var clave = previa || (claves.indexOf('anio') !== -1 ? 'anio' : claves[claves.length - 1]);
        function elegir(c) {
            clave = c;
            bloque.sgProy = { p: p, clave: c };
            Array.prototype.forEach.call(ctl.children, function (b) { b.setAttribute('aria-pressed', String(b.dataset.h === c)); });
            var h = p.horizontes.filter(function (x) { return x.clave === c; })[0];
            resumen.textContent = sgT('inf_proy_resumen', '', sgT('inf_proy_r_' + c), ENTERO.format(h.total), ENTERO.format(h.lo), ENTERO.format(h.hi));
            grafProyeccion(bloque, p, c);
        }
        if (ctl.dataset.claves !== claves.join()) {
            ctl.textContent = '';
            ctl.dataset.claves = claves.join();
            claves.forEach(function (c) {
                var b = document.createElement('button');
                b.type = 'button'; b.dataset.h = c; b.textContent = sgT('inf_proy_h_' + c, c);
                b.addEventListener('click', function () { elegir(c); });
                ctl.appendChild(b);
            });
        }
        if (validacion && p.metricas) {
            validacion.textContent = sgT('inf_proy_validacion', '', NUM.format(p.metricas.mae), NUM.format(p.metricas.mape_semanal), NUM.format(p.metricas.mae_ingenuo));
        }
        elegir(clave);
    }

    var bloquePlataforma = document.querySelector('[data-proyeccion-bloque="plataforma"]');
    var bloqueSimulacion = document.querySelector('[data-proyeccion-bloque="simulacion"]');
    function pedirJson(url) {
        return fetch(url, { headers: { Accept: 'application/json' } }).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); });
    }
    function sinProyeccion(bloque) {
        var r = bloque.querySelector('[data-proy-resumen]');
        r.textContent = sgT('inf_vivo_error', '', vivoHora || '-');
        r.classList.add('con-error');
    }
    if (bloqueSimulacion && main.dataset.simulacion) {
        pedirJson(main.dataset.simulacion).then(function (p) { pintarProyeccion(bloqueSimulacion, p); })
            .catch(function () { sinProyeccion(bloqueSimulacion); });
    }

    function consultarVivo() {
        if (document.hidden) return;
        fetch(main.dataset.vivo, { headers: { Accept: 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
            .then(pintarVivo)
            .catch(function () {
                vivoEstado.classList.add('con-error');
                vivoEstado.textContent = sgT('inf_vivo_error', '', vivoHora || '-');
            });
        if (bloquePlataforma && main.dataset.proyeccion) {
            pedirJson(main.dataset.proyeccion).then(function (p) {
                bloquePlataforma.querySelector('[data-proy-resumen]').classList.remove('con-error');
                pintarProyeccion(bloquePlataforma, p);
            }).catch(function () { sinProyeccion(bloquePlataforma); });
        }
    }
    consultarVivo();
    setInterval(consultarVivo, CADA_MS);
    document.addEventListener('visibilitychange', consultarVivo);
    addEventListener('resize', function () {
        if (vivoFig.sgDias) grafVivo(vivoFig.sgDias);
        [bloquePlataforma, bloqueSimulacion].forEach(function (b) { if (b && b.sgProy) grafProyeccion(b, b.sgProy.p, b.sgProy.clave); });
    });

    window.sgInformeDatos.then(pintar).catch(function () {
        var nota = document.querySelector('.informe-nota');
        nota.textContent = sgT('inf_error_datos');
        nota.setAttribute('role', 'alert');
    });
})();
