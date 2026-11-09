# SmartGlass: estado del proyecto al 9 de noviembre de 2026

Documento de traspaso para que otro agente se ponga al día y proponga en qué seguir trabajando.
Describe el proyecto completo, lo que cambió en los últimos días, lo que está sin commit, las reglas que no se pueden romper y los pendientes.

---

## 1. Qué es SmartGlass

Es una plataforma web para vender vidrio a medida y de catálogo que muestra la **huella de carbono estimada** de cada pieza antes de comprarla. El cliente diseña su pieza, ve el precio y el CO₂ al instante y envía la cotización. Del otro lado, la vidriería la aprueba y gestiona pedidos, finanzas y un informe técnico con proyecciones.

- **Contexto:** proyecto académico universitario de Cartagena (Colombia). Lo van a presentar en una feria con un pitch de venta.
- **Fase:** prototipo funcional, listo para un piloto. **Ninguna vidriería real lo usa todavía**, y no se debe decir que sí.
- **CO₂:** siempre se **estima o calcula** con factores de emisión. Nunca se dice que se "mide".

## 2. Tecnología y cómo se ejecuta

| Capa | Detalle |
|---|---|
| Backend | Spring Boot 4.1.1 con Java 26 (en la máquina está el JDK 27 en `C:\Program Files\Java\jdk-27`) |
| Vistas | Thymeleaf; las plantillas están en `frontend/templates` y los archivos estáticos en `frontend/static` |
| Datos | MySQL (JPA: usuarios, pedidos, facturas) y MongoDB (cotizaciones de vidrio, catálogo, reseñas, historial de actividad) |
| Minería de datos | Python con un Random Forest de 200 árboles y un dataset **sintético** de 10.000 piezas. El informe del panel lee `backend/src/main/resources/informe/informe-tecnico.json`, que genera `mineria de datos/exportar_informe.py` |
| Proyecciones | Mínimos cuadrados con tendencia y ciclos de Fourier (semanal y anual). La fórmula es la misma en `ProyeccionCo2.java` y en `proyeccion_co2.py` |
| Íconos | Subconjunto de Phosphor que genera `scripts/iconos_phosphor.py` revisando las plantillas. La prueba `IconosPhosphorTest` falla si falta un ícono |
| Idiomas | `messages.properties` (español) y `messages_en.properties` (inglés). `es_CO` y `es_ES` solo cambian algunas claves |
| Despliegue | Railway, desde GitHub (rama `main`). Para ver el historial de demostración en Railway hace falta `DEMO_DATOS=true` |

**Ejecución local:** las variables van en `.env`; la plantilla es `.env.example`.
- Arrancar la app: `cd backend` y luego `.\mvnw.cmd spring-boot:run`.
- Pruebas: `.\mvnw.cmd test`. Hoy pasan **45 de 45**.
- Si solo cambian plantillas o archivos estáticos con la app corriendo: `.\mvnw.cmd -q resources:resources`.
- Si cambia Java: `.\mvnw.cmd -q compile`; devtools reinicia la app.

**Ojo con Thymeleaf:** dentro de un `<script>` de una plantilla no se puede escribir `[[` ni `]]` pegados, porque Thymeleaf los interpreta como expresión y corta la página a mitad. Hay que separarlos: `[ ['a', 1] ]`.

## 3. Estructura del repositorio

```
backend/            Spring Boot (controladores, servicios, seeders, pruebas, messages*.properties, informe/*.json)
frontend/templates  Vistas Thymeleaf (44 plantillas; admin/ y fragments/ incluidos)
frontend/static     css/, js/, img/, vendor/ (three.js, topojson, phosphor, mapas)
mineria de datos/   Notebook, exportadores del informe y de la proyección
data/               Generación del dataset sintético
scripts/            Utilidades (subconjunto de íconos, etc.)
docs/               Informes de cambios y vistas previas (docs/preview no está en el repo)
documentos/         PDF del proyecto (varios son privados; ver §7)
video promocional/  Proyecto del video (Remotion)
```

