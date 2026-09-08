package com.smartglass.service;

import com.smartglass.model.mongo.Reseña;
import com.smartglass.repository.mongo.ReseñaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReseñaService {

    private final ReseñaRepository resenaRepository;

    public ReseñaService(ReseñaRepository resenaRepository) {
        this.resenaRepository = resenaRepository;
    }

    /**
     * Si el usuario ya habia reseñado este producto, actualiza esa
     * reseña (calificacion + comentario + fecha) en vez de duplicarla.
     */
    public Reseña crearOActualizar(String productoId, Long usuarioId, String nombreUsuario,
                                    int calificacion, String comentario) {
        if (calificacion < 1 || calificacion > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5.");
        }

        Reseña resena = resenaRepository.findByProductoIdAndUsuarioId(productoId, usuarioId)
                .orElseGet(() -> new Reseña(productoId, usuarioId, nombreUsuario, calificacion, comentario));

        resena.setNombreUsuario(nombreUsuario);
        resena.setCalificacion(calificacion);
        resena.setComentario(comentario);
        resena.setFecha(java.time.LocalDateTime.now());

        return resenaRepository.save(resena);
    }

    public List<Reseña> obtenerPorProducto(String productoId) {
        return resenaRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    public double calcularPromedio(String productoId) {
        List<Reseña> resenas = obtenerPorProducto(productoId);
        if (resenas.isEmpty()) return 0.0;
        double promedio = resenas.stream().mapToInt(Reseña::getCalificacion).average().orElse(0.0);
        return Math.round(promedio * 10.0) / 10.0;
    }

    public Optional<Reseña> obtenerDeUsuario(String productoId, Long usuarioId) {
        return resenaRepository.findByProductoIdAndUsuarioId(productoId, usuarioId);
    }

    // --- Moderacion (admin) ---

    public List<Reseña> obtenerTodas() {
        return resenaRepository.findAll();
    }

    public void eliminarPorId(String id) {
        resenaRepository.deleteById(id);
    }
}