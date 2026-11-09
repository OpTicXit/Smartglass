/* Recorrido de una pieza (inicio) como linea de produccion. Version sin costo de la skill "scroll world".
   Nov. 2026: ya no depende del scroll de la pagina. Cuando la seccion se ve en pantalla, las 5 estaciones
   avanzan solas cada INTERVALO ms, en bucle; el carro de vidrio y la barra avanzan con --p (registrado en
   recorrido.css) y la pieza del configurador (SGGlass.render) cambia en cada una.
   - Pausar / reanudar con su boton (WCAG 2.2.2). Tambien se detiene mientras el puntero o el foco estan
     dentro de la seccion y cuando la pestana no se ve.
   - Las estaciones y las flechas saltan a una estacion y reinician la cuenta.
   - Con movimiento reducido o el modo fotosensible empieza en pausa: la persona avanza a su ritmo, sin giros
     ni fundidos. */
(function () {
  'use strict';
  var s = document.getElementById('recorrido');
  if (!s || !window.SGGlass) return;
  var INTERVALO = 4200;
  var taller = s.querySelector('.linea-taller');
  var lienzo = document.getElementById('recorridoVidrio');
  var pieza = lienzo.parentElement;
  var caps = [].slice.call(s.querySelectorAll('.linea-capitulo'));
  var estaciones = [].slice.call(s.querySelectorAll('.linea-estacion'));
  var cuenta = s.querySelector('.linea-cuenta');
  var boton = s.querySelector('[data-rec-pausa]');
  var N = caps.length;
  var quieto = function () {
    return matchMedia('(prefers-reduced-motion: reduce)').matches || /fotosensible/.test(document.documentElement.getAttribute('data-a11y') || '');
  };

  var actual = -1, espera = 0, reloj = 0;
  var enPantalla = false, pausaPersona = quieto(), pausaPuntero = false;
  s.style.setProperty('--rec-ms', INTERVALO + 'ms');

  function activar(i) {
    if (i === actual) return;
    actual = i;
    s.style.setProperty('--p', (i / (N - 1)).toFixed(4));
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

  function corriendo() { return enPantalla && !pausaPersona && !pausaPuntero && !document.hidden; }

  /* Programa el siguiente paso; la barra de tiempo se reinicia quitando y poniendo la clase */
  function programar() {
    clearTimeout(reloj);
    s.classList.remove('corriendo');
    if (!corriendo()) return;
    void s.offsetWidth;
    s.classList.add('corriendo');
    reloj = setTimeout(function () { activar((actual + 1) % N); programar(); }, INTERVALO);
  }

  function pintarBoton() {
    if (!boton) return;
    var texto = boton.querySelector('span'), icono = boton.querySelector('i');
    boton.setAttribute('aria-pressed', String(pausaPersona));
    texto.textContent = boton.getAttribute(pausaPersona ? 'data-reanudar' : 'data-pausar') || (pausaPersona ? 'Reanudar' : 'Pausar');
    icono.className = 'ph ' + (pausaPersona ? 'ph-play' : 'ph-pause');
  }

  function ir(i) { activar((i + N) % N); programar(); }
  estaciones.forEach(function (e, i) { e.addEventListener('click', function () { ir(i); }); });
  s.querySelector('[data-ir="-1"]').addEventListener('click', function () { ir(actual - 1); });
  s.querySelector('[data-ir="1"]').addEventListener('click', function () { ir(actual + 1); });
  if (boton) boton.addEventListener('click', function () { pausaPersona = !pausaPersona; pintarBoton(); programar(); });

  // Mientras la persona lee o usa la seccion, no avanza sola
  s.addEventListener('pointerenter', function (e) { if (e.pointerType === 'mouse') { pausaPuntero = true; programar(); } });
  s.addEventListener('pointerleave', function (e) { if (e.pointerType === 'mouse') { pausaPuntero = false; programar(); } });
  s.addEventListener('focusin', function () { pausaPuntero = true; programar(); });
  s.addEventListener('focusout', function (e) { if (!s.contains(e.relatedTarget)) { pausaPuntero = false; programar(); } });
  document.addEventListener('visibilitychange', programar);

  if ('IntersectionObserver' in window) {
    new IntersectionObserver(function (entradas) {
      enPantalla = entradas[0].isIntersecting;
      programar();
    }, { threshold: 0.45 }).observe(taller);
  }

  activar(0);
  pintarBoton();
})();
