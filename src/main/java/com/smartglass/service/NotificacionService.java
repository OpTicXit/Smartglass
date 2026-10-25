package com.smartglass.service;

import com.smartglass.config.UsuarioSesion;
import com.smartglass.controller.PermisosAdvice;
import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.Notificacion;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.DetallePedidoRepository;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * T3 · notificaciones por POLLING (GET /api/notificaciones), sin WebSocket.
 *
 * Publicar nunca rompe el flujo que la origina (compra, cotizacion, admin): un
 * fallo de Mongo se registra y sigue. Leer resuelve los textos en el idioma de
 * quien pregunta (T0) y anade dos bloques calculados al vuelo, de solo lectura:
 * ofertas vigentes (T2) y "mas vendidos" (agregacion sobre DetallePedido).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int MAX_LISTA = 30;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(CatalogoProducto.ZONA);

    private final MongoTemplate mongoTemplate;
    private final MessageSource messageSource;
    private final ProductoService productoService;
    private final DetallePedidoRepository detallePedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioSesion usuarioSesion;

    public NotificacionService(MongoTemplate mongoTemplate, MessageSource messageSource,
                               ProductoService productoService, DetallePedidoRepository detallePedidoRepository,
                               UsuarioRepository usuarioRepository, UsuarioSesion usuarioSesion) {
        this.mongoTemplate = mongoTemplate;
        this.messageSource = messageSource;
        this.productoService = productoService;
        this.detallePedidoRepository = detallePedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioSesion = usuarioSesion;
    }

    // ------------------------------------------------------------------ publicar

    private void publicar(String destino, Long usuarioId, String tipo, String clave, String enlace, String... params) {
        try {
            mongoTemplate.save(new Notificacion(destino, usuarioId, tipo, clave, List.of(params), enlace));
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar la notificacion {} ({})", tipo, e.getMessage());
        }
    }

    /** D2: pesos con separador de miles es-CO en todos los idiomas. */
    static String pesos(double valor) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return "$" + new DecimalFormat("#,##0", s).format(Math.round(valor));
    }

    private static String ref(String id) {
        return id == null ? "" : id.substring(Math.max(0, id.length() - 6)).toUpperCase();
    }

    /** Cotizacion nueva, con cuenta o sin ella (T1): aviso al admin. */
    public void cotizacionNueva(VidrioSostenible v) {
        if (v == null || v.getId() == null) return;
        String enlace = "/vidrio-personalizado/cotizacion/" + v.getId();
        String tipo = v.getTipoVidrio() == null ? "" : v.getTipoVidrio();
        if (v.getUsuarioId() == null) {
            publicar(Notificacion.ADMIN, null, "COTIZACION_NUEVA", "notif.cotizacion_nueva_visitante", enlace, ref(v.getId()), tipo);
        } else {
            String cliente = usuarioRepository.findById(v.getUsuarioId()).map(Usuario::getUsername).orElse("#" + v.getUsuarioId());
            publicar(Notificacion.ADMIN, null, "COTIZACION_NUEVA", "notif.cotizacion_nueva", enlace, ref(v.getId()), cliente, tipo);
        }
    }

    /** Transicion de la maquina de estados (ya validada por VidrioSostenibleService). */
    public void cotizacionEstado(String id, String estado) {
        VidrioSostenible v = mongoTemplate.findById(id, VidrioSostenible.class);
        if (v == null || estado == null) return;
        String e = estado.toUpperCase();
        publicar(Notificacion.ADMIN, null, "COTIZACION_ESTADO", "notif.cotizacion_estado_admin",
                "/admin/cotizaciones", ref(id), "#notif.estado_cot." + e);
        if (v.getUsuarioId() != null) {
            publicar(Notificacion.USUARIO, v.getUsuarioId(), "COTIZACION_ESTADO", "notif.cotizacion_usuario." + e,
                    "/vidrio-personalizado/cotizacion/" + id, ref(id));
        }
    }

    /** Pedido confirmado: proceso para el admin y confirmacion para el cliente. */
    public void pedidoNuevo(Pedido p) {
        if (p == null || p.getId() == null) return;
        int unidades = p.getDetalles() == null ? 0 : p.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum();
        publicar(Notificacion.ADMIN, null, "PEDIDO_NUEVO", "notif.pedido_nuevo", "/admin/pedidos",
                p.getNumeroFactura(), pesos(p.getTotal()), String.valueOf(unidades));
        if (p.getUsuario() != null) {
            publicar(Notificacion.USUARIO, p.getUsuario().getId(), "PEDIDO_NUEVO", "notif.pedido_confirmado",
                    "/checkout/pedido/" + p.getId(), p.getNumeroFactura());
        }
    }

    public void pedidoEstado(Pedido p, String estado) {
        if (p == null || estado == null) return;
        String e = "#notif.estado_ped." + estado.toUpperCase();
        publicar(Notificacion.ADMIN, null, "PEDIDO_ESTADO", "notif.pedido_estado_admin", "/admin/pedidos",
                p.getNumeroFactura(), e);
        if (p.getUsuario() != null) {
            publicar(Notificacion.USUARIO, p.getUsuario().getId(), "PEDIDO_ESTADO", "notif.pedido_estado_usuario",
                    "/checkout/pedido/" + p.getId(), p.getNumeroFactura(), e);
        }
    }

    /** Tras una compra real (D1): stock bajo o agotado, para que el admin reponga. */
    public void stockBajo(CatalogoProducto p) {
        if (p == null || !p.isStockControlado()) return;
        if (p.isAgotado()) {
            publicar(Notificacion.ADMIN, null, "STOCK_AGOTADO", "notif.stock_agotado", "/admin/productos", p.getNombre());
        } else if (p.isUltimasUnidades()) {
            publicar(Notificacion.ADMIN, null, "STOCK_BAJO", "notif.stock_bajo", "/admin/productos",
                    p.getNombre(), String.valueOf(p.getStock()));
        }
    }

    public void productoNuevo(CatalogoProducto p) {
        if (p == null || p.getId() == null) return;
        publicar(Notificacion.TODOS, null, "PRODUCTO_NUEVO", "notif.producto_nuevo", "/detalle/" + p.getId(), p.getNombre());
    }

    public void ofertaNueva(CatalogoProducto p) {
        if (p == null || !p.isOfertaVigente()) return;
        publicar(Notificacion.TODOS, null, "OFERTA_NUEVA", "notif.oferta_nueva", "/detalle/" + p.getId(),
                p.getNombre(), pesos(p.getPrecioVigente()), String.valueOf(p.getDescuentoPorcentaje()));
    }

    public void reposicion(CatalogoProducto p) {
        if (p == null || p.getId() == null) return;
        publicar(Notificacion.TODOS, null, "REPOSICION", "notif.reposicion", "/detalle/" + p.getId(), p.getNombre());
    }

    // ------------------------------------------------------------------ leer

    /** Quien pregunta: admin, usuario con cuenta o visitante. */
    private record Lector(boolean admin, Long usuarioId) {
        boolean visitante() { return usuarioId == null; }
    }

    private Lector lector(Authentication auth) {
        if (!UsuarioSesion.autenticado(auth)) return new Lector(false, null);
        Long id = usuarioSesion.buscar(auth).map(Usuario::getId).orElse(null);
        return new Lector(PermisosAdvice.esAdmin(auth), id);
    }

    /** Lo que cada quien puede ver (tambien limita lo que puede marcar como leido). */
    private static Criteria visibles(Lector l) {
        if (l.admin()) return Criteria.where("destino").is(Notificacion.ADMIN);
        if (l.visitante()) return Criteria.where("destino").is(Notificacion.TODOS);
        return new Criteria().orOperator(
                Criteria.where("destino").is(Notificacion.TODOS),
                Criteria.where("destino").is(Notificacion.USUARIO).and("usuarioId").is(l.usuarioId()));
    }

    private String texto(String clave, List<String> params, Locale locale) {
        Object[] args = params == null ? new Object[0] : params.stream()
                .map(p -> p != null && p.startsWith("#") ? messageSource.getMessage(p.substring(1), null, p.substring(1), locale) : p)
                .toArray();
        return messageSource.getMessage(clave, args, clave, locale);
    }

    /**
     * Respuesta del polling. desdeMs: solo para visitantes, momento en que marcaron
     * todo como visto (se guarda en su navegador: no hay cuenta donde guardarlo).
     */
    public Map<String, Object> listar(Authentication auth, Long desdeMs) {
        Lector l = lector(auth);
        Locale locale = LocaleContextHolder.getLocale();
        Query q = new Query(visibles(l)).with(Sort.by(Sort.Direction.DESC, "fecha")).limit(MAX_LISTA);
        List<Map<String, Object>> items = new ArrayList<>();
        int noLeidas = 0;
        for (Notificacion n : mongoTemplate.find(q, Notificacion.class)) {
            boolean leida = l.visitante()
                    ? desdeMs != null && n.getFecha() != null && !n.getFecha().isAfter(Instant.ofEpochMilli(desdeMs))
                    : n.getLeidaPor() != null && n.getLeidaPor().contains(l.usuarioId());
            if (!leida) noLeidas++;
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("id", n.getId());
            it.put("tipo", n.getTipo());
            it.put("texto", texto(n.getClave(), n.getParams(), locale));
            it.put("enlace", n.getEnlace());
            it.put("fecha", n.getFecha() == null ? "" : FECHA.format(n.getFecha()));
            it.put("leida", leida);
            items.add(it);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("noLeidas", noLeidas);
        r.put("etiqueta", noLeidas > 0
                ? messageSource.getMessage("notif.n_sin_leer", new Object[]{String.valueOf(noLeidas)}, locale)
                : messageSource.getMessage("notif.sin_novedades", null, locale));
        r.put("items", items);
        r.put("ofertas", l.admin() ? List.of() : ofertas(locale));
        r.put("masVendidos", masVendidos(locale));
        return r;
    }

    /** U1: nombre del producto en ingles si lo tiene (mismo criterio que ContenidoLocalizado). */
    private static String nombre(CatalogoProducto p, Locale locale) {
        boolean en = locale != null && "en".equals(locale.getLanguage());
        return en && p.getNombreEn() != null && !p.getNombreEn().isBlank() ? p.getNombreEn() : p.getNombre();
    }

    private List<Map<String, Object>> ofertas(Locale locale) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (CatalogoProducto p : productoService.obtenerTodos()) {
            if (!p.isOfertaVigente() || p.isAgotado()) continue;
            lista.add(Map.of("texto", messageSource.getMessage("notif.oferta_item",
                            new Object[]{nombre(p, locale), pesos(p.getPrecioVigente()), String.valueOf(p.getDescuentoPorcentaje())}, locale),
                    "enlace", "/detalle/" + p.getId()));
            if (lista.size() == 4) break;
        }
        return lista;
    }

    /** Agregacion de solo lectura; las obras a medida no estan en el catalogo y se omiten. */
    private List<Map<String, Object>> masVendidos(Locale locale) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            for (Object[] fila : detallePedidoRepository.masVendidos(PageRequest.of(0, 10))) {
                CatalogoProducto p = productoService.obtenerPorIdOrNull((String) fila[0]);
                if (p == null) continue;
                lista.add(Map.of("texto", messageSource.getMessage("notif.vendido_item",
                                new Object[]{nombre(p, locale), String.valueOf(((Number) fila[1]).longValue())}, locale),
                        "enlace", "/detalle/" + p.getId()));
                if (lista.size() == 3) break;
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo calcular 'mas vendidos' ({})", e.getMessage());
        }
        return lista;
    }

    // ------------------------------------------------------------------ marcar como leida

    /** Una notificacion (solo si es visible para quien la marca). false si no hay cuenta. */
    public boolean marcarLeida(Authentication auth, String id) {
        Lector l = lector(auth);
        if (l.visitante()) return false;
        Query q = new Query(new Criteria().andOperator(Criteria.where("_id").is(id), visibles(l)));
        mongoTemplate.updateFirst(q, new Update().addToSet("leidaPor", l.usuarioId()), Notificacion.class);
        return true;
    }

    public boolean marcarTodas(Authentication auth) {
        Lector l = lector(auth);
        if (l.visitante()) return false;
        mongoTemplate.updateMulti(new Query(visibles(l)), new Update().addToSet("leidaPor", l.usuarioId()), Notificacion.class);
        return true;
    }
}
