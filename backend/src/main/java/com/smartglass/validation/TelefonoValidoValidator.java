package com.smartglass.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelefonoValidoValidator implements ConstraintValidator<TelefonoValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;
        try {
            // "CO" solo aplica si el número NO trae "+"; si trae, el país se detecta solo
            Phonenumber.PhoneNumber n =
                    PhoneNumberUtil.getInstance().parse(value.trim(), "CO");
            return PhoneNumberUtil.getInstance().isValidNumber(n);
        } catch (NumberParseException e) {
            return false;
        }
    }
}