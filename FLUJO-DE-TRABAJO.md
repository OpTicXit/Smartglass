# FLUJO DE TRABAJO · SmartGlass

Estado de cada ítem **leído contra el código del repositorio** (rama `optimizacion-pre-despliegue`, actualizado el 2026-10-26 con el despliegue del piloto en Render y los datos de demostración; las seis tandas de optimización están integradas en `main` y subidas a GitHub). Sustituye a cualquier versión anterior de este documento.

Ramas de trabajo: `cierre-bloqueantes` (ítems F y H) → `ux-accesibilidad-catalogo` (commit `8c13416`) → `comercio-stock-notif` (T0 en `fe2dfe8`; T1 y T2 en `c4cc809`; T3 en `57aea55`; T4 en `c6d8c16`; todo integrado en `main` local) → `mejoras-responsive-plano-saas` (T1 responsive en `901a93f`; T2 plano y cotizaciones en `d5f5ab6`; T3 sostenibilidad en `bed4199`; T4 estética en `ab3ccb1`; T5 SaaS en `515375e`; T6 despliegue en `38dc9b1`; todo integrado en `main` local) → `optimizacion-pre-despliegue` (U1 idiomas en `e42a55b`; U2 rendimiento en `a2ddb2e`; U3 SaaS visible en `823801f`; U4 CO₂ por etapas en `a995e04`; U5 inmersión en `a0bc5fc`; U6 guía automatizada en el commit «U6 guia automatizada»). Todo está en `main` y subido a GitHub; el piloto corre en Railway (`https://smartglass-production.up.railway.app`; Render suspendió la cuenta).

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

### T5 · SaaS: planes por funciones y consola de superadmin

| Ítem | Estado | Evidencia |
|---|---|---|
| Estudio de mercado (Smart Glazier, eDirectGlass, GlassGo; DANE EMICRON 2024 y adopción digital de mipymes en Colombia) con fuentes, conclusión y siguientes pasos sugeridos (WhatsApp, exportar a Excel, límite de cotizaciones en Básico) | 🧪 ESCRITO | Sección "Estudio de mercado" de la consola; claves `superadmin.m*` |
| Planes **Básico** y **Avanzado** sobre una sola instalación; 7 funciones con flag (geometrías avanzadas, corte personalizado, compromisos verdes, ficha ambiental, ofertas programadas, cifras públicas, varios idiomas). Sin documento guardado el plan es Avanzado (nadie pierde nada) | 🧪 ESCRITO (navegador: Básico → tienda en es-CO, 4 formas, sin editor ni compromisos, ficha redirige, ofertas desactivadas; vuelta a Avanzado con A–D idénticos en en) | `Funcion`, `PlanService` (bean `@plan`, caché de 60 s), `ConfiguracionPlataforma` (Mongo `configuracion_plataforma`) |
| El servidor aplica el plan aunque se envíen datos a mano: forma no incluida → Rectangular, compromisos → apagados, idioma → es-CO, ofertas conservadas | 🧪 ESCRITO (POST manual con "Hexagonal" y compromisos en Básico → guardada Rectangular y sin compromisos) | `VidrioSostenibleService.formaSegunPlan`, `CheckoutController`, `AdminController.actualizarComercio`, `I18nConfig` |
| Consola `/superadmin` aparte del panel: plan, ajustes por función (según plan / activar / desactivar), historial y estudio de mercado. Acceso por la variable `SUPERADMIN_USUARIOS` (el admin de la vidriería no puede dárselo desde /admin/usuarios); para cualquier otro responde 404 | 🧪 ESCRITO (sin sesión: 404; con `admin`: plan, ajuste de corte personalizado e historial probados) | `SuperadminController`, `superadmin.html`, `application.properties` (`smartglass.superadmin.usuarios`) |
| Pendiente del equipo: añadir `SUPERADMIN_USUARIOS` a `.vscode/launch.json` (en local) y a las variables del despliegue (T6) | POR HACER | — |

### T6 · Despliegue en Render o Railway

