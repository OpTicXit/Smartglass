package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import com.smartglass.service.EsquemaPreciosCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * T2 comercio, despues de los seeders del catalogo (@Order 1 y 2). Idempotente:
 *
 * - D3 balanceo: a los productos que nunca se balancearon (esquemaPrecio null) o que
 *   se balancearon con una version anterior del esquema les aplica EsquemaPreciosCatalogo
 *   y guarda el precio original en precioAnterior para poder revertirlo. Solo baja precios. Lo que el admin
 *   crea queda como "manual" y no se toca.
 * - D1 stock inicial: a los productos sin inventario (stock null) les asigna unas
 *   existencias iniciales fijas y plausibles. Es un dato de partida que el admin
 *   ajusta; despues SOLO lo cambian las compras reales o el admin, nunca un azar.
 */
@Component
@Order(3)
public class CatalogoComercioSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoComercioSeeder.class);

    private final CatalogoProductoRepository repositorio;

    public CatalogoComercioSeeder(CatalogoProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Existencias de partida: piezas de autor unicas o cortas, vidrio plano de linea en volumen. */
    static int stockInicial(CatalogoProducto p) {
        if (p.isEscultura()) {
            String t = p.getTamano() == null ? "" : p.getTamano();
            return switch (t) {
                case "grande" -> 1;
                case "mediana" -> 3;
                default -> 8;
            };
        }
        String tipo = p.getTipo() == null ? "" : p.getTipo().trim().toLowerCase();
        return switch (tipo) {
            case "inteligente" -> 6;
            case "insulado", "curvo", "tratado", "control solar", "solar" -> 15;
            default -> 40;
        };
    }

    @Override
    public void run(String... args) {
        List<CatalogoProducto> cambiados = new ArrayList<>();
        int balanceados = 0, conStock = 0;
        for (CatalogoProducto p : repositorio.findAll()) {
            boolean cambio = false;
            if (p.getEsquemaPrecio() == null || EsquemaPreciosCatalogo.esVersionAnterior(p.getEsquemaPrecio())) {
                // precioAnterior guarda SIEMPRE el precio original, previo a cualquier balanceo;
                // cada version del esquema se recalcula desde ese original, no desde la anterior
                double original = p.getPrecioAnterior() != null ? p.getPrecioAnterior() : p.getPrecio();
                var sugerido = EsquemaPreciosCatalogo.precioBalanceado(p, original);
                if (sugerido.isPresent()) {
                    p.setPrecioAnterior(original);
                    p.setPrecio(sugerido.get());
                    balanceados++;
                }
                // sin datos suficientes se marca igual: no se reintenta en cada arranque
                p.setEsquemaPrecio(sugerido.isPresent() ? EsquemaPreciosCatalogo.VERSION : "manual");
                cambio = true;
            }
            if (p.getStock() == null) {
                p.setStock(stockInicial(p));
                conStock++;
                cambio = true;
            }
            if (cambio) cambiados.add(p);
        }
        if (!cambiados.isEmpty()) {
            repositorio.saveAll(cambiados);
            log.info("Catalogo T2: {} precio(s) balanceado(s) con {}, {} producto(s) con stock inicial",
                    balanceados, EsquemaPreciosCatalogo.VERSION, conStock);
        }
    }
}
