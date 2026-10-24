package com.smartglass.model.mysql;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Entidad relacional de Usuario: credenciales (username/password),
 * rol (tipoUsuario) para autenticacion vía Spring Security + JWT,
 * y datos de contacto (email, teléfono con país).
 *
 * F43: `telefono` se guarda en formato E.164 (+573001234567) y
 * `telefonoPais` con el código ISO que detecta libphonenumber ("CO", "US"...).
 * F30/F43: `provider` y `providerId` soportan el login OAuth2 (Google):
 * provider = null es cuenta local con contraseña; provider = "google" con
 * providerId = "sub" de Google es cuenta creada o vinculada por OAuth2.
 * Una sola cuenta por correo: la vinculación no duplica usuarios.
 */
@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(length = 20)
    private String telefono;              // E.164: +573001234567

    @Column(name = "telefono_pais", length = 2)
    private String telefonoPais;          // ISO: CO, US, MX...

    @Column(name = "tipo_usuario", nullable = false)
    private String tipoUsuario;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String provider;              // null = local; "google" = OAuth2

    @Column(name = "provider_id", length = 60)
    private String providerId;            // "sub" de Google

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

    public String getTelefonoPais() { return telefonoPais; }
    public void setTelefonoPais(String telefonoPais) { this.telefonoPais = telefonoPais; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
}