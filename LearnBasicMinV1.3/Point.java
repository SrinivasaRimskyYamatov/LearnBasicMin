import java.awt.*;

public record Point(int x, int y, Color c) implements ShapeCmd {
    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);
        g.fillRect(x, y, 1, 1);
    }
}
