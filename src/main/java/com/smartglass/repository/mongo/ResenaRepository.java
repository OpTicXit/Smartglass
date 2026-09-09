package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.Resena;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResenaRepository extends MongoRepository<Resena, String> {

    List<Resena> findByProductoIdOrderByFechaDesc(String productoId);

    Optional<Resena> findByProductoIdAndUsuarioId(String productoId, Long usuarioId);
}