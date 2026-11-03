package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Recuperar o cambiar la contrasena con un codigo enviado por correo (MAIL_* en .env).
 *
 * - El codigo es de 6 digitos, vence a los 15 minutos y se guarda cifrado.
 * - Por cuenta: un correo cada 2 minutos y 5 intentos por hora, que un codigo nuevo no reinicia.
 * - El envio va en otro hilo: la respuesta tarda lo mismo exista o no el correo, y un SMTP lento no la frena.
 * - Las cuentas de Google no tienen contrasena propia: reciben un aviso para entrar con Google.
 *
 * shortcut: cambiar la contrasena no cierra otras sesiones abiertas ni revoca JWT (duran 1 hora); si hace
 * falta, guardar una version de credenciales en Usuario y comprobarla en cada peticion.
 */
@Service
public class RecuperacionService {

    public enum Resultado { OK, CODIGO_INVALIDO, CONTRASENA_INVALIDA }

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final Duration ESPERA_REENVIO = Duration.ofMinutes(2);
    private static final Duration VENTANA_INTENTOS = Duration.ofHours(1);
    private static final int MAX_INTENTOS = 5;

    private final SecureRandom azar = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JavaMailSender correo;
    private final MessageSource mensajes;
    private final String remitente;

    public RecuperacionService(UsuarioRepository usuarios, PasswordEncoder encoder, JavaMailSender correo,
                               MessageSource mensajes, @Value("${smartglass.mail.remitente:}") String remitente) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.correo = correo;
        this.mensajes = mensajes;
        this.remitente = remitente;
    }

    /** Sin remitente configurado no se ofrece la opcion: nadie se queda esperando un correo que no llegara. */
    public boolean activa() {
        return !remitente.isBlank();
    }

    public void solicitar(String email, Locale idioma) {
        if (!activa()) return;
        enOtroHilo(() -> procesar(email, idioma));
    }

    void procesar(String email, Locale idioma) {
        Usuario u = usuarios.findByEmail(normalizar(email)).orElse(null);
        if (u == null) return;

        Instant ahora = Instant.now();
        Instant enviada = u.getRecuperacionEnviada();
        if (enviada != null && ahora.isBefore(enviada.plus(ESPERA_REENVIO))) return;

        if (enviada == null || ahora.isAfter(enviada.plus(VENTANA_INTENTOS))) {
            u.setRecuperacionIntentos(0);
        } else if (intentos(u) >= MAX_INTENTOS) {
            return;
        }

        u.setRecuperacionEnviada(ahora);
        if (u.getProvider() != null) {
            usuarios.save(u);
            enviar(u.getEmail(), idioma, "recuperar.correo_asunto", "recuperar.correo_google");
            return;
        }

        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        u.setRecuperacionCodigo(encoder.encode(codigo));
        u.setRecuperacionVence(ahora.plus(VIGENCIA));
        usuarios.save(u);
        enviar(u.getEmail(), idioma, "recuperar.correo_asunto", "recuperar.correo_cuerpo", codigo, u.getUsername());
    }

    public Resultado cambiar(String email, String codigo, String nueva, Locale idioma) {
        Usuario u = usuarios.findByEmail(normalizar(email)).orElse(null);
        if (u == null || u.getRecuperacionCodigo() == null || u.getRecuperacionVence() == null
                || Instant.now().isAfter(u.getRecuperacionVence()) || intentos(u) >= MAX_INTENTOS) {
            return Resultado.CODIGO_INVALIDO;
        }

        // El intento se cuenta antes de comparar: fallar a medias no devuelve oportunidades
        u.setRecuperacionIntentos(intentos(u) + 1);
        usuarios.save(u);
        if (codigo == null || !encoder.matches(codigo.trim(), u.getRecuperacionCodigo())) {
            return Resultado.CODIGO_INVALIDO;
        }
        if (nueva.equalsIgnoreCase(u.getEmail()) || nueva.equalsIgnoreCase(u.getUsername())) {
            return Resultado.CONTRASENA_INVALIDA;
        }

        u.setPassword(encoder.encode(nueva));
        u.setRecuperacionCodigo(null);
        u.setRecuperacionVence(null);
        u.setRecuperacionEnviada(null);
        u.setRecuperacionIntentos(null);
        usuarios.save(u);
        // Aviso a la persona: si no fue ella, se entera por este correo
        String destino = u.getEmail();
        enOtroHilo(() -> enviar(destino, idioma, "recuperar.correo_asunto_cambio", "recuperar.correo_cambio"));
        return Resultado.OK;
    }

    private void enviar(String destino, Locale idioma, String asunto, String cuerpo, Object... args) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom("SmartGlass <" + remitente + ">");
        m.setTo(destino);
        m.setSubject(mensajes.getMessage(asunto, null, idioma));
        m.setText(mensajes.getMessage(cuerpo, args, idioma));
        correo.send(m);
    }

    private static void enOtroHilo(Runnable tarea) {
        Thread.startVirtualThread(() -> {
            try {
                tarea.run();
            } catch (RuntimeException e) {
                // Sin datos de la persona ni del codigo: solo por que fallo (SMTP caido, credenciales...)
                log.warn("No se pudo enviar el correo de recuperacion: {}", e.getClass().getSimpleName());
            }
        });
    }

    private static int intentos(Usuario u) {
        return u.getRecuperacionIntentos() == null ? 0 : u.getRecuperacionIntentos();
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
