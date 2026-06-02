package util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP para o microsserviço de personagens (http://localhost:8082).
 * Ajuste a porta conforme seu application.properties do character-service.
 */
public class CharacterServiceClient {

    private static final String BASE_URL = "http://localhost:8084";

    /**
     * POST /characters?name={name}&characterClass={class}&userId={userId}
     * Cria o personagem no banco e retorna o ID gerado.
     *
     * @return ID do personagem criado, ou -1 em caso de falha.
     */
    public static long createCharacter(String name, String characterClass) throws IOException {
        long userId = SessionState.getUserId();
        if (userId == 0) throw new IllegalStateException("Usuário não autenticado.");

        String url = String.format(
                "%s/characters?name=%s&characterClass=%s&userId=%d",
                BASE_URL,
                encode(name),
                encode(characterClass),
                userId
        );

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", SessionState.getBearerHeader());
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK) {
            String response = readStream(conn.getInputStream());
            conn.disconnect();
            System.out.println("[CharacterServiceClient] Personagem criado: " + response);

            // A resposta é o objeto Character com id, name, characterClass, etc.
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.has("id") ? json.get("id").getAsLong() : -1L;
        }

        System.out.println("[CharacterServiceClient] Falha ao criar personagem. HTTP: " + status);
        conn.disconnect();
        return -1L;
    }

    /**
     * GET /characters/{id}
     * Retorna os dados completos do personagem (CharacterDTO).
     */
    public static JsonObject getCharacter(long characterId) throws IOException {
        String url = BASE_URL + "/characters/" + characterId;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", SessionState.getBearerHeader());
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK) {
            String response = readStream(conn.getInputStream());
            conn.disconnect();
            return JsonParser.parseString(response).getAsJsonObject();
        }

        conn.disconnect();
        return null;
    }

    // -------------------------------------------------------------------------
    private static String readStream(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}