package com.smartglass.dto;

import com.smartglass.model.mysql.Usuario;

/**
 * Representacion segura de Usuario para respuestas REST: nunca
 * expone el hash de password.
 */
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String email;
    private String username;
    private String tipoUsuario;

    public UsuarioResponse() {
    }

    public static UsuarioResponse from(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.id = usuario.getId();
        response.nombre = usuario.getNombre();
        response.email = usuario.getEmail();
        response.username = usuario.getUsername();
        response.tipoUsuario = usuario.getTipoUsuario();
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }
}
