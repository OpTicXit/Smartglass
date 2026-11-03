package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.Resena;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import com.smartglass.repository.mongo.ResenaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera reseñas simuladas para que el catalogo no se vea vacio de
 * opiniones. Se ejecuta despues de los seeders del catalogo (@Order) y
 * solo para los productos sin reseñas simuladas -- asi los productos
 * nuevos reciben las suyas y no se duplica en cada arranque.
 *
 * usuarioId usa numeros negativos (-1, -2, ...) para no chocar nunca
 * con los id reales de MySQL (todos positivos, autoincrementales) --
 * asi se pueden distinguir reseñas simuladas de reseñas reales si
 * hiciera falta despues.
 */
@Component
@Order(4)
public class ResenaSeeder implements CommandLineRunner {

    private static final int RESENAS_POR_PRODUCTO = 40;

    private final CatalogoProductoRepository catalogoProductoRepository;
    private final ResenaRepository resenaRepository;
    private final Random random = new Random();

    private final String[] nombres = {
            "Juan", "María", "Carlos", "Ana", "Luis", "Camila", "Andrés", "Valentina",
            "Diego", "Sofía", "Javier", "Daniela", "Miguel", "Laura", "Fernando", "Paula",
            "Ricardo", "Carolina", "Sebastián", "Gabriela", "Alejandro", "Natalia", "Jorge",
            "Isabella", "Óscar", "Manuela", "Felipe", "Juliana", "Santiago", "Mariana"
    };

    private final String[] apellidos = {
            "Gómez", "Rodríguez", "Martínez", "López", "García", "Pérez", "Sánchez",
            "Ramírez", "Torres", "Flores", "Rivera", "Gutiérrez", "Vargas", "Castro",
            "Rojas", "Morales", "Ortiz", "Jiménez", "Reyes", "Mendoza"
    };

    private final String[] comentarios5 = {
            "Excelente calidad, superó mis expectativas.",
            "El vidrio llegó perfecto y a tiempo, muy recomendado.",
            "Justo lo que necesitaba para mi proyecto, acabado impecable.",
            "Atención al cliente excelente y producto de primera.",
            "Se nota la calidad industrial, lo volvería a comprar.",
            "Instalación sin problemas, el corte fue exacto.",
            "Muy satisfecho con la transparencia y el acabado.",
            "El mejor proveedor de vidrio que he probado.",
            "Cumplió exactamente con las medidas que pedí.",
            "Precio justo para la calidad que ofrece.",
            "Se ve espectacular en mi sala, gran compra.",
            "Producto robusto y bien empacado, sin rayones.",
            "La huella de carbono declarada me dio confianza en la marca.",
            "Rápido despacho y excelente terminado.",
            "Superó lo que esperaba por el precio."
    };

    private final String[] comentarios4 = {
            "Muy buen producto, aunque la entrega tardó un par de días más de lo esperado.",
            "Buena calidad, el empaque podría mejorar un poco.",
            "Cumple bien su función, aunque esperaba un poco más de brillo.",
            "Buen precio, solo un pequeño rayón en una esquina.",
            "En general conforme, la atención pudo ser más rápida.",
            "Buen vidrio, aunque el color es ligeramente distinto a la foto.",
            "Sólido y bien hecho, tardó algo en llegar.",
            "Cumple, aunque el instructivo de instalación es escaso.",
            "Buena relación calidad-precio.",
            "Todo bien, solo la caja llegó algo golpeada por fuera."
    };

    private final String[] comentarios3 = {
            "Producto correcto, nada extraordinario.",
            "Cumple lo básico, esperaba mejor terminado.",
            "Está bien pero el tiempo de entrega fue largo.",
            "Ni bueno ni malo, hace lo que promete.",
            "El precio es un poco alto para lo que ofrece.",
            "Aceptable, aunque tuve que pedir ayuda para instalarlo.",
            "Cumple la función pero llegó con retraso.",
            "Producto estándar, sin nada que lo destaque."
    };

    private final String[] comentariosBajos = {
            "Llegó con una fisura pequeña, tuve que reclamar.",
            "El tiempo de entrega fue mucho más largo de lo prometido.",
            "Las medidas no coincidieron exactamente con lo pedido.",
            "Esperaba mejor calidad por el precio pagado.",
            "El servicio al cliente tardó en responder mi reclamo.",
            "No quedé satisfecho con el acabado final.",
            "Tuve problemas para agendar la instalación."
    };

