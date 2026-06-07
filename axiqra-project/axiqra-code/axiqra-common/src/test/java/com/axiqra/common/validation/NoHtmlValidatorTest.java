package com.axiqra.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoHtmlValidatorTest {

    private NoHtmlValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NoHtmlValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    // ==================== Null/Blank ====================

    @Test
    @DisplayName("null 应返回 true")
    void nullValue_returnsTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("空白字符串应返回 true")
    void blankValue_returnsTrue() {
        assertTrue(validator.isValid("   ", context));
    }

    // ==================== Length ====================

    @Test
    @DisplayName("超过 500 字符应返回 false")
    void tooLong_returnsFalse() {
        String longStr = "a".repeat(501);
        assertFalse(validator.isValid(longStr, context));
    }

    // ==================== Basic XSS ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(1)",
            "data:text/html,<h1>test</h1>",
            "<svg onload=alert(1)>"
    })
    @DisplayName("危险标签和协议应返回 false")
    void dangerousInput_returnsFalse(String input) {
        assertFalse(validator.isValid(input, context));
    }

    private static final String LONG_SAFE_STRING =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @ParameterizedTest
    @ValueSource(strings = {
            "hello world",
            "user_123",
            "中文昵称",
            LONG_SAFE_STRING
    })
    @DisplayName("普通文本应返回 true")
    void safeInput_returnsTrue(String input) {
        assertTrue(validator.isValid(input, context));
    }

    // ==================== HTML Entity Bypass ====================

    @Test
    @DisplayName("命名实体 &lt;script&gt; 解码后应被拦截")
    void namedEntityLessThanScript_returnsFalse() {
        assertFalse(validator.isValid("&lt;script&gt;alert(1)&lt;/script&gt;", context));
    }

    @Test
    @DisplayName("十六进制数字实体 &#60;script&#62; 解码后应被拦截")
    void hexNumericEntityScript_returnsFalse() {
        assertFalse(validator.isValid("&#60;script&#62;alert(1)&#60;/script&#62;", context));
    }

    @Test
    @DisplayName("十进制数字实体 &#60;script&#62; 解码后应被拦截")
    void decimalNumericEntityScript_returnsFalse() {
        assertFalse(validator.isValid("&#60;script&#62;alert(1)&#60;/script&#62;", context));
    }

    @Test
    @DisplayName("混合大小写十六进制实体应被拦截")
    void mixedCaseHexEntity_returnsFalse() {
        assertFalse(validator.isValid("&#X3C;SCRIPT&#X3E;alert(1)&#X3C;/SCRIPT&#X3E;", context));
    }

    @Test
    @DisplayName("无害命名实体（&amp; &quot;）不应触发误报")
    void safeNamedEntities_returnsTrue() {
        assertTrue(validator.isValid("Tom & Jerry &quot;friends&quot;", context));
    }

    @Test
    @DisplayName("普通文本中的 &amp; 字符不应触发误报")
    void ampersandInNormalText_returnsTrue() {
        assertTrue(validator.isValid("A & B", context));
    }

    @Test
    @DisplayName("不带分号的实体应返回 true（未解析则不过滤）")
    void entityWithoutSemicolon_returnsTrue() {
        assertTrue(validator.isValid("Tom & Jerry", context));
    }

    @Test
    @DisplayName("超出范围的码点应返回 true（不崩溃）")
    void outOfRangeCodePoint_returnsTrue() {
        assertTrue(validator.isValid("&#x110000;", context));
    }
}
