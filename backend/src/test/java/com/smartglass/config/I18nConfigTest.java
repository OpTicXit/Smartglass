package com.smartglass.config;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nConfigTest {

    @Test
    void reconoceLosIdiomasDelSelectorYElEspanolASecas() {
        assertEquals(Optional.of(Locale.forLanguageTag("es-CO")), I18nConfig.soportado("es"));
        assertEquals(Optional.of(Locale.forLanguageTag("es-CO")), I18nConfig.soportado("es_co"));
        assertEquals(Optional.of(Locale.forLanguageTag("es-ES")), I18nConfig.soportado("es-ES"));
        assertEquals(Optional.of(Locale.forLanguageTag("en")), I18nConfig.soportado("EN"));
        assertEquals(Optional.empty(), I18nConfig.soportado("fr"));
        assertEquals(Optional.empty(), I18nConfig.soportado(" "));
    }
}