## 4. Qué hace hoy, por áreas

### Tienda y cliente
- Catálogo, destacados, comparador, carrito, checkout (tarjeta o PSE de prueba), facturas y seguimiento de pedidos.
- Reseñas y notificaciones.
- Recuperación de contraseña con código enviado por Gmail. El usuario es el que crea ese código.
- Login y registro en una sola tarjeta que cambia con una transición, y una entrada animada después de iniciar sesión.
- Guía automática del recorrido para quien entra por primera vez. Es suave y rápida, termina en el login y nunca muestra nada del admin.
- Modos de accesibilidad: fotosensible, daltónico (el atributo se llama `daltotonico`) y movimiento reducido. Español e inglés. Tema claro y oscuro con una transición de máscara.

### Inicio (`index.html`)
- **Hero:** tres **paneles de vidrio** en perspectiva con la foto de una montaña y tres etiquetas flotantes. Hoy entran con una animación y pasan un solo resplandor; después quedan quietos. **El usuario pidió darles más vida** (ver §8).
- **"Diseña tu pieza y mírala tomar forma":** se eligen vidrio, forma (incluido "Tu propio corte") y medidas, y se ven al instante el área, el CO₂ y la intensidad. El botón "Seguir con esta pieza" la lleva tal cual al configurador por la URL.
- **"Así nace una pieza SmartGlass":** una línea de producción con 5 estaciones que avanzan solas cada 4,2 s mientras la sección está a la vista. Tiene pausa y no depende del scroll.
- **Pie de página** con mucha información en poco espacio: insignias, cuatro columnas, la franja de los ODS y la nota de que el CO₂ es una estimación. No tiene datos de contacto inventados.

### Configurador de vidrio a medida (`vidrio-personalizado-form.html`)
- **Asistente de 5 pasos** con barra arriba: Uso y vidrio → Medidas y forma → Acabados → Huella → Entrega y resumen. Tiene Anterior/Siguiente y valida antes de avanzar. Los nombres de los campos del formulario no cambiaron.
- **Siempre a la vista:** la pieza (2D/3D), el precio y el CO₂. En el celular hay una barra fija abajo con una miniatura.
- **Impacto alto:** aparece un aviso corto, y la **configuración recomendada se abre en una ventana centrada** que compara "Tu diseño" con "Recomendado" (intensidad, CO₂, precio y % de cambio) con los botones Aplicar / Mantener. La misma ventana sirve de confirmación ("Antes de continuar") al enviar.
- **Editor de corte** (`corte-editor.js` + `corte-3d.js`): herramientas en una barra sobre el lienzo, formas base compactas, guía de 3 pasos, ayuda plegada, vista 3D y pintura del diseño.
- **Compromisos sin costo y sello "Compromiso verde":** no cambian ni el CO₂ ni el precio.

### Acerca de (`acerca.html`)
Tiene estas secciones: estado del proyecto, cómo calculamos el CO₂, cifras, ODS, 7 preguntas frecuentes y equipo.

### Panel de administración (`/admin`)
- **Diseño:** estilo pastel, con entrada animada y transiciones entre secciones.
- **Páginas:** Resumen, Informe técnico, Finanzas, Cotizaciones (en tarjetas), Pedidos, Catálogo, Usuarios, Reseñas y Planes. Tiene buscador con Ctrl K y campana de notificaciones.
- **Informe técnico, tal como quedó el 9 de noviembre (sin commit):**
  - Abre con el globo 3D de emisiones por país.
  - Sigue el bloque **"Cómo leer este informe"**.
  - Cada bloque lleva una **etiqueta de origen**: Plataforma / Estimado / Sintético / Modelo / Contexto externo.
  - Los **13 bloques** tienen un **"¿Qué estoy viendo?"** con qué muestra, de dónde sale y cómo leerlo.
  - **Colores:** azul = plataforma y predicción; ámbar = valor con el que se compara y proyección. Ningún color es una alerta.
  - "Real" pasó a "Calculado", los tipos con pocas piezas llevan "(n=…)" y hay una nota sobre la masa en la matriz de correlación.
  - Las dos proyecciones tienen títulos distintos y el porcentaje de reciclado muestra su base.
  - La prueba `InformeTecnicoTest` verifica que todas las claves existan en los dos idiomas.
