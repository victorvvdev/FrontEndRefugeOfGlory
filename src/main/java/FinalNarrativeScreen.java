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

public class FinalNarrativeScreen extends Screen {

    private static final Color DIALOG_BG     = new Color(0,   0,   0,   180);
    private static final Color DIALOG_BORDER = new Color(185, 145, 55,  200);
    private static final Color TEXT_COLOR    = new Color(235, 225, 200);
    private static final Color GOLD_HOVER    = new Color(215, 175, 85);
    private static final Color CARD_BG       = new Color(10,  16,  38,  230);
    private static final Color CARD_BORDER   = new Color(185, 145, 55,  215);

    private static final double DIALOG_HEIGHT  = 200;
    private static final double DIALOG_PADDING = 30;
    private static final int    DIALOG_FONT_SIZE = 26;
    private static final long   CHAR_DELAY       = 20;

    private static class DialogEntry {
        final String speaker;
        final String text;
        DialogEntry(String speaker, String text) {
            this.speaker = speaker;
            this.text    = text;
        }
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

    private boolean showEndCard = false;

    // Cooldown para evitar avanço múltiplo rápido
    private long lastAdvanceTime = 0;
    private static final long ADVANCE_COOLDOWN = 150;

    public FinalNarrativeScreen() {
        super("FINAL_NARRATIVE");
    }

    @Override
    public void prepare() {
        backgroundImage = Resources.images().get("Interface/Fim.png");

        if (dialogs.isEmpty()) {
            dialogs.add(new DialogEntry(null,
                    "Dizem que só os que enfrentaram o que não tem nome e continuaram de pé encontravam o caminho."));

            dialogs.add(new DialogEntry(null,
                    "As portas só se abrem para quem carrega a cicatriz na alma e paz no coração."));

            dialogs.add(new DialogEntry(null,
                    "Os portões rangeram quando ele se aproximou, e abriram."));

            dialogs.add(new DialogEntry(null,
                    "Não houve gritos, apenas a calmaria, ali o guerreiro entendeu que seu local de descanso e glória havia chegado."));
        }

        reset();

        if (!listenersAdded) {
            setupInput();
            listenersAdded = true;
        }

        super.prepare();
    }

    private void reset() {
        currentDialogIndex   = 0;
        currentDisplayedText = "";
        charIndex = 0;
        isTyping  = false;
        finished  = false;
        showEndCard = false;
        stopTypingTimer();
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
            stopTypingTimer();
            currentDialogIndex   = index;
            currentDisplayedText = "";
            charIndex = 0;
            isTyping  = true;
            startTypingTimer();
        } else {
            showEndCard = true;
            finished = true;
            stopTypingTimer();
        }
    }

    private void startTypingTimer() {
        stopTypingTimer();
        typingTimer = new Timer(true);
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isTyping || finished || currentDialogIndex >= dialogs.size()) {
                    stopTypingTimer();
                    return;
                }
                String fullText = dialogs.get(currentDialogIndex).text;
                if (charIndex < fullText.length()) {
                    currentDisplayedText += fullText.charAt(charIndex);
                    charIndex++;
                } else {
                    isTyping = false;
                    stopTypingTimer();
                }
            }
        }, 50, CHAR_DELAY);
    }

    private void stopTypingTimer() {
        if (typingTimer != null) {
            typingTimer.cancel();
            typingTimer = null;
        }
    }

    private void advanceDialog() {
        if (!Game.screens().current().getName().equals("FINAL_NARRATIVE")) return;

        long now = System.currentTimeMillis();
        if (now - lastAdvanceTime < ADVANCE_COOLDOWN) return;
        lastAdvanceTime = now;

        // Se o card "Fim" já está visível, redireciona para o login
        if (showEndCard) {
            Game.loop().perform(1, () -> Game.screens().display("LOGIN"));
            return;
        }

        if (finished || dialogs.isEmpty()) return;

        if (isTyping) {
            currentDisplayedText = dialogs.get(currentDialogIndex).text;
            charIndex = currentDisplayedText.length();
            isTyping  = false;
            stopTypingTimer();
        } else if (currentDialogIndex + 1 < dialogs.size()) {
            startDialog(currentDialogIndex + 1);
        } else {
            showEndCard = true;
            finished = true;
            stopTypingTimer();
        }
    }

    @Override
    public void render(Graphics2D g) {
        int sw = (int) Game.window().getResolution().getWidth();
        int sh = (int) Game.window().getResolution().getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, sw, sh, null);
        } else {
            g2.setColor(new Color(20, 20, 40));
            g2.fillRect(0, 0, sw, sh);
        }

        if (showEndCard) {
            renderEndCard(g2, sw, sh);
        }

        if (!dialogs.isEmpty() && !finished && currentDialogIndex < dialogs.size()) {
            renderDialogBox(g2, sw, sh);
        }

        g2.dispose();
        super.render(g);
    }

    private void renderDialogBox(Graphics2D g, int screenWidth, int screenHeight) {
        int dialogY     = screenHeight - (int) DIALOG_HEIGHT - 20;
        int dialogX     = 30;
        int dialogWidth = screenWidth - 60;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(dialogX + 3, dialogY + 3, dialogWidth, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BG);
        g.fill(new RoundRectangle2D.Double(dialogX, dialogY, dialogWidth, DIALOG_HEIGHT, 15, 15));
        g.setColor(DIALOG_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(dialogX, dialogY, dialogWidth, DIALOG_HEIGHT, 15, 15));

        int textX        = dialogX + (int) DIALOG_PADDING;
        int textY        = dialogY + 40;
        int maxTextWidth = dialogWidth - (int) (DIALOG_PADDING * 2);
        renderDialogText(g, currentDisplayedText, textX, textY, maxTextWidth);

        if (!isTyping && !finished) {
            String indicator = "▼";
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            int indicatorX = dialogX + dialogWidth - 30;
            int indicatorY = dialogY + (int) DIALOG_HEIGHT - 15;
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                g.setColor(GOLD_HOVER);
                g.drawString(indicator, indicatorX - fm.stringWidth(indicator) / 2, indicatorY);
            }
        }
    }

    private void renderDialogText(Graphics2D g, String text, int x, int y, int maxWidth) {
        g.setFont(new Font("SansSerif", Font.PLAIN, DIALOG_FONT_SIZE));
        g.setColor(TEXT_COLOR);
        String[] lines = wrapText(text != null ? text : "", g.getFontMetrics(), maxWidth);
        int lineHeight = 30;
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x, y + (i * lineHeight));
        }
    }

    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
        if (text.isEmpty()) return new String[]{""};
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (fm.stringWidth(currentLine + " " + word) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines.toArray(new String[0]);
    }

    private void renderEndCard(Graphics2D g, int sw, int sh) {
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

        g.setFont(new Font("Georgia", Font.BOLD, 52));
        g.setColor(GOLD_HOVER);
        FontMetrics fm = g.getFontMetrics();
        String title = "Fim";
        g.drawString(title, cx + (cw - fm.stringWidth(title)) / 2, cy + 90);

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(165, 155, 130));
            fm = g.getFontMetrics();
            String sub = "Pressione Enter ou clique para continuar";
            g.drawString(sub, cx + (cw - fm.stringWidth(sub)) / 2, cy + 140);
        }
    }

    @Override
    public void suspend() {
        super.suspend();
        stopTypingTimer();
    }
}