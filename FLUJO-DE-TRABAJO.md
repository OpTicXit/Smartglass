# FLUJO DE TRABAJO · SmartGlass

Estado de cada ítem **leído contra el código del repositorio** (rama `mejoras-responsive-plano-saas`, actualizado el 2026-10-25 con T4 · estética escrito). Sustituye a cualquier versión anterior de este documento.

Ramas de trabajo: `cierre-bloqueantes` (ítems F y H) → `ux-accesibilidad-catalogo` (commit `8c13416`) → `comercio-stock-notif` (T0 en `fe2dfe8`; T1 y T2 en `c4cc809`; T3 en `57aea55`; T4 en `c6d8c16`; todo integrado en `main` local) → `mejoras-responsive-plano-saas` (T1 responsive en `901a93f`; T2 plano y cotizaciones en `d5f5ab6`; T3 sostenibilidad en `bed4199`; T4 estética **sin commitear**). No se ha subido nada (`push`).

## Leyenda

| Marca | Significado |
|---|---|
| ✅ VERIFICADO | Visto funcionar en navegador o en un log del equipo. **Solo lo marca el equipo.** |
| 🧪 ESCRITO | Existe en el código y es coherente en lectura; compila y la app arranca. Falta verlo en navegador. |
| POR HACER | No está en el código, o está incompleto. |
| DIFERIDO | Decisión pendiente del equipo u otra tanda. |
| NO VISTO | El ítem no tiene definición anclable en el repositorio; no se puede clasificar sin inventar. |

## Resumen ejecutivo

- **Infraestructura:** compila y arranca sin ciclo ni placeholder (el log de §0 lo confirma). Arranque del 2026-10-24 12:37 con las **cuatro** variables obligatorias (`JWT_SECRET`, `MYSQL_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`): `HikariPool-1 - Start completed`, `Tomcat started on port 50500`, `Started SmartglassApplication`; sin `PlaceholderResolutionException` ni `BeanCurrentlyInCreation`. Sin `MYSQL_PASSWORD` el placeholder **no** da error: Hikari recibe el texto literal y MySQL responde `Public Key Retrieval is not allowed`.
- **Pruebas por línea de comandos (curl, no navegador):** `/js/theme-init.js` → 200; ráfaga de `POST /auth/registro` con cuerpo vacío → 400 en las peticiones 1–10 y 429 desde la 11 (H11).
- **Comportamiento:** los ítems F y H siguen 🧪. Las tandas de UX y T0–T2 de la reestructuración comercial pasaron el gate del equipo en navegador (✅ donde se indica); lo probado solo con scripts sigue 🧪.
- **Contrato de cálculo:** `AnalisisService` y las funciones del contrato de `glass-preview.js` (`calcular`, `impacto`, `recomendar`, `UMBRAL`, `fmtNum`, `fmtAuto`) siguen **idénticos a `main`** después de T0, T1 y T2 (`git diff main` vacío). También `PedidoService`, `CheckoutController`, `CustomOAuth2UserService`, `LoginRateLimitFilter`, `theme-init.js` y el logotipo. Las anclas A–D dan las mismas cifras en es-CO, es-ES y en, como visitante y con sesión.

## Tandas de UX (rama `ux-accesibilidad-catalogo`, commit `8c13416`)

| Tanda | Estado | Evidencia |
|---|---|---|
| Rediseño de plantillas y JS conservando la paleta | ✅ VERIFICADO (gate del equipo) | `static/css/theme.css`, `static/js/main.js` |
| T1 · Modo de accesibilidad global: daltónico (Okabe-Ito + icono y patrón) y fotosensible (sin animaciones), sincronizado con la cuenta | ✅ VERIFICADO | `static/js/a11y-init.js`; `PerfilAccesibilidadController.java`; `<html data-a11y>` |
| T2 · Catálogo con esculturas, *placeholders* y lista de fotos pendientes | ✅ VERIFICADO | `CatalogoEsculturasSeeder.java`; `CatalogoProducto.java` (`categoria`, `tamano`, `getImagenUrl`); `documentos/assets-catalogo-T2.md` |
| T3 · Pulido visual por tiers (tokens, contraste ≥ 4,5:1, objetivos táctiles de 44 px) | ✅ VERIFICADO | `theme.css` (`--on-*`, `--paper*`, `@media (pointer: coarse)`) |
| Panel de login y registro con la evolución animada del vidrio | ✅ VERIFICADO | `fragments/auth-visual.html` |

## Reestructuración comercial (rama `comercio-stock-notif`)

Cinco sub-tandas con gate: T0 i18n → T1 login solo al pagar → T2 stock, ofertas y precios → T3 notificaciones → T4 onboarding.

### T0 · i18n de textos (commit `fe2dfe8`)

