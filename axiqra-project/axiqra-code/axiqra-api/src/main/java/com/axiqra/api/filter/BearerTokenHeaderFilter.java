package com.axiqra.api.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Normalizes Authorization: Bearer <token> for Sa-Token, whose token-name is Authorization.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class BearerTokenHeaderFilter implements Filter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = httpRequest.getHeader(AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        String tokenValue = authorization.substring(BEARER_PREFIX.length()).trim();
        chain.doFilter(new HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getHeader(String name) {
                if (AUTHORIZATION.equalsIgnoreCase(name)) {
                    return tokenValue;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (AUTHORIZATION.equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList(tokenValue));
                }
                return super.getHeaders(name);
            }
        }, response);
    }
}
