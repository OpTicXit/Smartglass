/* ============================================================
   SmartGlass — script global de interaccion/estetica.
   Defensivo: cada bloque revisa si sus elementos existen antes
   de hacer nada, asi que es seguro incluirlo en toda pagina.
============================================================ */

document.addEventListener('DOMContentLoaded', () => {
  inicializarSelectorDeTema();
  revelarAlEntrar();
  rebotarCarritoSiHuboExito();
  scrollSuaveEnAnclas();
  inicializarComparador();
  inicializarLuzDeCursorEnCards();
  inicializarEfectoMagnetico();
});

/* --- Scroll-reveal: tarjetas aparecen con fade + slide al entrar --- */
function revelarAlEntrar() {
  const selectores = [
    '.producto-card', '.stat-box', '.stat-card', '.stat-pedido',
    '.pedido-card', '.cotizacion-card', '.card-panel', '.pilar-box',
    '.info-box', '.tech-card', '.co2-card', '.dashboard-widget'
  ];
  const elementos = document.querySelectorAll(selectores.join(','));
  if (!elementos.length) return;

  elementos.forEach(el => el.classList.add('sg-reveal'));

  if (!('IntersectionObserver' in window)) {
    elementos.forEach(el => el.classList.add('sg-reveal-visible'));
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry, index) => {
      if (entry.isIntersecting) {
        setTimeout(() => entry.target.classList.add('sg-reveal-visible'), index * 60);
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  elementos.forEach(el => observer.observe(el));
}

/* --- Rebote del icono del carrito cuando hubo un "agregado" exitoso --- */
function rebotarCarritoSiHuboExito() {
  const flashExito = document.querySelector('.alert-success, .flash-success');
  if (!flashExito) return;

  const linkCarrito = document.querySelector('a[href="/carrito"]');
  if (!linkCarrito) return;

  linkCarrito.classList.add('sg-cart-bounce-premium');
  linkCarrito.addEventListener('animationend', () => {
    linkCarrito.classList.remove('sg-cart-bounce-premium');
  }, { once: true });
}

/* --- Scroll suave para anclas internas (href="#seccion") --- */
function scrollSuaveEnAnclas() {
  document.querySelectorAll('a[href^="#"]').forEach(link => {
    link.addEventListener('click', (event) => {
      const destino = document.querySelector(link.getAttribute('href'));
      if (!destino) return;
      event.preventDefault();
      destino.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
}

/* --- Comparador de productos: checkboxes .chk-comparar + barra flotante --- */
function inicializarComparador() {
  const checks = document.querySelectorAll('.chk-comparar');
  const barra = document.getElementById('barra-comparador');
  const contador = document.getElementById('comparador-count');
  const btnComparar = document.getElementById('btn-comparar');
  if (!checks.length || !barra || !contador || !btnComparar) return;

  const MAX_SELECCION = 3;

  function seleccionados() {
    return Array.from(checks).filter(c => c.checked);
  }

  function actualizarBarra() {
    const sel = seleccionados();
    contador.textContent = sel.length;
    barra.style.display = sel.length >= 2 ? 'flex' : 'none';
    checks.forEach(c => {
      if (!c.checked) c.disabled = sel.length >= MAX_SELECCION;
    });
  }

  checks.forEach(chk => {
    chk.addEventListener('change', () => {
      const sel = seleccionados();
      if (sel.length > MAX_SELECCION) {
        chk.checked = false;
        return;
      }
      actualizarBarra();
    });
  });

  btnComparar.addEventListener('click', () => {
    const ids = seleccionados().map(c => c.value);
    if (ids.length < 2) return;
    const params = ids.map(id => 'ids=' + encodeURIComponent(id)).join('&');
    window.location.href = '/comparar?' + params;
  });

  actualizarBarra();
}

/* --- Luz que sigue al cursor en .producto-card/.info-box/.tech-card/.card-panel ---
   El CSS global (theme.css) ya dibuja el radial-gradient posicionado
   en las variables --mouse-x/--mouse-y; esta funcion es lo unico que
   falta: actualizar esas variables mientras el mouse se mueve dentro
   de cada card. Un solo listener delegado en document, no uno por
   card, para que no importe cuantas cards haya en la pagina. */
function inicializarLuzDeCursorEnCards() {
  const selector = '.producto-card, .info-box, .tech-card, .card-panel, .dashboard-widget';
  if (!document.querySelector(selector)) return;

  document.addEventListener('mousemove', (event) => {
    const card = event.target.closest(selector);
    if (!card) return;
    const rect = card.getBoundingClientRect();
    card.style.setProperty('--mouse-x', (event.clientX - rect.left) + 'px');
    card.style.setProperty('--mouse-y', (event.clientY - rect.top) + 'px');
  });
}

/* --- Efecto magnetico: el elemento "sigue" ligeramente al cursor ---
   Aplica a .sg-magnetico y al link del carrito (mismo selector que ya
   usa theme.css para la transicion). Se resetea al salir el mouse. */
function inicializarEfectoMagnetico() {
  const elementos = document.querySelectorAll('a[href="/carrito"], .sg-magnetico');
  if (!elementos.length) return;

  const INTENSIDAD = 0.25;

  elementos.forEach(el => {
    el.addEventListener('mousemove', (event) => {
      const rect = el.getBoundingClientRect();
      const offsetX = (event.clientX - rect.left - rect.width / 2) * INTENSIDAD;
      const offsetY = (event.clientY - rect.top - rect.height / 2) * INTENSIDAD;
      el.style.transform = `translate(${offsetX}px, ${offsetY}px)`;
    });

    el.addEventListener('mouseleave', () => {
      el.style.transform = 'translate(0, 0)';
    });
  });
}

/* --- Switch de tema claro/oscuro ---
   La eleccion se guarda en localStorage ('sg-theme'), asi que vale para
   TODAS las vistas y se sincroniza entre pestanas abiertas.
   - Si la pagina ya trae un <button class="sg-theme-switch"> (p. ej. dentro
     de un fragmento de navbar), este bloque solo lo conecta.
   - Si no, inyecta uno flotante en la esquina inferior derecha.
   Para evitar el parpadeo, cada pagina carga ademas theme-init.js en el
   <head>; si no lo tiene, aqui se aplica el tema igualmente. */
function inicializarSelectorDeTema() {
  const raiz = document.documentElement;
  const CLAVE = 'sg-theme';

  const leer = () => {
    try {
      const v = localStorage.getItem(CLAVE);
      return v === 'light' || v === 'dark' ? v : 'dark';
    } catch (e) { return 'dark'; }
  };

  let boton = document.querySelector('.sg-theme-switch');
  const yaEnPagina = Boolean(boton);

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
      setTimeout(() => raiz.classList.remove('sg-theme-transition'), 600);
    }
    raiz.setAttribute('data-theme', tema);
    const claro = tema === 'light';
    boton.setAttribute('aria-checked', String(claro));
    boton.setAttribute('aria-label', claro ? 'Modo claro activado. Cambiar a modo oscuro' : 'Modo oscuro activado. Cambiar a modo claro');
    boton.title = claro ? 'Cambiar a modo oscuro' : 'Cambiar a modo claro';
    if (guardar) {
      try { localStorage.setItem(CLAVE, tema); } catch (e) { /* sin almacenamiento */ }
    }
    document.dispatchEvent(new CustomEvent('sg-theme-change', { detail: { tema } }));
  }

  // Cambio con revelado circular que nace en el interruptor. Si el navegador
  // no soporta View Transitions (o el usuario pidio menos movimiento) cae a un
  // fundido de colores / cambio directo.
  function cambiarTema(tema) {
    const reducir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducir) { aplicar(tema, true, false); return; }
    if (!document.startViewTransition) { aplicar(tema, true, true); return; }

    const r = boton.getBoundingClientRect();
    const x = r.left + r.width / 2;
    const y = r.top + r.height / 2;
    const radio = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y));

    raiz.classList.add('sg-vt');
    const vt = document.startViewTransition(() => aplicar(tema, true, false));
    vt.ready.then(() => {
      raiz.animate(
        { clipPath: ['circle(0px at ' + x + 'px ' + y + 'px)', 'circle(' + radio + 'px at ' + x + 'px ' + y + 'px)'] },
        { duration: 750, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', pseudoElement: '::view-transition-new(root)' }
      );
    }).catch(() => {});
    vt.finished.finally(() => raiz.classList.remove('sg-vt'));
  }

  boton.addEventListener('click', () => {
    cambiarTema(raiz.getAttribute('data-theme') === 'light' ? 'dark' : 'light');
  });

  // Si otra pestana cambia el tema, esta lo sigue
  window.addEventListener('storage', (e) => {
    if (e.key === CLAVE && (e.newValue === 'light' || e.newValue === 'dark')) {
      aplicar(e.newValue, false, true);
    }
  });

  aplicar(leer(), false, false);
  if (!yaEnPagina) document.body.appendChild(boton);
}