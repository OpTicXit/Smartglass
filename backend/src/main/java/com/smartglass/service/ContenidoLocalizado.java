package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * U1 · Datos que vienen de la base en un solo idioma, mostrados en el idioma activo.
 * Las plantillas lo usan como ${@contenido.nombre(producto)}.
 *
 * - Productos: nombreEn / descripcionEn en ingles; en espanol (es-CO y es-ES) el texto de siempre.
 *   Los productos copiados en un carrito antes de U1 no traen nombreEn: se busca el producto por id.
 * - Obras a medida en el carrito ("Templado · obra a medida"): se rearma con el tipo traducido.
 * - Recomendacion del analisis: AnalisisService guarda una de 7 frases fijas en espanol; aqui se
 *   cambia por su clave en messages*.properties (sin tocar el calculo).
 */
@Component("contenido")
public class ContenidoLocalizado {

    private static final String SUFIJO_OBRA = " · obra a medida";

    /** Frases de AnalisisService.generarRecomendacion / validarEspesorParaUso -> clave i18n. */
    private static final Map<String, String> RECOMENDACIONES = Map.of(
            "Para mobiliario se recomienda un espesor entre 6 y 8 mm.", "rec.espesor_mobiliario",
            "Para ventanas se recomienda un espesor entre 6 y 10 mm.", "rec.espesor_ventana",
            "Para divisiones de baño/ducha se recomienda un espesor entre 8 y 10 mm por seguridad.", "rec.espesor_ducha",
            "Para barandas o pisos de vidrio se recomienda un espesor entre 12 y 25 mm.", "rec.espesor_baranda",
            "Considera usar vidrio reciclado para reducir significativamente la huella de CO2.", "rec.usar_reciclado",
            "Optar por energia renovable en la fabricacion reduciria aun mas el impacto ambiental.", "rec.usar_renovable",
            "Esta configuracion ya tiene un perfil ambiental favorable.", "rec.favorable");

    private final ProductoService productos;
    private final MessageSource mensajes;

    public ContenidoLocalizado(ProductoService productos, MessageSource mensajes) {
        this.productos = productos;
        this.mensajes = mensajes;
    }

    private static boolean enIngles() {
        Locale l = LocaleContextHolder.getLocale();
        return l != null && "en".equals(l.getLanguage());
    }

    private String msg(String clave, Object... args) {
        return mensajes.getMessage(clave, args, null, LocaleContextHolder.getLocale());
    }

    public String nombre(CatalogoProducto p) {
        if (p == null) return "";
        String base = p.getNombre() == null ? "" : p.getNombre();
        if (base.endsWith(SUFIJO_OBRA)) {           // obra a medida en el carrito
            String tipo = p.getTipo() == null ? base.substring(0, base.length() - SUFIJO_OBRA.length()) : p.getTipo();
            String tipoTxt = msg("vidrio.tipo." + tipo);
            String obra = msg("contenido.obra_a_medida", tipoTxt != null ? tipoTxt : tipo);
            return obra != null ? obra : base;
        }
        if (!enIngles()) return base;
        String en = p.getNombreEn();
        if (vacio(en) && p.getId() != null) {
            CatalogoProducto real = productos.obtenerPorIdOrNull(p.getId());
            if (real != null) en = real.getNombreEn();
        }
        return vacio(en) ? base : en;
    }

    /** Linea de un pedido: guarda el nombre del momento de la compra; se muestra en el idioma activo. */
    public String nombrePedido(com.smartglass.model.mysql.DetallePedido d) {
        if (d == null) return "";
        String base = d.getNombreProducto() == null ? "" : d.getNombreProducto();
        if (base.endsWith(SUFIJO_OBRA)) {
            String tipo = base.substring(0, base.length() - SUFIJO_OBRA.length());
            String tipoTxt = msg("vidrio.tipo." + tipo);
            String obra = msg("contenido.obra_a_medida", tipoTxt != null ? tipoTxt : tipo);
            return obra != null ? obra : base;
        }
        if (!enIngles() || d.getProductoId() == null) return base;
        CatalogoProducto real = productos.obtenerPorIdOrNull(d.getProductoId());
        return real != null && !vacio(real.getNombreEn()) ? real.getNombreEn() : base;
    }

    public String descripcion(CatalogoProducto p) {
        if (p == null) return "";
        if (!enIngles()) return p.getDescripcion();
        String en = p.getDescripcionEn();
        if (vacio(en) && p.getId() != null) {
            CatalogoProducto real = productos.obtenerPorIdOrNull(p.getId());
            if (real != null) en = real.getDescripcionEn();
        }
        return vacio(en) ? p.getDescripcion() : en;
    }

    public String descripcionCorta(CatalogoProducto p) {
        return CatalogoProducto.recortar(descripcion(p));
    }

    /** Texto guardado por AnalisisService -> idioma activo (si no es una frase conocida, se deja tal cual). */
    public String recomendacion(String texto) {
        if (texto == null) return null;
        String clave = RECOMENDACIONES.get(texto.trim());
        if (clave == null) return texto;
        String t = msg(clave);
        return t != null ? t : texto;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}
