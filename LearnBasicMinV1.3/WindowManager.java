import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class WindowManager {

    int next = 1;
    Map<Integer, DrawWindow> map = new HashMap<>();

    public int create(int h, int w) {
        int id = next++;
        map.put(id, new DrawWindow(id, h, w));
        return id;
    }

    DrawWindow get(int id) {
        DrawWindow w = map.get(id);
        if (w == null) throw new RuntimeException("invalid window id: " + id);
        return w;
    }

    public void delete(int id) {
        DrawWindow w = map.remove(id);
        if (w != null) w.dispose();
    }

    public void setColor(int id, int r, int g, int b) {
        get(id).setColor(new Color(r, g, b));
    }

    public void drawLine(int id, int x1, int y1, int x2, int y2) {
        get(id).add(new Line(x1, y1, x2, y2, get(id).c));
    }

    public void drawBox(int id, int x1, int y1, int x2, int y2) {
        get(id).add(new Box(x1, y1, x2, y2, get(id).c));
    }

    // ==================== 新規追加機能 ====================

    public void windowName(int id, String title) {
        get(id).setTitle(title);
    }

    public void windowSize(int id, int height, int width) {
        DrawWindow w = get(id);
        w.setSize(width, height);
        w.revalidate();
        w.repaint();
    }

    public void drawPut(int id, int x, int y) {
        get(id).add(new Point(x, y, get(id).c));
    }

    public void drawCircle(int id, int cx, int cy, int r) {
        get(id).add(new Circle(cx, cy, r, get(id).c));
    }

    public void drawTri(int id, int x1, int y1, int x2, int y2, int x3, int y3) {
        get(id).add(new Triangle(x1, y1, x2, y2, x3, y3, get(id).c));
    }

    public void drawCls(int id) {
        DrawWindow w = get(id);
        w.list.clear();
        w.repaint();
    }

    // ==================== draw.fill ====================
    public void drawFill(int id, int x, int y) {
        DrawWindow w = get(id);
        BufferedImage img = w.getImage();
        if (img == null) return;

        floodFill(img, x, y, w.c);
        w.repaintImage();
    }

    private void floodFill(BufferedImage img, int x, int y, Color newColor) {
        if (x < 0 || x >= img.getWidth() || y < 0 || y >= img.getHeight()) return;

        int targetColor = img.getRGB(x, y);
        int replacementColor = newColor.getRGB();

        if (targetColor == replacementColor) return;

        Queue<java.awt.Point> queue = new LinkedList<>();
        queue.add(new java.awt.Point(x, y));

        while (!queue.isEmpty()) {
            java.awt.Point p = queue.poll();
            int px = p.x;
            int py = p.y;

            if (px < 0 || px >= img.getWidth() || py < 0 || py >= img.getHeight()) continue;
            if (img.getRGB(px, py) != targetColor) continue;

            img.setRGB(px, py, replacementColor);

            queue.add(new java.awt.Point(px + 1, py));
            queue.add(new java.awt.Point(px - 1, py));
            queue.add(new java.awt.Point(px, py + 1));
            queue.add(new java.awt.Point(px, py - 1));
        }
    }
}