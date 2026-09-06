# Smartglass

# 🚀 SmartGlass - Plataforma de Gestión Sostenible e Industrial

**SmartGlass** es una solución de software híbrida desarrollada en Spring Boot diseñada para la industria de manufactura de vidrio, integrando el cálculo de emisiones de dióxido de carbono ($CO_2$), gestión de inventarios, catálogos especializados y un sistema seguro de autenticación por JWT y Google OAuth2.

---

## 🏗️ Arquitectura del Sistema

El sistema implementa un **modelo de persistencia híbrido** para optimizar el rendimiento y la flexibilidad de los datos:
*   **MySQL (JPA / Hibernate):** Utilizado para entidades relacionales transaccionales críticas.
    *   `Usuario`: Gestión de credenciales, roles y accesos.
    *   `Pedido`: Registro de órdenes de compra industriales.
    *   `DetallePedido`: Ítems del pedido optimizados mediante campos *snapshot* para desacoplar el historial de precios y productos.
*   **MongoDB (Spring Data MongoDB):** Utilizado para documentos flexibles, catálogos dinámicos y estructuras de alta lectura.
    *   `CatalogoProducto`: Variedades y especificaciones técnicas de vidrios.
    *   `VidrioSostenible`: Métricas de huella de carbono y eficiencia energética.
    *   `Carrito`: Sesiones temporales de compra de los usuarios.

---

## 🛠️ Tecnologías Utilizadas

*   **Backend:** Java 17 / 26, Spring Boot 4.x
*   **Seguridad:** Spring Security, JSON Web Tokens (JWT), OAuth2 Client (Google Login)
*   **Bases de Datos:** MySQL 8.x + MongoDB
*   **Herramientas:** Maven, Lombok, Git

---

## ⚙️ Requisitos Previos e Instalación

Asegúrate de tener instalado en tu entorno local:
1.  **JDK 17 o superior** (configurado en las variables de entorno).
2.  **MySQL Server** en ejecución (puerto por defecto `3306`).
3.  **MongoDB** activo (local o URI en la nube).
4.  **Maven** (o utilizar el wrapper `./mvnw` incluido).

### 1. Clonar el repositorio
```bash
git clone [https://github.com/OpTicXit/Smartglass.git](https://github.com/OpTicXit/Smartglass.git)
cd smartglass
