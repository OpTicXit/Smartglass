package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrito de compras persistido en MongoDB (reemplaza el hash de
 * Redis usado antes). Un carrito por usuario autenticado, indexado
 * por usuarioId para busquedas rapidas.
 */
@Document(collection = "carritos")
public class Carrito {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long usuarioId;

    private List<ItemCarrito> items = new ArrayList<>();

    public Carrito() {
    }

    public Carrito(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public List<ItemCarrito> getItems() { return items; }
    public void setItems(List<ItemCarrito> items) { this.items = items; }
}