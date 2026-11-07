/* Recorrido de una pieza (inicio). Version sin costo de la skill "scroll world": en lugar de video
   pre-renderizado, el scroll mueve una camara (CSS 3D, variable --p) alrededor de la pieza del
   configurador (SGGlass.render) y cada capitulo cambia la pieza. Sin animacion con movimiento reducido
   o el modo fotosensible: la pieza cambia igual, pero sin giro ni fundido. */
(function () {
  'use strict';
  var seccion = document.getElementById('recorrido');
  if (!seccion || !window.SGGlass) return;
  var lienzo = document.getElementById('recorridoVidrio');
  var pieza = lienzo.parentElement;
  var dato = document.getElementById('recorridoDato');
  var datoValor = dato.querySelector('strong');
  var pasos = Array.prototype.slice.call(seccion.querySelectorAll('[data-paso]'));
  var puntos = seccion.querySelectorAll('.recorrido-ruta li');
  var quieto = function () {
    return matchMedia('(prefers-reduced-motion: reduce)').matches || /fotosensible/.test(document.documentElement.getAttribute('data-a11y') || '');
  };
  var actual = -1, espera = 0;

  function config(i) {
    var c = JSON.parse(pasos[i].getAttribute('data-paso'));
    return Object.assign({ ancho: 120, alto: 160, espesor: 8, forma: 'Rectangular' }, c);
  }

  function activar(i) {
    if (i === actual) return;
    actual = i;
    pasos.forEach(function (p, k) { p.classList.toggle('es-activo', k === i); });
    Array.prototype.forEach.call(puntos, function (d, k) { d.classList.toggle('es-activo', k === i); });
    var c = config(i);
    var pintar = function () {
      SGGlass.render(lienzo, c);
      pieza.classList.remove('cambiando');
      var r = SGGlass.calcular(c);
      if (pasos[i].hasAttribute('data-con-dato')) {
        datoValor.textContent = SGGlass.fmtNum(r.co2, 1) + ' kg';
        dato.classList.add('visible');
      } else {
        dato.classList.remove('visible');
      }
    };
    clearTimeout(espera);
    if (quieto()) { pintar(); return; }
    pieza.classList.add('cambiando');
    espera = setTimeout(pintar, 260);
  }

  /* Capitulo activo: el que cruza la mitad de la ventana */
  if ('IntersectionObserver' in window) {
    var obs = new IntersectionObserver(function (entradas) {
      entradas.forEach(function (e) { if (e.isIntersecting) activar(pasos.indexOf(e.target)); });
    }, { rootMargin: '-45% 0px -45% 0px' });
    pasos.forEach(function (p) { obs.observe(p); });
  }
  activar(0);

  /* Camara: progreso del scroll dentro de la seccion (0 a 1) */
  var pendiente = false;
  function medir() {
    pendiente = false;
    if (quieto()) { seccion.style.setProperty('--p', 0); return; }
    var r = seccion.getBoundingClientRect();
    var total = r.height - innerHeight;
    var p = total > 0 ? Math.min(1, Math.max(0, -r.top / total)) : 0;
    seccion.style.setProperty('--p', p.toFixed(4));
  }
  addEventListener('scroll', function () {
    if (!pendiente) { pendiente = true; requestAnimationFrame(medir); }
  }, { passive: true });
  addEventListener('resize', medir);
  medir();
})();
