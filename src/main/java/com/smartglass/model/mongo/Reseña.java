package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Reseña de un producto del catalogo. Una por usuario por producto
 * (si el usuario vuelve a opinar, se actualiza la existente en vez
 * de duplicarla -- ver ResenaService.crearOActualizar).
 */
@Document(collection = "resenas")
public class Reseña {

    @Id
    private String id;

    private String productoId;
    private Long usuarioId;
    private String nombreUsuario;

    private int calificacion; // 1 a 5
    private String comentario;

    private LocalDateTime fecha;

    public Reseña() {
    }

    public Reseña(String productoId, Long usuarioId, String nombreUsuario, int calificacion, String comentario) {
        this.productoId = productoId;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fecha = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    /**
     * Representacion en estrellas para la vista, ej. "★★★☆☆" para
     * calificacion=3. Se calcula aqui (no en la plantilla) para
     * evitar aritmetica de tipos en SpringEL.
     */
    public String getEstrellas() {
        int c = Math.max(0, Math.min(5, calificacion));
        return "★".repeat(c) + "☆".repeat(5 - c);
    }
}