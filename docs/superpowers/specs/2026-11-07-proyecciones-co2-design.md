# Proyecciones de CO₂ en el informe técnico del admin

Fecha: 2026-11-07 · Estado: borrador para aprobación

## Para qué

El informe técnico del admin (`/admin/informe-tecnico`) muestra hoy el modelo que predice el CO₂ de **una pieza** y la actividad de los últimos 30 días. Le falta mirar hacia adelante: **cómo se verá el CO₂ total estimado en el futuro**, a 1 semana, 5 meses o lo más lejos que los datos permitan.

Lo verán sobre todo el jurado de la feria y dueños de vidrierías. La cifra que tiene que convencer es el CO₂: "si siguen así, en un año habrán emitido tanto". SmartGlass **estima** el CO₂, no lo mide, y así se dice en pantalla.

Como no hay datos de una vidriería real, el informe tiene **dos apartados** que no se mezclan:

1. **Simulación**: una historia sintética, generada a partir del dataset de 10.000 piezas.
2. **Actividad de la plataforma**: las cotizaciones guardadas en SmartGlass, más un historial inyectado para que la proyección tenga de dónde partir.

## Objetivos

- Proyectar el **CO₂ total estimado (kg CO₂e)** en los dos apartados, con un rango de incertidumbre que se abre con el tiempo.
- Dejar elegir el horizonte con botones (por ejemplo 1 semana, 1 mes, 5 meses, 1 año, 5 años). Solo aparecen los horizontes que el historial permite.
- Regla de horizonte: **no proyectar más lejos que la longitud del historial disponible**.
- Que funcione en Railway, donde no hay Python.

## Fuera de alcance

- Proyectar ventas o número de cotizaciones; puede añadirse después como segunda cifra.
- Open-Meteo: se descartó, porque da clima y no CO₂ del vidrio.
- Cambiar el modelo Random Forest que predice el CO₂ de una pieza, sus métricas y su JSON. El modelo de series de tiempo es **adicional**.

## Lo que no puede pasar (anti-objetivos)

- Ningún apartado se llama "datos reales" ni dice "reales". El historial inyectado va en "Actividad de la plataforma" sin aviso en pantalla (decisión del usuario); la aclaración vive solo en la documentación técnica (README o informe del proyecto).
- No proyectar más allá de lo que el historial permite, aunque se vea más espectacular.
- No inflar los contadores existentes de la sección en vivo: los contadores (total de cotizaciones, pendientes, pedidos, últimos 7 días) siguen contando solo lo guardado en la plataforma.
- No ensuciar las listas visibles: el historial inyectado no aparece en "Mis cotizaciones" de ningún cliente, ni en la lista de cotizaciones del admin, ni en el dashboard.

## Decisiones tomadas con el usuario

- Cifra a proyectar: CO₂ total estimado.
- Dos apartados con los nombres **"Simulación"** y **"Actividad de la plataforma"**.
- El historial inyectado se usa, sin marca visible en pantalla.
- Horizonte: hasta donde los datos lo permitan (los "10 años" eran solo un ejemplo).

## Cómo se construye (decisiones mías, revisables)

### Apartado 1: Simulación

- **Historia sintética nueva**, sin tocar `data/raw/smartglass_co2.csv`, del que dependen el modelo y sus métricas. Un script nuevo (por ejemplo `data/generacion_historial.py`, semilla 42, reproducible) genera `data/raw/smartglass_historial.csv` con **5 años diarios** hasta la fecha de generación.
  - Piezas por día: tendencia de crecimiento + estacionalidad semanal (menos los domingos) + estacionalidad anual (picos de obra en temporada seca) + ruido.
  - CO₂ de cada día: suma de piezas tomadas al azar del dataset de 10.000. Así la historia hereda la composición real del dataset (tipos, espesores, calcín).
- **Modelo de proyección**: regresión sobre el tiempo, con tendencia y términos de Fourier para la estacionalidad semanal y anual, hecho con scikit-learn, que ya usa el proyecto. Se valida reservando los **últimos 6 meses**: se reportan MAE y MAPE y se comparan con una referencia ingenua (repetir el mismo periodo del año anterior). El rango de incertidumbre sale de los residuos de validación y crece con el horizonte.
- **Exportación**: `mineria de datos/exportar_informe.py` añade una sección `proyeccion` al JSON versionado (`backend/src/main/resources/informe/informe-tecnico.json`), con la historia agregada, la proyección por horizonte, el rango y las métricas. Producción solo lee el JSON, igual que hoy.
- Para no pesar demasiado, la historia larga va agregada por semana y la última parte por día; el formato exacto lo decide quien lo construya.

### Apartado 2: Actividad de la plataforma

