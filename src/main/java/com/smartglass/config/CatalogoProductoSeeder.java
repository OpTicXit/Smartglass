package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga el catalogo inicial de productos en MongoDB, igual que
 * DataInitializer hace con el usuario admin en MySQL. Solo inserta
 * si la coleccion esta vacia, para no duplicar en cada arranque.
 *
 * SUPUESTO: precio, codigo, ancho/alto/espesor son valores de
 * relleno razonables -- el usuario solo dio nombre + descripcion +
 * archivo de imagen para cada producto. Ajustalos a los reales
 * cuando los tengas (por Mongo Compass, un endpoint admin, etc.).
 */
@Component
public class CatalogoProductoSeeder implements CommandLineRunner {

    private final CatalogoProductoRepository catalogoProductoRepository;

    public CatalogoProductoSeeder(CatalogoProductoRepository catalogoProductoRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
    }

    @Override
    public void run(String... args) {
        if (catalogoProductoRepository.count() > 0) {
            return;
        }

        List<CatalogoProducto> productos = List.of(
                producto("Vidrio Templado Craquelado", "vidrio1.jpg", 180000, "VID-001",
                        "Vidrio templado o laminado con efecto craquelado / roto, para acabados decorativos de alto impacto.",
                        "templado", 100.0, 100.0, 8.0),
                producto("Vidrio Flotado Transparente", "vidrio2.jpg", 90000, "VID-002",
                        "Vidrio flotado transparente estandar, la base mas comun para ventanas y muebles.",
                        "crudo", 100.0, 100.0, 6.0),
                producto("Vidrio Impreso Catedral", "vidrio3.jpg", 110000, "VID-003",
                        "Vidrio impreso o texturizado con patron decorativo tipo catedral.",
                        "decorativo", 100.0, 100.0, 5.0),
                producto("Doble Vidriado Hermetico (DVH)", "vidrio4.jpg", 320000, "VID-004",
                        "Doble vidriado hermetico (DVH) / acristalamiento aislante para eficiencia termica y acustica.",
                        "insulado", 120.0, 150.0, 24.0),
                producto("Vidrio Curvo", "vidrio5.jpg", 450000, "VID-005",
                        "Vidrio curvo para proyectos arquitectonicos y de diseño de interiores.",
                        "curvo", 150.0, 100.0, 10.0),
                producto("Vidrio Laminado de Seguridad", "vidrio6.jpg", 200000, "VID-006",
                        "Vidrio laminado de seguridad, no se astilla al romperse.",
                        "laminado", 100.0, 100.0, 8.0),
                producto("Vidrio Laminado Opal", "vidrio7.jpg", 210000, "VID-007",
                        "Vidrio laminado translucido / opal, ideal para techos o tragaluces.",
                        "laminado", 120.0, 120.0, 10.0),
                producto("Vitral Decorativo Moderno", "vidrio8.jpg", 380000, "VID-008",
                        "Vidrio decorativo / vitral moderno con paneles de colores (azul, verde y gris).",
                        "decorativo", 100.0, 150.0, 6.0),
                producto("Vidrio Extraclaro para Vitrinas", "vidrio9.jpg", 150000, "VID-009",
                        "Vidrio extraclaro o templado para vitrinas / expositores de iluminacion.",
                        "templado", 100.0, 100.0, 6.0),
                producto("Espejo Plano", "vidrio10.jpg", 95000, "VID-010",
                        "Espejo plano / vidrio espejado de uso general.",
                        "espejo", 100.0, 100.0, 4.0),
                producto("Vidrios Texturizados de Colores", "vidrio11.jpg", 130000, "VID-011",
                        "Vidrios texturizados o impresos de colores (blanco, azul, amarillo, verde).",
                        "decorativo", 100.0, 100.0, 5.0),
                producto("Doble Acristalamiento DVH", "vidrio12.jpg", 300000, "VID-012",
                        "Doble acristalamiento o DVH, esquema de perfil aislante.",
                        "insulado", 120.0, 150.0, 24.0),
                producto("Vidrio Inteligente Electrocromico", "vidrio13.jpg", 950000, "VID-013",
                        "Vidrio inteligente / electrocromico, con cambio de tonalidad para privacidad variable.",
                        "inteligente", 100.0, 100.0, 10.0),
                producto("Vidrio de Seguridad Antirrobo", "vidrio14.jpg", 260000, "VID-014",
                        "Vidrio de seguridad laminado resistente a impactos, probado contra intentos de robo.",
                        "laminado", 100.0, 100.0, 12.0),
                producto("Vidrio Hidrofobico Autolimpiante", "vidrio15.jpg", 220000, "VID-015",
                        "Vidrio con tratamiento hidrofobico o autolimpiante.",
                        "tratado", 100.0, 100.0, 6.0),
                producto("Vidrio Lacado Opaco", "vidrio16.jpg", 175000, "VID-016",
                        "Vidrio lacado / esmaltado opaco, de color solido.",
                        "decorativo", 100.0, 100.0, 6.0),
                producto("Vidrio Curvo Gran Formato", "vidrio17.jpg", 600000, "VID-017",
                        "Vidrio curvo de gran formato para arquitectura e interiores.",
                        "curvo", 200.0, 150.0, 12.0),
                producto("Mampara Divisoria Tintada", "vidrio18.jpg", 240000, "VID-018",
                        "Vidrio templado de color / mamparas divisorias tintadas (verde, azul, rojo).",
                        "templado", 100.0, 200.0, 8.0),
                producto("Vidrio Reflectivo Control Solar", "vidrio19.jpg", 280000, "VID-019",
                        "Vidrio reflectivo / control solar para fachadas de edificios.",
                        "control solar", 150.0, 150.0, 8.0),
                producto("Espejo Solar Reflectivo", "vidrio20.jpg", 1200000, "VID-020",
                        "Espejo o vidrio solar reflectivo de concentracion, tipo helioestato.",
                        "solar", 200.0, 200.0, 6.0)
        );

        catalogoProductoRepository.saveAll(productos);
        System.out.println("✅ Catalogo de productos cargado en MongoDB (" + productos.size() + " productos).");
    }

    private CatalogoProducto producto(String nombre, String imagen, double precio, String codigo,
                                       String descripcion, String tipo,
                                       Double ancho, Double alto, Double espesor) {
        return new CatalogoProducto(nombre, imagen, precio, codigo, descripcion, tipo, ancho, alto, espesor);
    }
}