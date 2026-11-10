# Revisión de lógica de SmartGlass · 10 de noviembre de 2026

Revisión del backend (unas 10.000 líneas de Java) y de la lógica del frontend, buscando errores de lógica, con cada hipótesis comprobada en la app local antes de corregir. No contiene contraseñas ni datos personales.

## 1. Errores corregidos (sin commit)

| # | Error | Cómo se comprobó | Corrección |
|---|---|---|---|
| 1 | **Pedidos duplicados.** Si el formulario de pago llegaba dos o tres veces a la vez (doble clic, reintento del navegador), cada envío creaba un pedido y descontaba existencias. | Tres envíos simultáneos con un producto en el carrito: 3 pedidos y existencias de 37 a 34. | Un candado por comprador en `CheckoutController`: el segundo envío encuentra el carrito vacío y va a la confirmación del primero. Ahora: 1 pedido y existencias de 34 a 33. |
| 2 | **Cancelar un pedido no devolvía las existencias.** El inventario bajaba para siempre aunque el pedido se cancelara. | Lectura del código y prueba. | `PedidoService.actualizarEstados`: cancelar devuelve las unidades; reactivar un cancelado las vuelve a apartar y, si ya no hay, el pedido sigue cancelado y el panel lo avisa. Prueba: 33 → 34 al cancelar → 33 al reactivar. |
| 3 | **Compensación de CO₂ cobrada dos veces.** Una obra cotizada con compensación (+$5.000 en su precio) podía pagar otros $5.000 en el checkout. | Lectura del código. | Si todo el carrito son obras ya compensadas, el checkout no ofrece ni cobra la casilla y el pedido cuenta como compensado. Prueba: $180.709 + $25.000 de envío = $205.709, aun enviando la casilla marcada a mano. |
| 4 | **Fechas 5 horas adelantadas en Railway.** El contenedor corre en UTC y las fechas se crean con la hora del servidor: pedidos, cotizaciones, reseñas y la gráfica diaria. Desde las 7 p. m. de Colombia, todo quedaba con fecha de mañana. | Lectura del código y del `Dockerfile` (no fija zona horaria). No se pudo observar en Railway sin entrar con una cuenta. | La app fija `America/Bogota` al arrancar (`SmartglassApplication`). |
| 5 | **El panel podía quedarse sin administrador.** Se le podía quitar el rol al único admin. | Prueba en local. | `AdminController.cambiarRol` lo impide y avisa: «No se puede quitar el rol al único administrador». |
| 6 | **El resumen del panel sumaba pedidos cancelados** en el CO₂ estimado (Finanzas sí los excluía). | Lectura del código. | `obtenerEstadisticasDashboard` excluye los cancelados, igual que `FinanzasService`. |
| 7 | **Reseñas huérfanas.** Se podía publicar una reseña para un producto que no existe. | Petición directa: se aceptaba. | `TiendaController` comprueba que el producto exista. |

**Pruebas:** pasan 55 de 55 (5 nuevas en `PedidoServiceTest`: cancelar, reactivar sin existencias, cambio normal sin tocar inventario y los dos casos de compensación).

## 2. Lo que se revisó y está correcto

- **Permisos por dueño:** pedidos, facturas, cotizaciones y fichas solo los ve su dueño o un admin; la cotización de un visitante, solo su misma sesión.
- **Existencias:** el descuento es atómico (`findAndModify` con `stock >= cantidad`); dos compras a la vez no dejan el inventario en negativo, y si el pedido falla se devuelven las unidades.
- **Máquina de estados de cotizaciones:** `COTIZACION → APROBADO | RECHAZADA` y `RECHAZADA → APROBADO`; `APROBADO` es final. Solo el dueño lleva al carrito una obra aprobada.
- **Validación de entradas manipuladas:** medidas, NaN, textos largos, PNG del diseño, cantidades (tope 999), método de pago, dirección, calificación de 1 a 5.
- **Cálculo:** la fórmula de precio del configurador (JavaScript) coincide con la del servidor (`AnalisisService`), y la huella por etapas tiene pruebas que comparan Java y Node.
- **Seguridad** (revisada el 9 de noviembre): CSRF, CSP con nonce, límite de solicitudes por IP real, cierre de sesión por POST.

## 3. Hallazgos que necesitan una decisión (no se tocaron)

1. **La compensación no suma exactamente $5.000.** En `AnalisisService` y en el configurador se suma *antes* del descuento por reciclado (−5 %) y del recargo por urgencia (+20 %). Con reciclado suma $4.750; con urgencia, $6.000. La pantalla dice «Suma 5.000 COP». Corregirlo es mover una línea en Java y otra en JavaScript, y no cambia los casos A–D (todos sin compensar). Pero es parte del contrato de cálculo que el equipo pidió no tocar.
2. **El carrito de un usuario con cuenta guarda el precio del día en que agregó el producto.** Si una oferta vence o el admin cambia el precio, ese carrito paga el precio viejo. El carrito de un visitante sí recalcula. Opción: revalidar precios al entrar al checkout y avisar si cambiaron.
3. **Cambiar el rol o la contraseña no cierra las sesiones abiertas.** Un admin degradado sigue entrando al panel hasta que cierre sesión.
4. **Los estados del pedido no tienen orden.** Un pedido «Entregado» puede volver a «Pendiente». Solo se validan los nombres.
5. **Las fechas viejas de pedidos en Railway** quedan como se guardaron (en UTC); las nuevas salen bien.
6. **Crear producto** acepta precio 0 o nombre vacío si se manipula el formulario (solo admin).

## 4. Mejoras de mayor impacto

| Mejora | Por qué |
|---|---|
| Mostrar la regla de envío antes del checkout («gratis desde $500.000; si no, $25.000») | Hoy el cliente solo la descubre al pagar; es de las causas más comunes de abandono de carrito. |
| Revalidar precios y existencias al entrar al checkout | Cierra el hallazgo 2 y evita sorpresas en la factura. |
| Correo al cliente cuando cambia el estado del pedido o se aprueba su cotización | Ya existe el envío por Brevo; hoy solo hay avisos dentro de la app. |
| Línea de tiempo del pedido para el cliente (pendiente → procesando → enviado → entregado) | Aprovecha los estados que ya existen y reduce consultas a la vidriería. |
| Pruebas de flujo automáticas en cada push (GitHub Actions) | Los flujos que se probaron a mano (compra, cotización, panel) quedarían protegidos contra regresiones. |
| Registro de acciones del admin (quién cambió qué estado, rol o precio) | Necesario en un piloto con una vidriería real. |
| Consultas por rango de fechas en lugar de leer todo (`findAll`) | Con miles de pedidos, el resumen, Finanzas y la actividad se vuelven lentos. |
| Pasarela de pago en modo de pruebas (Wompi o PayU) | Hoy el pago es simulado; con sandbox el piloto probaría el flujo completo sin dinero real. |

## 5. Verificación

- 55 pruebas automáticas.
- Prueba real en local con una cuenta de prueba: triple envío del checkout, cancelar y reactivar mirando las existencias, checkout de una obra ya compensada, y el intento de quitar el rol al único admin.
- No se probó en Railway: los cambios no están desplegados.
