# 🚀 SmartGlass
### Plataforma de Gestión Sostenible e Industrial del Vidrio

SmartGlass es una plataforma de software desarrollada con **Spring Boot** para la industria de manufactura y comercialización de vidrio. Integra la gestión de productos, inventarios y pedidos industriales con un cotizador que estima las emisiones de dióxido de carbono (CO₂) de cada pieza mediante fórmulas deterministas. Como componente de Minería de Datos I, incluye además un modelo predictivo de CO₂ entrenado y evaluado aparte; su integración en la aplicación es la siguiente fase.

El proyecto combina tecnologías de desarrollo backend, persistencia híbrida y minería de datos para facilitar la toma de decisiones, mejorar la trazabilidad de los productos y promover prácticas de producción más sostenibles.

Entre sus principales funcionalidades se encuentran la autenticación segura mediante JWT y Google OAuth2, la administración de catálogos especializados, la gestión de pedidos y la estimación de la huella de carbono de piezas de vidrio personalizadas.

---

## 📑 Tabla de contenidos

- [Equipo de desarrollo](#-equipo-de-desarrollo)
- [Objetivos del proyecto](#-objetivos-del-proyecto)
- [Funcionalidades principales](#-funcionalidades-principales)
- [Arquitectura del sistema](#️-arquitectura-del-sistema)
- [Tecnologías utilizadas](#️-tecnologías-utilizadas)
- [Estructura del repositorio](#-estructura-del-repositorio)
- [Requisitos previos](#-requisitos-previos)
- [Instalación y configuración](#-instalación-y-configuración)
- [Ejecución del proyecto](#-ejecución-del-proyecto)
- [Componente de minería de datos](#-componente-de-minería-de-datos)
- [Integración del modelo predictivo](#-integración-del-modelo-predictivo)
- [Alineación con los ODS](#-alineación-con-los-objetivos-de-desarrollo-sostenible)
- [Referencias técnicas](#-referencias-técnicas)
- [Licencia](#-licencia)

---

## 👥 Equipo de desarrollo

| Rol | Información |
|---|---|
| Integrantes | Javier Berrio, Argenis Caro y Miguel Martinez |
| Líder del proyecto de aula / Docente | Ing. Cristian Camilo Cuadrado Beltrán |
| Asignatura | Minería de Datos I |
| Fecha | Octubre de 2026 |

---

## 🎯 Objetivos del proyecto

### Objetivo general

Desarrollar una plataforma tecnológica para la gestión industrial del vidrio que integre procesos comerciales, administración de información y análisis predictivo de emisiones de CO₂, contribuyendo a una producción más eficiente y sostenible.

### Objetivos específicos

- Digitalizar la gestión de productos, inventarios y pedidos industriales.
- Implementar mecanismos seguros de autenticación y autorización.
- Organizar la información mediante una arquitectura de persistencia híbrida.
- Construir un conjunto de datos reproducible para el análisis de la huella de carbono.
- Entrenar y evaluar un modelo de aprendizaje automático para estimar las emisiones asociadas a piezas de vidrio.
- Explorar alternativas de fabricación que permitan reducir el impacto ambiental.

---

## ✨ Funcionalidades principales

### 🏭 Gestión industrial
- Administración de catálogos de productos y especificaciones técnicas.
- Gestión de pedidos y sus respectivos detalles.
- Conservación de información histórica de precios y productos mediante campos de tipo *snapshot*.
- Gestión de carritos de compra y sesiones de adquisición.

### 🔐 Seguridad y autenticación
- Gestión de usuarios, credenciales y roles.
- Autenticación mediante JSON Web Tokens (JWT).
- Integración con Google OAuth2 para el inicio de sesión.
- Control de acceso mediante Spring Security.

### 🌱 Gestión ambiental
- Estimación de emisiones de CO₂ por pieza de vidrio en el cotizador, con fórmulas deterministas (`AnalisisService`), no con el modelo predictivo.
- Análisis de variables técnicas relacionadas con el impacto ambiental.
- Evaluación del efecto potencial del contenido de vidrio reciclado o calcín.
- Integración del modelo predictivo en la cotización: **siguiente fase** (no implementada; ver [Integración del modelo predictivo](#-integración-del-modelo-predictivo)).

### 📊 Minería de datos
- Generación reproducible de datos sintéticos basados en factores de emisión documentados.
- Análisis exploratorio de datos (EDA).
- Entrenamiento de modelos de regresión.
- Evaluación mediante métricas estadísticas y validación cruzada.
- Visualización de resultados e importancia de las variables.

---

## 🏗️ Arquitectura del sistema

SmartGlass utiliza una arquitectura de persistencia híbrida que combina bases de datos relacionales y documentales. Esta separación permite adaptar el almacenamiento a las características de cada tipo de información.

### 🗄️ MySQL — Persistencia relacional

Se utiliza mediante **Spring Data JPA e Hibernate** para administrar información estructurada y transaccional.

| Entidad | Responsabilidad |
|---|---|
| `Usuario` | Gestión de usuarios, credenciales, roles y accesos. |
| `Pedido` | Registro y administración de órdenes de compra industriales. |
| `DetallePedido` | Almacenamiento de los productos, cantidades y precios asociados a cada pedido. |

Los detalles de los pedidos utilizan campos *snapshot* para conservar información histórica relevante, evitando que los cambios posteriores en los catálogos alteren los datos de operaciones anteriores.

### 🍃 MongoDB — Persistencia documental

Se utiliza mediante **Spring Data MongoDB** para almacenar documentos flexibles, catálogos dinámicos y datos orientados a consultas específicas.

| Documento | Responsabilidad |
|---|---|
| `CatalogoProducto` | Variedades de vidrio y sus especificaciones técnicas. |
| `VidrioSostenible` | Métricas ambientales, huella de carbono y eficiencia energética. |
| `Carrito` | Información temporal relacionada con las compras de los usuarios. |

### 🔄 Flujo general de información

1. El usuario accede a la plataforma mediante los mecanismos de autenticación disponibles.
2. Los controladores reciben las solicitudes y las delegan a los servicios de negocio.
3. La información transaccional se gestiona mediante MySQL.
4. Los catálogos y documentos flexibles se administran mediante MongoDB.
5. El cotizador calcula peso, huella de CO₂, intensidad (kg CO₂e/m²), precio y resistencia de cada pieza con fórmulas deterministas en el backend (`AnalisisService`).
6. El modelo predictivo de minería de datos se entrena y evalúa aparte, en Python; la aplicación Java **no** lo consulta.

> **Nota:** los pasos 1 a 5 están implementados en el repositorio. Conectar el modelo del paso 6 con las cotizaciones es la siguiente fase.

---

## 🛠️ Tecnologías utilizadas

| Categoría | Tecnologías |
|---|---|
| Lenguaje backend | Java |
| Framework | Spring Boot 4.x |
| Motor de plantillas | Thymeleaf |
| Seguridad | Spring Security, JWT y OAuth2 Client |
| Base de datos relacional | MySQL 8.x |
| Base de datos documental | MongoDB |
| Persistencia | Spring Data JPA, Hibernate y Spring Data MongoDB |
| Gestión de dependencias | Maven |
| Lenguaje de análisis | Python 3.13 |
| Procesamiento de datos | pandas |
| Aprendizaje automático | scikit-learn |
| Visualización | matplotlib y seaborn |
| Entorno de análisis | Google Colab y Jupyter Notebook |
| Control de versiones | Git y GitHub |

**Compatibilidad de Java:** el proyecto usa Spring Boot 4.1.1 y compila para Java 26 (`java.version` en `pom.xml`); se necesita un JDK 26 o posterior.

---

## 📂 Estructura del repositorio

```text
SmartGlass/
├── README.md
├── FLUJO-DE-TRABAJO.md             # Estado de cada ítem contra el código
├── requirements.txt                # Dependencias de Python
├── .env.example                    # Plantilla de variables de entorno (sin secretos)
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/smartglass/
│   │   │   ├── config/             # Spring Security, seeders, recursos subidos
│   │   │   ├── controller/         # Controladores REST y web
│   │   │   ├── dto/                # Objetos de entrada y salida
│   │   │   ├── model/              # Entidades JPA (mysql/) y documentos (mongo/)
│   │   │   ├── repository/         # Acceso a datos
│   │   │   ├── security/           # JWT y límites de solicitudes
│   │   │   ├── service/            # Lógica de negocio y cálculo ambiental
│   │   │   ├── validation/         # Validadores del registro
│   │   │   └── SmartglassApplication.java
│   │   │
│   │   └── resources/
│   │       ├── templates/          # Vistas Thymeleaf
│   │       ├── static/             # CSS, JavaScript e imágenes
│   │       └── application.properties
│   │
│   └── test/                       # Prueba de carga del contexto (contextLoads)
│
├── data/
│   ├── generacion_dataset.py       # Generador reproducible
│   └── raw/
│       └── smartglass_co2.csv      # Dataset generado
│
├── mineria de datos/
│   ├── analisis.ipynb              # EDA, entrenamiento y evaluación
│   ├── preparacion.py              # Preprocesamiento de datos
│   ├── modelo_smartglass_co2.pkl   # Modelo entrenado (no integrado en la app)
│   └── metricas_modelo.csv         # Resultados de evaluación
│
└── documentos/
    ├── propuesta_modelo.pdf        # Entregable 2
    └── informe_metodologico.pdf    # Entregable 3
```

---

## ⚙️ Requisitos previos

Antes de ejecutar SmartGlass, verifica que el entorno disponga de los siguientes componentes.

### Backend

- JDK compatible con la versión de Spring Boot utilizada.
- MySQL Server 8.x en ejecución, normalmente en el puerto `3306`.
- MongoDB en ejecución local o una instancia accesible mediante URI.
- Maven o el Maven Wrapper incluido en el repositorio.
- Git para clonar y administrar el código fuente.

### Minería de datos

- Python 3.13 o una versión compatible con las dependencias seleccionadas.
- `pip` para instalar las bibliotecas.
- Google Colab o Jupyter Notebook para ejecutar el análisis.

---

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/OpTicXit/Smartglass.git
cd Smartglass
```

### 2. Configurar las bases de datos

Asegúrate de que MySQL y MongoDB estén instalados y accesibles.

Configura las credenciales y las URI de conexión mediante las propiedades de Spring Boot o variables de entorno. No publiques contraseñas, tokens ni credenciales reales en el repositorio.

Las propiedades de conexión deben corresponder con los nombres y mecanismos de configuración que utilice la aplicación.

### 3. Configurar las variables de entorno

Si el proyecto utiliza autenticación con Google OAuth2, registra las credenciales correspondientes en Google Cloud y configura el identificador y el secreto del cliente fuera del código fuente.

De igual manera, configura las claves utilizadas para firmar o validar los JWT mediante un mecanismo seguro.

`application.properties` no contiene secretos: los lee de estas variables de entorno. La plantilla [`.env.example`](.env.example) las lista con valores de ejemplo no secretos. Spring Boot **no** lee ese archivo por sí solo: hay que definir las variables en el entorno (abajo).

| Variable | Obligatoria | Uso |
|---|---|---|
| `MYSQL_PASSWORD` | Sí | Contraseña de MySQL |
| `MYSQL_USER` | No (por defecto `root`) | Usuario de MySQL |
| `JWT_SECRET` | Sí | Clave HMAC-SHA256 para firmar los JWT (mínimo 32 bytes); debe ser **fija** mientras la app esté en uso |
| `GOOGLE_CLIENT_ID` | Sí | Cliente OAuth2 de Google |
| `GOOGLE_CLIENT_SECRET` | Sí | Secreto del cliente OAuth2 de Google |
| `ADMIN_PASSWORD` | Solo la primera vez | Contraseña del administrador inicial (`admin`); si falta, no se crea |

Sin las obligatorias, la aplicación no arranca (`PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET'`, por ejemplo). MongoDB no necesita variables: se conecta a `mongodb://localhost:27017/smartglass_catalog`.

Genera **una vez** un `JWT_SECRET` y guárdalo fijo en tu usuario de Windows (así sobrevive a reinicios de la app y de DevTools; si cambia, los JWT ya emitidos dejan de valer):

```powershell
[Environment]::SetEnvironmentVariable('JWT_SECRET', [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 })), 'User')
```

Luego, en cada terminal nueva (las variables de usuario se cargan al abrirla):

```powershell
$env:MYSQL_PASSWORD = '<tu contraseña de MySQL>'
$env:GOOGLE_CLIENT_ID = '<id de cliente de Google>'
$env:GOOGLE_CLIENT_SECRET = '<secreto de cliente de Google>'
# $env:ADMIN_PASSWORD = '<contraseña>'   # solo si el admin aún no existe en MySQL
.\mvnw.cmd spring-boot:run
```

Para comprobar qué falta antes de arrancar:

```powershell
'MYSQL_PASSWORD','JWT_SECRET','GOOGLE_CLIENT_ID','GOOGLE_CLIENT_SECRET' | Where-Object { -not [Environment]::GetEnvironmentVariable($_) }
```

**Importante:** no subas secretos reales a GitHub ni incluyas credenciales directamente en `application.properties`.

### 4. Instalar las dependencias de Python

Desde la raíz del repositorio, ejecuta:

```bash
python -m pip install -r requirements.txt
```

Se recomienda utilizar un entorno virtual para aislar las dependencias:

```bash
python -m venv .venv
```

En Windows PowerShell:

```powershell
.\.venv\Scripts\Activate.ps1
```

Después de activar el entorno, instala las dependencias de nuevo si es necesario.

---

## ▶️ Ejecución del proyecto

> Para publicar la aplicación en Render o Railway (Docker, perfil `prod`, MongoDB Atlas y MySQL gestionado), sigue [DESPLIEGUE.md](DESPLIEGUE.md).

### 1. Iniciar el backend

Desde la raíz del proyecto, ejecuta el Maven Wrapper.

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux o macOS:**

```bash
./mvnw spring-boot:run
```

Si el proyecto no contiene el Maven Wrapper, utiliza Maven instalado localmente:

```bash
mvn spring-boot:run
```

Cuando la aplicación se inicie correctamente, estará disponible en:

**http://localhost:50500**

Este puerto corresponde a la configuración indicada para el proyecto y debe coincidir con la propiedad `server.port` de Spring Boot.

### 2. Generar el dataset

Desde la raíz del repositorio, ejecuta:

```bash
python data/generacion_dataset.py
```

El script está diseñado para generar un conjunto reproducible de 1.000 registros de piezas de vidrio, con una semilla aleatoria fija de `42`.

El archivo de salida esperado es:

```text
data/raw/smartglass_co2.csv
```

### 3. Ejecutar el análisis predictivo

Abre el archivo `mineria de datos/analisis.ipynb` en Google Colab o Jupyter Notebook.

Ejecuta las celdas en orden para reproducir el análisis exploratorio, el preprocesamiento, el entrenamiento y la evaluación del modelo. El notebook busca el CSV junto a sí mismo (Colab, subiéndolo) o en `../data/raw/` (desde el repositorio). Al terminar, reescribe `modelo_smartglass_co2.pkl` y `metricas_modelo.csv`.

---

## 🧠 Componente de Minería de Datos I

### Problema analítico

¿Es posible estimar los kilogramos de CO₂ asociados a una pieza de vidrio personalizada a partir de sus parámetros técnicos, antes de fabricarla?

El proyecto aborda esta pregunta mediante un modelo de regresión que utiliza características relacionadas con el producto y su proceso de fabricación.

### Dataset

| Característica | Descripción |
|---|---|
| Registros | 1.000 piezas de vidrio |
| Columnas | 8: `tipo_vidrio`, `proceso`, `area_m2`, `espesor_mm`, `calcin_pct`, `masa_kg`, `energia_kwh_kg`, `co2_kg` |
| Catálogo | 20 tipos de vidrio (propios del dataset; el cotizador web ofrece 7) |
| Variable objetivo | `co2_kg` |
| Tipo de problema | Regresión supervisada |
| Semilla de generación | `42` |
| Fuente | Datos derivados de factores de emisión documentados |

Entre los tipos de vidrio considerados se encuentran:

- Float.
- Templado.
- Laminado.
- Blindado.
- Bajo emisivo.
- Insulado.
- Fotovoltaico.
- Inteligente.
- Reciclado.

El modelo usa **6 variables predictoras**: `tipo_vidrio` y `proceso` (categóricas, One-Hot) y `area_m2`, `espesor_mm`, `calcin_pct` y `energia_kwh_kg` (numéricas). **`masa_kg` se excluye**: el generador la calcula como `area_m2 × espesor_mm × 2,5`, la misma fórmula de la que sale `co2_kg`, así que incluirla filtraría la respuesta al modelo (*leakage*) e inflaría las métricas.

El dataset modela sus propios factores de emisión (multiplicador por tipo, energía del proceso, factor de red de 0,25 kg CO₂/kWh, calcín de 0 a 80 %). **No** reproduce las fórmulas del cotizador web (`AnalisisService`), que usa 7 tipos y factores distintos. Por eso el modelo es un estudio independiente, no una validación del cotizador.

### Modelo predictivo

El algoritmo seleccionado es **Random Forest Regressor**, un método de aprendizaje automático que combina múltiples árboles de decisión para realizar predicciones numéricas.

| Parámetro | Configuración documentada |
|---|---|
| Algoritmo | Random Forest Regressor |
| Número de árboles | 200 |
| Variables predictoras | 6 (sin `masa_kg`) |
| Preprocesamiento categórico | One-Hot Encoding |
| Datos de entrenamiento | 80 % (800 registros) |
| Datos de prueba | 20 % (200 registros) |
| Partición | Aleatoria, semilla `42` (la misma de `preparacion.dividir`) |
| Validación cruzada | 5 particiones (*5-fold*) |

### Métricas de evaluación

Resultados del notebook re-ejecutado sin `masa_kg` (los mismos que guarda `metricas_modelo.csv`):

| Métrica | Resultado | Interpretación |
|---|---:|---|
| MAE | 23,25 kg de CO₂ | Error absoluto medio en el conjunto de prueba. |
| RMSE | 33,22 kg de CO₂ | Penaliza en mayor medida los errores grandes. |
| R² (prueba) | 0,9433 | El modelo explica cerca del 94,3 % de la variabilidad en el conjunto de prueba. |
| R² de validación cruzada | 0,9271 ± 0,0115 | Media y desviación en cinco particiones; es la cifra más representativa. |

Una versión anterior reportaba MAE 20,69 y R² 0,9461. Esas cifras salían de un modelo que **incluía `masa_kg`** y por eso eran más altas; se descartaron por la filtración descrita arriba.

**Consideración metodológica:** un R² elevado no demuestra por sí solo que las predicciones representen con precisión las emisiones industriales reales. La calidad del modelo depende de la fiabilidad de los factores de emisión, la representatividad del dataset y la ausencia de filtraciones de información entre entrenamiento y prueba.

### Flujo de trabajo analítico

1. Generación del dataset mediante un script reproducible.
2. Exploración estadística de las variables.
3. Visualización de distribuciones y relaciones entre características.
4. Preprocesamiento de variables categóricas mediante One-Hot Encoding.
5. Separación de los datos en entrenamiento y prueba.
6. Entrenamiento del Random Forest Regressor.
7. Evaluación mediante MAE, RMSE y R².
8. Validación cruzada de cinco particiones.
9. Análisis de la importancia de las variables.
10. Visualización de los valores reales frente a las predicciones.

---

## 🔬 Integración del modelo predictivo

### Estado actual

**El modelo no está integrado en la aplicación.** `modelo_smartglass_co2.pkl` es un artefacto entrenado y evaluado aparte (MAE 23,25 kg · R² de validación cruzada 0,9271, sin filtración), versionado en el repositorio. Ningún componente Java lo carga ni lo consulta: las cifras de CO₂ que muestra la web salen de las fórmulas deterministas de `AnalisisService`. La presencia del archivo `.pkl` no implica integración.

Además, hoy no se podría conectar directamente: el modelo necesita `proceso`, `energia_kwh_kg` y `calcin_pct`, que el formulario de cotización no pide, y conoce 20 tipos de vidrio frente a los 7 del cotizador.

### Siguiente fase (no implementada)

La integración prevista, relacionada con las historias de usuario US-09 y US-13, permitiría:

- **Estimación durante la cotización:** consultar el modelo con los parámetros de la pieza, como segunda opinión junto al cálculo determinista.
- **Recomendación de alternativas sostenibles:** comparar escenarios de fabricación (por ejemplo, más calcín). Estas recomendaciones deben sustentarse en los datos y en las restricciones técnicas del proceso; no se debe asumir que aumentar el contenido reciclado siempre reduce las emisiones.
- **Reportes ambientales:** consolidar predicciones y datos operativos en indicadores, por ejemplo en Power BI.

### Requisitos para integrarlo

Hace falta un mecanismo de comunicación entre Python y Java, por ejemplo un servicio de inferencia independiente con API. También hay que garantizar:

- La compatibilidad entre las variables de entrada y el preprocesamiento del entrenamiento.
- El uso de las mismas categorías y transformaciones que utilizó el modelo original.
- El control de versiones del modelo y sus dependencias.
- La validación de entradas y el manejo de errores.
- La identificación de las predicciones como estimaciones, no como mediciones directas.
- Que el formulario de cotización recoja las variables que el modelo necesita, o que el dataset se alinee con las variables y tipos del cotizador.

---

## 🌍 Alineación con los Objetivos de Desarrollo Sostenible

SmartGlass se relaciona con los siguientes Objetivos de Desarrollo Sostenible (ODS) de las Naciones Unidas:

### ODS 9 — Industria, innovación e infraestructura

Promueve la digitalización de procesos industriales mediante herramientas especializadas para administrar productos, pedidos y datos técnicos.

### ODS 11 — Ciudades y comunidades sostenibles

Contribuye potencialmente a una mejor trazabilidad de la información ambiental de productos de vidrio utilizados en entornos urbanos y arquitectónicos.

### ODS 12 — Producción y consumo responsables

Facilita el análisis de materiales reciclados, las características de fabricación y las alternativas orientadas a reducir el impacto ambiental.

### ODS 13 — Acción por el clima

Incorpora la estimación de emisiones de CO₂ en el cotizador y un análisis predictivo de las emisiones como apoyo para la evaluación ambiental de productos y procesos industriales.

---

## 📚 Referencias técnicas

- Branca, T. A., Colla, V., & Algermissen, D. (2020). *Reducing CO₂ emissions in energy-intensive industries*. Energy, 211, 118650.
- Ministerio para la Transición Ecológica y el Reto Demográfico (MITECO). (2013). *Fabricación de vidrio: Proceso*.
- Guardian Glass. *How glass is made*.
- Colombia. Ley 2169 de 2021. Medidas para impulsar el desarrollo bajo en carbono del país y establecer metas y medidas mínimas en materia de carbono-neutralidad y resiliencia climática.

Las referencias deben contrastarse con las fuentes originales antes de la entrega académica, especialmente los factores de emisión específicos utilizados para construir el dataset.

---

## ⚖️ Licencia y uso académico

SmartGlass es un proyecto desarrollado con fines académicos

Su propósito es aplicar conocimientos de ingeniería de software, persistencia de datos y aprendizaje automático a un problema relacionado con la sostenibilidad en la industria del vidrio.

No se especifica una licencia de software abierta en este documento.

---

## 🤝 Contribuciones

Para contribuir al proyecto:

1. Clona el repositorio.
2. Crea una rama para tus cambios.
3. Implementa y prueba las modificaciones.
4. Registra los cambios mediante commits descriptivos.
5. Envía un *pull request* para su revisión.

Repositorio: [https://github.com/OpTicXit/Smartglass](https://github.com/OpTicXit/Smartglass)

---

**SmartGlass — Tecnología, industria y sostenibilidad para el futuro del vidrio.**
