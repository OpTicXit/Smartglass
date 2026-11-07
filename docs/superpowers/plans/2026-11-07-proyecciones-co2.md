# Proyecciones de CO₂ en el informe técnico — Plan de construcción

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir al informe técnico del admin una proyección del CO₂ total estimado en dos apartados, "Simulación" y "Actividad de la plataforma", con botones de horizonte y rango de incertidumbre.

**Architecture:** Un único método de proyección (mínimos cuadrados con tendencia y términos de Fourier) implementado dos veces con la misma fórmula: en Python con scikit-learn, para la Simulación exportada a JSON, y en Java puro, para la plataforma en vivo. El historial inyectado vive en una colección Mongo aparte, que solo lee la proyección. El frontend dibuja las dos proyecciones con el mismo componente.

**Tech Stack:** Spring Boot 4.1.1 (Java 26), MongoDB, Thymeleaf, JS sin framework (SVG propio de `informe-tecnico.js`), Python 3 con numpy, pandas y scikit-learn.

**Spec:** `docs/superpowers/specs/2026-11-07-proyecciones-co2-design.md`

## Global Constraints

- La cifra es el **CO₂ total estimado (kg CO₂e)**. En pantalla se dice "estima", nunca "mide" ni "reales".
- Nombres de los apartados: **"Simulación"** y **"Actividad de la plataforma"**.
- Regla de horizonte: no proyectar más lejos que la longitud del historial. Horizontes candidatos (clave → días): `semana` 7, `mes` 30, `cinco_meses` 150, `anio` 365, `cinco_anios` 1826.
- La gráfica muestra CO₂ por periodo. La frase-resumen es el acumulado desde hoy hasta el horizonte, sin contar lo ya emitido.
- Ninguna proyección ni límite inferior baja de 0.
- No tocar `data/raw/smartglass_co2.csv`, `mineria de datos/modelo_smartglass_co2.pkl`, `mineria de datos/metricas_modelo.csv` ni `backend/src/main/resources/informe/informe-tecnico.json`.
- El historial inyectado no aparece en listas de cotizaciones, "Mis cotizaciones", dashboard ni contadores en vivo; nada en pantalla lo distingue.
- Se activa con `DEMO_DATOS=true` (`smartglass.demo.datos`). Semilla fija 42.
- "Hoy" es siempre la fecha en `CatalogoProducto.ZONA` (America/Bogota), no la del servidor.
- Textos en `messages.properties` y `messages_en.properties`. Estilo, tooltip, tabla accesible y modos de accesibilidad iguales a las gráficas actuales del informe.
- **No hacer commit ni push sin que el usuario lo pida.** Los pasos finales de cada tarea dejan los cambios preparados (`git add`), no confirmados.

## Fórmula común (Python y Java deben coincidir)

Con historia diaria `y[0..n-1]` (t = 0 el primer día, n días):

- Variables: `1, t`; más `sin(2πkt/7), cos(2πkt/7)` para k = 1, 2 si `n ≥ 21`; más `sin(2πkt/365.25), cos(2πkt/365.25)` para k = 1, 2 si `n ≥ 730`.
- Ajuste por mínimos cuadrados ordinarios. σ = √(Σ residuo² / max(1, n − p)), con p el número de variables.
- Si `n < 7`, no hay proyección: `futuro` y `horizontes` vacíos.
- Día futuro h = 1..H (t = n − 1 + h): `y = max(0, ŷ)`, semiancho `1.96·σ·√(1 + h/n)`, `lo = max(0, ŷ − semiancho)`, `hi = ŷ + semiancho`.
- Total de un horizonte de d días: `total = Σ y(h)` para h = 1..d; semiancho `1.96·σ·√d·(1 + d/n)`; `lo = max(0, total − semiancho)`, `hi = total + semiancho`.
- `H` es el mayor horizonte disponible, es decir con `dias ≤ n`.

## Forma del JSON (los dos apartados)

