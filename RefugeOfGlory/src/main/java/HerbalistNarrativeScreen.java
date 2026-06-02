import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.litiengine.input.IKeyboard;
import de.gurkenlabs.litiengine.input.IMouse;
import de.gurkenlabs.litiengine.resources.Resources;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HerbalistNarrativeScreen extends Screen {

    private static final Color DIALOG_BG     = new Color(0,   0,   0,   180);
    private static final Color DIALOG_BORDER = new Color(185, 145, 55,  200);
    private static final Color TEXT_COLOR    = new Color(235, 225, 200);
    private static final Color GOLD_HOVER    = new Color(215, 175, 85);
    private static final Color NAME_BG       = new Color(10,  16,  38,  230);
    private static final Color NAME_BORDER   = new Color(185, 145, 55,  215);
    private static final Color NAME_TEXT     = new Color(215, 175, 85);

    private static final double DIALOG_HEIGHT    = 200;
    private static final double DIALOG_PADDING   = 30;
    private static final int    DIALOG_FONT_SIZE = 26;
    private static final long   CHAR_DELAY       = 20;
    private static final int    SPRITE_H         = 320;

    private static final int SPRITE_FIRST = 1;
    private static final int SPRITE_LAST  = 5;
    private static final int NAMEBOX_FIRST = 4;
    private static final int NAMEBOX_LAST  = 4;
    private static final int PART7_FROM = 9;

    private static class DialogEntry {
        final String speaker;
        final String text;
        DialogEntry(String speaker, String text) { this.speaker = speaker; this.text = text; }
    }

    private final List<DialogEntry> dialogs = new ArrayList<>();
    private int     currentDialogIndex   = 0;
    private String  currentDisplayedText = "";
    private int     charIndex  = 0;
    private boolean isTyping   = false;
    private boolean finished   = false;
    private boolean listenersAdded = false;

    private Timer         typingTimer;
    private BufferedImage bgPart6;
    private BufferedImage bgPart7;
    private BufferedImage herbalistSprite;

    // Cooldown para evitar avanço múltiplo rápido
    private long lastAdvanceTime = 0;
    private static final long ADVANCE_COOLDOWN = 150;

    public HerbalistNarrativeScreen() { super("HERBALIST_NARRATIVE"); }

    @Override
    public void prepare() {
        bgPart6        = Resources.images().get("Cenários/Parte6.png");
        bgPart7        = Resources.images().get("Cenários/Parte7.png");
        herbalistSprite = Resources.images().get("Sprites/NPCs/Herbalista.png");

        if (dialogs.isEmpty()) {
            dialogs.add(new DialogEntry(null,
                    "O guerreiro seguiu seu caminho, no meio da estrada avistou uma pequena cabana " +
                            "solitária que soltava fumaça pela chaminé. O cansaço e a noite chegando o fizeram bater na porta."));

            dialogs.add(new DialogEntry(null,
                    "Quem abriu foi uma jovem, bonita como o orvalho da manhã, com as mãos manchadas de verde. " +
                            "Dentro da cabana, o cheiro das ervas fervendo disputava com o cheiro de sangue."));

            dialogs.add(new DialogEntry(null,
                    "Em uma das camas, havia uma mulher com respiração curta e com seu corpo rasgado por garras."));

            dialogs.add(new DialogEntry(null,
                    "A jovem voltou o olhar para o guerreiro, os olhos cansados mas firmes."));

            dialogs.add(new DialogEntry("Herbalista",
                    "– Encontrei ela na floresta. Disse a herbalista. — Estava colhendo raízes e algo a atacou, " +
                            "ela não viu direito, só dentes, sombra e dor. Se essa coisa continua solta vai pegar mais pessoas."));

            dialogs.add(new DialogEntry(null,
                    "O guerreiro pediu para ver os ferimentos. Três cortes fundos, fundos demais para ser um lobo " +
                            "e largos demais para ser um urso. Ele conhecia aquela assinatura."));

            dialogs.add(new DialogEntry(null,
                    "– Pantera deslocadora. Murmurou o guerreiro."));

            dialogs.add(new DialogEntry(null,
                    "— Ela não ataca duas vezes no mesmo lugar mas volta pro covil para lamber o sangue de suas garras."));

            dialogs.add(new DialogEntry(null,
                    "No dia seguinte, seguiu os rastros. Sangue seco nas folhas, galhos quebrados, a trilha o levou " +
                            "para longe da cabana, até uma caverna."));

            dialogs.add(new DialogEntry(null,
                    "O que ele não imaginava é que a caverna era guardada por um golem."));
        }

        reset();
        if (!listenersAdded) { setupInput(); listenersAdded = true; }
        super.prepare();
    }

    private void reset() {
        currentDialogIndex = 0; currentDisplayedText = ""; charIndex = 0;
        isTyping = false; finished = false; stopTypingTimer();
        if (!dialogs.isEmpty()) startDialog(0);
    }

    private void setupInput() {
        Input.keyboard().onKeyPressed(KeyEvent.VK_ENTER, (IKeyboard.KeyPressedListener) e -> advanceDialog());
        Input.keyboard().onKeyPressed(KeyEvent.VK_SPACE, (IKeyboard.KeyPressedListener) e -> advanceDialog());
        Input.mouse().onClicked((IMouse.MouseClickedListener) e -> advanceDialog());
    }

    private void startDialog(int index) {
        if (finished) return;
        if (index >= 0 && index < dialogs.size()) {
            stopTypingTimer(); currentDialogIndex = index;
            currentDisplayedText = ""; charIndex = 0; isTyping = true;
            startTypingTimer();
        } else { finishScene(); }
    }

    private void startTypingTimer() {
        stopTypingTimer();
        typingTimer = new Timer(true);
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!isTyping || finished || currentDialogIndex >= dialogs.size()) { stopTypingTimer(); return; }
                String full = dialogs.get(currentDialogIndex).text;
                if (charIndex < full.length()) { currentDisplayedText += full.charAt(charIndex); charIndex++; }
                else { isTyping = false; stopTypingTimer(); }
            }
        }, 50, CHAR_DELAY);
    }

    private void stopTypingTimer() { if (typingTimer != null) { typingTimer.cancel(); typingTimer = null; } }

    private void advanceDialog() {
        if (!Game.screens().current().getName().equals("HERBALIST_NARRATIVE")) return;

        long now = System.currentTimeMillis();
        if (now - lastAdvanceTime < ADVANCE_COOLDOWN) return;
        lastAdvanceTime = now;

        if (finished || dialogs.isEmpty()) return;
        if (isTyping) {
            currentDisplayedText = dialogs.get(currentDialogIndex).text;
            charIndex = currentDisplayedText.length(); isTyping = false; stopTypingTimer();
        } else if (currentDialogIndex + 1 < dialogs.size()) {
            startDialog(currentDialogIndex + 1);
        } else { finishScene(); }
    }

    private void finishScene() {
        if (finished) return;
        finished = true;
        stopTypingTimer();
        // Troca de tela agendada para o próximo frame
        Game.loop().perform(1, () -> Game.screens().display("GOLEM_BATTLE"));
    }

    @Override
    public void render(Graphics2D g) {
        int sw = (int) Game.window().getResolution().getWidth();
        int sh = (int) Game.window().getResolution().getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferedImage bg = (currentDialogIndex >= PART7_FROM) ? bgPart7 : bgPart6;
        if (bg != null) g2.drawImage(bg, 0, 0, sw, sh, null);
        else { g2.setColor(new Color(20, 20, 40)); g2.fillRect(0, 0, sw, sh); }

        boolean mostrarSprite = currentDialogIndex >= SPRITE_FIRST && currentDialogIndex <= SPRITE_LAST && herbalistSprite != null;
        if (mostrarSprite) renderHerbalistSprite(g2, sw, sh);

        if (!dialogs.isEmpty() && !finished && currentDialogIndex < dialogs.size())
            renderDialogBox(g2, sw, sh);

        g2.dispose(); super.render(g);
    }

    private void renderHerbalistSprite(Graphics2D g, int sw, int sh) {
        double ratio = (double) herbalistSprite.getWidth() / herbalistSprite.getHeight();
        int spriteH  = SPRITE_H;
        int spriteW  = (int) (spriteH * ratio);
        int dialogY  = sh - (int) DIALOG_HEIGHT - 20;
        int spriteY  = dialogY - spriteH + 60;
        int spriteX  = sw - spriteW - 40;
        g.drawImage(herbalistSprite, spriteX, spriteY, spriteW, spriteH, null);
    }

    private void renderDialogBox(Graphics2D g, int sw, int sh) {
        int dialogY = sh - (int) DIALOG_HEIGHT - 20, dialogX = 30, dialogW = sw - 60;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(dialogX + 3, dialogY + 3, dialogW, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BG);
        g.fill(new RoundRectangle2D.Double(dialogX, dialogY, dialogW, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BORDER); g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(dialogX, dialogY, dialogW, DIALOG_HEIGHT, 15, 15));

        String speaker = dialogs.get(currentDialogIndex).speaker;
        if (speaker != null) renderNameBox(g, dialogX, dialogY, speaker);

        renderDialogText(g, currentDisplayedText, dialogX + (int) DIALOG_PADDING, dialogY + 40, dialogW - (int)(DIALOG_PADDING * 2));

        if (!isTyping && !finished && (System.currentTimeMillis() / 500) % 2 == 0) {
            String ind = "▼"; g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(GOLD_HOVER);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ind, dialogX + dialogW - 30 - fm.stringWidth(ind) / 2, dialogY + (int) DIALOG_HEIGHT - 15);
        }
    }

    private void renderNameBox(Graphics2D g, int dialogX, int dialogY, String name) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        int padH = 13, padV = 8;
        int boxW = fm.stringWidth(name) + padH * 2;
        int boxH = fm.getHeight() + padV * 2;
        int boxX = dialogX + 20, boxY = dialogY - boxH + 2;
        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(boxX + 2, boxY + 2, boxW, boxH, 8, 8));
        g.setColor(NAME_BG);
        g.fill(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 8, 8));
        g.setColor(NAME_BORDER); g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 8, 8));
        g.setColor(NAME_TEXT);
        g.drawString(name, boxX + padH, boxY + padV + fm.getAscent());
    }

    private void renderDialogText(Graphics2D g, String text, int x, int y, int maxW) {
        g.setFont(new Font("SansSerif", Font.PLAIN, DIALOG_FONT_SIZE)); g.setColor(TEXT_COLOR);
        String[] lines = wrapText(text != null ? text : "", g.getFontMetrics(), maxW);
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], x, y + i * 34);
    }

    private String[] wrapText(String text, FontMetrics fm, int maxW) {
        if (text.isEmpty()) return new String[]{""};
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (fm.stringWidth(cur + " " + w) <= maxW) { if (cur.length() > 0) cur.append(" "); cur.append(w); }
            else { if (cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(w); } else { lines.add(w); cur = new StringBuilder(); } }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines.toArray(new String[0]);
    }

    @Override public void suspend() { super.suspend(); stopTypingTimer(); }
}