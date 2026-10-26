package com.smartglass.config;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.Carrito;
import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mongo.Notificacion;
import com.smartglass.model.mongo.Resena;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.DetallePedido;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mongo.CarritoRepository;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import com.smartglass.repository.mongo.ResenaRepository;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.repository.mysql.PedidoRepository;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.AnalisisService;
import com.smartglass.service.NotificacionService;
import com.smartglass.service.PedidoService;
import com.smartglass.service.VidrioSostenibleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Datos de demostracion para una instalacion nueva (produccion del piloto).
 *
 * Solo corre con DEMO_DATOS=true (smartglass.demo.datos) y una sola vez: si el primer
 * cliente de la lista ya existe, no hace nada. Va despues de todos los seeders
 * (ApplicationReadyEvent): necesita el catalogo y el admin ya creados.
 *
 * Coherencia: las cotizaciones pasan por VidrioSostenibleService (mismo precio, CO2 por
 * etapas y notificaciones que el configurador); los pedidos siguen las reglas de
 * PedidoService (envio gratis desde $500.000, recargo de compensacion, huella por item);
 * el stock baja con cada compra; las notificaciones y fechas se reparten en las ultimas
 * diez semanas, en el orden en que habrian ocurrido.
 *
 * Los clientes no tienen contrasena conocida salvo que se defina DEMO_PASSWORD
 * (smartglass.demo.password): sin ella nadie puede entrar con esas cuentas.
 * Correos en example.com (dominio reservado): nunca llegan a una persona real.
 */
