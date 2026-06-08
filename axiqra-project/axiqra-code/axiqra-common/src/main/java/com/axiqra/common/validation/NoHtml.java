package com.axiqra.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string does not contain HTML or script content.
 * <p>
 * Before applying pattern checks, the input string is decoded:
 * HTML5 numeric entities ({@code &#xHH;}, {@code &#DDDD;}) and named
 * entities ({@code &lt;}, {@code &gt;}, {@code &amp;}, etc.) are decoded by
 * {@link NoHtmlValidator#decodeHtmlEntities(String)}.
 * After decoding, any string containing tags (e.g. {@code <script>})
 * decoded from entities (e.g. {@code &lt;script&gt;}) will be rejected.
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {

    String message() default "不得包含 HTML 标签或脚本内容";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
