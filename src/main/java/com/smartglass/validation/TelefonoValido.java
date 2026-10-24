package com.smartglass.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TelefonoValidoValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface TelefonoValido {
    String message() default "Teléfono inválido. Ej.: +57 300 123 4567";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}