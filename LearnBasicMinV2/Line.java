import java.awt.*;

public record Line(int x1, int y1, int x2, int y2, Color c) implements ShapeCmd {

    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
    }
}
