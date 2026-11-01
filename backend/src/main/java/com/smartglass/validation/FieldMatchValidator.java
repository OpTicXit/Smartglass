package com.smartglass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String first;
    private String second;

    @Override
    public void initialize(FieldMatch annotation) {
        this.first = annotation.first();
        this.second = annotation.second();
    }

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext ctx) {
        if (bean == null) return true;
        Object a = leer(bean, first);
        Object b = leer(bean, second);
        boolean ok = (a == null) ? b == null : a.equals(b);
        if (!ok) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(second)
                    .addConstraintViolation();
        }
        return ok;
    }

    private Object leer(Object bean, String propiedad) {
        try {
            String getter = "get" + Character.toUpperCase(propiedad.charAt(0)) + propiedad.substring(1);
            return bean.getClass().getMethod(getter).invoke(bean);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}