| Ítem | Estado | Evidencia |
|---|---|---|
| Configuración por variables con valores locales iguales a los de antes: `PORT`, `MYSQL_URL`, `MONGODB_URI`, `UPLOADS_DIR`; URL de vuelta de Google con `{baseUrl}` | 🧪 ESCRITO (la app local sigue igual en 50500) | `application.properties` |
| **AUD-12 resuelto:** Spring Boot 4 ignora `spring.data.mongodb.uri` (obsoleta con nivel error) y usaba `mongodb://localhost/test`; ahora `spring.mongodb.uri`. En local se mantiene `test` para no mover los datos | 🧪 ESCRITO | Metadatos de `spring-boot-mongodb-4.1.1`; `application.properties` |
| Perfil `prod`: cookie `Secure`, `forward-headers-strategy=native`, errores sin detalle, sin SQL ni DEBUG, pool de 5, caché de 7 días con versión por contenido en CSS y JS | 🧪 ESCRITO (JAR con perfil prod en 50600: cookie `Secure; HttpOnly; SameSite=Lax`, HSTS, `redirect_uri=https://<dominio>/…`, `/css/theme-<hash>.css`, 404 sin trazas, 0 líneas SQL) | `application-prod.properties` |
| `Dockerfile` en dos etapas (Temurin 26), usuario sin privilegios con `setpriv`, volumen `/data`; `.dockerignore`; `railway.json` y `render.yaml`; chequeo `/salud` | 🧪 ESCRITO (sin Docker en la máquina: la primera compilación en la plataforma valida la imagen) | `Dockerfile`, `railway.json`, `render.yaml`, `SaludController` |
| Fotos subidas en carpeta configurable (`UPLOADS_DIR`) para montar un volumen persistente | 🧪 ESCRITO (foto servida desde la carpeta del perfil prod) | `UploadsWebConfig`, `AdminController` |
| Guía `DESPLIEGUE.md`: plataforma recomendada (Railway), variables, pasos, migración de datos (`mongodump --db test` → Atlas), Google Cloud y comprobaciones | 🧪 ESCRITO | `DESPLIEGUE.md`; enlace en `README.md` |
| Secretos del historial (H01) revisados: en GitHub solo están la clave del MySQL local y un `JWT_SECRET` antiguo; el equipo decidió no reescribir el historial y usar valores nuevos en producción | 🧪 ESCRITO | `DESPLIEGUE.md` paso 1 |
| Push (`e504811..e8b3d1b`) y servicio en Render (Blueprint, región Virginia) con MySQL en Aiven y MongoDB Atlas M0; credencial nueva de Google en un proyecto propio | 🧪 ESCRITO (`/salud` = ok; inicio, catálogo con 27 productos, planes, acerca, login y configurador en 200; HSTS, `nosniff`, `X-Frame-Options: DENY`; CSS/JS con hash y caché de 7 días; Google acepta la URI de vuelta) | `https://smartglass-g075.onrender.com`; `DESPLIEGUE.md` «Despliegue del piloto» |
| Datos de demostración coherentes para producción (`DEMO_DATOS=true`, una sola vez): 11 clientes, 28 cotizaciones, 20 pedidos (31 líneas), 9 opiniones de compradores, 3 carritos y 147 notificaciones con fechas de las últimas diez semanas | 🧪 ESCRITO (probado contra bases locales aparte `smartglass_demo`: totales = productos + envío + compensación en los 20 pedidos, stock sin negativos, ninguna fecha futura, CO₂ por etapas en las 28 cotizaciones; segundo arranque sin duplicar; bases de prueba borradas) | `DemoDatosSeeder`; `application.properties` (`smartglass.demo.*`) |
| Render suspendió la cuenta tras el primer despliegue (`503 suspend-by-user`); el piloto pasó a Railway con las mismas bases (contraseñas nuevas en Aiven y Atlas), la misma credencial de Google y la misma imagen, con los datos de demostración cargados en el primer arranque | 🧪 ESCRITO (`/salud` = ok; inicio, catálogo, configurador, planes, acerca y login en 200 en 0,3–0,5 s; `/admin/planes` sin sesión 401 y 404 en rutas inexistentes; HSTS, `nosniff`, `X-Frame-Options: DENY`; Google pide volver a `https://smartglass-production.up.railway.app/login/oauth2/code/google`; «Más vendidos» con los pedidos de demostración; guía nueva servida) | `https://smartglass-production.up.railway.app`; `DESPLIEGUE.md` «Railway, paso a paso» |
| Pendiente del equipo: registrar el dominio de Railway en Google Cloud, quitar `DEMO_DATOS`, probar el login con Google y el del admin, y la respuesta de soporte de Render | POR HACER | — |

