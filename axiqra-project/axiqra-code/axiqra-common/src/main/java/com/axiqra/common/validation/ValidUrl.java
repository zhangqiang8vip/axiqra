package com.axiqra.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a valid HTTPS URL.
 * null and empty strings are considered valid (use @NotBlank to disallow).
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Documented
@Constraint(validatedBy = UrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUrl {

    String message() default "avatar 必须为有效的 HTTPS URL，仅支持 https:// 协议";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
