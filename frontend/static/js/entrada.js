/* Entrada tras iniciar sesion (nov. 2026), comun al admin y a los usuarios.
   La marca html.sg-entrada la pone el script en linea del menu (admin/fragments/nav.html y
   fragments/navbar-publico.html) solo en la primera pagina despues de iniciar sesion (acceso.js deja
   sg-entrada-pendiente). Aqui se arma la pantalla Azul Profundo: dos mitades del isotipo se unen,
   aparece el nombre y un circulo que crece desde la esquina superior izquierda revela la pagina.
   El nombre sale de data-nombre de este <script>. Estilos: theme.css "ENTRADA TRAS INICIAR SESION". */
(function () {
  'use strict';
  var raiz = document.documentElement;
  if (!raiz.classList.contains('sg-entrada')) return;
  var guion = document.currentScript;
  var terminado = false;
  var capa = document.createElement('div');
  function terminar() {
    if (terminado) return;
    terminado = true;
    raiz.classList.remove('sg-entrada', 'sg-entrada-lista');
    capa.remove();
  }
  setTimeout(terminar, 3200);   // nunca debe quedar tapando la pagina

  capa.className = 'sg-entrada-capa';
  capa.setAttribute('aria-hidden', 'true');
  var caja = document.createElement('span');
  caja.className = 'sg-entrada-logo';
  var logo = (guion && guion.getAttribute('data-logo')) || '/img/logo-icono-sm.png';
  [0, 1].forEach(function () { var im = document.createElement('img'); im.alt = ''; im.src = logo; caja.appendChild(im); });
  var nombre = document.createElement('p');
  nombre.className = 'sg-entrada-nombre';
  nombre.textContent = (guion && guion.getAttribute('data-nombre')) || 'SmartGlass';
  capa.append(caja, nombre);
  document.body.appendChild(capa);
  raiz.classList.add('sg-entrada-lista');

  var quieto = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    /(^|\s)fotosensible(\s|$)/.test(raiz.getAttribute('data-a11y') || '');
  if (typeof capa.animate !== 'function' || quieto) { terminar(); return; }

  var curva = 'cubic-bezier(0.2, 0.8, 0.2, 1)', mitades = caja.querySelectorAll('img');
  var uno = mitades[0].animate([{ transform: 'translate(-90px, -80px) rotate(-30deg)', opacity: 0 }, { transform: 'none', opacity: 1 }], { duration: 420, easing: curva, fill: 'both' });
  mitades[1].animate([{ transform: 'translate(90px, 80px) rotate(30deg)', opacity: 0 }, { transform: 'none', opacity: 1 }], { duration: 420, easing: curva, fill: 'both' });
  nombre.animate([{ opacity: 0, transform: 'translateY(6px)' }, { opacity: 1, transform: 'none' }], { duration: 320, delay: 300, easing: 'ease-out', fill: 'both' });
  uno.finished.then(function () {
    return new Promise(function (r) { setTimeout(r, 420); });
  }).then(function () {
    raiz.classList.remove('sg-entrada');   // la pagina arranca sus animaciones mientras se revela
    var radio = Math.hypot(innerWidth, innerHeight) + 40;
    return capa.animate([{ '--sg-hueco': '0px' }, { '--sg-hueco': radio + 'px' }],
      { duration: 720, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'forwards' }).finished;
  }).then(terminar, terminar);
})();