- **Finanzas y Resumen:**
  - La variación compara **el último mes cerrado con el anterior** (chip "oct · +23,2 %"), así que el mes en curso ya no da un −100 % falso.
  - Las barras de ingresos van en azul, PSE en gris y la dona tiene leyenda.

### Modelo predictivo (cifras vigentes, corrida de 10.000 registros del 02/11/2026)
MAE 13,52 kg · RMSE 25,56 kg · R² 0,9613 · R² CV 0,9615 ± 0,0036. Los datos son sintéticos. El modelo **no se usa todavía en el cotizador**, que calcula el CO₂ con sus propias fórmulas. **No cambiar el modelo.**

## 5. Historial reciente y estado de git

- `main` local va **5 commits por delante de `origin/main`**: `888a56e`, `7e829ad`, `95ec840`, `a69b47f` y `179fcea`. **No se ha hecho push**, así que Railway no tiene nada de esto.
- **Sin commit todavía (8 y 9 de noviembre):**
  - Asistente del configurador con la ventana de la recomendación.
  - Inicio con "libertad de personalización".
  - Recorrido automático.
  - Pie de página y Acerca de.
  - Editor de corte reorganizado.
  - Informe técnico con orígenes y explicaciones.
  - Finanzas corregidas.
  - README: solo cambió una palabra ("calculados").
  - Archivo nuevo: `frontend/templates/admin/fragments/informe.html`.
- **No subir nunca:**
  - `documentos/TERCERA_ENTREGA_PA_SmartGlass.pdf`
  - `documentos/Guion_Video_YouTube_SmartGlass.pdf`
  - `documentos/SmartGlass_Guion_Venta.pdf`
  - `docs/preview/`
  - `.env` y `.vscode/launch.json`

## 6. Materiales del pitch

- **Guion de venta:** es un discurso continuo, **sin enumeraciones ni nombres de integrantes**, porque todos se lo aprenden entero. Usa el modelo Canvas y los criterios de clasificación de emprendimientos (propósito, ámbito, tecnología, innovación, crecimiento, motivación, organización). El paso en pantalla va entre corchetes para guiar el video. Está en el Claude Doc del proyecto y en `documentos/SmartGlass_Guion_Venta.pdf`.
- **Si cambia la interfaz** que nombra un corchete (asistente, ventana de la recomendación, editor de corte), hay que ajustar el guion.

## 7. Reglas que no se pueden romper

1. **Push y Railway:** no hacer push sin que el usuario lo pida. Las pruebas en Railway las hace el usuario después del push.
2. **Modelo:** no cambiar el Random Forest.
3. **Secretos:** no mostrar ni repetir secretos (claves de Google, JWT, MySQL, admin). Las contraseñas de prueba solo van en archivos locales de práctica.
4. **CO₂:** se "estima" o "calcula", nunca se "mide". El valor absoluto de CO₂ no lleva color de juicio; el semáforo es solo para la intensidad.
5. **Lo que no se dice:** no se dan precios de los planes en la feria, y no se dice que una vidriería ya usa SmartGlass.
6. **Datos de demostración:** el historial de demostración inyectado **no se nota en pantalla**: nada se llama "datos reales" y la aclaración va solo en el README.
   - **Pendiente de decidir:** la introducción de "Actividad de la plataforma" dice "incluidos los de demostración". Ese texto es anterior y el usuario no ha dicho si lo quita.
