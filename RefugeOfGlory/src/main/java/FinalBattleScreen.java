import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.litiengine.resources.Resources;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class FinalBattleScreen extends Screen {

    private static final Color GOLD         = new Color(185, 145, 55);
    private static final Color GOLD_HOVER   = new Color(215, 175, 85);
    private static final Color CARD_BG      = new Color(10,  16,  38,  230);
    private static final Color CARD_BORDER  = new Color(185, 145, 55,  215);
    private static final Color TEXT_WHITE   = new Color(235, 225, 200);
    private static final Color HP_BAR_BG    = new Color(60,  20,  20);
    private static final Color HP_BAR_FG    = new Color(180, 50,  50);
    private static final Color HP_BAR_LOW   = new Color(220, 80,  30);

    private enum BattleState {
        PLAYER_TURN, PLAYER_CHOOSING_TARGET, ENEMY_TURN, VICTORY, GAME_OVER
    }
    private BattleState state = BattleState.PLAYER_TURN;

    private static final int PLAYER_MAX_HP = 100;
    private int playerHP = PLAYER_MAX_HP;

    private static class Enemy {
        final String name;
        final BufferedImage sprite;
        final int maxHp;
        int hp;
        boolean alive;
        float fadeAlpha = 1f;

        Enemy(String name, BufferedImage sprite, int maxHp) {
            this.name = name; this.sprite = sprite;
            this.maxHp = maxHp; this.hp = maxHp; this.alive = true;
        }
    }

    private final List<Enemy> enemies = new ArrayList<>();
    private int selectedEnemyIndex = 0;

    private static final float FADE_SPEED = 0.04f;
    private boolean allFadesDone = false;

    private static final int HORIZONTAL_SPACING   = 300;
    private static final int GLOBAL_VERTICAL_SHIFT = 70;
    private static final int ENEMY_VERTICAL_SHIFT  = 70;
    private static final int HP_BAR_H = 8;

    private static final String[] MENU_OPTIONS = {"Atacar"};
    private int     menuSelection  = 0;
    private boolean menuActive     = false;
    private boolean listenersAdded = false;

    private final String nextScreen;

    // Cooldown para evitar múltiplas confirmações rápidas
    private long lastConfirmTime = 0;
    private static final long CONFIRM_COOLDOWN = 300;

    // ── CONSTRUTOR MODIFICADO ─────────────────────────────────────────
    public FinalBattleScreen() {
        this("FINAL_BATTLE", "POST_FINAL_BATTLE");   // <-- AGORA APONTA PARA A NOVA TELA
    }

    public FinalBattleScreen(String screenName, String nextScreen) {
        super(screenName);
        this.nextScreen = nextScreen;
    }

    private BufferedImage background;
    private BufferedImage playerSprite;

    @Override
    public void prepare() {
        background   = Resources.images().get("Cenários/BatalhaFinal.png");
        playerSprite = Resources.images().get("Sprites/Personagens/" + getPlayerType() + ".png");

        int random = (int)(Math.random() * 100);
        BufferedImage bossSprite;
        String bossName;
        int bossHp;

        if (random % 2 == 0) {
            bossSprite = Resources.images().get("Sprites/Inimigos/Owlbear.png");
            bossName = "Owlbear";
            bossHp = 130;
        } else {
            bossSprite = Resources.images().get("Sprites/Inimigos/Demonio.png");
            bossName = "Demon";
            bossHp = 150;
        }

        enemies.clear();
        enemies.add(new Enemy(bossName, bossSprite, bossHp));

        playerHP = PLAYER_MAX_HP;
        state = BattleState.PLAYER_TURN;
        menuActive = true;
        menuSelection = 0;
        selectedEnemyIndex = 0;
        allFadesDone = false;

        if (!listenersAdded) { setupInput(); listenersAdded = true; }
        super.prepare();
    }

    private String getPlayerType() {
        String c = PlayerData.getInstance().getSelectedCharacter();
        return "ESPADACHIM".equals(c) ? "Espadachim" : "Barbaro";
    }

    private void setupInput() {
        Input.keyboard().onKeyPressed(KeyEvent.VK_UP,    e -> navigateMenu(-1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_DOWN,  e -> navigateMenu(1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_LEFT,  e -> changeTarget(-1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_RIGHT, e -> changeTarget(1));
        Input.keyboard().onKeyPressed(KeyEvent.VK_ENTER, e -> handleConfirm());
        Input.keyboard().onKeyPressed(KeyEvent.VK_SPACE, e -> handleConfirm());
    }

    private void handleConfirm() {
        if (!Game.screens().current().getName().equals(getName())) return;

        long now = System.currentTimeMillis();
        if (now - lastConfirmTime < CONFIRM_COOLDOWN) return;
        lastConfirmTime = now;

        if (state == BattleState.VICTORY && allFadesDone) {
            Game.loop().perform(1, () -> Game.screens().display(nextScreen));
            return;
        }
        if (state == BattleState.GAME_OVER) {
            prepare();
            return;
        }
        confirmAction();
    }

    private void navigateMenu(int dir) {
        if (state != BattleState.PLAYER_TURN) return;
        menuSelection = (menuSelection + dir + MENU_OPTIONS.length) % MENU_OPTIONS.length;
    }

    private void changeTarget(int dir) {
        if (state != BattleState.PLAYER_CHOOSING_TARGET || enemies.size() <= 1) return;
        int newIndex = selectedEnemyIndex; int attempts = 0;
        do { newIndex = (newIndex + dir + enemies.size()) % enemies.size(); attempts++; }
        while (!enemies.get(newIndex).alive && attempts < enemies.size());
        if (enemies.get(newIndex).alive) selectedEnemyIndex = newIndex;
    }

    private void confirmAction() {
        if (state == BattleState.PLAYER_TURN && menuSelection == 0) {
            attackEnemy(0);
            if (allEnemiesDead()) {
                state = BattleState.VICTORY;
                menuActive = false;
            } else {
                state = BattleState.ENEMY_TURN;
                menuActive = false;
                performEnemyTurns();
            }
        } else if (state == BattleState.PLAYER_CHOOSING_TARGET) {
            attackEnemy(selectedEnemyIndex);
            if (allEnemiesDead()) {
                state = BattleState.VICTORY;
                menuActive = false;
            } else {
                state = BattleState.ENEMY_TURN;
                menuActive = false;
                performEnemyTurns();
            }
        }
    }

    private void attackEnemy(int index) {
        if (index < 0 || index >= enemies.size()) return;
        Enemy target = enemies.get(index);
        if (!target.alive) return;
        int damage = 20 + (int)(Math.random() * 10);
        target.hp -= damage;
        if (target.hp <= 0) { target.hp = 0; target.alive = false; }
    }

    private void performEnemyTurns() {
        for (int i = 0; i < enemies.size(); i++) {
            final int idx = i;
            Game.loop().perform(200 + i * 200, () -> {
                if (state == BattleState.VICTORY || state == BattleState.GAME_OVER) return;
                Enemy e = enemies.get(idx);
                if (!e.alive) return;
                int dmg = 10 + (int)(Math.random() * 8);
                playerHP -= dmg;
                if (playerHP <= 0) {
                    playerHP = 0;
                    state = BattleState.GAME_OVER;
                    menuActive = false;
                }
                if (idx == enemies.size() - 1 && state != BattleState.GAME_OVER) {
                    state = BattleState.PLAYER_TURN;
                    menuActive = true;
                    menuSelection = 0;
                }
            });
        }
    }

    private boolean allEnemiesDead() {
        return enemies.stream().noneMatch(e -> e.alive);
    }

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

        if (playerSprite != null) {
            int pw = playerSprite.getWidth();
            int px = centerX - HORIZONTAL_SPACING - pw;
            int py = (sh - playerSprite.getHeight()) / 2 + GLOBAL_VERTICAL_SHIFT;
            g2.drawImage(playerSprite, px, py, null);
            drawHPBar(g2, px, py - 20, pw, playerHP, PLAYER_MAX_HP);
            drawName(g2, px, py - 35, pw, getPlayerType());
        }

        tickFades();
        boolean anyFading = false;
        int enemyX = centerX + HORIZONTAL_SPACING;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.fadeAlpha <= 0f) continue;
            if (!e.alive && e.fadeAlpha < 1f) anyFading = true;

            int ew = (e.sprite != null) ? e.sprite.getWidth() : 80;
            int eh = (e.sprite != null) ? e.sprite.getHeight() : 80;
            int ey = (sh - eh) / 2 + ENEMY_VERTICAL_SHIFT;

            Composite original = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.fadeAlpha));
            if (e.sprite != null) g2.drawImage(e.sprite, enemyX, ey, null);
            else { g2.setColor(new Color(80, 40, 80)); g2.fillRoundRect(enemyX, ey, ew, eh, 10, 10); }
            g2.setComposite(original);

            if (e.alive) {
                drawHPBar(g2, enemyX, ey - 20, ew, e.hp, e.maxHp);
                drawName(g2, enemyX, ey - 35, ew, e.name);
            }

            if (state == BattleState.PLAYER_CHOOSING_TARGET && i == selectedEnemyIndex && e.alive) {
                g2.setColor(new Color(185, 145, 55, 215));
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(enemyX - 2, ey - 2, ew + 4, eh + 4);
            }
        }

        if (state == BattleState.VICTORY && !anyFading) {
            boolean stillFading = enemies.stream().anyMatch(e -> !e.alive && e.fadeAlpha > 0f);
            allFadesDone = !stillFading;
        }

        if (menuActive) renderBattleMenu(g2, sw, sh);

        if (state == BattleState.VICTORY && allFadesDone) renderVictoryCard(g2, sw, sh);

        if (state == BattleState.GAME_OVER)
            drawCentered(g2, "Derrota — Enter para tentar novamente", sw, sh / 2,
                    new Font("SansSerif", Font.BOLD, 26), new Color(200, 60, 60));

        g2.dispose();
        super.render(g);
    }

    private void tickFades() {
        for (Enemy e : enemies) {
            if (!e.alive && e.fadeAlpha > 0f) {
                e.fadeAlpha = Math.max(0f, e.fadeAlpha - FADE_SPEED);
            }
        }
    }

    private void drawHPBar(Graphics2D g, int x, int y, int w, int cur, int max) {
        double ratio = Math.max(0, (double) cur / max);
        g.setColor(HP_BAR_BG); g.fillRoundRect(x, y, w, HP_BAR_H, 6, 6);
        Color barColor = ratio < 0.3 ? HP_BAR_LOW : HP_BAR_FG;
        g.setColor(barColor); g.fillRoundRect(x, y, (int)(w * ratio), HP_BAR_H, 6, 6);
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(x, y, w, HP_BAR_H, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.setColor(TEXT_WHITE);
        String txt = cur + "/" + max; FontMetrics fm = g.getFontMetrics();
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
        g.setColor(state == BattleState.PLAYER_TURN ? GOLD : TEXT_WHITE);
        g.drawString(MENU_OPTIONS[menuSelection], tx, ty);

        if (state == BattleState.PLAYER_TURN) {
            g.setColor(GOLD);
            g.drawString("► ", mx + 8, ty);
        }
        if (state == BattleState.ENEMY_TURN) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.setColor(new Color(200, 80, 80));
            g.drawString("Inimigos agindo...", tx, ty);
        }
    }

    private void renderVictoryCard(Graphics2D g, int sw, int sh) {
        int cw = 420, ch = 200;
        int cx = (sw - cw) / 2, cy = (sh - ch) / 2;

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(cx + 5, cy + 6, cw, ch, 18, 18);
        g.setColor(CARD_BG);
        g.fillRoundRect(cx, cy, cw, ch, 18, 18);
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx, cy, cw, ch, 18, 18);
        g.setColor(new Color(185, 145, 55, 80));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(cx + 8, cy + 8, cw - 16, ch - 16, 12, 12);

        g.setFont(new Font("Georgia", Font.BOLD, 42));
        g.setColor(GOLD_HOVER);
        FontMetrics fm = g.getFontMetrics();
        String title = "Vitória";
        g.drawString(title, cx + (cw - fm.stringWidth(title)) / 2, cy + 80);

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(165, 155, 130));
            fm = g.getFontMetrics();
            String sub = "Pressione Enter ou clique para continuar";
            g.drawString(sub, cx + (cw - fm.stringWidth(sub)) / 2, cy + 130);
        }
    }

    private void drawCentered(Graphics2D g, String text, int sw, int y, Font font, Color color) {
        g.setFont(font); g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (sw - fm.stringWidth(text)) / 2, y);
    }

    @Override
    public void suspend() { super.suspend(); }
}