```json
{ "fin": "2026-11-07" | null,
  "historia": [12.4, 0.0, ...],
  "futuro": [{"y": 13.1, "lo": 4.2, "hi": 22.0}, ...],
  "horizontes": [{"clave": "semana", "dias": 7, "total": 91.2, "lo": 60.1, "hi": 122.3}, ...],
  "metricas": {"mae": 3.1, "mape_semanal": 8.4, "mae_ingenuo": 4.7} | null }
```

`historia` es diaria y termina en `fin`. Con `fin = null` (Simulación), la página ancla el último día de la historia en la fecha de hoy. `metricas` solo existe en Simulación.

## Review Focus

1. Base vacía o sin `DEMO_DATOS` (sin historial ni cotizaciones): el apartado muestra "todavía no hay datos suficientes para proyectar" y ninguna gráfica rota. Pruebas en la Tarea 1 (n < 7) y en la Tarea 4 (pintado vacío).
2. Cotizaciones con fecha nula: se ignoran en la serie. Prueba en la Tarea 3.
3. Cotizaciones con fecha posterior a hoy (relojes o zonas distintas): se ignoran. Prueba en la Tarea 3.
4. Serie que cae fuerte al final: ningún `y`, `lo` ni `total` es negativo. Prueba en la Tarea 1.
5. Serie toda en ceros (σ = 0): sin `NaN`; totales en 0 con `lo = hi = 0`. Prueba en la Tarea 1.

---

### Task 1: Método de proyección en Java

**Files:**
- Create: `backend/src/main/java/com/smartglass/service/ProyeccionCo2.java`
- Test: `backend/src/test/java/com/smartglass/service/ProyeccionCo2Test.java`

**Interfaces:**
- Produces:
  - `public record Banda(double y, double lo, double hi)`
  - `public record Horizonte(String clave, int dias, double total, double lo, double hi)`
  - `public record Proyeccion(LocalDate fin, List<Double> historia, List<Banda> futuro, List<Horizonte> horizontes)` (sin `metricas`; Jackson lo serializa con los nombres de la sección "Forma del JSON").
  - `public static Proyeccion proyectar(List<Double> historiaDiaria, LocalDate fin)`: todos anidados en `final class ProyeccionCo2`.

- [ ] **Step 1: Escribir las pruebas que fallan**

```java
@Test void recuperaUnaTendenciaLineal() {        // y = 10 + 0.5t, n = 60, sin ruido
    var p = ProyeccionCo2.proyectar(serie(60, t -> 10 + 0.5 * t), FIN);
    assertEquals(10 + 0.5 * 60, p.futuro().get(0).y(), 1e-6);
    assertEquals(List.of("semana", "mes"), p.horizontes().stream().map(Horizonte::clave).toList());
    assertEquals(30, p.futuro().size());
}
@Test void conMenosDeSieteDiasNoProyecta() {
    var p = ProyeccionCo2.proyectar(serie(6, t -> 5), FIN);
    assertTrue(p.futuro().isEmpty()); assertTrue(p.horizontes().isEmpty());
}
@Test void nuncaBajaDeCero() {                    // 100 -> 0 en 60 dias
    var p = ProyeccionCo2.proyectar(serie(60, t -> Math.max(0, 100 - 2 * t)), FIN);
    p.futuro().forEach(b -> { assertTrue(b.y() >= 0); assertTrue(b.lo() >= 0); });
    p.horizontes().forEach(h -> { assertTrue(h.total() >= 0); assertTrue(h.lo() >= 0); });
}
@Test void serieEnCerosNoDaNaN() {
    var p = ProyeccionCo2.proyectar(serie(30, t -> 0), FIN);
    p.horizontes().forEach(h -> { assertEquals(0, h.total()); assertEquals(0, h.lo()); assertEquals(0, h.hi()); });
}
@Test void elRangoSeAbreConElTiempo() {          // con ruido determinista
    var p = ProyeccionCo2.proyectar(serie(400, t -> 20 + 0.01 * t + (t % 5) - 2), FIN);
    Banda a = p.futuro().get(0), b = p.futuro().get(299);
    assertTrue(b.hi() - b.lo() > a.hi() - a.lo());
    assertEquals(List.of("semana", "mes", "cinco_meses", "anio"), p.horizontes().stream().map(Horizonte::clave).toList());
}
```

