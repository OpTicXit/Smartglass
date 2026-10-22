# 🚀 SmartGlass
### Plataforma de Gestión Sostenible e Industrial del Vidrio

SmartGlass es una plataforma de software desarrollada con **Spring Boot** para la industria de manufactura y comercialización de vidrio. Integra la gestión de productos, inventarios y pedidos industriales con herramientas de análisis predictivo orientadas a la estimación de emisiones de dióxido de carbono (CO₂).

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
- Estimación de emisiones de CO₂ por pieza de vidrio.
- Análisis de variables técnicas relacionadas con el impacto ambiental.
- Evaluación del efecto potencial del contenido de vidrio reciclado o calcín.
- Posible integración de predicciones ambientales en el proceso de cotización.

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
5. El componente de minería de datos procesa variables técnicas para estimar las emisiones de CO₂.
6. Los resultados pueden incorporarse a cotizaciones, análisis ambientales y reportes.

> **Nota:** La descripción anterior representa la arquitectura prevista del proyecto. La disponibilidad efectiva de cada funcionalidad depende de su implementación y configuración en el repositorio.

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
| Reducción de código repetitivo | Lombok |
| Lenguaje de análisis | Python 3.13 |
| Procesamiento de datos | pandas |
| Aprendizaje automático | scikit-learn |
| Visualización | matplotlib y seaborn |
| Entorno de análisis | Google Colab y Jupyter Notebook |
| Control de versiones | Git y GitHub |

**Compatibilidad de Java:** el entorno debe utilizar una versión de Java compatible con la versión concreta de Spring Boot configurada en el proyecto. Aunque Java 17 aparece como requisito inicial, se debe verificar el requisito mínimo de Java de la versión de Spring Boot 4.x utilizada antes de ejecutar la aplicación.

---

## 📂 Estructura del repositorio

```text
SmartGlass/
├── README.md
├── requirements.txt
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/smartglass/
│   │   │   ├── config/             # Configuración de seguridad y JWT
│   │   │   ├── controller/         # Controladores REST y web
│   │   │   ├── model/              # Entidades y modelos de dominio
│   │   │   ├── repository/         # Acceso a datos
│   │   │   ├── service/            # Lógica de negocio
│   │   │   └── SmartGlassApplication.java
│   │   │
│   │   └── resources/
│   │       ├── templates/          # Vistas Thymeleaf
│   │       ├── static/             # CSS, JavaScript e imágenes
│   │       └── application.properties
│   │
│   └── test/                       # Pruebas automatizadas
│
├── data/
│   ├── generacion_dataset.py       # Generador reproducible
│   └── raw/
│       └── smartglass_co2.csv      # Dataset generado
│
├── mineria-datos/
│   ├── analisis.ipynb              # EDA, entrenamiento y evaluación
│   ├── preparacion.py              # Preprocesamiento de datos
│   ├── modelo_smartglass_co2.pkl   # Modelo entrenado
│   └── metricas_modelo.csv         # Resultados de evaluación
│
└── documentos/
    ├── propuesta_modelo.pdf        # Entregable 2
    └── informe_metodologico.pdf    # Entregable 3
```

*Nota: esta estructura representa la organización documentada del proyecto. Los directorios y archivos deben corresponder con los que se encuentren efectivamente en el repositorio.*

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

Abre el archivo `mineria-datos/analisis.ipynb` en Google Colab o Jupyter Notebook.

Ejecuta las celdas en orden para reproducir el análisis exploratorio, el preprocesamiento, el entrenamiento y la evaluación del modelo.

Antes de ejecutar el notebook, comprueba que las rutas a los archivos CSV y Python coincidan con la estructura del entorno de ejecución.

---

## 🧠 Componente de Minería de Datos I

### Problema analítico

¿Es posible estimar los kilogramos de CO₂ asociados a una pieza de vidrio personalizada a partir de sus parámetros técnicos, antes de fabricarla?

El proyecto aborda esta pregunta mediante un modelo de regresión que utiliza características relacionadas con el producto y su proceso de fabricación.

### Dataset

| Característica | Descripción |
|---|---|
| Registros | 1.000 piezas de vidrio |
| Variables | 8 variables documentadas |
| Catálogo | 20 tipos de vidrio |
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

La descripción de las variables contempla características como el tipo de vidrio, el área, el espesor, el proceso de fabricación y el porcentaje de calcín. La definición final de las ocho variables debe verificarse en el generador y en el dataset.

### Modelo predictivo

El algoritmo seleccionado es **Random Forest Regressor**, un método de aprendizaje automático que combina múltiples árboles de decisión para realizar predicciones numéricas.

| Parámetro | Configuración documentada |
|---|---|
| Algoritmo | Random Forest Regressor |
| Número de árboles | 200 |
| Preprocesamiento categórico | One-Hot Encoding |
| Datos de entrenamiento | 80 % (800 registros) |
| Datos de prueba | 20 % (200 registros) |
| Validación cruzada | 5 particiones (*5-fold*) |

### Métricas de evaluación

Los resultados reportados para el modelo son los siguientes:

| Métrica | Resultado | Interpretación |
|---|---:|---|
| MAE | 20,69 kg de CO₂ | Error absoluto medio de las predicciones. |
| RMSE | 33,49 kg de CO₂ | Penaliza en mayor medida los errores grandes. |
| R² | 0,9461 | El modelo explica aproximadamente el 94,61 % de la variabilidad de la variable objetivo en la evaluación reportada. |
| R² de validación cruzada | 0,9456 ± 0,0123 | Media y desviación reportadas en la validación de cinco particiones. |

Estos valores corresponden a los resultados documentados del proyecto y deben reproducirse a partir del dataset, el código de entrenamiento y el procedimiento de evaluación para confirmar su consistencia.

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

El artefacto entrenado `modelo_smartglass_co2.pkl` está destinado a una posible integración con el módulo ambiental de SmartGlass, relacionado con las historias de usuario US-09 y US-13.

Su integración permitiría desarrollar las siguientes funcionalidades:

### Estimación en tiempo real

Calcular una estimación de las emisiones de CO₂ durante la cotización de una pieza de vidrio personalizada, utilizando sus parámetros técnicos.

### Recomendación de alternativas sostenibles

Comparar escenarios de fabricación y estudiar si un mayor porcentaje de calcín u otras características del producto se asocian con una reducción de emisiones.

Estas recomendaciones deben sustentarse en los datos y en las restricciones técnicas del proceso industrial; no se debe asumir que aumentar el contenido reciclado siempre reduce las emisiones en cualquier escenario.

### Reportes ambientales

Consolidar predicciones y datos operativos para elaborar indicadores ambientales y facilitar su análisis en herramientas como Power BI.

### Consideraciones de integración

Para conectar el modelo de Python con el backend Java, se debe definir un mecanismo de comunicación, por ejemplo, un servicio de inferencia independiente o una API. También se deben garantizar:

- La compatibilidad entre las variables de entrada y el preprocesamiento del entrenamiento.
- El uso de las mismas categorías y transformaciones que utilizó el modelo original.
- El control de versiones del modelo y sus dependencias.
- La validación de entradas y el manejo de errores.
- La identificación de las predicciones como estimaciones, no como mediciones directas.

La presencia del archivo `.pkl` no implica que el modelo ya esté integrado en el backend. Esta funcionalidad depende de la implementación del mecanismo de inferencia.

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

Incorpora el análisis y la estimación de emisiones de CO₂ como apoyo para la evaluación ambiental de productos y procesos industriales.

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
