    package util;
    import com.google.gson.*;

    import java.io.*;
    import java.net.*;
    import java.nio.charset.StandardCharsets;
    import java.util.ArrayList;
    import java.util.List;

    /**
     * Cliente HTTP para o combat-service (http://localhost:8083).
     * Integra a FirstBattleScreen com o back-end real.
     */
    public class BattleServiceClient {

        private static final String BASE_URL = "http://localhost:8083";

        // ------------------------------------------------------------------
        // POST /battles/start?characterId={id}&enemyType={type}
        // ------------------------------------------------------------------

        public static BattleData startBattle(String enemyType, long characterId) throws IOException {
            String url = String.format("%s/battles/start?characterId=%d&enemyType=%s",
                    BASE_URL, characterId, URLEncoder.encode(enemyType, StandardCharsets.UTF_8));

            HttpURLConnection conn = post(url, "");
            int status = conn.getResponseCode();
            System.out.println("[BattleServiceClient] startBattle HTTP [" + enemyType + "]: " + status);

            if (status == HttpURLConnection.HTTP_OK) {
                String body = readStream(conn.getInputStream());
                System.out.println("[BattleServiceClient] startBattle RAW [" + enemyType + "]: " + body);
                conn.disconnect();
                return parseBattleData(body);
            }

            InputStream errStream = conn.getErrorStream();
            if (errStream != null) {
                System.err.println("[BattleServiceClient] startBattle erro [" + enemyType + "]: "
                        + readStream(errStream));
            }
            conn.disconnect();
            return null;
        }

        // ------------------------------------------------------------------
        // POST /battles/{battleId}/action?damageType={type}
        // ------------------------------------------------------------------

        public static BattleData executeAction(long battleId, String damageType) throws IOException {
            String url = String.format("%s/battles/%d/action?damageType=%s",
                    BASE_URL, battleId, URLEncoder.encode(damageType, StandardCharsets.UTF_8));

            HttpURLConnection conn = post(url, "");
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                String body = readStream(conn.getInputStream());
                conn.disconnect();
                return parseBattleData(body);
            }

            System.err.println("[BattleServiceClient] executeAction falhou. HTTP: " + status);
            conn.disconnect();
            return null;
        }

        // ------------------------------------------------------------------
        // GET /battles/{battleId}
        // ------------------------------------------------------------------

        public static BattleData getBattle(long battleId) throws IOException {
            URL url = new URL(BASE_URL + "/battles/" + battleId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", SessionState.getBearerHeader());
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                String body = readStream(conn.getInputStream());
                conn.disconnect();
                return parseBattleData(body);
            }

            conn.disconnect();
            return null;
        }

        // ------------------------------------------------------------------
        // DTOs — espelham BattleSession do servidor
        // ------------------------------------------------------------------

        public static class ParticipantData {
            public long    id;
            public String  name;
            public int     currentHp;
            public int     maxHp;
            public boolean alive;
            public boolean player;
        }

        public static class BattleData {
            public long                  battleId;
            public String                stateName;    // "PLAYER_TURN" | "ENEMY_TURN" | "VICTORY" | "GAME_OVER"
            public String                result;       // null | "VICTORY" | "GAME_OVER"
            public boolean               playerTurn = true; // true = vez do jogador nesta sessão
            public ParticipantData       player;
            public List<ParticipantData> enemies    = new ArrayList<>();
            public List<String>          battleLog  = new ArrayList<>();

            public boolean isOver()     { return result != null; }
            public boolean isVictory()  { return "VICTORY".equals(result); }
            public boolean isGameOver() { return "GAME_OVER".equals(result); }
        }

        // ------------------------------------------------------------------
        // Parsing
        // ------------------------------------------------------------------

        private static BattleData parseBattleData(String json) {
            try {
                JsonElement rootEl = JsonParser.parseString(json);
                if (rootEl == null || rootEl.isJsonNull() || !rootEl.isJsonObject()) {
                    System.err.println("[BattleServiceClient] JSON raiz inválido");
                    return null;
                }
                JsonObject root = rootEl.getAsJsonObject();
                BattleData data = new BattleData();

                System.out.println("[parse] lendo battleId");
                data.battleId = root.has("battleId") && !root.get("battleId").isJsonNull()
                        ? root.get("battleId").getAsLong() : 0L;

                System.out.println("[parse] lendo result");
                data.result = root.has("result") && !root.get("result").isJsonNull()
                        ? root.get("result").getAsString() : null;

                System.out.println("[parse] lendo currentState");
                if (root.has("currentState") && root.get("currentState").isJsonObject()) {
                    JsonObject s = root.getAsJsonObject("currentState");
                    data.stateName = s.has("stateName") ? s.get("stateName").getAsString() : "UNKNOWN";
                }

                System.out.println("[parse] lendo turnManager");
                if (root.has("turnManager") && root.get("turnManager").isJsonObject()) {
                    JsonObject tm = root.getAsJsonObject("turnManager");
                    data.playerTurn = !tm.has("playerTurn")
                            || tm.get("playerTurn").isJsonNull()
                            || tm.get("playerTurn").getAsBoolean();
                }

                System.out.println("[parse] lendo player");
                if (root.has("player") && root.get("player").isJsonObject()) {
                    data.player = parseParticipant(root.getAsJsonObject("player"), true);
                }

                System.out.println("[parse] lendo enemy");
                if (root.has("enemy") && root.get("enemy").isJsonObject()) {
                    data.enemies.add(parseParticipant(root.getAsJsonObject("enemy"), false));
                }

                System.out.println("[parse] lendo enemies array");
                if (root.has("enemies") && root.get("enemies").isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray("enemies")) {
                        if (el.isJsonObject()) {
                            data.enemies.add(parseParticipant(el.getAsJsonObject(), false));
                        }
                    }
                }

                System.out.println("[parse] lendo battleLog");
                if (root.has("battleLog") && root.get("battleLog").isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray("battleLog")) {
                        if (!el.isJsonNull()) data.battleLog.add(el.getAsString());
                    }
                }

                System.out.println("[parse] concluído, battleId=" + data.battleId);
                return data;

            } catch (Exception e) {
                System.err.println("[BattleServiceClient] parseBattleData EXCEPTION: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        private static ParticipantData parseParticipant(JsonObject obj, boolean isPlayer) {
            ParticipantData p = new ParticipantData();

            // "id" vem como null para inimigos — nunca chamar getAsLong() sem checar
            p.id = (obj.has("id") && !obj.get("id").isJsonNull())
                    ? obj.get("id").getAsLong() : 0L;

            p.name      = (obj.has("name")      && !obj.get("name").isJsonNull())
                    ? obj.get("name").getAsString()   : "?";
            p.currentHp = (obj.has("currentHp") && !obj.get("currentHp").isJsonNull())
                    ? obj.get("currentHp").getAsInt() : 0;
            p.maxHp     = (obj.has("maxHp")     && !obj.get("maxHp").isJsonNull())
                    ? obj.get("maxHp").getAsInt()     : 1;
            p.player    = isPlayer;

            if (obj.has("alive") && !obj.get("alive").isJsonNull()) {
                p.alive = obj.get("alive").getAsBoolean();
            } else {
                p.alive = p.currentHp > 0;
            }

            return p;
        }

        // ------------------------------------------------------------------
        // HTTP helpers
        // ------------------------------------------------------------------

        private static HttpURLConnection post(String urlStr, String jsonBody) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", SessionState.getBearerHeader());
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (jsonBody != null && !jsonBody.isBlank()) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            return conn;
        }

        private static String readStream(InputStream is) throws IOException {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }