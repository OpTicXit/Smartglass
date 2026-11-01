package com.smartglass.repository.mongo;

import com.smartglass.model.mongo.ConfiguracionPlataforma;
import org.springframework.data.mongodb.repository.MongoRepository;

/** T5: el documento unico del plan SaaS (id "plataforma"). */
public interface ConfiguracionPlataformaRepository extends MongoRepository<ConfiguracionPlataforma, String> {
}
