/* Recorrido de una pieza (inicio) como linea de produccion. Version sin costo de la skill "scroll world":
   el scroll mueve --p (0 a 1) dentro de la pista; el carro de vidrio avanza por la banda, la estacion activa
   es la mas cercana y la pieza del configurador (SGGlass.render) cambia en cada una. Las estaciones y las
   flechas saltan a una estacion. Con movimiento reducido o el modo fotosensible: sin giro ni fundidos. */
(function () {
  'use strict';
  var s = document.getElementById('recorrido');
  if (!s || !window.SGGlass) return;
  var pista = s.querySelector('.linea-pista');
  var taller = s.querySelector('.linea-taller');
  var lienzo = document.getElementById('recorridoVidrio');
  var pieza = lienzo.parentElement;
  var caps = [].slice.call(s.querySelectorAll('.linea-capitulo'));
  var estaciones = [].slice.call(s.querySelectorAll('.linea-estacion'));
  var cuenta = s.querySelector('.linea-cuenta');
  var N = caps.length;
  var quieto = function () {
    return matchMedia('(prefers-reduced-motion: reduce)').matches || /fotosensible/.test(document.documentElement.getAttribute('data-a11y') || '');
  };

  var actual = -1, espera = 0;
  function activar(i) {
    if (i === actual) return;
    actual = i;
    caps.forEach(function (c, k) { c.classList.toggle('es-activo', k === i); c.classList.toggle('antes', k < i); });
    estaciones.forEach(function (e, k) {
      e.classList.toggle('es-activo', k === i); e.classList.toggle('antes', k < i);
      if (k === i) e.setAttribute('aria-current', 'step'); else e.removeAttribute('aria-current');
    });
    cuenta.textContent = (i + 1) + ' / ' + N;
    taller.setAttribute('data-luz', caps[i].getAttribute('data-luz') || '');
    var c = Object.assign({ ancho: 120, alto: 160, espesor: 8, forma: 'Rectangular' }, JSON.parse(caps[i].getAttribute('data-paso')));
    var pintar = function () {
      SGGlass.render(lienzo, c);
      pieza.classList.remove('cambiando');
      var dato = caps[i].querySelector('.linea-dato strong');
      if (dato) dato.textContent = SGGlass.fmtNum(SGGlass.calcular(c).co2, 1) + ' kg';
    };
    clearTimeout(espera);
    if (quieto()) { pintar(); return; }
    pieza.classList.add('cambiando');
    espera = setTimeout(pintar, 260);
  }

  function recorrido() { return Math.max(1, pista.offsetHeight - innerHeight); }
  var pendiente = false;
  function medir() {
    pendiente = false;
    var p = Math.min(1, Math.max(0, -pista.getBoundingClientRect().top / recorrido()));
    s.style.setProperty('--p', p.toFixed(4));
    activar(Math.round(p * (N - 1)));
  }
  function ir(i) {
    i = Math.max(0, Math.min(N - 1, i));
    var y = pista.getBoundingClientRect().top + scrollY + recorrido() * i / (N - 1);
    scrollTo({ top: Math.ceil(y), behavior: quieto() ? 'auto' : 'smooth' });
  }
  estaciones.forEach(function (e, i) { e.addEventListener('click', function () { ir(i); }); });
  s.querySelector('[data-ir="-1"]').addEventListener('click', function () { ir(actual - 1); });
  s.querySelector('[data-ir="1"]').addEventListener('click', function () { ir(actual + 1); });
  addEventListener('scroll', function () { if (!pendiente) { pendiente = true; requestAnimationFrame(medir); } }, { passive: true });
  addEventListener('resize', medir);
  medir();
})();
