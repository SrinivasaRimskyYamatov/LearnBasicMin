import java.awt.*;

public record Box(int x1, int y1, int x2, int y2, Color c) implements ShapeCmd {

    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);

        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);

        g.drawRect(x, y, w, h);
    }
}
