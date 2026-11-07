/* Acceso (nov. 2026): login y registro en la misma tarjeta. Los enlaces "Crea una" / "Inicia sesion"
   cambian de vista sin recargar y actualizan la direccion (/login o /registro), asi atras y adelante
   funcionan y sin JS los enlaces siguen llevando a cada pagina. La transicion: la vista actual se
   desvanece, dos mitades del isotipo se unen en el centro, una luz sube y destella, la tarjeta toma
   el alto nuevo y aparece la otra vista. Con movimiento reducido o el modo fotosensible: cambio directo. */
(function () {
  'use strict';
  var card = document.getElementById('acceso');
  if (!card) return;
  // Al iniciar sesion de nuevo, el panel de admin vuelve a mostrar su entrada (admin-shell.js)
  try { sessionStorage.removeItem('sg-adm-intro'); } catch (e) { /* sin almacenamiento */ }
  var vistas = {
    login: card.querySelector('.acceso-vista[data-vista="login"]'),
    registro: card.querySelector('.acceso-vista[data-vista="registro"]')
  };
  var forja = card.querySelector('.acceso-forja');
  var mitadA = forja.querySelector('.acceso-mitad-a');
  var mitadB = forja.querySelector('.acceso-mitad-b');
  var orbe = forja.querySelector('.acceso-orbe');
  var titulos = { login: card.getAttribute('data-titulo-login'), registro: card.getAttribute('data-titulo-registro') };
  var rutas = { login: card.getAttribute('data-ruta-login'), registro: card.getAttribute('data-ruta-registro') };
  var actual = card.getAttribute('data-vista') === 'registro' ? 'registro' : 'login';
  var ocupado = false;

  function quieto() {
    return (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
      /(^|\s)fotosensible(\s|$)/.test(document.documentElement.getAttribute('data-a11y') || '') ||
      typeof card.animate !== 'function';
  }
  function mostrar(v) {
    vistas.login.hidden = v !== 'login';
    vistas.registro.hidden = v !== 'registro';
    card.setAttribute('data-vista', v);
    if (titulos[v]) document.title = titulos[v];
    actual = v;
  }
  function enfocar(v) {
    var h = vistas[v].querySelector('h1');
    if (h) h.focus({ preventScroll: true });
  }
  function fin(a) { return a && a.finished ? a.finished.catch(function () {}) : Promise.resolve(); }

  function cambiar(v, empujar) {
    if (v === actual || !vistas[v] || ocupado) return;
    if (empujar) history.pushState({ vista: v }, '', rutas[v]);
    if (quieto()) { mostrar(v); enfocar(v); return; }
    ocupado = true;
    var viejo = vistas[actual], nuevo = vistas[v];
    var alto0 = card.offsetHeight, alto1;
    card.style.height = alto0 + 'px';
    var animaciones = [];

    // 1. La vista actual se desvanece con desenfoque
    var sale = viejo.animate(
      [{ opacity: 1, filter: 'blur(0)', transform: 'none' }, { opacity: 0, filter: 'blur(8px)', transform: 'scale(0.98)' }],
      { duration: 220, easing: 'ease-in', fill: 'forwards' });
    animaciones.push(sale);

    fin(sale).then(function () {
      // Alto de la vista nueva (se mide visible pero transparente)
      mostrar(v);
      nuevo.style.opacity = '0';
      card.style.height = 'auto';
      alto1 = card.offsetHeight;
      card.style.height = alto0 + 'px';

      // 2. La tarjeta toma el alto nuevo mientras el isotipo se arma
      forja.hidden = false;
      var curva = 'cubic-bezier(0.2, 0.8, 0.2, 1)';
      animaciones.push(card.animate([{ height: alto0 + 'px' }, { height: alto1 + 'px' }],
        { duration: 680, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'forwards' }));
      var a = mitadA.animate([{ transform: 'translate(-130px, -110px) rotate(-28deg)', opacity: 0 }, { transform: 'none', opacity: 1 }],
        { duration: 320, easing: curva, fill: 'forwards' });
      var b = mitadB.animate([{ transform: 'translate(130px, 110px) rotate(28deg)', opacity: 0 }, { transform: 'none', opacity: 1 }],
        { duration: 320, easing: curva, fill: 'forwards' });
      animaciones.push(a, b);
      return Promise.all([fin(a), fin(b)]);
    }).then(function () {
      // 3. Una luz sube hasta el isotipo y destella
      var luz = orbe.animate([
        { transform: 'translateY(150px) scale(0.4)', opacity: 0 },
        { transform: 'translateY(0) scale(1)', opacity: 1, offset: 0.55 },
        { transform: 'translateY(0) scale(4)', opacity: 0 }
      ], { duration: 380, easing: 'ease-out' });
      animaciones.push(luz);
      return fin(luz);
    }).then(function () {
      // 4. Aparece la vista nueva y el isotipo se apaga
      nuevo.style.opacity = '';
      var entra = nuevo.animate(
        [{ opacity: 0, filter: 'blur(8px)', transform: 'scale(0.98)' }, { opacity: 1, filter: 'blur(0)', transform: 'none' }],
        { duration: 300, easing: 'ease-out' });
      var apaga = forja.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 240, easing: 'ease-out', fill: 'forwards' });
      animaciones.push(entra, apaga);
      return Promise.all([fin(entra), fin(apaga)]);
    }).then(function () {
      forja.hidden = true;
      animaciones.forEach(function (x) { try { x.cancel(); } catch (e) { /* ya termino */ } });
      card.style.height = '';
      ocupado = false;
      enfocar(v);
    });
  }

  card.addEventListener('click', function (e) {
    var enlace = e.target.closest('a[data-ir]');
    if (!enlace || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return;
    e.preventDefault();
    cambiar(enlace.getAttribute('data-ir'), true);
  });
  window.addEventListener('popstate', function () {
    cambiar(/\/registro\/?$/.test(location.pathname) ? 'registro' : 'login', false);
  });
  history.replaceState({ vista: actual }, '', location.href);
})();