| Ítem | Estado | Evidencia |
|---|---|---|
| Tres idiomas: es-CO (por defecto), es-ES y en; 873 textos | ✅ VERIFICADO | `messages.properties` (es-CO), `messages_es_ES.properties` (solo diferencias), `messages_en.properties` |
| `MessageSource` explícito (la autoconfiguración quedaba en caché entre reinicios de devtools) | ✅ VERIFICADO | `I18nConfig.java:67` |
| Idioma en la cookie `sg-lang` y `?lang=`; sin cookie se aplica la preferencia de la cuenta | ✅ VERIFICADO | `I18nConfig.java:76,85`; `Usuario.java:58` (`preferencia_idioma`) |
| `PATCH /perfil/idioma` (400 si el idioma no es válido) | ✅ VERIFICADO | `PerfilIdiomaController.java:39` |
| Selector de idioma en la barra pública, el admin y el panel flotante | ✅ VERIFICADO | `fragments/navbar-publico.html:75`; `admin/fragments/nav.html:58`; `main.js:738` |
| Textos de los scripts (`SG_TXT`, `sgT`, `tx`) | ✅ VERIFICADO | `fragments/recursos.html:23,97`; `main.js:30` |
| **D2:** los números del contrato no se traducen (`9,8` y `$175.959` en los tres idiomas) | ✅ VERIFICADO | `#numbers.formatDecimal(…,'POINT',…,'COMMA')` y `SGGlass.fmtNum` |
| Siguen en español en todos los idiomas | Limitación conocida | Frases dinámicas de `glass-preview.js` (congelado), avisos del servidor, recomendación de `AnalisisService`, datos guardados |

### T1 · Login solo al pagar (D4)

| Ítem | Estado | Evidencia |
|---|---|---|
| Abiertos a visitantes: `/carrito/**`, `/vidrio-personalizado/cotizar` (GET y POST), `GET /vidrio-personalizado/cotizacion/*` | ✅ VERIFICADO | `SecurityConfig.java:129-131` |
| Siguen exigiendo cuenta: checkout, mis pedidos, mis cotizaciones, cuenta, admin y aprobar/eliminar/agregar obra; la API REST sigue en 401 | 🧪 ESCRITO (probado con curl) | `SecurityConfig.java:132-147`; `/api/vidrios/**` sigue autenticada (el controlador ya responde 401: `VidrioPersonalizadoController.java:59-65`) |
| Carrito del visitante en la sesión HTTP (producto → cantidad) | ✅ VERIFICADO | `CarritoService.java:37` |
| Al iniciar sesión el carrito se suma al de la cuenta sin perder nada | ✅ VERIFICADO | `CarritoService.java:113`; `CustomSuccessHandler.java:46`; `SesionAnonima.java:75` |
| Respaldo: si queda algo del visitante en la sesión, se transfiere en la siguiente página | 🧪 ESCRITO | `SesionAnonima.java:90`; `VistaGlobalAttributesAdvice.java:90` |
| Obra cotizada sin cuenta: `usuarioId` null (el del formulario se ignora) y solo la ve la sesión que la creó | 🧪 ESCRITO (probado con curl en dos instancias) | `VidrioPersonalizadoWebController.java:93,125-127`; `SesionAnonima.java:55` |
| La obra anónima pasa a la cuenta al iniciar sesión | 🧪 ESCRITO (probado de punta a punta con curl; en el recorrido del equipo no se envió una obra) | `VidrioSostenibleService.java:154` |
| Pagar sin sesión lleva al login con el aviso "Tu carrito se conserva" y vuelve a `/checkout` | ✅ VERIFICADO | `CarritoController.java:86`; `LoginController.java:34`; `login.html:23` |
| Cotizaciones anónimas limitadas por F41 (solo visitantes) | 🧪 ESCRITO | `RateLimitFilter.java:70` |
| El admin ve "Visitante sin cuenta" en las obras sin dueño | 🧪 ESCRITO | `AdminCotizacionesController.java:157` |
| IDOR: obra o pedido ajenos redirigen | 🧪 ESCRITO (curl) | Sin cambios en F5 ni F8 |
| Login con Google conservando el carrito | POR VERIFICAR | Pasa por el mismo `CustomSuccessHandler`; no se probó |

### T2 · Stock, ofertas y balanceo de precios del catálogo

