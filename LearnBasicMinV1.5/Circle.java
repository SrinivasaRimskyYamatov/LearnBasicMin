import java.awt.*;

public record Circle(int cx, int cy, int r, Color c) implements ShapeCmd {
    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
    }
}
