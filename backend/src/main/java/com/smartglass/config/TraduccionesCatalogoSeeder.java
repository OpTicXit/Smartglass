package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * U1 · Nombre y descripcion en ingles de los productos sembrados (por codigo).
 * - Solo rellena los campos vacios: nunca pisa lo que escriba el admin.
 * - Corre despues de los seeders del catalogo; idempotente en cada arranque.
 * - Los productos que cree el admin sin version en ingles se muestran en espanol.
 */
@Component
@Order(100)
public class TraduccionesCatalogoSeeder implements CommandLineRunner {

    private static final Map<String, String[]> EN = Map.ofEntries(
            Map.entry("VID-001", new String[]{"Crackled Tempered Glass", "Tempered or laminated glass with a crackled, shattered effect for high-impact decorative finishes."}),
            Map.entry("VID-002", new String[]{"Clear Float Glass", "Standard clear float glass, the most common base for windows and furniture."}),
            Map.entry("VID-003", new String[]{"Cathedral Patterned Glass", "Patterned or textured glass with a decorative cathedral design."}),
            Map.entry("VID-004", new String[]{"Hermetic Double Glazing (IGU)", "Hermetic double glazing / insulating glass unit for thermal and acoustic efficiency."}),
            Map.entry("VID-005", new String[]{"Curved Glass", "Curved glass for architectural and interior design projects."}),
            Map.entry("VID-006", new String[]{"Laminated Safety Glass", "Laminated safety glass that does not shatter when it breaks."}),
            Map.entry("VID-007", new String[]{"Opal Laminated Glass", "Translucent opal laminated glass, ideal for roofs and skylights."}),
            Map.entry("VID-008", new String[]{"Modern Decorative Stained Glass", "Decorative glass / modern stained glass with coloured panels (blue, green and grey)."}),
            Map.entry("VID-009", new String[]{"Extra-Clear Display Glass", "Extra-clear or tempered glass for display cases and lit showcases."}),
            Map.entry("VID-010", new String[]{"Flat Mirror", "Flat mirror / general-purpose mirrored glass."}),
            Map.entry("VID-011", new String[]{"Coloured Textured Glass", "Coloured textured or patterned glass (white, blue, yellow, green)."}),
            Map.entry("VID-012", new String[]{"IGU Double Glazing", "Double glazing (IGU) with an insulating spacer profile."}),
            Map.entry("VID-013", new String[]{"Electrochromic Smart Glass", "Smart / electrochromic glass that changes tint for adjustable privacy."}),
            Map.entry("VID-014", new String[]{"Anti-Burglary Security Glass", "Impact-resistant laminated security glass, tested against break-in attempts."}),
            Map.entry("VID-015", new String[]{"Self-Cleaning Hydrophobic Glass", "Glass with a hydrophobic or self-cleaning treatment."}),
            Map.entry("VID-016", new String[]{"Opaque Lacquered Glass", "Lacquered / enamelled opaque glass in a solid colour."}),
            Map.entry("VID-017", new String[]{"Large-Format Curved Glass", "Large-format curved glass for architecture and interiors."}),
            Map.entry("VID-018", new String[]{"Tinted Partition Screen", "Coloured tempered glass / tinted partition screens (green, blue, red)."}),
            Map.entry("VID-019", new String[]{"Reflective Solar Control Glass", "Reflective / solar control glass for building facades."}),
            Map.entry("VID-020", new String[]{"Reflective Solar Mirror", "Concentrating reflective solar mirror or glass, heliostat type."}),
            Map.entry("ESC-G-01", new String[]{"Tempered Wave Sculpture", "A 1,6 m tempered glass wave with a polished edge and steel base. A large-format piece for lobbies, indoor gardens and corporate spaces."}),
            Map.entry("ESC-G-02", new String[]{"Cast Glass Monolith", "A layered cast glass monolith with natural colour veins. A centrepiece for plazas, receptions or double-height halls."}),
            Map.entry("ESC-M-01", new String[]{"Faceted Torso", "A glass torso carved into facets that split the light. For living rooms, offices and display cases."}),
            Map.entry("ESC-M-02", new String[]{"Recycled Spiral", "A spiral of 100 % recycled glass, melted and drawn by hand. Each piece keeps unique bubbles and shades."}),
            Map.entry("ESC-P-01", new String[]{"Crystal Drop", "A solid drop of extra-clear glass. A paperweight or desk piece that casts sparkles in daylight."}),
            Map.entry("ESC-P-02", new String[]{"Table Prism", "An optical glass prism that splits light into rainbows. A decorative piece for a table, shelf or studio."}),
            Map.entry("ESC-P-03", new String[]{"Blown Hummingbird", "A hand-blown, hot-coloured glass hummingbird on a recycled glass base. Artisan edition."})
    );

    private final CatalogoProductoRepository repositorio;

    public TraduccionesCatalogoSeeder(CatalogoProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        List<CatalogoProducto> cambiados = new ArrayList<>();
        for (CatalogoProducto p : repositorio.findAll()) {
            String[] en = p.getCodigo() == null ? null : EN.get(p.getCodigo());
            if (en == null) continue;
            boolean cambio = false;
            if (vacio(p.getNombreEn())) { p.setNombreEn(en[0]); cambio = true; }
            if (vacio(p.getDescripcionEn())) { p.setDescripcionEn(en[1]); cambio = true; }
            if (cambio) cambiados.add(p);
        }
        if (!cambiados.isEmpty()) {
            repositorio.saveAll(cambiados);
            System.out.println("[i18n] Traducciones del catalogo: " + cambiados.size() + " productos.");
        }
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}
