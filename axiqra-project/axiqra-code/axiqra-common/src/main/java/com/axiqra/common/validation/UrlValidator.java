package com.axiqra.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates that a string is a valid HTTPS URL.
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            URI uri = new URI(value);
            if (uri.getUserInfo() != null ) {
                return false;
            }
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            String host = uri.getHost();
            return host != null && !host.isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