## Optimización antes del despliegue (rama `optimizacion-pre-despliegue`)

Seis tandas con gate: U1 idiomas → U2 rendimiento → U3 SaaS visible (plan editable por cualquier admin, página pública de planes y preview animada) → U4 CO₂ por etapas (cambio de contrato aprobado) → U5 inmersión → U6 guía automatizada (simulada, sin guardar datos; termina en el inicio de sesión).

### U1 · Idiomas en todo el proyecto

| Ítem | Estado | Evidencia |
|---|---|---|
| Catálogo en inglés: `nombreEn` / `descripcionEn` en cada producto; el seeder rellena los 27 productos sembrados sin pisar lo que escriba el admin; el formulario de nuevo producto los pide (opcionales) | 🧪 ESCRITO (rastreo en inglés: catálogo, destacados, comparador y detalle sin español) | `CatalogoProducto`, `TraduccionesCatalogoSeeder`, `ContenidoLocalizado` (bean `@contenido`) |
| Carrito: productos copiados antes de U1 se resuelven por id; obras a medida "Templado · obra a medida" con el tipo traducido | 🧪 ESCRITO | `ContenidoLocalizado.nombre` |
| Recomendación del análisis (7 frases fijas de `AnalisisService`) traducida al mostrarla, sin tocar el cálculo | 🧪 ESCRITO | `ContenidoLocalizado.recomendacion` |
| Avisos del servidor (admin, carrito, pago, registro, tienda), errores de `AuthService`/`PedidoService`/`ResenaService` y validaciones del registro con claves i18n | 🧪 ESCRITO (registro inválido en inglés: "Invalid phone number…") | `Textos`, validadores `{valid.*}` |
| Scripts del admin (reseñas, pedidos, productos) y 3 textos fijos de plantillas | 🧪 ESCRITO | `RV_TXT` en `admin/resenas.html`; `SG_TXT` |
| Reseñas: texto de los clientes, no se traduce; en inglés se avisa "Reviews are shown in the language they were written in." | 🧪 ESCRITO | `detalle.html` |
| Páginas con sesión en inglés: Mis cotizaciones, Mis pedidos, cuenta, checkout, factura, confirmación, ficha y panel admin; líneas de pedido y nombres de producto del admin traducidos (los pedidos guardan el nombre de la compra y se muestra en el idioma activo) | 🧪 ESCRITO (rastreo con sesión: solo quedan reseñas y nombres de personas) | `ContenidoLocalizado.nombrePedido`, `AdminController` (reseñas) |

### U2 · Rendimiento

Medido en local (servidor con curl; navegador con la Performance API). El servidor ya respondía rápido salvo el catálogo en prod; lo que se notaba era peso de red, repintados y registros de desarrollo.

