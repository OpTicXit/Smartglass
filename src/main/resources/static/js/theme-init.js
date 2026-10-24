/* SmartGlass - aplica el tema guardado ANTES de pintar la pagina
   (evita el parpadeo). Cargar sincrono en el <head> de cada vista. */
(function () {
  var tema = 'dark';
  try {
    var guardado = localStorage.getItem('sg-theme');
    if (guardado === 'light' || guardado === 'dark') tema = guardado;
  } catch (e) { /* almacenamiento bloqueado: se queda en oscuro */ }
  document.documentElement.setAttribute('data-theme', tema);
})();