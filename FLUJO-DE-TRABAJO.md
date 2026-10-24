# FLUJO DE TRABAJO · SmartGlass

Estado de cada ítem **leído contra el código del repositorio** (rama `cierre-bloqueantes`, 2026-10-24). Sustituye a cualquier versión anterior de este documento.

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
- **Comportamiento:** todo 🧪 hasta verlo en navegador. Ningún ítem está ✅ todavía.
- **Contrato de cálculo:** `AnalisisService` y las funciones del contrato de `glass-preview.js` (`calcular`, `impacto`, `recomendar`, `UMBRAL`, `fmtNum`, `fmtAuto`) no se modificaron en esta tanda.

## Ítems F (funcionalidad)

| Ítem | Estado | Evidencia en el código |
|---|---|---|
| F1–F4, F6 | NO VISTO | Sin definición en el repositorio |
| F5 · Anti-IDOR en la API de cotizaciones | 🧪 ESCRITO | `VidrioPersonalizadoController.java:67,80-85` |
| F7 · Eliminar: dueño solo COTIZACION/RECHAZADA, admin cualquiera | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:137-138` |
| F8 · Detalle solo dueño o admin | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:77-90` |
| F9 · Sin `@ModelAttribute("usuario")` en rutas con `{id}` | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:49-52` |
| F10 · Aprobar: solo admin y solo desde COTIZACION | 🧪 ESCRITO | `VidrioPersonalizadoWebController.java:107-127` |
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
| F24 · Semáforo solo por intensidad; absoluto neutro | 🧪 ESCRITO | `VidrioSostenible.java:99-115`; `AdminCotizacionesController.java:158-159`; `vidrio-personalizado-detalle.html:482-485` |
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
| F45 · Corte personalizado | DIFERIDO (escrito, sin visto bueno) | Bifurcación aditiva en `AnalisisService.java:145-172`; front en `corte-editor.js` y `glass-preview.js:353-361`. Requiere visto bueno formal por tocar el contrato |

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
| H09 · Este documento | 🧪 ESCRITO | Sin commitear; lo decide el equipo |
| H10 · Archivo basura `how --stat ce8af77` | 🧪 ESCRITO | Borrado del índice (`git rm`) |
| H11 · Límite en `POST /auth/registro` | 🧪 ESCRITO | `RateLimitFilter.java:65-66,97`; la petición 11 por minuto devuelve 429 |

## Hallazgos de la auditoría aún abiertos (no tocados)

| Ítem | Qué falta | Dónde |
|---|---|---|
| AUD-01 | Validación de entrada de la cotización (medidas ≤ 0 o negativas, perforaciones negativas) | `CotizacionVidrioRequest.java` |
| AUD-02 | El rol de un usuario de Google se toma sin normalizar | `CustomOAuth2UserService.java:113-114` |
| AUD-03 | `cambiarRol` acepta cualquier texto | `AdminController.java:85-88` |
| AUD-04 | `accessDeniedPage("/error/403")` sin controlador | `SecurityConfig.java` |
| AUD-05 | Índices compuestos de `VidrioSostenible` | `VidrioSostenible.java:20` |
| AUD-06 | Mensajes de error, SQL y Security en modo detallado | `application.properties` |
| AUD-07 | Logout por GET y cookie con otro nombre | `SecurityConfig.java` |
| AUD-08 | `JwtAuthenticationFilter` registrado dos veces | `JwtAuthenticationFilter.java:27` |
| AUD-09 | Una obra aprobada se puede comprar varias veces | `CarritoService.java:74-85` |
| AUD-10 | Pruebas de permisos (solo existe `contextLoads`) | `src/test` |
| AUD-11 | Medidas y huella por línea en `DetallePedido` | `DetallePedido.java:59-66` |

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

Nada se da por cerrado hasta que los greps den el resultado esperado, la app arranque con las variables documentadas sin secretos en claro ni 404 del tema, y los casos A–D se vean en navegador.