| Ítem | Antes → después | Estado | Evidencia |
|---|---|---|---|
| Fondo del inicio y del login: era un PNG de 2,2 MB con extensión `.jpg` | 2.248 KB → 229 KB (JPEG real, misma imagen; original en `scripts/originales/`) | 🧪 ESCRITO | `scripts/Imagenes.java` |
| Logo en barras, pie y favicon (se muestra a 28–34 px) | 236 KB → 21 KB (copia a 128 px; el logotipo original no se toca y lo siguen usando factura y ficha) | 🧪 ESCRITO | `img/logo-icono-sm.png` |
| Iconos Phosphor desde unpkg: 164 KB de CSS que bloqueaban el pintado + 280 KB de fuentes, para 83 iconos | 9 KB de CSS (1,5 KB comprimido) + 17 KB de fuentes, alojados aquí | 🧪 ESCRITO (0 iconos vacíos en inicio, catálogo y configurador) | `scripts/iconos_phosphor.py`; `IconosPhosphorTest` falla si se usa un icono sin regenerar |
| Google Fonts (dos conexiones a terceros antes de pintar) | Alojadas aquí con *preload* (latin y latin-ext); 0 peticiones externas por página | 🧪 ESCRITO | `scripts/fuentes_google.py`; `fragments/recursos.html` |
| Peso total del inicio | ≈ 2,7 MB → 420 KB | 🧪 ESCRITO | Performance API |
| Prod: las imágenes de `/img` salían con `no-store` (se bajaban en cada página) | Caché de 7 días como CSS y JS | 🧪 ESCRITO | `UploadsWebConfig` |
| Prod: el catálogo tardaba ~90 ms (el filtro de versiones buscaba cada enlace `/detalle/…` en el JAR por el patrón `/**`) | ~90 ms → ~28 ms; configurador 18 → 8 ms | 🧪 ESCRITO | `EstaticosWebConfig` (un manejador por carpeta), `spring.web.resources.add-mappings=false` |
| Con sesión, cada página buscaba al usuario 3 veces en MySQL (≈ 3 viajes a la base remota en prod) | 1 por petición | 🧪 ESCRITO | `UsuarioSesion` (memoria de la petición); `VistaGlobalAttributesAdvice` la reutiliza |
| Desplazamiento del catálogo: 27 tarjetas con `backdrop-filter` sobre un fondo difuso | Peor fotograma 34,6 ms → 7,5 ms, sin cambio visible | 🧪 ESCRITO | `theme.css` (tarjetas sin desenfoque; barra, diálogos y auth lo conservan) |
| Animaciones sin fin (arcoíris, paneles del inicio, vidrio del configurador, evolución, fondo del login) repintaban fuera de pantalla | En pausa mientras no se ven | 🧪 ESCRITO | `main.js` `pausarAnimacionesFuera`, `.sg-fuera` |
| Aparición de tarjetas al desplazar se sentía lenta | 0,7 s + 280 ms de escalonado → 0,45 s + 160 ms | 🧪 ESCRITO | `theme.css` `.sg-reveal`, `main.js` |
| Desarrollo: SQL y trazas DEBUG de seguridad en cada petición (también por cada CSS e imagen) | Apagados por defecto; `SHOW_SQL=true` y `LOG_SEGURIDAD=DEBUG` los vuelven a encender | 🧪 ESCRITO | `application.properties` |
| Configurador: redibujo por cambio de campo | 0,16 ms por `render` (ya iba bien; sin cambios) | — | Performance API |

### U3 · SaaS visible

Decisión del equipo: cualquier admin edita el plan (sin lista de superadmins); la página pública no lleva precios.

| Ítem | Estado | Evidencia |
|---|---|---|
| Planes dentro del panel: pestaña «Planes» en la barra del admin, `/admin/planes` con plan, funciones, historial y estudio de mercado (plegable). `/superadmin` redirige ahí | 🧪 ESCRITO (con sesión de admin: aplicar Básico → «0 de 7», la tienda queda con 4 formas y sin sello, idioma ni cifras, y la página pública marca Básico; ajuste «Activar siempre» del corte → «1 de 7» y aparece «Personalizada»; vuelto a «Según el plan» y Avanzado, historial con los 4 cambios. Sin sesión: `/admin/planes` → login; `/superadmin` → 302) | `AdminPlanesController`, `admin/planes.html`, `admin/fragments/nav.html` |
| `SUPERADMIN_USUARIOS` retirada del código, `application.properties`, `.env.example`, `render.yaml` y `DESPLIEGUE.md` | 🧪 ESCRITO | `SecurityConfig` (ya no hay regla para `/superadmin`) |
| Página pública «Para empresas» (`/planes`): los dos planes, «Precio por definir», qué incluye cada uno, cómo elegir y la nota de fase piloto; marca el plan que usa esta demo. Enlace en la barra pública y en el pie | 🧪 ESCRITO (es-CO y en revisadas; en inglés sin restos en español) | `planes.html`, `HomeController.planes` |
| Vista previa animada: una vidriería de ejemplo cuyas piezas se apagan con Básico (idioma, ofertas, cifras, 9 formas, corte libre, sello y ficha quedan como huecos punteados con candado); la escena recorre las formas del plan; lista en texto al lado y resumen con `aria-live` cuando lo cambia la persona | 🧪 ESCRITO (escritorio, 375 px sin desborde, tema claro y oscuro) | `fragments/plan-preview.html`, `js/planes-preview.js`, `theme.css` «U3 · Vista previa de planes» |
| Movimiento de la vista previa: alterna sola cada 5,5 s con botón de pausa (WCAG 2.2.2); elegir un plan la detiene; fuera de pantalla o pestaña oculta no corre; con fotosensible o movimiento reducido ni alterna ni anima | 🧪 ESCRITO (probado: alterna sin modo; quieta y sin botón con fotosensible) | `planes-preview.js` |
| Barra pública: con «Para empresas» no cabía en una línea; menú hamburguesa hasta 1180 px (antes 950) y franja compacta 1181–1360 px | 🧪 ESCRITO (68 px de alto a 1181, 1200 y 1370 px) | `theme.css` |
| Barra del admin (7 pestañas, ya se partía entre 951 y ~1220 px antes de U3): hasta 1440 px la cuenta muestra solo el avatar (el nombre queda para lectores de pantalla) y hasta 1360 px enlaces y márgenes más juntos | 🧪 ESCRITO (una línea a 1181, 1361, 1441 y 1600 px; hamburguesa a 1100 px) | `theme.css` (`.navbar--admin`) |

