package com.smartglass.model.mysql;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Entidad relacional de Usuario: credenciales (username/password)
 * y rol (tipoUsuario) para autenticacion vía Spring Security + JWT.
 */
@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    private String telefono;

    @Column(name = "tipo_usuario", nullable = false)
    private String tipoUsuario;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    public Usuario() {
    }

    public Usuario(String nombre, String email, String username, String password) {
        this.nombre = nombre;
        this.email = email;
        this.username = username;
        this.password = password;
        this.tipoUsuario = "ROLE_USER";
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
