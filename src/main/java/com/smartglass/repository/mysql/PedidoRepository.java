package com.smartglass.repository.mysql;

import com.smartglass.model.mysql.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Spring Data resuelve esto como pedido.usuario.id gracias a la
    // relacion @ManyToOne "usuario" en la entidad Pedido.
    List<Pedido> findByUsuarioIdOrderByFechaPedidoDesc(Long usuarioId);

    Optional<Pedido> findByNumeroFactura(String numeroFactura);

    List<Pedido> findByEstado(String estado);

    List<Pedido> findByCompensacionCarbonoTrue();
}