### U4 · CO₂ por etapas (cambio de contrato aprobado)

Decisiones del equipo: la energía renovable cubre solo la electricidad del taller (efecto real); transporte fijo de 300 km; la capa activa de inteligente y fotovoltaico conserva el +80 % del modelo anterior, declarado como estimación.

**Modelo** (`AnalisisService.huellaPorEtapas` = `glass-preview.js` `etapas`), kg CO₂e por pieza:

| Etapa | Fórmula | Fuente |
|---|---|---|
| Fusión del vidrio flotado | peso × 1,03 × (1 − 0,5 × 0,15 con calcín) | EPD AGC Planibel (9,8 kg/m² a 4 mm, 5 % bajo la media europea) y Guardian ExtraClear (9,64); calcín medio 26 % (Glass for Europe 2024); −5 % por cada 10 puntos de calcín (FEVE) |
| Templado (Templado, Templado + Laminado) | área × espesor × 0,8 kWh × (0,220 red SIN │ 0,03 renovable) | Fortemp (4 kWh/m² a 5 mm); UPME Res. 000085 de 2026 (SIN 2024, inventarios); IPCC AR5 Anexo III (solar 48, eólica 11 g/kWh) |
| Lámina de PVB (Laminado, Templado + Laminado) | área × 3,6 | Eastman Saflex R, LCA revisada por Quantis (4,5 kg/kg; 3,6 kg/m² a 0,76 mm) |
| Capa activa (Inteligente, Fotovoltaico) | fusión × 0,8 | Estimación (factor del modelo anterior); sin EPD pública comparable por m² |
| Transporte | peso / 1000 × 300 km × 0,097 | GLEC 2019 vía Climatiq (carretera, del pozo a la rueda); distancia supuesta |

Umbrales de intensidad sin cambios (12 y 20 kg/m²). Precios, composición y energía requerida sin cambios.

**Anclas nuevas** (compensar desactivado; precio igual que antes):

| Ancla | Antes | Ahora |
|---|---|---|
| A · Templado 105 × 105 × 7, calcín y renovable | 9,8 kg · 8,9 · Favorable | 19,1 kg · 17,4 · Moderado · $175.959 |
| B · Templado + Laminado 105 × 105 × 20, sin opciones | 80,1 · Alto | 68,6 kg · 62,2 · Alto · $1.234.800 (recomendación: Laminado 12 mm → 34,2, sigue alto) |
| C · Templado 105 × 105 × 40, calcín y renovable | 51,0 · Alto | 109,3 kg · 99,1 · Alto (recomienda 8 mm) |
| D · Templado 400 × 400 × 8, calcín y renovable | 163,2 · 10,2 · Favorable | 317,3 kg · 19,8 · Moderado · $2.918.400 |

| Ítem | Estado | Evidencia |
|---|---|---|
| Servidor y configurador con las mismas etapas | 🧪 ESCRITO (anclas idénticas en Java, Node y el configurador del navegador) | `HuellaPorEtapasTest` (6 pruebas); `AnalisisService`, `glass-preview.js` |
| Productos de catálogo con las mismas etapas (sin calcín extra ni renovable) | 🧪 ESCRITO | `calcularHuellaCarbonoItem` |
| Cada obra nueva guarda su desglose y la versión del método (`etapas-2026-10`); las anteriores conservan su huella y lo dicen | 🧪 ESCRITO — falta verlo con sesión (guardar una obra nueva y abrir una antigua) | `VidrioSostenible.huellaEtapas` / `metodoHuella` |
| Intensidad de la obra guardada con el área exacta (antes con el área redondeada a 2 decimales: 62,3 frente a 62,2) | 🧪 ESCRITO | `VidrioSostenible.getIntensidadCarbono` |
| «De dónde sale el CO₂»: desglose con barras en el configurador, el detalle de la obra y la ficha ambiental | 🧪 ESCRITO (configurador visto) | `vidrio-personalizado-form.html`, `vidrio-personalizado-detalle.html`, `vidrio-ficha.html` |
| Metodología en Acerca: 6 pasos y 9 fuentes enlazadas; textos de las opciones de calcín y renovable con su efecto real; inicio sin el «−45 %» | 🧪 ESCRITO (es-CO y en revisados, sin restos en español) | `acerca.html`, `index.html`, `messages*.properties` |

