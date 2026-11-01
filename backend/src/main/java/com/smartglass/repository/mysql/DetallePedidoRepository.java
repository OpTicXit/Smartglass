package com.smartglass.repository.mysql;

import com.smartglass.model.mysql.DetallePedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * T3: solo LECTURA para "mas vendidos" (no se escribe nada nuevo en el flujo de compra).
 */
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    /** [productoId, unidades vendidas], de mas a menos (los pedidos cancelados no cuentan). */
    @Query("select d.productoId, sum(d.cantidad) from DetallePedido d "
            + "where d.pedido.estado <> 'CANCELADO' and d.productoId is not null "
            + "group by d.productoId order by sum(d.cantidad) desc")
    List<Object[]> masVendidos(Pageable pagina);
}
