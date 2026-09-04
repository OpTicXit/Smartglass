package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.Carrito;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoRepository extends MongoRepository<Carrito, String> {

    Optional<Carrito> findByUsuarioId(Long usuarioId);
}