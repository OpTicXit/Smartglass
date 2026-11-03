package com.smartglass.controller;

import com.smartglass.config.UsuarioSesion;
import com.smartglass.dto.NuevaContrasenaRequest;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.RecuperacionService;
import com.smartglass.service.Textos;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Recuperar la contrasena desde el login, o cambiarla con la sesion iniciada.
 *   GET/POST /recuperar           pide el correo (con sesion, se usa siempre el de la cuenta) y envia el codigo
 *   GET/POST /recuperar/codigo    codigo y contrasena nueva
 *   POST     /recuperar/reenviar  otro codigo
 * El correo viaja en la sesion, no en la URL, y la respuesta es la misma exista o no.
 */
@Controller
public class RecuperacionController {

    private static final String CORREO = "recuperacionEmail";
    private static final int MAX_EMAIL = 254;

    private final RecuperacionService recuperacion;
    private final UsuarioSesion usuarioSesion;
    private final Textos textos;

    public RecuperacionController(RecuperacionService recuperacion, UsuarioSesion usuarioSesion, Textos textos) {
        this.recuperacion = recuperacion;
        this.usuarioSesion = usuarioSesion;
        this.textos = textos;
    }

    @GetMapping("/recuperar")
    public String pedirCorreo(@RequestParam(required = false) String limite, Authentication auth, Model model) {
        model.addAttribute("activa", recuperacion.activa());
        Usuario usuario = usuarioSesion.buscar(auth).orElse(null);
        model.addAttribute("correoCuenta", usuario == null ? null : usuario.getEmail());
        model.addAttribute("cuentaGoogle", usuario != null && usuario.getProvider() != null);
        if (limite != null) model.addAttribute("error", textos.t("flash.demasiados_intentos"));
        return "recuperar";
    }

    @PostMapping("/recuperar")
    public String enviarCodigo(@RequestParam(required = false) String email, Authentication auth, HttpSession sesion) {
        String destino = usuarioSesion.buscar(auth).map(Usuario::getEmail).orElse(email);
        if (destino == null || destino.isBlank() || destino.length() > MAX_EMAIL || !destino.contains("@")) {
            return "redirect:/recuperar";
        }
        sesion.setAttribute(CORREO, destino);
        recuperacion.solicitar(destino, LocaleContextHolder.getLocale());
        return "redirect:/recuperar/codigo";
    }

    @PostMapping("/recuperar/reenviar")
    public String reenviar(HttpSession sesion, RedirectAttributes ra) {
        if (!(sesion.getAttribute(CORREO) instanceof String destino)) return "redirect:/recuperar";
        recuperacion.solicitar(destino, LocaleContextHolder.getLocale());
        ra.addFlashAttribute("mensaje", textos.t("recuperar.reenviado"));
        return "redirect:/recuperar/codigo";
    }

    @GetMapping("/recuperar/codigo")
    public String pedirCodigo(HttpSession sesion, Model model) {
        if (!(sesion.getAttribute(CORREO) instanceof String destino)) return "redirect:/recuperar";
        model.addAttribute("correo", destino);
        return "recuperar-codigo";
    }

    @PostMapping("/recuperar/codigo")
    public String cambiar(@Valid @ModelAttribute NuevaContrasenaRequest datos, BindingResult errores,
                          Authentication auth, HttpSession sesion, Model model, RedirectAttributes ra) {
        if (!(sesion.getAttribute(CORREO) instanceof String destino)) return "redirect:/recuperar";
        model.addAttribute("correo", destino);
        if (errores.hasErrors()) {
            model.addAttribute("error", errores.getAllErrors().get(0).getDefaultMessage());
            return "recuperar-codigo";
        }
        switch (recuperacion.cambiar(destino, datos.getCodigo(), datos.getPassword(), LocaleContextHolder.getLocale())) {
            case CODIGO_INVALIDO -> {
                model.addAttribute("error", textos.t("recuperar.codigo_invalido"));
                return "recuperar-codigo";
            }
            case CONTRASENA_INVALIDA -> {
                model.addAttribute("error", textos.t("registro.distinta_del_correo_y_del"));
                return "recuperar-codigo";
            }
            case OK -> { }
        }
        sesion.removeAttribute(CORREO);
        ra.addFlashAttribute("mensaje", textos.t("recuperar.cambiada"));
        return UsuarioSesion.autenticado(auth) ? "redirect:/usuario" : "redirect:/login";
    }
}
