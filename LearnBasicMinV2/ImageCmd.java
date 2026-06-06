import java.awt.*;
import java.awt.image.BufferedImage;

public record ImageCmd(int x, int y, int w, int h, BufferedImage img) implements ShapeCmd {
    @Override
    public void draw(Graphics2D g) {
        if (img == null) return;
        if (w > 0 && h > 0) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.drawImage(img, x, y, null);
        }
    }
}