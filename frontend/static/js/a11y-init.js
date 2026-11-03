/* SmartGlass - aplica el modo de accesibilidad guardado ANTES de pintar
   la pagina (evita el parpadeo). Hermano de theme-init.js: cargar sincrono
   en el <head> de cada vista (lo hace fragments/recursos.html).

   <html data-a11y="daltotonico | fotosensible | daltotonico fotosensible">
   - Sin atributo = modo normal.
   - Los modos van separados por ESPACIO en el atributo para que el CSS use
     [data-a11y~="..."]; en localStorage y en el backend se guardan
     separados por coma ("daltotonico,fotosensible").
   - La sincronizacion con la cuenta (GET/PATCH /perfil/accesibilidad) y el
     control de la barra los maneja main.js, despues de pintar. */
(function () {
  var MODOS = { daltotonico: 1, fotosensible: 1 };
  var valor = '';
  try { valor = localStorage.getItem('sg-a11y') || ''; } catch (e) { /* almacenamiento bloqueado */ }

  var modos = [];
  valor.split(',').forEach(function (m) {
    m = m.trim();
    if (MODOS[m] && modos.indexOf(m) === -1) modos.push(m);
  });
  modos.sort();

  var raiz = document.documentElement;
  if (modos.length) raiz.setAttribute('data-a11y', modos.join(' '));
  else raiz.removeAttribute('data-a11y');

  /* Guia automatizada en curso (js/guia.js): la pagina se atenua desde el primer cuadro para que no
     destelle entre un paso y otro. Donde la guia no corre (panel de administracion, errores) se retira. */
  try {
    if (sessionStorage.getItem('sg-guia')) {
      raiz.classList.add('sg-guia-activa');
      document.addEventListener('DOMContentLoaded', function () {
        if (!document.getElementById('sg-tour-datos') || document.querySelector('.pagina-error')) raiz.classList.remove('sg-guia-activa');
      });
    }
  } catch (e) { /* almacenamiento bloqueado */ }

  /* Modo fotosensible: matchMedia('(prefers-reduced-motion: reduce)') responde
     true para TODOS los scripts (glass-preview.js, el configurador, main.js...)
     sin tocarlos. Se evalua en cada llamada, asi refleja el modo vigente; los
     scripts que lo consultan una sola vez al cargar lo toman al recargar. */
  if (window.matchMedia) {
    var nativo = window.matchMedia.bind(window);
    window.matchMedia = function (consulta) {
      var mql = nativo(consulta);
      var esMovimiento = /prefers-reduced-motion\s*:\s*reduce/.test(String(consulta));
      var fotosensible = /(^|\s)fotosensible(\s|$)/.test(raiz.getAttribute('data-a11y') || '');
      if (!esMovimiento || !fotosensible || mql.matches) return mql;
      return {
        matches: true,
        media: mql.media,
        onchange: null,
        addEventListener: mql.addEventListener.bind(mql),
        removeEventListener: mql.removeEventListener.bind(mql),
        addListener: mql.addListener ? mql.addListener.bind(mql) : function () {},
        removeListener: mql.removeListener ? mql.removeListener.bind(mql) : function () {},
        dispatchEvent: mql.dispatchEvent.bind(mql)
      };
    };
  }
})();