`serie(n, f)` es un ayudante del test que devuelve `List<Double>`; `FIN = LocalDate.of(2026, 11, 7)`.

- [ ] **Step 2: Correr y ver que fallan**

Run: `cd backend; .\mvnw.cmd -q test -Dtest=ProyeccionCo2Test` (con `JAVA_HOME=C:\Program Files\Java\jdk-27`)
Expected: error de compilación porque `ProyeccionCo2` no existe.

- [ ] **Step 3: Implementar `ProyeccionCo2.proyectar(List<Double>, LocalDate)`** siguiendo la "Fórmula común". Ecuaciones normales resueltas con eliminación gaussiana con pivote parcial, matriz de máximo 10×10, sin librerías nuevas. Una lista vacía o `null` se trata como n = 0.

- [ ] **Step 4: Correr y ver que pasan**

Run: el mismo comando. Expected: `Tests run: 5, Failures: 0`.

- [ ] **Step 5: Preparar los cambios**: `git add` de los dos archivos.

---

### Task 2: Historial inyectado

**Files:**
- Create: `backend/src/main/java/com/smartglass/model/mongo/HistorialActividad.java` (colección `historial_actividad`)
- Create: `backend/src/main/java/com/smartglass/repository/mongo/HistorialActividadRepository.java`
- Create: `backend/src/main/java/com/smartglass/config/HistorialActividadSeeder.java`
- Modify: `backend/src/main/java/com/smartglass/config/DemoDatosSeeder.java:213` (añadir `@Order(1)` al método `cargar()`)
- Test: `backend/src/test/java/com/smartglass/config/HistorialActividadSeederTest.java`

**Interfaces:**
- Produces:
  - `HistorialActividad`: `id` (String), `fecha` (LocalDate), `cotizaciones` (int), `co2Kg` (double), `origen` (String, siempre `"demo"`; nunca se muestra). Constructor vacío, getters y setters, como `Resena`.
  - `HistorialActividadRepository extends MongoRepository<HistorialActividad, String>` con `List<HistorialActividad> findAllByOrderByFechaAsc()`.
  - `HistorialActividadSeeder`: `@Component`, `@ConditionalOnProperty(name = "smartglass.demo.datos", havingValue = "true")`, método `@EventListener(ApplicationReadyEvent.class) @Order(2) void cargar()`.
  - `static List<HistorialActividad> generar(LocalDate primerDiaReal, double ritmoDiario, DoubleSupplier co2DeUnaCotizacion, Random azar)`.

- [ ] **Step 1: Escribir las pruebas que fallan**

```java
@Test void generaDosAniosQueTerminanAntesDeLoReal() {
    var h = HistorialActividadSeeder.generar(D, 3.0, () -> 20.0, new Random(42));
    assertEquals(730, h.size());
    assertEquals(D.minusDays(1), h.get(729).getFecha());
    assertEquals(D.minusDays(730), h.get(0).getFecha());
}
@Test void elFinalEmpalmaConElRitmoReal() {
    var h = HistorialActividadSeeder.generar(D, 3.0, () -> 20.0, new Random(42));
    double media = h.subList(700, 730).stream().mapToInt(HistorialActividad::getCotizaciones).average().orElse(0);
    assertEquals(3.0, media, 3.0 * 0.35);
    assertEquals(h.get(729).getCotizaciones() * 20.0, h.get(729).getCo2Kg(), 1e-9);
}
@Test void esReproducible() {
    assertEquals(HistorialActividadSeeder.generar(D, 3, () -> 20, new Random(42)).stream().map(HistorialActividad::getCotizaciones).toList(),
                 HistorialActividadSeeder.generar(D, 3, () -> 20, new Random(42)).stream().map(HistorialActividad::getCotizaciones).toList());
}
@Test void noSeRepiteNiTocaCotizaciones() {        // repositorio con datos: no hace nada
    when(historial.count()).thenReturn(5L);
    seeder.cargar();
    verify(historial, never()).saveAll(any());
    verify(vidrios, never()).save(any());
}
```

