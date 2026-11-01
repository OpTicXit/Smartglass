package com.smartglass.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Fuerza bruta repartida: cada intento llega desde otra IP pero contra la misma cuenta. */
class LoginRateLimitFilterTest {

    @Test
    void laCuentaSeFrenaAunqueCambieLaIp() throws Exception {
        LoginRateLimitFilter filtro = new LoginRateLimitFilter(2, true); // 2 por IP, 10 por cuenta
        for (int i = 0; i < 10; i++) {
            assertNull(intento(filtro, "10.0.0." + i, "Admin").getRedirectedUrl(), "intento " + i + " debia pasar");
        }
        assertEquals("/login?bloqueado", intento(filtro, "10.0.1.1", "admin").getRedirectedUrl());
        assertNull(intento(filtro, "10.0.1.2", "otra-cuenta").getRedirectedUrl());
    }

    private static MockHttpServletResponse intento(LoginRateLimitFilter filtro, String ip, String usuario) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
        req.setServletPath("/login");
        req.addHeader("X-Forwarded-For", ip);
        req.setParameter("username", usuario);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, new MockFilterChain());
        return res;
    }
}
