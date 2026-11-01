package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * T2: busqueda del catalogo con filtros de categoria y tamano. Reutiliza
     * buscarPorNombre (sin cambiarla) y filtra en memoria: el catalogo es corto.
     * categoria/tamano vacios = sin filtro; un valor desconocido no coincide con
     * nada (la vista muestra "No hay piezas con ese filtro"). Sin categoria
     * guardada cuenta como vidrio plano (CatalogoProducto.getCategoriaEfectiva).
     */
    public List<CatalogoProducto> buscar(String query, String categoria, String tamano) {
        String cat = categoria == null ? "" : categoria.trim().toLowerCase();
        String tam = tamano == null ? "" : tamano.trim().toLowerCase();
        return buscarPorNombre(query).stream()
                .filter(p -> cat.isEmpty() || cat.equals(p.getCategoriaEfectiva()))
                .filter(p -> tam.isEmpty() || tam.equals(p.getTamano()))
                .toList();
    }

    /** Cuantos productos hay por categoria efectiva (para los contadores del filtro). */
    public Map<String, Long> contarPorCategoria() {
        return obtenerTodos().stream()
                .collect(Collectors.groupingBy(CatalogoProducto::getCategoriaEfectiva, Collectors.counting()));
    }

    public CatalogoProducto guardar(CatalogoProducto producto) {
        return catalogoProductoRepository.save(producto);
    }

    public void eliminarPorId(String id) {
        catalogoProductoRepository.deleteById(id);
    }
}