package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.Reseña;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReseñaRepository extends MongoRepository<Reseña, String> {

    List<Reseña> findByProductoIdOrderByFechaDesc(String productoId);

    Optional<Reseña> findByProductoIdAndUsuarioId(String productoId, Long usuarioId);
}