### U5 · Inmersión

Todo con `transform`/`opacity` o propiedades registradas; el modo fotosensible y `prefers-reduced-motion` lo apagan.

| Ítem | Estado | Evidencia |
|---|---|---|
| Transición entre páginas: la que se va se desvanece (0,16 s) y la nueva entra con un leve ascenso (0,34 s); la barra superior tiene su propia capa y no parpadea. Chrome/Edge 126+ y Safari 18.2+; en Firefox la navegación sigue normal | 🧪 ESCRITO (`pageswap` con `viewTransition` al ir de inicio a catálogo) | `theme.css` `@view-transition`, `view-transition-name: barra` |
| El cambio de tema conserva su revelado circular (sus reglas quedan solo para `html.sg-vt`) | 🧪 ESCRITO (oscuro → claro → oscuro sin errores) | `theme.css` |
| La luz ambiental acompaña el desplazamiento (sube y se abre al bajar), sin JS | 🧪 ESCRITO (catálogo: p95 7,1 ms y peor fotograma 7,2 ms con la capa promovida; igual que sin efecto) | `theme.css` `animation-timeline: scroll(root)` |
| El escenario del configurador muestra un halo verde, ámbar o rojo según el nivel de CO₂ (decorativo: el termómetro ya lo dice con texto e icono) | 🧪 ESCRITO (4 mm favorable, 7 moderado, 20 alto) | `theme.css` `--stage-halo`, `vidrio-personalizado-form.html` |
| Fotosensible: sin luz que se mueve, sin transición entre páginas ni del halo | 🧪 ESCRITO (luz ambiental `none`, halo 0 s) | `theme.css` |

### U6 · Guía automatizada

Decisión del equipo: simular sin guardar. Sustituye al recorrido estático de T4 (`inicializarTour` sale de `main.js`).

| Ítem | Estado | Evidencia |
|---|---|---|
| 15 pasos por inicio → catálogo (escribe en la búsqueda, marca y desmarca dos productos para comparar) → ficha de un producto → configurador (mueve medidas, cambia a Laminado de 8 mm, forma Arco, quita y pone el calcín, abre «De dónde sale el CO₂») → editor de corte (Hexagonal, girar, reflejar; se cierra sin aplicar) → botón de cotizar (no se pulsa) → planes → idioma y accesibilidad → `/login?guia=fin` con aviso | 🧪 ESCRITO (recorrido completo sin errores hasta el login) | `js/guia.js`, `fragments/tour.html`, `login.html` |
| Nada se guarda ni se envía: todo envío de formulario queda bloqueado mientras corre; no pulsa carrito, cotizar ni los interruptores de accesibilidad; al terminar o salir restaura el borrador del editor y la selección del comparador | 🧪 ESCRITO (envío bloqueado con aviso; borrador y comparador previos restaurados tal cual) | `guia.js` |
| Cursor simulado con pulso de clic; el elemento queda en el tercio superior para que la caja no lo tape; barra de progreso «Paso n de 15» | 🧪 ESCRITO | `theme.css` «U6 · Guía automatizada» |
| **Arreglo (26/10/2026):** en escritorio el configurador tiene dos paneles con desplazamiento propio y la guía movía la ventana hasta el pie de página buscando lo que estaba dentro del panel. Ahora desplaza el panel, mueve la ventana solo lo justo y centra cada objetivo en la franja libre entre la barra y la caja del recorrido; el pie de página no entra en esa franja si el objetivo cabe más abajo | 🧪 ESCRITO (1900 × 980: termómetro del paso 10 visible dentro de su panel, ventana sin bajar, sin pie de página; filtros y tarjeta del catálogo al 48–50 % de la franja libre. 375 × 812: pasos 8–12 al 50 % sobre la caja; en el paso 12 el pie queda detrás de la caja) | `guia.js` (`panelDe`, `zonaLibre`, `centrar`, `irA`) |
| Controles: Pausar/Reanudar (WCAG 2.2.2), Siguiente, Salir y Esc; si la persona navega a otra página, el recorrido espera con «Continuar» | 🧪 ESCRITO | `guia.js` |
| Fotosensible / movimiento reducido: sin cursor, acciones inmediatas y sin avance automático (cada paso espera a Siguiente) | 🧪 ESCRITO (12 s sin avanzar; sin botón de pausa ni cursor) | `guia.js`, `theme.css` |
| Primera visita: invitación «¿Te mostramos SmartGlass?» en vez de abrir el recorrido solo; se inicia también desde /guia o con `?tour=1` | 🧪 ESCRITO (aparece una vez; «Ahora no» la recuerda) | `guia.js` |
| Textos en es-CO y en (la búsqueda de ejemplo usa la palabra del idioma activo) | 🧪 ESCRITO | `messages*.properties` (`guia2.*`) |

