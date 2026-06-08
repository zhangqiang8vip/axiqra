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
    // null/blank values are valid per JSR-380 convention (ConstraintValidator contract)

    @Test
    @DisplayName("null 应返回 true / null returns true")
    void nullValue_returnsTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("空白字符串应返回 true / Blank string returns true")
    void blankValue_returnsTrue() {
        assertTrue(validator.isValid("   ", context));
    }

    // ==================== Length ====================
    // Inputs exceeding MAX_LENGTH (500) are rejected without further processing

    @Test
    @DisplayName("超过 500 字符应返回 false / >500 chars returns false")
    void tooLong_returnsFalse() {
        String longStr = "a".repeat(501);
        assertFalse(validator.isValid(longStr, context));
    }

    // ==================== Basic XSS ====================
    // Dangerous tags (<script>, <img>, <svg>), protocols (javascript:, data:), and event handlers (on*)

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(1)",
            "data:text/html,<h1>test</h1>",
            "<svg onload=alert(1)>"
    })
    @DisplayName("危险标签和协议应返回 false / Dangerous tags and protocols return false")
    void dangerousInput_returnsFalse(String input) {
        assertFalse(validator.isValid(input, context));
    }

    // ==================== Safe Inputs ====================
    // Ordinary text should pass validation

    private static final String LONG_SAFE_STRING =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @ParameterizedTest
    @ValueSource(strings = {
            "hello world",
            "user_123",
            "中文昵称",
            LONG_SAFE_STRING
    })
    @DisplayName("普通文本应返回 true / Normal text returns true")
    void safeInput_returnsTrue(String input) {
        assertTrue(validator.isValid(input, context));
    }

    // ==================== HTML Entity Bypass ====================
    // Tests that HTML-entity-encoded XSS payloads (named, hex, decimal) are decoded and blocked

    @Test
    @DisplayName("命名实体 &lt;script&gt; 解码后应被拦截 / Named entity decoded and blocked")
    void namedEntityLessThanScript_returnsFalse() {
        assertFalse(validator.isValid("&lt;script&gt;alert(1)&lt;/script&gt;", context));
    }

    @Test
    @DisplayName("十六进制数字实体小写应被拦截 / Hex numeric entity lowercase decoded and blocked")
    void hexNumericEntityScript_returnsFalse() {
        assertFalse(validator.isValid("&#x3c;script&#x3e;alert(1)&#x3c;/script&#x3e;", context));
    }

    @Test
    @DisplayName("十进制数字实体应被拦截 / Decimal numeric entity decoded and blocked")
    void decimalNumericEntityScript_returnsFalse() {
        assertFalse(validator.isValid("&#60;script&#62;alert(1)&#60;/script&#62;", context));
    }

    @Test
    @DisplayName("混合大小写十六进制实体应被拦截 / Mixed-case hex entity blocked")
    void mixedCaseHexEntity_returnsFalse() {
        assertFalse(validator.isValid("&#X3C;SCRIPT&#X3E;alert(1)&#X3C;/SCRIPT&#X3E;", context));
    }

    @Test
    @DisplayName("无害命名实体不应触发误报 / Safe named entities cause no false positive")
    void safeNamedEntities_returnsTrue() {
        assertTrue(validator.isValid("Tom & Jerry &quot;friends&quot;", context));
    }

    @Test
    @DisplayName("普通文本中的 &amp; 字符不应触发误报 / Ampersand in normal text causes no false positive")
    void ampersandInNormalText_returnsTrue() {
        assertTrue(validator.isValid("A & B", context));
    }

    @Test
    @DisplayName("不带分号的实体应返回 true / Entity without semicolon returns true (not decoded)")
    void entityWithoutSemicolon_returnsTrue() {
        assertTrue(validator.isValid("Tom & Jerry", context));
    }

    @Test
    @DisplayName("超出范围的码点应返回 true / Out-of-range code point returns true (no crash)")
    void outOfRangeCodePoint_returnsTrue() {
        assertTrue(validator.isValid("&#x110000;", context));
    }

    @Test
    @DisplayName("极大安全输入（>400字符）性能测试 / Large safe input (>400 chars) performance test")
    void largeSafeInput_returnsTrue() {
        String largeSafe = "hello world".repeat(40);
        assertTrue(largeSafe.length() > 400);
        assertTrue(validator.isValid(largeSafe, context));
    }

    @Test
    @DisplayName("极大危险输入（>400字符）应返回 false / Large dangerous input (>400 chars) returns false")
    void largeDangerousInput_returnsFalse() {
        String largeDangerous = "<script>".repeat(51);
        assertTrue(largeDangerous.length() > 400);
        assertFalse(validator.isValid(largeDangerous, context));
    }

    @Test
    @DisplayName("超长输入（>1KB）快速返回 false / Very large input (>1KB) fast-rejected at length check")
    void veryLargeInput_rejectedAtLengthCheck() {
        String veryLarge = "x".repeat(2048);
        assertTrue(veryLarge.length() > 1024);
        assertFalse(validator.isValid(veryLarge, context));
    }
}
