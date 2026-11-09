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
/* Transicion entre paginas que el navegador cancela porque mostro la pagina nueva antes de terminar de
   leerla: la pagina sale sin animacion y Chrome deja un "InvalidStateError ... opt-in disabled" sin
   capturar. No es un fallo de SmartGlass, asi que ese unico aviso no se muestra; los demas siguen. */
addEventListener('unhandledrejection', function (e) {
  var r = e.reason;
  if (r && r.name === 'InvalidStateError' && /ViewTransition/.test(r.message || '')) e.preventDefault();
});

/* Sin manejadores en linea (la CSP solo deja correr scripts propios o con nonce):
   - <img data-quitar-si-falla>: si la imagen no carga, se quita y queda el respaldo de debajo.
   - <button data-imprimir>: abre la impresion. */
document.addEventListener('error', function (e) {
  var t = e.target;
  if (t && t.tagName === 'IMG' && t.hasAttribute('data-quitar-si-falla')) t.remove();
}, true);
document.addEventListener('click', function (e) {
  if (e.target.closest && e.target.closest('[data-imprimir]')) window.print();
});
