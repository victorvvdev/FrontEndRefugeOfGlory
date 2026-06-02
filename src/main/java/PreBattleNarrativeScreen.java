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

public class PreBattleNarrativeScreen extends Screen {

    private static final Color DIALOG_BG      = new Color(0,   0,   0,   180);
    private static final Color DIALOG_BORDER  = new Color(185, 145, 55,  200);
    private static final Color TEXT_COLOR     = new Color(235, 225, 200);
    private static final Color GOLD_HOVER     = new Color(215, 175, 85);
    private static final Color NAME_BG        = new Color(10,  16,  38,  230);
    private static final Color NAME_BORDER    = new Color(185, 145, 55,  215);
    private static final Color NAME_TEXT      = new Color(215, 175, 85);

    private static final double DIALOG_HEIGHT  = 200;
    private static final double DIALOG_PADDING = 30;
    private static final int    DIALOG_FONT_SIZE = 26;
    private static final long   CHAR_DELAY       = 20;
    private static final int    CRIANCA_SPRITE_H  = 320;

    private static final int CRIANCA_FIRST = 3;
    private static final int CRIANCA_LAST  = 6;

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
    private BufferedImage criancaSprite;

    // Cooldown para evitar avanço múltiplo rápido
    private long lastAdvanceTime = 0;
    private static final long ADVANCE_COOLDOWN = 150;

    public PreBattleNarrativeScreen() {
        super("PRE_BATTLE_NARRATIVE");
    }

    @Override
    public void prepare() {
        backgroundImage = Resources.images().get("Cenários/Parte5.png");
        criancaSprite   = Resources.images().get("Sprites/NPCs/Crianca.png");

        if (dialogs.isEmpty()) {
            dialogs.add(new DialogEntry(null,
                    "O guerreiro avançava por vales sombrios e bosques silenciosos quando um cheiro " +
                            "o alcançou, a podridão. Ao longe aparecia uma vila diante dele, deserta e ensanguentada."));

            dialogs.add(new DialogEntry(null,
                    "As casas estavam inteiras e suas portas abertas. O sangue falava por si só. " +
                            "Ele cruzou a rua principal com a mão no cabo de sua arma, não havia nenhum som, " +
                            "nenhuma alma, até que um barulho o fez olhar de lado."));

            dialogs.add(new DialogEntry(null,
                    "Atrás de uma grande árvore, quase engolida pela floresta, havia uma menina que " +
                            "com olhos arregalados, tremia como um animal indefeso."));

            dialogs.add(new DialogEntry(null,
                    "Ele aproximou-se dela, ajoelhou-se, e serenamente para não assustá-la mais do " +
                            "que já estava, perguntou o que houve."));

            dialogs.add(new DialogEntry("Garota Desconhecida",
                    "— Os monstros... Eles vieram a noite... Sussurrou a menina."));

            dialogs.add(new DialogEntry("Garota Desconhecida",
                    "— Garras, dentes compridos, e algo que parecia uma... uma mão. Mataram todos! " +
                            "Continuou ela com uma voz chorosa."));

            dialogs.add(new DialogEntry("Garota Desconhecida",
                    "— Só eu e mais alguns estamos vivos, estávamos na beira do riacho fazendo preces."));

            dialogs.add(new DialogEntry(null,
                    "A menina conduziu o guerreiro por uma trilha difícil até chegar em um refúgio " +
                            "improvisado. Lá, restavam alguns poucos sobreviventes com os rostos abatidos pela " +
                            "perda, não passava de cinco pessoas."));

            dialogs.add(new DialogEntry(null,
                    "Eles disseram \"Carniçais. Eram carniçais. Junto deles aquela mão rastejante com " +
                            "unhas compridas. Foi muito sangue por todo lado, só nos resta a dor.\""));

            dialogs.add(new DialogEntry(null,
                    "O guerreiro ouviu em silêncio e assentiu, não fez promessas, apenas disse: " +
                            "\"Mostrem onde deixaram os mortos.\""));

            dialogs.add(new DialogEntry(null,
                    "O guerreiro se preparou para aquela noite, arrastou a carcaça de um veado para " +
                            "o centro da vila, o sangue riscava a terra como um convite aos monstros. " +
                            "Esperou, imóvel, com sua arma descansando sobre seus joelhos."));

            dialogs.add(new DialogEntry(null,
                    "Junto com a noite vieram os monstros, primeiro os rosnados e depois os vultos. " +
                            "Carniçais famintos, e entre eles, a tal mão se arrastando pálida pelo chão."));

            dialogs.add(new DialogEntry(null,
                    "A armadilha havia funcionado."));
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
            finishScene();
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
        if (!Game.screens().current().getName().equals("PRE_BATTLE_NARRATIVE")) return;

        long now = System.currentTimeMillis();
        if (now - lastAdvanceTime < ADVANCE_COOLDOWN) return;
        lastAdvanceTime = now;

        if (finished || dialogs.isEmpty()) return;

        if (isTyping) {
            currentDisplayedText = dialogs.get(currentDialogIndex).text;
            charIndex = currentDisplayedText.length();
            isTyping  = false;
            stopTypingTimer();
        } else if (currentDialogIndex + 1 < dialogs.size()) {
            startDialog(currentDialogIndex + 1);
        } else {
            finishScene();
        }
    }

    private void finishScene() {
        if (finished) return;
        finished = true;
        stopTypingTimer();
        // Agenda a troca de tela para o próximo frame, evitando conflitos
        Game.loop().perform(1, () -> Game.screens().display("FIRST_BATTLE"));
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

        boolean mostrarCrianca = currentDialogIndex >= CRIANCA_FIRST
                && currentDialogIndex <= CRIANCA_LAST
                && criancaSprite != null;
        if (mostrarCrianca) {
            renderCriancaSprite(g2, sw, sh);
        }

        if (!dialogs.isEmpty() && !finished && currentDialogIndex < dialogs.size()) {
            renderDialogBox(g2, sw, sh);
        }

        g2.dispose();
        super.render(g);
    }

    private void renderCriancaSprite(Graphics2D g, int sw, int sh) {
        double ratio = (double) criancaSprite.getWidth() / criancaSprite.getHeight();
        int spriteH  = CRIANCA_SPRITE_H;
        int spriteW  = (int) (spriteH * ratio);

        int dialogY = sh - (int) DIALOG_HEIGHT - 20;
        int spriteY = dialogY - spriteH + 60;
        int spriteX = sw - spriteW - 40;

        g.drawImage(criancaSprite, spriteX, spriteY, spriteW, spriteH, null);
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

        String speaker = dialogs.get(currentDialogIndex).speaker;
        if (speaker != null) {
            renderNameBox(g, dialogX, dialogY, speaker);
        }

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

    private void renderNameBox(Graphics2D g, int dialogX, int dialogY, String name) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();

        int padH  = 13;
        int padV  = 8;
        int boxW  = fm.stringWidth(name) + padH * 2;
        int boxH  = fm.getHeight() + padV * 2;
        int boxX  = dialogX + 20;
        int boxY  = dialogY - boxH + 2;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(boxX + 2, boxY + 2, boxW, boxH, 8, 8));
        g.setColor(NAME_BG);
        g.fill(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 8, 8));
        g.setColor(NAME_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 8, 8));
        g.setColor(NAME_TEXT);
        g.drawString(name, boxX + padH, boxY + padV + fm.getAscent());
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

    @Override
    public void suspend() {
        super.suspend();
        stopTypingTimer();
    }
}