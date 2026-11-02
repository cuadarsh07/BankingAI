package in.ashish.combined.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that establishes distributed request tracing using SLF4J MDC (Mapped Diagnostic Context).
 *
 * Interview Concepts:
 * - Distributed Tracing & Correlation IDs: In high-concurrency environments with thousands of concurrent
 *   requests, thread logs interleave. By tagging every request with a unique ID and storing it in MDC,
 *   every log statement printed by any service or repository during that request automatically carries
 *   the same ID.
 * - Root-Cause Analysis: When an API fails, support or DevOps can find all associated log lines
 *   instantaneously using the Correlation ID returned in the HTTP response header and error body.
 * - Preventing ThreadLocal Leaks: Web servers (like Tomcat) reuse worker threads. The finally block
 *   calls MDC.clear() to ensure data from one request does not leak into another.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (!StringUtils.hasText(correlationId)) {
            correlationId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
