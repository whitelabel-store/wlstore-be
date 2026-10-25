package com.wlstore.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Tenant-Id";
    public static final String ATTRIBUTE = "wlstore.tenantId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);

        if (header == null || header.isBlank()) {
            reject(response, "Header " + HEADER + " is required");
            return;
        }

        try {
            request.setAttribute(ATTRIBUTE, UUID.fromString(header.trim()));
        } catch (IllegalArgumentException e) {
            reject(response, "Header " + HEADER + " must be a valid UUID");
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":400,\"message\":\"" + message + "\"}");
    }
}