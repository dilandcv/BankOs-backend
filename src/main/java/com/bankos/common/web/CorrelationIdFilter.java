package com.bankos.common.web;

import com.bankos.common.exception.ErrorCode;
import com.bankos.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    private final ObjectMapper objectMapper;

    public CorrelationIdFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String correlationId;

        if (incoming == null || incoming.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            try {
                correlationId = UUID.fromString(incoming).toString();
            } catch (IllegalArgumentException ex) {
                correlationId = UUID.randomUUID().toString();
                CorrelationIdContext.set(correlationId);
                MDC.put("correlationId", correlationId);
                response.setHeader(HEADER, correlationId);
                response.setStatus(ErrorCode.VALIDATION_ERROR.status().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(
                        response.getOutputStream(),
                        ApiResponse.error(ErrorCode.VALIDATION_ERROR, "X-Correlation-ID debe ser un UUID válido")
                );
                CorrelationIdContext.clear();
                MDC.remove("correlationId");
                return;
            }
        }

        CorrelationIdContext.set(correlationId);
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
            MDC.remove("correlationId");
        }
    }
}
