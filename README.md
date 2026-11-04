# 🚀 SmartGlass
### Plataforma de venta de vidrio a medida con estimación de huella de carbono

SmartGlass es una aplicación web desarrollada con **Spring Boot** para vidrierías. El cliente compra vidrio de catálogo o diseña una pieza a medida y ve al instante su precio y su CO₂ estimado; el taller aprueba la cotización antes del pago. Como componente de Minería de Datos I, incluye además un modelo predictivo de CO₂ entrenado y evaluado aparte; la aplicación no lo consulta.

El proyecto combina desarrollo backend, persistencia híbrida y minería de datos para facilitar la toma de decisiones, mejorar la trazabilidad de los productos y promover prácticas de producción más sostenibles.

> **Estado:** prototipo funcional, listo para iniciar una fase piloto. Usa datos, precios y reseñas de demostración y todavía no lo opera una vidriería real. El CO₂ es una estimación con factores promedio, no una medición ni una certificación.

**¿Quieres practicar con el código?** Ve a [Practica con tu propia copia](#-practica-con-tu-propia-copia).

---

## 📑 Tabla de contenidos

- [Equipo de desarrollo](#-equipo-de-desarrollo)
- [Objetivos del proyecto](#-objetivos-del-proyecto)
- [Funcionalidades principales](#-funcionalidades-principales)
- [Arquitectura del sistema](#️-arquitectura-del-sistema)
- [Tecnologías utilizadas](#️-tecnologías-utilizadas)
- [Estructura del repositorio](#-estructura-del-repositorio)
- [Requisitos previos](#️-requisitos-previos)
- [Instalación y configuración](#-instalación-y-configuración)
- [Ejecución del proyecto](#️-ejecución-del-proyecto)
- [Practica con tu propia copia](#-practica-con-tu-propia-copia)
- [Componente de Minería de Datos I](#-componente-de-minería-de-datos-i)
- [Integración del modelo predictivo](#-integración-del-modelo-predictivo)
- [Alineación con los ODS](#-alineación-con-los-objetivos-de-desarrollo-sostenible)
- [Referencias técnicas](#-referencias-técnicas)
- [Licencia y uso académico](#️-licencia-y-uso-académico)
- [Contribuciones](#-contribuciones)

---

## 👥 Equipo de desarrollo

| Rol | Información |
|---|---|
| Integrantes | Javier Berrio, Argenis Caro y Miguel Martínez |
| Líder del proyecto de aula / Docente | Ing. Cristian Camilo Cuadrado Beltrán |
| Programa | Tecnología en Desarrollo de Software |
| Asignatura | Minería de Datos I |
| Aplicación publicada | [smartglass-production.up.railway.app](https://smartglass-production.up.railway.app) |
| Última actualización | Noviembre de 2026 |

---

## 🎯 Objetivos del proyecto

### Objetivo general

Desarrollar y evaluar una plataforma web para la venta de vidrio a medida que integre procesos comerciales, administración de información y estimación de la huella de carbono de cada pieza antes de fabricarla.

### Objetivos específicos

- Digitalizar el catálogo, el inventario, las cotizaciones y los pedidos.
- Implementar mecanismos seguros de autenticación y autorización.
- Organizar la información mediante una arquitectura de persistencia híbrida.
- Construir un conjunto de datos reproducible para el análisis de la huella de carbono.
- Entrenar y evaluar un modelo de aprendizaje automático para estimar las emisiones asociadas a piezas de vidrio.
- Mostrar alternativas de fabricación que permitan comparar el impacto ambiental al decidir la compra.

---

## ✨ Funcionalidades principales

### 🛒 Tienda
- Catálogo de 42 productos: 20 de vidrio plano, 7 esculturas y 15 figuras decorativas, con filtros por categoría y búsqueda.
- Las figuras usan ilustraciones rotuladas como tales; los precios y las reseñas son de demostración.
- Comparador de hasta tres productos, ofertas, control de existencias y carrito que no exige cuenta.
- Pedidos con factura y seguimiento. Los detalles conservan el precio de la compra mediante campos de tipo *snapshot*.

### 📐 Diseño a medida
- Configurador con precio, peso, resistencia y CO₂ que se actualizan al instante.
- Editor de corte: formas base, contorno dibujado a mano y puntos ajustables. El área real del contorno entra en el precio y la huella.
- Pintar y rellenar un diseño de color, que llega al taller como referencia y no cambia el precio.
- Vista 3D en vivo de la pieza, que se puede girar y acercar.
- Cotización que el taller aprueba o rechaza antes de que la pieza pase al carrito.

### 🌱 Gestión ambiental
- Huella de CO₂ por etapas (fusión, templado, laminado, capa activa y transporte), calculada con fórmulas deterministas (`AnalisisService`), no con el modelo predictivo.
- Opciones que cambian la estimación, como material reciclado y energía solar y eólica.
- Compromisos verdes, sello ambiental y ficha ambiental imprimible.

### 🔐 Seguridad y cuentas
- Usuarios, roles y autenticación con JSON Web Tokens (JWT) y Google OAuth2.
- Spring Security con denegación por defecto, protección CSRF y límites de solicitudes.
- Recuperación y cambio de contraseña con un código enviado por correo, si se configura un remitente.

### 🧭 Experiencia de uso
- Guía de compra y recorrido guiado automático, con opción de pausarlo o repetirlo.
- Español de Colombia, español de España e inglés.
- Tema claro y oscuro, colores para daltonismo y un modo sin animaciones ni destellos.

### 🧰 Panel de administración
- Gestión de productos, pedidos, cotizaciones, usuarios y reseñas.
- Planes Básico y Avanzado que activan o limitan funciones de la plataforma. Aún no tienen precio: se definirán con los resultados del piloto.
- Informe técnico con un globo 3D interactivo y las gráficas del modelo. El globo muestra el CO₂ fósil total por país en 2024 como contexto, no el del vidrio.

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
| `Pedido` | Registro y administración de órdenes de compra. |
| `DetallePedido` | Almacenamiento de los productos, cantidades y precios asociados a cada pedido. |

Los detalles de los pedidos utilizan campos *snapshot* para conservar información histórica relevante, evitando que los cambios posteriores en los catálogos alteren los datos de operaciones anteriores.

### 🍃 MongoDB — Persistencia documental

Se utiliza mediante **Spring Data MongoDB** para almacenar documentos flexibles, catálogos dinámicos y datos orientados a consultas específicas.

| Documento | Responsabilidad |
|---|---|
| `CatalogoProducto` | Productos de la tienda y sus especificaciones técnicas. |
| `VidrioSostenible` | Piezas a medida, cotizaciones, huella de carbono y diseño adjunto. |
| `Carrito` | Información temporal relacionada con las compras de los usuarios. |
| `Resena` | Opiniones y calificaciones de los productos. |
| `Notificacion` | Avisos de pedidos y cotizaciones. |
| `ConfiguracionPlataforma` | Plan activo y funciones de la plataforma. |

### 🔄 Flujo general de información

1. El usuario accede a la plataforma; la tienda y el configurador no exigen cuenta hasta el momento de pagar.
2. Los controladores reciben las solicitudes y las delegan a los servicios de negocio.
3. La información transaccional se gestiona mediante MySQL.
4. Los catálogos y documentos flexibles se administran mediante MongoDB.
5. El cotizador calcula peso, huella de CO₂, intensidad (kg CO₂e/m²), precio y resistencia de cada pieza con fórmulas deterministas en el backend (`AnalisisService`).
6. El taller revisa la cotización en el panel de administración; al aprobarla, la pieza puede pasar al carrito y al pago.
7. El modelo predictivo de minería de datos se entrena y evalúa aparte, en Python; la aplicación Java **no** lo consulta.

---

## 🛠️ Tecnologías utilizadas

| Categoría | Tecnologías |
|---|---|
| Lenguaje backend | Java |
| Framework | Spring Boot 4.1.1 |
| Motor de plantillas | Thymeleaf |
| Interfaz | JavaScript, CSS y Three.js para las vistas 3D |
| Seguridad | Spring Security, JWT y OAuth2 Client |
| Base de datos relacional | MySQL 8.x |
| Base de datos documental | MongoDB |
| Persistencia | Spring Data JPA, Hibernate y Spring Data MongoDB |
| Gestión de dependencias | Maven |
| Lenguaje de análisis | Python 3.13 |
| Procesamiento de datos | pandas y NumPy |
| Aprendizaje automático | scikit-learn |
| Visualización | matplotlib y seaborn |
| Entorno de análisis | Google Colab y Jupyter Notebook |
| Despliegue | Docker y Railway |
| Control de versiones | Git y GitHub |

**Compatibilidad de Java:** el proyecto compila para Java 26 (`java.version` en `backend/pom.xml`); se necesita un JDK 26 o posterior. Los pasos de este README se probaron con JDK 27.

---

## 📂 Estructura del repositorio

```text
Smartglass/
├── README.md
├── FLUJO-DE-TRABAJO.md             # Estado de cada ítem contra el código
├── DESPLIEGUE.md                   # Publicar en Render o Railway
├── Dockerfile, railway.json, render.yaml
├── requirements.txt                # Dependencias de Python
├── .env.example                    # Plantilla de variables de entorno (sin secretos)
│
├── backend/                        # API y lógica (Spring Boot): mvnw, pom.xml, src/
│  └── src/
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
│   │   └── resources/              # application*.properties, textos i18n (messages*) e informe técnico
│   │
│   └── test/                       # Pruebas automáticas
│
├── frontend/                       # Interfaz: el pom.xml la empaqueta en el mismo JAR
│   ├── templates/                  # Vistas Thymeleaf
│   └── static/                     # CSS, JavaScript, fuentes e imágenes
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
│   ├── metricas_modelo.csv         # Resultados de evaluación
│   └── exportar_informe.py         # JSON del informe técnico del panel admin
│
├── scripts/                        # Utilidades, como las ilustraciones de las figuras
├── video promocional/              # Video del proyecto
└── documentos/                     # Informe, propuesta del modelo, marca y pendón
```

---

## ⚙️ Requisitos previos

### Backend

- **JDK 26 o posterior.** Compruébalo con `java -version`.
- **MySQL Server 8.x** en ejecución, normalmente en el puerto `3306`.
- **MongoDB** en ejecución local o una instancia accesible mediante URI.
- **Git** para clonar el repositorio. Maven no hace falta: el repositorio incluye Maven Wrapper (`mvnw`).

### Minería de datos

- **Python 3.13** y `pip`.
- **Jupyter Notebook** o Google Colab para ejecutar el análisis.

---

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/OpTicXit/Smartglass.git
cd Smartglass
```

### 2. Crear la base de datos de MySQL

La aplicación crea las tablas, pero la base de datos debe existir. Con MySQL en ejecución:

```sql
CREATE DATABASE smartglass_db;
```

MongoDB no necesita preparación: por defecto se conecta a `mongodb://localhost:27017/test`.

### 3. Crear el archivo `.env`

Copia la plantilla en la raíz del repositorio. Spring Boot lee `.env` al arrancar, y Git lo ignora.

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

**Linux o macOS:**

```bash
cp .env.example .env
```

Después abre `.env` y ajusta estas variables:

| Variable | Obligatoria | Uso |
|---|---|---|
| `MYSQL_PASSWORD` | Sí | Contraseña de tu MySQL |
| `JWT_SECRET` | Sí | Clave para firmar los JWT, de al menos 32 bytes; debe ser **fija** mientras la app esté en uso |
| `GOOGLE_CLIENT_ID` | Sí | Cliente OAuth2 de Google; para practicar sirve el valor de la plantilla |
| `GOOGLE_CLIENT_SECRET` | Sí | Secreto de ese cliente; para practicar sirve el valor de la plantilla |
| `MYSQL_USER` | No (por defecto `root`) | Usuario de MySQL |
| `ADMIN_PASSWORD` | Solo la primera vez | Crea el administrador inicial (usuario `admin`); si falta, no se crea |
| `DEMO_DATOS` | No | Con `true`, carga clientes, cotizaciones y pedidos de ejemplo una sola vez, en una instalación nueva |
| `DEMO_PASSWORD` | No | Contraseña de los clientes de ejemplo; sin ella nadie puede entrar con esas cuentas |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | No | Remitente de los códigos de recuperación; sin ellos, el inicio de sesión no ofrece esa opción |

Las variables de despliegue de la plantilla van comentadas con `#`; déjalas así en local, porque una línea vacía como `MYSQL_URL=` impide arrancar. Sin una variable obligatoria, la aplicación tampoco arranca.

Para generar un `JWT_SECRET` propio:

**Windows PowerShell:**

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

**Linux o macOS:**

```bash
openssl rand -base64 48
```

**Importante:** no subas secretos reales a GitHub ni los incluyas en `application.properties`.

### 4. Instalar las dependencias de Python

Solo hacen falta para la parte de minería de datos. Desde la raíz del repositorio, crea un entorno virtual y actívalo:

**Windows PowerShell:**

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

**Linux o macOS:**

```bash
python3 -m venv .venv
source .venv/bin/activate
```

Después instala las dependencias y Jupyter:

```bash
python -m pip install -r requirements.txt notebook
```

---

## ▶️ Ejecución del proyecto

> Para publicar la aplicación en Render o Railway (Docker, perfil `prod`, MongoDB Atlas y MySQL gestionado), sigue [DESPLIEGUE.md](DESPLIEGUE.md).

### 1. Iniciar la aplicación

Ejecútala desde la carpeta `backend/`.

**Windows PowerShell:**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Linux o macOS:**

```bash
cd backend
./mvnw spring-boot:run
```

La primera vez tarda más porque descarga las dependencias. Al arrancar con las bases vacías, carga el catálogo y las reseñas de demostración. Cuando termine, abre:

**http://localhost:50500**

### 2. Ejecutar las pruebas

Desde `backend/`, con MySQL y MongoDB en ejecución:

**Windows PowerShell:**

```powershell
.\mvnw.cmd test
```

**Linux o macOS:**

```bash
./mvnw test
```

Una de las pruebas arranca la aplicación completa, así que usa las bases de datos configuradas en `.env` y puede cargar en ellas los datos iniciales.

### 3. Generar el dataset

Desde la raíz del repositorio:

```bash
python data/generacion_dataset.py
```

El script genera 10.000 registros con la semilla fija `42` en `data/raw/smartglass_co2.csv`. Con la misma semilla y los mismos factores, el resultado es idéntico.

### 4. Ejecutar el análisis predictivo

Abre `mineria de datos/analisis.ipynb` en Jupyter Notebook o Google Colab y ejecuta las celdas en orden:

```bash
jupyter notebook "mineria de datos/analisis.ipynb"
```

El notebook busca el CSV junto a sí mismo (en Colab, subiéndolo) o en `../data/raw/` (desde el repositorio). Al terminar, reescribe `modelo_smartglass_co2.pkl` y `metricas_modelo.csv`.

---

## 🧪 Practica con tu propia copia

Puedes copiar este repositorio para aprender y hacer pruebas sin afectar el original. Todo lo que hagas queda en tu copia.

### 1. Haz tu copia

- **Con GitHub:** pulsa **Fork** en [el repositorio](https://github.com/OpTicXit/Smartglass) y clona tu copia con tu usuario.
- **Sin cuenta de GitHub:** clónalo directamente, como en la [instalación](#-instalación-y-configuración). También puedes descargarlo con **Code > Download ZIP**.

Luego trabaja en una rama propia:

```bash
git switch -c mi-practica
```

### 2. Usa valores de práctica en `.env`

Tras seguir la [instalación](#-instalación-y-configuración), esta configuración basta para practicar. Google queda con los valores de la plantilla y el correo queda vacío:

```dotenv
MYSQL_PASSWORD=la_contraseña_de_tu_mysql_local
JWT_SECRET=un_valor_generado_con_el_comando_de_la_instalacion
ADMIN_PASSWORD=una_contraseña_solo_para_practicar
DEMO_DATOS=true
DEMO_PASSWORD=otra_contraseña_solo_para_practicar
```

Usa contraseñas inventadas para esta práctica, nunca las de tus cuentas reales.

### 3. Entra a la aplicación

| Cuenta | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | La de `ADMIN_PASSWORD` |
| Cliente de ejemplo | `laura.gomez` | La de `DEMO_PASSWORD` |

El inicio de sesión pide el usuario, no el correo. Si `ADMIN_PASSWORD` estaba vacía la primera vez, agrégala y reinicia: el administrador se crea solo si aún no existe.

Con esta configuración funcionan la tienda, el configurador, las cuentas locales y el panel de administración. **No** funcionan el inicio de sesión con Google ni la recuperación de contraseña, porque necesitan credenciales propias.

### 4. Ideas para practicar

**Desarrollo web:**

- Agrega un producto en `CatalogoFigurasSeeder` y mira cómo aparece al reiniciar.
- Cambia un texto en `backend/src/main/resources/messages.properties` y su versión en `messages_en.properties`.
- Modifica un factor de CO₂ en `AnalisisService` y en `frontend/static/js/glass-preview.js`, que hace el mismo cálculo en pantalla, y comprueba cómo cambia la cifra en el configurador.
- Escribe una prueba nueva en `backend/src/test` y ejecútala.

**Minería de datos:**

- Cambia `n` o un multiplicador en `data/generacion_dataset.py` y vuelve a ejecutar el notebook.
- Compara Random Forest con otro algoritmo de regresión, usando MAE, RMSE y R².
- Agrega `masa_kg` a las variables y observa por qué sube la métrica: es el efecto de la filtración descrita en [el componente de minería](#-componente-de-minería-de-datos-i).
- Si reentrenas y quieres ver los resultados en el panel, ejecuta `mineria de datos/exportar_informe.py`, que necesita conexión a internet.

### 5. Si algo falla

| Mensaje | Causa probable | Solución |
|---|---|---|
| `UnsupportedClassVersionError` o error de versión de Java | JDK anterior a 26 | Instala un JDK 26 o posterior y apunta `JAVA_HOME` a él |
| `Could not resolve placeholder` | Falta `.env` o una variable obligatoria | Revisa el paso 3 de la instalación y arranca desde `backend/` |
| `Failed to configure a DataSource` | `MYSQL_URL` quedó definida y vacía en `.env` | Coméntala con `#` o bórrala |
| `Unknown database 'smartglass_db'` | No se creó la base | Ejecuta `CREATE DATABASE smartglass_db;` |
| `Access denied for user` | Credenciales de MySQL incorrectas | Revisa `MYSQL_USER` y `MYSQL_PASSWORD` |
| No entra `admin` | `ADMIN_PASSWORD` estaba vacía al arrancar | Defínela y reinicia |
| El puerto `50500` está ocupado | Otra aplicación lo usa | Define `PORT=50501` en `.env` y usa ese puerto |

---

## 🧠 Componente de Minería de Datos I

### Problema analítico

¿Es posible estimar los kilogramos de CO₂ asociados a una pieza de vidrio personalizada a partir de sus parámetros técnicos, antes de fabricarla?

El proyecto aborda esta pregunta mediante un modelo de regresión que utiliza características relacionadas con el producto y su proceso de fabricación.

### Dataset

| Característica | Descripción |
|---|---|
| Registros | 10.000 piezas de vidrio |
| Columnas | 8: `tipo_vidrio`, `proceso`, `area_m2`, `espesor_mm`, `calcin_pct`, `masa_kg`, `energia_kwh_kg`, `co2_kg` |
| Catálogo | 20 tipos de vidrio (propios del dataset; el cotizador web ofrece 7) |
| Variable objetivo | `co2_kg` |
| Tipo de problema | Regresión supervisada |
| Semilla de generación | `42` |
| Fuente | Datos sintéticos derivados de factores de emisión documentados y de multiplicadores por tipo definidos por el equipo |

El dataset **no** proviene de una planta ni de una vidriería. Contiene 0 valores nulos y 2 filas repetidas (0,02 %), que se conservan porque dos piezas pueden coincidir. El generador sortea el tipo de vidrio y el proceso de forma independiente, así que algunas combinaciones son poco realistas.

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

El dataset modela sus propios factores de emisión (multiplicador por tipo, energía del proceso, factor de red de 0,220 kg CO₂/kWh de UPME para 2024, calcín de 10 a 80 % con media cercana al 25 %). **No** reproduce las fórmulas del cotizador web (`AnalisisService`), que usa 7 tipos y factores distintos. Por eso el modelo es un estudio independiente, no una validación del cotizador.

### Patrones encontrados

| Patrón | Evidencia en el dataset |
|---|---|
| El peso domina el CO₂ total | Correlación de 0,905 entre `masa_kg` y `co2_kg` |
| El tipo de vidrio cambia la intensidad | De 0,99 kg de CO₂ por kg en el reciclado a 3,10 en el inteligente, frente a 1,40 del float |
| El calcín reduce la huella | Sin el tipo reciclado, de 1,81 kg de CO₂ por kg con 10 % de calcín a 1,55 con 40 % (14,3 % menos) |

Como el generador se construye con estas relaciones, los patrones comprueban que el método las recupera; no prueban que ocurran igual en una vidriería real.

### Modelo predictivo

El algoritmo seleccionado es **Random Forest Regressor**, un método de aprendizaje automático que combina múltiples árboles de decisión para realizar predicciones numéricas.

| Parámetro | Configuración documentada |
|---|---|
| Algoritmo | Random Forest Regressor |
| Número de árboles | 200 |
| Variables predictoras | 6 (sin `masa_kg`) |
| Preprocesamiento categórico | One-Hot Encoding |
| Datos de entrenamiento | 80 % (8.000 registros) |
| Datos de prueba | 20 % (2.000 registros) |
| Partición | Aleatoria, semilla `42` (la misma de `preparacion.dividir`) |
| Validación cruzada | 5 particiones (*5-fold*) |

### Métricas de evaluación

Resultados en los 2.000 registros de prueba:

| Modelo | MAE (kg de CO₂) | RMSE (kg de CO₂) | R² |
|---|---:|---:|---:|
| Media (siempre predice el promedio) | 99,87 | 129,99 | -0,0004 |
| Regresión lineal | 31,84 | 45,09 | 0,880 |
| **Random Forest (200 árboles)** | **13,52** | **25,56** | **0,9613** |

El R² de validación cruzada es de 0,9615 ± 0,0036. En entrenamiento, el modelo obtiene R² 0,9947 y MAE de 4,86 kg: hay algo de sobreajuste, aunque la validación cruzada es estable.

**Consideración metodológica:** el R² de 0,9613 no significa que acierte el 96 % de las veces: explica esa parte de la variación dentro de datos sintéticos. El modelo también falla más en los tipos con pocos registros; por ejemplo, fotovoltaico promedia 314 kg reales y 215 predichos. La precisión con piezas reales se medirá en la fase piloto.

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

**El modelo no está integrado en la aplicación.** `modelo_smartglass_co2.pkl` es un artefacto entrenado y evaluado aparte, versionado en el repositorio. Ningún componente Java lo carga ni lo consulta (el informe técnico del panel admin solo muestra sus resultados, ya exportados): las cifras de CO₂ que muestra la web salen de las fórmulas deterministas de `AnalisisService`. La presencia del archivo `.pkl` no implica integración.

Además, hoy no se podría conectar directamente: el modelo necesita `proceso`, `energia_kwh_kg` y `calcin_pct`, que el formulario de cotización no pide, y conoce 20 tipos de vidrio frente a los 7 del cotizador.

### Siguiente fase (no implementada)

La decisión actual es no usar el modelo para cotizar hasta validarlo con datos reales del piloto. Una integración futura permitiría:

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
- Unidad de Planeación Minero Energética (UPME). (2024). [*Factores de emisión del Sistema Interconectado Nacional*](https://docs.upme.gov.co/SIMEC/Cambio-Climatico/Documents/Calculo-FE-del-SIN/Factores_de_emision_SIN_2024.pdf).
- Guardian Glass. *How glass is made* y declaraciones ambientales de producto.
- Glass for Europe. Datos del contenido de calcín en el vidrio plano.
- Our World in Data. [*CO₂ and greenhouse gas emissions*](https://github.com/owid/co2-data). Usado solo como contexto en el globo del informe técnico.
- Colombia. Ley 2169 de 2021. Medidas para impulsar el desarrollo bajo en carbono del país y establecer metas y medidas mínimas en materia de carbono-neutralidad y resiliencia climática.

---

## ⚖️ Licencia y uso académico

SmartGlass es un proyecto desarrollado con fines académicos.

Su propósito es aplicar conocimientos de ingeniería de software, persistencia de datos y aprendizaje automático a un problema relacionado con la sostenibilidad en la industria del vidrio.

Puedes clonar, ejecutar y modificar este repositorio para aprender y practicar. No se especifica una licencia de software abierta; para otros usos, consulta al equipo.

---

## 🤝 Contribuciones

Para contribuir al proyecto:

1. Crea un *fork* del repositorio y clónalo.
2. Crea una rama para tus cambios.
3. Implementa y prueba las modificaciones.
4. Registra los cambios mediante commits descriptivos.
5. Envía un *pull request* para su revisión.

Repositorio: [https://github.com/OpTicXit/Smartglass](https://github.com/OpTicXit/Smartglass)

---

**SmartGlass — Tecnología, industria y sostenibilidad para el futuro del vidrio.**
