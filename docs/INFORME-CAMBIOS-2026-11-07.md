# Informe de cambios · 7 de noviembre de 2026

Trabajo hecho de corrido, con las preguntas resueltas al inicio. Todo quedó en un commit local en `main`, **sin push**, para probarlo primero en local.

## 1. Proyecciones del CO₂ estimado (informe técnico del admin)

En **Admin → Informe técnico** hay dos proyecciones nuevas del **CO₂ total estimado**. Cada una tiene botones de horizonte (1 semana, 1 mes, 5 meses, 1 año, 5 años), una gráfica con la historia, la proyección y su rango probable, y una frase del tipo "En el próximo año se estiman X kg de CO₂e (entre Y y Z)".

| Apartado | De dónde salen los datos | Hasta dónde proyecta |
|---|---|---|
| **Actividad de la plataforma** | Cotizaciones guardadas en SmartGlass más 2 años de historial de demostración (con `DEMO_DATOS=true`). Se actualiza cada 30 s | Hasta 1 año (el historial tiene 2 años) |
| **Simulación** | Historia sintética de 5 años generada a partir del dataset de 10.000 piezas | Hasta 5 años |

- **Método:** mínimos cuadrados con tendencia y estacionalidad semanal y anual. Es un modelo nuevo y aparte: el Random Forest, su dataset y sus métricas no se tocaron.
- **Misma fórmula en Python y en Java:** una prueba compara los números de las dos versiones y dan lo mismo.
- **Validación de la Simulación** con los últimos 6 meses: error medio de 591,75 kg por día y de 14,1 % por semana. Repetir el año anterior falla por 690,53 kg por día, así que el modelo es mejor que la referencia ingenua.
- **Nunca proyecta** más lejos que el historial disponible ni da valores negativos. Sin historial, muestra "Todavía no hay datos suficientes para proyectar".
- **Historial de demostración:** va en una colección aparte (`historial_actividad`). No aparece en listas, contadores ni en el dashboard, y la pantalla no lo distingue. Solo el README lo explica. Para dejar únicamente datos reales, se borra esa colección.

**Archivos:**
- **Backend:** `ProyeccionCo2.java`, `ProyeccionPlataformaService.java`, `HistorialActividad.java`, su repositorio y `HistorialActividadSeeder.java`, más dos rutas nuevas en `AdminInformeController`.
- **Python:** `data/generacion_historial.py`, `mineria de datos/proyeccion_co2.py`, `exportar_proyeccion.py` y `test_proyeccion.py`.
- **Datos exportados:** `informe/proyeccion-simulacion.json` y `data/raw/smartglass_historial.csv`.
- **Documentos de diseño y plan:** `docs/superpowers/specs/2026-11-07-proyecciones-co2-design.md` y `docs/superpowers/plans/2026-11-07-proyecciones-co2.md`.

## 2. Panel de administración con el estilo del video

Las **9 páginas del admin** tienen ahora el aspecto del dashboard del video:
- **Fondo y vidrio:** degradado azul petróleo, con menú lateral y barra superior de vidrio, y un panel esmerilado con tarjetas blancas translúcidas.
- **Menú por grupos:** Panel, Comercio, Personas y Sistema, con insignia de cotizaciones por revisar y, abajo, el plan activo con sus funciones.
- **Barra superior:** buscador del panel (**Ctrl+K**, flechas y Enter para saltar a una sección), notificaciones, idioma, accesibilidad y la cuenta con avatar.
- **Animaciones:** al cambiar de sección el menú queda quieto y el contenido entra con un desenfoque breve. Los bloques aparecen escalonados y los números cuentan hacia arriba.
- **Resumen (nuevo dashboard):**
  - **Encabezado:** saludo según la hora y botones a Informe técnico y Nuevo producto.
  - **Gráficas:** ventas por mes con pestañas de 3, 6 y 12 meses y dona de cotizaciones por estado.
  - **8 cifras clave:** ingresos con variación frente al mes anterior, CO₂ estimado, pedidos por atender, cotizaciones por revisar, pedidos, ticket promedio, usuarios y catálogo.
  - **Abajo:** los últimos pedidos y un acceso al informe.
- **Finanzas (página nueva, `/admin/finanzas`):**
  - **Cifras:** ingresos, por cobrar, entregado y ticket promedio.
  - **Gráficas:** ingresos por mes y cómo pagan los clientes (tarjeta, PSE y cuántos compensaron su CO₂).
  - **Tabla:** pedidos recientes, con filtros por estado y **Exportar CSV**, que se genera en el navegador.
- **Datos reales:** todo sale de lo guardado. Los pedidos cancelados no suman ingresos.
- **Teléfono:** el menú se abre como cajón y el buscador baja a su propia fila.
- **Modos:** claro (el del video), oscuro con el mismo vidrio más profundo, daltonismo, sin animaciones y movimiento reducido.

