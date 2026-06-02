package util;

/**
 * Guarda o JWT e dados da sessão em memória durante o jogo.
 */
public class SessionState {

    private static String token;
    private static long   userId;
    private static String username;

    private SessionState() {}

    public static void setToken(String jwt) {
        token    = jwt;
        userId   = extractUserId(jwt);
        username = extractUsername(jwt);
    }

    public static String getToken()    { return token; }
    public static long   getUserId()   { return userId; }
    public static String getUsername() { return username; }

    public static boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public static void clear() {
        token    = null;
        userId   = 0;
        username = null;
    }

    public static String getBearerHeader() {
        return "Bearer " + token;
    }

    // -------------------------------------------------------------------------
    // Extrai claims do payload JWT (Base64) sem validar assinatura.
    // A validação real é feita pelo servidor via /auth/validate.
    // -------------------------------------------------------------------------
    private static long extractUserId(String jwt) {
        try {
            com.google.gson.JsonObject payload = decodePayload(jwt);
            return payload.has("userId") ? payload.get("userId").getAsLong() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String extractUsername(String jwt) {
        try {
            com.google.gson.JsonObject payload = decodePayload(jwt);
            // O subject do JWT é o username (definido em JwtProvider.generateToken)
            return payload.has("sub") ? payload.get("sub").getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static com.google.gson.JsonObject decodePayload(String jwt) {
        String[] parts   = jwt.split("\\.");
        byte[]   decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
        String   json    = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}