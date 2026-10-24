# Fotos pendientes del catálogo (T2)

El código ya espera estos archivos en `src/main/resources/static/img/`. Mientras no existan, cada producto muestra el isotipo (`logo-icono.png`, respaldo F18) con la etiqueta «Foto en preparación»; nunca aparece una imagen rota.

**Cómo activarlas:** copia el archivo con el nombre exacto y reinicia la app. `CatalogoEsculturasSeeder` comprueba qué archivos existen y solo entonces llena `imagenPrincipal` y `galeria` del producto. No hay que tocar código ni base de datos, y no cambia ninguna clave (código, tipo ni los `value` del configurador, F40).

**Formato recomendado:** JPG o WebP, 1600 × 1200 px (4:3) o más, fondo neutro u oscuro, sin texto ni marcas de agua. Los nombres van en minúsculas.

El logotipo y el isotipo no forman parte de esta lista y no se tocan.

## Esculturas (7 piezas nuevas)

Foto principal más dos vistas para la galería del detalle (opcionales).

| Código | Pieza | Tamaño | Foto principal | Galería (opcional) |
|---|---|---|---|---|
| ESC-G-01 | Escultura Ola Templada | Grande | `esc-g-01.jpg` | `esc-g-01-2.jpg`, `esc-g-01-3.jpg` |
| ESC-G-02 | Monolito de Vidrio Fundido | Grande | `esc-g-02.jpg` | `esc-g-02-2.jpg`, `esc-g-02-3.jpg` |
| ESC-M-01 | Torso Facetado | Mediana | `esc-m-01.jpg` | `esc-m-01-2.jpg`, `esc-m-01-3.jpg` |
| ESC-M-02 | Espiral Reciclada | Mediana | `esc-m-02.jpg` | `esc-m-02-2.jpg`, `esc-m-02-3.jpg` |
| ESC-P-01 | Gota de Cristal | Pequeña | `esc-p-01.jpg` | `esc-p-01-2.jpg`, `esc-p-01-3.jpg` |
| ESC-P-02 | Prisma de Mesa | Pequeña | `esc-p-02.jpg` | `esc-p-02-2.jpg`, `esc-p-02-3.jpg` |
| ESC-P-03 | Colibrí Soplado | Pequeña | `esc-p-03.jpg` | `esc-p-03-2.jpg`, `esc-p-03-3.jpg` |

## Fotos realistas de los tipos de vidrio (20 productos existentes)

Reemplazan visualmente a `vidrio1.jpg` … `vidrio20.jpg`; si el archivo nuevo no existe, se sigue viendo la imagen actual.

| Código | Producto | Archivo esperado |
|---|---|---|
| VID-001 | Vidrio Templado Craquelado | `vidrio-templado-craquelado-real.jpg` |
| VID-002 | Vidrio Flotado Transparente | `vidrio-flotado-transparente-real.jpg` |
| VID-003 | Vidrio Impreso Catedral | `vidrio-impreso-catedral-real.jpg` |
| VID-004 | Doble Vidriado Hermético (DVH) | `vidrio-dvh-real.jpg` |
| VID-005 | Vidrio Curvo | `vidrio-curvo-real.jpg` |
| VID-006 | Vidrio Laminado de Seguridad | `vidrio-laminado-seguridad-real.jpg` |
| VID-007 | Vidrio Laminado Opal | `vidrio-laminado-opal-real.jpg` |
| VID-008 | Vitral Decorativo Moderno | `vitral-decorativo-real.jpg` |
| VID-009 | Vidrio Extraclaro para Vitrinas | `vidrio-extraclaro-real.jpg` |
| VID-010 | Espejo Plano | `espejo-plano-real.jpg` |
| VID-011 | Vidrios Texturizados de Colores | `vidrio-texturizado-colores-real.jpg` |
| VID-012 | Doble Acristalamiento DVH | `vidrio-doble-acristalamiento-real.jpg` |
| VID-013 | Vidrio Inteligente Electrocrómico | `vidrio-inteligente-real.jpg` |
| VID-014 | Vidrio de Seguridad Antirrobo | `vidrio-antirrobo-real.jpg` |
| VID-015 | Vidrio Hidrofóbico Autolimpiante | `vidrio-autolimpiante-real.jpg` |
| VID-016 | Vidrio Lacado Opaco | `vidrio-lacado-real.jpg` |
| VID-017 | Vidrio Curvo Gran Formato | `vidrio-curvo-gran-formato-real.jpg` |
| VID-018 | Mampara Divisoria Tintada | `mampara-tintada-real.jpg` |
| VID-019 | Vidrio Reflectivo Control Solar | `vidrio-control-solar-real.jpg` |
| VID-020 | Espejo Solar Reflectivo | `espejo-solar-real.jpg` |

En total: 7 fotos principales de esculturas (más 14 opcionales de galería) y 20 fotos realistas de vidrio plano.
