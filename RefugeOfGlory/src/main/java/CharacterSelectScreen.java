import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.resources.Resources;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class CharacterSelectScreen extends Screen implements MouseListener, MouseMotionListener {

    private static final Color CARD_BG      = new Color(10, 16, 38, 218);
    private static final Color CARD_BORDER  = new Color(185, 145, 55, 215);
    private static final Color GOLD         = new Color(185, 145, 55);
    private static final Color GOLD_HOVER   = new Color(215, 175, 85);
    private static final Color TEXT_LABEL   = new Color(165, 155, 130);
    private static final Color SELECTED_BG  = new Color(185, 145, 55, 40);
    private static final Color SELECTED_BORDER = new Color(215, 175, 85, 230);

    private static final double CARD_W = 280;
    private static final double CARD_H = 380;
    private static final double SPRITE_MAX_W = 200;
    private static final double SPRITE_MAX_H = 250;

    private BufferedImage bgImage, logotipo, barbaroSprite, espadachimSprite;
    private double cardBarbaroX, cardBarbaroY, cardEspadachimX, cardEspadachimY;
    private int logoX, logoY, logoW, logoH;
    private String selectedCharacter = null;
    private Rectangle cardBarbaroBounds, cardEspadachimBounds, btnConfirmarBounds;
    private boolean hoverBarbaro, hoverEspadachim, hoverConfirmar;
    private boolean listenersRegistered = false;

    public CharacterSelectScreen() {
        super("CHARACTER_SELECT");
    }

    @Override
    public void prepare() {
        bgImage = Resources.images().get("Interface/Inicio.png");
        logotipo = Resources.images().get("Interface/LogoPNG.png");
        barbaroSprite = Resources.images().get("Sprites/Personagens/Barbaro.png");
        espadachimSprite = Resources.images().get("Sprites/Personagens/Espadachim.png");

        if (logotipo != null) {
            int larguraDesejada = 200;
            logoH = (int) (logotipo.getHeight() * ((double) larguraDesejada / logotipo.getWidth()));
            logoW = larguraDesejada;
        }
        calculatePositions();

        // Registra os listeners apenas uma vez, para evitar múltiplas chamadas
        if (!listenersRegistered) {
            Game.window().getRenderComponent().addMouseListener(this);
            Game.window().getRenderComponent().addMouseMotionListener(this);
            listenersRegistered = true;
        }

        super.prepare();
    }

    private void calculatePositions() {
        double sw = Game.window().getResolution().getWidth();
        int logoSpacing = 20;
        double totalW = CARD_W * 2 + 50;
        logoX = (int) ((sw - logoW) / 2.0);
        logoY = 30;
        double cardsStartY = logoY + logoH + logoSpacing + 50;
        double cardsStartX = (sw - totalW) / 2.0;

        cardBarbaroX = cardsStartX;
        cardBarbaroY = cardsStartY;
        cardEspadachimX = cardsStartX + CARD_W + 50;
        cardEspadachimY = cardsStartY;

        cardBarbaroBounds = new Rectangle((int)cardBarbaroX, (int)cardBarbaroY, (int)CARD_W, (int)CARD_H);
        cardEspadachimBounds = new Rectangle((int)cardEspadachimX, (int)cardEspadachimY, (int)CARD_W, (int)CARD_H);

        double btnW = 200, btnH = 50;
        double btnX = (sw - btnW) / 2.0;
        double btnY = cardsStartY + CARD_H + 30;
        btnConfirmarBounds = new Rectangle((int)btnX, (int)btnY, (int)btnW, (int)btnH);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double mx = e.getX(), my = e.getY();
        if (cardBarbaroBounds.contains(mx, my)) {
            selectedCharacter = "BARBARO";
            System.out.println("Bárbaro selecionado!");
        } else if (cardEspadachimBounds.contains(mx, my)) {
            selectedCharacter = "ESPADACHIM";
            System.out.println("Espadachim selecionado!");
        } else if (btnConfirmarBounds.contains(mx, my) && selectedCharacter != null) {
            PlayerData.getInstance().setSelectedCharacter(selectedCharacter);
            Game.screens().display("CHARACTER_STORY");
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {
        hoverBarbaro = hoverEspadachim = hoverConfirmar = false;
    }
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {
        double mx = e.getX(), my = e.getY();
        hoverBarbaro = cardBarbaroBounds.contains(mx, my);
        hoverEspadachim = cardEspadachimBounds.contains(mx, my);
        hoverConfirmar = btnConfirmarBounds.contains(mx, my);
    }

    @Override
    public void suspend() {
        super.suspend();
        // Remove os listeners para não interferirem em outras telas
        Game.window().getRenderComponent().removeMouseListener(this);
        Game.window().getRenderComponent().removeMouseMotionListener(this);
        listenersRegistered = false;
    }

    @Override
    public void render(Graphics2D g) {
        int sw = (int) Game.window().getResolution().getWidth();
        int sh = (int) Game.window().getResolution().getHeight();
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (bgImage != null) g2.drawImage(bgImage, 0, 0, sw, sh, null);
        else { g2.setColor(new Color(20, 20, 40)); g2.fillRect(0, 0, sw, sh); }

        if (logotipo != null) g2.drawImage(logotipo, logoX, logoY, logoW, logoH, null);

        renderTitle(g2, sw);
        renderCharacterCard(g2, cardBarbaroX, cardBarbaroY, "Bárbaro", barbaroSprite, "BARBARO", hoverBarbaro);
        renderCharacterCard(g2, cardEspadachimX, cardEspadachimY, "Espadachim", espadachimSprite, "ESPADACHIM", hoverEspadachim);
        renderConfirmButton(g2, hoverConfirmar);

        g2.dispose();
        super.render(g);
    }

    private void renderTitle(Graphics2D g, int screenWidth) {
        String title = "Selecione o seu guerreiro:";
        g.setFont(new Font("Georgia", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        double titleY = logoY + logoH + 35;
        double titleX = (screenWidth - fm.stringWidth(title)) / 2.0;
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(title, (int)titleX + 2, (int)titleY + 2);
        g.setColor(GOLD);
        g.drawString(title, (int)titleX, (int)titleY);
    }

    private void renderCharacterCard(Graphics2D g, double x, double y, String name,
                                     BufferedImage sprite, String characterId, boolean hovered) {
        boolean isSelected = characterId.equals(selectedCharacter);
        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 4, CARD_W, CARD_H, 15, 15));
        g.setColor(isSelected ? SELECTED_BG : CARD_BG);
        g.fill(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 15, 15));

        if (isSelected) {
            g.setColor(SELECTED_BORDER);
            g.setStroke(new BasicStroke(2.5f));
        } else if (hovered) {
            g.setColor(GOLD_HOVER);
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setColor(CARD_BORDER);
            g.setStroke(new BasicStroke(1.5f));
        }
        g.draw(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 15, 15));

        g.setFont(new Font("Georgia", Font.BOLD, 22));
        g.setColor(isSelected || hovered ? GOLD_HOVER : GOLD);
        FontMetrics fm = g.getFontMetrics();
        double nameX = x + (CARD_W - fm.stringWidth(name)) / 2.0;
        double nameY = y + 35;
        g.drawString(name, (int)nameX, (int)nameY);

        g.setColor(isSelected || hovered ? GOLD_HOVER : GOLD);
        g.setStroke(new BasicStroke(1.5f));
        double lineY = nameY + 8;
        double lineW = 100;
        g.draw(new RoundRectangle2D.Double(x + (CARD_W - lineW) / 2.0, lineY, lineW, 2, 2, 2));

        if (isSelected) {
            String indicator = "✦ SELECIONADO ✦";
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(GOLD_HOVER);
            fm = g.getFontMetrics();
            double indicatorY = lineY + 18;
            g.drawString(indicator, (int)(x + (CARD_W - fm.stringWidth(indicator)) / 2.0), (int)indicatorY);
        }

        if (sprite != null) {
            double spriteRatio = (double) sprite.getWidth() / sprite.getHeight();
            double scaleFactor = characterId.equals("BARBARO") ? 1.25 : 1.0;
            double spriteW, spriteH;
            if (spriteRatio > 1) {
                spriteW = SPRITE_MAX_W * scaleFactor;
                spriteH = (SPRITE_MAX_W / spriteRatio) * scaleFactor;
            } else {
                spriteH = SPRITE_MAX_H * scaleFactor;
                spriteW = (SPRITE_MAX_H * spriteRatio) * scaleFactor;
            }
            double spriteX = x + (CARD_W - spriteW) / 2.0;
            double yOffset = characterId.equals("ESPADACHIM") ? 65.0 : 0.0;
            double spriteY = lineY + 25 + yOffset;
            g.drawImage(sprite, (int)spriteX, (int)spriteY, (int)spriteW, (int)spriteH, null);
        } else {
            g.setColor(new Color(50, 50, 70, 150));
            double placeholderY = lineY + 25;
            double placeholderH = SPRITE_MAX_H;
            g.fill(new RoundRectangle2D.Double(x + 40, placeholderY, CARD_W - 80, placeholderH, 10, 10));
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(TEXT_LABEL);
            String placeholder = "Sprite não encontrado";
            fm = g.getFontMetrics();
            g.drawString(placeholder, (int)(x + (CARD_W - fm.stringWidth(placeholder)) / 2.0),
                    (int)(placeholderY + placeholderH / 2));
        }
    }

    private void renderConfirmButton(Graphics2D g, boolean hovered) {
        double x = btnConfirmarBounds.getX(), y = btnConfirmarBounds.getY();
        double w = btnConfirmarBounds.getWidth(), h = btnConfirmarBounds.getHeight();
        boolean enabled = selectedCharacter != null;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 3, w, h, 10, 10));
        if (!enabled) g.setColor(new Color(60, 60, 60, 200));
        else if (hovered) g.setColor(GOLD_HOVER);
        else g.setColor(GOLD);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(enabled ? Color.BLACK : new Color(100, 100, 100));
        FontMetrics fm = g.getFontMetrics();
        String txt = "Confirmar";
        double txtX = x + (w - fm.stringWidth(txt)) / 2.0;
        double txtY = y + (h + fm.getAscent() - fm.getDescent()) / 2.0 - 1;
        g.drawString(txt, (int)txtX, (int)txtY);
    }
}