7. **Guía automática:** no muestra nada del admin.
8. **Archivos privados:** no subir los PDF privados ni poner el guion de YouTube en el README ni en el repositorio.
9. **Forma de trabajar:**
   - Antes de cambios grandes se hacen las preguntas necesarias.
   - Las referencias que manda el usuario (por ejemplo, videos de TikTok) se reciben todas antes de empezar.
   - Los commits son locales, salvo que el usuario pida otra cosa.

## 8. Pendientes y en qué se puede trabajar

### Pedidos por el usuario el 9 de noviembre, aún sin empezar
1. **README como "cara" del repositorio en GitHub:**
   - Hoy no tiene **ninguna imagen**. Hay que agregar capturas reales del proyecto (inicio, configurador por pasos, ventana de la recomendación, editor de corte, panel e informe técnico, en claro y oscuro) guardadas en el repositorio, por ejemplo en `docs/img/`.
   - Hay que poner el contenido al día con lo de §4.
   - Las capturas deben usar datos de prueba, sin secretos ni datos personales.
2. **Más animación en los tres paneles de vidrio del hero del inicio** (`index.html`, clases `.panel-vidrio.p1/.p2/.p3`, la montaña y las tres `.badge-flotante`):
   - Hoy hacen una entrada y un solo resplandor (`::after`), y después quedan quietos.
   - Ideas: un brillo que pase cada cierto tiempo, una flotación suave desfasada entre paneles, parallax al mover el puntero, reflejos que respondan a la inclinación y etiquetas que respiren.
   - Hay que respetar el movimiento reducido y el modo fotosensible: en esos modos ya está `animation: none !important`. Nada de parpadeos de más de 3 Hz, y hay que pausar con la pestaña oculta.

### Otros pendientes
3. **Commit local** de todo lo de §5 cuando el usuario lo apruebe; **push** solo si lo pide.
4. **Railway:** revisar con `DEMO_DATOS=true` después del push. Lo hace el usuario.
5. **Lo que queda de la guía de visualización** (para después de la feria):
   - Exportar el informe a PDF o CSV.
   - Submenú fijo en el informe.
   - En Pedidos: barra "Guardar cambios (N)" en vez de un botón por fila, leyenda de las facturas `TMP-` y `SG-`, paginación y orden por columna.
   - Selector de 7/30/90 días en "CO₂ por día".
   - Pausar el refresco de 30 s con la pestaña oculta y en modo fotosensible.
   - Revisar el contraste AA del gris secundario y `:focus-visible` en los controles del globo.
6. **Documentos viejos:** `Introduccion a Smartglass.pdf` y `propuesta_modelo_actualizada.pdf` todavía citan la corrida de 1.000 registros (MAE 23,25). Ya se entregaron. Basta aclarar en otros materiales que la corrida vigente es la de 10.000.
7. **Pendientes anteriores:** el login dice "Usuario o correo" pero solo acepta el usuario. Hay un aviso inofensivo en consola ("Transition was aborted… ViewTransition opt-in disabled") al salir de algunas páginas.

### Orden sugerido para la próxima sesión
1. Animación de los paneles del hero: es un cambio acotado y visible para la feria.
2. Capturas y README: conviene hacerlos después del punto 1, para que las capturas ya muestren el hero nuevo.
3. Commit local de todo y, si el usuario lo pide, push y revisión en Railway.

## 9. Cómo se ha verificado el trabajo

- **Pruebas del backend:** `mvnw test`, con bases de datos de prueba aparte de las de la app local.
- **Navegador:** pruebas automáticas con Chrome sin ventana, en 1440 px y en 375/390 px, en claro y oscuro, con movimiento reducido y en inglés. Se revisa que no haya desborde horizontal, que no salgan errores en consola y que la página se renderice completa.
- **Admin en local:** se entra con el usuario `admin` y una contraseña de prueba que viene de una variable de entorno; no se escribe en los scripts.
