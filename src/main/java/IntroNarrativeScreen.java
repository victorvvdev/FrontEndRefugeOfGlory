import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.litiengine.resources.Resources;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class IntroNarrativeScreen extends Screen {

    // Cores e dimensões
    private static final Color DIALOG_BG = new Color(0, 0, 0, 180);
    private static final Color DIALOG_BORDER = new Color(185, 145, 55, 200);
    private static final Color TEXT_COLOR = new Color(235, 225, 200);
    private static final Color GOLD_HOVER = new Color(215, 175, 85);

    private static final double DIALOG_HEIGHT = 200;
    private static final double DIALOG_PADDING = 30;
    private static final int DIALOG_FONT_SIZE = 26;
    private static final long CHAR_DELAY = 20;

    // Diálogos
    private List<DialogLine> dialogs;
    private int currentDialogIndex = 0;
    private String currentDisplayedText = "";
    private int charIndex = 0;
    private boolean isTyping = false;
    private Timer typingTimer;
    private BufferedImage backgroundImage;

    // Controle de cooldown para evitar múltiplos avanços rápidos
    private long lastAdvanceTime = 0;
    private static final long ADVANCE_COOLDOWN = 150;

    public IntroNarrativeScreen() {
        super("INTRO_NARRATIVE");
        this.dialogs = new ArrayList<>();
    }

    @Override
    public void prepare() {
        backgroundImage = Resources.images().get("Cenários/Parte1e2.png");
        initDialogs();
        setupInput();
        currentDialogIndex = 0;
        if (!dialogs.isEmpty()) {
            startDialog(0);
        }
        super.prepare();
    }

    private void initDialogs() {
        dialogs.clear();
        dialogs.add(new DialogLine(null,
                "Há séculos, bestas sedentas por sangue humano apareceram na terra " +
                        "destruindo vilas e povoados. Os reis erguiam muralhas altas, mas não " +
                        "era o suficiente para manter o povo seguro, as crianças já nascem " +
                        "aprendendo caminhos que nunca deveriam seguir..."));
        dialogs.add(new DialogLine(null,
                "Sem ordens de reis ou promessas de recompensas, duas lâminas se " +
                        "ergueram contra o que ninguém ousava lutar e partiram por estradas " +
                        "cobertas por medo e sangue. Onde as muralhas falharam, dois bravos " +
                        "guerreiros avançavam nas ruínas. Pela primeira vez em séculos, as " +
                        "bestas sentiram temor..."));
        dialogs.add(new DialogLine(null,
                "Os habitantes dos vilarejos dizem que, segundo histórias antigas, " +
                        "há um lugar que vive nos sussurros além das terras desconhecidas. " +
                        "Ninguém provou sua existência e nenhum mapa mostrou seu caminho. " +
                        "Somente os que ousarem eliminar todas as bestas da terra conseguem " +
                        "ver e adentrar pelos os seus portões. Ao entrar no lugar que é " +
                        "chamado O Refúgio da Glória, o guerreiro ascende, ele estará livre " +
                        "do sangue nas suas mãos e do peso de sua lâmina. Seu dever encerrará " +
                        "ali e enfim encontrará a paz que nunca conhecera antes."));
    }

    private void setupInput() {
        Input.keyboard().onKeyPressed(KeyEvent.VK_ENTER, e -> advanceDialog());
        Input.keyboard().onKeyPressed(KeyEvent.VK_SPACE, e -> advanceDialog());
        Input.mouse().onClicked(e -> advanceDialog());
    }

    private void startDialog(int index) {
        if (index >= 0 && index < dialogs.size()) {
            stopTypingTimer();
            currentDialogIndex = index;
            currentDisplayedText = "";
            charIndex = 0;
            isTyping = true;
            startTypingTimer();
        } else {
            onDialogsFinished();
        }
    }

    private void startTypingTimer() {
        stopTypingTimer();
        typingTimer = new Timer(true);
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isTyping && currentDialogIndex < dialogs.size()) {
                    String fullText = dialogs.get(currentDialogIndex).getText();
                    if (charIndex < fullText.length()) {
                        currentDisplayedText += fullText.charAt(charIndex);
                        charIndex++;
                    } else {
                        isTyping = false;
                        stopTypingTimer();
                    }
                } else {
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
        // Verifica se esta tela está ativa
        if (!Game.screens().current().getName().equals("INTRO_NARRATIVE")) return;

        // Cooldown para evitar múltiplos avanços no mesmo frame
        long now = System.currentTimeMillis();
        if (now - lastAdvanceTime < ADVANCE_COOLDOWN) return;
        lastAdvanceTime = now;

        if (dialogs.isEmpty()) return;

        if (isTyping) {
            // Completa o texto instantaneamente
            currentDisplayedText = dialogs.get(currentDialogIndex).getText();
            charIndex = currentDisplayedText.length();
            isTyping = false;
            stopTypingTimer();
        } else if (currentDialogIndex + 1 < dialogs.size()) {
            // Avança para o próximo diálogo
            startDialog(currentDialogIndex + 1);
        } else {
            // Último diálogo finalizado
            onDialogsFinished();
        }
    }

    private void onDialogsFinished() {
        stopTypingTimer();
        // Agenda a troca de tela para o próximo frame, evitando conflitos
        Game.loop().perform(1, () -> Game.screens().display("CHARACTER_SELECT"));
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

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, sw, sh, null);
        } else {
            g2.setColor(new Color(20, 20, 40));
            g2.fillRect(0, 0, sw, sh);
        }

        if (dialogs != null && !dialogs.isEmpty()) {
            renderDialogBox(g2, sw, sh);
        }
        g2.dispose();
        super.render(g);
    }

    private void renderDialogBox(Graphics2D g, int screenWidth, int screenHeight) {
        if (dialogs == null || dialogs.isEmpty() || currentDialogIndex >= dialogs.size()) return;

        int dialogY = screenHeight - (int) DIALOG_HEIGHT - 20;
        int dialogX = 30;
        int dialogWidth = screenWidth - 60;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(dialogX + 3, dialogY + 3, dialogWidth, DIALOG_HEIGHT, 15, 15));

        g.setColor(DIALOG_BG);
        g.fill(new RoundRectangle2D.Double(dialogX, dialogY, dialogWidth, DIALOG_HEIGHT, 15, 15));

        g.setColor(DIALOG_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(dialogX, dialogY, dialogWidth, DIALOG_HEIGHT, 15, 15));

        int textX = dialogX + (int) DIALOG_PADDING;
        int textY = dialogY + 40;
        int maxTextWidth = dialogWidth - (int) (DIALOG_PADDING * 2);
        renderDialogText(g, currentDisplayedText, textX, textY, maxTextWidth);

        if (!isTyping) {
            String indicator = "▼";
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            int indicatorX = dialogX + dialogWidth - 40;
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
        String[] lines = wrapText(text, g.getFontMetrics(), maxWidth);
        int lineHeight = 30;
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x, y + (i * lineHeight));
        }
    }

    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
        if (text == null || text.isEmpty()) return new String[]{""};
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

    @Override
    public void suspend() {
        super.suspend();
        stopTypingTimer();
    }

    private static class DialogLine {
        private final String speaker;
        private final String text;
        public DialogLine(String speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
        public String getSpeaker() { return speaker; }
        public String getText() { return text; }
    }
}