/* U3 · Vista previa animada de los planes (fragments/plan-preview.html).
   - El selector Basico/Avanzado es un radiogroup (flechas, Inicio y Fin; tabindex movil).
   - Alterna solo cada 5,5 s mientras se ve; quien elige un plan lo quiere quieto (se pausa).
     El boton de pausa cumple WCAG 2.2.2. Fuera de pantalla o con la pestana oculta no corre.
   - Con movimiento reducido o el modo fotosensible: ni alterna ni anima (cambio inmediato).
   - La escena recorre las formas del plan con SGGlass.poligonoForma (glass-preview.js);
     sin ese archivo queda el rectangulo del HTML. Numeros con formato es-CO (D2). */
(function () {
  'use strict';

  var CLASICAS = ['Rectangular', 'Circular', 'Triangular', 'Irregular'];
  var AVANZADAS = ['EsquinasRedondeadas', 'Capsula', 'Arco', 'TrianguloRectangulo', 'TrapecioRectangulo',
                   'Fronton', 'Rombo', 'Hexagonal', 'Octogonal'];
  var ALTERNAR_MS = 5500, FORMA_MS = 1400;

  function menosMovimiento() {
    var a = document.documentElement.getAttribute('data-a11y') || '';
    return a.indexOf('fotosensible') !== -1 ||
      (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  }
  function fmt(n) { return String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, '.'); }

  function iniciar(raiz) {
    var botones = Array.prototype.slice.call(raiz.querySelectorAll('[data-pv-plan]'));
    var pausa = raiz.querySelector('[data-pv-pausa]');
    var resumen = raiz.querySelector('[data-pv-resumen]');
    var etiqueta = raiz.querySelector('[data-pv-etiqueta]');
    var pieza = raiz.querySelector('[data-pv-forma]');
    var nombreForma = raiz.querySelector('[data-pv-forma-nombre]');
    var G = window.SGGlass;
    var plan = raiz.getAttribute('data-plan') === 'BASICO' ? 'BASICO' : 'AVANZADO';
    var pausado = false, visible = true, tAlternar = 0, tForma = 0, iForma = 0, primera = true;

    /* Siluetas de los chips de forma (una sola vez) */
    Array.prototype.forEach.call(raiz.querySelectorAll('[data-pv-chip]'), function (chip) {
      var f = chip.getAttribute('data-pv-chip');
      chip.title = G && G.nombreForma ? G.nombreForma(f) : f;
      if (!G || !G.poligonoForma) return;
      var pts = G.poligonoForma(f, 16, 12, 24).map(function (p) { return (p.x + 1).toFixed(1) + ',' + (p.y + 1).toFixed(1); }).join(' ');
      chip.innerHTML = '<svg viewBox="0 0 18 14" aria-hidden="true"><polygon points="' + pts + '"/></svg>';
    });

    function formas() { return plan === 'AVANZADO' ? CLASICAS.concat(AVANZADAS) : CLASICAS; }

    function dibujarForma(f) {
      if (!G || !G.poligonoForma || !pieza) return;
      var w = 92, h = 64;
      pieza.setAttribute('points', G.poligonoForma(f, w, h, 40).map(function (p) {
        return (p.x + 14).toFixed(1) + ',' + (p.y + 10).toFixed(1);   // poligonoForma da {x, y}
      }).join(' '));
      if (nombreForma) nombreForma.textContent = G.nombreForma ? G.nombreForma(f) : f;
      Array.prototype.forEach.call(raiz.querySelectorAll('[data-pv-chip]'), function (c) {
        c.classList.toggle('is-actual', c.getAttribute('data-pv-chip') === f);
      });
    }

    function contar() {
      Array.prototype.forEach.call(raiz.querySelectorAll('[data-pv-n]'), function (el) {
        var fin = parseFloat(el.getAttribute('data-pv-n'));
        if (menosMovimiento()) { el.textContent = fmt(fin); return; }
        var t0 = performance.now(), dur = 900;
        (function paso(t) {
          var k = Math.min(1, (t - t0) / dur), e = 1 - Math.pow(1 - k, 3);
          el.textContent = fmt(fin * e);
          if (k < 1) requestAnimationFrame(paso);
        })(t0);
      });
    }

    function aplicar(nuevo, anunciar) {
      plan = nuevo;
      raiz.setAttribute('data-plan', plan);
      var basico = plan === 'BASICO';
      botones.forEach(function (b) {
        var si = b.getAttribute('data-pv-plan') === plan;
        b.setAttribute('aria-checked', String(si));
        b.tabIndex = si ? 0 : -1;
      });
      Array.prototype.forEach.call(raiz.querySelectorAll('[data-f]'), function (el) {
        el.classList.toggle('is-off', basico);
      });
      Array.prototype.forEach.call(raiz.querySelectorAll('[data-pv-f]'), function (li) {
        var fuera = basico && li.getAttribute('data-en-basico') !== 'true';
        li.classList.toggle('is-off', fuera);
        var est = li.querySelector('.pv-item-estado');
        if (est) est.textContent = est.getAttribute(fuera ? 'data-no' : 'data-si');
      });
      if (etiqueta) etiqueta.textContent = botones.filter(function (b) { return b.getAttribute('data-pv-plan') === plan; })[0].textContent;
      if (resumen) {
        var txt = resumen.getAttribute(basico ? 'data-basico' : 'data-avanzado');
        // aria-live solo cuando lo decide la persona (el cambio automatico no se anuncia cada 5 s)
        resumen.setAttribute('aria-live', anunciar ? 'polite' : 'off');
        resumen.textContent = txt;
      }
      if (!basico && !primera) contar();
      primera = false;
      iForma = 0;
      dibujarForma(formas()[0]);
    }

    function corriendo() { return !pausado && visible && !document.hidden && !menosMovimiento(); }

    function programar() {
      clearInterval(tAlternar); clearInterval(tForma); tAlternar = tForma = 0;
      if (pausa) {
        pausa.hidden = menosMovimiento();
        pausa.setAttribute('aria-pressed', String(pausado));
        var txt = pausa.getAttribute(pausado ? 'data-reanudar' : 'data-pausar');
        pausa.innerHTML = '<i class="ph ' + (pausado ? 'ph-play' : 'ph-pause') + '" aria-hidden="true"></i><span class="visually-hidden"></span>';
        pausa.lastChild.textContent = txt;
        pausa.title = txt;
      }
      if (!corriendo()) return;
      tAlternar = setInterval(function () { aplicar(plan === 'BASICO' ? 'AVANZADO' : 'BASICO', false); }, ALTERNAR_MS);
      tForma = setInterval(function () {
        var lista = formas();
        iForma = (iForma + 1) % lista.length;
        dibujarForma(lista[iForma]);
      }, FORMA_MS);
    }

    botones.forEach(function (b, i) {
      b.addEventListener('click', function () {
        pausado = true;
        aplicar(b.getAttribute('data-pv-plan'), true);
        programar();
      });
      b.addEventListener('keydown', function (e) {
        var j = { ArrowRight: i + 1, ArrowDown: i + 1, ArrowLeft: i - 1, ArrowUp: i - 1, Home: 0, End: botones.length - 1 }[e.key];
        if (j === undefined) return;
        e.preventDefault();
        var otro = botones[(j + botones.length) % botones.length];
        otro.focus();
        otro.click();
      });
    });
    if (pausa) pausa.addEventListener('click', function () { pausado = !pausado; programar(); });

    if ('IntersectionObserver' in window) {
      new IntersectionObserver(function (entries) {
        visible = entries[0].isIntersecting;
        programar();
      }, { threshold: 0 }).observe(raiz);   // en movil la vista mide mas que la pantalla: basta con verse en parte
    }
    document.addEventListener('visibilitychange', programar);
    document.addEventListener('sg-a11y-change', programar);

    aplicar(plan, false);
    programar();
  }

  function listo() { Array.prototype.forEach.call(document.querySelectorAll('[data-plan-preview]'), iniciar); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', listo); else listo();
})();
