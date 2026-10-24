package com.smartglass.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordValidaValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordValida {
    String message() default "La contraseña debe tener 8–72 caracteres, sin espacios, con al menos una mayúscula, una minúscula y un dígito, y ser distinta del correo y del usuario";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}