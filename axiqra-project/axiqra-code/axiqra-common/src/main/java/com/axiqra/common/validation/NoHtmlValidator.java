package com.axiqra.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates that a string does not contain HTML or script tags.
 * Checks: (1) length limit, (2) javascript:/data: scheme, (3) on* event handlers,
 * (4) strict HTML tag pattern <tagname> or </tagname>.
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)javascript:|data:text/html"
    );

    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "(?i)\\bon\\w+\\s*="
    );

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "<\\/?[a-zA-Z][a-zA-Z0-9]*(?:\\s[^>]*)?>"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.length() > 100) {
            return false;
        }
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (SCRIPT_PATTERN.matcher(lower).find()) {
            return false;
        }
        if (EVENT_HANDLER_PATTERN.matcher(value).find()) {
            return false;
        }
        return !HTML_TAG_PATTERN.matcher(value).find();
    }
}
