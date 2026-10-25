package com.smartglass.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * U1 · Textos del servidor en el idioma activo (avisos flash, errores de formularios).
 * - t("clave", args): messages*.properties. Los numeros llegan como texto (D2).
 * - traducir(mensaje): los servicios lanzan algunas frases fijas en espanol (las usa tambien la
 *   API REST); aqui se cambian por su clave. Una frase desconocida se devuelve tal cual.
 */
@Component
public class Textos {

    private static final Map<String, String> CONOCIDOS = Map.of(
            "No se pudo completar el registro.", "auth.registro_fallo",
            "Ya existe una cuenta con ese email.", "auth.email_existe",
            "El username o el email ya estan en uso.", "auth.usuario_o_email_en_uso",
            "Telefono invalido. Ej.: +57 300 123 4567", "valid.telefono",
            "Carrito vacío", "checkout.carrito_vacio",
            "La calificación debe estar entre 1 y 5.", "resena.calificacion_rango");

    private final MessageSource mensajes;

    public Textos(MessageSource mensajes) {
        this.mensajes = mensajes;
    }

    public String t(String clave, Object... args) {
        return mensajes.getMessage(clave, args, clave, LocaleContextHolder.getLocale());
    }

    public String traducir(String mensaje) {
        if (mensaje == null) return null;
        String clave = CONOCIDOS.get(mensaje.trim());
        return clave == null ? mensaje : t(clave);
    }
}
