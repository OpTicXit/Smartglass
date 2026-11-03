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
            Map.entry("ESC-P-03", new String[]{"Blown Hummingbird", "A hand-blown, hot-coloured glass hummingbird on a recycled glass base. Artisan edition."}),
            Map.entry("FIG-A-01", new String[]{"Crystal Swan", "A clear glass swan with a curved neck. A classic piece for display cases, coffee tables and anniversary gifts."}),
            Map.entry("FIG-A-02", new String[]{"Lucky Elephant", "A glass elephant with its trunk raised, a traditional symbol of good fortune for shelves and desks."}),
            Map.entry("FIG-A-03", new String[]{"Leaping Dolphin", "A blue glass dolphin in mid-leap on a base of the same material. It brings movement to shelves and display cases."}),
            Map.entry("FIG-A-04", new String[]{"Stained-Glass Butterfly", "A glass butterfly with violet wings. It stands on its base or sits by a window so the light passes through."}),
            Map.entry("FIG-A-05", new String[]{"Wise Owl", "An amber glass owl with large eyes. A desk figure for libraries, offices and studies."}),
            Map.entry("FIG-A-06", new String[]{"Tropical Fish", "A turquoise glass fish with clear fins. Fresh and bright for bathrooms, kitchens and shelves."}),
            Map.entry("FIG-N-01", new String[]{"Everlasting Rose", "A glass rose with layered petals on a solid stem. A gift that never wilts for special occasions."}),
            Map.entry("FIG-N-02", new String[]{"Spring Tulip", "A pink glass tulip with leaves on its stem. It stands out alone or in a vase with other shades."}),
            Map.entry("FIG-N-03", new String[]{"Tree of Life", "A glass disc engraved with a tree on a straight base. A symbolic piece for living rooms, clinics and receptions."}),
            Map.entry("FIG-N-04", new String[]{"Seashell", "A glass shell with raised ribs that spread the light. Ideal as a small centrepiece or jewellery holder."}),
            Map.entry("FIG-C-01", new String[]{"Faceted Heart", "A solid heart with carved faces that reflect the light. A classic keepsake for anniversaries and weddings."}),
            Map.entry("FIG-C-02", new String[]{"Crystal Star", "A five-pointed faceted star. A table ornament, an award or a holiday season detail."}),
            Map.entry("FIG-C-03", new String[]{"Luminous Sphere", "A solid clear glass sphere that inverts and enlarges what lies behind it. For desks, receptions and photography."}),
            Map.entry("FIG-C-04", new String[]{"Blown Vase", "A blown glass vase with a classic profile and sea-green reflections. It works with flowers or as a piece on its own."}),
            Map.entry("FIG-C-05", new String[]{"Guardian Angel", "A glass angel with open wings and a pleated robe. A common gift for baptisms, births and family keepsakes."})
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
