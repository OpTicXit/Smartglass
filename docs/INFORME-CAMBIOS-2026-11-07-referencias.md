# Informe de cambios · 7 de noviembre de 2026 (noche)

Trabajo hecho de corrido con las tres referencias de TikTok y las respuestas que diste antes de salir. Todo quedó en un **commit local en `main`, sin push**.

## 1. Cambio de modo claro / oscuro (video "Glassy Liquid Footer")

- **El efecto:** el tema nuevo crece desde el interruptor con el **borde difuso**, como vidrio líquido, en lugar del círculo nítido de antes.
- **Desenfoque:** la página nueva entra con un leve desenfoque que se aclara en 0,9 s, y la vieja se desenfoca apenas.
- **Alcance:** funciona en todo el sitio, en la tienda y en el admin.
- **Movimiento reducido y fotosensible:** el cambio es directo, sin efecto.
- **Archivos:** `css/theme.css` (máscara con `@property --sg-r`) y `js/main.js` (`cambiarTema`).

## 2. Login y registro en una sola tarjeta (video "Login Page V7")

- **Una sola página:** login y registro viven en la misma tarjeta (`acceso.html`). "Crea una" e "Inicia sesión" cambian de vista **sin recargar**.
- **La transición (~1,1 s):**
  1. El formulario se desvanece con desenfoque.
  2. Dos mitades del isotipo de SmartGlass llegan desde esquinas opuestas y se unen en el centro.
  3. Una luz cian sube y destella.
  4. La tarjeta toma el alto nuevo y aparece el otro formulario.
- **Direcciones y navegación:** `/login` y `/registro` siguen abriendo cada uno su vista. La dirección y el título cambian con la vista, y Atrás/Adelante del navegador funcionan.
- **Sin JavaScript:** los enlaces siguen llevando a cada página.
- **Accesibilidad:** el foco pasa al título del formulario nuevo. Con movimiento reducido o fotosensible el cambio es directo.
- **El formulario de registro no cambió:** mismas validaciones, mismo teléfono con país, misma disponibilidad en vivo y mismo campo trampa. Solo se renombraron los `id` de usuario y contraseña del registro para que no choquen con los del login.
- **Archivos:**
  - **Nuevos:** `templates/acceso.html`, `css/acceso.css` y `js/acceso.js`.
  - **Borrados:** `login.html` y `registro.html`.
  - **Ajustados:** `LoginController` y `RegistroController` ahora devuelven `acceso` con la vista correspondiente.

## 3. Admin con el estilo del video "Dashboard V22"

- **Estilo:** fondo claro con bruma pastel (cielo, cian y durazno) y panel de vidrio blanco esmerilado con tarjetas blancas muy redondeadas. Tiene **versión oscura** con el mismo orden.
- **Paleta:** la de SmartGlass en pastel, con el Azul Profundo como acento oscuro y el durazno como acento cálido de las gráficas.
- **Menú:**
  - Es una **cápsula Azul Profundo con íconos y nombres** (como pediste), con cada ícono en un círculo.
  - La sección activa es una píldora clara.
  - La tarjeta del plan es pastel, con barra de progreso a rayas.
- **Barra superior:** píldora de vidrio blanco.
- **Detalles del video:**
  - íconos de las cifras en círculos pastel;
  - tarjeta destacada oscura (la del Informe técnico en el Resumen);
  - avatares pastel;
  - **cifras que ruedan como contador**.
- **Entrada al admin:** **una vez por sesión, al entrar después de iniciar sesión**.
  1. Pantalla Azul Profundo donde el isotipo se arma y aparece "SmartGlass Admin".
  2. Un círculo que crece desde la esquina revela el panel.
  3. Las tarjetas entran en cascada.
  - Al volver a iniciar sesión, la entrada se muestra otra vez.
  - Si algo fallara, a los 3–4 s se quita sola.
- **Cambio de sección:** el menú queda quieto. El contenido se **esmerila y se borra**, y el nuevo entra con las **tarjetas primero vacías y luego llenas**, una tras otra.
- **Movimiento reducido y fotosensible:** no hay entrada, cascada ni números rodando.
- **Archivos:** `css/admin.css` (sección "ARMAZON DEL PANEL"), `js/admin-shell.js` y `admin/fragments/nav.html`.

## 4. Entrada al configurador ("personalizar un vidrio")

- **Desde dónde:** cualquier botón que lleve a `/vidrio-personalizado/cotizar` ("Diseñar a medida", "Vidrio personalizado", "Diseñar la mía", etc.).
- **La transición:** un **círculo crece desde el botón que tocaste** y revela el configurador. Luego sus bloques entran **en cascada**.
- **Diseño:** el configurador no cambió de diseño, como pediste.
- **Navegadores:** el círculo usa las transiciones entre páginas del navegador (Chrome y Edge). En otros solo se ve la cascada.
- **Archivos:** `js/main.js` (guarda el punto del clic), `vidrio-personalizado-form.html` (círculo y cascada) y `css/theme.css`.

## 5. Lo que estaba pendiente y también quedó en el commit

- **Cotizaciones del admin en tarjetas:** con Ver / Rechazar / Aprobar juntos en la barra del pie. Ya toma el estilo nuevo del admin.
- **Galería del detalle:** se puede deslizar con el dedo en el teléfono, y las vistas extra de los vidrios planos se enlazan cuando existan sus fotos.

## 6. Pruebas

- **Backend:** `mvnw test` pasa las **44 pruebas**.
- **Navegador**, en la copia local con datos de demostración:
  - **Login ↔ registro:**
    - la transición corre completa;
    - la dirección y el título cambian;
    - Atrás vuelve al login;
    - `/registro` directo abre el registro;
    - un **registro real** de un usuario de prueba termina en el login con el aviso de cuenta creada;
    - en inglés los textos salen traducidos.
  - **Tema:** el borde difuso avanza desde el interruptor.
  - **Admin:**
    - la entrada se ve una vez y se marca en la sesión;
    - el cambio de sección esmerila y hace la cascada;
    - las 9 páginas se ven en claro y oscuro sin desbordes a 1440 px;
    - el cajón del menú funciona en el teléfono.
  - **Configurador:** el navegador hizo la transición y el círculo creció desde "Diseñar a medida". Los 8 bloques entraron en cascada.

## 7. Pendiente o sin verificar

- **Railway:** no se probó ni se hizo push, como pediste.
- **Teléfono real:** no lo probé; usé la emulación del navegador.
- **Aviso en la consola:** al salir de algunas páginas del admin, el navegador escribe un aviso de que omitió la transición automática entre páginas ("ViewTransition opt-in disabled"). La navegación funciona igual; no encontré la causa.
- **Sin commit:** quedaron fuera la carpeta `docs/preview/` (las previews de Platzi, reloj y línea) y la entrada `preview-estilo` de `.claude/launch.json`.
- **Base de prueba:** el registro de prueba creó un usuario `pruebaNNNNNN` en la base local `smartglass_vidrio_prueba`.
