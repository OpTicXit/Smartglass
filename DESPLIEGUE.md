# Despliegue de SmartGlass (Render o Railway)

Guía para publicar la aplicación con MongoDB Atlas y un MySQL gestionado. La imagen se construye con el `Dockerfile` de la raíz y arranca con el perfil `prod` (`application-prod.properties`).

## Qué plataforma elegir

| | Railway (recomendado) | Render |
|---|---|---|
| MySQL gestionado | Sí, dentro del mismo proyecto | No (solo PostgreSQL): hay que usar un MySQL externo, por ejemplo el de Railway o Aiven |
| Disco para las fotos subidas | Volumen persistente | Disco persistente, solo en planes de pago |
| Archivo de configuración | `railway.json` | `render.yaml` (Blueprint) |
| Chequeo de salud | `/salud` | `/salud` |

En los dos casos MongoDB va en **MongoDB Atlas** (el plan M0 gratuito alcanza para empezar).

## Antes de desplegar

1. **Secretos del historial (H01, revisado el 25/10/2026).** En los commits antiguos de `application.properties` que ya están en GitHub quedaron dos valores: la clave del MySQL local y un `JWT_SECRET` antiguo. El secreto de Google, la clave del admin y la URI de Atlas nunca se publicaron. El historial no se reescribe; en su lugar, producción usa valores nuevos: Render genera su propio `JWT_SECRET`, el MySQL gestionado tiene su propia clave y ninguno de los dos valores viejos se reutiliza en ningún sitio.
2. **MongoDB Atlas.** Crea un clúster, un usuario de base de datos y permite el acceso de red de la plataforma (o `0.0.0.0/0` mientras pruebas). Copia la cadena `mongodb+srv://…` y **añádele el nombre de la base**, por ejemplo `/smartglass`.
3. **Google Cloud (login con Google).** En la credencial OAuth, añade:
   - URI de redirección autorizada: `https://<tu-dominio>/login/oauth2/code/google`
   - Origen de JavaScript autorizado: `https://<tu-dominio>`

   La app construye esa URL sola a partir del dominio con el que se entra (`{baseUrl}`), así que en local sigue funcionando `http://localhost:50500/login/oauth2/code/google`.

## Variables de entorno

| Variable | Obligatoria | Valor |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sí | `prod` (el `Dockerfile` ya la pone) |
| `MYSQL_URL` | Sí | `jdbc:mysql://HOST:PUERTO/BASE?sslMode=REQUIRED&serverTimezone=UTC` (en la red privada de Railway: `?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`) |
| `MYSQL_USER` / `MYSQL_PASSWORD` | Sí | Usuario y clave del MySQL gestionado |
| `MONGODB_URI` | Sí | Cadena de Atlas **con el nombre de la base** |
| `JWT_SECRET` | Sí | 32 caracteres o más, fijo (si cambia, las sesiones JWT emitidas dejan de valer) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Sí | Credencial OAuth de Google |
| `ADMIN_PASSWORD` | Solo la primera vez | Crea el usuario `admin` si la base está vacía |
| `UPLOADS_DIR` | Sí | `/data/uploads` (el `Dockerfile` ya la pone; el volumen se monta en `/data`) |
| `PORT` | No | Lo asigna la plataforma |
| `DB_POOL` | No | Conexiones a MySQL (por defecto 5) |

## Railway, paso a paso

1. **New Project → Deploy from GitHub repo** y elige el repositorio. Railway detecta `railway.json` y construye con el `Dockerfile`.
2. **+ New → Database → MySQL** en el mismo proyecto.
3. En el servicio de la app, **Variables**, usa referencias a la base:
   - `MYSQL_URL` = `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `MYSQL_USER` = `${{MySQL.MYSQLUSER}}`
   - `MYSQL_PASSWORD` = `${{MySQL.MYSQLPASSWORD}}`
   - Las demás de la tabla anterior.
4. **Volume:** añade un volumen al servicio de la app con punto de montaje `/data`. Ahí quedan las fotos que sube el admin.
5. **Settings → Networking → Generate Domain** y registra ese dominio en Google Cloud (ver arriba).
6. Despliega. El chequeo `/salud` debe responder `ok`.

## Render, paso a paso

1. **New → Blueprint** y elige el repositorio: Render lee `render.yaml` (servicio en la región `virginia`, junto a Atlas en AWS us-east-1; la región no se puede cambiar después).
2. Escribe en el panel los valores marcados `sync: false` (MySQL externo, Atlas, Google, `ADMIN_PASSWORD`). `JWT_SECRET` lo genera Render.
3. El disco `fotos` se monta en `/data` y requiere un plan de pago. Sin disco, las fotos subidas se pierden en cada despliegue.
4. Registra el dominio `https://<servicio>.onrender.com` en Google Cloud. Si el nombre ya estaba ocupado, Render le añade un sufijo (el piloto quedó en `https://smartglass-g075.onrender.com`).

### Despliegue del piloto (25/10/2026)

