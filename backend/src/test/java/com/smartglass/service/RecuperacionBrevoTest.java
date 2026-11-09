package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Con BREVO_API_KEY el codigo sale por la API HTTPS de Brevo (aqui, un servidor local que hace de Brevo). */
class RecuperacionBrevoTest {

    private HttpServer servidor;

    @AfterEach
    void cerrar() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void elCodigoSalePorLaApiDeBrevoYNoPorSmtp() throws Exception {
        AtomicReference<String> clave = new AtomicReference<>(), cuerpo = new AtomicReference<>();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", ex -> {
            clave.set(ex.getRequestHeaders().getFirst("api-key"));
            cuerpo.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(201, -1);
            ex.close();
        });
        servidor.start();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        UsuarioRepository repo = mock(UsuarioRepository.class);
        JavaMailSender smtp = mock(JavaMailSender.class);
        Usuario ana = new Usuario("Ana", "ana@ejemplo.com", "ana", encoder.encode("ClaveVieja123"));
        when(repo.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(ana));
        StaticMessageSource textos = new StaticMessageSource();
        textos.addMessage("recuperar.correo_asunto", Locale.ROOT, "Asunto");
        textos.addMessage("recuperar.correo_cuerpo", Locale.ROOT, "Codigo {0} para {1}");

        RecuperacionService servicio = new RecuperacionService(repo, encoder, smtp, textos, "remitente@gmail.com",
                "clave-de-prueba", "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email");
        servicio.procesar("ana@ejemplo.com", Locale.ROOT);

        assertEquals("clave-de-prueba", clave.get());
        assertTrue(cuerpo.get().contains("\"email\":\"ana@ejemplo.com\""), cuerpo.get());
        assertTrue(cuerpo.get().contains("\"email\":\"remitente@gmail.com\""), cuerpo.get());
        assertTrue(cuerpo.get().matches("(?s).*Codigo \\d{6} para ana.*"), cuerpo.get());
        verify(smtp, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
