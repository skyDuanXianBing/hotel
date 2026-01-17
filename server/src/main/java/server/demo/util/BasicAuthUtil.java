package server.demo.util;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BasicAuthUtil {

    private BasicAuthUtil() {}

    public static boolean isBasicAuthOk(HttpServletRequest request, String expectedUsername, String expectedPassword) {
        if (request == null) {
            return false;
        }
        if (expectedUsername == null || expectedUsername.isBlank() || expectedPassword == null || expectedPassword.isBlank()) {
            return false;
        }

        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return false;
        }
        String prefix = "Basic ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }
        String encoded = header.substring(prefix.length()).trim();
        if (encoded.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            String credential = new String(decoded, StandardCharsets.UTF_8);
            int idx = credential.indexOf(':');
            if (idx <= 0) {
                return false;
            }
            String user = credential.substring(0, idx);
            String pass = credential.substring(idx + 1);
            return user.equals(expectedUsername) && pass.equals(expectedPassword);
        } catch (Exception ignored) {
            return false;
        }
    }
}

