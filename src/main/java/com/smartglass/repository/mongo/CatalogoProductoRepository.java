package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.CatalogoProducto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoProductoRepository extends MongoRepository<CatalogoProducto, String> {

    List<CatalogoProducto> findTop8ByOrderByIdDesc();

    List<CatalogoProducto> findByNombreContainingIgnoreCase(String nombre);
}
