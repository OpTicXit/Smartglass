package com.smartglass.controller;

import com.smartglass.dto.RegistroRequest;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Envoltorio MVC (formulario HTML) sobre AuthService.register(...).
 * AuthController ya expone el mismo registro como API REST
 * (POST /auth/registro, JSON); este controller es solo para registro.html.
 *
 * F43: @Valid + BindingResult activan los validadores del DTO y vuelven al
 * form con el primer mensaje de error en espanol. El honeypot redirige en
 * silencio a /login. El parametro ?limite lo pone RateLimitFilter (F41)
 * cuando se agota la cubeta de 10 registros/min por IP.
 *
 * H06: GET /registro/disponible responde a la comprobacion en vivo de registro.html
 * (comprobarDisponibilidad). RateLimitFilter ya la trata como ruta sensible (429 JSON).
 */
@Controller
public class RegistroController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    public RegistroController(AuthService authService, UsuarioRepository usuarioRepository) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * H06: disponibilidad de correo y/o usuario. Solo responde las claves de los
     * parametros enviados; normaliza igual que los setters de RegistroRequest
     * (trim + minusculas) para que coincida con lo que se guardaria.
     */
    @GetMapping("/registro/disponible")
    @ResponseBody
    public Map<String, Object> disponible(@RequestParam(required = false) String email,
                                          @RequestParam(required = false) String username) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        if (email != null && !email.isBlank()) {
            boolean libre = !usuarioRepository.existsByEmail(email.trim().toLowerCase(Locale.ROOT));
            respuesta.put("emailDisponible", libre);
            respuesta.put("emailMensaje", libre ? "" : "Ya existe una cuenta con ese correo.");
        }
        if (username != null && !username.isBlank()) {
            boolean libre = !usuarioRepository.existsByUsername(username.trim().toLowerCase(Locale.ROOT));
            respuesta.put("usernameDisponible", libre);
            respuesta.put("usernameMensaje", libre ? "" : "Ese nombre de usuario ya está en uso.");
        }
        return respuesta;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(@RequestParam(required = false) String limite, Model model) {
        if (limite != null) {
            model.addAttribute("error", "Demasiados intentos seguidos. Espera un minuto y vuelve a intentar.");
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute RegistroRequest request,
                            BindingResult errores,
                            Model model,
                            RedirectAttributes ra) {
        if (request.esSpam()) {
            return "redirect:/login"; // honeypot: rechazo silencioso
        }
        if (errores.hasErrors()) {
            model.addAttribute("error", errores.getAllErrors().get(0).getDefaultMessage());
            return "registro"; // Spring repuebla el form con el atributo "registroRequest"
        }
        try {
            authService.register(request);
            ra.addFlashAttribute("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "registro";
        }
    }
}