`D = LocalDate.of(2026, 9, 1)`; `seeder` se construye con mocks de `HistorialActividadRepository`, `VidrioSostenibleRepository` y `AnalisisService`.

- [ ] **Step 2: Correr y ver que fallan**: `.\mvnw.cmd -q test -Dtest=HistorialActividadSeederTest`. Expected: error de compilación.

- [ ] **Step 3: Implementar `generar`**. Día i = 0..729, desde `primerDiaReal − 730`:
  - `λ = ritmoDiario · crecimiento · semana · anual`, con `crecimiento` lineal de 0.35 (i = 0) a 1.0 (i = 729).
  - `semana`: 1.1 de lunes a viernes, 0.75 sábado, 0.25 domingo (media ≈ 1).
  - `anual = 1 + 0.15·sin(2π·díaDelAño/365.25)`.
  - `cotizaciones ~ Poisson(λ)` con el algoritmo de Knuth sobre `azar`; `co2Kg` es la suma de `cotizaciones` llamadas a `co2DeUnaCotizacion`.

- [ ] **Step 4: Implementar `cargar()`**:
  - Si `historial.count() > 0`, no hace nada.
  - `primerDiaReal` = fecha (en `CatalogoProducto.ZONA`) de la cotización más antigua con fecha no nula, o hoy si no hay ninguna.
  - `ritmoDiario = max(1, cotizaciones de los últimos 30 días / 30)`.
  - `co2DeUnaCotizacion`: arma un `VidrioSostenible` al azar con `tipoVidrio` en {Crudo, Templado, Laminado, TempladoLaminado, Insulado}, ancho y alto uniformes de 30 a 250 cm, espesor en {4, 6, 8, 10, 12} mm, forma Rectangular y reciclado al 30 %, y devuelve `analisisService.calcularCotizacionVidrio(v).getHuellaCarbono()`.
  - `saveAll` con `new Random(42)` y un log de una línea, sin la palabra "demo" en pantalla.

- [ ] **Step 5: Añadir `@Order(1)` a `DemoDatosSeeder.cargar()`**, para que el historial empalme con las cotizaciones de demostración.

- [ ] **Step 6: Correr y ver que pasan**: el mismo comando. Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 7: Preparar los cambios**: `git add` de los archivos de la tarea.

---

### Task 3: Proyección de la plataforma (servicio + endpoint)

**Files:**
- Create: `backend/src/main/java/com/smartglass/service/ProyeccionPlataformaService.java`
- Modify: `backend/src/main/java/com/smartglass/controller/AdminInformeController.java` (dos endpoints)
- Test: `backend/src/test/java/com/smartglass/service/ProyeccionPlataformaServiceTest.java`

**Interfaces:**
- Consumes: `ProyeccionCo2.proyectar` (Task 1), `HistorialActividadRepository.findAllByOrderByFechaAsc()` (Task 2), `VidrioSostenibleRepository.findAll()`.
- Produces:
  - `ProyeccionPlataformaService.proyeccion(): ProyeccionCo2.Proyeccion` y `Proyeccion calcular(LocalDate hoy)` (package-private, para el test).
  - `GET /admin/informe-tecnico/proyeccion` devuelve `Proyeccion` como JSON.
  - `GET /admin/informe-tecnico/proyeccion-simulacion` sirve el classpath `informe/proyeccion-simulacion.json` (Task 5), igual que `datos()`.

- [ ] **Step 1: Escribir la prueba que falla**

