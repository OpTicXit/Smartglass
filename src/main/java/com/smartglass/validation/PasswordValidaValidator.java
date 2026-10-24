package com.smartglass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordValidaValidator implements ConstraintValidator<PasswordValida, Object> {

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext ctx) {
        if (bean == null) return true;
        String password = (String) leer(bean, "getPassword");
        if (password == null) return false;
        if (password.length() < 8) return false;
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) return false; // límite BCrypt
        if (password.contains(" ")) return false;
        boolean mayus = false, minus = false, digito = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) mayus = true;
            else if (Character.isLowerCase(c)) minus = true;
            else if (Character.isDigit(c)) digito = true;
        }
        if (!(mayus && minus && digito)) return false;
        String email = (String) leer(bean, "getEmail");
        String username = (String) leer(bean, "getUsername");
        return !password.equals(email) && !password.equals(username);
    }

    private Object leer(Object bean, String getter) {
        try {
            return bean.getClass().getMethod(getter).invoke(bean);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}