package com.axiqra.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string does not contain HTML or script tags.
 * Strips HTML tags before validation so that a string like
 * "Hello &lt;script&gt;..." passes as long as it has no tags.
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
