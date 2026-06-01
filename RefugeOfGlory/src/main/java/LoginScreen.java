import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.gui.GuiComponent;
import de.gurkenlabs.litiengine.gui.TextFieldComponent;
import de.gurkenlabs.litiengine.gui.screens.Screen;
import de.gurkenlabs.litiengine.resources.Resources;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class LoginScreen extends Screen {

    private static final Color CARD_BG      = new Color(10,  16,  38,  218);
    private static final Color CARD_BORDER  = new Color(185, 145, 55,  215);
    private static final Color INPUT_BG     = new Color(18,  24,  52,  245);
    private static final Color INPUT_BORDER = new Color(100, 80,  30,  170);
    private static final Color GOLD         = new Color(185, 145, 55);
    private static final Color GOLD_HOVER   = new Color(215, 175, 85);
    private static final Color TEXT_WHITE   = new Color(235, 225, 200);
    private static final Color TEXT_HINT    = new Color(135, 125, 105);
    private static final Color TEXT_LABEL   = new Color(165, 155, 130);
    private static final Color BTN_DARK     = new Color(18,  14,  4);

    private static final double CARD_W = 390;
    private static final double CARD_H = 360;
    private static final double PAD    = 30;

    private double cardX, cardY;
    private int logoX, logoY, logoW, logoH;

    private TextFieldComponent usuarioField;
    private TextFieldComponent senhaField;
    private BufferedImage bgImage;
    private BufferedImage logoImage;

    public LoginScreen() {
        super("LOGIN");
    }

    @Override
    public void prepare() {
        bgImage = Resources.images().get("Interface/Inicio.png");
        logoImage = Resources.images().get("Interface/LogoPNG.png");

        if (logoImage != null) {
            int larguraDesejada = 260;
            logoH = (int) (logoImage.getHeight() * ((double) larguraDesejada / logoImage.getWidth()));
            logoW = larguraDesejada;
        } else {
            logoW = 0;
            logoH = 0;
        }

        int logoSpacing = 15;

        double sw = Game.window().getResolution().getWidth();
        double sh = Game.window().getResolution().getHeight();

        double totalH = logoH + logoSpacing + CARD_H;
        double startY = (sh - totalH) / 2.0;

        logoX = (int) ((sw - logoW) / 2.0);
        logoY = (int) startY;

        cardX = Math.round((sw - CARD_W) / 2.0);
        cardY = Math.round(startY + logoH + logoSpacing);

        buildComponents();
        super.prepare();
    }

    private void buildComponents() {
        this.getComponents().clear();
        double fw = CARD_W - PAD * 2;
        double fx = cardX + PAD;

        usuarioField = buildTextField(fx, cardY + 63, fw, 42, false);
        senhaField   = buildTextField(fx, cardY + 163, fw, 42, true);

        this.getComponents().add(usuarioField);
        this.getComponents().add(senhaField);
        this.getComponents().add(buildBtnEntrar(fx, cardY + 245, fw, 48));
        this.getComponents().add(buildBtnCadastrar(cardX, cardY + 317, CARD_W, 30));
    }

    private TextFieldComponent buildTextField(double x, double y, double w, double h, boolean isPassword) {
        double paddingLeft = 16;
        TextFieldComponent field = new TextFieldComponent(x + paddingLeft, y, w - (paddingLeft * 2), h, "") {
            private String realPassword = "";
            @Override
            public void render(Graphics2D g) {
                if (!this.isVisible()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(INPUT_BG);
                g2.fillRect((int)(getX() - paddingLeft), (int)getY(), (int)(getWidth() + paddingLeft * 2), (int)getHeight());
                g2.dispose();
                super.render(g);
            }
            @Override
            public void setText(String text) {
                if (!isPassword) { super.setText(text); return; }
                this.realPassword = text == null ? "" : text;
                super.setText("*".repeat(this.realPassword.length()));
            }
            @Override
            public String getText() {
                if (isPassword) {
                    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                        if (element.getMethodName().contains("render")) return super.getText();
                    }
                    return this.realPassword;
                }
                return super.getText();
            }
        };
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.getAppearance().setForeColor(TEXT_WHITE);
        return field;
    }

    private GuiComponent buildBtnEntrar(double x, double y, double w, double h) {
        GuiComponent btn = new GuiComponent(x, y, w, h) {
            @Override
            public void render(Graphics2D g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHovered() ? GOLD_HOVER : GOLD);
                g2.fill(new RoundRectangle2D.Double(getX(), getY(), getWidth(), getHeight(), 10, 10));
                g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                g2.setColor(BTN_DARK);
                FontMetrics fm = g2.getFontMetrics();
                String txt = "Entrar";
                g2.drawString(txt, (int)(getX() + (getWidth() - fm.stringWidth(txt)) / 2.0), (int)(getY() + (getHeight() + fm.getAscent() - fm.getDescent()) / 2.0 - 1));
                g2.dispose();
            }
        };
        // CORREÇÃO: redireciona para a tela de introdução narrativa
        btn.onClicked(ignored -> Game.screens().display("INTRO_NARRATIVE"));
        return btn;
    }

    private GuiComponent buildBtnCadastrar(double x, double y, double w, double h) {
        GuiComponent btn = new GuiComponent(x, y, w, h) {
            @Override
            public void render(Graphics2D g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color cor = isHovered() ? GOLD_HOVER : new Color(165, 130, 50);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.setColor(cor);
                FontMetrics fm = g2.getFontMetrics();
                String txt = "Cadastrar-se";
                int tx = (int)(getX() + (getWidth() - fm.stringWidth(txt)) / 2.0);
                int ty = (int)(getY() + (getHeight() + fm.getAscent() - fm.getDescent()) / 2.0);
                g2.drawString(txt, tx, ty);
                if (isHovered()) {
                    g2.drawLine(tx, ty + 2, tx + fm.stringWidth(txt), ty + 2);
                }
                g2.dispose();
            }
        };
        btn.onClicked(ignored -> Game.screens().display("REGISTER"));
        return btn;
    }

    @Override
    public void render(Graphics2D g) {
        int sw = (int) Game.window().getResolution().getWidth();
        int sh = (int) Game.window().getResolution().getHeight();
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);

        if (bgImage != null) g2.drawImage(bgImage, 0, 0, sw, sh, null);
        if (logoImage != null) g2.drawImage(logoImage, logoX, logoY, logoW, logoH, null);

        renderCard(g2);
        g2.dispose();
        super.render(g);
        renderFieldBorders(g);
    }

    private void renderCard(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 95));
        g.fill(new RoundRectangle2D.Double(cardX + 5, cardY + 6, CARD_W, CARD_H, 18, 18));
        g.setColor(CARD_BG);
        g.fill(new RoundRectangle2D.Double(cardX, cardY, CARD_W, CARD_H, 18, 18));
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(cardX, cardY, CARD_W, CARD_H, 18, 18));

        g.setFont(new Font("Georgia", Font.ITALIC, 12));
        g.setColor(TEXT_HINT);
        String sub = "Entre para continuar sua jornada";
        g.drawString(sub, (int)(cardX + (CARD_W - g.getFontMetrics().stringWidth(sub)) / 2.0), (int)(cardY + 23));

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(TEXT_LABEL);
        g.drawString("Nome de usuário", (int)(cardX + PAD), (int)(cardY + 55));
        g.drawString("Senha",           (int)(cardX + PAD), (int)(cardY + 155));
    }

    private void renderFieldBorders(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double fw = CARD_W - PAD * 2;
        int[] yPositions = {(int)cardY + 62, (int)cardY + 162};
        for (int y : yPositions) {
            g2.setColor(INPUT_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Double(cardX + PAD, y, fw, 44, 8, 8));
        }
        g2.dispose();
    }
}