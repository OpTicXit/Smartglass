package com.smartglass;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U2 · Los iconos Phosphor se sirven como subconjunto (static/vendor/phosphor). Si alguien usa
 * un icono nuevo sin regenerarlo, se veria un hueco: este test lo detecta.
 * Arreglo: python scripts/iconos_phosphor.py
 */
class IconosPhosphorTest {

    private static final Pattern TOKEN = Pattern.compile("ph-[a-z0-9]+(?:-[a-z0-9]+)*");
    /** Clases con prefijo ph- que no son iconos. */
    private static final Set<String> NO_ICONOS = Set.of("ph-fill", "ph-txt");

    @Test
    void todosLosIconosUsadosEstanEnElSubconjunto() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/static/vendor/phosphor/phosphor.css"));
        Set<String> faltan = new TreeSet<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src/main"))) {
            for (Path p : (Iterable<Path>) archivos.filter(Files::isRegularFile)::iterator) {
                String nombre = p.toString();
                if (nombre.contains("vendor") || !(nombre.endsWith(".html") || nombre.endsWith(".js")
                        || nombre.endsWith(".java") || nombre.endsWith(".properties"))) continue;
                Matcher m = TOKEN.matcher(Files.readString(p));
                while (m.find()) {
                    String icono = m.group();
                    if (!NO_ICONOS.contains(icono) && !css.contains(".ph." + icono + ":before")) faltan.add(icono + " (" + p.getFileName() + ")");
                }
            }
        }
        assertTrue(faltan.isEmpty(), "Iconos sin regenerar (python scripts/iconos_phosphor.py): " + faltan);
    }
}
