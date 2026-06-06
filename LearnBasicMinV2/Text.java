import java.awt.*;

public record Text(int x, int y, String text, double scale, Color c) implements ShapeCmd {
    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);
        Font font = g.getFont();
        g.setFont(font.deriveFont((float)(font.getSize() * scale)));
        g.drawString(text, x, y);
    }
}