**Archivos:**
- **Plantillas:** `admin/fragments/nav.html`, `admin/dashboard.html` y `admin/finanzas.html`.
- **Estilos y comportamiento:** `css/admin.css` y `js/admin-shell.js`.
- **Backend:** `FinanzasService.java`, más ajustes en `AdminController` y `VistaGlobalAttributesAdvice`.
- **Íconos:** se regeneró el subconjunto de Phosphor para incluir los nuevos.

## 3. Inicio: "Así nace una pieza SmartGlass" (scroll world sin costo)

Es una sección nueva en la página de inicio:
- **El recorrido:** al bajar, una cámara gira alrededor de la pieza de vidrio 3D del configurador y la pieza cambia en 5 capítulos: materia reciclada, templado, forma propia, fotovoltaico y su huella.
- **El final:** el último capítulo muestra el CO₂ estimado de la pieza y el botón "Diseñar la mía".
- **Sin videos generados:** no usa Higgsfield ni Monid, así que no tuvo costo.
- **Movimiento reducido o fotosensible:** la pieza no gira, pero los capítulos se leen igual.

**Archivos:** `css/recorrido.css`, `js/recorrido.js` e `index.html`.

## 4. Revisión de las demás páginas

Recorrí en ancho de teléfono inicio, catálogo, detalle, comparar, destacados, acerca, planes, guía, cotizar, carrito, mi cuenta, mis pedidos, mis cotizaciones y la página de error:
- **Desbordes:** ninguna página se sale horizontalmente.
- **Textos y accesibilidad:** no falta ningún texto traducido, ninguna imagen sin `alt` y ningún botón visible sin nombre.
- **Cambios:** como ya estaban bien, no cambié su diseño, para respetar "pulir sin cambiar identidad".

## 5. Pruebas

- **Backend:** `mvnw test` pasa las **44 pruebas**. Hay 13 nuevas: proyección, historial, servicio de proyección y finanzas.
- **Python:** `python "mineria de datos/test_proyeccion.py"` imprime `ok`.
- **Navegador**, en la copia local con datos de demostración, a 1440 px y 375 px:
  - **Resumen:** gráficas y cifras.
  - **Finanzas:** gráficas y tabla.
  - **Buscador:** Ctrl+K lleva a Finanzas.
  - **Teléfono:** el cajón del menú abre bien.
  - **Catálogo:** el modal de nuevo producto se ve completo.
  - **Informe:** las dos proyecciones se dibujan.
  - **Inicio:** el recorrido gira la pieza y cambia de capítulo.

## 6. Cómo probarlo en local

1. **Activa los datos de demostración:** en tu `.env`, pon `DEMO_DATOS=true` y un `DEMO_PASSWORD`. El historial de 2 años se crea **una sola vez** al arrancar, aunque tu base ya tenga los datos de demostración.
2. **Arranca la aplicación** como siempre y entra como admin.
3. **Revisa estas rutas:** `/admin/dashboard`, `/admin/finanzas` y `/admin/informe-tecnico`, en las secciones "Actividad de la plataforma" y "Simulación". Prueba Ctrl+K en cualquier página del admin.
4. **Revisa el inicio:** abre `/home` y baja hasta "Así nace una pieza SmartGlass".

## 7. Decisiones que tomé yo

- **Menú:** solo con secciones que funcionan. "Ver la tienda" está en Sistema.
- **Nombres:** marca SmartGlass y el nombre del admin que inició sesión, en lugar de "Code XR" y "Andriy".
- **Las dos donas:** la del Resumen muestra cotizaciones por estado; la de Finanzas, el método de pago.
- **Finanzas no cambia el estado de los pedidos:** eso sigue en Pedidos, para no duplicar acciones.
- **Archivo aparte:** la proyección de Simulación va en su propio JSON, para no volver a correr el script del Random Forest.
- **Barra superior más opaca:** el vidrio no puede desenfocar el contenido que pasa por debajo, así que el tinte es más denso para que siempre se lea.

## 8. Pendiente o sin verificar

- **Railway:** no se probó, como pediste. Para que "Actividad de la plataforma" tenga historial allá hace falta `DEMO_DATOS=true` en sus variables.
- **Teléfono real:** no lo probé; usé la emulación del navegador.
- **Bases de prueba locales:** quedaron `smartglass_vidrio_prueba` y `smartglass_push_prueba` en tu Mongo y tu MySQL locales, de las pruebas. Se pueden borrar cuando quieras.
- **Historial de demostración:** la pantalla no lo distingue. Si un jurado pregunta, la explicación está en el README.
