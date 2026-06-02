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

public class PostFirstBattleScreen extends Screen {

    private static final Color DIALOG_BG     = new Color(0,   0,   0,   180);
    private static final Color DIALOG_BORDER = new Color(185, 145, 55,  200);
    private static final Color TEXT_COLOR    = new Color(235, 225, 200);
    private static final Color GOLD_HOVER    = new Color(215, 175, 85);

    private static final double DIALOG_HEIGHT  = 200;
    private static final double DIALOG_PADDING = 30;
    private static final int    DIALOG_FONT_SIZE = 26;
    private static final long   CHAR_DELAY       = 20;

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
    private BufferedImage backgroundImage;

    // Cooldown para evitar avanço múltiplo rápido
    private long lastAdvanceTime = 0;
    private static final long ADVANCE_COOLDOWN = 150;

    public PostFirstBattleScreen() { super("POST_FIRST_BATTLE"); }

    @Override
    public void prepare() {
        backgroundImage = Resources.images().get("Cenários/Parte5.png");

        if (dialogs.isEmpty()) {
            dialogs.add(new DialogEntry(null,
                    "Os monstros que antes haviam ensanguentado aquelas ruas com o sangue de inocentes, " +
                            "agora manchavam a terra com seu próprio sangue."));
            dialogs.add(new DialogEntry(null,
                    "A vila estava habitável mais uma vez. Livre do perigo, mas não da lembrança. " +
                            "Nem da lembrança dolorida, tampouco da lembrança de gratidão que teriam pelo " +
                            "guerreiro nos próximos tempos."));
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
        if (!Game.screens().current().getName().equals("POST_FIRST_BATTLE")) return;

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
        Game.loop().perform(1, () -> Game.screens().display("HERBALIST_NARRATIVE"));
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
        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, sw, sh, null);
        else { g2.setColor(new Color(20, 20, 40)); g2.fillRect(0, 0, sw, sh); }
        if (!dialogs.isEmpty() && !finished && currentDialogIndex < dialogs.size()) renderDialogBox(g2, sw, sh);
        g2.dispose(); super.render(g);
    }

    private void renderDialogBox(Graphics2D g, int sw, int sh) {
        int dialogY = sh - (int) DIALOG_HEIGHT - 20, dialogX = 30, dialogW = sw - 60;
        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(dialogX + 3, dialogY + 3, dialogW, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BG);
        g.fill(new RoundRectangle2D.Double(dialogX, dialogY, dialogW, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BORDER); g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(dialogX, dialogY, dialogW, DIALOG_HEIGHT, 15, 15));
        renderDialogText(g, currentDisplayedText, dialogX + (int) DIALOG_PADDING, dialogY + 40, dialogW - (int)(DIALOG_PADDING * 2));
        if (!isTyping && !finished && (System.currentTimeMillis() / 500) % 2 == 0) {
            String ind = "▼"; g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(GOLD_HOVER);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ind, dialogX + dialogW - 30 - fm.stringWidth(ind) / 2, dialogY + (int) DIALOG_HEIGHT - 15);
        }
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