```java
@Test void juntaHistorialYCotizacionesHastaHoy() {
    when(historial.findAllByOrderByFechaAsc()).thenReturn(List.of(dia(HOY.minusDays(10), 2, 30.0), dia(HOY.minusDays(9), 1, 10.0)));
    when(vidrios.findAll()).thenReturn(List.of(
            obra(HOY.minusDays(3).atTime(10, 0), 7.5), obra(HOY.minusDays(3).atTime(15, 0), 2.5),
            obra(null, 99), obra(HOY.plusDays(2).atStartOfDay(), 99)));   // sin fecha y futura: fuera
    var p = new ProyeccionPlataformaService(historial, vidrios).calcular(HOY);
    assertEquals(HOY, p.fin());
    assertEquals(11, p.historia().size());                 // de HOY-10 a HOY, dias vacios en 0
    assertEquals(30.0, p.historia().get(0));
    assertEquals(10.0, p.historia().get(7));               // HOY-3
    assertEquals(0.0, p.historia().get(10));
    assertEquals(List.of("semana"), p.horizontes().stream().map(ProyeccionCo2.Horizonte::clave).toList());
}
@Test void sinNadaNoProyecta() {
    when(historial.findAllByOrderByFechaAsc()).thenReturn(List.of());
    when(vidrios.findAll()).thenReturn(List.of());
    var p = new ProyeccionPlataformaService(historial, vidrios).calcular(HOY);
    assertTrue(p.historia().isEmpty()); assertTrue(p.horizontes().isEmpty());
}
```

- [ ] **Step 2: Correr y ver que fallan**: `.\mvnw.cmd -q test -Dtest=ProyeccionPlataformaServiceTest`.

- [ ] **Step 3: Implementar `calcular(LocalDate hoy)`**. La serie va desde la fecha más antigua (historial o cotización) hasta `hoy`; cada día suma `co2Kg` del historial y `huellaCarbono` de las cotizaciones de ese día. Se ignoran fechas nulas o posteriores a `hoy`. Luego `ProyeccionCo2.proyectar(serie, hoy)`. `proyeccion()` usa `LocalDate.now(CatalogoProducto.ZONA)`.

- [ ] **Step 4: Añadir los dos `@GetMapping`** a `AdminInformeController` (quedan bajo `/admin/**`, es decir ROLE_ADMIN).

- [ ] **Step 5: Correr y ver que pasan**; luego `.\mvnw.cmd -q test` completo en verde.

- [ ] **Step 6: Preparar los cambios**: `git add`.

---

### Task 4: Pantalla de las proyecciones

**Files:**
- Modify: `frontend/templates/admin/informe-tecnico.html`
- Modify: `frontend/static/js/informe-tecnico.js`
- Modify: `frontend/static/css/informe-tecnico.css`
- Modify: `backend/src/main/resources/messages.properties`, `backend/src/main/resources/messages_en.properties`

**Interfaces:**
- Consumes: `data-proyeccion` y `data-simulacion` en `<main>` (rutas de la Task 3), JSON con la forma de la sección "Forma del JSON".
- Produces: `function grafProyeccion(fig, p, horizonteClave)` y `function pintarProyeccion(fig, p)` en `informe-tecnico.js`.

- [ ] **Step 1: Plantilla**
  - Añadir `data-proyeccion=@{/admin/informe-tecnico/proyeccion}` y `data-simulacion=@{/admin/informe-tecnico/proyeccion-simulacion}` al `th:attr` de `<main>`.
  - Después de la figura `data-vivo-grafica`, poner un bloque `<figure class="grafica" data-proyeccion-grafica="plataforma">` con `<h3>` "Proyección del CO₂ estimado".
  - Antes del `<h2 id="modelo">`, una sección nueva `<h2 id="simulacion">` "Simulación", con intro y `<figure ... data-proyeccion-grafica="simulacion">`.
  - Cada figura lleva un grupo `role="group"` de botones `button[data-horizonte]`, un `<p class="proy-resumen" aria-live="polite">` y la `figcaption`.
  - Las claves nuevas pasan al bloque de textos JS con el mismo patrón `inf_...: /*[[#{...}]]*/ '...'`.