- **MySQL en Aiven** (plan gratuito, MySQL 8.4): `MYSQL_URL=jdbc:mysql://<host>.aivencloud.com:<puerto>/defaultdb?sslMode=REQUIRED&serverTimezone=UTC`, usuario `avnadmin`. Aiven exige clave primaria en todas las tablas; las de SmartGlass la tienen.
- **MongoDB Atlas M0** en AWS us-east-1, con acceso de red `0.0.0.0/0` (la protección es la contraseña del usuario de la base).
- **Google OAuth**: credencial nueva en un proyecto propio («SmartGlass»), con `http://localhost:50500` y el dominio de Render como orígenes y sus `/login/oauth2/code/google` como URI de redirección; app publicada (solo pide `profile` y `email`, no necesita verificación).
- **Errores que salieron y su causa** (todos en las variables, no en el código):

  | Mensaje en el log | Causa | Arreglo |
  |---|---|---|
  | `'url' must start with "jdbc"` | `MYSQL_URL` pegada sin el prefijo (`mysql://…`) | Empezar por `jdbc:mysql://` |
  | `Database name must not be empty` | `MONGODB_URI` sin el nombre de la base (`….mongodb.net/?…`) | Añadir `/smartglass` antes del `?` |
  | `redirect_uri_mismatch` (Google) | Dominio sin registrar en la credencial | Origen y URI de redirección del dominio de Render |

### Datos de demostración

Con `DEMO_DATOS=true` la app carga, una sola vez y después de los seeders del catálogo, un conjunto coherente para enseñar el piloto (`DemoDatosSeeder`): 11 clientes de varias ciudades de Colombia (uno en inglés y dos con accesibilidad), 28 cotizaciones repartidas en diez semanas (17 aprobadas, 4 rechazadas, 7 pendientes, con su CO₂ por etapas), 20 pedidos con sus facturas y estados (entregados, enviados, en proceso, pendientes y uno cancelado), stock descontado, 9 opiniones de compradores reales, 3 carritos abiertos y las notificaciones que habría generado cada paso.

1. En Render, **Environment → Add** `DEMO_DATOS` = `true` → **Save and deploy**.
2. El log muestra `Datos de demostracion cargados: 11 clientes, 28 cotizaciones, 20 pedidos…`.
3. Quita la variable (o ponla en `false`). Aunque se quede, un segundo arranque no duplica nada: si el primer cliente ya existe, no hace nada.

Los clientes usan correos `@example.com` (dominio reservado) y no tienen contraseña conocida. Si quieres entrar con uno para la demostración, define también `DEMO_PASSWORD` antes de la carga.

## Llevar los datos de local a producción

- **MySQL** (usuarios y pedidos): `mysqldump smartglass_db > smartglass.sql` y luego importarlo en la base gestionada.
- **MongoDB** (catálogo, carritos, cotizaciones, notificaciones y plan SaaS). En local los datos están en la base **`test`** (ver AUD-12 abajo). Para pasarlos a Atlas con el nombre `smartglass`:

  ```bash
  mongodump --db test --out ./volcado
  mongorestore --uri "<MONGODB_URI sin el nombre de base>" --nsFrom "test.*" --nsTo "smartglass.*" ./volcado
  ```
- **Fotos subidas**: la carpeta local `uploads/img` (hoy vacía) se copia al volumen `/data/uploads/img`.

Si arrancas con bases vacías, la app crea las tablas, los índices, el catálogo inicial y, con `ADMIN_PASSWORD`, el usuario `admin`.

## Comprobaciones después de desplegar

1. `https://<dominio>/salud` responde `ok`.
2. Inicio, catálogo y configurador cargan con estilos (las URL de CSS y JS llevan un hash).
3. Login con usuario y contraseña, y login con Google (vuelve a `https://<dominio>/…`).
4. Cotizar una obra, aprobarla desde el admin, añadirla al carrito y pagar.
5. Subir una foto de producto, volver a desplegar y comprobar que sigue ahí (volumen).
6. Con el admin, `/admin/planes` (los planes SaaS; cualquier admin los edita) y, sin sesión, la página pública `/planes`.

## Qué cambia el perfil `prod`

- Cookie de sesión `Secure` (solo HTTPS), `HttpOnly` y `SameSite=Lax`; HSTS lo añade Spring Security.
- `server.forward-headers-strategy=native`: detrás del proxy de la plataforma la app ve `https`, el dominio real y la IP del cliente (login con Google y límite por IP).
- Errores sin mensaje ni traza para el visitante.
- Sin SQL en consola y logs de seguridad en `WARN`.
- Caché de 7 días para estáticos, con versión por contenido en CSS y JS.
- Pool de MySQL de 5 conexiones (`DB_POOL`).

## Notas conocidas

- **AUD-12 (base `test` de Mongo), resuelto.** Spring Boot 4 ignora `spring.data.mongodb.uri` (obsoleta con nivel *error* desde la 4.0), por eso la app usaba su valor por defecto `mongodb://localhost/test`. Ahora la propiedad es `spring.mongodb.uri`. En local se mantiene `test` a propósito para no mover los datos existentes; en producción manda `MONGODB_URI`.
- **Una sola instancia.** La sesión y algunas cachés viven en memoria. No escales a varias réplicas sin una sesión compartida.
- **Servicio externo de resistencia.** El detalle de la obra consulta `api-production-d556.up.railway.app`, que hoy rechaza peticiones de otros dominios (CORS). La página lo maneja mostrando "Referencia externa no disponible"; para mostrar la referencia, ese servicio debe permitir el dominio nuevo.
- **Imagen sin probar localmente.** En la máquina de desarrollo no hay Docker; se probó el JAR con el perfil `prod`, que es lo que ejecuta la imagen. La primera compilación en la plataforma es la prueba del `Dockerfile`.
