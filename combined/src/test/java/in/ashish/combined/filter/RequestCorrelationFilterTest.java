package in.ashish.combined.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    @DisplayName("Should generate new correlation ID and set MDC and response header when header is missing")
    void doFilter_NoIncomingHeader_GeneratesNewId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcCaptured.set(MDC.get(RequestCorrelationFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertTrue(responseHeader.startsWith("REQ-"));
        assertEquals(responseHeader, mdcCaptured.get());

        // Verify MDC cleaned up after request completes
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Should preserve incoming correlation ID from client")
    void doFilter_WithIncomingHeader_PreservesId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String clientReqId = "CLIENT-CUSTOM-12345";
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, clientReqId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcCaptured.set(MDC.get(RequestCorrelationFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertEquals(clientReqId, responseHeader);
        assertEquals(clientReqId, mdcCaptured.get());

        // Verify MDC cleaned up
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }
}
