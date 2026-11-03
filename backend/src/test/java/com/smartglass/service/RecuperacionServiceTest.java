package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lo que protege la cuenta: vigencia, tope de intentos, frecuencia de envio y cuentas de Google. */
class RecuperacionServiceTest {

    private static final String CORREO = "ana@ejemplo.com";
    private static final String NUEVA = "ClaveNueva123";
    private static final Locale IDIOMA = Locale.ROOT;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final JavaMailSender correo = mock(JavaMailSender.class);
    private Usuario ana;
    private RecuperacionService servicio;

    @BeforeEach
    void preparar() {
        ana = new Usuario("Ana", CORREO, "ana", encoder.encode("ClaveVieja123"));
        when(repo.findByEmail(CORREO)).thenReturn(Optional.of(ana));

        StaticMessageSource textos = new StaticMessageSource();
        textos.addMessage("recuperar.correo_asunto", IDIOMA, "Asunto");
        textos.addMessage("recuperar.correo_asunto_cambio", IDIOMA, "Cambio");
        textos.addMessage("recuperar.correo_cuerpo", IDIOMA, "Codigo {0} para {1}");
        textos.addMessage("recuperar.correo_google", IDIOMA, "Entra con Google");
        textos.addMessage("recuperar.correo_cambio", IDIOMA, "Cambiada");
        servicio = new RecuperacionService(repo, encoder, correo, textos, "smartglass@gmail.com");
    }

    private String pedirCodigo() {
        servicio.procesar(CORREO, IDIOMA);
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(enviado.capture());
        Matcher m = Pattern.compile("\\d{6}").matcher(enviado.getValue().getText());
        assertTrue(m.find(), "el correo debe traer el codigo");
        return m.group();
    }

    private static String otroCodigo(String codigo) {
        return codigo.equals("000000") ? "000001" : "000000";
    }

    @Test
    void conElCodigoCorrectoCambiaLaContrasenaYLoBorra() {
        String codigo = pedirCodigo();
        assertFalse(codigo.equals(ana.getRecuperacionCodigo()), "el codigo no se guarda en claro");

        assertEquals(RecuperacionService.Resultado.OK, servicio.cambiar(CORREO, codigo, NUEVA, IDIOMA));
        assertTrue(encoder.matches(NUEVA, ana.getPassword()));
        assertNull(ana.getRecuperacionCodigo());
        assertEquals(RecuperacionService.Resultado.CODIGO_INVALIDO, servicio.cambiar(CORREO, codigo, NUEVA, IDIOMA), "un solo uso");
    }

    @Test
    void cincoFallosBloqueanInclusoElCodigoCorrecto() {
        String codigo = pedirCodigo();
        for (int i = 0; i < 5; i++) {
            assertEquals(RecuperacionService.Resultado.CODIGO_INVALIDO, servicio.cambiar(CORREO, otroCodigo(codigo), NUEVA, IDIOMA));
        }
        assertEquals(RecuperacionService.Resultado.CODIGO_INVALIDO, servicio.cambiar(CORREO, codigo, NUEVA, IDIOMA));
        assertFalse(encoder.matches(NUEVA, ana.getPassword()));
    }

    @Test
    void unCodigoNuevoNoDevuelveLosIntentosGastados() {
        pedirCodigo();
        ana.setRecuperacionIntentos(5);
        ana.setRecuperacionEnviada(Instant.now().minusSeconds(600));
        servicio.procesar(CORREO, IDIOMA);
        verify(correo, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void elCodigoVencido() {
        String codigo = pedirCodigo();
        ana.setRecuperacionVence(Instant.now().minusSeconds(1));
        assertEquals(RecuperacionService.Resultado.CODIGO_INVALIDO, servicio.cambiar(CORREO, codigo, NUEVA, IDIOMA));
    }

    @Test
    void noReenviaAntesDeDosMinutos() {
        pedirCodigo();
        servicio.procesar(CORREO, IDIOMA);
        verify(correo, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void unCorreoQueNoExisteNoEnviaNada() {
        when(repo.findByEmail("nadie@ejemplo.com")).thenReturn(Optional.empty());
        servicio.procesar("nadie@ejemplo.com", IDIOMA);
        verify(correo, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void laCuentaDeGoogleRecibeElAvisoSinCodigo() {
        ana.setProvider("google");
        servicio.procesar(CORREO, IDIOMA);
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(enviado.capture());
        assertEquals("Entra con Google", enviado.getValue().getText());
        assertNull(ana.getRecuperacionCodigo());
        assertNotNull(ana.getRecuperacionEnviada());
    }

    @Test
    void laContrasenaNoPuedeSerElCorreoNiElUsuario() {
        String codigo = pedirCodigo();
        assertEquals(RecuperacionService.Resultado.CONTRASENA_INVALIDA, servicio.cambiar(CORREO, codigo, CORREO, IDIOMA));
        assertFalse(encoder.matches(CORREO, ana.getPassword()));
    }
}
