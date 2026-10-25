# Graph Report - Smartglass  (2026-10-24)

## Corpus Check
- 1 files · ~74,901 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1308 nodes · 2720 edges · 67 communities (22 shown, 45 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 422 edges (avg confidence: 0.85)
- Token cost: 59,184 input · 0 output

## Community Hubs (Navigation)
- org.springframework.ui.Model
- Glass Quote Request DTO
- Polygon Cut Editor JS
- CO2 Model Papers & Glass Detail
- Public Navbar & Account Pages
- Custom Validators
- Platform Docs & Data Mining
- Sustainable Glass Entity
- Flujo de trabajo (estado del cierre)
- User Entity
- Contract Math & Polygon Area
- Glass Preview Renderer JS
- Order Line Items
- Order Entity
- Cart Service & Controller
- Catalog Product Model
- Glass Quote Calculation
- JWT Provider
- Google OAuth2 User Service
- Security Configuration
- Repository Interfaces
- Registration Request
- AuthController.java
- Resena
- AdminController
- ItemCarrito
- AuthService
- Quote Table Row
- CO2 Dataset Python Pipeline
- org.springframework.ui.Model (2)
- AdminController (2)
- F43 Registration Validation JS
- Rate Limit Filter
- Login Success Redirect
- Contract Math & Polygon Area (2)
- org.springframework.stereotype.Service
- ResenaService
- Glass Quote Response DTO
- Main JS (Theme, Compare)
- Data Seeders
- Admin Dashboard & Users Pages
- User Response & Lookup
- Login Rate Limit Filter
- org.springframework.web.bind.annotation.GetMapping
- Product Comparison Model
- Data Seeders (2)
- ProductoService
- LoginRequest
- Admin Catalog Page
- Admin Reviews App
- Catalog & Cart Pages
- org.springframework.context.annotation.Configuration
- Cart Service & Controller (2)
- Admin Quotes Page
- Checkout Page
- Product Comparison Page
- Application Tests
- Spring Boot Entry Point
- Flujo de trabajo (estado del cierre) (2)
- Maven Project

## God Nodes (most connected - your core abstractions)
1. `VidrioSostenible` - 106 edges
2. `Usuario` - 61 edges
3. `CatalogoProducto` - 56 edges
4. `CotizacionVidrioRequest` - 50 edges
5. `Pedido` - 48 edges
6. `UsuarioRepository` - 41 edges
7. `DetallePedido` - 37 edges
8. `CarritoService` - 31 edges
9. `VidrioSostenibleService` - 29 edges
10. `ResultadoAnalisisVidrio` - 29 edges

## Surprising Connections (you probably didn't know these)
- `Resumen ejecutivo` --references--> `AnalisisService`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/java/com/smartglass/service/AnalisisService.java
- `Resumen ejecutivo` --references--> `calcular()`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/resources/static/js/glass-preview.js
- `Resumen ejecutivo` --references--> `fmtAuto()`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/resources/static/js/glass-preview.js
- `Resumen ejecutivo` --references--> `fmtNum()`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/resources/static/js/glass-preview.js
- `Resumen ejecutivo` --references--> `impacto()`  [INFERRED]
  FLUJO-DE-TRABAJO.md → src/main/resources/static/js/glass-preview.js

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Admin panel pages sharing admin navbar fragment** — src_main_resources_templates_admin_fragments_nav_navbar, src_main_resources_templates_admin_dashboard_page, src_main_resources_templates_admin_cotizaciones_page, src_main_resources_templates_admin_pedidos_page, src_main_resources_templates_admin_productos_page, src_main_resources_templates_admin_usuarios_page, src_main_resources_templates_admin_resenas_page, src_main_resources_templates_admin_usuario_detalle_page [EXTRACTED 1.00]
- **Atelier live CO2 impact + recommended configuration flow** — src_main_resources_templates_vidrio_personalizado_form_actualizar, src_main_resources_templates_vidrio_personalizado_form_gestionaraviso, src_main_resources_templates_vidrio_personalizado_form_aplicarconfig, src_main_resources_templates_vidrio_personalizado_form_dlgimpacto, src_main_resources_templates_vidrio_personalizado_form_termometro_ambiental, src_main_resources_static_js_glass_preview_sgglass [EXTRACTED 1.00]
- **Balanced-version round trip from obra detail back to atelier** — src_main_resources_templates_vidrio_personalizado_detalle_nivel_impacto_ambiental, src_main_resources_templates_vidrio_personalizado_detalle_version_balanceada_link, src_main_resources_templates_vidrio_personalizado_form_prellenar, src_main_resources_templates_vidrio_personalizado_form_aplicarconfig [EXTRACTED 1.00]
- **CO2 data mining pipeline (generate, preprocess, train, evaluate)** — readme_generacion_dataset, readme_smartglass_co2_dataset, readme_preparacion, readme_analisis_notebook, readme_random_forest_model, readme_metricas_modelo [EXTRACTED 1.00]
- **CO2 predictive model pipeline (dataset, predictors, RF, metrics)** — documentos_informe_metodologico_dataset_sintetico_co2, documentos_informe_metodologico_variables_predictoras, documentos_informe_metodologico_exclusion_masa_kg, documentos_informe_metodologico_random_forest_regressor, documentos_informe_metodologico_metricas_modelo, documentos_informe_metodologico_co2_kg [EXTRACTED 1.00]
- **Post-purchase flow: confirmation -> orders -> invoice** — src_main_resources_templates_pedido_confirmacion, src_main_resources_templates_mis_pedidos, src_main_resources_templates_factura [EXTRACTED 1.00]
- **F43 registration live validation flow** — src_main_resources_templates_registro_f43_validacion_registro, src_main_resources_templates_registro_comprobardisponibilidad, src_main_resources_templates_registro_sugerencia, src_main_resources_templates_registro_levenshtein, src_main_resources_templates_registro_telefonovalido, src_main_resources_templates_registro_pintarchecklist, src_main_resources_templates_registro_pintarconfirmacion, src_main_resources_templates_registro_campo_trampa_honeypot [EXTRACTED 1.00]
- **Order persistence and snapshot gaps** — flujo_de_trabajo_f33_snapshot_pedido, flujo_de_trabajo_aud11_detalle_pedido, flujo_de_trabajo_f29_factura_oficial, flujo_de_trabajo_f38_recargo_compensar [INFERRED 0.75]
- **Carbon footprint shown across checkout/order views** — src_main_resources_templates_factura_huella_carbono, src_main_resources_templates_pedido_confirmacion_resumen_orden, src_main_resources_templates_mis_pedidos_kpi_huella_compensada, src_main_resources_templates_mis_cotizaciones_impacto_ambiental_chip, src_main_resources_templates_mis_cotizaciones_resumen_kpis [INFERRED 0.85]
- **Blocker-closing security hardening (secrets, rate limits, registration, OAuth)** — flujo_de_trabajo_h01_secretos, flujo_de_trabajo_rate_limiting, flujo_de_trabajo_validacion_registro, flujo_de_trabajo_vinculacion_google, flujo_de_trabajo_variables_obligatorias [INFERRED 0.85]
- **Quote authorization and lifecycle controls** — flujo_de_trabajo_f5_anti_idor, flujo_de_trabajo_f7_f10_permisos_cotizacion, flujo_de_trabajo_estados_cotizacion, flujo_de_trabajo_aud10_pruebas_permisos [INFERRED 0.85]
- **Login authentication flow (form, Google OAuth2, rate-limit, Spring Security/JWT)** — src_main_resources_templates_login_login_form, src_main_resources_templates_login_google_oauth_button, src_main_resources_templates_login_bloqueado_alert, readme_spring_security, readme_jwt_authentication, readme_google_oauth2 [INFERRED 0.85]
- **Customer purchase flow: catalog -> cart -> checkout** — src_main_resources_templates_catalogo_page, src_main_resources_templates_catalogo_agregar_carrito_form, src_main_resources_templates_carrito_page, src_main_resources_templates_carrito_procesar_compra_form, src_main_resources_templates_checkout_page, src_main_resources_templates_checkout_form_checkout [INFERRED 0.85]

## Communities (67 total, 45 thin omitted)

### Community 0 - "org.springframework.ui.Model"
Cohesion: 0.06
Nodes (10): AdminCotizacionesController, PermisosAdvice, VidrioPersonalizadoController, VidrioPersonalizadoWebController, VistaGlobalAttributesAdvice, ResultadoCambio, NO_ENCONTRADA, OK (+2 more)

### Community 2 - "Polygon Cut Editor JS"
Cohesion: 0.09
Nodes (50): abrir(), actualizarBotones(), anadirPunto(), aplicarFormaBase(), aplicarRecomendado(), areaPoligono(), aristaMasCercana(), aTexto() (+42 more)

### Community 3 - "CO2 Model Papers & Glass Detail"
Cohesion: 0.05
Nodes (45): Informe metodologico (Entregable 3: mineria de datos, regresion CO2), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Recycled glass (calcin_pct) reduces emissions (corr -0.45), co2_kg target variable, Synthetic CO2 dataset (1000 records, 20 glass types, data/generacion_dataset.py), Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers, Guardian Glass: How glass is made, Ley 2169 de 2021 (carbono-neutralidad Colombia) (+37 more)

### Community 4 - "Public Navbar & Account Pages"
Cohesion: 0.06
Nodes (42): destacados.html (Featured collection page), Featured editorial gallery grid, detalle.html (Product detail page), Ficha Tecnica (tipo, dimensiones, espesor), Add-to-cart form (/carrito/agregar), Review create/update form (/detalle/{id}/resenas), Product reviews section (promedio + list), error/403.html (Access denied page) (+34 more)

### Community 5 - "Custom Validators"
Cohesion: 0.12
Nodes (10): EmailValido, EmailValidoValidator, FieldMatch, FieldMatchValidator, PasswordValida, PasswordValidaValidator, TelefonoValido, TelefonoValidoValidator (+2 more)

### Community 6 - "Platform Docs & Data Mining"
Cohesion: 0.05
Nodes (40): analisis.ipynb (EDA, training, evaluation), AnalisisService (deterministic CO2 quoter), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Carrito document, CatalogoProducto document, DetallePedido entity, generacion_dataset.py generator, Google OAuth2 login (+32 more)

### Community 8 - "Flujo de trabajo (estado del cierre)"
Cohesion: 0.07
Nodes (32): Piece Anatomy closes with weight (F25) and min 1 layer resistance (F27), AUD-01 Quote input validation, AUD-05 VidrioSostenible compound indexes, AUD-09 Approved work purchasable multiple times, AUD-10 Permission tests missing, AUD-11 Per-line measures and footprint in DetallePedido, AUD security gaps (AUD-02 role normalization, AUD-03 cambiarRol, AUD-04 403 page, AUD-07 logout GET, AUD-08 duplicate JwtAuthenticationFilter), Open Audit Findings AUD-01..AUD-11 (+24 more)

### Community 11 - "Glass Preview Renderer JS"
Cohesion: 0.11
Nodes (23): FLUJO DE TRABAJO · SmartGlass, Hallazgos de la auditoría aún abiertos (no tocados), Hallazgos H (cierre de bloqueantes), Leyenda, Lo que debe verificar el equipo en navegador, Resumen ejecutivo, agujeros(), calcular() (+15 more)

### Community 18 - "Google OAuth2 User Service"
Cohesion: 0.18
Nodes (3): UsuarioRepository, CustomOAuth2UserService, UserService

### Community 20 - "Repository Interfaces"
Cohesion: 0.18
Nodes (3): CarritoRepository, CatalogoProductoRepository, VidrioSostenibleRepository

### Community 22 - "AuthController.java"
Cohesion: 0.12
Nodes (3): AuthController, ErrorResponse, TokenResponse

### Community 28 - "CO2 Dataset Python Pipeline"
Cohesion: 0.13
Nodes (3): cargar(), dividir(), preprocesador()

### Community 31 - "F43 Registration Validation JS"
Cohesion: 0.19
Nodes (14): registro.html (Registration page), comprobarDisponibilidad(), Registration form (POST /registro), intl-tel-input phone picker (default Colombia, E.164), levenshtein(), mostrarSugerencia(), pintar(), pintarChecklist() (+6 more)

### Community 40 - "Main JS (Theme, Compare)"
Cohesion: 0.18
Nodes (6): inicializarComparador(), actualizarBarra(), seleccionados(), inicializarSelectorDeTema(), aplicar(), cambiarTema()

### Community 43 - "Admin Dashboard & Users Pages"
Cohesion: 0.20
Nodes (12): KPI grid (ingresos, co2Total, pendientes, totalPedidos, usuarios, productos), Admin Dashboard page (/admin/dashboard), Latest orders table (stats.pedidos), Pending quotes nav badge (cotizacionesPendientes from PermisosAdvice), Admin navbar fragment (th:fragment navbar), Batch order state form (POST /admin/pedidos/estados), Admin Orders page (/admin/pedidos), Order states (PENDIENTE, PROCESANDO, ENVIADO, ENTREGADO, CANCELADO) (+4 more)

### Community 52 - "Admin Catalog Page"
Cohesion: 0.25
Nodes (8): aplicarFiltros(), Add product modal (POST /admin/productos multipart), Delete product form (POST /admin/productos/{id}/eliminar), Glass types (crudo, templado, laminado, insulado, ... curvo), mostrarFoto() photo upload validation (image, max 5MB), Admin Catalog page (/admin/productos), Product card grid with type chips and search, aplicar() search + role filter

### Community 53 - "Admin Reviews App"
Cohesion: 0.32
Nodes (8): Delete review form (POST /admin/resenas/{id}/eliminar), h() DOM builder, Admin Reviews page (/admin/resenas), ruta(), Client-side reviews app (grouped by product, hash routing #p=ID), sessionStorage restore of product/page after delete (sg-resenas-sel), vistaGeneral(), vistaProducto()

### Community 54 - "Catalog & Cart Pages"
Cohesion: 0.29
Nodes (6): Update quantity form (POST /carrito/actualizar), Shopping Cart page (/carrito), Add to cart form (POST /carrito/agregar, logged-in only), Compare floating bar + chk-comparar checkboxes, Catalog search form (GET /catalogo?q=), Public Catalog page (/catalogo)

### Community 57 - "Admin Quotes Page"
Cohesion: 0.29
Nodes (7): Approve quote form (POST /admin/cotizaciones/{id}/aprobar), CO2 impact metrics (proceso nivel/intensidad kg/m2 + pieza kg CO2e), Quote state filters (TODAS/COTIZACION/APROBADO/RECHAZADA), Admin Quotes approval page (/admin/cotizaciones), Reject quote form (POST /admin/cotizaciones/{id}/rechazar), Quote thumbnail script using SGGlass.render / nombreTipo / fmtAuto, CO2 offset option (huellaCarbono, compensarCarbono +$5.000)

### Community 58 - "Checkout Page"
Cohesion: 0.33
Nodes (6): Batch save script (refrescar, pendientes, beforeunload guard), Finish purchase form (POST /carrito/procesar-compra), Double-submit guard script, Checkout form (POST /checkout/procesar), Payment method selection (TARJETA_CREDITO, PSE), Checkout page

### Community 59 - "Product Comparison Page"
Cohesion: 0.33
Nodes (5): aplicarFiltro() show only differences, Comparison subgrid layout (sticky label column), numero() locale-aware price parser, Product Comparison page (up to 3 products), unicoMejor() best price / best rating chips

### Community 63 - "Flujo de trabajo (estado del cierre) (2)"
Cohesion: 0.67
Nodes (3): H02 CO2 model without masa_kg (MAE 23.25, R2 0.9433), H03 README does not promise .pkl integration, H08 requirements.txt

## Knowledge Gaps
- **79 isolated node(s):** `NO_ENCONTRADA`, `OK`, `TRANSICION_INVALIDA`, `com.smartglass:smartglass`, `Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers` (+74 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 374 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **45 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VidrioSostenible` connect `Sustainable Glass Entity` to `org.springframework.ui.Model`, `Glass Quote Request DTO`, `Contract Math & Polygon Area (2)`, `Mongo Document Imports`, `Glass Quote Response DTO`, `Contract Math & Polygon Area`, `Catalog Product Model`, `Google OAuth2 User Service`, `Repository Interfaces`, `AdminController (2)`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **What connects `NO_ENCONTRADA`, `OK`, `TRANSICION_INVALIDA` to the rest of the system?**
  _79 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.ui.Model` be split into smaller, more focused modules?**
  _Cohesion score 0.05612694681163679 - nodes in this community are weakly interconnected._
- **Why does `Usuario` connect `User Entity` to `org.springframework.ui.Model`, `org.springframework.stereotype.Service`, `ResenaService`, `Admin & OAuth2 Imports`, `Data Seeders`, `Order Entity`, `Product Comparison Model`, `Google OAuth2 User Service`, `Repository Interfaces`, `Registration Request`, `AuthController.java`, `ItemCarrito`, `AuthService`, `org.springframework.ui.Model (2)`, `AdminController (2)`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Should `Glass Quote Request DTO` be split into smaller, more focused modules?**
  _Cohesion score 0.040674603174603176 - nodes in this community are weakly interconnected._
- **Why does `Pedido` connect `Order Entity` to `Mongo Document Imports`, `User Entity`, `Order Line Items`, `Repository Interfaces`, `org.springframework.ui.Model (2)`, `AdminController (2)`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Should `Polygon Cut Editor JS` be split into smaller, more focused modules?**
  _Cohesion score 0.08650937689050212 - nodes in this community are weakly interconnected._