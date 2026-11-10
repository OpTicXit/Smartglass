package com.smartglass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartglassApplication {

	public static void main(String[] args) {
		// El contenedor de Railway corre en UTC y las fechas se crean con LocalDateTime.now(): pedidos, cotizaciones
		// y la grafica diaria quedaban 5 horas adelantados (a las 19:00 de Colombia ya era "manana").
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(com.smartglass.model.mongo.CatalogoProducto.ZONA));
		SpringApplication.run(SmartglassApplication.class, args);
	}

}
