package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.VidrioSostenible;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VidrioSostenibleRepository extends MongoRepository<VidrioSostenible, String> {

    List<VidrioSostenible> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<VidrioSostenible> findByEstado(String estado);
}
