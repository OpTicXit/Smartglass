package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.Resena;
import com.smartglass.repository.mongo.CatalogoProductoRepository;
import com.smartglass.repository.mongo.ResenaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResenaSeederTest {

    private final CatalogoProductoRepository catalogo = mock(CatalogoProductoRepository.class);
    private final ResenaRepository resenas = mock(ResenaRepository.class);
    private final ResenaSeeder seeder = new ResenaSeeder(catalogo, resenas);

    private static CatalogoProducto producto(String id, String categoria) {
        CatalogoProducto p = new CatalogoProducto();
        p.setId(id);
        p.setCategoria(categoria);
        return p;
    }

    @Test
    @SuppressWarnings("unchecked")
    void soloResenaLosProductosQueNoTienenSimuladas() {
        when(catalogo.findAll()).thenReturn(List.of(producto("con", CatalogoProducto.CATEGORIA_VIDRIO_PLANO), producto("sin", CatalogoProducto.CATEGORIA_FIGURA)));
        when(resenas.existsByProductoIdAndUsuarioIdLessThan("con", 0L)).thenReturn(true);

        seeder.run();

        ArgumentCaptor<List<Resena>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(resenas).saveAll(guardadas.capture());
        List<Resena> lista = guardadas.getValue();
        assertEquals(40, lista.size());
        assertTrue(lista.stream().allMatch(r -> "sin".equals(r.getProductoId()) && r.getUsuarioId() < 0));
        assertEquals(40, lista.stream().map(Resena::getUsuarioId).distinct().count());
        // una figura no se instala ni se corta a medida
        assertFalse(lista.stream().anyMatch(r -> r.getComentario().matches("(?i).*(instal|corte|medidas).*")));
    }

    @Test
    void noGuardaNadaSiTodosTienenSimuladas() {
        when(catalogo.findAll()).thenReturn(List.of(producto("con", CatalogoProducto.CATEGORIA_ESCULTURA)));
        when(resenas.existsByProductoIdAndUsuarioIdLessThan("con", 0L)).thenReturn(true);

        seeder.run();

        verify(resenas, never()).saveAll(any());
    }
}
