package server.demo.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import server.demo.dto.ApiResponse;

import java.io.IOException;

/**
 * Writes {@link ApiResponse} JSON from interceptors (outside {@code @RestController} serialization).
 * Uses Jackson so localized messages with quotes/newlines stay valid JSON.
 */
public final class ApiResponseHttpWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private ApiResponseHttpWriter() {
    }

    public static void write(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(response.getWriter(), body);
    }

    public static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, ApiResponse.error(message));
    }
}
