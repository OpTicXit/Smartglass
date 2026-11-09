package com.smartglass.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void lasIpv6DeLaMismaRedCompartenCubeta() {
        assertEquals(RateLimitFilter.claveDeIp(desde("2001:db8:1:2::a"), false), RateLimitFilter.claveDeIp(desde("2001:db8:1:2:ffff::1"), false));
        assertNotEquals(RateLimitFilter.claveDeIp(desde("2001:db8:1:2::a"), false), RateLimitFilter.claveDeIp(desde("2001:db8:1:3::a"), false));
        assertEquals("203.0.113.7", RateLimitFilter.claveDeIp(desde("203.0.113.7"), false));
    }

    @Test
    void detrasDelProxyCuentaLaUltimaIpDeXForwardedFor() {
        MockHttpServletRequest req = desde("162.220.232.10");                     // el proxy de la plataforma
        req.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.9");                 // el visitante escribio 1.2.3.4
        assertEquals("203.0.113.9", RateLimitFilter.claveDeIp(req, true), "la que agrega el proxy, no la falsificable");
        assertEquals("162.220.232.10", RateLimitFilter.claveDeIp(req, false), "sin proxy de confianza se ignora el encabezado");
        MockHttpServletRequest solo = desde("162.220.232.10");
        solo.addHeader("X-Forwarded-For", "198.51.100.4");
        assertEquals("198.51.100.4", RateLimitFilter.claveDeIp(solo, true));
    }

    private static MockHttpServletRequest desde(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(ip);
        return req;
    }
}
