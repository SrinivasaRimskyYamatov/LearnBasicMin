import java.awt.*;

public record Triangle(int x1, int y1, int x2, int y2, int x3, int y3, Color c) implements ShapeCmd {
    @Override
    public void draw(Graphics2D g) {
        g.setColor(c);
        int[] xPoints = {x1, x2, x3};
        int[] yPoints = {y1, y2, y3};
        g.drawPolygon(xPoints, yPoints, 3);
    }
}
