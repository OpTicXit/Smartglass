/* CSRF (Spring Security): los fetch del mismo origen que cambian datos llevan la cabecera X-XSRF-TOKEN
   con el valor de la cookie XSRF-TOKEN. Los formularios con th:action ya traen el token oculto.
   A otros origenes (p. ej. la API externa de resistencia) no se envia nunca. */
(function () {
  var original = window.fetch;
  if (!original) return;
  window.fetch = function (recurso, opciones) {
    var url = typeof recurso === 'string' ? recurso : (recurso && recurso.url) || '';
    var metodo = ((opciones && opciones.method) || (recurso && recurso.method) || 'GET').toUpperCase();
    var mismoOrigen = !/^[a-z][a-z0-9+.-]*:/i.test(url) || url.indexOf(location.origin + '/') === 0;
    var token = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    if (mismoOrigen && token && metodo !== 'GET' && metodo !== 'HEAD') {
      opciones = Object.assign({}, opciones);
      var cabeceras = new Headers(opciones.headers || (typeof recurso === 'object' ? recurso.headers : undefined) || {});
      cabeceras.set('X-XSRF-TOKEN', decodeURIComponent(token[1]));
      opciones.headers = cabeceras;
    }
    return original.call(this, recurso, opciones);
  };
})();