- [ ] **Step 2: Textos** (es / en):

```properties
admin.informe.sim_titulo=Simulación
admin.informe.sim_intro=Historia de 5 años generada a partir del dataset sintético de 10.000 piezas. Muestra cómo se proyecta el CO₂ estimado; no son emisiones medidas.
admin.informe.proy_titulo=Proyección del CO₂ estimado
admin.informe.proy_resumen=En {0} se estiman {1} kg de CO₂e (entre {2} y {3}).
admin.informe.proy_sin_datos=Todavía no hay datos suficientes para proyectar.
admin.informe.proy_h_semana=1 semana
admin.informe.proy_h_mes=1 mes
admin.informe.proy_h_cinco_meses=5 meses
admin.informe.proy_h_anio=1 año
admin.informe.proy_h_cinco_anios=5 años
admin.informe.proy_r_semana=la próxima semana
admin.informe.proy_r_mes=el próximo mes
admin.informe.proy_r_cinco_meses=los próximos 5 meses
admin.informe.proy_r_anio=el próximo año
admin.informe.proy_r_cinco_anios=los próximos 5 años
admin.informe.proy_historia=Historia
admin.informe.proy_futuro=Proyección
admin.informe.proy_rango=Rango probable
admin.informe.proy_validacion=Validación con los últimos 6 meses: error medio de {0} kg por día; error semanal de {1} %. Referencia ingenua: {2} kg por día.
```

Las versiones en inglés van con el mismo sentido ("estimated", nunca "measured" ni "real").

- [ ] **Step 3: `grafProyeccion(fig, p, clave)`**, con `lienzo`, `nodo`, `escalaBonita`, `compacto`, `conTip`, `tabla` y `leyenda` existentes.
  - Agregación por horizonte: diaria si `dias ≤ 31`, semanal si `≤ 366`, mensual (30 días) si es mayor.
  - Se dibuja la historia de la misma longitud que el horizonte (mínimo 30 días): línea `g-a` sólida, proyección discontinua y banda `lo..hi` semitransparente.
  - Fechas: el último día de `historia` es `p.fin`, o hoy si `p.fin` es `null`.

  `pintarProyeccion(fig, p)`:
  - Crea los botones solo para `p.horizontes`; el horizonte por defecto es `anio` si existe, si no el mayor disponible.
  - Escribe la frase `proy_resumen` con `proy_r_<clave>` y los números de `NUM.format`.
  - Con `horizontes` vacío oculta los botones y la gráfica y muestra `proy_sin_datos`.
  - En Simulación con `metricas`, añade `proy_validacion`.
  - Redibuja en `resize` como `grafVivo`.

- [ ] **Step 4: Cargas**: Simulación se pide una vez con `fetch(main.dataset.simulacion)`; la plataforma, dentro de `consultarVivo()`, al mismo ritmo de 30 s. Si alguno falla, se ve el mismo aviso de error que ya usa la sección en vivo.

- [ ] **Step 5: CSS**: botones de horizonte con el estilo de los controles del informe; estado activo con `aria-pressed="true"`; banda y línea discontinua con tokens de color del tema (claro y oscuro). Con `prefers-reduced-motion`, sin transiciones.

- [ ] **Step 6: Verificar en el navegador**: arrancar la copia local de prueba (puerto 50501, bases `smartglass_push_prueba`, `DEMO_DATOS=true`, `DEMO_PASSWORD` y `ADMIN_PASSWORD` de prueba) y entrar como admin a `/admin/informe-tecnico`.
  - Expected: dos gráficas con botones; la de plataforma ofrece al menos hasta `anio`.
  - Cambiar el horizonte cambia la frase y la gráfica.
  - Sin errores en consola, sin desborde a 375 px.
  - En modo oscuro las bandas se distinguen.

