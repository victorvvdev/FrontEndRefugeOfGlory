import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.litiengine.input.IKeyboard;
import de.gurkenlabs.litiengine.resources.Resources;
import util.BattleServiceClient;
import util.BattleServiceClient.BattleData;
import util.BattleServiceClient.ParticipantData;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GolemBattleScreen extends Screen {

    // ── Paleta (Mantendo a identidade visual sombria/dourada) ───────────
    private static final Color GOLD        = new Color(185, 145, 55);
    private static final Color GOLD_HOVER  = new Color(215, 175, 85);
    private static final Color CARD_BG     = new Color(10,  16,  38,  230);
    private static final Color CARD_BORDER = new Color(185, 145, 55,  215);
    private static final Color TEXT_WHITE  = new Color(235, 225, 200);
    private static final Color HP_BAR_BG   = new Color(60,  20,  20);
    private static final Color HP_BAR_FG   = new Color(180, 50,  50);
    private static final Color HP_BAR_LOW  = new Color(220, 80,  30);

    // ── Layout (Ajustado para 1 único inimigo centralizado verticalmente) ──
    private static final int   HORIZONTAL_SPACING    = 300;
    private static final int   GLOBAL_VERTICAL_SHIFT = 70;
    private static final int   ENEMY_VERTICAL_SHIFT  = 70; // Alinhado ao centro assim como o player
    private static final int   HP_BAR_H              = 8;
    private static final float FADE_SPEED            = 0.04f;

    // ── Estado local seguindo o seu padrão de arquitetura ─────────────────
    private enum LocalState { PLAYER_TURN, WAITING_SERVER, VICTORY, GAME_OVER }
    private LocalState localState = LocalState.PLAYER_TURN;

    // ── Sessões de batalha (Apenas 1 elemento no array, pois é um Boss Solo) ──
    private long[]    battleIds         = { -1L };
    private boolean[] sessionPlayerTurn = { true };

    // ── View local do Golem ──────────────────────────────────────────────
    private static class EnemyView {
        String  name      = "Golem";
        int     currentHp = 0;
        int     maxHp     = 1;
        boolean alive     = false;
        float   fadeAlpha = 1f;
    }
    private final List<EnemyView> enemyViews = new ArrayList<>();

    // ── Dados do jogador ─────────────────────────────────────────────────
    private int    playerCurrentHp = 100;
    private int    playerMaxHp     = 100;
    private String playerName      = "Herói";

    // ── Seleção / UI ──────────────────────────────────────────────────────
    private int     selectedEnemyIndex = 0;
    private boolean allFadesDone       = false;
    private static final String[] MENU_OPTIONS = {"Atacar"};
    private int menuSelection = 0;

    // ── Cooldown ─────────────────────────────────────────────────────────
    private long    lastConfirmTime = 0;
    private static final long CONFIRM_COOLDOWN = 300;
    private boolean listenersAdded  = false;

    // ── Recursos ─────────────────────────────────────────────────────────
    private final String nextScreen;
    private BufferedImage background;
    private BufferedImage playerSprite;
    private final BufferedImage[] enemySprites = new BufferedImage[1];

    // ─────────────────────────────────────────────────────────────────────

    public GolemBattleScreen() { this("GOLEM_BATTLE", "POST_GOLEM_BATTLE"); }
    public GolemBattleScreen(String screenName, String nextScreen) {
        super(screenName);
        this.nextScreen = nextScreen;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void prepare() {
        background      = Resources.images().get("Cenários/Batalha2.png");
        playerSprite    = Resources.images().get("Sprites/Personagens/" + getPlayerType() + ".png");
        enemySprites[0] = Resources.images().get("Sprites/Inimigos/Golem.png");

        localState        = LocalState.WAITING_SERVER;
        allFadesDone      = false;
        battleIds         = new long[]{ -1L };
        sessionPlayerTurn = new boolean[]{ true };
        enemyViews.clear();

        if (!listenersAdded) { setupInput(); listenersAdded = true; }

        // Inicia a sessão única do Golem no servidor
        Game.loop().perform(1, () -> {
            try {
                long charId = PlayerData.getInstance().getCharacterId();

                // Chama o back-end buscando pelo tipo correto mapeado no seu BattleService ("GOLEM")
                BattleData sessionBoss = BattleServiceClient.startBattle("GOLEM", charId);

                if (sessionBoss != null) {
                    battleIds = new long[]{ sessionBoss.battleId };

                    applySessionData(0, sessionBoss);

                    if (sessionBoss.player != null) {
                        playerCurrentHp = sessionBoss.player.currentHp;
                        playerMaxHp     = sessionBoss.player.maxHp;
                        playerName      = sessionBoss.player.name;
                    }

                    resetSelection();

                    // Verifica se o Boss age primeiro
                    if (!sessionPlayerTurn[selectedEnemyIndex]) {
                        processEnemyTurn(selectedEnemyIndex);
                    } else {
                        localState = LocalState.PLAYER_TURN;
                    }
                } else {
                    fallbackToLocal();
                }
            } catch (Exception e) {
                System.err.println("[GolemBattleScreen] Erro ao iniciar batalha: " + e.getMessage());
                fallbackToLocal();
            }
        });

        super.prepare();
    }

    @Override
    public void suspend() { super.suspend(); }

    // ─────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────

    private void setupInput() {
        Input.keyboard().onKeyPressed(KeyEvent.VK_UP,    (IKeyboard.KeyPressedListener) e -> navigateMenu(-1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_DOWN,  (IKeyboard.KeyPressedListener) e -> navigateMenu(1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_LEFT,  (IKeyboard.KeyPressedListener) e -> changeTarget(-1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_RIGHT, (IKeyboard.KeyPressedListener) e -> changeTarget(1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_ENTER, (IKeyboard.KeyPressedListener) e -> handleConfirm());
        Input.keyboard().onKeyPressed(KeyEvent.VK_SPACE, (IKeyboard.KeyPressedListener) e -> handleConfirm());
    }

    private void handleConfirm() {
        if (!Game.screens().current().getName().equals(getName())) return;

        long now = System.currentTimeMillis();
        if (now - lastConfirmTime < CONFIRM_COOLDOWN) return;
        lastConfirmTime = now;

        if (localState == LocalState.VICTORY && allFadesDone) {
            Game.loop().perform(1, () -> Game.screens().display(nextScreen));
            return;
        }
        if (localState == LocalState.GAME_OVER) { prepare(); return; }
        if (localState == LocalState.WAITING_SERVER) return;
        if (localState == LocalState.PLAYER_TURN) confirmAction();
    }

    private void navigateMenu(int dir) {
        if (!Game.screens().current().getName().equals(getName())) return;
        if (localState != LocalState.PLAYER_TURN) return;
        menuSelection = (menuSelection + dir + MENU_OPTIONS.length) % MENU_OPTIONS.length;
    }

    private void changeTarget(int dir) {
        if (!Game.screens().current().getName().equals(getName())) return;
        if (localState != LocalState.PLAYER_TURN) return;
        // Sendo boss único, a lógica de alternar alvos apenas garante segurança matemática
        int newIdx = selectedEnemyIndex;
        int tries  = 0;
        do {
            newIdx = (newIdx + dir + enemyViews.size()) % enemyViews.size();
            tries++;
        } while (!enemyViews.get(newIdx).alive && tries < enemyViews.size());
        if (enemyViews.get(newIdx).alive) selectedEnemyIndex = newIdx;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ações de batalha
    // ─────────────────────────────────────────────────────────────────────

    private void confirmAction() {
        if (selectedEnemyIndex >= battleIds.length || battleIds[selectedEnemyIndex] < 0) return;

        // Se o estado local diz que é a vez do jogador, ele DEVE conseguir atacar,
        // independente do que a flag da sessão salvou no último turno.
        long targetBattleId = battleIds[selectedEnemyIndex];
        localState = LocalState.WAITING_SERVER;

        Game.loop().perform(1, () -> {
            try {
                // Envia o ataque do jogador para o servidor
                BattleData result = BattleServiceClient.executeAction(targetBattleId, "PHYSICAL");
                if (result != null) {
                    applyPlayerHp(result);
                    applySessionData(selectedEnemyIndex, result);

                    if (result.isGameOver() || playerCurrentHp <= 0) {
                        playerCurrentHp = 0;
                        localState = LocalState.GAME_OVER;
                    } else if (result.isVictory()) {
                        allFadesDone = false;
                        localState = LocalState.VICTORY;
                    } else {
                        // Se o Golem ainda está vivo, verifica quem joga agora.
                        // Se o servidor mudou o turno para o inimigo, engatilha o turno dele.
                        if (!sessionPlayerTurn[selectedEnemyIndex]) {
                            processEnemyTurn(selectedEnemyIndex);
                        } else {
                            localState = LocalState.PLAYER_TURN;
                        }
                    }
                } else {
                    localState = LocalState.PLAYER_TURN;
                }
            } catch (Exception e) {
                System.err.println("[GolemBattleScreen] Erro na ação do jogador: " + e.getMessage());
                localState = LocalState.PLAYER_TURN;
            } finally {
                lastConfirmTime = 0;
            }
        });
    }

    private void processEnemyTurn(int enemyIdx) {
        long targetBattleId = battleIds[enemyIdx];
        localState = LocalState.WAITING_SERVER;

        // Delay de 1 segundo para o jogador ver que o Golem está pensando/agindo
        Game.loop().perform(1000, () -> {
            try {
                // O servidor executa a ação do Golem e avança o turno de volta para o Player
                BattleData result = BattleServiceClient.executeAction(targetBattleId, "PHYSICAL");
                if (result != null) {
                    applyPlayerHp(result);
                    applySessionData(enemyIdx, result);

                    if (result.isGameOver() || playerCurrentHp <= 0) {
                        playerCurrentHp = 0;
                        localState = LocalState.GAME_OVER;
                    } else if (result.isVictory()) {
                        allFadesDone = false;
                        localState = LocalState.VICTORY;
                    } else {
                        // Se o servidor devolveu o turno para o jogador, libera a UI
                        if (sessionPlayerTurn[enemyIdx]) {
                            localState = LocalState.PLAYER_TURN;
                        } else {
                            // Caso bizarro do Golem ter múltiplos turnos, chama de novo
                            processEnemyTurn(enemyIdx);
                        }
                    }
                } else {
                    localState = LocalState.PLAYER_TURN;
                }
            } catch (Exception e) {
                System.err.println("[GolemBattleScreen] Erro no turno inimigo: " + e.getMessage());
                localState = LocalState.PLAYER_TURN;
            } finally {
                lastConfirmTime = 0;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sincronização com dados do servidor
    // ─────────────────────────────────────────────────────────────────────

    private synchronized void applySessionData(int enemyIdx, BattleData data) {
        while (enemyViews.size() <= enemyIdx) enemyViews.add(new EnemyView());

        EnemyView view = enemyViews.get(enemyIdx);

        if (data.enemies != null && !data.enemies.isEmpty()) {
            ParticipantData srv = data.enemies.get(0);
            view.name      = srv.name;
            view.currentHp = srv.currentHp;
            view.maxHp     = srv.maxHp;
            boolean wasAlive = view.alive;
            view.alive     = srv.alive;
            if (wasAlive && !view.alive && view.fadeAlpha == 1f) view.fadeAlpha = 0.99f;
        }

        if (enemyIdx < sessionPlayerTurn.length) {
            sessionPlayerTurn[enemyIdx] = data.playerTurn;
        }
    }

    private void applyPlayerHp(BattleData data) {
        if (data.player != null) {
            playerCurrentHp = data.player.currentHp;
            playerMaxHp     = data.player.maxHp;
            playerName      = data.player.name;
        }
    }

    private void resetSelection() {
        for (int i = 0; i < enemyViews.size(); i++) {
            if (enemyViews.get(i).alive) { selectedEnemyIndex = i; return; }
        }
    }

    private void fallbackToLocal() {
        playerCurrentHp   = 100; playerMaxHp = 100; playerName = getPlayerType();
        sessionPlayerTurn = new boolean[]{ true };

        enemyViews.clear();
        EnemyView boss = new EnemyView();
        boss.name = "Golem"; boss.maxHp = 100; boss.currentHp = 100; boss.alive = true;
        enemyViews.add(boss);

        selectedEnemyIndex = 0;
        localState = LocalState.PLAYER_TURN;
        System.out.println("[GolemBattleScreen] Rodando em modo offline (fallback local).");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g) {
        int sw = (int) Game.window().getResolution().getWidth();
        int sh = (int) Game.window().getResolution().getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);

        if (background != null) g2.drawImage(background, 0, 0, sw, sh, null);
        else { g2.setColor(Color.DARK_GRAY); g2.fillRect(0, 0, sw, sh); }

        int centerX = sw / 2;

        // ── Jogador ──────────────────────────────────────────────────────
        if (playerSprite != null) {
            int pw = playerSprite.getWidth();
            int px = centerX - HORIZONTAL_SPACING - pw;
            int py = (sh - playerSprite.getHeight()) / 2 + GLOBAL_VERTICAL_SHIFT;
            g2.drawImage(playerSprite, px, py, null);
            drawHPBar(g2, px, py - 20, pw, playerCurrentHp, playerMaxHp);
            drawName(g2, px, py - 35, pw, playerName);
        }

        // ── Boss (Golem) ─────────────────────────────────────────────────
        tickFades();
        boolean anyFading = false;
        int enemyX = centerX + HORIZONTAL_SPACING;

        for (int i = 0; i < enemyViews.size(); i++) {
            EnemyView ev = enemyViews.get(i);
            if (ev.fadeAlpha <= 0f) continue;
            if (!ev.alive && ev.fadeAlpha < 1f) anyFading = true;

            BufferedImage sprite = (i < enemySprites.length) ? enemySprites[i] : null;
            int ew = (sprite != null) ? sprite.getWidth()  : 80;
            int eh = (sprite != null) ? sprite.getHeight() : 80;

            // Layout centralizado: Como é 1 único Boss, tiramos os offsets verticais multiplicadores multiplicados da FirstBattle
            int ey = (sh - eh) / 2 + ENEMY_VERTICAL_SHIFT;

            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ev.fadeAlpha));
            if (sprite != null) g2.drawImage(sprite, enemyX, ey, null);
            else { g2.setColor(new Color(80, 40, 80)); g2.fillRoundRect(enemyX, ey, ew, eh, 10, 10); }
            g2.setComposite(orig);

            if (ev.alive) {
                drawHPBar(g2, enemyX, ey - 20, ew, ev.currentHp, ev.maxHp);
                drawName(g2, enemyX, ey - 35, ew, ev.name);
            }

            if (localState == LocalState.PLAYER_TURN && i == selectedEnemyIndex && ev.alive) {
                g2.setColor(new Color(185, 145, 55, 215));
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(enemyX - 2, ey - 2, ew + 4, eh + 4);
            }
        }

        if (localState == LocalState.VICTORY && !anyFading) {
            boolean stillFading = enemyViews.stream().anyMatch(ev -> !ev.alive && ev.fadeAlpha > 0f);
            allFadesDone = !stillFading;
        }

        // ── Overlays de UI ────────────────────────────────────────────────
        renderBattleMenu(g2, sw, sh);
        if (localState == LocalState.VICTORY && allFadesDone) renderVictoryCard(g2, sw, sh);
        if (localState == LocalState.GAME_OVER)
            drawCentered(g2, "Derrota — Enter para tentar novamente", sw, sh / 2,
                    new Font("SansSerif", Font.BOLD, 26), new Color(200, 60, 60));
        if (localState == LocalState.WAITING_SERVER)
            drawCentered(g2, "Aguardando servidor...", sw, sh / 2 + 50,
                    new Font("SansSerif", Font.PLAIN, 16), new Color(180, 170, 140));

        g2.dispose();
        super.render(g);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers de render
    // ─────────────────────────────────────────────────────────────────────

    private void tickFades() {
        for (EnemyView ev : enemyViews) {
            if (!ev.alive && ev.fadeAlpha > 0f)
                ev.fadeAlpha = Math.max(0f, ev.fadeAlpha - FADE_SPEED);
        }
    }

    private void drawHPBar(Graphics2D g, int x, int y, int w, int cur, int max) {
        double ratio = Math.max(0, (double) cur / max);
        g.setColor(HP_BAR_BG); g.fillRoundRect(x, y, w, HP_BAR_H, 6, 6);
        g.setColor(ratio < 0.3 ? HP_BAR_LOW : HP_BAR_FG);
        g.fillRoundRect(x, y, (int)(w * ratio), HP_BAR_H, 6, 6);
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, HP_BAR_H, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.setColor(TEXT_WHITE);
        String txt = cur + "/" + max;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, x + (w - fm.stringWidth(txt)) / 2, y + HP_BAR_H - 1);
    }

    private void drawName(Graphics2D g, int x, int y, int w, String name) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(GOLD);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(name, x + (w - fm.stringWidth(name)) / 2, y);
    }

    private void renderBattleMenu(Graphics2D g, int sw, int sh) {
        int mw = 360, mh = 200, mx = (sw - mw) / 2, my = sh - mh - 30;
        g.setColor(CARD_BG); g.fillRoundRect(mx, my, mw, mh, 15, 15);
        g.setColor(CARD_BORDER); g.setStroke(new BasicStroke(2)); g.drawRoundRect(mx, my, mw, mh, 15, 15);

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        int tx = mx + 35, ty = my + 25 + fm.getAscent();

        switch (localState) {
            case PLAYER_TURN -> {
                g.setColor(GOLD); g.drawString(MENU_OPTIONS[menuSelection], tx, ty);
                g.drawString("► ", mx + 8, ty);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(150, 140, 120));
                g.drawString("Enter para atacar o Golem", tx, ty + 28);
            }
            case WAITING_SERVER -> {
                g.setColor(new Color(180, 170, 140));
                g.drawString("Processando...", tx, ty);
            }
            default -> {}
        }
    }

    private void renderVictoryCard(Graphics2D g, int sw, int sh) {
        int cw = 420, ch = 200, cx = (sw - cw) / 2, cy = (sh - ch) / 2;
        g.setColor(new Color(0, 0, 0, 120)); g.fillRoundRect(cx + 5, cy + 6, cw, ch, 18, 18);
        g.setColor(CARD_BG); g.fillRoundRect(cx, cy, cw, ch, 18, 18);
        g.setColor(CARD_BORDER); g.setStroke(new BasicStroke(2f)); g.drawRoundRect(cx, cy, cw, ch, 18, 18);

        g.setFont(new Font("Georgia", Font.BOLD, 42)); g.setColor(GOLD_HOVER);
        FontMetrics fm = g.getFontMetrics();
        String title = "Vitória";
        g.drawString(title, cx + (cw - fm.stringWidth(title)) / 2, cy + 80);

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(165, 155, 130));
            fm = g.getFontMetrics();
            String sub = "Pressione Enter para continuar";
            g.drawString(sub, cx + (cw - fm.stringWidth(sub)) / 2, cy + 130);
        }
    }

    private void drawCentered(Graphics2D g, String text, int sw, int y, Font font, Color color) {
        g.setFont(font); g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (sw - fm.stringWidth(text)) / 2, y);
    }

    private String getPlayerType() {
        String c = PlayerData.getInstance().getSelectedCharacter();
        return "ESPADACHIM".equals(c) ? "Espadachim" : "Barbaro";
    }
}