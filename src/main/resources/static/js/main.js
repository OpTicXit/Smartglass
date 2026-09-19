/* ============================================================
   SmartGlass — script global de interaccion/estetica.
   Defensivo: cada bloque revisa si sus elementos existen antes
   de hacer nada, asi que es seguro incluirlo en toda pagina.
============================================================ */

document.addEventListener('DOMContentLoaded', () => {
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