### Pruebas antes del push (25/10/2026, con la sesión del admin)

| Ítem | Estado | Evidencia |
|---|---|---|
| Cotización nueva con el método `etapas-2026-10` (Templado 105 × 105 cm, 7 mm, calcín y energía renovable): el detalle y la ficha muestran fusión 18,4 · templado 0,2 · transporte 0,6 kg (total 19,1 kg); la vista de impresión oculta menú, botones y guía y conserva las etapas | 🧪 ESCRITO (navegador) | Obra `…e07267` (referencia E07267) |
| Obra antigua sin etapas: el detalle y la ficha muestran «Huella calculada con el método anterior…» y ninguna etapa inventada | 🧪 ESCRITO (navegador) | Obra `…ea5bd10` |
| Pedido de prueba desde el carrito (obra 04BCC7 y Espejo Solar Reflectivo) con compensación y los tres compromisos: recargo de $ 5.000, total $ 670.709, factura SG-2026-000014 con huella y compromisos | 🧪 ESCRITO (navegador) | Pedido 14 |
| **Arreglo:** el subtotal de cada línea del carrito salía «$180,709» (dependía del idioma del sistema); ahora «$180.709» siempre (D2) | 🧪 ESCRITO | `ItemCarrito.getSubtotalFormateado` |
| **Arreglo:** la confirmación y la factura mostraban el código `TARJETA_CREDITO`; ahora el nombre traducido («Tarjeta de crédito o débito», «PSE o transferencia») | 🧪 ESCRITO (navegador: pedido 14) | `Pedido.getMetodoPagoClave`, `pedido-confirmacion.html`, `factura.html` |

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
| H01 · Secretos fuera del repositorio | 🧪 ESCRITO | Variables sin valor por defecto en `application.properties:8,17,44-45`; contraseña del admin inicial desde `ADMIN_PASSWORD` (`DataInitializer.java:26,47`); plantilla `.env.example`; `.env` en `.gitignore`. **Revisado el 25/10/2026:** en el historial publicado solo están la clave del MySQL local y un JWT antiguo (Google, admin y Atlas nunca se publicaron). Decisión del equipo: no reescribir el historial; producción usa un JWT generado por Render y la clave propia del MySQL gestionado |
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
| Planes SaaS (mejoras T5) | Con `SUPERADMIN_USUARIOS` definido, `/superadmin` deja cambiar de plan; en Básico desaparecen las 7 funciones de Avanzado y al volver todo reaparece; un usuario fuera de la lista ve 404 |
| Recorrido guiado (T4) | Sale solo la primera vez; "Saltar" o Esc lo cierran; con fotosensible no se mueve nada; se relanza desde "Guía de compra" en el pie |

Nada se da por cerrado hasta que los greps den el resultado esperado, el app arranque con las env vars, el toggle de idioma mantenga las anclas en es-CO, el flujo anónimo→pago-con-login conserve el carrito, el stock sea transaccional y veraz, las notificaciones y el tour respeten i18n y accesibilidad, y los casos A–D se vean en navegador; ningún cambio movió un decimal del contrato ni tocó el logotipo, y D1–D4 no se aplicaron sin mi visto bueno.