    // Esculturas y figuras: no se instalan ni se cortan a medida, y sus opiniones hablan de la pieza.
    private final String[] decorativos5 = {
            "Es más bonita en persona que en la imagen.",
            "Refleja la luz de la ventana y cambia durante todo el día.",
            "Se ve espectacular en la sala, todos preguntan por ella.",
            "Acabado impecable, se nota el cuidado en cada detalle.",
            "Llegó perfectamente protegida, sin un solo rayón.",
            "Era justo el detalle que le faltaba a ese espacio.",
            "Muy buena calidad del vidrio, tiene un brillo precioso.",
            "Fue un regalo y a la persona le encantó.",
            "Se convirtió en la pieza favorita de la casa.",
            "Con la luz de la tarde se ve increíble.",
            "Mejor de lo que esperaba por el precio.",
            "Despacho rápido y muy bien empacada.",
            "Se siente sólida, nada frágil como temía.",
            "Da un toque elegante sin recargar el lugar.",
            "Ya es la segunda que compro, repetiría sin dudarlo.",
            "Los detalles están muy bien logrados.",
            "Muy contento con la compra, es tal como la describen.",
            "Me gustó poder ver la huella de carbono de la pieza antes de comprarla.",
            "Quedó preciosa en el lugar que le tenía pensado.",
            "Un trabajo muy fino, se nota la calidad.",
            "Quien la ve pregunta dónde la compré."
    };

    private final String[] decorativos4 = {
            "Muy bonita, aunque la entrega tardó un par de días más.",
            "Buen acabado; la caja llegó algo golpeada, pero la pieza intacta.",
            "Me gustó mucho, aunque la imaginaba un poco más grande.",
            "Bonita y bien hecha, el empaque podría ser mejor.",
            "Buena pieza por el precio, aunque el tono es algo distinto a la imagen.",
            "Cumple lo que promete, solo faltó una guía para limpiarla.",
            "Se ve muy bien, aunque marca las huellas de los dedos con facilidad.",
            "Muy linda, aunque esperaba que brillara un poco más.",
            "Buena relación calidad-precio.",
            "Todo bien, solo tardaron en confirmar el despacho.",
            "Bien lograda, aunque tiene una marca mínima en un costado."
    };

    private final String[] decorativos3 = {
            "Es bonita, pero nada extraordinario.",
            "Correcta para el precio, esperaba un acabado más fino.",
            "Se ve bien, pero la entrega fue larga.",
            "Ni buena ni mala, cumple como adorno.",
            "El precio me pareció un poco alto para lo que es.",
            "Más pequeña de lo que imaginaba.",
            "Aceptable, aunque el color me gustaba más en la imagen."
    };

    private final String[] decorativosBajos = {
            "Llegó con una pequeña fisura, tuve que reclamar.",
            "La entrega tardó mucho más de lo prometido.",
            "No se parece tanto a la imagen como esperaba.",
            "Esperaba mejor calidad por lo que pagué.",
            "El servicio al cliente tardó en responder mi reclamo.",
            "El acabado tiene marcas que no me convencieron."
    };

    public ResenaSeeder(CatalogoProductoRepository catalogoProductoRepository,
                         ResenaRepository resenaRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    public void run(String... args) {
        List<Resena> todas = new ArrayList<>();

        for (CatalogoProducto producto : catalogoProductoRepository.findAll()) {
            if (resenaRepository.existsByProductoIdAndUsuarioIdLessThan(producto.getId(), 0L)) {
                continue;
            }
            boolean decorativo = producto.isEscultura() || producto.isFigura();
            int desfase = random.nextInt(100);
            int[] usados = new int[6]; // por estrellas: recorre los comentarios para no repetirlos en un producto

            for (int i = 0; i < RESENAS_POR_PRODUCTO; i++) {
                int calificacion = calificacionAleatoria();
                String[] comentarios = comentariosParaCalificacion(calificacion, decorativo);
                String comentario = comentarios[(desfase + usados[calificacion]++) % comentarios.length];
                String nombreCompleto = nombres[random.nextInt(nombres.length)] + " "
                        + apellidos[random.nextInt(apellidos.length)];

                Resena resena = new Resena(producto.getId(), -(i + 1L), nombreCompleto,
                        calificacion, comentario);
                resena.setFecha(fechaAleatoria());
                todas.add(resena);
            }
        }

        if (todas.isEmpty()) {
            return;
        }
        resenaRepository.saveAll(todas);
        System.out.println("✅ Reseñas simuladas cargadas: " + todas.size()
                + " (" + RESENAS_POR_PRODUCTO + " por producto x " + todas.size() / RESENAS_POR_PRODUCTO + " productos).");
    }

    /**
     * Distribucion realista: mayoria positivas, pocas negativas --
     * como en cualquier tienda con buena reputacion.
     * 5★: 55% | 4★: 25% | 3★: 12% | 2★: 5% | 1★: 3%
     */
    private int calificacionAleatoria() {
        int n = random.nextInt(100);
        if (n < 55) return 5;
        if (n < 80) return 4;
        if (n < 92) return 3;
        if (n < 97) return 2;
        return 1;
    }

    private String[] comentariosParaCalificacion(int calificacion, boolean decorativo) {
        return switch (calificacion) {
            case 5 -> decorativo ? decorativos5 : comentarios5;
            case 4 -> decorativo ? decorativos4 : comentarios4;
            case 3 -> decorativo ? decorativos3 : comentarios3;
            default -> decorativo ? decorativosBajos : comentariosBajos;
        };
    }

    private LocalDateTime fechaAleatoria() {
        return LocalDateTime.now()
                .minusDays(random.nextInt(200))
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));
    }
}