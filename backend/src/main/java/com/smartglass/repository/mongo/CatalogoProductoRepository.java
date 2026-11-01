package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.CatalogoProducto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoProductoRepository extends MongoRepository<CatalogoProducto, String> {

    List<CatalogoProducto> findTop8ByOrderByIdDesc();

    List<CatalogoProducto> findByNombreContainingIgnoreCase(String nombre);

    // T2: soporte del seeder de esculturas (idempotente por codigo)
    Optional<CatalogoProducto> findFirstByCodigo(String codigo);
}
