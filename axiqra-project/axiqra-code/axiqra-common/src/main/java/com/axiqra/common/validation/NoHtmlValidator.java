package com.axiqra.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

/**
 * Validates that a string does not contain HTML or script tags.
 * Checks: (1) length limit, (2) javascript:/data: scheme, (3) on* event handlers,
 * (4) strict HTML tag pattern, (5) HTML entity-encoded tags (e.g. &lt;script&gt;).
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    private static final int MAX_LENGTH = 500;

    /**
     * Blocks javascript: and all data: URIs (including data:text/javascript,
     * data:application/javascript, etc.).
     */
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)(javascript:|data:)"
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
        if (value.length() > MAX_LENGTH) {
            return false;
        }

        String decoded = decodeHtmlEntities(value);

        if (SCRIPT_PATTERN.matcher(decoded).find()) {
            return false;
        }
        if (EVENT_HANDLER_PATTERN.matcher(decoded).find()) {
            return false;
        }
        return !HTML_TAG_PATTERN.matcher(decoded).find();
    }

    /**
     * Decodes HTML5 named and numeric entities using Apache Commons Text.
     * Returns null for null input; blank input returns blank (isBlank caught above).
     * If decoding fails (e.g., invalid Unicode code point), returns the original
     * string so validation can still proceed on the raw input.
     */
    private String decodeHtmlEntities(String raw) {
        if (raw == null) return null;
        try {
            return StringEscapeUtils.unescapeHtml4(raw);
        } catch (IllegalArgumentException e) {
            return raw;
        }
    }
}
