package com.smartglass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.regex.Pattern;

public class UsernameValidoValidator implements ConstraintValidator<UsernameValido, String> {

    private static final Pattern RE = Pattern.compile("^[a-z0-9._-]{3,20}$");

    private static final Set<String> RESERVADOS = Set.of(
            "admin", "administrador", "root", "soporte", "support", "smartglass",
            "sistema", "test", "usuario", "nadie", "null", "undefined");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return false;
        String u = value.trim();
        return RE.matcher(u).matches() && !RESERVADOS.contains(u);
    }
}