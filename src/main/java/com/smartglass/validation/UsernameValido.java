package com.smartglass.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UsernameValidoValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface UsernameValido {
    String message() default "El usuario debe tener 3–20 caracteres (minúsculas, dígitos, punto, guion) y no ser un nombre reservado";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}