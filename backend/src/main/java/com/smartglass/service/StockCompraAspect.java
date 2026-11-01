package com.smartglass.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * T2 (D1): descuenta el stock al CONFIRMAR una compra sin modificar PedidoService
 * ni CheckoutController (el prompt los deja fuera de alcance salvo el cambio de
 * auth de T1). Rodea PedidoService.crearPedidoDesdeCarrito:
 *  1. reserva el stock del carrito (atomico; si no alcanza, la compra se rechaza
 *     con un mensaje que CheckoutController ya muestra como aviso);
 *  2. crea el pedido como siempre;
 *  3. si crear el pedido falla, devuelve las unidades reservadas.
 */
@Aspect
@Component
public class StockCompraAspect {

    private final StockService stockService;
    private final CarritoService carritoService;

    public StockCompraAspect(StockService stockService, CarritoService carritoService) {
        this.stockService = stockService;
        this.carritoService = carritoService;
    }

    @Around("execution(* com.smartglass.service.PedidoService.crearPedidoDesdeCarrito(..))")
    public Object descontarStock(ProceedingJoinPoint pjp) throws Throwable {
        Map<String, Integer> reservado = stockService.reservar(carritoService.getItems());
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            stockService.liberar(reservado);
            throw t;
        }
    }
}
