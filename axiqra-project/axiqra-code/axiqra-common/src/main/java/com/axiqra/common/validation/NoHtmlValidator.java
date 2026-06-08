package com.axiqra.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
     * Decodes HTML5 numeric (&#xHH; &#DDDD;) and named (&lt; &gt; &amp; &quot;) entities.
     * Only decodes dangerous patterns; leaves harmless content intact.
     *
     * <p>Returns {@code true} for {@code null} or blank input, consistent with
     * JSR-380 bean-validation convention: null values are considered valid.</p>
     */
    private String decodeHtmlEntities(String raw) {
        return decodeHtmlEntitiesMinimal(raw);
    }

    private String decodeHtmlEntitiesMinimal(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&') {
                int semicolon = raw.indexOf(';', i);
                if (semicolon == -1) {
                    sb.append(c);
                    i++;
                    continue;
                }
                String entity = raw.substring(i, semicolon + 1);
                String decoded = decodeSingleEntity(entity);
                if (decoded != null) {
                    sb.append(decoded);
                    i = semicolon + 1;
                } else {
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Decodes a single HTML entity ({@code &name;} or {@code &#xHH;}/{#DDD;}).
     *
     * <p>Uses {@code substring(1, len-1)} for the inner content because all
     * entities share the prefix {@code &} and suffix {@code ;}:</p>
     * <ul>
     *   <li>{@code &amp;apos;} → inner = {@code "amp;apos"} → starts with {@code "#"}? No → skip</li>
     *   <li>{@code &#x3C;}  → inner = {@code "#x3C"}      → starts with {@code "#x"}? Yes → hex decode</li>
     *   <li>{@code &#60;}    → inner = {@code "#60"}       → starts with {@code "#"}? Yes → dec decode</li>
     * </ul>
     */
    private String decodeSingleEntity(String entity) {
        // Guard against malformed entities that are too short to decode.
        // Minimum length is 3: one char prefix + one char content + ';'.
        if (entity.length() < 3) {
            return null;
        }
        // Named entities
        return switch (entity.toLowerCase(java.util.Locale.ROOT)) {
            case "&lt;"  -> "<";
            case "&gt;"  -> ">";
            case "&amp;" -> "&";
            case "&quot;" -> "\"";
            case "&apos;" -> "'";
            case "&nbsp;" -> " ";
            default -> {
                // Numeric: &#xHHHH; (hex) or &#DDDD; (decimal)
                String inner = entity.substring(1, entity.length() - 1);
                if (inner.startsWith("#x") || inner.startsWith("#X")) {
                    try {
                        int codePoint = Integer.parseInt(inner.substring(2), 16);
                        if (codePoint > 0 && codePoint <= 0x10FFFF) {
                            yield new String(Character.toChars(codePoint));
                        }
                    } catch (IllegalArgumentException ignored) {}
                } else if (inner.startsWith("#")) {
                    try {
                        int codePoint = Integer.parseInt(inner.substring(1));
                        if (codePoint > 0 && codePoint <= 0x10FFFF) {
                            yield new String(Character.toChars(codePoint));
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                yield null;
            }
        };
    }
}
