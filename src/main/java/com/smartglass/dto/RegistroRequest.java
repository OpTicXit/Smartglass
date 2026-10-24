package com.smartglass.dto;

import com.smartglass.validation.EmailValido;
import com.smartglass.validation.FieldMatch;
import com.smartglass.validation.PasswordValida;
import com.smartglass.validation.TelefonoValido;
import com.smartglass.validation.UsernameValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Locale;

/**
 * F43: datos del formulario de registro. Los setters normalizan (trim, minusculas)
 * para que la validacion y la unicidad trabajen siempre sobre el mismo valor.
 * "website" es el campo trampa (honeypot): un usuario real nunca lo ve ni lo llena.
 */
@PasswordValida
@FieldMatch(first = "password", second = "confirmPassword", message = "Las contraseñas no coinciden")
public class RegistroRequest {

    @NotBlank(message = "Ingresa tu nombre")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü' -]{2,100}$",
            message = "El nombre solo puede tener letras, espacios, apóstrofo y guion (2 a 100 caracteres)")
    private String nombre;

    @EmailValido
    private String email;

    @TelefonoValido
    private String telefono;

    @UsernameValido
    private String username;

    private String password;
    private String confirmPassword;

    private String website;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) {
        this.telefono = telefono == null ? null : telefono.trim();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    /** true si el campo trampa llego con valor: se trata como bot y se rechaza en silencio. */
    public boolean esSpam() {
        return website != null && !website.isBlank();
    }
}
