package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.HistorialActividad;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialActividadRepository extends MongoRepository<HistorialActividad, String> {

    List<HistorialActividad> findAllByOrderByFechaAsc();
}
