package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Figuras decorativas de vidrio: animales, naturaleza y formas clasicas para vitrina o mesa.
 * Inserta las que falten, por codigo (no duplica ni pisa lo que el admin haya editado).
 * Las imagenes son ilustraciones (scripts/figuras_catalogo.py), no fotos: la vista las rotula asi.
 * El "espesor" es el equivalente que, con ancho x alto, da la masa de la pieza, como en las esculturas.
 */
@Component
@Order(2)
public class CatalogoFigurasSeeder implements CommandLineRunner {

    private final CatalogoProductoRepository repositorio;

    public CatalogoFigurasSeeder(CatalogoProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    private static List<CatalogoProducto> figuras() {
        return List.of(
                figura("FIG-A-01", "Cisne de Cristal", "cisne", "mediana", 140_000, 18.0, 16.0, 8.0, "Cisne de vidrio transparente con el cuello en curva. Una pieza clásica para vitrinas, mesas de centro y regalos de aniversario."),
                figura("FIG-A-02", "Elefante de la Suerte", "elefante", "pequena", 95_000, 12.0, 9.0, 7.0, "Elefante de vidrio con la trompa en alto, un símbolo tradicional de buena fortuna para repisas y escritorios."),
                figura("FIG-A-03", "Delfín en Salto", "delfin", "mediana", 130_000, 20.0, 15.0, 6.0, "Delfín de vidrio azul en pleno salto, sobre una base del mismo material. Da movimiento a repisas y vitrinas."),
                figura("FIG-A-04", "Mariposa de Vitral", "mariposa", "pequena", 85_000, 12.0, 10.0, 2.0, "Mariposa de vidrio con alas en tono violeta. Se apoya sobre su base o se coloca junto a una ventana para que la atraviese la luz."),
                figura("FIG-A-05", "Búho Sabio", "buho", "pequena", 90_000, 8.0, 11.0, 7.0, "Búho de vidrio ámbar con grandes ojos. Una figura de escritorio para bibliotecas, oficinas y estudios."),
                figura("FIG-A-06", "Pez Tropical", "pez", "pequena", 80_000, 14.0, 9.0, 4.0, "Pez de vidrio en tonos turquesa, con aletas transparentes. Fresco y luminoso para baños, cocinas y repisas."),
                figura("FIG-N-01", "Rosa Eterna", "rosa", "pequena", 110_000, 8.0, 18.0, 6.0, "Rosa de vidrio con pétalos en capas sobre un tallo macizo. Un detalle que no se marchita para fechas especiales."),
                figura("FIG-N-02", "Tulipán de Primavera", "tulipan", "pequena", 95_000, 7.0, 20.0, 5.0, "Tulipán de vidrio rosado con hojas en el tallo. Se luce solo o en un florero con varios tonos."),
                figura("FIG-N-03", "Árbol de la Vida", "arbol", "mediana", 190_000, 22.0, 26.0, 4.0, "Disco de vidrio con un árbol grabado y una base recta. Una pieza simbólica para salas, consultorios y recepciones."),
                figura("FIG-N-04", "Concha Marina", "concha", "pequena", 85_000, 14.0, 12.0, 6.0, "Concha de vidrio con nervaduras en relieve que reparten la luz. Ideal como centro de mesa pequeño o portajoyas."),
                figura("FIG-C-01", "Corazón Facetado", "corazon", "pequena", 75_000, 10.0, 10.0, 4.0, "Corazón macizo con caras talladas que reflejan la luz. Un recuerdo clásico para aniversarios y bodas."),
                figura("FIG-C-02", "Estrella de Cristal", "estrella", "pequena", 70_000, 11.0, 11.0, 3.0, "Estrella de cinco puntas con facetas. Sirve de adorno de mesa, reconocimiento o detalle para la temporada navideña."),
                figura("FIG-C-03", "Esfera Luminosa", "esfera", "mediana", 150_000, 12.0, 12.0, 8.0, "Esfera maciza de vidrio transparente que invierte y amplía lo que tiene detrás. Para escritorios, recepciones y fotografía."),
                figura("FIG-C-04", "Jarrón Soplado", "jarron", "mediana", 210_000, 16.0, 28.0, 5.0, "Jarrón de vidrio soplado de perfil clásico con reflejos verde agua. Funciona con flores o como pieza decorativa por sí solo."),
                figura("FIG-C-05", "Ángel Guardián", "angel", "pequena", 120_000, 14.0, 16.0, 3.0, "Ángel de vidrio con alas abiertas y túnica de pliegues. Un obsequio habitual para bautizos, nacimientos y recuerdos familiares.")
        );
    }

    private static CatalogoProducto figura(String codigo, String nombre, String dibujo, String tamano, double precio,
                                           Double ancho, Double alto, Double espesorEquivalente, String descripcion) {
        CatalogoProducto p = new CatalogoProducto(nombre, null, precio, codigo, descripcion, "figura", ancho, alto, espesorEquivalente);
        p.setCategoria(CatalogoProducto.CATEGORIA_FIGURA);
        p.setTamano(tamano);
        p.setImagenPrincipal(CatalogoProducto.CARPETA_FIGURAS + dibujo + ".svg");
        return p;
    }

    @Override
    public void run(String... args) {
        int nuevas = 0;
        for (CatalogoProducto f : figuras()) {
            if (repositorio.findFirstByCodigo(f.getCodigo()).isEmpty()) {
                repositorio.save(f);
                nuevas++;
            }
        }
        if (nuevas > 0) System.out.println("✅ Catalogo: " + nuevas + " figura(s) decorativa(s) nueva(s).");
    }
}
