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
 * opiniones. Se ejecuta despues de CatalogoProductoSeeder (@Order) y
 * solo si la coleccion de reseñas esta vacia -- no duplica en cada
 * arranque.
 *
 * usuarioId usa numeros negativos (-1, -2, ...) para no chocar nunca
 * con los id reales de MySQL (todos positivos, autoincrementales) --
 * asi se pueden distinguir reseñas simuladas de reseñas reales si
 * hiciera falta despues.
 */
@Component
@Order(2)
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

    public ResenaSeeder(CatalogoProductoRepository catalogoProductoRepository,
                         ResenaRepository resenaRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    public void run(String... args) {
        if (resenaRepository.count() > 0) {
            return;
        }

        List<CatalogoProducto> productos = catalogoProductoRepository.findAll();
        if (productos.isEmpty()) {
            return; // el catalogo aun no tiene productos que reseñar
        }

        List<Resena> todas = new ArrayList<>();
        long usuarioIdSimulado = -1;

        for (CatalogoProducto producto : productos) {
            for (int i = 0; i < RESENAS_POR_PRODUCTO; i++) {
                int calificacion = calificacionAleatoria();
                String comentario = comentarioParaCalificacion(calificacion);
                String nombreCompleto = nombres[random.nextInt(nombres.length)] + " "
                        + apellidos[random.nextInt(apellidos.length)];

                Resena resena = new Resena(producto.getId(), usuarioIdSimulado, nombreCompleto,
                        calificacion, comentario);
                resena.setFecha(fechaAleatoria());
                todas.add(resena);

                usuarioIdSimulado--;
            }
        }

        resenaRepository.saveAll(todas);
        System.out.println("✅ Reseñas simuladas cargadas: " + todas.size()
                + " (" + RESENAS_POR_PRODUCTO + " por producto x " + productos.size() + " productos).");
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

    private String comentarioParaCalificacion(int calificacion) {
        return switch (calificacion) {
            case 5 -> comentarios5[random.nextInt(comentarios5.length)];
            case 4 -> comentarios4[random.nextInt(comentarios4.length)];
            case 3 -> comentarios3[random.nextInt(comentarios3.length)];
            default -> comentariosBajos[random.nextInt(comentariosBajos.length)];
        };
    }

    private LocalDateTime fechaAleatoria() {
        return LocalDateTime.now()
                .minusDays(random.nextInt(200))
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));
    }
}