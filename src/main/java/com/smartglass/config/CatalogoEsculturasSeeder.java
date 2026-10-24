package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * T2: amplia el catalogo con esculturas de vidrio y enlaza fotos realistas.
 *
 * 1) Esculturas: inserta las que falten, buscando por codigo (idempotente; no
 *    duplica ni pisa las que el admin ya haya editado).
 * 2) Fotos: cada producto tiene un nombre de archivo ESPERADO en
 *    src/main/resources/static/img/. Solo se guarda en imagenPrincipal /
 *    galeria si el archivo existe en el classpath; mientras no exista, la vista
 *    muestra el isotipo (F18) como "foto en preparacion". No se inventan URLs.
 *    Para activar una foto: copiar el archivo con ese nombre y reiniciar.
 *
 * No toca claves ni calculos: tipo = "escultura" no es una clave del
 * configurador (F40) y AnalisisService.calcularHuellaCarbonoItem lo trata con
 * su valor por defecto (densidad 2,5 y factor 1,0). El "espesor" de una
 * escultura es el espesor equivalente que, con ancho x alto, da su masa.
 */
@Component
@Order(2)
public class CatalogoEsculturasSeeder implements CommandLineRunner {

    private final CatalogoProductoRepository repositorio;

    public CatalogoEsculturasSeeder(CatalogoProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Esculturas nuevas: codigo -> datos. Foto principal esperada = codigo en minusculas + ".jpg". */
    private static List<CatalogoProducto> esculturas() {
        return List.of(
                escultura("ESC-G-01", "Escultura Ola Templada", "grande", 4_800_000, 80.0, 160.0, 30.0,
                        "Ola de vidrio templado de 1,6 m de alto, con canto pulido y base de acero. Pieza de gran formato para lobbies, jardines interiores y espacios corporativos."),
                escultura("ESC-G-02", "Monolito de Vidrio Fundido", "grande", 6_200_000, 50.0, 180.0, 40.0,
                        "Monolito de vidrio fundido en capas con vetas de color natural. Una pieza central para plazas, recepciones o halls de doble altura."),
                escultura("ESC-M-01", "Torso Facetado", "mediana", 1_850_000, 40.0, 70.0, 25.0,
                        "Torso de vidrio tallado en facetas que descomponen la luz. Para salas, oficinas y vitrinas de exhibición."),
                escultura("ESC-M-02", "Espiral Reciclada", "mediana", 1_400_000, 45.0, 60.0, 20.0,
                        "Espiral de vidrio 100 % reciclado, fundido y estirado a mano. Cada pieza conserva burbujas y matices únicos."),
                escultura("ESC-P-01", "Gota de Cristal", "pequena", 180_000, 12.0, 18.0, 30.0,
                        "Gota maciza de vidrio extraclaro. Pisapapeles o pieza de escritorio que proyecta destellos con la luz del día."),
                escultura("ESC-P-02", "Prisma de Mesa", "pequena", 240_000, 15.0, 20.0, 25.0,
                        "Prisma de vidrio óptico que dispersa la luz en arcoíris. Pieza decorativa para mesa, repisa o estudio."),
                escultura("ESC-P-03", "Colibrí Soplado", "pequena", 320_000, 18.0, 22.0, 15.0,
                        "Colibrí de vidrio soplado y coloreado en caliente, sobre base de vidrio reciclado. Edición artesanal.")
        );
    }

    /** Galeria esperada de las esculturas: dos vistas extra por pieza (codigo-2.jpg, codigo-3.jpg). */
    private static List<String> galeriaEsperada(String codigo) {
        String base = codigo.toLowerCase();
        return List.of(base + "-2.jpg", base + "-3.jpg");
    }

    /**
     * Fotos realistas esperadas para los productos de vidrio plano existentes
     * (por codigo). Reemplazan visualmente a vidrioN.jpg cuando existan; no cambian
     * ni el codigo ni el tipo del producto.
     */
    private static Map<String, String> fotosRealistas() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("VID-001", "vidrio-templado-craquelado-real.jpg");
        m.put("VID-002", "vidrio-flotado-transparente-real.jpg");
        m.put("VID-003", "vidrio-impreso-catedral-real.jpg");
        m.put("VID-004", "vidrio-dvh-real.jpg");
        m.put("VID-005", "vidrio-curvo-real.jpg");
        m.put("VID-006", "vidrio-laminado-seguridad-real.jpg");
        m.put("VID-007", "vidrio-laminado-opal-real.jpg");
        m.put("VID-008", "vitral-decorativo-real.jpg");
        m.put("VID-009", "vidrio-extraclaro-real.jpg");
        m.put("VID-010", "espejo-plano-real.jpg");
        m.put("VID-011", "vidrio-texturizado-colores-real.jpg");
        m.put("VID-012", "vidrio-doble-acristalamiento-real.jpg");
        m.put("VID-013", "vidrio-inteligente-real.jpg");
        m.put("VID-014", "vidrio-antirrobo-real.jpg");
        m.put("VID-015", "vidrio-autolimpiante-real.jpg");
        m.put("VID-016", "vidrio-lacado-real.jpg");
        m.put("VID-017", "vidrio-curvo-gran-formato-real.jpg");
        m.put("VID-018", "mampara-tintada-real.jpg");
        m.put("VID-019", "vidrio-control-solar-real.jpg");
        m.put("VID-020", "espejo-solar-real.jpg");
        return m;
    }