@Component
@ConditionalOnProperty(name = "smartglass.demo.datos", havingValue = "true")
public class DemoDatosSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDatosSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final CatalogoProductoRepository catalogoRepository;
    private final VidrioSostenibleRepository vidrioRepository;
    private final CarritoRepository carritoRepository;
    private final ResenaRepository resenaRepository;
    private final VidrioSostenibleService vidrioService;
    private final AnalisisService analisisService;
    private final NotificacionService notificacionService;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String passwordDemo;

    public DemoDatosSeeder(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
                           CatalogoProductoRepository catalogoRepository, VidrioSostenibleRepository vidrioRepository,
                           CarritoRepository carritoRepository, ResenaRepository resenaRepository,
                           VidrioSostenibleService vidrioService, AnalisisService analisisService,
                           NotificacionService notificacionService, MongoTemplate mongoTemplate,
                           PasswordEncoder passwordEncoder,
                           @Value("${smartglass.demo.password:}") String passwordDemo) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.catalogoRepository = catalogoRepository;
        this.vidrioRepository = vidrioRepository;
        this.carritoRepository = carritoRepository;
        this.resenaRepository = resenaRepository;
        this.vidrioService = vidrioService;
        this.analisisService = analisisService;
        this.notificacionService = notificacionService;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.passwordDemo = passwordDemo;
    }

    // ------------------------------------------------------------------ datos

    private record Cliente(String username, String nombre, String direccion, String idioma, String accesibilidad) {}

    private static final List<Cliente> CLIENTES = List.of(
            new Cliente("laura.gomez", "Laura Gómez", "Carrera 11 # 93-42, Bogotá", null, null),
            new Cliente("andres.rojas", "Andrés Rojas", "Calle 10 # 43B-25, Medellín", null, null),
            new Cliente("valentina.ortiz", "Valentina Ortiz", "Avenida 6N # 25-38, Cali", null, null),
            new Cliente("camilo.herrera", "Camilo Herrera", "Carrera 53 # 82-110, Barranquilla", null, null),
            new Cliente("daniela.mejia", "Daniela Mejía", "Calle 36 # 27-51, Bucaramanga", null, "fotosensible"),
            new Cliente("santiago.vargas", "Santiago Vargas", "Avenida Circunvalar # 12-08, Pereira", null, null),
            new Cliente("emily.carter", "Emily Carter", "Calle del Arsenal # 10-43, Cartagena", "en", null),
            new Cliente("julian.moreno", "Julián Moreno", "Carrera 23 # 64-15, Manizales", null, null),
            new Cliente("paula.restrepo", "Paula Restrepo", "Transversal 39 # 72-30, Medellín", null, "daltonico"),
            new Cliente("vidrios.andinos", "Vidrios Andinos S.A.S.", "Calle 13 # 68D-40, Bogotá", null, null),
            new Cliente("sofia.lara", "Sofía Lara · Arquitecta", "Carrera 7 # 71-21, Bogotá", null, null));

    /** Una obra a medida tal como la habria enviado el configurador. */
    private record Obra(int cliente, String uso, String tipo, double ancho, double alto, double espesor, String forma,
                        String tratamiento, String bordes, int perforaciones, boolean reciclado, boolean renovable,
                        boolean compensar, boolean urgente, boolean empaque, boolean consolidada, boolean retoma,
                        boolean retazos, String estado, int diasAtras, String nota) {}

    private static final String COT = VidrioSostenibleService.COTIZACION;
    private static final String APR = VidrioSostenibleService.APROBADO;
    private static final String REC = VidrioSostenibleService.RECHAZADA;

    // cliente: indice en CLIENTES. Las aprobadas mas antiguas se compran despues (PEDIDOS).
    private static final List<Obra> OBRAS = List.of(
            new Obra(0, "Ducha", "Templado", 90, 190, 8, "Rectangular", "Ninguno", "PulidoPlano", 4, true, false, false, false, true, false, true, false, APR, 68, "Cabina de ducha del baño principal, con bisagras a muro."),
            new Obra(1, "Baranda", "TempladoLaminado", 120, 100, 12, "Rectangular", "Ninguno", "PulidoPlano", 4, true, true, true, false, true, true, false, false, APR, 66, "Baranda de balcón, piso 8."),
            new Obra(9, "Fachada", "Laminado", 150, 220, 10, "Rectangular", "ControlSolar", "CorteLimpio", 0, true, false, false, false, false, true, false, true, APR, 63, "Lote de prueba para la fachada de una oficina en Chapinero."),
            new Obra(2, "Mobiliario", "Templado", 110, 110, 10, "Circular", "Ninguno", "Biselado", 0, true, true, false, false, true, false, false, false, APR, 60, "Mesa de comedor para seis personas."),
            new Obra(10, "Ventana", "Insulado", 140, 120, 24, "Arco", "Ninguno", "CorteLimpio", 0, true, true, true, false, true, true, true, false, APR, 57, "Ventana en arco de una casa en Usaquén, mejor aislamiento térmico."),
            new Obra(3, "Espejo", "Crudo", 80, 120, 5, "EsquinasRedondeadas", "Ninguno", "Biselado", 2, false, false, false, false, false, false, false, false, APR, 55, "Espejo de baño con dos perforaciones para la lámpara."),
            new Obra(4, "Puerta", "Templado", 85, 210, 10, "Rectangular", "Esmerilado", "PulidoPlano", 4, true, false, false, false, true, false, true, false, APR, 52, "Puerta de estudio, esmerilada para dar privacidad."),
            new Obra(7, "Repisa", "Templado", 60, 25, 10, "Capsula", "Ninguno", "PulidoPlano", 0, true, false, false, false, false, true, false, true, REC, 50, "Tres repisas iguales para una biblioteca."),
            new Obra(5, "Techo", "Laminado", 200, 150, 12, "Rectangular", "TintadoGris", "CorteLimpio", 0, true, true, true, false, false, true, false, false, APR, 47, "Cubierta de patio interior."),
            new Obra(6, "Mobiliario", "Templado", 140, 80, 8, "Hexagonal", "Ninguno", "Biselado", 0, true, false, false, false, true, false, false, false, APR, 45, "Coffee table top for a rental apartment."),
            new Obra(9, "Fachada", "Laminado", 150, 220, 10, "Rectangular", "ControlSolar", "CorteLimpio", 0, true, true, true, false, false, true, false, true, APR, 41, "Segundo lote de la fachada, mismas medidas."),
            new Obra(8, "Ducha", "Templado", 70, 200, 8, "TrapecioRectangulo", "Esmerilado", "PulidoPlano", 4, true, false, false, true, false, false, false, false, APR, 38, "Ducha bajo escalera, una esquina en diagonal."),
            new Obra(1, "Ventana", "Insulado", 160, 140, 24, "Rectangular", "ControlSolar", "CorteLimpio", 0, true, true, false, false, true, true, true, false, APR, 34, "Ventanal de sala orientado al occidente."),
            new Obra(10, "Mobiliario", "Templado", 220, 90, 12, "EsquinasRedondeadas", "Ninguno", "PulidoPlano", 0, true, true, true, false, true, true, false, false, REC, 31, "Escritorio de sala de juntas, medida especial."),
            new Obra(0, "Espejo", "Crudo", 100, 160, 6, "Arco", "Ninguno", "Biselado", 0, false, false, false, false, false, false, false, false, APR, 28, "Espejo de cuerpo entero para el vestier."),
            new Obra(3, "Baranda", "TempladoLaminado", 100, 95, 10, "Rectangular", "Ninguno", "PulidoPlano", 4, true, false, false, false, true, false, false, false, APR, 25, "Baranda de escalera, cuatro paños iguales."),
            new Obra(9, "Fachada", "Inteligente", 120, 180, 10, "Rectangular", "Ninguno", "CorteLimpio", 0, true, true, true, false, false, false, false, false, APR, 22, "Sala de reuniones con vidrio que se opaca."),
            new Obra(2, "Repisa", "Templado", 80, 30, 8, "Rectangular", "Ninguno", "PulidoPlano", 0, true, false, false, false, false, true, false, true, REC, 19, "Repisas de cocina."),
            new Obra(5, "Techo", "Fotovoltaico", 160, 100, 12, "Rectangular", "Ninguno", "CorteLimpio", 0, true, true, true, false, false, true, false, false, APR, 16, "Pérgola que genere algo de energía."),
            new Obra(7, "Puerta", "Templado", 90, 205, 10, "Fronton", "Serigrafiado", "PulidoPlano", 4, true, false, false, false, true, false, true, false, APR, 13, "Puerta principal con serigrafía de líneas."),
            new Obra(6, "Ventana", "Laminado", 120, 100, 8, "Octogonal", "TintadoBronce", "CorteLimpio", 0, true, false, false, false, false, false, false, false, REC, 11, "Octagonal window for the stairwell."),
            new Obra(4, "Mobiliario", "Templado", 50, 50, 6, "Rombo", "Ninguno", "Biselado", 0, true, false, false, false, false, false, false, true, COT, 8, "Mesa auxiliar pequeña."),
            new Obra(10, "Piso", "TempladoLaminado", 100, 100, 20, "Rectangular", "Esmerilado", "PulidoPlano", 0, true, true, true, false, true, true, true, false, COT, 6, "Losa transitable para un mezanine."),
            new Obra(8, "Baranda", "TempladoLaminado", 110, 100, 12, "Rectangular", "Ninguno", "PulidoPlano", 4, true, true, false, false, true, true, false, false, COT, 4, "Baranda de terraza."),
            new Obra(1, "Ducha", "Templado", 100, 200, 10, "Rectangular", "Antireflejo", "PulidoPlano", 4, true, true, true, false, true, false, true, false, COT, 3, "Cambio de la división de ducha del segundo baño."),
            new Obra(3, "Espejo", "Crudo", 60, 90, 4, "Circular", "Ninguno", "Biselado", 0, false, false, false, false, false, false, false, false, COT, 2, "Espejo redondo para la entrada."),
            new Obra(9, "Ventana", "Insulado", 180, 150, 24, "Rectangular", "ControlSolar", "CorteLimpio", 0, true, true, true, false, false, true, true, true, COT, 1, "Ventanas del tercer piso de la oficina."),
            new Obra(0, "Mobiliario", "Templado", 120, 60, 8, "Capsula", "Ninguno", "PulidoPlano", 0, true, false, false, false, true, false, false, false, COT, 0, "Escritorio para trabajo en casa."));

    /**
     * Pedido: lineas "CODIGO:cantidad" del catalogo o "OBRA:n" (indice en OBRAS, una aprobada).
     * estado final segun la antiguedad, como lo habria movido el admin.
     */
    private record Compra(int cliente, List<String> lineas, int diasAtras, String estado, String pago, boolean compensar,
                          boolean empaque, boolean consolidada, boolean retoma) {}

    private static final List<Compra> PEDIDOS = List.of(
            new Compra(0, List.of("VID-010:1", "ESC-P-01:1"), 70, "ENTREGADO", "TARJETA_CREDITO", false, false, false, false),
            new Compra(1, List.of("VID-006:2"), 64, "ENTREGADO", "PSE", true, true, false, false),
            new Compra(0, List.of("OBRA:0"), 62, "ENTREGADO", "TARJETA_CREDITO", false, true, false, true),
            new Compra(9, List.of("OBRA:2", "VID-019:2"), 58, "ENTREGADO", "PSE", true, false, true, false),
            new Compra(2, List.of("OBRA:3", "ESC-P-02:1"), 54, "ENTREGADO", "TARJETA_CREDITO", false, true, false, false),
            new Compra(10, List.of("OBRA:4"), 50, "ENTREGADO", "PSE", true, true, true, true),
            new Compra(3, List.of("OBRA:5", "VID-010:2"), 48, "ENTREGADO", "TARJETA_CREDITO", false, false, false, false),
            new Compra(5, List.of("VID-004:2", "VID-015:1"), 44, "ENTREGADO", "PSE", true, false, true, false),
            new Compra(4, List.of("OBRA:6"), 40, "ENTREGADO", "TARJETA_CREDITO", false, true, false, true),
            new Compra(6, List.of("OBRA:9", "ESC-P-03:2"), 36, "ENTREGADO", "TARJETA_CREDITO", false, true, false, false),
            new Compra(9, List.of("OBRA:10"), 33, "ENTREGADO", "PSE", true, false, true, false),
            new Compra(7, List.of("VID-018:1", "VID-009:1"), 29, "CANCELADO", "TARJETA_CREDITO", false, false, false, false),
            new Compra(5, List.of("OBRA:8"), 26, "ENTREGADO", "PSE", true, false, true, false),
            new Compra(8, List.of("OBRA:11"), 22, "ENVIADO", "TARJETA_CREDITO", false, false, false, false),
            new Compra(1, List.of("OBRA:12", "VID-016:1"), 18, "ENVIADO", "PSE", false, true, true, true),
            new Compra(3, List.of("OBRA:15"), 12, "PROCESANDO", "TARJETA_CREDITO", false, true, false, false),
            new Compra(2, List.of("VID-011:2", "ESC-M-02:1"), 9, "PROCESANDO", "TARJETA_CREDITO", true, true, false, false),
            new Compra(9, List.of("OBRA:16", "VID-014:2"), 5, "PROCESANDO", "PSE", true, false, true, false),
            new Compra(10, List.of("VID-013:1"), 2, "PENDIENTE", "PSE", false, false, false, false),
            new Compra(6, List.of("VID-001:1", "ESC-P-01:1"), 1, "PENDIENTE", "TARJETA_CREDITO", false, true, false, false));

    /** Opiniones de quienes ya recibieron su compra (codigo de catalogo, cliente, estrellas, texto). */
    private record Opinion(String codigo, int cliente, int estrellas, int diasAtras, String texto) {}

    private static final List<Opinion> OPINIONES = List.of(
            new Opinion("VID-010", 0, 5, 63, "Llegó bien empacado y el biselado quedó parejo. Lo instalamos el mismo día."),
            new Opinion("ESC-P-01", 0, 5, 63, "La gota es más bonita en persona, refleja la luz de la ventana todo el día."),
            new Opinion("VID-006", 1, 4, 57, "Buen vidrio de seguridad. La entrega tardó un día más de lo que decía."),
            new Opinion("VID-019", 9, 5, 50, "Bajó bastante el calor en la oficina de la tarde. Pediremos más para el otro piso."),
            new Opinion("ESC-P-02", 2, 4, 47, "Muy buen acabado; la caja llegó un poco golpeada pero la pieza intacta."),
            new Opinion("VID-010", 3, 4, 41, "Buen espejo por el precio. Faltó un instructivo para colgarlo."),
            new Opinion("VID-004", 5, 5, 37, "Se nota la diferencia de ruido con la avenida. Muy recomendado."),
            new Opinion("VID-015", 5, 3, 37, "Funciona, pero con la lluvia fuerte igual toca limpiarlo de vez en cuando."),
            new Opinion("ESC-P-03", 6, 5, 29, "Beautiful piece, my guests always ask about it."));

    /** Carritos abiertos: alguien que dejo productos o una obra aprobada sin pagar. */
    private record CarritoAbierto(int cliente, List<String> lineas) {}

    private static final List<CarritoAbierto> CARRITOS = List.of(
            new CarritoAbierto(0, List.of("OBRA:14", "VID-016:1")),
            new CarritoAbierto(5, List.of("OBRA:18")),
            new CarritoAbierto(8, List.of("ESC-M-01:1", "VID-002:2")));

    // ------------------------------------------------------------------ carga

    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        if (usuarioRepository.findByUsername(CLIENTES.get(0).username()).isPresent()) {
            log.info("Datos de demostracion: ya estaban cargados, no se toca nada.");
            return;
        }
        if (catalogoRepository.count() == 0) {
            log.warn("Datos de demostracion: el catalogo esta vacio, no se cargan.");
            return;
        }
        Long adminId = usuarioRepository.findByUsername("admin").map(Usuario::getId).orElse(null);

        List<Usuario> clientes = crearClientes();
        List<VidrioSostenible> obras = crearObras(clientes, adminId);
        int pedidos = crearPedidos(clientes, obras, adminId);
        int opiniones = crearOpiniones(clientes);
        int carritos = crearCarritos(clientes, obras);

        log.info("Datos de demostracion cargados: {} clientes, {} cotizaciones, {} pedidos, {} opiniones, {} carritos.",
                clientes.size(), obras.size(), pedidos, opiniones, carritos);
    }

    /** Fecha pasada a una hora de oficina; nunca en el futuro (lo de "hoy" puede caer antes de esa hora). */
    private static LocalDateTime haceDias(int dias, int hora) {
        LocalDateTime ahora = LocalDateTime.now(CatalogoProducto.ZONA);
        LocalDateTime f = ahora.minusDays(dias).withHour(hora).withMinute((dias * 7 + hora * 13) % 60).withSecond(0).withNano(0);
        return f.isAfter(ahora) ? ahora.minusMinutes(30L + dias + hora) : f;
    }

    private static Instant instante(LocalDateTime fecha) {
        return fecha.atZone(CatalogoProducto.ZONA).toInstant();
    }

    /**
     * Ejecuta una accion que publica notificaciones (servicio o aspecto) y les pone la fecha en que
     * habria ocurrido. Las mas viejas quedan leidas por quien las recibio.
     */
    private void conFecha(LocalDateTime cuando, Long adminId, Runnable accion) {
        Instant antes = Instant.now().minusMillis(1);
        accion.run();
        Instant fecha = instante(cuando);
        Query nuevas = new Query(Criteria.where("fecha").gt(antes));
        for (Notificacion n : mongoTemplate.find(nuevas, Notificacion.class)) {
            Update u = new Update().set("fecha", fecha);
            boolean vieja = cuando.isBefore(LocalDateTime.now(CatalogoProducto.ZONA).minusDays(3));
            Long lector = Notificacion.ADMIN.equals(n.getDestino()) ? adminId : n.getUsuarioId();
            if (vieja && lector != null) u.addToSet("leidaPor", lector);
            mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(n.getId())), u, Notificacion.class);
        }
    }

    private List<Usuario> crearClientes() {
        List<Usuario> creados = new ArrayList<>();
        for (Cliente c : CLIENTES) {
            String clave = passwordDemo == null || passwordDemo.isBlank() ? UUID.randomUUID().toString() : passwordDemo;
            Usuario u = new Usuario(c.nombre(), c.username() + "@example.com", c.username(), passwordEncoder.encode(clave));
            u.setPreferenciaIdioma(c.idioma());
            u.setPreferenciaAccesibilidad(c.accesibilidad());
            creados.add(usuarioRepository.save(u));
        }
        return creados;
    }

    private List<VidrioSostenible> crearObras(List<Usuario> clientes, Long adminId) {
        List<VidrioSostenible> obras = new ArrayList<>();
        for (Obra o : OBRAS) {
            CotizacionVidrioRequest r = new CotizacionVidrioRequest();
            r.setUsuarioId(clientes.get(o.cliente()).getId());
            r.setUsoPrincipal(o.uso());
            r.setTipoVidrio(o.tipo());
            r.setAncho(o.ancho());
            r.setAlto(o.alto());
            r.setEspesor(o.espesor());
            r.setForma(o.forma());
            r.setTratamiento(o.tratamiento());
            r.setAcabadoBordes(o.bordes());
            r.setPerforaciones(o.perforaciones());
            r.setUsarVidrioReciclado(o.reciclado());
            r.setEnergiaRenovable(o.renovable());
            r.setCompensarCarbono(o.compensar());
            r.setUrgente(o.urgente());
            r.setEmpaqueReutilizable(o.empaque());
            r.setEntregaConsolidada(o.consolidada());
            r.setRetomaVidrio(o.retoma());
            r.setUsarRetazos(o.retazos());
            r.setNotasAdicionales(o.nota());

            LocalDateTime creada = haceDias(o.diasAtras(), 9 + o.cliente() % 8);
            VidrioSostenible[] v = new VidrioSostenible[1];
            conFecha(creada, adminId, () -> v[0] = vidrioService.generarCotizacion(r));
            v[0].setFechaCreacion(creada);
            vidrioRepository.save(v[0]);

            if (!COT.equals(o.estado())) {
                // el admin la revisa uno o dos dias despues
                LocalDateTime revisada = creada.plusDays(1 + o.diasAtras() % 2).withHour(15);
                String id = v[0].getId();
                conFecha(revisada, adminId, () -> vidrioService.cambiarEstado(id, o.estado()));
                v[0] = vidrioRepository.findById(id).orElse(v[0]);
            }
            obras.add(v[0]);
        }
        return obras;
    }

    /** Producto del carrito como lo arma CarritoService (catalogo o snapshot de una obra aprobada). */
    private CatalogoProducto producto(String linea, List<VidrioSostenible> obras) {
        String clave = linea.split(":")[0];
        if ("OBRA".equals(clave)) {
            VidrioSostenible v = obras.get(Integer.parseInt(linea.split(":")[1]));
            CatalogoProducto snapshot = new CatalogoProducto();
            snapshot.setId(v.getId());
            snapshot.setNombre(v.getTipoVidrio() + " · obra a medida");
            snapshot.setTipo(v.getTipoVidrio());
            snapshot.setPrecio(v.getPrecioEstimado());
            snapshot.setAncho(v.getAncho());
            snapshot.setAlto(v.getAlto());
            snapshot.setEspesor(v.getEspesor());
            return snapshot;
        }
        return catalogoRepository.findFirstByCodigo(clave).orElse(null);
    }

    private static int cantidad(String linea) {
        return linea.startsWith("OBRA:") ? 1 : Integer.parseInt(linea.split(":")[1]);
    }

    private int crearPedidos(List<Usuario> clientes, List<VidrioSostenible> obras, Long adminId) {
        Map<String, VidrioSostenible> obraPorId = new HashMap<>();
        obras.forEach(v -> obraPorId.put(v.getId(), v));
        int creados = 0;
        for (Compra c : PEDIDOS) {
            Usuario cliente = clientes.get(c.cliente());
            LocalDateTime fecha = haceDias(c.diasAtras(), 10 + c.cliente() % 9);
            Pedido p = new Pedido();
            p.setUsuario(cliente);
            p.setFechaPedido(fecha);
            p.setDireccionEnvio(CLIENTES.get(c.cliente()).direccion());
            p.setMetodoPago(c.pago());
            p.setCompensacionCarbono(c.compensar());
            p.setEmpaqueReutilizable(c.empaque());
            p.setEntregaConsolidada(c.consolidada());
            p.setRetomaVidrio(c.retoma());

            double huella = 0;
            List<CatalogoProducto> delCatalogo = new ArrayList<>();
            for (String linea : c.lineas()) {
                CatalogoProducto prod = producto(linea, obras);
                if (prod == null) continue;
                int n = cantidad(linea);
                if (prod.isStockControlado() && prod.getStock() < n + 1) continue;   // no deja el catalogo sin unidades
                p.agregarDetalle(new DetallePedido(prod, n));
                VidrioSostenible obra = obraPorId.get(prod.getId());
                huella += obra != null ? obra.getHuellaCarbono() * n
                        : analisisService.calcularHuellaCarbonoItem(prod.getAncho(), prod.getAlto(), prod.getEspesor(), prod.getTipo(), n);
                if (obra == null) delCatalogo.add(prod);
            }
            if (p.getDetalles().isEmpty()) continue;

            double subtotal = p.getSubtotalProductos();
            p.setTotal(subtotal + (subtotal > 500000 ? 0 : 25000));
            if (c.compensar()) {
                p.setRecargoCompensacion(PedidoService.RECARGO_COMPENSACION_PEDIDO);
                p.setTotal(p.getTotal() + PedidoService.RECARGO_COMPENSACION_PEDIDO);
            }
            p.setHuellaCarbono(Math.round(huella * 100.0) / 100.0);

            // se sigue usando p (con sus lineas en memoria): save() de una entidad ya guardada devuelve otra copia
            pedidoRepository.save(p);
            p.generarNumeroFacturaOficial();
            pedidoRepository.save(p);
            Pedido pedido = p;
            conFecha(fecha, adminId, () -> notificacionService.pedidoNuevo(pedido));

            // D1: un pedido cancelado devuelve las unidades; los demas las descuentan
            if (!"CANCELADO".equals(c.estado())) {
                for (DetallePedido d : pedido.getDetalles()) {
                    delCatalogo.stream().filter(x -> x.getId().equals(d.getProductoId()) && x.isStockControlado()).findFirst()
                            .ifPresent(x -> {
                                CatalogoProducto tras = mongoTemplate.findAndModify(
                                        new Query(Criteria.where("_id").is(x.getId())), new Update().inc("stock", -d.getCantidad()),
                                        FindAndModifyOptions.options().returnNew(true), CatalogoProducto.class);
                                if (tras != null && (tras.isUltimasUnidades() || tras.isAgotado())) {
                                    conFecha(fecha, adminId, () -> notificacionService.stockBajo(tras));
                                }
                            });
                }
            }

            // el admin lo movio hasta su estado actual; se avisa la ultima transicion
            if (!"PENDIENTE".equals(c.estado())) {
                pedido.setEstado(c.estado());
                pedidoRepository.save(pedido);
                int dias = switch (c.estado()) {
                    case "PROCESANDO", "CANCELADO" -> 1;
                    case "ENVIADO" -> 3;
                    default -> 6;
                };
                LocalDateTime movido = fecha.plusDays(Math.min(dias, Math.max(1, c.diasAtras() - 1))).withHour(16);
                conFecha(movido, adminId, () -> notificacionService.pedidoEstado(pedido, c.estado()));
            }
            creados++;
        }
        return creados;
    }

    private int crearOpiniones(List<Usuario> clientes) {
        int creadas = 0;
        for (Opinion o : OPINIONES) {
            CatalogoProducto prod = catalogoRepository.findFirstByCodigo(o.codigo()).orElse(null);
            if (prod == null) continue;
            Usuario u = clientes.get(o.cliente());
            Resena r = new Resena(prod.getId(), u.getId(), u.getNombre(), o.estrellas(), o.texto());
            r.setFecha(haceDias(o.diasAtras(), 20));
            resenaRepository.save(r);
            creadas++;
        }
        return creadas;
    }

    private int crearCarritos(List<Usuario> clientes, List<VidrioSostenible> obras) {
        int creados = 0;
        for (CarritoAbierto c : CARRITOS) {
            Carrito carrito = new Carrito(clientes.get(c.cliente()).getId());
            for (String linea : c.lineas()) {
                CatalogoProducto prod = producto(linea, obras);
                if (prod == null || (prod.isStockControlado() && prod.getStock() < cantidad(linea))) continue;
                carrito.getItems().add(new ItemCarrito(linea.startsWith("OBRA:") ? prod : prod.copiaParaCarrito(), cantidad(linea)));
            }
            if (carrito.getItems().isEmpty()) continue;
            carritoRepository.save(carrito);
            creados++;
        }
        return creados;
    }
}
