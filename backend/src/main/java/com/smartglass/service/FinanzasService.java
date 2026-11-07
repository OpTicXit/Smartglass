package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.repository.mysql.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cifras de dinero del panel (Resumen y Finanzas), sumadas de los pedidos guardados en cada consulta.
 * Los pedidos CANCELADOS no cuentan como ingreso. "Por cobrar" = PENDIENTE + PROCESANDO.
 */
@Service
public class FinanzasService {

    static final int MESES = 12;
    static final int RECIENTES = 12;

    private final PedidoRepository pedidos;

    public FinanzasService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    /** Ingresos de un mes; clave "2026-11". */
    public record Mes(String clave, double ingresos, int pedidos) {}

    /** variacionMes: % frente al mes anterior, null si el mes anterior no tuvo ingresos. */
    public record Resumen(double ingresos, double ingresosMes, Double variacionMes, double ticketPromedio,
                          long pedidos, long porAtender, double porCobrar, double entregado,
                          double compensacion, int pctCompensados, List<Mes> meses,
                          Map<String, Double> porMetodo, Map<String, Long> porEstado, List<Pedido> recientes) {}

    public Resumen resumen() {
        return calcular(pedidos.findAll(), LocalDate.now(CatalogoProducto.ZONA));
    }

    static Resumen calcular(List<Pedido> todos, LocalDate hoy) {
        List<Pedido> validos = todos.stream().filter(p -> !"CANCELADO".equals(p.getEstado())).toList();
        double ingresos = validos.stream().mapToDouble(Pedido::getTotal).sum();

        Map<String, double[]> porMes = new LinkedHashMap<>();
        YearMonth actual = YearMonth.from(hoy);
        for (int i = MESES - 1; i >= 0; i--) porMes.put(actual.minusMonths(i).toString(), new double[2]);
        for (Pedido p : validos) {
            if (p.getFechaPedido() == null) continue;
            double[] m = porMes.get(YearMonth.from(p.getFechaPedido()).toString());
            if (m != null) { m[0] += p.getTotal(); m[1]++; }
        }
        List<Mes> meses = new ArrayList<>();
        porMes.forEach((k, v) -> meses.add(new Mes(k, redondear(v[0]), (int) v[1])));
        double esteMes = meses.get(MESES - 1).ingresos(), anterior = meses.get(MESES - 2).ingresos();
        Double variacion = anterior > 0 ? Math.round((esteMes - anterior) / anterior * 1000) / 10.0 : null;

        Map<String, Double> porMetodo = new LinkedHashMap<>();
        porMetodo.put("TARJETA_CREDITO", 0.0);
        porMetodo.put("PSE", 0.0);
        for (Pedido p : validos) porMetodo.merge(p.getMetodoPago() == null ? "OTRO" : p.getMetodoPago(), p.getTotal(), Double::sum);
        porMetodo.replaceAll((k, v) -> redondear(v));

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String e : List.of("PENDIENTE", "PROCESANDO", "ENVIADO", "ENTREGADO", "CANCELADO")) porEstado.put(e, 0L);
        for (Pedido p : todos) if (p.getEstado() != null) porEstado.merge(p.getEstado(), 1L, Long::sum);

        double porCobrar = sumar(validos, "PENDIENTE", "PROCESANDO");
        long compensados = validos.stream().filter(Pedido::isCompensacionCarbono).count();
        List<Pedido> recientes = todos.stream()
                .sorted(Comparator.comparing(Pedido::getFechaPedido, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECIENTES).toList();

        return new Resumen(redondear(ingresos), esteMes, variacion,
                validos.isEmpty() ? 0 : redondear(ingresos / validos.size()),
                todos.size(), porEstado.get("PENDIENTE") + porEstado.get("PROCESANDO"),
                redondear(porCobrar), redondear(sumar(validos, "ENTREGADO")),
                redondear(validos.stream().mapToDouble(Pedido::getRecargoCompensacion).sum()),
                validos.isEmpty() ? 0 : (int) Math.round(100.0 * compensados / validos.size()),
                meses, porMetodo, porEstado, recientes);
    }

    private static double sumar(List<Pedido> lista, String... estados) {
        List<String> buscados = List.of(estados);
        return lista.stream().filter(p -> buscados.contains(Objects.toString(p.getEstado(), "")))
                .mapToDouble(Pedido::getTotal).sum();
    }

    private static double redondear(double v) { return Math.round(v * 100) / 100.0; }
}