| Ítem | Estado | Evidencia |
|---|---|---|
| **D1:** stock transaccional, sin azar; solo baja con compras reales | 🧪 ESCRITO (compras 7–10 con la cuenta de prueba: Flotado 40 → 36, Ola 1 → 0) | `StockService.java:67,73` (`findAndModify` con `stock >= cantidad`), `:94` (compensación) |
| Descuento al confirmar sin tocar `PedidoService` ni `CheckoutController` | 🧪 ESCRITO | `StockCompraAspect.java:31` (`@Around` sobre `crearPedidoDesdeCarrito`) |
| Compra rechazada si el stock no alcanza: aviso veraz y stock intacto | 🧪 ESCRITO (carrera con dos sesiones: "Solo quedan 37…", sin pedido) | `StockService.java:67`; aviso nuevo en `checkout.html:114` (antes el checkout no mostraba errores) |
| El carrito no admite más unidades que el stock | ✅ VERIFICADO | `CarritoController.java:61,91` |
| Stock inicial fijo y plausible (escultura grande 1, mediana 3, pequeña 8; vidrio plano 6–40) | ✅ VERIFICADO | `CatalogoComercioSeeder.java:27,39` |
| Badge "¡Últimas unidades! · Quedan N" (menos de 5, configurable con `smartglass.stock.umbral-bajo`) | ✅ VERIFICADO | `CatalogoProducto.java:215`; `fragments/comercio.html:15` |
| Agotado: tarjeta atenuada, texto "Agotado" y botón deshabilitado con `aria-label`; el servidor también bloquea | ✅ VERIFICADO | `CatalogoProducto.java:212`; `catalogo.html:181`; `detalle.html:152` |
| Ofertas con precio, fechas y activa; precio de lista tachado; se cobra la oferta | ✅ VERIFICADO (vistas y modal); cobro 🧪 (pedido 10 a $75.000) | `CatalogoProducto.java:220,242`; `fragments/comercio.html:34` |
| Panel admin "Stock y oferta" (solo `ROLE_ADMIN`, parámetros explícitos, validación) | ✅ VERIFICADO | `AdminController.java:190`; `admin/productos.html:312` |
| Badges accesibles: icono, texto y borde; sin animación; sin colores del semáforo (F24) | ✅ VERIFICADO | `theme.css:1394` |
| **D3:** balanceo solo del catálogo, con la regla "solo baja" (decisión del equipo) | ✅ VERIFICADO | `EsquemaPreciosCatalogo.java:26,94` (versión `T2-v3`); precio original en `precioAnterior` (reversible); lo que crea el admin queda `manual` (`AdminController.java:177`) |

Precios tras el balanceo `T2-v3`: bajan 12 productos y 15 se quedan igual. Por ejemplo, el espejo solar pasa de 1.200.000 a 485.000; las esculturas pequeñas, de 180.000–320.000 a 90.000–100.000; las grandes, de 4,8–6,2 M a 3,96–4,1 M. La cotización personalizada no cambió.

### T3 · Notificaciones (polling, sin WebSocket)

| Ítem | Estado | Evidencia |
|---|---|---|
| Entidad `Notificacion` en Mongo: clave i18n + parámetros como texto (D2); destino `ADMIN`, `USUARIO` o `TODOS`; leída por usuario | 🧪 ESCRITO | `Notificacion.java:25` |
| Polling: `GET /api/notificaciones` cada 60 s, en pausa con la pestaña oculta, con caché de 30 s; `PATCH /{id}/leida` y `PATCH /leida` | 🧪 ESCRITO (curl: GET 200 en los tres idiomas, PATCH 204 con cuenta y 401 sin ella) | `NotificacionController.java:26,31,38`; `SecurityConfig.java:145`; `main.js:769` |
| Cada quien ve solo lo suyo: admin `ADMIN`; usuario `TODOS` + las suyas; visitante solo `TODOS` (su "visto" queda en el navegador) | 🧪 ESCRITO | `NotificacionService.java:181,200` |
| Admin: cotización nueva, **también anónima** ("visitante sin cuenta") | ✅ VERIFICADO (campana del admin; la del equipo, FF57D3, también llegó) | `NotificacionService.java:92`; `NotificacionEventosAspect.java:34` (sin tocar `VidrioSostenibleService`) |
| Admin y usuario: aprobación o rechazo de la obra (solo si la máquina de estados aceptó la transición) | ✅ VERIFICADO (10 aprobaciones del equipo; las obras anónimas solo avisan al admin) | `NotificacionEventosAspect.java:41` |
| Admin: pedido nuevo; usuario: "Recibimos tu pedido" con enlace a la factura | ✅ VERIFICADO (pedido 12 del equipo: aviso al admin y confirmación leída por el cliente) | `NotificacionEventosAspect.java:47` (sin tocar `PedidoService`) |
| Admin y usuario: cambio de estado del pedido desde el panel (uno o varios) | ✅ VERIFICADO (cambio múltiple de 6 pedidos; el cliente lo recibe en es-CO y en) | `NotificacionEventosAspect.java:54` |
| Admin: stock bajo o agotado tras una compra real | ✅ VERIFICADO (visto y leído en la campana del admin) | `StockService.java:89` |
| Todos: producto nuevo, oferta nueva y reposición, desde el panel de admin | 🧪 ESCRITO (falta probarlo con admin) | `AdminController.java:185,239` |
| Ofertas vigentes y "más vendidos" calculados al leer (solo lectura sobre `DetallePedido`; las obras a medida y los pedidos cancelados no cuentan) | 🧪 ESCRITO | `NotificacionService.java:231,244`; `DetallePedidoRepository.java:16` |
| Campana en la barra pública y en la del admin: contador con número y `aria-label` "N sin leer", etiqueta "Nueva" en texto, sin animación, sin colores del semáforo, contraste 6,3:1 | 🧪 ESCRITO (vista en el navegador como visitante) | `fragments/notificaciones.html:14`; `navbar-publico.html:72`; `admin/fragments/nav.html:55`; `theme.css:1429` |
| Textos en los tres idiomas, resueltos por el servidor en el idioma de quien lee | 🧪 ESCRITO (curl en es-CO, es-ES y en) | `messages*.properties`, sección "T3 notificaciones" |

Limitación: una oferta con fecha de inicio futura no genera aviso el día que empieza (no hay tarea programada); sí aparece desde ese día en "Ofertas activas".

