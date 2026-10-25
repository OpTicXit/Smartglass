package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * T3 · notificacion (Mongo, aditiva). El texto NO se guarda: se guarda una clave
 * i18n (T0) y sus parametros ya formateados como texto (D2: cifras es-CO), y se
 * resuelve en el idioma de quien la lee. Un parametro que empieza por "#" es a su
 * vez una clave (p. ej. "#notif.estado.ENVIADO").
 *
 * Destino:
 *  - ADMIN: todos los administradores (cotizaciones nuevas, pedidos, stock bajo...).
 *  - USUARIO: solo usuarioId (su obra aprobada, su pedido enviado...).
 *  - TODOS: novedades del catalogo para cualquier visitante (producto nuevo, oferta, reposicion).
 * leidaPor guarda quien la leyo (una notificacion de ADMIN o TODOS la leen varios).
 */
@Document(collection = "notificaciones")
@CompoundIndex(name = "destino_fecha", def = "{'destino': 1, 'fecha': -1}")
public class Notificacion {

    public static final String ADMIN = "ADMIN";
    public static final String USUARIO = "USUARIO";
    public static final String TODOS = "TODOS";

    @Id
    private String id;

    private String destino;
    private Long usuarioId;          // solo con destino USUARIO
    private String tipo;             // COTIZACION_NUEVA, PEDIDO_NUEVO, OFERTA_NUEVA...
    private String clave;            // clave i18n del texto
    private List<String> params = new ArrayList<>();
    private String enlace;           // enlace profundo (cotizacion, pedido, producto)
    private Instant fecha;
    private Set<Long> leidaPor = new HashSet<>();

    public Notificacion() {
    }

    public Notificacion(String destino, Long usuarioId, String tipo, String clave, List<String> params, String enlace) {
        this.destino = destino;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.clave = clave;
        this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
        this.enlace = enlace;
        this.fecha = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }

    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }

    public Set<Long> getLeidaPor() { return leidaPor; }
    public void setLeidaPor(Set<Long> leidaPor) { this.leidaPor = leidaPor; }
}