    @Override
    public void run(String... args) {
        int nuevas = 0, fotos = 0;

        for (CatalogoProducto e : esculturas()) {
            CatalogoProducto actual = repositorio.findFirstByCodigo(e.getCodigo()).orElse(null);
            if (actual == null) {
                actual = repositorio.save(e);
                nuevas++;
            }
            if (enlazarFotos(actual, e.getCodigo().toLowerCase() + ".jpg", galeriaEsperada(e.getCodigo()))) {
                repositorio.save(actual);
                fotos++;
            }
        }

        for (Map.Entry<String, String> foto : fotosRealistas().entrySet()) {
            CatalogoProducto p = repositorio.findFirstByCodigo(foto.getKey()).orElse(null);
            if (p != null && enlazarFotos(p, foto.getValue(), List.of())) {
                repositorio.save(p);
                fotos++;
            }
        }

        if (nuevas > 0 || fotos > 0) {
            System.out.println("✅ Catalogo T2: " + nuevas + " escultura(s) nueva(s), " + fotos + " producto(s) con foto actualizada.");
        }
    }

    /** Pone imagenPrincipal/galeria solo con archivos que existen. Devuelve true si cambio algo. */
    private static boolean enlazarFotos(CatalogoProducto p, String principal, List<String> galeria) {
        boolean cambio = false;
        if (existe(principal) && !principal.equals(p.getImagenPrincipal())) {
            p.setImagenPrincipal(principal);
            cambio = true;
        }
        List<String> presentes = new ArrayList<>();
        for (String g : galeria) if (existe(g)) presentes.add(g);
        if (!presentes.isEmpty() && !Objects.equals(presentes, p.getGaleria())) {
            p.setGaleria(presentes);
            cambio = true;
        }
        return cambio;
    }

    private static boolean existe(String archivo) {
        return new ClassPathResource("static/img/" + archivo).exists();
    }

    private static CatalogoProducto escultura(String codigo, String nombre, String tamano, double precio,
                                              Double ancho, Double alto, Double espesorEquivalente, String descripcion) {
        // imagen = null a proposito: la foto real llega por imagenPrincipal cuando exista el archivo
        CatalogoProducto p = new CatalogoProducto(nombre, null, precio, codigo, descripcion, "escultura",
                ancho, alto, espesorEquivalente);
        p.setCategoria(CatalogoProducto.CATEGORIA_ESCULTURA);
        p.setTamano(tamano);
        return p;
    }
}