### T4 · Onboarding (solo front-end)

| Ítem | Estado | Evidencia |
|---|---|---|
| Recorrido guiado de 6 pasos (bienvenida → tienda → diseñar → carrito → pagar con cuenta → idioma y accesibilidad) que resalta cada elemento de la barra | 🧪 ESCRITO (probado en el navegador como visitante, en escritorio y en móvil) | `main.js:880`; `fragments/tour.html:12`; atributos `data-tour` en `navbar-publico.html:40` y siguientes |
| Aparece una sola vez a un visitante o usuario nuevo (`localStorage` "sg-tour-visto"); no sale en login, registro ni checkout | 🧪 ESCRITO | `main.js:880` |
| Descartable con Esc o "Saltar recorrido"; diálogo **no modal** que no atrapa el foco; al cerrar, el foco vuelve a donde estaba | 🧪 ESCRITO | `main.js:880` |
| Resaltado estático (contorno cian), sin animación en fotosensible ni con movimiento reducido; foco visible; objetivos táctiles de 44 px; no tapa el switch de tema | 🧪 ESCRITO | `theme.css:1460` |
| Página `/guia` con los mismos pasos (funciona sin JS) y botón para relanzar el recorrido; enlace "Guía de compra" en el pie | 🧪 ESCRITO | `HomeController.java:30`; `guia.html:32`; `footer.html:41` |
| Textos en los tres idiomas | 🧪 ESCRITO (curl y navegador en es-CO y en) | `messages*.properties`, sección "T4 recorrido guiado" |
| Flag "visto" en `Usuario` | No se hizo (opcional en el prompt) | El recorrido sale una vez por navegador |

## Mejoras de plataforma (rama `mejoras-responsive-plano-saas`)

Seis tandas con gate: T1 responsive → T2 plano y cotizaciones → T3 sostenibilidad → T4 estética → T5 SaaS (superadmin y planes) → T6 despliegue (Render o Railway). Decisiones del equipo: hosting en Render o Railway con MongoDB Atlas y MySQL gestionado; SaaS por planes de funciones en una sola instalación; sostenibilidad sin tocar el cálculo; F45 aprobado y a pulir.

### T1 · Responsive en todos los dispositivos (commit `901a93f`)

| Ítem | Estado | Evidencia |
|---|---|---|
| 28 páginas sin desborde horizontal, campos de 16 px, textos de 12 px como mínimo y objetivos táctiles de 44 px; contenido más ancho en pantallas de 1680, 2200 y 2800 px | 🧪 ESCRITO (auditoría con script en 375, 768, 1024 y 2560 px) | `theme.css` (sección "Responsive en todos los dispositivos"), `admin.css` |

### T2 · Plano de personalización y cotizaciones

| Ítem | Estado | Evidencia |
|---|---|---|
| Lienzo más grande: columna del escenario ~56 % del ancho (703 px a 1440) y pieza más grande dentro del SVG; el precio queda fijo abajo mientras se desplaza la columna | 🧪 ESCRITO (navegador, 1440 y 375 px) | `vidrio-personalizado-form.html` (`.atelier-grid`, `.precio-caja`); `glass-preview.js` (`render`) |
| Controles de vista: cotas, escala humana (silueta de 1,70 m a la misma escala) y vista ampliada a pantalla completa (Esc la cierra); preferencia por navegador | 🧪 ESCRITO | `glass-preview.js` (`controlesVista`, `silueta`); `theme.css` (`.sg-vista-ctl`, `.sg-ampliado`) |
| 14 geometrías (antes 5): esquinas redondeadas, cápsula, arco, triángulo rectángulo, inclinada, frontón, rombo, hexagonal y octogonal nuevas. Solo presentación: se cotizan por caja envolvente; nota visible en el formulario | 🧪 ESCRITO | `glass-preview.js` (`POLIGONOS`, `contorno`); `VidrioSostenibleService.FORMAS` (valor desconocido → "Rectangular") |
| F45 aprobado y pulido: lienzo de 420 × 420 cm, encuadre automático, zoom con rueda y arrastre del fondo, puntos de tamaño táctil, X/Y del punto elegido, medidas exactas, girar 90°, reflejar, centrar, imán de 1/5/10 cm, plantillas de todas las formas y arranque desde la forma y medidas ya elegidas; en móvil el lienzo va arriba | 🧪 ESCRITO (navegador: clic real, deshacer, transformar, confirmar y enviar) | `corte-editor.js` |
| Cotizaciones: estados, tipos, formas, tratamientos y bordes traducidos; miniaturas con el corte real; filtros por estado en Mis cotizaciones; búsqueda en el panel; el admin aprueba o rechaza desde el detalle y vuelve a él con aviso; "Crear una variante" abre el configurador con la obra cargada | 🧪 ESCRITO (navegador con sesión de admin: rechazar, re-aprobar, añadir al carrito, crear variante y eliminar) | `vidrio-personalizado-detalle.html`, `mis-cotizaciones.html`, `admin-cotizaciones.html`, `AdminCotizacionesController.java`, `VidrioPersonalizadoWebController.java` |
| Textos de JS del configurador, editor y detalle en los tres idiomas; avisos del servidor con `MessageSource` | 🧪 ESCRITO (navegador en es-CO, es-ES y en) | `fragments/textos-vidrio.html`; `messages*.properties`, sección "T2 plano y cotizaciones" |
| Contrato: fórmulas, constantes y umbrales sin cambios; en `impacto` y `recomendar` solo pasan los textos por el traductor | 🧪 ESCRITO | Anclas A–D idénticas en los tres idiomas (ver abajo) |

