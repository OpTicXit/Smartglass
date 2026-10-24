package com.smartglass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class EmailValidoValidator implements ConstraintValidator<EmailValido, String> {

    private static final Pattern RE =
            Pattern.compile("^[A-Za-z0-9_+%.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,24}$");

    private static final Set<String> DESECHABLES = Set.of(
            "mailinator.com", "tempmail.com", "temp-mail.org", "10minutemail.com",
            "guerrillamail.com", "yopmail.com", "trashmail.com", "fakeinbox.com",
            "getnada.com", "dispostable.com");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return false; // sin @NotBlank, esta anotación ya exige presencia
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254) return false;
        if (!RE.matcher(email).matches()) return false;
        int arroba = email.indexOf('@');
        if (arroba <= 0 || arroba != email.lastIndexOf('@')) return false;
        String local = email.substring(0, arroba);
        String dominio = email.substring(arroba + 1);
        if (local.isEmpty() || local.length() > 64) return false;
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) return false;
        if (dominio.startsWith("-") || dominio.startsWith(".") || dominio.endsWith(".")
                || dominio.contains("..")) return false;
        return !DESECHABLES.contains(dominio);
    }
}