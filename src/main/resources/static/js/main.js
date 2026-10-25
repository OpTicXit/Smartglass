/* ============================================================
   SmartGlass — script global de interaccion.
   Defensivo: cada bloque revisa si sus elementos existen antes
   de hacer nada, asi que es seguro incluirlo en toda pagina.

   API publica (window.SG):
     SG.toast(mensaje, { tipo: 'success'|'error'|'warning'|'info', duracion })
     SG.confirmar({ titulo, mensaje, aceptar, cancelar, tono }) -> Promise<boolean>

   Atributos declarativos que entiende:
     data-confirm="Texto"            en <form> o <a>: pide confirmacion antes de seguir
       data-confirm-title / data-confirm-ok / data-confirm-tone="neutral"
     data-no-loading                 en <form>: no mostrar el estado "enviando"
     data-stepper                    contenedor con [data-step="-1|1"] + <input type=number>
     data-toast (+ data-toast-type)  elemento cuyo texto se muestra como toast al cargar
============================================================ */
(function () {
  'use strict';

  // Se consulta en vivo: el modo fotosensible (data-a11y) puede activarse sin recargar.
  // a11y-init.js hace que este matchMedia tambien responda true en ese modo.
  function menosMovimiento() {
    return (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) ||
      /(^|\s)fotosensible(\s|$)/.test(document.documentElement.getAttribute('data-a11y') || '');
  }
  var puntero = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;

  // T0 i18n: textos de window.SG_TXT (fragments/recursos.html, desde messages*.properties).
  // tx('clave', 'respaldo es-CO', arg0, arg1...) reemplaza {0}, {1}... Sin numeros del contrato (D2).
  function tx(clave, respaldo) {
    var txt = (window.SG_TXT && window.SG_TXT[clave]) || respaldo || '';
    var args = Array.prototype.slice.call(arguments, 2);
    return txt.replace(/\{(\d+)\}/g, function (m, i) { return args[i] != null ? args[i] : m; });
  }

  var ICONOS = {
    success: 'ph-check-circle',
    error: 'ph-warning-circle',
    warning: 'ph-warning',
    info: 'ph-info'
  };

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  ready(function () {
    // Cada inicializador va aislado: si uno falla, los demas siguen.
    [
      inicializarSaltoAlContenido,
      inicializarNavegacion,
      inicializarSelectorDeTema,
      inicializarAccesibilidad,
      inicializarIdioma,
      inicializarNotificaciones,
      inicializarTour,
      inicializarEvolucionVidrio,
      inicializarAlertas,
      inicializarToastsDeclarativos,
      inicializarConfirmaciones,
      inicializarEnvioDeFormularios,
      inicializarSteppers,
      inicializarVerPassword,
      inicializarImagenesDeRespaldo,
      revelarAlEntrar,
      contarAlEntrar,
      rebotarCarritoSiHuboExito,
      scrollSuaveEnAnclas,
      inicializarComparador,
      inicializarLuzDeCursorEnCards,
      inicializarEfectoMagnetico,
      pausarAnimacionesFuera
    ].forEach(function (fn) {
      try { fn(); } catch (e) { if (window.console) console.error('[SmartGlass] ' + fn.name, e); }
    });
  });

  /* ==========================================================
     TOASTS
  ========================================================== */
  var regionToasts = null;

  function toast(mensaje, opciones) {
    if (!mensaje) return null;
    opciones = opciones || {};
    var tipo = ICONOS[opciones.tipo] ? opciones.tipo : 'info';
    var duracion = opciones.duracion != null ? opciones.duracion : (tipo === 'error' ? 8000 : 5000);

    if (!regionToasts) {
      regionToasts = document.createElement('div');
      regionToasts.className = 'sg-toasts';
      regionToasts.setAttribute('role', 'region');
      regionToasts.setAttribute('aria-label', tx('notificaciones', 'Notificaciones'));
      document.body.appendChild(regionToasts);
    }

    var t = document.createElement('div');
    t.className = 'sg-toast sg-toast--' + tipo;
    t.setAttribute('role', tipo === 'error' ? 'alert' : 'status');

    var icono = document.createElement('i');
    icono.className = 'ph ' + ICONOS[tipo];
    icono.setAttribute('aria-hidden', 'true');

    var texto = document.createElement('p');
    texto.textContent = mensaje;

    var cerrar = document.createElement('button');
    cerrar.type = 'button';
    cerrar.setAttribute('aria-label', tx('cerrar_notificacion', 'Cerrar notificación'));
    cerrar.innerHTML = '<i class="ph ph-x" aria-hidden="true"></i>';

    t.append(icono, texto, cerrar);
    regionToasts.appendChild(t);

    var temporizador = null;
    function quitar() {
      clearTimeout(temporizador);
      if (t.classList.contains('is-leaving')) return;
      t.classList.add('is-leaving');
      t.addEventListener('animationend', function () { t.remove(); }, { once: true });
      if (menosMovimiento()) t.remove();
    }
    function programar() { if (duracion > 0) temporizador = setTimeout(quitar, duracion); }

    cerrar.addEventListener('click', quitar);
    t.addEventListener('mouseenter', function () { clearTimeout(temporizador); });
    t.addEventListener('mouseleave', programar);
    programar();
    return t;
  }

  function inicializarToastsDeclarativos() {
    document.querySelectorAll('[data-toast]').forEach(function (el) {
      var texto = el.textContent.trim();
      if (texto) toast(texto, { tipo: el.getAttribute('data-toast-type') || 'info' });
      el.remove();
    });
  }

  /* ==========================================================
     ALERTAS EN PAGINA
     Agrega un boton de cerrar a .alert y desvanece sola la de
     exito despues de unos segundos (las de error se quedan).
  ========================================================== */
  function inicializarAlertas() {
    document.querySelectorAll('.alert').forEach(function (alerta) {
      if (alerta.querySelector('.alert-close') || alerta.hasAttribute('data-persistente')) return;
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'alert-close';
      b.setAttribute('aria-label', tx('cerrar_aviso', 'Cerrar aviso'));
      b.innerHTML = '<i class="ph ph-x" aria-hidden="true"></i>';
      b.addEventListener('click', function () { cerrarAlerta(alerta); });
      alerta.appendChild(b);

      if (alerta.classList.contains('alert--success')) {
        setTimeout(function () { cerrarAlerta(alerta); }, 7000);
      }
    });
  }
  function cerrarAlerta(alerta) {
    if (!alerta.isConnected) return;
    if (menosMovimiento()) { alerta.remove(); return; }
    alerta.classList.add('is-leaving');
    alerta.addEventListener('animationend', function () { alerta.remove(); }, { once: true });
  }

  /* ==========================================================
     CONFIRMACIONES (reemplaza a window.confirm)
  ========================================================== */
  var dialogo = null;

  function confirmar(op) {
    op = op || {};
    if (typeof HTMLDialogElement === 'undefined') {
      return Promise.resolve(window.confirm(op.mensaje || op.titulo || tx('continuar', '¿Continuar?')));
    }
    if (!dialogo) {
      dialogo = document.createElement('dialog');
      dialogo.className = 'sg-dialog';
      dialogo.setAttribute('aria-labelledby', 'sg-dialog-titulo');
      dialogo.setAttribute('aria-describedby', 'sg-dialog-texto');
      dialogo.innerHTML =
        '<form method="dialog">' +
          '<div class="sg-dialog-body">' +
            '<div class="sg-dialog-icon" aria-hidden="true"><i class="ph ph-warning"></i></div>' +
            '<h2 id="sg-dialog-titulo"></h2>' +
            '<p id="sg-dialog-texto"></p>' +
          '</div>' +
          '<div class="sg-dialog-actions">' +
            '<button type="submit" value="cancelar" class="btn btn-secondary" data-cancelar></button>' +
            '<button type="submit" value="aceptar" class="btn" data-aceptar></button>' +
          '</div>' +
        '</form>';
      document.body.appendChild(dialogo);
    }

    var neutral = op.tono === 'neutral';
    dialogo.querySelector('#sg-dialog-titulo').textContent = op.titulo || tx('confirmar_titulo', '¿Confirmas esta acción?');
    dialogo.querySelector('#sg-dialog-texto').textContent = op.mensaje || '';
    dialogo.querySelector('.sg-dialog-icon').classList.toggle('is-neutral', neutral);
    dialogo.querySelector('.sg-dialog-icon i').className = 'ph ' + (neutral ? 'ph-question' : 'ph-warning');
    var ok = dialogo.querySelector('[data-aceptar]');
    ok.textContent = op.aceptar || tx('confirmar', 'Confirmar');
    ok.className = 'btn ' + (neutral ? 'btn-primary' : 'btn-danger');
    dialogo.querySelector('[data-cancelar]').textContent = op.cancelar || tx('cancelar', 'Cancelar');

    return new Promise(function (resolve) {
      dialogo.returnValue = '';
      dialogo.addEventListener('close', function () {
        resolve(dialogo.returnValue === 'aceptar');
      }, { once: true });
      dialogo.showModal();
      dialogo.querySelector('[data-cancelar]').focus();
    });
  }

  function opcionesDe(el) {
    return {
      titulo: el.getAttribute('data-confirm-title') || el.getAttribute('data-confirm'),
      mensaje: el.hasAttribute('data-confirm-title') ? el.getAttribute('data-confirm') : '',
      aceptar: el.getAttribute('data-confirm-ok'),
      tono: el.getAttribute('data-confirm-tone')
    };
  }

  function inicializarConfirmaciones() {
    // Formularios: se intercepta en captura para decidir antes que nadie
    document.addEventListener('submit', function (e) {
      var form = e.target;
      if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-confirm')) return;
      if (form.__sgConfirmado) { form.__sgConfirmado = false; return; }
      e.preventDefault();
      e.stopImmediatePropagation();
      var submitter = e.submitter || null;
      confirmar(opcionesDe(form)).then(function (si) {
        if (!si) return;
        form.__sgConfirmado = true;
        if (form.requestSubmit) form.requestSubmit(submitter && submitter.form === form ? submitter : undefined);
        else form.submit();
      });
    }, true);

    // Enlaces (p. ej. "Cerrar sesion")
    document.addEventListener('click', function (e) {
      var a = e.target.closest && e.target.closest('a[data-confirm]');
      if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey) return;
      e.preventDefault();
      confirmar(opcionesDe(a)).then(function (si) { if (si) window.location.href = a.href; });
    });
  }

  /* ==========================================================
     ENVIO DE FORMULARIOS: estado "enviando" y anti doble envio
  ========================================================== */
  function inicializarEnvioDeFormularios() {
    document.addEventListener('submit', function (e) {
      var form = e.target;
      if (!(form instanceof HTMLFormElement) || e.defaultPrevented) return;
      if (form.hasAttribute('data-no-loading') || form.method.toLowerCase() === 'dialog') return;
      if (form.getAttribute('target') === '_blank') return;

      if (form.__sgEnviando) { e.preventDefault(); return; }
      form.__sgEnviando = true;

      var boton = e.submitter && e.submitter.form === form ? e.submitter : form.querySelector('button[type="submit"], button:not([type])');
      // Se espera un tick: el navegador arma los datos del formulario
      // con el boton aun habilitado (si no, se perderia su name/value).
      setTimeout(function () {
        if (boton && boton.classList.contains('btn')) boton.classList.add('is-loading');
        form.querySelectorAll('button[type="submit"], button:not([type])').forEach(function (b) { b.disabled = true; });
        // Botones fuera del form con form="id"
        if (form.id) document.querySelectorAll('button[form="' + form.id + '"]').forEach(function (b) { b.disabled = true; });
      }, 0);
    });

    // Al volver con el boton "atras" (bfcache) se restaura todo
    window.addEventListener('pageshow', function (e) {
      if (!e.persisted) return;
      document.querySelectorAll('form').forEach(function (f) { f.__sgEnviando = false; });
      document.querySelectorAll('.btn.is-loading').forEach(function (b) { b.classList.remove('is-loading'); });
      document.querySelectorAll('button[disabled]').forEach(function (b) {
        if (!b.hasAttribute('data-disabled')) b.disabled = false;
      });
    });
  }

  /* ==========================================================
     SELECTOR DE CANTIDAD
  ========================================================== */
  function inicializarSteppers() {
    document.querySelectorAll('[data-stepper]').forEach(function (caja) {
      var input = caja.querySelector('input[type="number"]');
      if (!input) return;
      var min = input.min !== '' ? parseInt(input.min, 10) : 1;
      var max = input.max !== '' ? parseInt(input.max, 10) : 99;
      var botones = caja.querySelectorAll('[data-step]');

      function pintar() {
        var v = parseInt(input.value, 10);
        botones.forEach(function (b) {
          var paso = parseInt(b.getAttribute('data-step'), 10);
          b.disabled = paso < 0 ? v <= min : v >= max;
        });
      }
      botones.forEach(function (b) {
        b.addEventListener('click', function () {
          var v = parseInt(input.value, 10) || min;
          var nuevo = Math.min(max, Math.max(min, v + parseInt(b.getAttribute('data-step'), 10)));
          if (nuevo === v) return;
          input.value = nuevo;
          input.dispatchEvent(new Event('change', { bubbles: true }));
          pintar();
        });
      });
      input.addEventListener('change', function () {
        var v = parseInt(input.value, 10);
        if (isNaN(v) || v < min) input.value = min;
        else if (v > max) input.value = max;
        pintar();
      });
      pintar();
    });
  }

  /* ==========================================================
     MOSTRAR / OCULTAR CONTRASENA
     <div class="input-password"><input type="password"></div>
  ========================================================== */
  function inicializarVerPassword() {
    document.querySelectorAll('.input-password > input').forEach(function (input) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'ver-password';
      function pintar() {
        var visible = input.type === 'text';
        b.setAttribute('aria-label', visible ? tx('ocultar_password', 'Ocultar contraseña') : tx('mostrar_password', 'Mostrar contraseña'));
        b.setAttribute('aria-pressed', String(visible));
        b.innerHTML = '<i class="ph ' + (visible ? 'ph-eye-slash' : 'ph-eye') + '" aria-hidden="true"></i>';
      }
      b.addEventListener('click', function () {
        input.type = input.type === 'password' ? 'text' : 'password';
        pintar();
        input.focus();
      });
      // Al enviar, vuelve a ocultarse (que no quede en texto plano al volver atras)
      if (input.form) input.form.addEventListener('submit', function () { input.type = 'password'; pintar(); });
      pintar();
      input.parentNode.appendChild(b);
    });
  }

  /* ==========================================================
     IMAGENES SIN ARCHIVO: se reemplazan por un panel neutro
     (ultima red: las vistas ya usan CatalogoProducto.imagenUrl con el
      isotipo como respaldo F18; esto cubre archivos borrados o renombrados)
  ========================================================== */
  var RESPALDO =
    'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300">' +
      '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#16224a"/><stop offset="1" stop-color="#0b1330"/></linearGradient>' +
      '<linearGradient id="v" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#bfe3ff" stop-opacity=".35"/><stop offset="1" stop-color="#60a5fa" stop-opacity=".08"/></linearGradient></defs>' +
      '<rect width="400" height="300" fill="url(#g)"/>' +
      '<rect x="140" y="70" width="120" height="160" rx="4" fill="url(#v)" stroke="#cdecff" stroke-opacity=".55"/>' +
      '<path d="M150 210 L250 90" stroke="#fff" stroke-opacity=".25" stroke-width="10"/></svg>');

  function inicializarImagenesDeRespaldo() {
    function reparar(img) {
      if (img.__sgRespaldo) return;
      img.__sgRespaldo = true;
      img.src = RESPALDO;
      img.classList.add('img-respaldo');
    }
    document.addEventListener('error', function (e) {
      if (e.target && e.target.tagName === 'IMG' && !e.target.hasAttribute('data-sin-respaldo')) reparar(e.target);
    }, true);
    // Las que ya fallaron antes de que corriera este script
    document.querySelectorAll('img').forEach(function (img) {
      if (img.complete && img.naturalWidth === 0 && img.getAttribute('src') && !img.hasAttribute('data-sin-respaldo')) reparar(img);
    });
  }

  /* ==========================================================
     SCROLL-REVEAL: solo lo que esta por debajo del pliegue
     (lo visible al cargar aparece sin esperar).
  ========================================================== */
  function revelarAlEntrar() {
    if (menosMovimiento() || !('IntersectionObserver' in window)) return;
    var selectores = [
      '.producto-card', '.stat-box', '.stat-card', '.stat-pedido',
      '.pedido-card', '.cotizacion-card', '.obra-card', '.card-panel', '.pilar-box',
      '.info-box', '.tech-card', '.co2-card', '.dashboard-widget', '[data-reveal]'
    ];
    var limite = window.innerHeight * 0.92;
    var elementos = Array.prototype.filter.call(document.querySelectorAll(selectores.join(',')), function (el) {
      return el.getBoundingClientRect().top > limite && !el.closest('.no-reveal');
    });
    if (!elementos.length) return;

    elementos.forEach(function (el) { el.classList.add('sg-reveal'); });

    var observer = new IntersectionObserver(function (entries) {
      var visibles = entries.filter(function (en) { return en.isIntersecting; });
      visibles.forEach(function (entry, i) {
        entry.target.style.transitionDelay = Math.min(i * 50, 160) + 'ms';   // U2: antes 70/280 ms, se sentia lento al desplazar
        entry.target.classList.add('sg-reveal-visible');
        observer.unobserve(entry.target);
        // Se limpia el retraso para que no afecte al hover despues
        setTimeout(function () { entry.target.style.transitionDelay = ''; }, 1100);
      });
    }, { threshold: 0.06, rootMargin: '0px 0px -40px 0px' });

    elementos.forEach(function (el) { observer.observe(el); });
  }

  /* --- U2: las animaciones decorativas sin fin (borde arcoiris, paneles del inicio, vidrio del
     configurador, evolucion, fondo del login) se pausan mientras no se ven: repintaban cada
     fotograma aunque estuvieran fuera de pantalla. theme.css: .sg-fuera --- */
  function pausarAnimacionesFuera() {
    if (!('IntersectionObserver' in window)) return;
    var piezas = document.querySelectorAll('.sg-rainbow, .hero-visual, .sg-stage, .evolucion, .auth-visual');
    if (!piezas.length) return;
    var obs = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) { en.target.classList.toggle('sg-fuera', !en.isIntersecting); });
    }, { rootMargin: '80px 0px' });
    piezas.forEach(function (el) { obs.observe(el); });
  }

  /* --- T4: cifras que cuentan desde 0 al entrar en pantalla ---
     <strong data-contar="1234.5" data-decimales="1" data-sufijo=" kg">1.234,5 kg</strong>
     El servidor ya pinta el valor final (sin JS o con movimiento reducido se queda asi). Formato es-CO (D2). */
  function contarAlEntrar() {
    var els = document.querySelectorAll('[data-contar]');
    if (!els.length || menosMovimiento() || !('IntersectionObserver' in window)) return;
    function fmt(v, dec) {
      var p = Math.abs(v).toFixed(dec).split('.');
      return (v < 0 ? '-' : '') + p[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.') + (dec ? ',' + p[1] : '');
    }
    var obs = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (!en.isIntersecting) return;
        obs.unobserve(en.target);
        var el = en.target, fin = parseFloat(el.getAttribute('data-contar')) || 0;
        var dec = parseInt(el.getAttribute('data-decimales') || '0', 10), suf = el.getAttribute('data-sufijo') || '';
        if (menosMovimiento()) { el.textContent = fmt(fin, dec) + suf; return; }   // el modo puede activarse sin recargar
        var t0 = null, dur = 1100;
        function paso(t) {
          if (t0 === null) t0 = t;
          var k = Math.min(1, (t - t0) / dur), e = 1 - Math.pow(1 - k, 3);
          el.textContent = fmt(fin * e, dec) + suf;
          if (k < 1) requestAnimationFrame(paso); else el.textContent = fmt(fin, dec) + suf;
        }
        requestAnimationFrame(paso);
      });
    }, { threshold: 0.4 });
    Array.prototype.forEach.call(els, function (el) { obs.observe(el); });
  }

  /* --- Rebote del icono del carrito cuando hubo un "agregado" exitoso --- */
  function rebotarCarritoSiHuboExito() {
    var exito = document.querySelector('.alert--success, .alert-banner.alert-success, .flash-success, [data-carrito-actualizado]');
    if (!exito || menosMovimiento()) return;
    var carrito = document.querySelector('.nav-cart');
    if (!carrito) return;
    carrito.classList.add('sg-cart-bounce-premium');
    carrito.addEventListener('animationend', function () {
      carrito.classList.remove('sg-cart-bounce-premium');
    }, { once: true });
  }

  /* --- Scroll suave para anclas internas (href="#seccion") --- */
  function scrollSuaveEnAnclas() {
    document.addEventListener('click', function (event) {
      var link = event.target.closest && event.target.closest('a[href^="#"]');
      if (!link) return;
      var id = link.getAttribute('href');
      if (id.length < 2) return;
      var destino = null;
      try { destino = document.querySelector(id); } catch (e) { return; }
      if (!destino) return;
      event.preventDefault();
      destino.scrollIntoView({ behavior: menosMovimiento() ? 'auto' : 'smooth', block: 'start' });
      if (history.replaceState) history.replaceState(null, '', id);
    });
  }

  /* --- Saltar al contenido (accesibilidad de teclado) --- */
  function inicializarSaltoAlContenido() {
    var destino = document.querySelector('main') || document.querySelector('.container');
    if (!destino || document.querySelector('.skip-link')) return;
    if (!destino.id) destino.id = 'contenido';
    if (!destino.hasAttribute('tabindex')) destino.setAttribute('tabindex', '-1');
    var a = document.createElement('a');
    a.className = 'skip-link';
    a.href = '#' + destino.id;
    a.textContent = tx('saltar', 'Saltar al contenido');
    document.body.insertBefore(a, document.body.firstChild);
  }

  /* ==========================================================
     COMPARADOR: checkboxes .chk-comparar + barra flotante
     La seleccion se recuerda en sessionStorage, asi sobrevive
     a una busqueda o a volver desde una ficha.
  ========================================================== */
  function inicializarComparador() {
    var checks = Array.prototype.slice.call(document.querySelectorAll('.chk-comparar'));
    var barra = document.getElementById('barra-comparador');
    var contador = document.getElementById('comparador-count');
    var btnComparar = document.getElementById('btn-comparar');
    if (!checks.length || !barra || !contador || !btnComparar) return;

    var MAX = 3;
    var CLAVE = 'sg-comparar';
    var btnLimpiar = document.getElementById('btn-comparar-limpiar');

    var guardados = [];
    try { guardados = JSON.parse(sessionStorage.getItem(CLAVE) || '[]'); } catch (e) { guardados = []; }
    var seleccion = guardados.filter(function (id) { return typeof id === 'string'; }).slice(0, MAX);

    function guardar() { try { sessionStorage.setItem(CLAVE, JSON.stringify(seleccion)); } catch (e) { /* sin almacenamiento */ } }

    function pintar() {
      checks.forEach(function (c) {
        c.checked = seleccion.indexOf(c.value) !== -1;
        c.disabled = !c.checked && seleccion.length >= MAX;
        var card = c.closest('.producto-card');
        if (card) card.classList.toggle('is-comparando', c.checked);
      });
      contador.textContent = seleccion.length;
      var visible = seleccion.length >= 1;
      barra.classList.toggle('is-visible', visible);
      barra.style.display = visible ? 'flex' : 'none';
      btnComparar.disabled = seleccion.length < 2;
      var ayuda = barra.querySelector('[data-comparador-ayuda]');
      if (ayuda) ayuda.textContent = seleccion.length < 2 ? tx('comparar_min', 'Elige al menos 2 para comparar') : (seleccion.length >= MAX ? tx('comparar_tope', 'Máximo {0} productos', MAX) : tx('comparar_uno_mas', 'Puedes sumar uno más'));
    }

    checks.forEach(function (chk) {
      chk.addEventListener('change', function () {
        var i = seleccion.indexOf(chk.value);
        if (chk.checked && i === -1) {
          if (seleccion.length >= MAX) { chk.checked = false; toast(tx('comparar_max', 'Puedes comparar hasta {0} productos a la vez.', MAX), { tipo: 'warning' }); return; }
          seleccion.push(chk.value);
        } else if (!chk.checked && i !== -1) {
          seleccion.splice(i, 1);
        }
        guardar();
        pintar();
      });
    });

    if (btnLimpiar) btnLimpiar.addEventListener('click', function () { seleccion = []; guardar(); pintar(); });

    btnComparar.addEventListener('click', function () {
      if (seleccion.length < 2) return;
      var params = seleccion.map(function (id) { return 'ids=' + encodeURIComponent(id); }).join('&');
      try { sessionStorage.removeItem(CLAVE); } catch (e) { /* sin almacenamiento */ }
      window.location.href = '/comparar?' + params;
    });

    pintar();
  }

  /* ==========================================================
     LUZ QUE SIGUE AL CURSOR en las tarjetas
     Un solo listener delegado y como mucho una escritura por
     frame (requestAnimationFrame).
  ========================================================== */
  function inicializarLuzDeCursorEnCards() {
    if (!puntero) return;
    var selector = '.producto-card, .info-box, .tech-card, .card-panel, .dashboard-widget';
    if (!document.querySelector(selector)) return;

    var pendiente = null, frame = 0;
    document.addEventListener('pointermove', function (event) {
      var card = event.target.closest && event.target.closest(selector);
      if (!card) return;
      pendiente = { card: card, x: event.clientX, y: event.clientY };
      if (frame) return;
      frame = requestAnimationFrame(function () {
        frame = 0;
        if (!pendiente) return;
        var rect = pendiente.card.getBoundingClientRect();
        pendiente.card.style.setProperty('--mouse-x', (pendiente.x - rect.left) + 'px');
        pendiente.card.style.setProperty('--mouse-y', (pendiente.y - rect.top) + 'px');
      });
    }, { passive: true });
  }

  /* --- Efecto magnetico: .sg-magnetico "sigue" ligeramente al cursor --- */
  function inicializarEfectoMagnetico() {
    if (!puntero || menosMovimiento()) return;
    var INTENSIDAD = 0.2;
    document.querySelectorAll('.sg-magnetico').forEach(function (el) {
      el.addEventListener('pointermove', function (event) {
        var rect = el.getBoundingClientRect();
        var x = (event.clientX - rect.left - rect.width / 2) * INTENSIDAD;
        var y = (event.clientY - rect.top - rect.height / 2) * INTENSIDAD;
        el.style.transform = 'translate(' + x + 'px, ' + y + 'px)';
      });
      el.addEventListener('pointerleave', function () { el.style.transform = ''; });
    });
  }

  /* ==========================================================
     ACCESIBILIDAD GLOBAL (T1): modos "daltotonico" y "fotosensible"
     - <html data-a11y="daltotonico fotosensible"> lo pone a11y-init.js
       antes de pintar; el CSS global de theme.css responde a ese atributo.
     - localStorage 'sg-a11y' ("daltotonico,fotosensible") es la fuente
       inmediata; la cuenta (GET/PATCH /perfil/accesibilidad) sincroniza
       entre dispositivos. El usuario lo decide la sesion en el servidor.
     - El control vive en las barras (data-a11y-control); en las vistas sin
       barra se inyecta uno flotante junto al switch de tema.
  ========================================================== */
  function inicializarAccesibilidad() {
    var raiz = document.documentElement;
    var CLAVE = 'sg-a11y';
    var URL = '/perfil/accesibilidad';
    var VALIDOS = ['daltotonico', 'fotosensible'];
    var NOMBRES = { daltotonico: tx('a11y_nombre_dalt', 'colores para daltonismo'), fotosensible: tx('a11y_nombre_foto', 'sin animaciones') };
    function unir(modos) { return modos.map(function (m) { return NOMBRES[m]; }).join(tx('a11y_y', ' y ')); }

    function normalizar(lista) {
      return VALIDOS.filter(function (m) { return lista.indexOf(m) !== -1; });
    }
    function actuales() { return normalizar((raiz.getAttribute('data-a11y') || '').split(/\s+/)); }
    function desdeTexto(txt) { return normalizar(String(txt || '').split(',').map(function (x) { return x.trim(); })); }

    /* ---- Control flotante para vistas sin barra ---- */
    if (!document.querySelector('[data-a11y-control]')) {
      var flot = document.createElement('div');
      flot.className = 'nav-item has-menu nav-a11y a11y-flotante';
      flot.setAttribute('data-a11y-control', '');
      flot.innerHTML =
        '<button type="button" class="nav-link nav-trigger nav-a11y-trigger" aria-haspopup="true" aria-expanded="false" title="' + tx('a11y', 'Accesibilidad') + '">' +
          '<svg class="ico-a11y" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="7" r="1.3" fill="currentColor"/><path d="M7 10h10M12 10v4l-2.5 4.5M12 14l2.5 4.5"/></svg>' +
          '<span class="nav-a11y-texto visually-hidden">' + tx('a11y', 'Accesibilidad') + '</span>' +
        '</button>' +
        '<div class="nav-dropdown nav-dropdown--right a11y-panel" role="group" aria-label="' + tx('a11y_opciones', 'Opciones de accesibilidad') + '">' +
          '<p class="nav-head">' + tx('a11y', 'Accesibilidad') + '</p>' +
          '<button type="button" class="a11y-opcion" data-a11y-modo="daltotonico" aria-pressed="false"><span class="a11y-opcion-texto"><strong>' + tx('a11y_dalt', 'Colores para daltonismo') + '</strong><small>' + tx('a11y_dalt_det', 'Semáforo con icono, patrón y texto') + '</small></span><span class="a11y-switch" aria-hidden="true"></span></button>' +
          '<button type="button" class="a11y-opcion" data-a11y-modo="fotosensible" aria-pressed="false"><span class="a11y-opcion-texto"><strong>' + tx('a11y_foto', 'Sin animaciones ni destellos') + '</strong><small>' + tx('a11y_foto_det', 'Para fotosensibilidad o mareo por movimiento') + '</small></span><span class="a11y-switch" aria-hidden="true"></span></button>' +
          '<p class="a11y-estado" data-a11y-estado aria-live="polite"></p>' +
          idiomaMarkup() +
        '</div>';
      document.body.appendChild(flot);

      // Fuera de la barra no corre la logica de menus de inicializarNavegacion
      var disp = flot.querySelector('.nav-trigger');
      var abrirFlot = function (abierto) {
        flot.classList.toggle('open', abierto);
        disp.setAttribute('aria-expanded', String(abierto));
      };
      disp.addEventListener('click', function (e) { e.stopPropagation(); abrirFlot(!flot.classList.contains('open')); });
      document.addEventListener('click', function (e) { if (!flot.contains(e.target)) abrirFlot(false); });
      document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && flot.classList.contains('open')) { abrirFlot(false); disp.focus(); }
      });
    }

    var opciones = Array.prototype.slice.call(document.querySelectorAll('[data-a11y-modo]'));
    var estados = Array.prototype.slice.call(document.querySelectorAll('[data-a11y-estado]'));
    var controles = Array.prototype.slice.call(document.querySelectorAll('[data-a11y-control]'));

    function pintar(modos) {
      opciones.forEach(function (b) {
        b.setAttribute('aria-pressed', String(modos.indexOf(b.getAttribute('data-a11y-modo')) !== -1));
      });
      controles.forEach(function (c) {
        c.classList.toggle('is-activo', modos.length > 0);
        var disp = c.querySelector('.nav-a11y-trigger');
        if (disp) disp.setAttribute('aria-label', modos.length ? tx('a11y_aria_activo', 'Accesibilidad: {0} activado', unir(modos)) : tx('a11y', 'Accesibilidad'));
      });
    }

    var sesion = (function () {
      if (document.querySelector('.auth')) return false;                 // login / registro
      var nav = document.querySelector('.navbar[data-nav]');
      if (!nav) return true;                                              // checkout, factura...: se intenta (401 = sin sesion)
      if (nav.getAttribute('data-nav') === 'admin') return true;
      return nav.getAttribute('data-auth') === 'true';
    })();

    function guardarServidor(modos) {
      if (!sesion || !window.fetch) return Promise.resolve(null);
      return fetch(URL, {
        method: 'PATCH',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ preferencia: modos.length ? modos.join(',') : null })
      }).then(function (r) {
        if (r.status === 401) { sesion = false; return null; }
        return r.ok ? r.json() : Promise.reject(r.status);
      });
    }

    function aplicar(modos, op) {
      op = op || {};
      modos = normalizar(modos);
      if (modos.length) raiz.setAttribute('data-a11y', modos.join(' '));
      else raiz.removeAttribute('data-a11y');
      try {
        if (modos.length) localStorage.setItem(CLAVE, modos.join(','));
        else localStorage.removeItem(CLAVE);
      } catch (e) { /* sin almacenamiento: dura hasta recargar */ }
      pintar(modos);
      document.dispatchEvent(new CustomEvent('sg-a11y-change', { detail: { modos: modos } }));

      if (op.sincronizar) {
        var aviso = modos.length ? tx('a11y_activado', 'Activado: {0}.', unir(modos)) : tx('a11y_normal', 'Accesibilidad en modo normal.');
        guardarServidor(modos).then(function (r) {
          estados.forEach(function (e) { e.textContent = aviso + (r ? tx('a11y_guardado', ' Guardado en tu cuenta.') : ''); });
        }).catch(function () {
          estados.forEach(function (e) { e.textContent = aviso + tx('a11y_no_guardado', ' No se pudo guardar en tu cuenta; queda en este navegador.'); });
        });
      }
    }

    opciones.forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.stopPropagation();                                              // el menu queda abierto
        var modo = b.getAttribute('data-a11y-modo');
        var modos = actuales();
        var i = modos.indexOf(modo);
        if (i === -1) modos.push(modo); else modos.splice(i, 1);
        aplicar(modos, { sincronizar: true });
      });
    });

    // Otra pestana cambio la preferencia: esta la sigue
    window.addEventListener('storage', function (e) {
      if (e.key === CLAVE) aplicar(desdeTexto(e.newValue));
    });

    pintar(actuales());

    // Sincronizacion con la cuenta: lo guardado en el servidor manda; si el
    // servidor no tiene nada y este navegador si, se sube lo local.
    if (sesion && window.fetch) {
      fetch(URL, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (d) {
          if (!d) { sesion = false; return; }
          var remotos = desdeTexto(d.preferencia);
          var locales = actuales();
          if (remotos.length) {
            if (remotos.join() !== locales.join()) aplicar(remotos);
          } else if (locales.length) {
            guardarServidor(locales).catch(function () {});
          }
        })
        .catch(function () { /* sin red: queda lo local */ });
    }
  }

  /* ==========================================================
     IDIOMA (T0 i18n)
     Los enlaces ?lang=es-CO|es-ES|en funcionan sin JS (cookie sg-lang).
     Con JS: PATCH /perfil/idioma fija la cookie y, si hay sesion, lo guarda
     en la cuenta (usuarios.preferencia_idioma); luego se recarga la pagina.
     Regla D2: cambia los TEXTOS; los numeros del contrato siguen en es-CO.
  ========================================================== */
  var IDIOMAS = [
    { codigo: 'es-CO', clave: 'idioma_es_CO', nombre: 'Español (Colombia)' },
    { codigo: 'es-ES', clave: 'idioma_es_ES', nombre: 'Español (Castellano)' },
    { codigo: 'en', clave: 'idioma_en', nombre: 'English' }
  ];

  /** Seccion "Idioma" para el panel flotante (vistas sin barra). */
  function idiomaMarkup() {
    var actual = window.SG_LANG || 'es-CO';
    return '<div class="idioma-flotante" data-idioma-control>' +
      '<p class="nav-head">' + tx('idioma_titulo', 'Idioma') + '</p>' +
      IDIOMAS.map(function (i) {
        return '<a href="?lang=' + i.codigo + '" hreflang="' + i.codigo + '" lang="' + i.codigo + '" data-idioma="' + i.codigo + '"' +
          (i.codigo === actual ? ' aria-current="true"' : '') + '>' + tx(i.clave, i.nombre) + '</a>';
      }).join('') +
      '<p class="idioma-nota">' + tx('idioma_nota', 'Los precios y cifras se muestran siempre en formato colombiano.') + '</p>' +
      '</div>';
  }

  function inicializarIdioma() {
    document.addEventListener('click', function (e) {
      var a = e.target.closest && e.target.closest('a[data-idioma]');
      if (!a || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) return;
      e.preventDefault();
      var codigo = a.getAttribute('data-idioma');
      var destino = new URL(window.location.href);
      destino.searchParams.set('lang', codigo);
      if (codigo === window.SG_LANG) return;
      if (!window.fetch) { window.location.href = destino.toString(); return; }
      fetch('/perfil/idioma', {
        method: 'PATCH',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ idioma: codigo })
      }).then(function (r) {
        if (!r.ok) throw new Error(r.status);
        destino.searchParams.delete('lang');          // la cookie ya quedo puesta
        window.location.href = destino.toString();
      }).catch(function () { window.location.href = destino.toString(); });
    });
  }

  /* ==========================================================
     NOTIFICACIONES (T3) · campana de fragments/notificaciones.html
     Polling de GET /api/notificaciones cada 60 s, en pausa con la pestana
     oculta (sin WebSocket). Cache de 30 s en sessionStorage para que navegar
     no gaste el limite de F41. Todo se pinta con textContent (sin innerHTML).
     Visitante: "visto" se guarda en su navegador; con cuenta, en el servidor.
  ========================================================== */
  function inicializarNotificaciones() {
    var ctl = document.querySelector('[data-notif-control]');
    if (!ctl || !window.fetch) return;
    var boton = ctl.querySelector('.nav-notif-trigger');
    var contador = ctl.querySelector('[data-notif-count]');
    var estado = ctl.querySelector('[data-notif-estado]');
    var lista = ctl.querySelector('[data-notif-lista]');
    var marcar = ctl.querySelector('[data-notif-marcar]');
    var titulo = ctl.getAttribute('data-titulo') || 'Notificaciones';
    var nav = document.querySelector('.navbar[data-nav]');
    var conCuenta = !!nav && (nav.getAttribute('data-nav') === 'admin' || nav.getAttribute('data-auth') === 'true');
    var VISTO = 'sg-notif-visto', CACHE = 'sg-notif-cache-' + (conCuenta ? 'c' : 'v') + '-' + (window.SG_LANG || '');

    function leer(store, k) { try { return window[store].getItem(k); } catch (e) { return null; } }
    function escribir(store, k, v) { try { window[store].setItem(k, v); } catch (e) { /* almacenamiento bloqueado */ } }
    function interno(url) { return typeof url === 'string' && url.charAt(0) === '/' && url.charAt(1) !== '/'; }

    function fila(item, conEstado) {
      var li = document.createElement('li');
      var a = document.createElement('a');
      a.className = 'notif-item' + (conEstado && !item.leida ? ' is-nueva' : '');
      a.href = interno(item.enlace) ? item.enlace : '#';
      if (conEstado && !item.leida) {
        var tag = document.createElement('span');
        tag.className = 'notif-tag';
        tag.textContent = ctl.getAttribute('data-nueva') || 'Nueva';
        a.appendChild(tag);
      }
      var txt = document.createElement('span');
      txt.className = 'notif-texto';
      txt.textContent = item.texto;
      a.appendChild(txt);
      if (item.fecha) {
        var f = document.createElement('small');
        f.className = 'notif-fecha';
        f.textContent = item.fecha;
        a.appendChild(f);
      }
      if (conEstado && conCuenta && !item.leida && item.id) {
        a.addEventListener('click', function () {
          fetch('/api/notificaciones/' + encodeURIComponent(item.id) + '/leida',
                { method: 'PATCH', credentials: 'same-origin', keepalive: true });
          try { sessionStorage.removeItem(CACHE); } catch (e) { /* nada */ }
        });
      }
      li.appendChild(a);
      return li;
    }

    function seccion(clave, items) {
      var caja = ctl.querySelector('[data-notif-' + clave + '-box]');
      var ul = ctl.querySelector('[data-notif-' + clave + ']');
      if (!caja || !ul) return;
      ul.replaceChildren();
      (items || []).forEach(function (it) { ul.appendChild(fila(it, false)); });
      caja.hidden = !(items && items.length);
    }

    function pintar(d) {
      var n = d.noLeidas || 0;
      contador.textContent = n > 9 ? '9+' : String(n);
      contador.hidden = n === 0;
      boton.setAttribute('aria-label', titulo + ': ' + (d.etiqueta || ''));
      boton.title = titulo + ': ' + (d.etiqueta || '');
      lista.replaceChildren();
      (d.items || []).forEach(function (it) { lista.appendChild(fila(it, true)); });
      estado.textContent = (d.items && d.items.length) ? '' : (ctl.getAttribute('data-vacio') || '');
      estado.hidden = !!(d.items && d.items.length);
      marcar.hidden = n === 0;
      seccion('ofertas', d.ofertas);
      seccion('vendidos', d.masVendidos);
    }

    function cargar(forzar) {
      if (!forzar) {
        var c = leer('sessionStorage', CACHE);
        if (c) {
          try { var g = JSON.parse(c); if (Date.now() - g.t < 30000) { pintar(g.d); return; } } catch (e) { /* recarga */ }
        }
      }
      var url = '/api/notificaciones';
      var visto = !conCuenta && leer('localStorage', VISTO);
      if (visto) url += '?desde=' + encodeURIComponent(visto);
      fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
        .then(function (d) { escribir('sessionStorage', CACHE, JSON.stringify({ t: Date.now(), d: d })); pintar(d); })
        .catch(function () { estado.hidden = false; estado.textContent = ctl.getAttribute('data-error') || ''; });
    }

    marcar.addEventListener('click', function (e) {
      e.stopPropagation();
      if (!conCuenta) { escribir('localStorage', VISTO, String(Date.now())); cargar(true); return; }
      fetch('/api/notificaciones/leida', { method: 'PATCH', credentials: 'same-origin' })
        .then(function () { cargar(true); });
    });
    // inicializarNavegacion ya abrio/cerro el menu: al abrirlo se refresca (con la cache de 30 s)
    boton.addEventListener('click', function () { if (ctl.classList.contains('open')) cargar(false); });

    cargar(false);
    setInterval(function () { if (!document.hidden) cargar(true); }, 60000);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) cargar(false); });
  }

  /* ==========================================================
     RECORRIDO GUIADO (T4) · textos en fragments/tour.html (idioma activo)
     Aparece una sola vez (localStorage "sg-tour-visto") en las paginas publicas;
     se relanza con ?tour=1 o con [data-tour-iniciar] (/guia).
     Accesible: Esc o "Saltar" lo cierran; NO es modal ni atrapa el foco (Tab
     sale con normalidad); al cerrar el foco vuelve a donde estaba; el resaltado
     es estatico (sin animacion: fotosensible y movimiento reducido).
  ========================================================== */
  function inicializarTour() {
    var datos = document.getElementById('sg-tour-datos');
    if (!datos || !datos.content) return;
    var VISTO = 'sg-tour-visto';
    var pasos = Array.prototype.slice.call(datos.content.querySelectorAll('section')).map(function (s) {
      return { objetivo: s.getAttribute('data-objetivo'), titulo: s.querySelector('h2').textContent, texto: s.querySelector('p').textContent };
    });
    if (!pasos.length) return;
    function d(k, r) { return datos.getAttribute('data-' + k) || r; }
    var caja = null, i = 0, resaltado = null, focoPrevio = null;

    function visible(el) {
      var r = el.getBoundingClientRect();
      return r.width > 0 && r.height > 0 && getComputedStyle(el).visibility !== 'hidden';
    }
    /* El elemento de la barra para el paso; null si no se ve (menu movil cerrado) */
    function objetivo(nombre) {
      if (!nombre) return null;
      var c = document.querySelectorAll('.navbar [data-tour="' + nombre + '"]');
      for (var k = 0; k < c.length; k++) if (visible(c[k])) return c[k];
      return null;
    }
    function quitarResalte() {
      if (resaltado) resaltado.classList.remove('sg-tour-objetivo');
      resaltado = null;
    }
    function marcarVisto() { try { localStorage.setItem(VISTO, '1'); } catch (e) { /* almacenamiento bloqueado */ } }
    function boton(texto, clase, fn) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = clase;
      b.textContent = texto;
      b.addEventListener('click', fn);
      return b;
    }

    function pintar() {
      var p = pasos[i];
      quitarResalte();
      var t = objetivo(p.objetivo);
      if (t) { t.classList.add('sg-tour-objetivo'); resaltado = t; }
      caja.querySelector('.sg-tour-n').textContent = d('paso', 'Paso {0} de {1}').replace('{0}', i + 1).replace('{1}', pasos.length);
      var h = caja.querySelector('.sg-tour-titulo');
      h.textContent = p.titulo;
      caja.querySelector('.sg-tour-texto').textContent = p.texto;
      caja.querySelector('.sg-tour-ant').hidden = i === 0;
      caja.querySelector('.sg-tour-sig').textContent = i === pasos.length - 1 ? d('terminar', 'Terminar') : d('siguiente', 'Siguiente');
      h.focus();
    }
    function teclas(e) { if (e.key === 'Escape') { e.preventDefault(); cerrar(); } }
    function cerrar() {
      if (!caja) return;
      quitarResalte();
      caja.remove();
      caja = null;
      document.removeEventListener('keydown', teclas);
      marcarVisto();
      if (focoPrevio && focoPrevio.focus && document.contains(focoPrevio)) focoPrevio.focus();
    }
    function abrir() {
      if (caja) return;
      focoPrevio = document.activeElement;
      i = 0;
      caja = document.createElement('div');
      caja.className = 'sg-tour';
      caja.setAttribute('role', 'dialog');
      caja.setAttribute('aria-modal', 'false');
      caja.setAttribute('aria-labelledby', 'sg-tour-titulo');
      caja.setAttribute('aria-describedby', 'sg-tour-texto');

      var n = document.createElement('p'); n.className = 'sg-tour-n';
      var h = document.createElement('h2'); h.className = 'sg-tour-titulo'; h.id = 'sg-tour-titulo'; h.tabIndex = -1;
      var tx = document.createElement('p'); tx.className = 'sg-tour-texto'; tx.id = 'sg-tour-texto';
      var acc = document.createElement('div'); acc.className = 'sg-tour-acciones';
      acc.appendChild(boton(d('saltar', 'Saltar'), 'sg-tour-saltar', cerrar));
      acc.appendChild(boton(d('anterior', 'Anterior'), 'btn btn-secondary btn-sm sg-tour-ant', function () { i--; pintar(); }));
      acc.appendChild(boton(d('siguiente', 'Siguiente'), 'btn btn-primary btn-sm sg-tour-sig', function () {
        if (i === pasos.length - 1) cerrar(); else { i++; pintar(); }
      }));
      caja.appendChild(n); caja.appendChild(h); caja.appendChild(tx); caja.appendChild(acc);
      document.body.appendChild(caja);
      document.addEventListener('keydown', teclas);
      marcarVisto();                     // aparece una sola vez aunque se abandone a medias
      pintar();
    }

    document.querySelectorAll('[data-tour-iniciar]').forEach(function (b) { b.addEventListener('click', abrir); });
    var pedido = /[?&]tour=1(&|$)/.test(location.search);
    var visto;
    try { visto = localStorage.getItem(VISTO); } catch (e) { visto = '1'; }   // sin almacenamiento: no insistir
    var publica = document.querySelector('.navbar[data-nav="public"]');
    var excluida = document.querySelector('.auth') || /^\/(checkout|login|registro)/.test(location.pathname);
    if (pedido || (!visto && publica && !excluida)) abrir();
  }

  /* ==========================================================
     EVOLUCION DEL VIDRIO (panel de login y registro)
     Avanza solo por las 5 etapas (fragments/auth-visual.html). WCAG 2.2.2:
     se puede pausar y elegir el paso; se detiene con la pestana oculta.
     Con movimiento reducido o modo fotosensible no avanza solo.
  ========================================================== */
  function inicializarEvolucionVidrio() {
    var fig = document.querySelector('[data-evolucion]');
    if (!fig) return;

    var DURACION = 3600;
    var botones = Array.prototype.slice.call(fig.querySelectorAll('[data-evo-paso]'));
    var pausa = fig.querySelector('[data-evo-pausa]');
    var texto = fig.querySelector('.evo-texto');
    var lblPaso = fig.querySelector('#evo-paso');
    var lblTitulo = fig.querySelector('#evo-titulo');
    var lblTexto = fig.querySelector('#evo-texto');
    var total = botones.length;
    fig.style.setProperty('--evo-dur', DURACION + 'ms');

    /* Granos de la materia prima: un monton centrado en (200, 258) */
    var arena = fig.querySelector('.evo-arena');
    if (arena && !arena.childElementCount) {
      var NS = 'http://www.w3.org/2000/svg';
      for (var i = 0; i < 64; i++) {
        var x = 200 + (Math.random() + Math.random() + Math.random() - 1.5) * 92;
        var alto = Math.max(0, 1 - Math.pow((x - 200) / 112, 2)) * 58;
        var y = 258 - Math.random() * alto;
        var c = document.createElementNS(NS, 'circle');
        c.setAttribute('cx', x.toFixed(1));
        c.setAttribute('cy', y.toFixed(1));
        c.setAttribute('r', (2.2 + Math.random() * 3).toFixed(1));
        c.setAttribute('class', 'evo-grano g' + (1 + Math.floor(Math.random() * 4)));
        c.style.setProperty('--dx', (200 - x).toFixed(1) + 'px');
        c.style.setProperty('--dy', (240 - y).toFixed(1) + 'px');
        c.style.setProperty('--d', (Math.random() * 0.45).toFixed(2) + 's');
        arena.appendChild(c);
      }
    }

    var actual = parseInt(fig.getAttribute('data-etapa'), 10) || total;
    var temporizador = 0;
    var pausado = false;

    function mostrar(n, animarTexto) {
      actual = n;
      fig.setAttribute('data-etapa', String(n));
      var b = botones[n - 1];
      botones.forEach(function (x) { if (x === b) x.setAttribute('aria-current', 'step'); else x.removeAttribute('aria-current'); });
      lblPaso.textContent = tx('evo_paso', 'Paso {0} de {1}', n, total);
      lblTitulo.textContent = b.getAttribute('data-titulo');
      lblTexto.textContent = b.getAttribute('data-texto');
      if (animarTexto && !menosMovimiento()) {
        texto.classList.remove('is-cambiando');
        void texto.offsetWidth;                                   // reinicia la animacion del texto
        texto.classList.add('is-cambiando');
      }
      // Reinicia la barra de progreso del paso actual
      if (fig.classList.contains('is-reproduciendo')) {
        fig.classList.remove('is-reproduciendo');
        void fig.offsetWidth;
        fig.classList.add('is-reproduciendo');
      }
    }

    function detener() {
      clearInterval(temporizador);
      temporizador = 0;
      fig.classList.remove('is-reproduciendo');
    }
    function reproducir() {
      detener();
      if (pausado || menosMovimiento() || document.hidden) return;
      fig.classList.add('is-reproduciendo');
      mostrar(actual, false);
      temporizador = setInterval(function () { mostrar(actual % total + 1, true); }, DURACION);
    }
    function pintarPausa() {
      if (!pausa) return;
      var puede = !menosMovimiento();
      pausa.hidden = !puede;
      pausa.setAttribute('aria-pressed', String(pausado));
      pausa.innerHTML = '<i class="ph ' + (pausado ? 'ph-play' : 'ph-pause') + '" aria-hidden="true"></i>' +
        '<span class="visually-hidden">' + (pausado ? tx('evo_reanudar', 'Reanudar animación') : tx('evo_pausar', 'Pausar animación')) + '</span>';
    }

    botones.forEach(function (b, i) {
      b.addEventListener('click', function () {
        pausado = true;                                           // quien elige un paso, lo quiere quieto
        detener();
        mostrar(i + 1, true);
        pintarPausa();
      });
    });
    if (pausa) {
      pausa.addEventListener('click', function () {
        pausado = !pausado;
        pintarPausa();
        if (pausado) detener(); else reproducir();
      });
    }
    document.addEventListener('visibilitychange', function () { if (document.hidden) detener(); else reproducir(); });
    document.addEventListener('sg-a11y-change', function () { pintarPausa(); reproducir(); });

    pintarPausa();
    if (!menosMovimiento()) {
      mostrar(1, false);                                          // con movimiento empieza por la materia prima
      reproducir();
    }
  }

  /* ==========================================================
     SWITCH DE TEMA CLARO/OSCURO
     La eleccion se guarda en localStorage ('sg-theme') y se
     sincroniza entre pestanas. Si la pagina ya trae un
     <button class="sg-theme-switch"> solo se conecta; si no,
     se inyecta uno flotante.
  ========================================================== */
  function inicializarSelectorDeTema() {
    var raiz = document.documentElement;
    var CLAVE = 'sg-theme';
    var metaColor = document.querySelector('meta[name="theme-color"]');

    function leer() {
      try {
        var v = localStorage.getItem(CLAVE);
        return v === 'light' || v === 'dark' ? v : 'dark';
      } catch (e) { return 'dark'; }
    }

    var boton = document.querySelector('.sg-theme-switch');
    var yaEnPagina = Boolean(boton);

    if (!boton) {
      boton = document.createElement('button');
      boton.type = 'button';
      boton.className = 'sg-theme-switch';
      boton.setAttribute('role', 'switch');
      boton.innerHTML =
        '<span class="sg-theme-track" aria-hidden="true">' +
          '<span class="sg-theme-thumb">' +
            '<svg class="sg-ico-moon" viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>' +
            '<svg class="sg-ico-sun" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4" fill="currentColor"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>' +
          '</span>' +
        '</span>';
    }

    function aplicar(tema, guardar, animar) {
      if (animar) {
        raiz.classList.add('sg-theme-transition');
        setTimeout(function () { raiz.classList.remove('sg-theme-transition'); }, 600);
      }
      raiz.setAttribute('data-theme', tema);
      var claro = tema === 'light';
      boton.setAttribute('aria-checked', String(claro));
      boton.setAttribute('aria-label', claro ? tx('tema_claro_on', 'Modo claro activado. Cambiar a modo oscuro') : tx('tema_oscuro_on', 'Modo oscuro activado. Cambiar a modo claro'));
      boton.title = claro ? tx('tema_a_oscuro', 'Cambiar a modo oscuro') : tx('tema_a_claro', 'Cambiar a modo claro');
      if (metaColor) metaColor.setAttribute('content', claro ? '#F4F7FD' : '#070B19');
      if (guardar) {
        try { localStorage.setItem(CLAVE, tema); } catch (e) { /* sin almacenamiento */ }
      }
      document.dispatchEvent(new CustomEvent('sg-theme-change', { detail: { tema: tema } }));
    }

    // Revelado circular que nace en el interruptor. Sin View Transitions
    // (o con movimiento reducido) cae a un fundido o a un cambio directo.
    function cambiarTema(tema) {
      if (menosMovimiento()) { aplicar(tema, true, false); return; }
      if (!document.startViewTransition) { aplicar(tema, true, true); return; }

      var r = boton.getBoundingClientRect();
      var x = r.left + r.width / 2;
      var y = r.top + r.height / 2;
      var radio = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y));

      raiz.classList.add('sg-vt');
      var vt = document.startViewTransition(function () { aplicar(tema, true, false); });
      vt.ready.then(function () {
        raiz.animate(
          { clipPath: ['circle(0px at ' + x + 'px ' + y + 'px)', 'circle(' + radio + 'px at ' + x + 'px ' + y + 'px)'] },
          { duration: 700, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', pseudoElement: '::view-transition-new(root)' }
        );
      }).catch(function () {});
      vt.finished.finally(function () { raiz.classList.remove('sg-vt'); });
    }

    boton.addEventListener('click', function () {
      cambiarTema(raiz.getAttribute('data-theme') === 'light' ? 'dark' : 'light');
    });

    // Si otra pestana cambia el tema, esta lo sigue
    window.addEventListener('storage', function (e) {
      if (e.key === CLAVE && (e.newValue === 'light' || e.newValue === 'dark')) aplicar(e.newValue, false, true);
    });

    aplicar(leer(), false, false);
    if (!yaEnPagina) document.body.appendChild(boton);
  }

  /* ==========================================================
     NAVEGACION ORGANIZADA
     Funciona con el <nav class="navbar" data-nav="public|admin">
     de los fragmentos: marca la pagina activa, abre/cierra los
     menus, controla el menu movil y rellena la barra secundaria.
  ========================================================== */
  function inicializarNavegacion() {
    var nav = document.querySelector('.navbar[data-nav]');
    if (!nav) return;

    var path = location.pathname.replace(/\/+$/, '') || '/';

    // "=/ruta" = coincidencia exacta (ignora el context-path); sin "=" = contiene
    function coincide(patrones) {
      return (patrones || '').split('|').some(function (p) {
        p = p.trim();
        if (!p) return false;
        if (p.charAt(0) === '=') {
          var e = p.slice(1);
          if (e === '/') return path === '/';
          return path === e || path.slice(-e.length) === e;
        }
        return path.indexOf(p) !== -1;
      });
    }

    /* Pagina activa */
    nav.querySelectorAll('[data-match]').forEach(function (a) {
      if (!coincide(a.getAttribute('data-match'))) return;
      a.setAttribute('aria-current', 'page');
      var item = a.closest('.nav-item');
      if (item && item.classList.contains('has-menu')) item.classList.add('is-active');
    });

    /* Menus desplegables: clic, teclado y hover con retardo en escritorio */
    var menus = Array.prototype.slice.call(nav.querySelectorAll('.nav-item.has-menu'));
    function disparador(m) { return m.querySelector(':scope > .nav-trigger'); }
    function abrir(m) {
      cerrar(m);
      m.classList.add('open');
      var t = disparador(m);
      if (t) t.setAttribute('aria-expanded', 'true');
    }
    function cerrar(excepto) {
      menus.forEach(function (m) {
        if (m === excepto) return;
        m.classList.remove('open');
        var t = disparador(m);
        if (t) t.setAttribute('aria-expanded', 'false');
      });
    }
    var escritorio = window.matchMedia('(min-width: 951px) and (hover: hover)');

    menus.forEach(function (m) {
      var t = disparador(m);
      if (!t) return;
      var enlaces = function () { return Array.prototype.slice.call(m.querySelectorAll('.nav-dropdown a, .nav-dropdown button')); };
      var salida = 0;

      t.addEventListener('click', function (e) {
        e.stopPropagation();
        if (m.classList.contains('open')) cerrar(null); else abrir(m);
      });
      t.addEventListener('keydown', function (e) {
        if (e.key !== 'ArrowDown') return;
        e.preventDefault();
        abrir(m);
        var primero = enlaces()[0];
        if (primero) primero.focus();
      });
      // Flechas dentro del menu
      m.addEventListener('keydown', function (e) {
        if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
        var lista = enlaces();
        var i = lista.indexOf(document.activeElement);
        if (i === -1) return;
        e.preventDefault();
        var sig = lista[(i + (e.key === 'ArrowDown' ? 1 : -1) + lista.length) % lista.length];
        if (sig) sig.focus();
      });
      m.addEventListener('pointerenter', function (e) {
        if (e.pointerType !== 'mouse' || !escritorio.matches) return;
        clearTimeout(salida);
        abrir(m);
      });
      m.addEventListener('pointerleave', function (e) {
        if (e.pointerType !== 'mouse' || !escritorio.matches) return;
        salida = setTimeout(function () { if (!m.contains(document.activeElement)) cerrar(null); }, 220);
      });
      m.addEventListener('focusout', function (e) {
        if (!m.contains(e.relatedTarget)) {
          m.classList.remove('open');
          t.setAttribute('aria-expanded', 'false');
        }
      });
    });
    document.addEventListener('click', function (e) { if (!nav.contains(e.target)) cerrar(null); });
    document.addEventListener('keydown', function (e) {
      if (e.key !== 'Escape') return;
      var abierto = menus.filter(function (m) { return m.classList.contains('open'); })[0];
      cerrar(null);
      if (abierto) { var t = disparador(abierto); if (t) t.focus(); }
      if (nav.classList.contains('mobile-open') && toggle) { cerrarMovil(); toggle.focus(); }
    });

    /* Menu movil */
    var toggle = nav.querySelector('.nav-toggle');
    function cerrarMovil() {
      nav.classList.remove('mobile-open');
      if (toggle) toggle.setAttribute('aria-expanded', 'false');
    }
    if (toggle) {
      toggle.addEventListener('click', function () {
        var abierto = nav.classList.toggle('mobile-open');
        toggle.setAttribute('aria-expanded', String(abierto));
        toggle.setAttribute('aria-label', abierto ? tx('cerrar_menu', 'Cerrar menú') : tx('abrir_menu', 'Abrir menú'));
      });
      nav.querySelectorAll('.nav-panel a').forEach(function (a) { a.addEventListener('click', cerrarMovil); });
      window.matchMedia('(min-width: 951px)').addEventListener('change', function (e) { if (e.matches) cerrarMovil(); });
    }

    /* Altura real de la barra (con o sin barra secundaria) para los elementos sticky */
    function fijarAltura() { document.documentElement.style.setProperty('--nav-h', nav.offsetHeight + 'px'); }
    fijarAltura();
    if (window.ResizeObserver) new ResizeObserver(fijarAltura).observe(nav);
    else window.addEventListener('resize', fijarAltura);

    /* Sombra al desplazarse */
    var tickScroll = false;
    function alScroll() {
      if (tickScroll) return;
      tickScroll = true;
      requestAnimationFrame(function () {
        nav.classList.toggle('scrolled', window.scrollY > 24);
        tickScroll = false;
      });
    }
    window.addEventListener('scroll', alScroll, { passive: true });
    alScroll();

    /* Barra secundaria: opciones de la seccion actual (solo navbar publico) */
    var sub = nav.querySelector('.subnav');
    if (!sub) return;
    var SECCIONES = [
      { match: '/vidrio-personalizado', titulo: tx('sub_vidrio', 'Vidrio personalizado'), items: [
        { k: 'cotizar', texto: tx('sub_nueva_cot', 'Nueva cotización'), match: '=/vidrio-personalizado/cotizar' },
        { k: 'mis-cotizaciones', texto: tx('sub_mis_cot', 'Mis cotizaciones'), match: '/mis-cotizaciones|/vidrio-personalizado/cotizacion' }
      ] },
      { match: '=/usuario|/checkout/mis-pedidos|/checkout/pedido', titulo: tx('sub_cuenta', 'Mi cuenta'), items: [
        { k: 'usuario', texto: tx('sub_resumen', 'Resumen'), match: '=/usuario' },
        { k: 'mis-pedidos', texto: tx('sub_mis_pedidos', 'Mis pedidos'), match: '/checkout/mis-pedidos|/checkout/pedido' },
        { k: 'mis-cotizaciones', texto: tx('sub_mis_cot', 'Mis cotizaciones'), match: '/mis-cotizaciones' }
      ] },
      { match: '/catalogo|/destacados|/detalle|/comparar', titulo: tx('sub_tienda', 'Tienda'), items: [
        { k: 'catalogo', texto: tx('sub_catalogo', 'Catálogo'), match: '/catalogo|/detalle|/comparar' },
        { k: 'destacados', texto: tx('sub_destacados', 'Destacados'), match: '/destacados' }
      ] }
    ];
    var seccion = SECCIONES.filter(function (s) { return coincide(s.match); })[0];
    if (!seccion) return;
    var enlaces = seccion.items
      .map(function (it) { return { it: it, origen: nav.querySelector('[data-key="' + it.k + '"]') }; })
      .filter(function (x) { return x.origen; });
    if (enlaces.length < 2) return;

    var titulo = document.createElement('span');
    titulo.className = 'subnav-title';
    titulo.textContent = seccion.titulo;
    sub.append(titulo);
    enlaces.forEach(function (x) {
      var a = document.createElement('a');
      a.href = x.origen.getAttribute('href');
      a.textContent = x.it.texto;
      if (coincide(x.it.match)) a.setAttribute('aria-current', 'page');
      sub.append(a);
    });
    sub.hidden = false;
  }

  window.SG = window.SG || {};
  window.SG.toast = toast;
  window.SG.confirmar = confirmar;
})();