### T3 · Sostenibilidad sin tocar el cálculo

| Ítem | Estado | Evidencia |
|---|---|---|
| Configurador: "Material y proceso" (reciclado, renovable, compensación, como antes) y 4 **compromisos sin costo** con su propio dato: retoma del vidrio que se reemplaza (≈ kg = peso de la pieza), empaque reutilizable (marcado por defecto), entrega consolidada (excluyente con urgente, también en el servidor) y cortar de retazos | 🧪 ESCRITO (navegador: con los 4 activos el precio y el CO₂ no cambian) | `vidrio-personalizado-form.html`; `CotizacionVidrioRequest`, `VidrioSostenible`, `VidrioSostenibleService` |
| Sello **Compromiso verde** (Bronce ≥ 35, Plata ≥ 60, Oro ≥ 85 de 100 puntos), en vivo en el configurador y en detalle, Mis cotizaciones y panel; separado del semáforo de impacto (F24) | 🧪 ESCRITO | `CompromisoVerde.java` (fuente única de pesos; el configurador los recibe del servidor) |
| **Ficha ambiental imprimible** por obra (`/cotizacion/{id}/ficha`, mismas reglas de acceso que el detalle): pieza, composición, huella, decisiones y aviso de que no es una certificación de terceros | 🧪 ESCRITO (navegador; la impresión en tema oscuro fuerza tinta oscura, falta verla en vista previa de impresión) | `vidrio-ficha.html`; `VidrioPersonalizadoWebController.verFicha`; `SecurityConfig` |
| Checkout: compromisos de entrega para el pedido, preseleccionados desde las obras del carrito; se guardan en `pedidos` (columnas nuevas en 0 para los anteriores) y se ven en confirmación, factura y pedidos del admin | 🧪 ESCRITO (vista del checkout probada; falta un pago real con compromisos) | `Pedido`, `PedidoService` (sobrecarga), `CheckoutController`, `checkout.html` |
| Panel del admin: resumen de compromisos de los clientes (retoma con kg recuperables, empaque, consolidada, retazos y sellos) | 🧪 ESCRITO (navegador) | `AdminCotizacionesController.listar`; `admin-cotizaciones.html` |
| Compensación del checkout (+$5.000): ahora se cobra en el pedido (decisión del equipo) y queda en `pedidos.recargo_compensacion` (0 en los pedidos anteriores, que la marcaban sin cobrarla); el resumen del checkout la suma al marcarla; confirmación y factura la desglosan. La factura además desglosa bien productos y envío (antes "Subtotal" era el total y "Envío" estaba fijo en $ 0) y los importes salen en formato es-CO | 🧪 ESCRITO (vista del checkout y facturas #6 y #13 en navegador; falta un pago con compensación) | `PedidoService.RECARGO_COMPENSACION_PEDIDO`; `Pedido` (`getSubtotalProductos`, `getCostoEnvio`); `checkout.html`, `factura.html`, `pedido-confirmacion.html` |

### T4 · Estética: acerca, inicio y espacios vacíos

| Ítem | Estado | Evidencia |
|---|---|---|
| Cifras sin respaldo retiradas (decisión del equipo: "datos reales del modelo"): 98,2 % de claridad, ISO 14001, −25 % de energía, 500 t de CO₂, "carbono cero 2026", −15 % de desperdicio y "hasta 40 % reciclado" | 🧪 ESCRITO | `acerca.html`, `index.html`; claves nuevas `acerca2.*` e `index2.*` (las antiguas quedan sin uso) |
| Acerca rediseñado: hero con tres piezas dibujadas por el motor; "La plataforma en cifras" con contadores **en vivo** (obras, pedidos, % con reciclado, kg de calcín, kg con retoma, sellos Oro); "Cómo calculamos la huella" con las reglas del contrato (densidades, 1,1 / 0,6, −15 %, ×1,4 / ×1,8, umbrales 12/20); pilares y "Lo que eliges sí cuenta" con hechos | 🧪 ESCRITO (navegador en es-CO, es-ES y en, escritorio y 375 px) | `EstadisticasPlataformaService` (caché de 5 min; si la base falla, el bloque se oculta); `HomeController` |
| Inicio: datos del hero con respaldo (−45 % con renovable, −15 % con calcín, 14 geometrías), "Huella a la vista", "Así de simple" en 3 pasos y cierre con cifras en vivo; `aria-label` de la muestra traducido | 🧪 ESCRITO | `index.html` |
| Espacios vacíos: capa de luz ambiental fija y estática detrás de todas las páginas (oculta al imprimir); contadores que cuentan al entrar (respetan "sin animaciones" y movimiento reducido, también si se activan con la página abierta); aparición al hacer scroll con umbral 6 % para bloques altos | 🧪 ESCRITO | `theme.css` (sección "T4 · Ambiente"); `main.js` (`contarAlEntrar`, `revelarAlEntrar`) |

### Decisiones D1–D4

| Decisión | Estado |
|---|---|
| D1 · Stock transaccional, no aleatorio | Aplicada la recomendación |
| D2 · Sin i18n de números | Aplicada la recomendación |
| D3 · Balancear solo precios de catálogo | Aplicada la recomendación, con la regla "solo baja" del equipo |
| D4 · Login solo al pagar con gate estricto | Aplicada; gate pasado |
| Notificaciones por *polling*, no WebSocket (nota §5) | Aplicada la recomendación |

## Ítems F (funcionalidad)

| Ítem | Estado | Evidencia en el código |
|---|---|---|
| F1–F4, F6 | NO VISTO | Sin definición en el repositorio |
| F5 · Anti-IDOR en la API de cotizaciones | 🧪 ESCRITO | `VidrioPersonalizadoController.java:67,80-85` |
| F7 · Eliminar: dueño solo COTIZACION/RECHAZADA, admin cualquiera | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:162-163` |
| F8 · Detalle solo dueño o admin (T1: o la sesión anónima que la cotizó) | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:89-113` |
| F9 · Sin `@ModelAttribute("usuario")` en rutas con `{id}` | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:56-59` |
| F10 · Aprobar: solo admin y solo desde COTIZACION | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:132-152` |
| F11 · Intensidad de carbono en la entidad | 🧪 ESCRITO | `VidrioSostenible.java:99,111` |
| F12 · Huella almacenada de la obra × cantidad en el checkout | 🧪 ESCRITO | `PedidoService.java:62-81` |
| F13 · Densidad por tipo en la huella de catálogo | 🧪 ESCRITO | `AnalisisService.java:91` |
| F14 | NO VISTO | Sin definición en el repositorio |
| F15 · Badge de cotizaciones pendientes en el nav admin | 🧪 ESCRITO | `admin/fragments/nav.html:42`; `PermisosAdvice.java:43-49` |
| F16, F17 | NO VISTO | Sin definición en el repositorio |
| F18 · Imagen nula en mis pedidos | 🧪 ESCRITO | `mis-pedidos.html:655` |
| F19 · Jerarquía del KPI (`kpi-num` / `kpi-unidad`) | 🧪 ESCRITO | `mis-pedidos.html:135-144,561-562` |
| F20 · KPI "Sostenible" con datos reales | 🧪 ESCRITO | `CheckoutController.java:93-103`; `mis-pedidos.html:547-565` |
| F21 · Fila tipada del panel (sin `BeanWrapper`) | 🧪 ESCRITO | `AdminCotizacionesController.java:121-152` |
| F22 · `normalizarTipo` sin espacios ni `/` | 🧪 ESCRITO | `AnalisisService.java:274` |
| F23 | NO VISTO | Sin definición en el repositorio |
| F24 · Semáforo solo por intensidad; absoluto neutro | 🧪 ESCRITO | `VidrioSostenible.java:99-115`; `AdminCotizacionesController.java:161-162`; `vidrio-personalizado-detalle.html:482-485` |
| F25 · Anatomía de la pieza cierra con el peso | 🧪 ESCRITO | `AnalisisService.java:184-190` |
| F26 · Botones separados por rol | 🧪 ESCRITO | `vidrio-personalizado-detalle.html:206,292` |
| F27 · Mínimo 1 capa en la resistencia | 🧪 ESCRITO | `AnalisisService.java:216`; `CotizacionVidrioRequest.java:15` |
| F28 · Chips con todos los modificadores | 🧪 ESCRITO | `vidrio-personalizado-detalle.html:52-53` |
| F29 · Factura oficial `SG-AAAA-000000` | 🧪 ESCRITO | `PedidoService.java:104-108`; `Pedido.java:84-90` |
| F30 · Principal de Google = `mysql_username` | 🧪 ESCRITO | `CustomOAuth2UserService.java:117-122` |
| F31 | DIFERIDO | Fuera de esta tanda |
| F32 · Ruta del panel de cotizaciones | 🧪 ESCRITO | `AdminCotizacionesController.java:74` → `templates/admin-cotizaciones.html` |
| F33 · Snapshot del pedido | 🧪 ESCRITO (parcial) | Precio, nombre e imagen en `DetallePedido.java:59-66`; huella a nivel de pedido en `PedidoService.java:102`. **Falta:** medidas y huella por línea de la obra a medida (ver AUD-11) |
| F34 | DIFERIDO | Toca el contrato; requiere visto bueno |
| F35 · Índices de Mongo | 🧪 ESCRITO (parcial) | `auto-index-creation=true` en `application.properties:57`; único en `Carrito.java:21`. **Faltan** los índices compuestos de `VidrioSostenible` (ver AUD-05) |
| F36 | NO VISTO | Sin definición anclable en el repositorio |
| F37 | DIFERIDO | Fuera de esta tanda |
| F38 · Recargo de $5.000 por compensar | DIFERIDO | El checkout lo anuncia (`checkout.html:936`) pero `PedidoService.java:91-102` no lo cobra. Decisión: cobrarlo o quitar el texto |
| F39 · El servidor manda; la API externa es solo referencia | 🧪 ESCRITO | `vidrio-personalizado-detalle.html:542-584` |
| F40 · Valores de tratamiento y bordes | 🧪 ESCRITO | `vidrio-personalizado-form.html:477-511`; etiquetas en `vidrio-personalizado-detalle.html:256-258,531-537` |
| F41 · Rate limit por actor | 🧪 ESCRITO | `RateLimitFilter.java`; `SecurityConfig.java` (después de autenticar) |
| F42 · Paginación | POR HACER | No hay `Pageable` ni `Page` en el código; `VidrioPersonalizadoController.java:86-89` devuelve `List`. Solo existe el ajuste de pool y Tomcat (`application.properties`) |
| F43 · Validación del registro | 🧪 ESCRITO | `@Valid` + `BindingResult` en `AuthController.java:40-41` y `RegistroController.java`; E.164 en `AuthService.java:105-118`; disponibilidad en vivo en `RegistroController.java:50` (H06) |
| F44 · Vinculación con Google invalida la contraseña local | 🧪 ESCRITO | `CustomOAuth2UserService.java:56-58` (3 parámetros, `@Lazy`), `:106` (`setPassword`) |
| F45 · Corte personalizado | 🧪 ESCRITO (aprobado por el equipo el 2026-10-24; pulido en T2) | Bifurcación aditiva en `AnalisisService.java:145-172`; editor en `corte-editor.js`; área en `glass-preview.js` (`areaPoligono`) |

## Hallazgos H (cierre de bloqueantes)

| Ítem | Estado | Evidencia |
|---|---|---|
| H01 · Secretos fuera del repositorio | 🧪 ESCRITO | Variables sin valor por defecto en `application.properties:8,17,44-45`; contraseña del admin inicial desde `ADMIN_PASSWORD` (`DataInitializer.java:26,47`); plantilla `.env.example`; `.env` en `.gitignore`. **Pendiente del equipo:** rotar JWT, MySQL, Google y la contraseña del admin, y decidir si se reescribe el historial |
| H02 · Modelo sin `masa_kg` | 🧪 ESCRITO | `analisis.ipynb` celda 5; notebook re-ejecutado: MAE 23,25 · R² 0,9433 · CV 0,9271 ± 0,0115 (`metricas_modelo.csv`). **Pendiente:** los PDF de `documentos/` siguen citando 0,9461 / 20,69 |
| H03 · El README no promete integración del `.pkl` | 🧪 ESCRITO | README, secciones "Funcionalidades", "Flujo general" e "Integración del modelo predictivo" |
| H04 · Caja de las plantillas | 🧪 ESCRITO | `comparar.html`, `admin/resenas.html`; `/comparar` probado con `java -jar` (200) |
| H05 · Límite de intentos de login | 🧪 ESCRITO | `LoginRateLimitFilter.java`; `SecurityConfig.java` (antes de `UsernamePasswordAuthenticationFilter`); 10 intentos por minuto por IP |
| H06 · `GET /registro/disponible` | 🧪 ESCRITO | `RegistroController.java:50`; llamador en `registro.html:517,607` |
| H07 · `theme-init.js` sin 404 | 🧪 ESCRITO | `static/js/theme-init.js`; 26 referencias exactas |
| H08 · `requirements.txt` | 🧪 ESCRITO | Raíz; instalación probada con Python 3.13 |
| H09 · Este documento | 🧪 ESCRITO | Actualizado con T4; sin commitear |
| H10 · Archivo basura `how --stat ce8af77` | 🧪 ESCRITO | Borrado del índice (`git rm`) |
| H11 · Límite en `POST /auth/registro` | 🧪 ESCRITO | `RateLimitFilter.java:65-66,97`; la petición 11 por minuto devuelve 429 |

## Hallazgos de la auditoría aún abiertos (no tocados)

| Ítem | Qué falta | Dónde |
|---|---|---|
| AUD-01 | Validación de entrada de la cotización (medidas ≤ 0 o negativas, perforaciones negativas) | `CotizacionVidrioRequest.java` |
| AUD-02 | El rol de un usuario de Google se toma sin normalizar | `CustomOAuth2UserService.java:113-114` |
| AUD-03 | `cambiarRol` acepta cualquier texto | `AdminController.java:85-88` |
| AUD-04 | `accessDeniedPage("/error/403")` sin controlador: un usuario sin rol de admin que entra a `/admin` recibe 404 | `SecurityConfig.java` |
| AUD-05 | Índices compuestos de `VidrioSostenible` | `VidrioSostenible.java:20` |
| AUD-06 | Mensajes de error, SQL y Security en modo detallado | `application.properties` |
| AUD-07 | Logout por GET y cookie con otro nombre | `SecurityConfig.java` |
| AUD-08 | `JwtAuthenticationFilter` registrado dos veces | `JwtAuthenticationFilter.java:27` |
| AUD-09 | Una obra aprobada se puede comprar varias veces | `CarritoService.java:74-85` |
| AUD-10 | Pruebas de permisos (solo existe `contextLoads`) | `src/test` |
| AUD-11 | Medidas y huella por línea en `DetallePedido` | `DetallePedido.java:59-66` |
| AUD-12 | La app guarda los datos en la base Mongo `test`, no en `smartglass_catalog`: probablemente Spring Boot 4 ignora `spring.data.mongodb.uri`. Revisar también F35 | `application.properties:13` |
| AUD-13 | Pedidos, factura y carrito muestran los miles con coma (`$ 180,000`): `String.format` usa el idioma de la JVM | `Pedido.java:132`; `DetallePedido.java:116`; `ItemCarrito.java:48`; `admin/dashboard.html:25` |
| AUD-14 | VS Code compila al guardar y devtools reinicia la app: cierra la sesión y puede ejecutar estados intermedios del código | Entorno de desarrollo |
| AUD-15 | Datos de prueba de T1 y T2 por limpiar: usuario `pruebat1…` (id 105) con sus pedidos 7–10, carrito y obras; obras anónimas de prueba; el Craquelado y el Flotado sumados al carrito del admin | MySQL `smartglass_db`, Mongo `test` |

## Lo que debe verificar el equipo en navegador

| Prueba | Resultado esperado |
|---|---|
| Caso A · Templado 105 × 105 × 7, reciclado + renovable | 9,8 kg · 8,9 kg/m² · Favorable · $175.959 · 180 MPa |
| Caso B · Baranda Templado+Laminado 20 mm, sin reciclado ni renovable | 80,08 kg/m² · ALTO |
| Caso C · Mobiliario 40 mm | Alerta de espesor + recomendación de 6 a 8 mm |
| Caso D · Templado 400 × 400 × 8, reciclado + renovable | 163,2 kg (neutro) y 10,2 kg/m² con chip verde, separados; anatomía cierra en 320,00 |
| Selector de tema | Sin parpadeo al cargar; `/js/theme-init.js` responde 200 |
| Fuerza bruta de login | Al 11.º intento seguido: `/login?bloqueado` con aviso |
| `POST /auth/registro` en ráfaga | La petición 11 en un minuto devuelve 429 |
| Login con Google | Entra con las credenciales rotadas; la cuenta local con el mismo correo queda vinculada |
| Login con Google desde el pago (T1) | Vuelve a `/checkout` con el carrito del visitante sumado |
| Obra cotizada sin cuenta (T1) | Tras iniciar sesión aparece en Mis cotizaciones, sin el aviso azul |
| Compra real (T2) | El stock del producto baja en lo comprado (Mongo `catalogo_productos.stock`) |
| Oferta con fecha futura (T2) | El Laminado Opal (oferta desde el 26/10) muestra su precio normal hasta ese día |
| Agotado (T2) | La Ola Templada sigue en 0 a propósito; se devuelve a 1 desde "Stock y oferta" |
| Campana del admin (T3) | ✅ Verificado: cotización nueva (anónima y con cuenta), pedido nuevo y stock bajo aparecen con enlace y quedan leídas |
| Aprobar una obra (T3) | ✅ Verificado: su dueño recibe "Tu obra Nº … fue aprobada" con enlace |
| Cambiar el estado de un pedido (T3) | ✅ Verificado: el cliente recibe "Tu pedido … está enviado / en preparación / entregado" |
| Oferta o producto nuevo (T3) | Aparece en la campana de un visitante como "Nueva" (pendiente) |
| Plano (mejoras T2) | Lienzo grande con el precio visible; cotas, escala humana y ampliar; 14 formas; el editor F45 arranca con la forma elegida y permite medidas exactas, girar, reflejar y editar puntos |
| Cotizaciones (mejoras T2) | Estados y nombres traducidos; filtros en Mis cotizaciones; búsqueda en el panel; aprobar o rechazar desde el detalle; "Crear una variante" |
| Sostenibilidad (mejoras T3) | Los 4 compromisos no cambian precio ni CO₂; el sello sube de Bronce a Oro; la urgente bloquea la consolidada; la ficha se imprime legible; al pagar llegan marcados y aparecen en confirmación, factura y pedidos del admin; con compensación el total sube $5.000 y la factura la desglosa |
| Acerca e inicio (mejoras T4) | Ninguna cifra sin respaldo; contadores en vivo coinciden con el panel del admin; sin desbordes a 375 px; con "sin animaciones" las cifras salen fijas |
| Recorrido guiado (T4) | Sale solo la primera vez; "Saltar" o Esc lo cierran; con fotosensible no se mueve nada; se relanza desde "Guía de compra" en el pie |

Nada se da por cerrado hasta que los greps den el resultado esperado, el app arranque con las env vars, el toggle de idioma mantenga las anclas en es-CO, el flujo anónimo→pago-con-login conserve el carrito, el stock sea transaccional y veraz, las notificaciones y el tour respeten i18n y accesibilidad, y los casos A–D se vean en navegador; ningún cambio movió un decimal del contrato ni tocó el logotipo, y D1–D4 no se aplicaron sin mi visto bueno.
