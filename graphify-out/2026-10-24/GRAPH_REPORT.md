# Graph Report - Smartglass  (2026-10-24)

## Corpus Check
- 110 files · ~73,310 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1271 nodes · 2651 edges · 62 communities (22 shown, 40 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 383 edges (avg confidence: 0.85)
- Token cost: 67,712 input · 0 output

## Community Hubs (Navigation)
- Reviews & Store Services
- Polygon Cut Editor JS
- Auth & Registration Controllers
- CO2 Model Papers & Glass Detail
- Sustainable Glass Entity
- Public Navbar & Account Pages
- Custom Validators
- Platform Docs & Data Mining
- Glass Quote Calculation
- Glass Quote Request DTO
- Security Configuration
- Contract Math & Polygon Area
- Catalog Product Model
- Order Line Items
- Quote Approval Workflow
- Global View Attributes
- Web MVC Controllers
- Cart Model
- Glass Preview Renderer JS
- Cart Service & Controller
- User Response & Lookup
- Repository Interfaces
- Registration Request
- User Entity
- Order Entity
- JWT Provider
- Quote Table Row
- CO2 Dataset Python Pipeline
- User Service & Details
- Data Seeders
- Custom Glass Web Controller
- F43 Registration Validation JS
- Login Success Redirect
- Google OAuth2 User Service
- User Repository & View Advice
- Glass Quote Response DTO
- Main JS (Theme, Compare)
- Admin Dashboard & Users Pages
- Order Service & Repository
- Login Rate Limit Filter
- Admin Account Seeder
- Product Comparison Model
- Rate Limit Filter
- Admin Catalog Page
- Admin Reviews App
- Catalog & Cart Pages
- Token Bucket
- Admin Quotes Page
- Checkout Page
- Product Comparison Page
- Application Tests
- Spring Boot Entry Point
- Maven Project

## God Nodes (most connected - your core abstractions)
1. `VidrioSostenible` - 106 edges
2. `Usuario` - 60 edges
3. `CatalogoProducto` - 56 edges
4. `CotizacionVidrioRequest` - 50 edges
5. `Pedido` - 48 edges
6. `DetallePedido` - 37 edges
7. `UsuarioRepository` - 35 edges
8. `CarritoService` - 31 edges
9. `ResultadoAnalisisVidrio` - 29 edges
10. `VidrioSostenibleService` - 29 edges

## Surprising Connections (you probably didn't know these)
- `Opciones de sostenibilidad (usarVidrioReciclado, energiaRenovable, compensarCarbono)` --conceptually_related_to--> `Recycled glass (calcin_pct) reduces emissions (corr -0.45)`  [INFERRED]
  src/main/resources/templates/vidrio-personalizado-form.html → documentos/informe_metodologico.pdf
- `Nivel de impacto ambiental (intensidad kg CO2e/m2: <=12 favorable, 12-20 moderado, >20 alto)` --implements--> `Modulo Ambiental de SmartGlass`  [INFERRED]
  src/main/resources/templates/vidrio-personalizado-detalle.html → documentos/informe_metodologico.pdf
- `gestionarAviso() high-impact alert + recommended config` --implements--> `Automatic recommendation of sustainable materials (e.g. increase calcin %) in the cotizador`  [INFERRED]
  src/main/resources/templates/vidrio-personalizado-form.html → documentos/propuesta_modelo.pdf
- `Termometro ambiental (intensidad kg CO2e/m2, thresholds 12/20)` --implements--> `Sello ambiental en tiempo real en el cotizador`  [INFERRED]
  src/main/resources/templates/vidrio-personalizado-form.html → documentos/propuesta_modelo.pdf
- `SDG cards (9, 11, 12, 13)` --semantically_similar_to--> `UN SDG alignment (ODS 9, 11, 12, 13)`  [INFERRED] [semantically similar]
  src/main/resources/templates/acerca.html → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Admin panel pages sharing admin navbar fragment** — src_main_resources_templates_admin_fragments_nav_navbar, src_main_resources_templates_admin_dashboard_page, src_main_resources_templates_admin_cotizaciones_page, src_main_resources_templates_admin_pedidos_page, src_main_resources_templates_admin_productos_page, src_main_resources_templates_admin_usuarios_page, src_main_resources_templates_admin_resenas_page, src_main_resources_templates_admin_usuario_detalle_page [EXTRACTED 1.00]
- **Atelier live CO2 impact + recommended configuration flow** — src_main_resources_templates_vidrio_personalizado_form_actualizar, src_main_resources_templates_vidrio_personalizado_form_gestionaraviso, src_main_resources_templates_vidrio_personalizado_form_aplicarconfig, src_main_resources_templates_vidrio_personalizado_form_dlgimpacto, src_main_resources_templates_vidrio_personalizado_form_termometro_ambiental, src_main_resources_static_js_glass_preview_sgglass [EXTRACTED 1.00]
- **Balanced-version round trip from obra detail back to atelier** — src_main_resources_templates_vidrio_personalizado_detalle_nivel_impacto_ambiental, src_main_resources_templates_vidrio_personalizado_detalle_version_balanceada_link, src_main_resources_templates_vidrio_personalizado_form_prellenar, src_main_resources_templates_vidrio_personalizado_form_aplicarconfig [EXTRACTED 1.00]
- **CO2 predictive model pipeline (dataset, predictors, RF, metrics)** — documentos_informe_metodologico_dataset_sintetico_co2, documentos_informe_metodologico_variables_predictoras, documentos_informe_metodologico_exclusion_masa_kg, documentos_informe_metodologico_random_forest_regressor, documentos_informe_metodologico_metricas_modelo, documentos_informe_metodologico_co2_kg [EXTRACTED 1.00]
- **Post-purchase flow: confirmation -> orders -> invoice** — src_main_resources_templates_pedido_confirmacion, src_main_resources_templates_mis_pedidos, src_main_resources_templates_factura [EXTRACTED 1.00]
- **F43 registration live validation flow** — src_main_resources_templates_registro_f43_validacion_registro, src_main_resources_templates_registro_comprobardisponibilidad, src_main_resources_templates_registro_sugerencia, src_main_resources_templates_registro_levenshtein, src_main_resources_templates_registro_telefonovalido, src_main_resources_templates_registro_pintarchecklist, src_main_resources_templates_registro_pintarconfirmacion, src_main_resources_templates_registro_campo_trampa_honeypot [EXTRACTED 1.00]
- **Carbon footprint shown across checkout/order views** — src_main_resources_templates_factura_huella_carbono, src_main_resources_templates_pedido_confirmacion_resumen_orden, src_main_resources_templates_mis_pedidos_kpi_huella_compensada, src_main_resources_templates_mis_cotizaciones_impacto_ambiental_chip, src_main_resources_templates_mis_cotizaciones_resumen_kpis [INFERRED 0.85]
- **Customer purchase flow: catalog -> cart -> checkout** — src_main_resources_templates_catalogo_page, src_main_resources_templates_catalogo_agregar_carrito_form, src_main_resources_templates_carrito_page, src_main_resources_templates_carrito_procesar_compra_form, src_main_resources_templates_checkout_page, src_main_resources_templates_checkout_form_checkout [INFERRED 0.85]
- **Login authentication flow (form, Google OAuth2, rate-limit, Spring Security/JWT)** — src_main_resources_templates_login_login_form, src_main_resources_templates_login_google_oauth_button, src_main_resources_templates_login_bloqueado_alert, readme_spring_security, readme_jwt_authentication, readme_google_oauth2 [INFERRED 0.85]
- **CO2 data mining pipeline (generate, preprocess, train, evaluate)** — readme_generacion_dataset, readme_smartglass_co2_dataset, readme_preparacion, readme_analisis_notebook, readme_random_forest_model, readme_metricas_modelo [EXTRACTED 1.00]

## Communities (62 total, 40 thin omitted)

### Community 0 - "Reviews & Store Services"
Cohesion: 0.05
Nodes (7): AdminController, TiendaController, UsuarioDashboardController, Resena, ResenaRepository, ProductoService, ResenaService

### Community 1 - "Polygon Cut Editor JS"
Cohesion: 0.09
Nodes (50): abrir(), actualizarBotones(), anadirPunto(), aplicarFormaBase(), aplicarRecomendado(), areaPoligono(), aristaMasCercana(), aTexto() (+42 more)

### Community 2 - "Auth & Registration Controllers"
Cohesion: 0.05
Nodes (7): AuthController, HomeController, LoginController, RegistroController, ErrorResponse, LoginRequest, TokenResponse

### Community 3 - "CO2 Model Papers & Glass Detail"
Cohesion: 0.05
Nodes (45): Informe metodologico (Entregable 3: mineria de datos, regresion CO2), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Recycled glass (calcin_pct) reduces emissions (corr -0.45), co2_kg target variable, Synthetic CO2 dataset (1000 records, 20 glass types, data/generacion_dataset.py), Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers, Guardian Glass: How glass is made, Ley 2169 de 2021 (carbono-neutralidad Colombia) (+37 more)

### Community 5 - "Public Navbar & Account Pages"
Cohesion: 0.06
Nodes (42): destacados.html (Featured collection page), Featured editorial gallery grid, detalle.html (Product detail page), Ficha Tecnica (tipo, dimensiones, espesor), Add-to-cart form (/carrito/agregar), Review create/update form (/detalle/{id}/resenas), Product reviews section (promedio + list), error/403.html (Access denied page) (+34 more)

### Community 6 - "Custom Validators"
Cohesion: 0.12
Nodes (10): EmailValido, EmailValidoValidator, FieldMatch, FieldMatchValidator, PasswordValida, PasswordValidaValidator, TelefonoValido, TelefonoValidoValidator (+2 more)

### Community 7 - "Platform Docs & Data Mining"
Cohesion: 0.05
Nodes (40): analisis.ipynb (EDA, training, evaluation), AnalisisService (deterministic CO2 quoter), Branca, Colla & Algermissen (2020) Reducing CO2 emissions in energy-intensive industries, Carrito document, CatalogoProducto document, DetallePedido entity, generacion_dataset.py generator, Google OAuth2 login (+32 more)

### Community 14 - "Quote Approval Workflow"
Cohesion: 0.11
Nodes (6): AdminCotizacionesController, ResultadoCambio, NO_ENCONTRADA, OK, TRANSICION_INVALIDA, VidrioSostenibleService

### Community 18 - "Glass Preview Renderer JS"
Cohesion: 0.14
Nodes (17): agujeros(), calcular(), contar(), paso(), texto(), contorno(), describir(), equivalencias() (+9 more)

### Community 30 - "CO2 Dataset Python Pipeline"
Cohesion: 0.13
Nodes (3): cargar(), dividir(), preprocesador()

### Community 32 - "User Service & Details"
Cohesion: 0.23
Nodes (3): UserDetailsServiceImpl, AuthService, UserService

### Community 33 - "Data Seeders"
Cohesion: 0.20
Nodes (3): CatalogoProductoSeeder, ResenaSeeder, CatalogoProductoRepository

### Community 35 - "F43 Registration Validation JS"
Cohesion: 0.19
Nodes (14): registro.html (Registration page), comprobarDisponibilidad(), Registration form (POST /registro), intl-tel-input phone picker (default Colombia, E.164), levenshtein(), mostrarSugerencia(), pintar(), pintarChecklist() (+6 more)

### Community 38 - "User Repository & View Advice"
Cohesion: 0.24
Nodes (3): VidrioPersonalizadoController, VistaGlobalAttributesAdvice, UsuarioRepository

### Community 40 - "Main JS (Theme, Compare)"
Cohesion: 0.18
Nodes (6): inicializarComparador(), actualizarBarra(), seleccionados(), inicializarSelectorDeTema(), aplicar(), cambiarTema()

### Community 42 - "Admin Dashboard & Users Pages"
Cohesion: 0.20
Nodes (12): KPI grid (ingresos, co2Total, pendientes, totalPedidos, usuarios, productos), Admin Dashboard page (/admin/dashboard), Latest orders table (stats.pedidos), Pending quotes nav badge (cotizacionesPendientes from PermisosAdvice), Admin navbar fragment (th:fragment navbar), Batch order state form (POST /admin/pedidos/estados), Admin Orders page (/admin/pedidos), Order states (PENDIENTE, PROCESANDO, ENVIADO, ENTREGADO, CANCELADO) (+4 more)

### Community 49 - "Admin Catalog Page"
Cohesion: 0.25
Nodes (8): aplicarFiltros(), Add product modal (POST /admin/productos multipart), Delete product form (POST /admin/productos/{id}/eliminar), Glass types (crudo, templado, laminado, insulado, ... curvo), mostrarFoto() photo upload validation (image, max 5MB), Admin Catalog page (/admin/productos), Product card grid with type chips and search, aplicar() search + role filter

### Community 50 - "Admin Reviews App"
Cohesion: 0.32
Nodes (8): Delete review form (POST /admin/resenas/{id}/eliminar), h() DOM builder, Admin Reviews page (/admin/resenas), ruta(), Client-side reviews app (grouped by product, hash routing #p=ID), sessionStorage restore of product/page after delete (sg-resenas-sel), vistaGeneral(), vistaProducto()

### Community 51 - "Catalog & Cart Pages"
Cohesion: 0.29
Nodes (6): Update quantity form (POST /carrito/actualizar), Shopping Cart page (/carrito), Add to cart form (POST /carrito/agregar, logged-in only), Compare floating bar + chk-comparar checkboxes, Catalog search form (GET /catalogo?q=), Public Catalog page (/catalogo)

### Community 53 - "Admin Quotes Page"
Cohesion: 0.29
Nodes (7): Approve quote form (POST /admin/cotizaciones/{id}/aprobar), CO2 impact metrics (proceso nivel/intensidad kg/m2 + pieza kg CO2e), Quote state filters (TODAS/COTIZACION/APROBADO/RECHAZADA), Admin Quotes approval page (/admin/cotizaciones), Reject quote form (POST /admin/cotizaciones/{id}/rechazar), Quote thumbnail script using SGGlass.render / nombreTipo / fmtAuto, CO2 offset option (huellaCarbono, compensarCarbono +$5.000)

### Community 55 - "Checkout Page"
Cohesion: 0.33
Nodes (6): Batch save script (refrescar, pendientes, beforeunload guard), Finish purchase form (POST /carrito/procesar-compra), Double-submit guard script, Checkout form (POST /checkout/procesar), Payment method selection (TARJETA_CREDITO, PSE), Checkout page

### Community 56 - "Product Comparison Page"
Cohesion: 0.33
Nodes (5): aplicarFiltro() show only differences, Comparison subgrid layout (sticky label column), numero() locale-aware price parser, Product Comparison page (up to 3 products), unicoMejor() best price / best rating chips

## Knowledge Gaps
- **67 isolated node(s):** `com.smartglass:smartglass`, `NO_ENCONTRADA`, `OK`, `TRANSICION_INVALIDA`, `Feature importance: mass and glass type dominate; fotovoltaico/inteligente high multipliers` (+62 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 356 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VidrioSostenible` connect `Sustainable Glass Entity` to `Custom Glass Web Controller`, `User Repository & View Advice`, `Glass Quote Response DTO`, `Glass Quote Calculation`, `Contract Math & Polygon Area`, `Catalog Product Model`, `Quote Approval Workflow`, `Web MVC Controllers`, `Mongo Document Imports`, `Repository Interfaces`, `Servlet Filter Imports`, `Quote Row Mapping`, `Admin & OAuth2 Imports`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **What connects `com.smartglass:smartglass`, `NO_ENCONTRADA`, `OK` to the rest of the system?**
  _67 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Reviews & Store Services` be split into smaller, more focused modules?**
  _Cohesion score 0.05267778753292362 - nodes in this community are weakly interconnected._
- **Why does `Usuario` connect `User Entity` to `Reviews & Store Services`, `User Service & Details`, `Auth & Registration Controllers`, `Custom Glass Web Controller`, `Google OAuth2 User Service`, `User Repository & View Advice`, `Order Creation from Cart`, `Order Service & Repository`, `Product Comparison Model`, `Web MVC Controllers`, `Global View Attributes`, `Mongo Document Imports`, `User Response & Lookup`, `Repository Interfaces`, `Registration Request`, `Order Entity`, `Admin & OAuth2 Imports`?**
  _High betweenness centrality (0.089) - this node is a cross-community bridge._
- **Should `Polygon Cut Editor JS` be split into smaller, more focused modules?**
  _Cohesion score 0.08650937689050212 - nodes in this community are weakly interconnected._
- **Why does `CotizacionVidrioRequest` connect `Glass Quote Request DTO` to `Custom Glass Web Controller`, `Sustainable Glass Entity`, `User Repository & View Advice`, `Contract Math & Polygon Area`, `Global View Attributes`, `Web MVC Controllers`, `Repository Interfaces`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Should `Auth & Registration Controllers` be split into smaller, more focused modules?**
  _Cohesion score 0.053109713487071976 - nodes in this community are weakly interconnected._