- [ ] **Step 7: Preparar los cambios**: `git add`.

---

### Task 5: Simulación en Python

**Files:**
- Create: `data/generacion_historial.py` → `data/raw/smartglass_historial.csv` (`fecha,piezas,co2_kg`)
- Create: `mineria de datos/proyeccion_co2.py` (módulo con la "Fórmula común")
- Create: `mineria de datos/exportar_proyeccion.py` → `backend/src/main/resources/informe/proyeccion-simulacion.json`
- Test: `mineria de datos/test_proyeccion.py` (asserts, sin pytest)

**Interfaces:**
- Produces: `proyectar(historia: np.ndarray) -> dict` con las claves `historia`, `futuro`, `horizontes` de la sección "Forma del JSON" (`fin` lo pone el exportador como `None`).

- [ ] **Step 1: Entorno**: un Python 3 con `python -m pip install numpy pandas scikit-learn`. La máquina actual no tiene scikit-learn en `python`.

- [ ] **Step 2: Escribir `test_proyeccion.py`**, con los mismos casos de la Task 1 (tendencia 10 + 0.5t con `futuro[0]["y"] ≈ 40`, n = 6 sin proyección, sin negativos, ceros sin NaN). Además, una serie fija de 60 días cuyos totales de `semana` y `mes` se comparan con los que da la Task 1 para la misma serie: copiar a mano los números de Java y verificar con tolerancia 1e-6.
  - Run: `python "mineria de datos/test_proyeccion.py"`. Expected: falla porque el módulo no existe.

- [ ] **Step 3: Implementar `proyeccion_co2.py`**: matriz de variables según la "Fórmula común" y `sklearn.linear_model.LinearRegression(fit_intercept=False)` con la columna de unos explícita. Correr el test. Expected: imprime `ok`.

- [ ] **Step 4: `generacion_historial.py`** (semilla 42, 1826 días hasta la fecha de generación):
  - `piezas ~ Poisson(λ)` con `λ = 12 · crecimiento(0.6→1.0) · semana(1.1/0.75/0.25) · (1 + 0.15·sin(2π·díaDelAño/365.25))`.
  - `co2_kg` es la suma de `piezas` valores tomados con reemplazo de la columna `co2_kg` de `smartglass_co2.csv`.

- [ ] **Step 5: `exportar_proyeccion.py`**:
  - Lee el CSV del historial, guarda los últimos 182 días para validar y ajusta con el resto.
  - `metricas`: `mae` diario; `mape_semanal` sobre los totales por semana; `mae_ingenuo` usando el mismo día de 364 días antes.
  - Luego ajusta con toda la historia y escribe el JSON con `fin: null`, redondeado a 2 decimales y separadores compactos, como `exportar_informe.py`.
  - Run: `python data/generacion_historial.py; python "mineria de datos/exportar_proyeccion.py"`. Expected: el JSON existe, ofrece hasta `cinco_anios` y `mae < mae_ingenuo`.

- [ ] **Step 6: Comprobar en la página** que la gráfica de Simulación aparece con hasta "5 años" y la frase de validación.

- [ ] **Step 7: Preparar los cambios**: `git add` (CSV y JSON incluidos; producción no tiene Python).

---

### Task 6: Cierre

- [ ] **Step 1:** `.\mvnw.cmd -q test` completo en verde, con las bases de prueba de la copia local.
- [ ] **Step 2:** Añadir una línea a `FLUJO-DE-TRABAJO.md` y al README (sección técnica) que diga de dónde salen los datos de cada apartado, incluido que "Actividad de la plataforma" lleva historial generado cuando `DEMO_DATOS=true`. Es la aclaración que el diseño deja solo en la documentación.
- [ ] **Step 3:** Pedir al usuario el commit y el push; recordarle que active `DEMO_DATOS=true` en Railway.