- **Datos**: CO₂ por día de las cotizaciones (`VidrioSostenible.fechaCreacion` y `huellaCarbono`), igual que la gráfica en vivo actual, **más** el historial inyectado.
- **Historial inyectado**: colección nueva de Mongo con un documento por día (`fecha`, `cotizaciones`, `co2Kg` y una marca interna de origen, nunca visible en pantalla). Se descartó crear miles de cotizaciones falsas, porque aparecerían en las listas y en el dashboard.
  - Se genera **2 años hacia atrás** desde el día anterior a la cotización real más antigua, para no solaparse con lo real.
  - El CO₂ de cada día se calcula con el cotizador (`AnalisisService`) sobre configuraciones al azar dentro de los límites del formulario, para que sea coherente con el CO₂ de las cotizaciones reales.
  - Se activa con el mismo interruptor que los demás datos de demostración (`DEMO_DATOS=true`), corre una sola vez y no se repite si ya existe. Tiene **su propio guardia** (la colección ya tiene documentos), no el de `DemoDatosSeeder`, para que también corra en una base que ya tenía los datos de demostración. Corre después de las cotizaciones de demostración, para saber dónde empieza lo real.
  - **Volumen y empalme** (decisión mía): el historial es una curva de crecimiento que **termina en el ritmo real** de los últimos 30 días de la plataforma, con un mínimo de 1 cotización por día. Así la serie no cae de golpe donde empieza lo real y la tendencia no se vuelve negativa. Ninguna proyección baja de cero.
- **Proyección en vivo, en Java**: el mismo método que en Simulación (tendencia + Fourier, mínimos cuadrados) implementado en el backend, porque estos datos cambian con cada cotización. Va en un endpoint nuevo bajo `/admin/informe-tecnico/` (protegido por ROLE_ADMIN como todo `/admin/**`).
  - Estacionalidad semanal solo con 3 semanas o más de historial; anual solo con 2 años o más. Si hay muy poco historial, solo tendencia.
  - Si no hay historial, el apartado dice que todavía no hay datos suficientes para proyectar.
- **Contadores**: los de la sección en vivo no cambian (ver anti-objetivos).

### En pantalla

- La página conserva el globo arriba. Debajo, dos bloques o pestañas: **Simulación** (el modelo actual más su proyección) y **Actividad de la plataforma** (la actividad en vivo actual más su proyección).
- Cada proyección: gráfica con la historia (línea), la proyección (línea discontinua) y el rango (banda), los botones de horizonte y una frase-resumen del tipo "En el próximo año se estiman 18.400 kg CO₂e (entre 15.900 y 20.800)".
- **Qué significa la cifra** (decisión mía): la gráfica muestra el CO₂ **por periodo** (día, semana o mes). La frase-resumen es el **acumulado desde hoy hasta el horizonte**, sin contar lo ya emitido. Su rango se calcula para la suma del periodo completo, no día a día, y el cálculo es el mismo en Python y en Java.
- **Fecha de Simulación** (decisión mía): la historia sintética es ilustrativa, así que el JSON guarda los días relativos al final de la historia y la página la ancla a la fecha de hoy al pintarla. La proyección siempre empieza hoy, aunque el JSON se haya generado semanas antes.
- Reutilizar el estilo de gráficas, tooltip y colores del informe actual, en español e inglés, con modo oscuro y los modos de accesibilidad (movimiento reducido, daltónico, fotosensible) igual que el resto del informe.

## Dónde se usa

- **En la feria se muestra en dos lugares a la vez:** el usuario presenta desde su computador en local, porque navega más rápido, y el jurado puede seguirlo o probar por su cuenta en Railway.
- Los dos entornos necesitan `DEMO_DATOS=true` para que "Actividad de la plataforma" tenga historial. En Railway lo configura el usuario; hoy no se sabe si está activado.
- El generador del historial usa una semilla fija, así que local y Railway muestran una historia parecida. No será idéntica, porque cada base empalma con sus propias cotizaciones guardadas.
- Railway se actualiza con cada push y sus pruebas las hace el usuario.

## Restricciones

- Spring Boot + Thymeleaf + MySQL + Mongo; Railway sin Python.
- Textos en `messages.properties` y `messages_en.properties`.
- No cambiar `smartglass_co2.csv`, `modelo_smartglass_co2.pkl` ni `metricas_modelo.csv`.
- Pruebas: una para el método de proyección en Java (recupera una tendencia conocida, respeta el límite de horizonte, sin historial no proyecta), una para que el historial inyectado no se duplique y no toque cotizaciones, y que `mvnw test` siga en verde.

## Lo que queda a criterio de quien lo construya

- Formato exacto del JSON y del endpoint.
- Diseño de las pestañas o bloques, dentro del estilo del informe.
- Agregación (día, semana o mes) según el horizonte elegido.
- Valores concretos de tendencia y estacionalidad de la historia sintética, mientras se vean creíbles para una vidriería mediana.
