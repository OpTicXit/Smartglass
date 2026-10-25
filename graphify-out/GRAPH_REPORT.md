# Graph Report - Smartglass  (2026-10-25)

## Corpus Check
- 72 files · ~91,171 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1611 nodes · 3565 edges · 98 communities (32 shown, 66 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 433 edges (avg confidence: 0.85)
- Token cost: 309,087 input · 0 output

## Community Hubs (Navigation)
- main.js: interfaz global
- Entidad VidrioSostenible
- Editor de corte (corte-editor.js)
- Validaciones Jakarta
- DTO CotizacionVidrioRequest
- Calculo de cotizacion
- Modelo CatalogoProducto
- Paginas de tienda y fragmentos
- Entidad Pedido
- Entidad DetallePedido
- Controladores de perfil y auth
- Plataforma y persistencia (docs)
- Resenas
- Entidad Usuario
- Seeders y repos de catalogo
- Tienda y ProductoService
- SecurityConfig
- CarritoService
- Modelo predictivo (documentos)
- Entidad Notificacion
- NotificacionService y API
- I18nConfig
- Aspectos de stock y notificaciones
- Registro (RegistroRequest)
- VidrioSostenibleService (estados)
- Exito de login
- CheckoutController
- Geometria del corte (PuntoCorte)
- StockService
- Filtros de seguridad HTTP
- Esquema de precios y seeder
- AnalisisService (contrato)
- Vistas de login y registro
- Repos y servicios de usuario
- Panel admin de cotizaciones
- Pipeline de mineria (Python)
- FilaCotizacion
- VidrioPersonalizadoWebController
- Configurador: envio y alertas
- AuthService y login API
- JwtProvider
- Repositorios Mongo
- UserDetailsServiceImpl
- API REST de vidrios
- ItemCarrito
- PedidoService
- CotizacionVidrioResponse
- Vista del detalle de obra
- SesionAnonima (T1)
- AdminController y modelo
- UsuarioResponse
- Panel de cotizaciones (docs)
- Contrato JS (resumen)
- Navegacion y panel admin
- CarritoController
- glass-preview.js (render)
- Admin de productos (vista)
- Flujo de trabajo: decisiones
- Hallazgos H
- PermisosAdvice
- LoginRateLimitFilter
- Mas vendidos (repositorio)
- Documento Carrito
- RateLimitFilter
- Assets y tandas UX
- Reestructuracion comercial
- HomeController
- PerfilAccesibilidadController
- Vistas de inicio y configurador
- Mis cotizaciones y cuenta
- VistaGlobalAttributesAdvice
- ResenaSeeder
- ComparacionItem
- TokenResponse
- TokenBucket
- Flujo de trabajo (estructura)
- Tests de la app
- SmartglassApplication
- Contador animado (JS)
- Importancia de variables
- Artefacto Maven
- altura() del configurador
- ligar() del configurador

## God Nodes (most connected - your core abstractions)
1. `CatalogoProducto` - 114 edges
2. `VidrioSostenible` - 92 edges
3. `Usuario` - 73 edges
4. `Pedido` - 50 edges
5. `CotizacionVidrioRequest` - 47 edges
6. `CarritoService` - 44 edges
7. `NotificacionService` - 40 edges
8. `DetallePedido` - 36 edges
9. `VidrioSostenibleService` - 33 edges
10. `ProductoService` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Resumen ejecutivo` --references--> `AnalisisService`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/java/com/smartglass/service/AnalisisService.java
- `Respaldo isotipo 'Foto en preparacion' (F18)` --semantically_similar_to--> `admin/productos.html (catalogo admin)`  [INFERRED] [semantically similar]
  documentos/assets-catalogo-T2.md → src/main/resources/templates/admin/productos.html
- `Quotation actions (approve / add to cart / delete)` --references--> `VidrioPersonalizadoController`  [INFERRED]
  src/main/resources/templates/mis-cotizaciones.html → src/main/java/com/smartglass/controller/VidrioPersonalizadoController.java
- `Order summary (invoice, payment, CO2, total)` --shares_data_with--> `Pedido`  [INFERRED]
  src/main/resources/templates/pedido-confirmacion.html → src/main/java/com/smartglass/model/mysql/Pedido.java
- `Resumen ejecutivo` --references--> `calcular()`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/resources/static/js/glass-preview.js

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **CO2 data mining pipeline (generate, preprocess, train, evaluate)** — readme_generacion_dataset, readme_smartglass_co2_dataset, readme_preparacion, readme_analisis_notebook, readme_random_forest_model, readme_metricas_modelo [EXTRACTED 1.00]
- **CO2 predictive model pipeline (dataset, predictors, RF, metrics)** — documentos_informe_metodologico_dataset_sintetico_co2, documentos_informe_metodologico_variables_predictoras, documentos_informe_metodologico_exclusion_masa_kg, documentos_informe_metodologico_random_forest_regressor, documentos_informe_metodologico_metricas_modelo, documentos_informe_metodologico_co2_kg [EXTRACTED 1.00]
- **Sub-tandas con gate de la reestructuracion comercial (T0 i18n -> T4 onboarding)** — flujo_de_trabajo_t0_i18n, flujo_de_trabajo_t1_login_solo_al_pagar, flujo_de_trabajo_t2_stock_ofertas_precios, flujo_de_trabajo_t3_notificaciones_polling, flujo_de_trabajo_t4_onboarding [EXTRACTED 1.00]
- **Decisiones de diseno D1-D4 aplicadas** — flujo_de_trabajo_d1_stock_transaccional, flujo_de_trabajo_d2_sin_i18n_numeros, flujo_de_trabajo_d3_balanceo_precios_catalogo, flujo_de_trabajo_d4_login_al_pagar_gate [EXTRACTED 1.00]
- **Patron de busqueda normalizada + chips de filtro en cliente en paneles admin** — src_main_resources_templates_admin_productos_buscador_filtros_tipo, src_main_resources_templates_admin_usuarios_buscador_filtro_rol, src_main_resources_templates_admin_resenas_vistageneral [INFERRED 0.85]
- **T2 estado comercial y precios de catalogo (stock real, ofertas)** — src_main_resources_templates_fragments_comercio_estado, src_main_resources_templates_fragments_comercio_precio, src_main_resources_templates_catalogo_page, src_main_resources_templates_detalle_page, src_main_resources_templates_comparar_page [EXTRACTED 1.00]
- **Flujo de compra: catalogo -> detalle -> carrito -> checkout -> factura** — src_main_resources_templates_catalogo_page, src_main_resources_templates_detalle_page, src_main_resources_templates_carrito_page, src_main_resources_templates_checkout_page, src_main_resources_templates_factura_page [INFERRED 0.85]
- **T4 onboarding: recorrido guiado y guia de compra** — src_main_resources_templates_fragments_tour_tour, src_main_resources_templates_guia_page, src_main_resources_templates_fragments_navbar_publico_navbar_publico, src_main_resources_templates_fragments_footer_footer [INFERRED 0.85]
- **Atelier live configurator update flow** — src_main_resources_templates_vidrio_personalizado_form_leer, src_main_resources_templates_vidrio_personalizado_form_paracalculo, src_main_resources_templates_vidrio_personalizado_form_preciode, src_main_resources_templates_vidrio_personalizado_form_actualizar, src_main_resources_templates_vidrio_personalizado_form_gestionaraviso, src_main_resources_templates_vidrio_personalizado_form_termometro [EXTRACTED 1.00]
- **Registration client-side validation** — src_main_resources_templates_registro_sugerencia, src_main_resources_templates_registro_comprobardisponibilidad, src_main_resources_templates_registro_reglas, src_main_resources_templates_registro_telefonovalido, src_main_resources_templates_registro_submit_handler, src_main_resources_templates_registro_honeypot [EXTRACTED 1.00]
- **Carbon footprint display across pages** — src_main_resources_templates_vidrio_personalizado_form_termometro, src_main_resources_templates_vidrio_personalizado_detalle_huella, src_main_resources_templates_mis_cotizaciones_render_script, src_main_resources_templates_mis_pedidos_huella_compensada, src_main_resources_templates_pedido_confirmacion_resumen_orden [INFERRED 0.75]

## Communities (98 total, 66 thin omitted)

### Community 0 - "main.js: interfaz global"
Cohesion: 0.05
Nodes (62): cerrarAlerta(), confirmar(), idiomaMarkup(), inicializarAccesibilidad(), actuales(), aplicar(), desdeTexto(), guardarServidor() (+54 more)

### Community 2 - "Editor de corte (corte-editor.js)"
Cohesion: 0.08
Nodes (51): abrir(), actualizarBotones(), anadirPunto(), aplicarFormaBase(), aplicarRecomendado(), areaPoligono(), aristaMasCercana(), aTexto() (+43 more)

### Community 3 - "Validaciones Jakarta"
Cohesion: 0.12
Nodes (10): EmailValido, EmailValidoValidator, FieldMatch, FieldMatchValidator, PasswordValida, PasswordValidaValidator, TelefonoValido, TelefonoValidoValidator (+2 more)

### Community 7 - "Paginas de tienda y fragmentos"
Cohesion: 0.11
Nodes (35): Objetivos de Desarrollo Sostenible (ODS 9, 11, 12, 13), acerca.html (Nuestra mision y sostenibilidad), Pilares de innovacion (geometria, transparencia ambiental, economia circular), Script auto-envio de cantidades del carrito, carrito.html (Tu carrito), Barra flotante del comparador, Filtros de catalogo (categoria y tamano), catalogo.html (Catalogo) (+27 more)

### Community 10 - "Controladores de perfil y auth"
Cohesion: 0.13
Nodes (4): UsuarioSesion, AuthController, PerfilIdiomaController, ErrorResponse

### Community 11 - "Plataforma y persistencia (docs)"
Cohesion: 0.08
Nodes (24): analisis.ipynb (EDA, training, evaluation), AnalisisService (deterministic CO2 quoter), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Carrito document, CatalogoProducto document, DetallePedido entity, generacion_dataset.py generator, Google OAuth2 login (+16 more)

### Community 12 - "Resenas"
Cohesion: 0.11
Nodes (3): Resena, ResenaRepository, ResenaService

### Community 14 - "Seeders y repos de catalogo"
Cohesion: 0.17
Nodes (4): CatalogoComercioSeeder, CatalogoEsculturasSeeder, CatalogoProductoSeeder, CatalogoProductoRepository

### Community 15 - "Tienda y ProductoService"
Cohesion: 0.14
Nodes (3): TiendaController, UsuarioDashboardController, ProductoService

### Community 18 - "Modelo predictivo (documentos)"
Cohesion: 0.12
Nodes (21): Informe metodologico (Entregable 3: mineria de datos, regresion CO2), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Recycled glass (calcin_pct) reduces emissions (corr -0.45), co2_kg target variable, Synthetic CO2 dataset (1000 records, 20 glass types, data/generacion_dataset.py), Guardian Glass: How glass is made, Ley 2169 de 2021 (carbono-neutralidad Colombia), Model metrics: MAE 20.69 kg, RMSE 33.49 kg, R2 0.9461, CV 5-fold 0.9456 +/- 0.0123 (+13 more)

### Community 20 - "NotificacionService y API"
Cohesion: 0.19
Nodes (3): NotificacionController, Lector, NotificacionService

### Community 21 - "I18nConfig"
Cohesion: 0.13
Nodes (3): I18nConfig, HandlerInterceptor, UploadsWebConfig

### Community 24 - "VidrioSostenibleService (estados)"
Cohesion: 0.13
Nodes (5): ResultadoCambio, NO_ENCONTRADA, OK, TRANSICION_INVALIDA, VidrioSostenibleService

### Community 26 - "CheckoutController"
Cohesion: 0.18
Nodes (7): CheckoutController, mis-pedidos.html (my orders template), Compensated carbon footprint stat, Order card with line items and invoice link, pedido-confirmacion.html (order confirmation template), Order summary (invoice, payment, CO2, total), Order next-steps timeline (received, fabrication, shipping, delivery)

### Community 33 - "Vistas de login y registro"
Cohesion: 0.14
Nodes (17): login.html (login page template), Login lockout alert (too many attempts), Continue with Google (OAuth2), Login form (POST /login), registro.html (registration page template), comprobarDisponibilidad() live email/username availability, Registration form (POST /registro), intl-tel-input 18.2.1 phone country selector (+9 more)

### Community 34 - "Repos y servicios de usuario"
Cohesion: 0.29
Nodes (6): DataInitializer, RegistroController, UsuarioRepository, AuthService, CustomOAuth2UserService, UserService

### Community 36 - "Pipeline de mineria (Python)"
Cohesion: 0.13
Nodes (3): cargar(), dividir(), preprocesador()

### Community 39 - "Configurador: envio y alertas"
Cohesion: 0.17
Nodes (16): actualizar() live preview update, actualizarFades() scroll fade masks, aplicarConfig() apply recommended configuration, High-impact confirmation dialog, gestionarAviso() impact alert manager, hayCorte(), leer() read form configuration, listar() (+8 more)

### Community 50 - "Vista del detalle de obra"
Cohesion: 0.19
Nodes (12): Pay-requires-login notice (T1 D4), vidrio-personalizado-detalle.html (quotation detail / La Obra), Material anatomy bars, External resistance API (analizar-resistencia on Railway), Balanced-version link to configurator, Investment and approval panel, Structural analysis and external verification, Carbon footprint ring and impact level (+4 more)

### Community 54 - "Panel de cotizaciones (docs)"
Cohesion: 0.22
Nodes (10): F15 Badge de cotizaciones pendientes en el nav admin, F32 Ruta del panel de cotizaciones, F42 Paginacion (por hacer), Items F (funcionalidad F1-F45), admin-cotizaciones.html (panel de aprobacion de cotizaciones), Acciones Aprobar/Rechazar cotizacion (POST /admin/cotizaciones/{id}), Filtros por estado de cotizacion (TODAS/COTIZACION/APROBADO/RECHAZADA), Metricas Proceso (intensidad con color) y Pieza (CO2 neutro) (+2 more)

### Community 55 - "Contrato JS (resumen)"
Cohesion: 0.25
Nodes (9): Resumen ejecutivo, calcular(), equivalencias(), fmt(), fmtAuto(), fmtNum(), impacto(), recomendar() (+1 more)

### Community 56 - "Navegacion y panel admin"
Cohesion: 0.24
Nodes (7): T3 Notificaciones por polling (sin WebSocket), admin/dashboard.html (panel admin), Tabla de ultimos pedidos, Fragmento navbar del admin, Guardado en lote de estados de pedidos (form-estados), Formulario cambiar rol (POST /admin/usuarios/{id}/rol), Buscador y chips de filtro por rol de usuario

### Community 58 - "glass-preview.js (render)"
Cohesion: 0.29
Nodes (7): agujeros(), contorno(), describir(), f(), num(), render(), selector() visual picker over select

### Community 59 - "Admin de productos (vista)"
Cohesion: 0.20
Nodes (8): Esculturas del catalogo (7 piezas ESC-G/M/P), admin/productos.html (catalogo admin), Buscador y chips de filtro por tipo de producto, Modal Agregar producto (multipart, foto opcional), Subida de foto con drag and drop (max 5 MB), ruta() (router por hash #p=ID), vistaGeneral() (tarjetas por producto), vistaProducto() (resenas paginadas en cliente)

### Community 60 - "Flujo de trabajo: decisiones"
Cohesion: 0.24
Nodes (7): AUD-03 cambiarRol acepta cualquier texto, AUD-13 Miles con coma por String.format/locale, Hallazgos de auditoria abiertos (AUD-01 a AUD-15), Casos de verificacion en navegador A-D, T0 i18n de textos (es-CO, es-ES, en), Grid de KPIs (ingresos, CO2, pedidos, usuarios, productos), Selector de idioma (?lang=es-CO/es-ES/en)

### Community 61 - "Hallazgos H"
Cohesion: 0.20
Nodes (10): F41 Rate limit por actor, H04 Caja de las plantillas, H05 Limite de intentos de login, H08 requirements.txt, Hallazgos H (cierre de bloqueantes H01-H11), requirements.txt (mineria de datos, Python 3.13), joblib >=1.4, pandas >=2.2,<3 (+2 more)

### Community 73 - "Assets y tandas UX"
Cohesion: 0.25
Nodes (6): Esculturas (7 piezas nuevas), Fotos pendientes del catálogo (T2), Fotos realistas de los tipos de vidrio (20 productos existentes), Fotos realistas de tipos de vidrio (VID-001 a VID-020), Tandas de UX (rama ux-accesibilidad-catalogo), Panel de accesibilidad (daltonico / fotosensible)

### Community 74 - "Reestructuracion comercial"
Cohesion: 0.29
Nodes (5): Reestructuracion comercial (rama comercio-stock-notif, T0-T4), T1 Login solo al pagar, T2 Stock, ofertas y balanceo de precios del catalogo, T4 Onboarding (recorrido guiado y /guia), Modal Stock y oferta (POST /admin/productos/{id}/comercio)

### Community 78 - "Vistas de inicio y configurador"
Cohesion: 0.29
Nodes (8): index.html (home page template), Custom-glass configurator sample section, Benefits strip (beneficios-grid), dibujar() configurator sample renderer, Home hero with refracted glass panels, Product line showcase (vitrina), vidrio-personalizado-form.html (Atelier configurator), Forge waiting screen

### Community 79 - "Mis cotizaciones y cuenta"
Cohesion: 0.29
Nodes (8): mis-cotizaciones.html (my quotations gallery), Quotation actions (approve / add to cart / delete), Quotation gallery card (obra-card), Gallery mini-render and impact chip script, Quotation summary stats (total, pending, approved, CO2), usuario.html (user account dashboard), Account quick links (catalog, cart, orders, quotations), Featured products grid (destacados)

### Community 88 - "Flujo de trabajo (estructura)"
Cohesion: 0.33
Nodes (6): FLUJO DE TRABAJO · SmartGlass, Hallazgos de la auditoría aún abiertos (no tocados), Hallazgos H (cierre de bloqueantes), Leyenda, Lo que debe verificar el equipo en navegador, Ítems F (funcionalidad)

### Community 91 - "Contador animado (JS)"
Cohesion: 1.00
Nodes (3): contar(), paso(), texto()

## Ambiguous Edges - Review These
- `D2 Sin i18n de numeros (formato colombiano fijo)` → `Grid de KPIs (ingresos, CO2, pedidos, usuarios, productos)`  [AMBIGUOUS]
  src/main/resources/templates/admin/dashboard.html · relation: conceptually_related_to
- `F42 Paginacion (por hacer)` → `vistaProducto() (resenas paginadas en cliente)`  [AMBIGUOUS]
  FLUJO-DE-TRABAJO.md · relation: conceptually_related_to

## Knowledge Gaps
- **53 isolated node(s):** `com.smartglass:smartglass`, `Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers`, `Modulo Ambiental de SmartGlass`, `ODS 9, 12, 13`, `User stories US-09 and US-13` (+48 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 462 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **66 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CotizacionVidrioRequest` connect `DTO CotizacionVidrioRequest` to `Controladores de perfil y auth`, `Geometria del corte (PuntoCorte)`, `API REST de vidrios`, `Vistas de inicio y configurador`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **What connects `com.smartglass:smartglass`, `Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers`, `Modulo Ambiental de SmartGlass` to the rest of the system?**
  _53 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `main.js: interfaz global` be split into smaller, more focused modules?**
  _Cohesion score 0.05333333333333334 - nodes in this community are weakly interconnected._
- **Why does `vidrio-personalizado-form.html (Atelier configurator)` connect `Vistas de inicio y configurador` to `main.js: interfaz global`, `Editor de corte (corte-editor.js)`, `DTO CotizacionVidrioRequest`, `Configurador: envio y alertas`, `Mis cotizaciones y cuenta`, `Vista del detalle de obra`, `glass-preview.js (render)`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Should `Entidad VidrioSostenible` be split into smaller, more focused modules?**
  _Cohesion score 0.03860391327340032 - nodes in this community are weakly interconnected._

### Low-confidence Hints
_AMBIGUOUS edges — the extractor was unsure. Verify before acting on these._

- **What is the exact relationship between `D2 Sin i18n de numeros (formato colombiano fijo)` and `Grid de KPIs (ingresos, CO2, pedidos, usuarios, productos)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `F42 Paginacion (por hacer)` and `vistaProducto() (resenas paginadas en cliente)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._