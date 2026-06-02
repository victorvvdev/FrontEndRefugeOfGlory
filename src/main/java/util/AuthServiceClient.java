package util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthServiceClient {

    private static final String BASE_URL = "http://localhost:8081/auth";

    // POST /auth/login → retorna o token JWT ou null
    public static String login(String username, String password) throws IOException {
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                escape(username), escape(password)
        );
        HttpURLConnection conn = post("/login", body);
        int status = conn.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK) {
            String response = readStream(conn.getInputStream());
            conn.disconnect();
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.get("accessToken").getAsString(); // TokenResponse.accessToken
        }

        conn.disconnect();
        return null;
    }

    // POST /auth/register → retorna true se criado com sucesso
    public static boolean register(String username, String password, String email) throws IOException {
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s\"}",
                escape(username), escape(password), escape(email)
        );
        HttpURLConnection conn = post("/register", body);
        int status = conn.getResponseCode();
        conn.disconnect();
        return status == HttpURLConnection.HTTP_OK;
    }

    // GET /auth/validate → retorna true se o token atual é válido
    public static boolean validate() throws IOException {
        if (!SessionState.isLoggedIn()) return false;

        URL url = new URL(BASE_URL + "/validate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", SessionState.getBearerHeader());
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        conn.disconnect();
        return status == HttpURLConnection.HTTP_OK;
    }

    // -------------------------------------------------------------------------
    private static HttpURLConnection post(String path, String jsonBody) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private static String readStream(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}