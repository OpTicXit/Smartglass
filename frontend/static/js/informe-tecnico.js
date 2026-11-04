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
    function lienzo(fig, ancho, alto) {
        var viejo = fig.querySelector('svg');
        if (viejo) viejo.remove();
        var s = nodo('svg', { viewBox: '0 0 ' + ancho + ' ' + alto, role: 'img', 'aria-label': fig.querySelector('figcaption').textContent });
        var detalles = fig.querySelector('details');
        fig.insertBefore(s, detalles);
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
        fig.insertBefore(u, fig.querySelector('svg') || fig.querySelector('details'));
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

    function grafRealPredTipo(fig, d) {
        var nombres = d.claves.map(nombreTipo);
        tabla(fig, [sgT('inf_tipo'), sgT('inf_real'), sgT('inf_predicho')],
            nombres.map(function (n, i) { return [n, NUM.format(d.real[i]), NUM.format(d.pred[i])]; }));
        return function () {
            barras(fig, nombres, [
                { clase: 'g-b', nombre: sgT('inf_real'), valores: d.real },
                { clase: 'g-a', nombre: sgT('inf_predicho'), valores: d.pred }
            ], { detalle: function (v) { return NUM.format(v) + ' kg'; } });
            if (!fig.querySelector('.grafica-leyenda')) leyenda(fig, [['b', sgT('inf_real')], ['a', sgT('inf_predicho')]]);
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
                        nodo('text', { x: izq + j * lado + lado / 2, y: i * lado + lado / 2, 'class': 'g-celda-texto ' + (pct >= 60 ? 'fuerte' : 'suave') }, s, NUM.format(Math.round(v * 100) / 100 || 0));
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
            grafRealPredTipo(figuras.real_pred_tipo, m.real_pred_tipo)
        ];
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
        var abierta = vivoFig.querySelector('details');
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
        vivoFig.querySelector('details').open = !!estabaAbierta;
    }

    function pintarVivo(a) {
        vivoHora = new Date().toLocaleTimeString(window.SG_LANG || 'es-CO');
        vivoEstado.classList.remove('con-error');
        vivoEstado.textContent = sgT('inf_vivo_estado', '', vivoHora);
        vivoMetricas.textContent = '';
        [[ENTERO.format(a.cotizaciones), sgT('inf_vivo_cotizaciones')], [ENTERO.format(a.pendientes), sgT('inf_vivo_pendientes')],
         [ENTERO.format(a.pedidos), sgT('inf_vivo_pedidos')], [NUM.format(a.co2Kg) + ' kg', sgT('inf_vivo_co2')],
         [ENTERO.format(a.cotizacionesSemana), sgT('inf_vivo_semana')], [NUM.format(a.co2KgSemana) + ' kg', sgT('inf_vivo_co2_semana')],
         [ENTERO.format(a.pedidosSemana), sgT('inf_vivo_pedidos') + ' · 7 d'], [a.pctReciclado + ' %', sgT('inf_vivo_reciclado')]].forEach(function (x) {
            var d = document.createElement('div'), b = document.createElement('b'), t = document.createElement('small');
            b.textContent = x[0]; t.textContent = x[1]; d.appendChild(b); d.appendChild(t); vivoMetricas.appendChild(d);
        });
        grafVivo(a.dias);
        vivoFig.sgDias = a.dias;
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
    }
    consultarVivo();
    setInterval(consultarVivo, CADA_MS);
    document.addEventListener('visibilitychange', consultarVivo);
    addEventListener('resize', function () { if (vivoFig.sgDias) grafVivo(vivoFig.sgDias); });

    window.sgInformeDatos.then(pintar).catch(function () {
        var nota = document.querySelector('.informe-nota');
        nota.textContent = sgT('inf_error_datos');
        nota.setAttribute('role', 'alert');
    });
})();
