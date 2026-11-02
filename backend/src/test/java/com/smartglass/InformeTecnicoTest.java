package com.smartglass;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El informe tecnico depende de un JSON generado por exportar_informe.py y de textos en dos idiomas:
 * si falta una seccion o una clave, la pagina se romperia sin que el arranque lo detecte.
 */
class InformeTecnicoTest {

    private static final Path RECURSOS = Path.of("src/main/resources");

    @Test
    void elJsonTraeLoQueDibujaLaPagina() throws IOException {
        JsonNode datos = JsonMapper.builder().build().readTree(RECURSOS.resolve("informe/informe-tecnico.json").toFile());
        JsonNode modelo = datos.get("modelo");

        for (String seccion : new String[] { "metricas", "referencia", "tipos", "co2_tipo", "correlacion", "real_pred", "errores", "importancia", "real_pred_tipo" }) {
            assertTrue(modelo.has(seccion), "falta modelo." + seccion);
        }
        assertEquals(10000, modelo.get("registros").asInt());
        assertEquals(modelo.get("prueba").asInt(), modelo.get("real_pred").get("real").size());
        assertEquals(modelo.get("errores").get("conteo").size() + 1, modelo.get("errores").get("bordes").size());

        JsonNode filas = datos.get("paises").get("filas");
        assertTrue(filas.size() > 10, "el globo necesita paises");
        for (JsonNode fila : filas) {
            assertTrue(fila.get("id").asInt() > 0 && fila.get("co2_mt").asDouble() > 0, "fila invalida: " + fila);
        }
    }

    @Test
    void todasLasClavesDelInformeEstanEnLosDosIdiomas() throws IOException {
        String vista = Files.readString(Path.of("../frontend/templates/admin/informe-tecnico.html"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("../frontend/templates/admin/dashboard.html"), StandardCharsets.UTF_8);
        Properties es = cargar("messages.properties");
        Properties en = cargar("messages_en.properties");

        Set<String> faltan = new TreeSet<>();
        Matcher m = Pattern.compile("#\\{(admin\\.informe\\.[a-zA-Z0-9_.]+)\\}").matcher(vista);
        int usadas = 0;
        while (m.find()) {
            usadas++;
            if (!es.containsKey(m.group(1))) faltan.add("es: " + m.group(1));
            if (!en.containsKey(m.group(1))) faltan.add("en: " + m.group(1));
        }
        assertTrue(usadas > 40, "no se encontraron las claves de la vista");
        assertTrue(faltan.isEmpty(), "Claves sin traducir: " + faltan);
    }

    private static Properties cargar(String archivo) throws IOException {
        Properties p = new Properties();
        try (var lector = Files.newBufferedReader(RECURSOS.resolve(archivo), StandardCharsets.UTF_8)) {
            p.load(lector);
        }
        return p;
    }
}
