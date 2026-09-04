package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final CatalogoProductoRepository catalogoProductoRepository;

    public ProductoService(CatalogoProductoRepository catalogoProductoRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
    }

    public List<CatalogoProducto> obtenerTodos() {
        return catalogoProductoRepository.findAll();
    }

    public Optional<CatalogoProducto> obtenerPorId(String id) {
        return catalogoProductoRepository.findById(id);
    }

    public CatalogoProducto obtenerPorIdOrNull(String id) {
        return catalogoProductoRepository.findById(id).orElse(null);
    }

    public List<CatalogoProducto> obtenerDestacados() {
        return catalogoProductoRepository.findTop8ByOrderByIdDesc();
    }

    public List<CatalogoProducto> buscarPorNombre(String query) {
        if (query == null || query.trim().isEmpty()) {
            return obtenerTodos();
        }
        return catalogoProductoRepository.findByNombreContainingIgnoreCase(query);
    }

    public CatalogoProducto guardar(CatalogoProducto producto) {
        return catalogoProductoRepository.save(producto);
    }

    public void eliminarPorId(String id) {
        catalogoProductoRepository.deleteById(id);
    }
}