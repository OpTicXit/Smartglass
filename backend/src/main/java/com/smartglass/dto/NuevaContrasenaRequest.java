package com.smartglass.dto;

import com.smartglass.validation.FieldMatch;
import com.smartglass.validation.PasswordValida;

/** Codigo y contrasena nueva del formulario de recuperacion; mismas reglas de contrasena que el registro. */
@PasswordValida
@FieldMatch(first = "password", second = "confirmPassword", message = "{valid.passwords_no_coinciden}")
public class NuevaContrasenaRequest {

    private String codigo;
    private String password;
    private String confirmPassword;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
